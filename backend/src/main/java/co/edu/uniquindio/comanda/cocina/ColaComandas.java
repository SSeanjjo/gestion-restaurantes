package co.edu.uniquindio.comanda.cocina;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.PedidoDto;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoCambioPedido;

/**
 * F-08 · Almacenar y reenviar comandas ante pérdida de conexión (SWR-08, modo M-03).
 *
 * <ol>
 * <li>Al crearse un pedido se guarda una {@link EntregaComanda} pendiente en la misma transacción.</li>
 * <li>La pantalla de cocina confirma (ACK) cada comanda que muestra.</li>
 * <li>Cada pocos segundos se reenvían, en orden de confirmación, las que siguen sin ACK.
 * La cocina descarta duplicados por {@code idPedido} (idempotencia).</li>
 * </ol>
 *
 * Desacoplado del módulo de pedidos mediante eventos (patrón <b>Observer</b>): el servicio de
 * pedidos no sabe que existe esta cola.
 */
@Service
public class ColaComandas {

    private static final Logger log = LoggerFactory.getLogger(ColaComandas.class);

    private final EntregaComandaRepository entregas;
    private final PedidoRepository pedidos;
    private final PresenciaCocina presencia;
    private final ApplicationEventPublisher eventos;
    private final ComandaProperties props;

    public ColaComandas(EntregaComandaRepository entregas, PedidoRepository pedidos, PresenciaCocina presencia,
            ApplicationEventPublisher eventos, ComandaProperties props) {
        this.entregas = entregas;
        this.pedidos = pedidos;
        this.presencia = presencia;
        this.eventos = eventos;
        this.props = props;
    }

    /** Se ejecuta dentro de la transacción del pedido (listener síncrono). */
    @EventListener
    public void alCambiarPedido(Eventos.PedidoCambiado evento) {
        Long pedidoId = evento.pedido().id();
        switch (evento.tipo()) {
            case CREADO -> entregas.save(new EntregaComanda(pedidoId));
            // Si la cocina ya actuó sobre el pedido (o se anuló) no tiene sentido reenviarlo
            case ACTUALIZADO, ANULADO -> confirmarSinPublicar(List.of(pedidoId));
            case REENVIADO -> {
                // lo genera esta misma clase
            }
        }
        publicarEstado();
    }

    @EventListener
    public void alCambiarPresencia(Eventos.PresenciaCocinaCambiada evento) {
        publicarEstado();
    }

    /** ACK de la pantalla de cocina. Idempotente. */
    @Transactional
    public Eventos.EstadoCocina confirmar(Collection<Long> pedidoIds) {
        confirmarSinPublicar(pedidoIds);
        return publicarEstado();
    }

    @Transactional(readOnly = true)
    public Eventos.EstadoCocina estado() {
        return new Eventos.EstadoCocina(presencia.conectada(), entregas.countByConfirmadaEnIsNull());
    }

    /** Reenvía las comandas sin ACK cuya última publicación es más antigua que el umbral. */
    @Scheduled(fixedDelay = 5_000, initialDelay = 10_000)
    @Transactional
    public void reenviarPendientes() {
        if (!presencia.conectada()) {
            return; // se reenviarán cuando la cocina vuelva: al reconectarse recarga y confirma todo
        }
        LocalDateTime limite = LocalDateTime.now().minusSeconds(props.comandas().segundosReenvio());
        List<EntregaComanda> vencidas = entregas.findByConfirmadaEnIsNullAndPublicadaEnBefore(limite);
        if (vencidas.isEmpty()) {
            return;
        }
        List<Long> ids = vencidas.stream().map(EntregaComanda::getPedidoId).toList();
        pedidos.buscarCompletosPorIds(ids).forEach(p -> {
            if (EstadoPedido.EN_COCINA.contains(p.getEstado())) {
                eventos.publishEvent(new Eventos.PedidoCambiado(TipoCambioPedido.REENVIADO, PedidoDto.de(p)));
            }
        });
        vencidas.forEach(EntregaComanda::registrarReenvio);
        log.info("Reenviadas {} comandas sin confirmación de cocina", vencidas.size());
    }

    private void confirmarSinPublicar(Collection<Long> pedidoIds) {
        if (!pedidoIds.isEmpty()) {
            entregas.findByPedidoIdInAndConfirmadaEnIsNull(pedidoIds).forEach(EntregaComanda::confirmar);
        }
    }

    private Eventos.EstadoCocina publicarEstado() {
        entregas.flush();
        Eventos.EstadoCocina estado = estado();
        eventos.publishEvent(estado);
        return estado;
    }
}
