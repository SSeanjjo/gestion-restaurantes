package co.edu.uniquindio.comanda.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.common.NegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UsuarioDto(Long id, String username, String nombre, Rol rol) {
        static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getUsername(), u.getNombre(), u.getRol());
        }
    }

    public record LoginResponse(String token, UsuarioDto usuario) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = usuarios.findByUsername(req.username().trim())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED,
                        "CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos"));
        return new LoginResponse(tokenService.emitir(usuario), UsuarioDto.de(usuario));
    }

    @GetMapping("/yo")
    public UsuarioDto yo(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.findById(UsuarioActual.id(jwt))
                .map(UsuarioDto::de)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_EXISTE", "El usuario ya no existe"));
    }
}
