package co.edu.uniquindio.comanda.menu;

import java.math.BigDecimal;
import java.util.List;

public final class MenuDtos {

    private MenuDtos() {
    }

    /** Plato tal como lo ve el mesero en IU-01. */
    public record ProductoMenuDto(Long id, String nombre, String descripcion, BigDecimal precio, String categoria,
            boolean disponible) {
    }

    /** Línea del catálogo de recetas. */
    public record LineaRecetaDto(Long ingredienteId, String ingrediente, String unidad, BigDecimal cantidad,
            BigDecimal stockActual) {
    }

    /** Plato con su receta completa (catálogo para el administrador). */
    public record RecetaDto(Long productoId, String producto, String categoria, EstadoProducto estado,
            BigDecimal precio, boolean disponible, List<LineaRecetaDto> ingredientes) {
    }
}
