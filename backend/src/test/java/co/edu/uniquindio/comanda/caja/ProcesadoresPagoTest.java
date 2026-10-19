package co.edu.uniquindio.comanda.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.comanda.caja.pago.PagoEfectivo;
import co.edu.uniquindio.comanda.caja.pago.PagoTarjeta;
import co.edu.uniquindio.comanda.caja.pago.PasarelaPagoSimulada;
import co.edu.uniquindio.comanda.caja.pago.SolicitudPago;
import co.edu.uniquindio.comanda.common.NegocioException;

/** Estrategias de pago (RF-08, RF-09, curso alterno 5a). */
class ProcesadoresPagoTest {

    private final PagoEfectivo efectivo = new PagoEfectivo();
    private final PagoTarjeta tarjeta = new PagoTarjeta(new PasarelaPagoSimulada());

    @Test
    void efectivoCalculaElCambio() {
        Pago p = efectivo.procesar(new SolicitudPago(MetodoPago.EFECTIVO, new BigDecimal("28000"),
                new BigDecimal("50000"), null), "PED-1");
        assertThat(p.getCambio()).isEqualByComparingTo("22000");
    }

    @Test
    void efectivoInsuficienteSeRechaza() {
        assertThatThrownBy(() -> efectivo.procesar(new SolicitudPago(MetodoPago.EFECTIVO, new BigDecimal("28000"),
                new BigDecimal("20000"), null), "PED-1"))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("EFECTIVO_INSUFICIENTE");
    }

    @Test
    void tarjetaAprobadaGuardaLaAutorizacion() {
        Pago p = tarjeta.procesar(new SolicitudPago(MetodoPago.TARJETA, new BigDecimal("28000"), null, "4242"),
                "PED-1");
        assertThat(p.getAutorizacion()).startsWith("AUT-");
        assertThat(p.getUltimosDigitos()).isEqualTo("4242");
    }

    @Test
    void tarjetaRechazadaPorElBanco() {
        assertThatThrownBy(() -> tarjeta.procesar(new SolicitudPago(MetodoPago.TARJETA, new BigDecimal("28000"),
                null, "0000"), "PED-1"))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("PAGO_RECHAZADO");
    }
}
