package co.edu.uniquindio.comanda.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.uniquindio.comanda.auth.Rol;
import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.auth.UsuarioRepository;
import co.edu.uniquindio.comanda.common.JornadaOperativa;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.mesa.EstadoMesa;
import co.edu.uniquindio.comanda.mesa.Mesa;
import co.edu.uniquindio.comanda.mesa.MesaRepository;
import co.edu.uniquindio.comanda.mesa.MesaService;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

/**
 * F-13 · Anulación de un pedido. Antes de la refactorización H-02 esta operación no tenía pruebas;
 * fija el orden de los eventos que reciben las pantallas y el estado final de la mesa.
 */
class PedidoServiceAnularTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final MesaRepository mesas = mock(MesaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ConsumoIngredientesPedido consumo = mock(ConsumoIngredientesPedido.class);
    private final JornadaOperativa jornada = mock(JornadaOperativa.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);

    private PedidoService servicio;
    private Mesa mesa;
    private Pedido pedido;

    @BeforeEach
    void preparar() {
        servicio = new PedidoService(pedidos, mesas, new MesaService(mesas, pedidos, eventos), usuarios, consumo,
                jornada, eventos);
        Usuario mesero = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(mesero, "id", 2L);
        ReflectionTestUtils.setField(mesero, "nombre", "Mesero 2");
        mesa = BeanUtils.instantiateClass(Mesa.class);
        ReflectionTestUtils.setField(mesa, "id", 5L);
        ReflectionTestUtils.setField(mesa, "numero", 5);
        mesa.ocupar();
        pedido = BeanUtils.instantiateClass(Pedido.class);
        ReflectionTestUtils.setField(pedido, "id", 20L);
        ReflectionTestUtils.setField(pedido, "estado", EstadoPedido.REGISTRADO);
        ReflectionTestUtils.setField(pedido, "total", new BigDecimal("28000"));
        ReflectionTestUtils.setField(pedido, "mesa", mesa);
        ReflectionTestUtils.setField(pedido, "mesero", mesero);
        when(pedidos.buscarCompleto(20L)).thenReturn(Optional.of(pedido));
        when(mesas.findByIdParaActualizar(5L)).thenReturn(Optional.of(mesa));
        when(usuarios.findById(2L)).thenReturn(Optional.of(mesero));
    }

    @Test
    void anularLiberaLaMesaYPublicaLosEventosEnElMismoOrden() {
        servicio.anular(20L, "El cliente se fue", 2L, Rol.MESERO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.DISPONIBLE);
        ArgumentCaptor<Object> publicados = ArgumentCaptor.forClass(Object.class);
        verify(eventos, times(4)).publishEvent(publicados.capture());
        assertThat(publicados.getAllValues().get(0)).isInstanceOf(Eventos.PedidoCambiado.class);
        assertThat(publicados.getAllValues().get(1)).isInstanceOf(Eventos.MesaCambiada.class);
        assertThat(publicados.getAllValues().get(2)).isInstanceOf(Eventos.MenuActualizado.class);
        assertThat(publicados.getAllValues().get(3)).isInstanceOf(Eventos.CajaActualizada.class);
    }

    @Test
    void unMeseroNoPuedeAnularElPedidoDeOtro() {
        assertThatThrownBy(() -> servicio.anular(20L, "x", 9L, Rol.MESERO))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("NO_ES_SU_PEDIDO");
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.OCUPADA);
        verify(eventos, never()).publishEvent(org.mockito.ArgumentMatchers.<Object>any());
    }
}
