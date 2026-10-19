package co.edu.uniquindio.comanda.pedido;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PedidoRequests {

    private PedidoRequests() {
    }

    /**
     * Contrato 1: crearPedido(numeroMesa, listaItems).
     * {@code claveIdempotencia} (opcional, UUID generado por la tablet) evita duplicados si la
     * tablet reintenta el envío tras perder la conexión (F-08).
     */
    public record CrearPedido(
            @NotNull Integer numeroMesa,
            @NotEmpty(message = "El pedido debe tener al menos un plato") List<@Valid Item> items,
            @Size(max = 36) String claveIdempotencia) {
    }

    public record Item(@NotNull Long productoId, @Min(1) int cantidad) {
    }

    /** Contrato 2: actualizarEstado(idPedido, nuevoEstado). */
    public record CambiarEstado(@NotNull EstadoPedido nuevoEstado) {
    }

    public record MarcarItem(boolean preparado) {
    }

    public record Anular(@NotBlank @Size(max = 255) String motivo) {
    }

    /** F-08: la pantalla de cocina confirma que ya muestra estas comandas. */
    public record ConfirmarRecepcion(@NotNull List<Long> pedidoIds) {
    }

    /** Plato no disponible al confirmar (F-05). */
    public record PlatoNoDisponible(Long productoId, String nombre) {
    }
}
