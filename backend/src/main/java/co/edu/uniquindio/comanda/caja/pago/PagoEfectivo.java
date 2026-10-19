package co.edu.uniquindio.comanda.caja.pago;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.comanda.caja.MetodoPago;
import co.edu.uniquindio.comanda.caja.Pago;
import co.edu.uniquindio.comanda.common.NegocioException;

/** Pago en efectivo: valida el dinero recibido y calcula el cambio (IU-03). */
@Component
public class PagoEfectivo implements ProcesadorPago {

    @Override
    public MetodoPago metodo() {
        return MetodoPago.EFECTIVO;
    }

    @Override
    public Pago procesar(SolicitudPago solicitud, String referencia) {
        BigDecimal recibido = solicitud.montoRecibido() == null ? solicitud.monto() : solicitud.montoRecibido();
        if (recibido.compareTo(solicitud.monto()) < 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "EFECTIVO_INSUFICIENTE",
                    "El efectivo recibido es menor que el monto a pagar");
        }
        return new Pago(MetodoPago.EFECTIVO, solicitud.monto(), recibido, recibido.subtract(solicitud.monto()),
                null, null);
    }
}
