package co.edu.uniquindio.comanda.caja.dian;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.comanda.config.ComandaProperties;

/**
 * Adaptador de pruebas del servicio DIAN (ambiente de habilitación). Recalcula el CUFE con los
 * datos recibidos y acepta la factura solo si coincide, igual que la validación real.
 */
@Component
public class DianSimulada implements ServicioFacturacionElectronica {

    private static final Logger log = LoggerFactory.getLogger(DianSimulada.class);

    private final ComandaProperties.Facturacion config;

    public DianSimulada(ComandaProperties props) {
        this.config = props.facturacion();
    }

    @Override
    public Respuesta validarFactura(DocumentoFactura d) {
        String esperado = CalculadoraCufe.calcular(d.numero(), d.emision(), d.base(), d.impuesto(), d.total(),
                config.nitEmisor(), config.claveTecnica(), config.ambiente());
        if (!esperado.equals(d.cufe())) {
            log.warn("DIAN simulada: CUFE inválido para {}", d.numero());
            return new Respuesta(false, "Rechazada: el CUFE no corresponde a los datos de la factura");
        }
        log.info("DIAN simulada: factura {} validada", d.numero());
        return new Respuesta(true, "Documento validado por la DIAN (ambiente " + config.ambiente() + ")");
    }

    @Override
    public String enviarReporteDiario(ReporteDiario reporte) {
        String comprobante = "RD-" + reporte.fecha().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("DIAN simulada: reporte diario {} recibido ({} facturas)", comprobante, reporte.numeroFacturas());
        return comprobante;
    }
}
