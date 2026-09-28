package co.edu.uniquindio.comanda.pedido;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.comanda.config.ComandaProperties;

/**
 * Curso alterno 6a del CU-01 / SWR-11: si el mesero no atiende la notificación de
 * plato listo en N minutos (5 por defecto), el sistema reenvía la alerta.
 */
@Component
public class RecordatorioPlatosListos {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioPlatosListos.class);

    private final PedidoService pedidoService;
    private final ComandaProperties props;

    public RecordatorioPlatosListos(PedidoService pedidoService, ComandaProperties props) {
        this.pedidoService = pedidoService;
        this.props = props;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void revisar() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(props.notificaciones().minutosReenvio());
        int reenviadas = pedidoService.reenviarAlertasVencidas(limite);
        if (reenviadas > 0) {
            log.info("Reenviadas {} alertas de platos listos sin servir", reenviadas);
        }
    }
}
