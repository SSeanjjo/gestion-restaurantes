package co.edu.uniquindio.comanda.menu;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Estrategia por defecto: compara la receta del plato con el stock actual (RF-03). */
@Component
public class DisponibilidadPorReceta implements ReglaDisponibilidad {

    @Override
    public boolean disponible(ProductoMenu producto) {
        return CalculadoraReceta.disponible(producto);
    }

    @Override
    public Map<Long, BigDecimal> requerimientos(Map<Long, ProductoMenu> productos,
            Map<Long, Integer> unidadesPorProducto) {
        return CalculadoraReceta.requerimientos(productos, unidadesPorProducto);
    }
}
