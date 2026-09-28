package co.edu.uniquindio.comanda.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración pública que necesita el frontend (p. ej. el nombre del restaurante
 * en el encabezado). Así el mismo código sirve para cualquier restaurante.
 */
@RestController
public class ConfigController {

    public record ConfigPublica(String nombreRestaurante) {
    }

    private final ComandaProperties props;

    public ConfigController(ComandaProperties props) {
        this.props = props;
    }

    @GetMapping("/api/config")
    public ConfigPublica config() {
        return new ConfigPublica(props.restaurante().nombre());
    }
}
