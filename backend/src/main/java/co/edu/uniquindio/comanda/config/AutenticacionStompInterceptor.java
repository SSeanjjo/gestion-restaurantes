package co.edu.uniquindio.comanda.config;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.comanda.auth.UsuarioSesion;

/**
 * Seguridad del canal STOMP (IS-04, RNF-04).
 *
 * <ul>
 * <li>CONNECT: exige el encabezado {@code Authorization: Bearer <jwt>} y asocia el usuario a la sesión.</li>
 * <li>SUBSCRIBE: solo permite los tópicos autorizados para el rol (ver {@link PoliticaSuscripcion}).</li>
 * </ul>
 *
 * Patrón <b>Chain of Responsibility</b>: el interceptor es un eslabón de la cadena de
 * procesamiento de mensajes entrantes del broker.
 */
@Component
public class AutenticacionStompInterceptor implements ChannelInterceptor {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public AutenticacionStompInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(autenticar(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            autorizarSuscripcion(accessor.getUser(), accessor.getDestination());
        }
        return message;
    }

    private UsuarioSesion autenticar(String encabezado) {
        if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
            throw new MessagingException("Falta el token de sesión en la conexión WebSocket");
        }
        try {
            return UsuarioSesion.de(jwtDecoder.decode(encabezado.substring(PREFIJO_BEARER.length())));
        } catch (JwtException e) {
            throw new MessagingException("Token de sesión inválido o vencido");
        }
    }

    private static void autorizarSuscripcion(Principal usuario, String destino) {
        UsuarioSesion sesion = usuario instanceof UsuarioSesion u ? u : null;
        if (!PoliticaSuscripcion.permitido(sesion, destino)) {
            throw new MessagingException("No tiene permiso para suscribirse a " + destino);
        }
    }
}
