package co.edu.uniquindio.comanda.menu;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Reglas puras de disponibilidad basadas en recetas (RF-03, SWR-02, SWR-06).
 * No accede a la base de datos para que se pueda probar de forma unitaria.
 */
public final class CalculadoraReceta {

    private CalculadoraReceta() {
    }

    /** Un plato está disponible si hay stock para preparar al menos una unidad. */
    public static boolean disponible(ProductoMenu producto) {
        return producto.getReceta().stream()
                .allMatch(r -> r.getIngrediente().alcanzaPara(r.getCantidad()));
    }

    /**
     * Cantidad total que consume un pedido de cada ingrediente:
     * suma de (cantidad en receta × unidades pedidas) por ingrediente.
     *
     * @param productos         productos del pedido, por id
     * @param unidadesPorProducto unidades pedidas de cada producto
     * @return mapa idIngrediente → cantidad requerida
     */
    public static Map<Long, BigDecimal> requerimientos(Map<Long, ProductoMenu> productos,
            Map<Long, Integer> unidadesPorProducto) {
        Map<Long, BigDecimal> requerido = new HashMap<>();
        unidadesPorProducto.forEach((productoId, unidades) -> {
            ProductoMenu producto = productos.get(productoId);
            for (RecetaIngrediente linea : producto.getReceta()) {
                BigDecimal cantidad = linea.getCantidad().multiply(BigDecimal.valueOf(unidades));
                requerido.merge(linea.getIngrediente().getId(), cantidad, BigDecimal::add);
            }
        });
        return requerido;
    }
}
