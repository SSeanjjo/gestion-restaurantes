package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.auth.UsuarioRepository;
import co.edu.uniquindio.comanda.caja.CajaDtos.CuentaDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.DesgloseDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.DetalleCuentaDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.FacturaDto;
import co.edu.uniquindio.comanda.caja.pago.ProcesadorPago;
import co.edu.uniquindio.comanda.caja.pago.SolicitudPago;
import co.edu.uniquindio.comanda.common.JornadaOperativa;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.mesa.MesaService;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;
import co.edu.uniquindio.comanda.pedido.PedidoDto;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

/**
 * CU-02 (primera parte) · Cobro de la cuenta y facturación electrónica. <b>Facade</b> del
 * módulo de caja: coordina las estrategias de pago, la emisión de la factura y el pedido.
 */
@Service
public class CajaService {

    private final PedidoRepository pedidos;
    private final MesaService servicioMesas;
    private final UsuarioRepository usuarios;
    private final FacturaRepository facturas;
    private final EmisorFactura emisor;
    private final JornadaOperativa jornada;
    private final ApplicationEventPublisher eventos;
    private final ComandaProperties.Facturacion config;
    private final Map<MetodoPago, ProcesadorPago> procesadores;

    public CajaService(PedidoRepository pedidos, MesaService servicioMesas, UsuarioRepository usuarios,
            FacturaRepository facturas, EmisorFactura emisor, JornadaOperativa jornada,
            ApplicationEventPublisher eventos, ComandaProperties props, List<ProcesadorPago> procesadores) {
        this.pedidos = pedidos;
        this.servicioMesas = servicioMesas;
        this.usuarios = usuarios;
        this.facturas = facturas;
        this.emisor = emisor;
        this.jornada = jornada;
        this.eventos = eventos;
        this.config = props.facturacion();
        this.procesadores = new EnumMap<>(procesadores.stream()
                .collect(Collectors.toMap(ProcesadorPago::metodo, Function.identity())));
    }

    // ------------------------------------------------------------------
    // Consulta de cuentas (IU-03, panel izquierdo y derecho)
    // ------------------------------------------------------------------

    /** Mesas con pedido activo; primero las que pidieron la cuenta, luego por hora de llegada. */
    @Transactional(readOnly = true)
    public List<CuentaDto> cuentasAbiertas() {
        return pedidos.cuentasAbiertas(EstadoPedido.ACTIVOS).stream()
                .map(CuentaDto::de)
                .sorted(Comparator.comparing((CuentaDto c) -> !c.cuentaSolicitada())
                        .thenComparing(c -> !c.cobrable())
                        .thenComparing(CuentaDto::fechaHora))
                .toList();
    }

    /** RF-06: total calculado automáticamente con su desglose de impuesto. */
    @Transactional(readOnly = true)
    public DetalleCuentaDto detalle(Long pedidoId) {
        Pedido pedido = pedidos.buscarCompleto(pedidoId)
                .orElseThrow(() -> NegocioException.noEncontrado("PEDIDO_NO_EXISTE", "No existe el pedido " + pedidoId));
        PedidoDto dto = PedidoDto.de(pedido);
        return new DetalleCuentaDto(CuentaDto.de(pedido), dto.detalles(), desgloseDto(pedido));
    }

    @Transactional(readOnly = true)
    public List<FacturaDto> facturasDelDia(LocalDate fecha) {
        return facturas.deLaJornada(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay()).stream()
                .sorted(Comparator.comparing(Factura::getId).reversed())
                .map(FacturaDto::de)
                .toList();
    }

    // ------------------------------------------------------------------
    // Contrato 3 · registrarFactura(idPedido, metodoPago)
    // ------------------------------------------------------------------

