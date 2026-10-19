package co.edu.uniquindio.comanda.tiemporeal;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.uniquindio.comanda.inventario.IngredienteDto;
import co.edu.uniquindio.comanda.mesa.MesaDto;
import co.edu.uniquindio.comanda.pedido.PedidoDto;

/**
 * Eventos de dominio (patrón <b>Observer</b>). Los servicios los publican con
 * {@code ApplicationEventPublisher} sin conocer a sus observadores; {@link PublicadorTiempoReal}
 * los difunde por WebSocket DESPUÉS del commit de la transacción, para no anunciar cambios
 * que luego se reviertan.
 */
public final class Eventos {

    private Eventos() {
    }

    public enum TipoCambioPedido {
        CREADO,
        ACTUALIZADO,
        ANULADO,
        /** F-08: la comanda se reenvía porque la cocina no confirmó su recepción. */
        REENVIADO
    }

    public enum TipoNotificacion {
        /** El plato/pedido acaba de quedar listo. */
        LISTO,
        /** Recordatorio: lleva N minutos listo sin servirse (curso alterno 6a). */
        RECORDATORIO,
        /** El mesero ya lo sirvió; la tablet debe retirar la notificación. */
        ATENDIDA
    }

    /** Payload que recibe la cocina en /topic/cocina. */
    public record PedidoCambiado(TipoCambioPedido tipo, PedidoDto pedido) {
    }

    /** Payload que recibe la tablet en /topic/mesas. */
    public record MesaCambiada(MesaDto mesa) {
    }

    /** Señal para que las tablets vuelvan a pedir el menú (cambió el stock). */
    public record MenuActualizado() {
    }

    /** Payload que recibe el mesero en /topic/meseros/{id}. */
    public record NotificacionMesero(Long meseroId, TipoNotificacion tipo, Long pedidoId, Integer mesaNumero,
            List<String> platos, LocalDateTime fecha) {
    }

    /** Ingredientes que quedaron en su nivel mínimo o por debajo (SWR-06). */
    public record AlertaInventario(List<IngredienteDto> ingredientes) {
    }

    /** F-08: una pantalla de cocina se conectó o se desconectó. */
    public record PresenciaCocinaCambiada(boolean conectada) {
    }

    /** F-08: estado que ve la tablet (cocina conectada y comandas pendientes de confirmar). */
    public record EstadoCocina(boolean conectada, long comandasEnCola) {
    }

    /** CU-02: cambió la lista de cuentas por cobrar (pedido entregado, cuenta solicitada o factura emitida). */
    public record CajaActualizada(String motivo, Long pedidoId) {
    }
}
