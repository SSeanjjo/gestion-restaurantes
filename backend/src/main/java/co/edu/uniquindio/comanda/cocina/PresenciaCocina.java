package co.edu.uniquindio.comanda.cocina;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import co.edu.uniquindio.comanda.auth.Rol;
import co.edu.uniquindio.comanda.auth.UsuarioSesion;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

/**
 * F-08 · Sabe si hay al menos una pantalla de cocina conectada, observando las suscripciones
 * STOMP a {@code /topic/cocina} de usuarios con rol COCINERO (patrón <b>Observer</b> sobre los
 * eventos de sesión de Spring).
 */
@Component
public class PresenciaCocina {

    static final String TOPICO_COCINA = "/topic/cocina";

    /** Claves "sesión:suscripción" de las pantallas de cocina activas. */
    private final Set<String> suscripciones = ConcurrentHashMap.newKeySet();
    private final ApplicationEventPublisher eventos;

    public PresenciaCocina(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    public boolean conectada() {
        return !suscripciones.isEmpty();
    }

    @EventListener
    public void alSuscribir(SessionSubscribeEvent evento) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(evento.getMessage());
        boolean esCocinero = evento.getUser() instanceof UsuarioSesion u && u.rol() == Rol.COCINERO;
        if (esCocinero && TOPICO_COCINA.equals(h.getDestination())) {
            cambiar(() -> suscripciones.add(clave(h.getSessionId(), h.getSubscriptionId())));
        }
    }

    @EventListener
    public void alCancelar(SessionUnsubscribeEvent evento) {
        StompHeaderAccessor h = StompHeaderAccessor.wrap(evento.getMessage());
        cambiar(() -> suscripciones.remove(clave(h.getSessionId(), h.getSubscriptionId())));
    }

    @EventListener
    public void alDesconectar(SessionDisconnectEvent evento) {
        String prefijo = evento.getSessionId() + ":";
        cambiar(() -> suscripciones.removeIf(s -> s.startsWith(prefijo)));
    }

    private void cambiar(java.util.function.BooleanSupplier operacion) {
        boolean antes = conectada();
        operacion.getAsBoolean();
        if (antes != conectada()) {
            eventos.publishEvent(new Eventos.PresenciaCocinaCambiada(conectada()));
        }
    }

    private static String clave(String sesion, String suscripcion) {
        return sesion + ":" + suscripcion;
    }
}
