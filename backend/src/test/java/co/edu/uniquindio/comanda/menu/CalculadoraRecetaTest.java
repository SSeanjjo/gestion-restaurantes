package co.edu.uniquindio.comanda.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.uniquindio.comanda.inventario.Ingrediente;

/** Verificación de F-02 y F-06: disponibilidad y consumo calculados desde las recetas. */
class CalculadoraRecetaTest {

    @Test
    void platoDisponibleSoloSiAlcanzaCadaIngrediente() {
        Ingrediente arroz = ingrediente(1L, "Arroz", "1000");
        Ingrediente pollo = ingrediente(2L, "Pollo", "100");
        ProductoMenu arrozConPollo = producto(10L, linea(arroz, "200"), linea(pollo, "150"));

        assertThat(CalculadoraReceta.disponible(arrozConPollo)).isFalse();

        ReflectionTestUtils.setField(pollo, "stock", new BigDecimal("150"));
        assertThat(CalculadoraReceta.disponible(arrozConPollo)).isTrue();
    }

    @Test
    void requerimientosMultiplicanPorUnidadesYSumanEntrePlatos() {
        Ingrediente arroz = ingrediente(1L, "Arroz", "10000");
        ProductoMenu bandeja = producto(10L, linea(arroz, "150"));
        ProductoMenu arrozConLeche = producto(11L, linea(arroz, "60"));

        Map<Long, BigDecimal> requerido = CalculadoraReceta.requerimientos(
                Map.of(10L, bandeja, 11L, arrozConLeche),
                Map.of(10L, 2, 11L, 1));

        // 2 × 150 g + 1 × 60 g
        assertThat(requerido.get(1L)).isEqualByComparingTo("360");
    }

    // --- utilidades ---------------------------------------------------------

    private static Ingrediente ingrediente(Long id, String nombre, String stock) {
        Ingrediente i = BeanUtils.instantiateClass(Ingrediente.class);
        ReflectionTestUtils.setField(i, "id", id);
        ReflectionTestUtils.setField(i, "nombre", nombre);
        ReflectionTestUtils.setField(i, "unidad", "g");
        ReflectionTestUtils.setField(i, "stock", new BigDecimal(stock));
        return i;
    }

    private static RecetaIngrediente linea(Ingrediente ingrediente, String cantidad) {
        RecetaIngrediente r = BeanUtils.instantiateClass(RecetaIngrediente.class);
        ReflectionTestUtils.setField(r, "ingrediente", ingrediente);
        ReflectionTestUtils.setField(r, "cantidad", new BigDecimal(cantidad));
        return r;
    }

    private static ProductoMenu producto(Long id, RecetaIngrediente... lineas) {
        ProductoMenu p = BeanUtils.instantiateClass(ProductoMenu.class);
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "receta", List.of(lineas));
        return p;
    }
}
