package co.edu.uniquindio.comanda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal de eventos en tiempo real (IS-04). STOMP sobre WebSocket nativo, autenticado con JWT.
 *
 * Tópicos (permisos en {@link PoliticaSuscripcion}):
 *   /topic/mesas                         estado de mesas (F-01, CU-02 libera la mesa)
 *   /topic/menu                          disponibilidad del menú cambió (F-02)
 *   /topic/cocina                        comandas creadas / actualizadas / anuladas (F-07, F-09, F-13)
 *   /topic/estado-cocina                 cocina conectada y comandas en cola (F-08)
 *   /topic/meseros/{usuarioId}           notificaciones de plato listo (F-11)
 *   /topic/inventario/alertas            ingredientes en nivel mínimo (F-06)
 *   /topic/caja                          cuentas por cobrar y facturas (CU-02)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ComandaProperties props;
    private final AutenticacionStompInterceptor autenticacion;

    public WebSocketConfig(ComandaProperties props, AutenticacionStompInterceptor autenticacion) {
        this.props = props;
        this.autenticacion = autenticacion;
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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(autenticacion);
    }
}
