package co.edu.uniquindio.comanda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal de eventos en tiempo real (IS-04). STOMP sobre WebSocket nativo.
 *
 * Tópicos:
 *   /topic/mesas                         estado de mesas (F-01)
 *   /topic/menu                          disponibilidad del menú cambió (F-02)
 *   /topic/cocina                        comandas creadas / actualizadas / anuladas (F-07, F-09, F-13)
 *   /topic/meseros/{usuarioId}           notificaciones de plato listo (F-11)
 *   /topic/inventario/alertas            ingredientes en nivel mínimo (F-06)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ComandaProperties props;

    public WebSocketConfig(ComandaProperties props) {
        this.props = props;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(props.cors().origenes().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
