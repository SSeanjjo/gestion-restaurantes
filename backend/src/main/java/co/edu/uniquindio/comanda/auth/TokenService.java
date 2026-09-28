package co.edu.uniquindio.comanda.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import co.edu.uniquindio.comanda.config.ComandaProperties;

/**
 * Emite los JWT de sesión. El token lleva el id, el nombre y el rol del usuario.
 */
@Service
public class TokenService {

    public static final String CLAIM_USUARIO_ID = "uid";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_NOMBRE = "nombre";

    private final JwtEncoder encoder;
    private final ComandaProperties props;

    public TokenService(JwtEncoder encoder, ComandaProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("comanda")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(props.jwt().horasValidez(), ChronoUnit.HOURS))
                .subject(usuario.getUsername())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_NOMBRE, usuario.getNombre())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
