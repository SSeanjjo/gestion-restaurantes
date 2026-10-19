package co.edu.uniquindio.comanda.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades propias de la aplicación (prefijo {@code comanda} en application.yml).
 */
@ConfigurationProperties(prefix = "comanda")
public record ComandaProperties(
        Restaurante restaurante,
        Jwt jwt,
        Cors cors,
        Notificaciones notificaciones,
        Comandas comandas,
        Facturacion facturacion) {

    public record Restaurante(String nombre) {
    }

    public record Jwt(String secreto, int horasValidez) {
    }

    public record Cors(List<String> origenes) {
    }

    public record Notificaciones(int minutosReenvio) {
    }

    /** F-08: reenvío de comandas sin confirmación de recepción. */
    public record Comandas(int segundosReenvio) {
    }

    /** CU-02: datos para la factura electrónica (simulación del servicio DIAN). */
    public record Facturacion(String nitEmisor, String prefijo, BigDecimal tarifaImpuesto, String nombreImpuesto,
            String claveTecnica, int ambiente) {
    }
}
