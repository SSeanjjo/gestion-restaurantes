package co.edu.uniquindio.comanda.auth;

import java.security.Principal;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Usuario autenticado de una sesión WebSocket (STOMP). Se construye a partir del JWT
 * enviado en la trama CONNECT y queda asociado a la sesión durante toda la conexión.
 */
public record UsuarioSesion(Long id, String username, Rol rol) implements Principal {

    public static UsuarioSesion de(Jwt jwt) {
        return new UsuarioSesion(UsuarioActual.id(jwt), jwt.getSubject(), UsuarioActual.rol(jwt));
    }

    @Override
    public String getName() {
        return username;
    }
}
