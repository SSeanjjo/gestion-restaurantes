package co.edu.uniquindio.comanda.cocina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.PedidoDto;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoCambioPedido;

/** F-08 · Cola de comandas con confirmación de recepción. */
class ColaComandasTest {

    private final EntregaComandaRepository entregas = mock(EntregaComandaRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final PresenciaCocina presencia = mock(PresenciaCocina.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ColaComandas cola = new ColaComandas(entregas, pedidos, presencia, eventos,
            new ComandaProperties(null, null, null, null, new ComandaProperties.Comandas(5), null));

    private static PedidoDto pedido(long id) {
        return new PedidoDto(id, 1, 1L, "Laura", LocalDateTime.now(), EstadoPedido.REGISTRADO, BigDecimal.TEN,
                null, List.of());
    }

    @Test
    void alCrearseUnPedidoQuedaPendienteDeConfirmar() {
        cola.alCambiarPedido(new Eventos.PedidoCambiado(TipoCambioPedido.CREADO, pedido(9L)));

        ArgumentCaptor<EntregaComanda> guardada = ArgumentCaptor.forClass(EntregaComanda.class);
        verify(entregas).save(guardada.capture());
        assertThat(guardada.getValue().getPedidoId()).isEqualTo(9L);
        assertThat(guardada.getValue().isConfirmada()).isFalse();
    }

    @Test
    void elAckEsIdempotente() {
        EntregaComanda e = new EntregaComanda(9L);
        e.confirmar();
        LocalDateTime primera = e.getConfirmadaEn();
        e.confirmar();
        assertThat(e.getConfirmadaEn()).isEqualTo(primera);
    }

    @Test
    void sinCocinaConectadaNoSeReenvia() {
        when(presencia.conectada()).thenReturn(false);
        cola.reenviarPendientes();
        verify(entregas, never()).findByConfirmadaEnIsNullAndPublicadaEnBefore(any());
    }
}
