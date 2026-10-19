package co.edu.uniquindio.comanda.caja.pago;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.comanda.caja.MetodoPago;
import co.edu.uniquindio.comanda.caja.Pago;
import co.edu.uniquindio.comanda.common.NegocioException;

/** Pago con tarjeta a través de la pasarela bancaria (RF-08, curso alterno 5a). */
@Component
public class PagoTarjeta implements ProcesadorPago {

    private final PasarelaPago pasarela;

    public PagoTarjeta(PasarelaPago pasarela) {
        this.pasarela = pasarela;
    }

    @Override
    public MetodoPago metodo() {
        return MetodoPago.TARJETA;
    }

    @Override
    public Pago procesar(SolicitudPago solicitud, String referencia) {
        if (solicitud.ultimosDigitos() == null) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "TARJETA_REQUERIDA",
                    "Indique los últimos 4 dígitos de la tarjeta");
        }
        PasarelaPago.Autorizacion aut = pasarela.autorizar(solicitud.monto(), solicitud.ultimosDigitos(), referencia);
        if (!aut.aprobada()) {
            throw new NegocioException(HttpStatus.PAYMENT_REQUIRED, "PAGO_RECHAZADO",
                    aut.mensaje() + ". Reintente o cambie el medio de pago.");
        }
        return new Pago(MetodoPago.TARJETA, solicitud.monto(), null, null, aut.codigo(), solicitud.ultimosDigitos());
    }

    @Override
    public void revertir(Pago pago) {
        pasarela.reversar(pago.getAutorizacion());
    }
}
