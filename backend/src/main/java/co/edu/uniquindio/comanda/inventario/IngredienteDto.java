package co.edu.uniquindio.comanda.inventario;

import java.math.BigDecimal;

public record IngredienteDto(Long id, String nombre, String unidad, BigDecimal stock, BigDecimal stockMinimo,
        boolean enNivelMinimo) {

    public static IngredienteDto de(Ingrediente i) {
        return new IngredienteDto(i.getId(), i.getNombre(), i.getUnidad(), i.getStock(), i.getStockMinimo(),
                i.enNivelMinimo());
    }
}
