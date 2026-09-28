package co.edu.uniquindio.comanda.pedido;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.auth.Rol;
import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.auth.UsuarioRepository;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.inventario.Ingrediente;
import co.edu.uniquindio.comanda.inventario.IngredienteDto;
import co.edu.uniquindio.comanda.inventario.IngredienteRepository;
import co.edu.uniquindio.comanda.inventario.MovimientoInventario;
import co.edu.uniquindio.comanda.inventario.MovimientoInventarioRepository;
import co.edu.uniquindio.comanda.inventario.TipoMovimiento;
import co.edu.uniquindio.comanda.menu.CalculadoraReceta;
import co.edu.uniquindio.comanda.menu.ProductoMenu;
import co.edu.uniquindio.comanda.menu.ProductoMenuRepository;
import co.edu.uniquindio.comanda.mesa.EstadoMesa;
import co.edu.uniquindio.comanda.mesa.Mesa;
import co.edu.uniquindio.comanda.mesa.MesaDto;
import co.edu.uniquindio.comanda.mesa.MesaRepository;
import co.edu.uniquindio.comanda.pedido.PedidoRequests.PlatoNoDisponible;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoCambioPedido;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoNotificacion;

/**
 * Lógica del CU-01 Gestionar Pedido.
 *
 * Orden de bloqueo en todas las operaciones que tocan inventario: primero la mesa y luego
 * los ingredientes (ordenados por id). Mantener siempre el mismo orden evita interbloqueos.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final ProductoMenuRepository productos;
    private final IngredienteRepository ingredientes;
    private final MovimientoInventarioRepository movimientos;
    private final UsuarioRepository usuarios;
    private final ApplicationEventPublisher eventos;

    public PedidoService(PedidoRepository pedidos, MesaRepository mesas, ProductoMenuRepository productos,
            IngredienteRepository ingredientes, MovimientoInventarioRepository movimientos,
            UsuarioRepository usuarios, ApplicationEventPublisher eventos) {
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.productos = productos;
        this.ingredientes = ingredientes;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    // ------------------------------------------------------------------
    // F-01 · Mesas y pedido activo
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MesaDto> listarMesas() {
        Map<Long, Pedido> activosPorMesa = pedidos.findByEstadoIn(EstadoPedido.ACTIVOS).stream()
                .collect(Collectors.toMap(p -> p.getMesa().getId(), Function.identity(), (a, b) -> a));
        return mesas.findAllByOrderByNumeroAsc().stream()
                .map(m -> aDto(m, activosPorMesa.get(m.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PedidoDto> pedidoActivoDeMesa(Integer numeroMesa) {
        if (mesas.findByNumero(numeroMesa).isEmpty()) {
            throw NegocioException.noEncontrado("MESA_NO_EXISTE", "No existe la mesa " + numeroMesa);
        }
        return pedidos.buscarPorMesaYEstados(numeroMesa, EstadoPedido.ACTIVOS).map(PedidoDto::de);
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
        Usuario mesero = usuarios.findById(meseroId)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_EXISTE", "Usuario no encontrado"));

        // 1. Bloquear la mesa (RD-01: no se puede abrir pedido en una mesa ocupada)
        Mesa mesa = mesas.findByNumeroParaActualizar(req.numeroMesa())
                .orElseThrow(() -> NegocioException.noEncontrado("MESA_NO_EXISTE",
                        "No existe la mesa " + req.numeroMesa()));
        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw NegocioException.conflicto("MESA_OCUPADA",
                    "La mesa " + mesa.getNumero() + " ya tiene un pedido activo");
        }

        // 2. Agrupar unidades por producto (el mismo plato puede venir en varias líneas)
        Map<Long, Integer> unidades = new LinkedHashMap<>();
        req.items().forEach(i -> unidades.merge(i.productoId(), i.cantidad(), Integer::sum));

        // 3. Bloquear los ingredientes ANTES de cargar los productos, para leer el stock ya bloqueado
        List<Long> idsIngredientes = unidades.isEmpty() ? List.of()
                : productos.idsIngredientesDeProductos(unidades.keySet());
        Map<Long, Ingrediente> bloqueados = idsIngredientes.isEmpty() ? Map.of()
                : ingredientes.bloquearPorIds(idsIngredientes).stream()
                        .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));

        // 4. Cargar productos con su receta y validar que existan y estén activos (RD-02)
        Map<Long, ProductoMenu> porId = productos.findConRecetaPorIds(unidades.keySet()).stream()
                .collect(Collectors.toMap(ProductoMenu::getId, Function.identity()));
        for (Long id : unidades.keySet()) {
            ProductoMenu p = porId.get(id);
            if (p == null) {
                throw NegocioException.noEncontrado("PRODUCTO_NO_EXISTE", "No existe el producto " + id);
            }
            if (!p.isActivo()) {
                throw new NegocioException(HttpStatus.CONFLICT, "PRODUCTO_INACTIVO",
                        "El plato " + p.getNombre() + " está suspendido",
                        List.of(new PlatoNoDisponible(p.getId(), p.getNombre())));
            }
        }

        // 5. SWR-05: re-validar stock con el consumo total del pedido
        Map<Long, BigDecimal> requerido = CalculadoraReceta.requerimientos(porId, unidades);
        Set<Long> faltantes = requerido.entrySet().stream()
                .filter(e -> !bloqueados.get(e.getKey()).alcanzaPara(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!faltantes.isEmpty()) {
            List<PlatoNoDisponible> platos = unidades.keySet().stream()
                    .map(porId::get)
                    .filter(p -> p.getReceta().stream()
                            .anyMatch(r -> faltantes.contains(r.getIngrediente().getId())))
                    .map(p -> new PlatoNoDisponible(p.getId(), p.getNombre()))
                    .toList();
            throw new NegocioException(HttpStatus.CONFLICT, "INGREDIENTES_INSUFICIENTES",
                    "No hay ingredientes suficientes para: "
                            + platos.stream().map(PlatoNoDisponible::nombre).collect(Collectors.joining(", ")),
                    platos);
        }

        // 6. Registrar el pedido y sus detalles
        Pedido pedido = new Pedido(mesa, mesero);
        unidades.forEach((productoId, cant) -> pedido.agregarDetalle(porId.get(productoId), cant));
        pedidos.save(pedido);

        // 7. SWR-06: descontar inventario en la misma transacción
        List<IngredienteDto> enMinimo = new ArrayList<>();
        requerido.forEach((ingredienteId, cantidad) -> {
            Ingrediente ing = bloqueados.get(ingredienteId);
            boolean yaEstabaEnMinimo = ing.enNivelMinimo();
            ing.descontar(cantidad);
            movimientos.save(new MovimientoInventario(ing, TipoMovimiento.SALIDA_PEDIDO, cantidad.negate(),
                    pedido.getId(), meseroId, "Pedido #" + pedido.getId()));
            if (!yaEstabaEnMinimo && ing.enNivelMinimo()) {
                enMinimo.add(IngredienteDto.de(ing));
            }
        });

        // 8. Contrato 1: la mesa pasa a Ocupada
        mesa.setEstado(EstadoMesa.OCUPADA);

        // 9. Eventos (se difunden después del commit)
        PedidoDto dto = PedidoDto.de(pedido);
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.CREADO, dto));
        eventos.publishEvent(new Eventos.MesaCambiada(aDto(mesa, pedido)));
        eventos.publishEvent(new Eventos.MenuActualizado());
        if (!enMinimo.isEmpty()) {
            eventos.publishEvent(new Eventos.AlertaInventario(enMinimo));
        }
        return dto;
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
        if (nuevoEstado == EstadoPedido.LISTO_PARA_SERVIR) {
            pedido.marcarListo();
        } else {
            pedido.cambiarEstado(nuevoEstado);
        }
        PedidoDto dto = PedidoDto.de(pedido);
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.ACTUALIZADO, dto));
        eventos.publishEvent(new Eventos.MesaCambiada(aDto(pedido.getMesa(), pedido)));
        if (nuevoEstado == EstadoPedido.LISTO_PARA_SERVIR) {
            eventos.publishEvent(notificacion(pedido, TipoNotificacion.LISTO));
        }
        return dto;
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
    // F-11, F-12 · Notificación y entrega
    // ------------------------------------------------------------------

    /** Notificaciones pendientes del mesero (para recuperarlas si recarga la tablet). */
    @Transactional(readOnly = true)
    public List<Eventos.NotificacionMesero> notificacionesPendientes(Long meseroId) {
        return pedidos.findByMeseroIdAndEstadoOrderByNotificadoEnAsc(meseroId, EstadoPedido.LISTO_PARA_SERVIR)
                .stream()
                .map(p -> notificacion(p, TipoNotificacion.LISTO))
                .toList();
    }

    @Transactional
    public PedidoDto marcarServido(Long pedidoId) {
        Pedido pedido = cargar(pedidoId);
        pedido.marcarEntregado();
        PedidoDto dto = PedidoDto.de(pedido);
        // Sale de la pantalla de cocina y se retira la notificación de la tablet.
        // La mesa sigue ocupada hasta que se facture (CU-02).
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.ACTUALIZADO, dto));
        eventos.publishEvent(new Eventos.MesaCambiada(aDto(pedido.getMesa(), pedido)));
        eventos.publishEvent(notificacion(pedido, TipoNotificacion.ATENDIDA));
        return dto;
    }

    /** Curso alterno 6a: reenvía la alerta de los pedidos listos que no se han servido. */
    @Transactional
    public int reenviarAlertasVencidas(java.time.LocalDateTime limite) {
        List<Pedido> vencidos = pedidos.findByEstadoAndNotificadoEnBefore(EstadoPedido.LISTO_PARA_SERVIR, limite);
        vencidos.forEach(p -> {
            p.registrarRecordatorio();
            eventos.publishEvent(notificacion(p, TipoNotificacion.RECORDATORIO));
        });
        return vencidos.size();
    }

    // ------------------------------------------------------------------
    // F-13 · Anular (versión provisional; ver README)
    // ------------------------------------------------------------------

    /**
     * Anula un pedido que aún no entra a preparación: lo retira de cocina, reintegra
     * los ingredientes y libera la mesa. Queda auditado (usuario, fecha y motivo).
     */
    @Transactional
    public PedidoDto anular(Long pedidoId, String motivo, Long usuarioId, Rol rol) {
        Pedido pedido = cargar(pedidoId);
        if (rol == Rol.MESERO && !pedido.getMesero().getId().equals(usuarioId)) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "NO_ES_SU_PEDIDO",
                    "Solo el mesero que tomó el pedido o un administrador pueden anularlo");
        }
        if (pedido.getEstado() != EstadoPedido.REGISTRADO) {
            throw NegocioException.conflicto("NO_ANULABLE",
                    "Solo se puede anular un pedido que la cocina no ha empezado a preparar");
        }
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_EXISTE", "Usuario no encontrado"));

        // Mismo orden de bloqueo que crearPedido: mesa → ingredientes
        Mesa mesa = mesas.findByIdParaActualizar(pedido.getMesa().getId()).orElseThrow();

        Map<Long, ProductoMenu> porId = new LinkedHashMap<>();
        Map<Long, Integer> unidades = new LinkedHashMap<>();
        pedido.getDetalles().forEach(d -> {
            porId.put(d.getProducto().getId(), d.getProducto());
            unidades.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum);
        });
        List<Long> idsIngredientes = productos.idsIngredientesDeProductos(unidades.keySet());
        Map<Long, Ingrediente> bloqueados = idsIngredientes.isEmpty() ? Map.of()
                : ingredientes.bloquearPorIds(idsIngredientes).stream()
                        .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));
        Map<Long, ProductoMenu> conReceta = productos.findConRecetaPorIds(porId.keySet()).stream()
                .collect(Collectors.toMap(ProductoMenu::getId, Function.identity()));

        CalculadoraReceta.requerimientos(conReceta, unidades).forEach((ingredienteId, cantidad) -> {
            Ingrediente ing = bloqueados.get(ingredienteId);
            ing.sumar(cantidad);
            movimientos.save(new MovimientoInventario(ing, TipoMovimiento.REINTEGRO, cantidad, pedido.getId(),
                    usuarioId, "Anulación pedido #" + pedido.getId()));
        });

        pedido.anular(usuario, motivo.trim());
        mesa.setEstado(EstadoMesa.DISPONIBLE);

        PedidoDto dto = PedidoDto.de(pedido);
        eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.ANULADO, dto));
        eventos.publishEvent(new Eventos.MesaCambiada(aDto(mesa, null)));
        eventos.publishEvent(new Eventos.MenuActualizado());
        return dto;
    }

    // ------------------------------------------------------------------

    private Pedido cargar(Long pedidoId) {
        return pedidos.buscarCompleto(pedidoId)
                .orElseThrow(() -> NegocioException.noEncontrado("PEDIDO_NO_EXISTE",
                        "No existe el pedido " + pedidoId));
    }

    private static MesaDto aDto(Mesa mesa, Pedido activo) {
        return new MesaDto(mesa.getId(), mesa.getNumero(), mesa.getCapacidad(), mesa.getEstado(),
                activo == null ? null : activo.getId(),
                activo == null ? null : activo.getEstado());
    }

    private static Eventos.NotificacionMesero notificacion(Pedido p, TipoNotificacion tipo) {
        List<String> platos = p.getDetalles().stream()
                .sorted(Comparator.comparing(DetallePedido::getId))
                .map(d -> d.getCantidad() + "x " + d.getProducto().getNombre())
                .toList();
        return new Eventos.NotificacionMesero(p.getMesero().getId(), tipo, p.getId(), p.getMesa().getNumero(),
                platos, p.getNotificadoEn());
    }
}
