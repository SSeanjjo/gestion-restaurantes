package co.edu.uniquindio.comanda.mesa;

import co.edu.uniquindio.comanda.pedido.EstadoPedido;

/**
 * Estado de una mesa para la cuadrícula de IU-01. Incluye el pedido activo, si existe.
 */
public record MesaDto(
        Long id,
        Integer numero,
        Integer capacidad,
        EstadoMesa estado,
        Long pedidoActivoId,
        EstadoPedido estadoPedido) {
}
