package co.edu.uniquindio.comanda.caja.pago;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de pruebas de la pasarela bancaria. Aprueba cualquier tarjeta salvo las que
 * terminan en {@code 0000}, que se rechazan para poder demostrar el curso alterno 5a del CU-02.
 */
@Component
public class PasarelaPagoSimulada implements PasarelaPago {

    private static final Logger log = LoggerFactory.getLogger(PasarelaPagoSimulada.class);
    static final String TARJETA_RECHAZADA = "0000";

    @Override
    public Autorizacion autorizar(BigDecimal monto, String ultimosDigitos, String referencia) {
        if (TARJETA_RECHAZADA.equals(ultimosDigitos)) {
            log.info("Pasarela simulada: rechazo de {} por {}", referencia, monto);
            return new Autorizacion(false, null, "Transacción rechazada por la entidad bancaria");
        }
        String codigo = "AUT-" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        log.info("Pasarela simulada: {} aprobada por {} ({})", referencia, monto, codigo);
        return new Autorizacion(true, codigo, "Aprobada");
    }

    @Override
    public void reversar(String codigoAutorizacion) {
        log.info("Pasarela simulada: reverso de {}", codigoAutorizacion);
    }
}
