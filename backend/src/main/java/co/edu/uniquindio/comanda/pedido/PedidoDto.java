package co.edu.uniquindio.comanda.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDto(
        Long id,
        Integer mesaNumero,
        Long meseroId,
        String meseroNombre,
        LocalDateTime fechaHora,
        EstadoPedido estado,
        BigDecimal total,
        LocalDateTime cuentaSolicitadaEn,
        List<DetalleDto> detalles) {

    public record DetalleDto(Long id, Long productoId, String producto, Integer cantidad, BigDecimal precioUnitario,
            BigDecimal subtotal, boolean preparado) {
    }

    /** Debe llamarse dentro de una transacción (las asociaciones son perezosas). */
    public static PedidoDto de(Pedido p) {
        return new PedidoDto(
                p.getId(),
                p.getMesa().getNumero(),
                p.getMesero().getId(),
                p.getMesero().getNombre(),
                p.getFechaHora(),
                p.getEstado(),
                p.getTotal(),
                p.getCuentaSolicitadaEn(),
                p.getDetalles().stream()
                        .map(d -> new DetalleDto(d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal(), d.isPreparado()))
                        .toList());
    }
}
