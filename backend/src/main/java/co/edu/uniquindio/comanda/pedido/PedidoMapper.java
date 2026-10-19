package co.edu.uniquindio.comanda.pedido;

import java.util.Comparator;
import java.util.List;

import co.edu.uniquindio.comanda.mesa.Mesa;
import co.edu.uniquindio.comanda.mesa.MesaDto;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;
import co.edu.uniquindio.comanda.tiemporeal.Eventos.TipoNotificacion;

/** Conversión de entidades a los DTO que viajan por la API y por WebSocket. */
public final class PedidoMapper {

    private PedidoMapper() {
    }

    public static MesaDto mesa(Mesa mesa, Pedido activo) {
        return new MesaDto(mesa.getId(), mesa.getNumero(), mesa.getCapacidad(), mesa.getEstado(),
                activo == null ? null : activo.getId(),
                activo == null ? null : activo.getEstado());
    }

    public static Eventos.NotificacionMesero notificacion(Pedido p, TipoNotificacion tipo) {
        List<String> platos = p.getDetalles().stream()
                .sorted(Comparator.comparing(DetallePedido::getId))
                .map(d -> d.getCantidad() + "x " + d.getProducto().getNombre())
                .toList();
        return new Eventos.NotificacionMesero(p.getMesero().getId(), tipo, p.getId(), p.getMesa().getNumero(),
                platos, p.getNotificadoEn());
    }
}
