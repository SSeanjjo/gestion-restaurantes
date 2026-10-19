package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.uniquindio.comanda.caja.pago.SolicitudPago;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;
import co.edu.uniquindio.comanda.pedido.PedidoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Objetos de transferencia del módulo de caja (IU-03 e IU-04). */
public final class CajaDtos {

    private CajaDtos() {
    }

    /** Fila del panel izquierdo de IU-03: mesa con cuenta abierta. */
    public record CuentaDto(Long pedidoId, Integer mesaNumero, String meseroNombre, EstadoPedido estado,
            boolean cuentaSolicitada, boolean cobrable, int platos, BigDecimal total, LocalDateTime fechaHora) {

        public static CuentaDto de(Pedido p) {
            int platos = p.getDetalles().stream().mapToInt(d -> d.getCantidad()).sum();
            return new CuentaDto(p.getId(), p.getMesa().getNumero(), p.getMesero().getNombre(), p.getEstado(),
                    p.getCuentaSolicitadaEn() != null, p.esCobrable(), platos, p.getTotal(), p.getFechaHora());
        }
    }

    public record DesgloseDto(BigDecimal base, BigDecimal impuesto, BigDecimal tarifa, String nombreImpuesto,
            BigDecimal total) {

        public static DesgloseDto de(Desglose d, String nombreImpuesto) {
            return new DesgloseDto(d.base(), d.impuesto(), d.tarifa(), nombreImpuesto, d.total());
        }
    }

    /** Panel derecho de IU-03: desglose de la cuenta seleccionada. */
    public record DetalleCuentaDto(CuentaDto cuenta, List<PedidoDto.DetalleDto> items, DesgloseDto desglose) {
    }

    /** Solicitud de cobro: una o varias partes que deben sumar el total. */
    public record SolicitudCobro(@NotEmpty @Size(max = 4) List<@Valid SolicitudPago> pagos) {
    }

    public record PagoDto(MetodoPago metodo, BigDecimal monto, BigDecimal montoRecibido, BigDecimal cambio,
            String autorizacion, String ultimosDigitos) {

        static PagoDto de(Pago p) {
            return new PagoDto(p.getMetodo(), p.getMonto(), p.getMontoRecibido(), p.getCambio(), p.getAutorizacion(),
                    p.getUltimosDigitos());
        }
    }

    public record FacturaDto(Long id, String numero, String cufe, EstadoFactura estadoDian, String mensajeDian,
            Long pedidoId, Integer mesaNumero, LocalDateTime fechaEmision, BigDecimal subtotal, BigDecimal impuesto,
            BigDecimal total, List<PagoDto> pagos) {

        public static FacturaDto de(Factura f) {
            return new FacturaDto(f.getId(), f.getNumero(), f.getCufe(), f.getEstadoDian(), f.getMensajeDian(),
                    f.getPedido().getId(), f.getPedido().getMesa().getNumero(), f.getFechaEmision(), f.getSubtotal(),
                    f.getImpuesto(), f.getTotal(), f.getPagos().stream().map(PagoDto::de).toList());
        }
    }

    /** Solicitud de cierre (RF-10, RF-11). */
    public record SolicitudCierre(
            @DecimalMin(value = "0.00") BigDecimal efectivoContado,
            @Size(max = 255) String observacion,
            boolean confirmarInconsistencias) {
    }

    public record CierreDto(Long id, LocalDate fecha, String ejecutadoPor, LocalDateTime ejecutadoEn,
            int numeroFacturas, int numeroTransacciones, BigDecimal totalVentas, BigDecimal totalEfectivo,
            BigDecimal totalTarjeta, BigDecimal totalImpuesto, BigDecimal efectivoContado, String inconsistencias,
            String observacion, String comprobanteDian) {

        public static CierreDto de(CierreCaja c) {
            return new CierreDto(c.getId(), c.getFecha(), c.getEjecutadoPor().getNombre(), c.getEjecutadoEn(),
                    c.getNumeroFacturas(), c.getNumeroTransacciones(), c.getTotalVentas(), c.getTotalEfectivo(),
                    c.getTotalTarjeta(), c.getTotalImpuesto(), c.getEfectivoContado(), c.getInconsistencias(),
                    c.getObservacion(), c.getComprobanteDian());
        }
    }

    /** Resumen de la jornada para el panel del administrador y la vista previa del cierre. */
    public record ResumenDiaDto(ResumenJornada resumen, boolean cajaCerrada) {
    }
}
