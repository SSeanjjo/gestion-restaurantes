package co.edu.uniquindio.comanda.caja.dian;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IS-02 · Puerto hacia el servicio web de facturación electrónica de la DIAN (SOAP/HTTPS).
 * Patrón <b>Adapter</b>: la implementación vigente es {@link DianSimulada}.
 */
public interface ServicioFacturacionElectronica {

    /** Datos de la factura que se envían para validación. */
    record DocumentoFactura(String numero, LocalDateTime emision, BigDecimal base, BigDecimal impuesto,
            BigDecimal total, String cufe) {
    }

    record Respuesta(boolean aceptada, String mensaje) {
    }

    /** Reporte consolidado de ventas del día (CU-06, paso 8–10). */
    record ReporteDiario(LocalDate fecha, int numeroFacturas, BigDecimal totalVentas, BigDecimal totalImpuesto) {
    }

    Respuesta validarFactura(DocumentoFactura documento);

    /** @return comprobante de recepción del reporte */
    String enviarReporteDiario(ReporteDiario reporte);
}
