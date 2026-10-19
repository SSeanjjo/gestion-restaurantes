package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.mesa.Mesa;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;

/** Objetos de prueba construidos sin base de datos. */
final class Fixtures {

    private Fixtures() {
    }

    static Mesa mesa(long id, int numero) {
        Mesa m = BeanUtils.instantiateClass(Mesa.class);
        ReflectionTestUtils.setField(m, "id", id);
        ReflectionTestUtils.setField(m, "numero", numero);
        m.ocupar();
        return m;
    }

    static Usuario usuario(long id) {
        Usuario u = BeanUtils.instantiateClass(Usuario.class);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "nombre", "Usuario " + id);
        return u;
    }

    static Pedido pedido(long id, EstadoPedido estado, String total, Mesa mesa) {
        Pedido p = BeanUtils.instantiateClass(Pedido.class);
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "estado", estado);
        ReflectionTestUtils.setField(p, "total", new BigDecimal(total));
        ReflectionTestUtils.setField(p, "mesa", mesa);
        return p;
    }

    /** Factura validada con un único pago por el total. */
    static Factura factura(String numero, String total, MetodoPago metodo) {
        Pedido pedido = pedido(1L, EstadoPedido.FACTURADO, total, mesa(1L, 1));
        Factura f = Factura.builder()
                .pedido(pedido)
                .cajero(usuario(4L))
                .desglose(Desglose.de(new BigDecimal(total), new BigDecimal("0.08")))
                .pago(new Pago(metodo, new BigDecimal(total), null, null, null, null))
                .build();
        f.numerar(numero, "a".repeat(96));
        f.registrarValidacion(EstadoFactura.ACEPTADA, "ok");
        return f;
    }
}
