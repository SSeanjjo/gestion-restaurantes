package co.edu.uniquindio.comanda.menu;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Patrón <b>Strategy</b> (y principio de inversión de dependencias): regla con la que se decide
 * si un plato se puede ofrecer y cuántos ingredientes consume un pedido.
 *
 * La implementación vigente es {@link DisponibilidadPorReceta}. Una política distinta
 * (p. ej. reservar un margen de seguridad de stock) se agrega con una nueva clase, sin
 * modificar los servicios que la usan (principio abierto/cerrado).
 */
public interface ReglaDisponibilidad {

    /** ¿Hay ingredientes suficientes para preparar al menos una unidad del plato? (SWR-02) */
    boolean disponible(ProductoMenu producto);

    /** Consumo total de cada ingrediente para las unidades pedidas (SWR-05, SWR-06). */
    Map<Long, BigDecimal> requerimientos(Map<Long, ProductoMenu> productos, Map<Long, Integer> unidadesPorProducto);
}
