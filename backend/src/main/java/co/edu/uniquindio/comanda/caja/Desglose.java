package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * RF-06 · Desglose de la cuenta. Los precios del menú ya incluyen el impuesto al consumo,
 * así que el total a cobrar es la suma de los platos y la base se obtiene descontando la tarifa:
 * {@code base = total / (1 + tarifa)}, {@code impuesto = total − base}.
 *
 * Objeto de valor inmutable y sin dependencias (fácil de probar).
 */
public record Desglose(BigDecimal base, BigDecimal impuesto, BigDecimal tarifa, BigDecimal total) {

    public static Desglose de(BigDecimal totalConImpuesto, BigDecimal tarifa) {
        BigDecimal total = totalConImpuesto.setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = total.divide(BigDecimal.ONE.add(tarifa), 2, RoundingMode.HALF_UP);
        return new Desglose(base, total.subtract(base), tarifa, total);
    }
}