    /**
     * Cobra la cuenta y emite la factura electrónica en una transacción:
     * valida el pedido (RD-05), procesa cada parte del pago con su estrategia, construye la
     * factura, la valida ante la DIAN, pasa el pedido a FACTURADO y libera la mesa.
     * Si algo falla después de aprobar una tarjeta, se reversa la autorización (compensación).
     */
    @Transactional
    public FacturaDto cobrar(Long pedidoId, CajaDtos.SolicitudCobro solicitud, Long cajeroId) {
        jornada.exigirCajaAbierta();
        Usuario cajero = usuarios.findById(cajeroId)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_EXISTE", "Usuario no encontrado"));
        Pedido pedido = bloquearPedidoCobrable(pedidoId);
        Desglose desglose = desglose(pedido);
        validarQueCuadra(solicitud.pagos(), desglose.total());

        List<Pago> pagos = procesarPagos(solicitud.pagos(), "PED-" + pedido.getId());
        try {
            Factura factura = emisor.emitir(pedido, cajero, desglose, pagos);
            cerrarPedido(pedido);
            eventos.publishEvent(new Eventos.CajaActualizada("FACTURA_EMITIDA", pedido.getId()));
            return FacturaDto.de(factura);
        } catch (RuntimeException e) {
            revertir(pagos);
            throw e;
        }
    }

    // ------------------------------------------------------------------

    private Pedido bloquearPedidoCobrable(Long pedidoId) {
        Pedido pedido = pedidos.bloquearPorId(pedidoId)
                .orElseThrow(() -> NegocioException.noEncontrado("PEDIDO_NO_EXISTE", "No existe el pedido " + pedidoId));
        if (facturas.existsByPedidoId(pedidoId)) {
            throw NegocioException.conflicto("YA_FACTURADO", "El pedido ya fue facturado");
        }
        if (!pedido.esCobrable()) {
            throw NegocioException.conflicto("PEDIDO_NO_ENTREGADO",
                    "Solo se puede cobrar un pedido entregado (estado actual: " + pedido.getEstado() + ")");
        }
        return pedido;
    }

    private static void validarQueCuadra(List<SolicitudPago> partes, BigDecimal total) {
        BigDecimal suma = partes.stream().map(SolicitudPago::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(total) != 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "PAGOS_NO_CUADRAN",
                    "La suma de los pagos (" + suma + ") debe ser igual al total de la cuenta (" + total + ")");
        }
    }

    /** Primero las tarjetas (pueden ser rechazadas), luego el efectivo. */
    private List<Pago> procesarPagos(List<SolicitudPago> partes, String referencia) {
        List<Pago> procesados = new ArrayList<>();
        List<SolicitudPago> ordenadas = partes.stream()
                .sorted(Comparator.comparing(p -> p.metodo() == MetodoPago.EFECTIVO))
                .toList();
        try {
            for (SolicitudPago parte : ordenadas) {
                procesados.add(procesador(parte.metodo()).procesar(parte, referencia));
            }
            return procesados;
        } catch (RuntimeException e) {
            revertir(procesados);
            throw e;
        }
    }

    /** Contrato 3 y CU-05 paso 9: el pedido queda FACTURADO y la mesa vuelve a Disponible. */
    private void cerrarPedido(Pedido pedido) {
        pedido.marcarFacturado();
        servicioMesas.liberar(pedido.getMesa().getId());
    }

    private void revertir(List<Pago> pagos) {
        pagos.forEach(p -> procesador(p.getMetodo()).revertir(p));
    }

    private ProcesadorPago procesador(MetodoPago metodo) {
        ProcesadorPago p = procesadores.get(metodo);
        if (p == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "METODO_NO_SOPORTADO",
                    "Método de pago no soportado: " + metodo);
        }
        return p;
    }

    private Desglose desglose(Pedido pedido) {
        return Desglose.de(pedido.getTotal(), config.tarifaImpuesto());
    }

    private DesgloseDto desgloseDto(Pedido pedido) {
        return DesgloseDto.de(desglose(pedido), config.nombreImpuesto());
    }
}
