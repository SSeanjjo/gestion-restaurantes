package co.edu.uniquindio.comanda.pedido;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.auth.Rol;
import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.auth.UsuarioRepository;
import co.edu.uniquindio.comanda.common.JornadaOperativa;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.inventario.IngredienteDto;
import co.edu.uniquindio.comanda.menu.ProductoMenu;
import co.edu.uniquindio.comanda.mesa.Mesa;
import co.edu.uniquindio.comanda.mesa.MesaRepository;
import co.edu.uniquindio.comanda.mesa.MesaService;
import co.edu.uniquindio.comanda.pedido.ConsumoIngredientesPedido.Reserva;
import co.edu.uniquindio.comanda.pedido.PedidoRequests.PlatoNoDisponible;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoCambioPedido;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoNotificacion;

/**
 * Lógica del CU-01 Gestionar Pedido. Actúa como <b>Facade</b> para los controladores: cada
 * método público es una operación del sistema (contratos 1 y 2) y coordina repositorios,
 * consumo de inventario y eventos.
 *
 * Orden de bloqueo en todas las operaciones que tocan inventario: primero la mesa y luego
 * los ingredientes (ordenados por id). Mantener siempre el mismo orden evita interbloqueos.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final MesaService servicioMesas;
    private final UsuarioRepository usuarios;
    private final ConsumoIngredientesPedido consumo;
    private final JornadaOperativa jornada;
    private final ApplicationEventPublisher eventos;

    public PedidoService(PedidoRepository pedidos, MesaRepository mesas, MesaService servicioMesas,
            UsuarioRepository usuarios, ConsumoIngredientesPedido consumo, JornadaOperativa jornada, ApplicationEventPublisher eventos) {
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.servicioMesas = servicioMesas;
        this.usuarios = usuarios;
        this.consumo = consumo;
        this.jornada = jornada;
        this.eventos = eventos;
    }

    // ------------------------------------------------------------------
    // F-03, F-05, F-06, F-07 · Registrar y confirmar pedido
    // ------------------------------------------------------------------

    /**
     * Contrato 1 · crearPedido(numeroMesa, listaItems).
     *
     * En una sola transacción: valida la mesa, vuelve a validar el stock con los ingredientes
     * bloqueados (SWR-05), registra el pedido, descuenta el inventario (SWR-06), ocupa la mesa
     * y, tras el commit, envía la comanda a cocina (SWR-07).
     */
    @Transactional
    public PedidoDto crearPedido(PedidoRequests.CrearPedido req, Long meseroId) {
        jornada.exigirCajaAbierta();
        Usuario mesero = usuario(meseroId);
        Mesa mesa = bloquearMesa(req.numeroMesa());

        Optional<Pedido> repetido = pedidoYaRegistrado(req.claveIdempotencia());
        if (repetido.isPresent()) {
            return PedidoDto.de(repetido.get());
        }
        if (mesa.estaOcupada()) {
            throw NegocioException.conflicto("MESA_OCUPADA",
                    "La mesa " + mesa.getNumero() + " ya tiene un pedido activo");
        }

        Map<Long, Integer> unidades = unidadesPorProducto(req.items());
        Reserva reserva = consumo.reservar(unidades);
        validarProductos(unidades, reserva.productos());
        validarStock(unidades, reserva);

        Pedido pedido = new Pedido(mesa, mesero, req.claveIdempotencia());
        unidades.forEach((productoId, cant) -> pedido.agregarDetalle(reserva.productos().get(productoId), cant));
        pedidos.save(pedido);

        List<IngredienteDto> enMinimo = consumo.descontar(reserva, pedido.getId(), meseroId);
        mesa.ocupar();

        publicarCreacion(pedido, enMinimo);
        return PedidoDto.de(pedido);
    }

    // ------------------------------------------------------------------
    // F-09, F-10 · Cocina
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PedidoDto> comandasEnCocina() {
        return pedidos.comandasEnCocina(EstadoPedido.EN_COCINA).stream().map(PedidoDto::de).toList();
    }

    /**
     * Contrato 2 · actualizarEstado(idPedido, nuevoEstado). Desde cocina solo se permite
     * pasar a EN_PREPARACION o a LISTO_PARA_SERVIR, siempre respetando RD-03.
     */
    @Transactional
    public PedidoDto actualizarEstadoDesdeCocina(Long pedidoId, EstadoPedido nuevoEstado) {
        if (nuevoEstado != EstadoPedido.EN_PREPARACION && nuevoEstado != EstadoPedido.LISTO_PARA_SERVIR) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "ESTADO_NO_PERMITIDO",
                    "Desde cocina solo se puede pasar a EN_PREPARACION o LISTO_PARA_SERVIR");
        }
        Pedido pedido = cargar(pedidoId);
        boolean quedaListo = nuevoEstado == EstadoPedido.LISTO_PARA_SERVIR;
        if (quedaListo) {
            pedido.marcarListo();
        } else {
            pedido.cambiarEstado(nuevoEstado);
        }
        publicarActualizacion(pedido);
        if (quedaListo) {
            eventos.publishEvent(PedidoMapper.notificacion(pedido, TipoNotificacion.LISTO));
        }
        return PedidoDto.de(pedido);
    }

    @Transactional
    public PedidoDto marcarItemPreparado(Long pedidoId, Long detalleId, boolean preparado) {
        Pedido pedido = cargar(pedidoId);
        pedido.marcarItemPreparado(detalleId, preparado);
        PedidoDto dto = PedidoDto.de(pedido);
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.ACTUALIZADO, dto));
        return dto;
    }

    // ------------------------------------------------------------------
    // F-11, F-12, F-14 · Notificación, entrega y solicitud de cuenta
    // ------------------------------------------------------------------

    /** Notificaciones pendientes del mesero (para recuperarlas si recarga la tablet). */
    @Transactional(readOnly = true)
    public List<Eventos.NotificacionMesero> notificacionesPendientes(Long meseroId) {
        return pedidos.findByMeseroIdAndEstadoOrderByNotificadoEnAsc(meseroId, EstadoPedido.LISTO_PARA_SERVIR)
                .stream()
                .map(p -> PedidoMapper.notificacion(p, TipoNotificacion.LISTO))
                .toList();
    }

    @Transactional
    public PedidoDto marcarServido(Long pedidoId) {
        Pedido pedido = cargar(pedidoId);
        pedido.marcarEntregado();
        // Sale de la pantalla de cocina, se retira la notificación y queda disponible para cobro (CU-02)
        publicarActualizacion(pedido);
        eventos.publishEvent(PedidoMapper.notificacion(pedido, TipoNotificacion.ATENDIDA));
        eventos.publishEvent(new Eventos.CajaActualizada("PEDIDO_ENTREGADO", pedido.getId()));
        return PedidoDto.de(pedido);
    }

    /** F-14: el mesero avisa a caja que el cliente pidió la cuenta (estado E-07). */
    @Transactional
    public PedidoDto solicitarCuenta(Long pedidoId) {
        Pedido pedido = cargar(pedidoId);
        pedido.solicitarCuenta();
        eventos.publishEvent(new Eventos.CajaActualizada("CUENTA_SOLICITADA", pedido.getId()));
        return PedidoDto.de(pedido);
    }

    /** Curso alterno 6a: reenvía la alerta de los pedidos listos que no se han servido. */
    @Transactional
    public int reenviarAlertasVencidas(LocalDateTime limite) {
        List<Pedido> vencidos = pedidos.findByEstadoAndNotificadoEnBefore(EstadoPedido.LISTO_PARA_SERVIR, limite);
        vencidos.forEach(p -> {
            p.registrarRecordatorio();
            eventos.publishEvent(PedidoMapper.notificacion(p, TipoNotificacion.RECORDATORIO));
        });
        return vencidos.size();
    }

    // ------------------------------------------------------------------
    // F-13 · Anular
    // ------------------------------------------------------------------

    /**
     * Anula un pedido que aún no entra a preparación: lo retira de cocina, reintegra
     * los ingredientes y libera la mesa. Queda auditado (usuario, fecha y motivo).
     */
    @Transactional
    public PedidoDto anular(Long pedidoId, String motivo, Long usuarioId, Rol rol) {
        Pedido pedido = cargar(pedidoId);
        validarQuePuedeAnular(pedido, usuarioId, rol);
        Usuario usuario = usuario(usuarioId);

        // Mismo orden de bloqueo que crearPedido: mesa → ingredientes
        Mesa mesa = mesas.findByIdParaActualizar(pedido.getMesa().getId()).orElseThrow();
        consumo.reintegrar(consumo.reservar(unidadesDe(pedido)), pedido.getId(), usuarioId);

        pedido.anular(usuario, motivo.trim());

        PedidoDto dto = PedidoDto.de(pedido);
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.ANULADO, dto));
        servicioMesas.liberar(mesa); // libera y publica MesaCambiada (mismo orden de eventos que antes)
        eventos.publishEvent(new Eventos.MenuActualizado());
        eventos.publishEvent(new Eventos.CajaActualizada("PEDIDO_ANULADO", pedido.getId()));
        return dto;
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares (Extract Method: cada uno hace una sola cosa)
    // ------------------------------------------------------------------

    private Usuario usuario(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_EXISTE", "Usuario no encontrado"));
    }

    /** Bloquea la mesa (RD-01): dos meseros no pueden abrir pedido en la misma mesa a la vez. */
    private Mesa bloquearMesa(Integer numero) {
        return mesas.findByNumeroParaActualizar(numero)
                .orElseThrow(() -> NegocioException.noEncontrado("MESA_NO_EXISTE", "No existe la mesa " + numero));
    }

    private Optional<Pedido> pedidoYaRegistrado(String clave) {
        return clave == null || clave.isBlank() ? Optional.empty() : pedidos.findByClaveIdempotencia(clave);
    }

    /** El mismo plato puede venir en varias líneas: se agrupan las unidades por producto. */
    private static Map<Long, Integer> unidadesPorProducto(List<PedidoRequests.Item> items) {
        Map<Long, Integer> unidades = new LinkedHashMap<>();
        items.forEach(i -> unidades.merge(i.productoId(), i.cantidad(), Integer::sum));
        return unidades;
    }

    private static Map<Long, Integer> unidadesDe(Pedido pedido) {
        Map<Long, Integer> unidades = new LinkedHashMap<>();
        pedido.getDetalles().forEach(d -> unidades.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum));
        return unidades;
    }

    /** RD-02: todos los productos deben existir y estar activos. */
    private static void validarProductos(Map<Long, Integer> unidades, Map<Long, ProductoMenu> productos) {
        for (Long id : unidades.keySet()) {
            ProductoMenu p = productos.get(id);
            if (p == null) {
                throw NegocioException.noEncontrado("PRODUCTO_NO_EXISTE", "No existe el producto " + id);
            }
            if (!p.isActivo()) {
                throw new NegocioException(HttpStatus.CONFLICT, "PRODUCTO_INACTIVO",
                        "El plato " + p.getNombre() + " está suspendido",
                        List.of(new PlatoNoDisponible(p.getId(), p.getNombre())));
            }
        }
    }

    /** SWR-05: si falta algún ingrediente se rechaza indicando los platos afectados. */
    private static void validarStock(Map<Long, Integer> unidades, Reserva reserva) {
        Set<Long> faltantes = reserva.faltantes();
        if (faltantes.isEmpty()) {
            return;
        }
        List<PlatoNoDisponible> platos = unidades.keySet().stream()
                .map(reserva.productos()::get)
                .filter(p -> p.getReceta().stream().anyMatch(r -> faltantes.contains(r.getIngrediente().getId())))
                .map(p -> new PlatoNoDisponible(p.getId(), p.getNombre()))
                .toList();
        throw new NegocioException(HttpStatus.CONFLICT, "INGREDIENTES_INSUFICIENTES",
                "No hay ingredientes suficientes para: "
                        + platos.stream().map(PlatoNoDisponible::nombre).collect(Collectors.joining(", ")),
                platos);
    }

    private static void validarQuePuedeAnular(Pedido pedido, Long usuarioId, Rol rol) {
        if (rol == Rol.MESERO && !pedido.getMesero().getId().equals(usuarioId)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "NO_ES_SU_PEDIDO",
                    "Solo el mesero que tomó el pedido o un administrador pueden anularlo");
        }
        if (pedido.getEstado() != EstadoPedido.REGISTRADO) {
            throw NegocioException.conflicto("NO_ANULABLE",
                    "Solo se puede anular un pedido que la cocina no ha empezado a preparar");
        }
    }

    private void publicarCreacion(Pedido pedido, List<IngredienteDto> enMinimo) {
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.CREADO, PedidoDto.de(pedido)));
        eventos.publishEvent(new Eventos.MesaCambiada(PedidoMapper.mesa(pedido.getMesa(), pedido)));
        eventos.publishEvent(new Eventos.MenuActualizado());
        if (!enMinimo.isEmpty()) {
            eventos.publishEvent(new Eventos.AlertaInventario(enMinimo));
        }
    }

    private void publicarActualizacion(Pedido pedido) {
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.ACTUALIZADO, PedidoDto.de(pedido)));
        eventos.publishEvent(new Eventos.MesaCambiada(PedidoMapper.mesa(pedido.getMesa(), pedido)));
    }

    private Pedido cargar(Long pedidoId) {
        return pedidos.buscarCompleto(pedidoId)
                .orElseThrow(() -> NegocioException.noEncontrado("PEDIDO_NO_EXISTE",
                        "No existe el pedido " + pedidoId));
    }
}
