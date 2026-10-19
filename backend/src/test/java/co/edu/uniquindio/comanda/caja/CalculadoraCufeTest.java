package co.edu.uniquindio.comanda.caja;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.comanda.caja.dian.CalculadoraCufe;

/** RF-07: el CUFE es un SHA-384 determinista de los datos de la factura. */
class CalculadoraCufeTest {

    private static final LocalDateTime EMISION = LocalDateTime.of(2026, 10, 19, 13, 45, 0);

    private static String cufe(String total) {
        return CalculadoraCufe.calcular("FEBS-1", EMISION, new BigDecimal("25925.93"), new BigDecimal("2074.07"),
                new BigDecimal(total), "900123456", "clave", 2);
    }

    @Test
    void tiene96CaracteresHexadecimales() {
        assertThat(cufe("28000")).hasSize(96).matches("[0-9a-f]{96}");
    }

    @Test
    void esDeterministaYCambiaSiCambianLosDatos() {
        assertThat(cufe("28000")).isEqualTo(cufe("28000.00"));
        assertThat(cufe("28000")).isNotEqualTo(cufe("28001"));
    }
}
