package co.edu.uniquindio.comanda.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utilidades para leer el usuario autenticado desde el JWT.
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static Long id(Jwt jwt) {
        Object valor = jwt.getClaim(TokenService.CLAIM_USUARIO_ID);
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        return Long.valueOf(String.valueOf(valor));
    }

    public static Rol rol(Jwt jwt) {
        return Rol.valueOf(jwt.getClaimAsString(TokenService.CLAIM_ROL));
    }
}
