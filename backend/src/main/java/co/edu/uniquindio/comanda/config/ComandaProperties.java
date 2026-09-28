package co.edu.uniquindio.comanda.config;

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
        Notificaciones notificaciones) {

    public record Restaurante(String nombre) {
    }

    public record Jwt(String secreto, int horasValidez) {
    }

    public record Cors(List<String> origenes) {
    }

    public record Notificaciones(int minutosReenvio) {
    }
}
