package co.edu.uniquindio.comanda.mesa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.uniquindio.comanda.pedido.PedidoRepository;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

/** Hallazgo H-02: la regla «liberar la mesa y avisar a las tablets» vive en un solo lugar. */
class MesaServiceTest {

    private final MesaRepository mesas = mock(MesaRepository.class);
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final MesaService servicio = new MesaService(mesas, pedidos, eventos);

    private Mesa mesa;

    @BeforeEach
    void preparar() {
        mesa = BeanUtils.instantiateClass(Mesa.class);
        ReflectionTestUtils.setField(mesa, "id", 5L);
        ReflectionTestUtils.setField(mesa, "numero", 5);
        mesa.ocupar();
    }

    @Test
    void liberarPorIdBloqueaLaMesaLaDejaDisponibleYPublicaElEvento() {
        when(mesas.findByIdParaActualizar(5L)).thenReturn(Optional.of(mesa));

        servicio.liberar(5L);

        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.DISPONIBLE);
        ArgumentCaptor<Object> publicado = ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(publicado.capture());
        Eventos.MesaCambiada evento = (Eventos.MesaCambiada) publicado.getValue();
        assertThat(evento.mesa().numero()).isEqualTo(5);
        assertThat(evento.mesa().estado()).isEqualTo(EstadoMesa.DISPONIBLE);
        assertThat(evento.mesa().pedidoActivoId()).isNull();
    }

    @Test
    void liberarUnaMesaYaBloqueadaNoVuelveAConsultarLaBaseDeDatos() {
        servicio.liberar(mesa);

        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.DISPONIBLE);
        verify(eventos).publishEvent(any(Eventos.MesaCambiada.class));
    }
}
