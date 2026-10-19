package co.edu.uniquindio.comanda.tiemporeal;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observador que traduce los eventos de dominio a mensajes STOMP (patrón <b>Adapter</b> entre
 * el modelo de eventos de Spring y el broker). Solo publica si la transacción se confirmó
 * (AFTER_COMMIT); {@code fallbackExecution} permite publicar también eventos emitidos fuera
 * de una transacción.
 */
@Component
public class PublicadorTiempoReal {

    private final SimpMessagingTemplate broker;

    public PublicadorTiempoReal(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void pedido(Eventos.PedidoCambiado evento) {
        broker.convertAndSend("/topic/cocina", evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void mesa(Eventos.MesaCambiada evento) {
        broker.convertAndSend("/topic/mesas", evento.mesa());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void menu(Eventos.MenuActualizado evento) {
        broker.convertAndSend("/topic/menu", Map.of("tipo", "MENU_ACTUALIZADO"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notificacion(Eventos.NotificacionMesero evento) {
        broker.convertAndSend("/topic/meseros/" + evento.meseroId(), evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alertaInventario(Eventos.AlertaInventario evento) {
        broker.convertAndSend("/topic/inventario/alertas", evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void estadoCocina(Eventos.EstadoCocina evento) {
        broker.convertAndSend("/topic/estado-cocina", evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void caja(Eventos.CajaActualizada evento) {
        broker.convertAndSend("/topic/caja", evento);
    }
}
