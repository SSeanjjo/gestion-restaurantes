package co.edu.uniquindio.comanda.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verificación de F-10 / RD-03: la máquina de estados no permite saltos ni retrocesos. */
class EstadoPedidoTest {

    @Test
    void aceptaLaSecuenciaCompleta() {
        assertThat(EstadoPedido.REGISTRADO.puedePasarA(EstadoPedido.EN_PREPARACION)).isTrue();
        assertThat(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.LISTO_PARA_SERVIR)).isTrue();
        assertThat(EstadoPedido.LISTO_PARA_SERVIR.puedePasarA(EstadoPedido.ENTREGADO)).isTrue();
        assertThat(EstadoPedido.ENTREGADO.puedePasarA(EstadoPedido.FACTURADO)).isTrue();
    }

    @Test
    void rechazaSaltos() {
        assertThat(EstadoPedido.REGISTRADO.puedePasarA(EstadoPedido.LISTO_PARA_SERVIR)).isFalse();
        assertThat(EstadoPedido.REGISTRADO.puedePasarA(EstadoPedido.ENTREGADO)).isFalse();
    }

    @Test
    void rechazaRetrocesos() {
        assertThat(EstadoPedido.LISTO_PARA_SERVIR.puedePasarA(EstadoPedido.EN_PREPARACION)).isFalse();
        assertThat(EstadoPedido.ENTREGADO.puedePasarA(EstadoPedido.REGISTRADO)).isFalse();
    }

    @Test
    void soloSeAnulaDesdeRegistrado() {
        assertThat(EstadoPedido.REGISTRADO.puedePasarA(EstadoPedido.CANCELADO)).isTrue();
        assertThat(EstadoPedido.EN_PREPARACION.puedePasarA(EstadoPedido.CANCELADO)).isFalse();
        assertThat(EstadoPedido.FACTURADO.puedePasarA(EstadoPedido.CANCELADO)).isFalse();
    }
}
