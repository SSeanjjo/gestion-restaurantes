package co.edu.uniquindio.comanda.caja;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/** RF-10 / RF-11 · Conciliación del cierre de caja. */
class ResumenJornadaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    @Test
    void totalizaPorMetodoDePagoYCuadra() {
        List<Factura> facturas = List.of(
                Fixtures.factura("FEBS-1", "28000", MetodoPago.EFECTIVO),
                Fixtures.factura("FEBS-2", "12000", MetodoPago.TARJETA));

        ResumenJornada r = ResumenJornada.calcular(HOY, facturas, 0, new BigDecimal("28000"));

        assertThat(r.numeroFacturas()).isEqualTo(2);
        assertThat(r.numeroTransacciones()).isEqualTo(2);
        assertThat(r.totalVentas()).isEqualByComparingTo("40000");
        assertThat(r.totalEfectivo()).isEqualByComparingTo("28000");
        assertThat(r.totalTarjeta()).isEqualByComparingTo("12000");
        assertThat(r.cuadra()).isTrue();
    }

    @Test
    void detectaFacturaSinCufePedidosAbiertosYDescuadreDeEfectivo() {
        Factura sinCufe = Fixtures.factura("FEBS-3", "5000", MetodoPago.EFECTIVO);
        sinCufe.numerar("FEBS-3", null);

        ResumenJornada r = ResumenJornada.calcular(HOY, List.of(sinCufe), 2, new BigDecimal("4000"));

        assertThat(r.cuadra()).isFalse();
        assertThat(r.inconsistencias()).hasSize(3)
                .anyMatch(s -> s.contains("CUFE"))
                .anyMatch(s -> s.contains("2 pedido"))
                .anyMatch(s -> s.contains("efectivo contado"));
    }
}
