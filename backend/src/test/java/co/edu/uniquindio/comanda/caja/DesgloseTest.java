package co.edu.uniquindio.comanda.caja;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** RF-06: el total es la suma de los platos y el impuesto al consumo va incluido. */
class DesgloseTest {

    @Test
    void separaLaBaseYElImpuestoIncluido() {
        Desglose d = Desglose.de(new BigDecimal("28000"), new BigDecimal("0.08"));

        assertThat(d.total()).isEqualByComparingTo("28000.00");
        assertThat(d.base()).isEqualByComparingTo("25925.93");
        assertThat(d.impuesto()).isEqualByComparingTo("2074.07");
        assertThat(d.base().add(d.impuesto())).isEqualByComparingTo(d.total());
    }

    @Test
    void conTarifaCeroNoHayImpuesto() {
        Desglose d = Desglose.de(new BigDecimal("12000"), BigDecimal.ZERO);
        assertThat(d.impuesto()).isEqualByComparingTo("0");
        assertThat(d.base()).isEqualByComparingTo("12000");
    }
}
