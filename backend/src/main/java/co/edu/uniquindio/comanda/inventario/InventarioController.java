package co.edu.uniquindio.comanda.inventario;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.auth.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class InventarioController {

    public record EntradaRequest(
            @NotNull Long ingredienteId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal cantidad,
            String observacion) {
    }

    private final InventarioService inventario;

    public InventarioController(InventarioService inventario) {
        this.inventario = inventario;
    }

    @GetMapping
    public List<IngredienteDto> listar() {
        return inventario.listar();
    }

    /** Suma stock a un ingrediente (llegada de mercancía). */
    @PostMapping("/entradas")
    public IngredienteDto registrarEntrada(@Valid @RequestBody EntradaRequest req, @AuthenticationPrincipal Jwt jwt) {
        return inventario.registrarEntrada(req.ingredienteId(), req.cantidad(), req.observacion(),
                UsuarioActual.id(jwt));
    }
}
