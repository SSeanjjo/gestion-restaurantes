package co.edu.uniquindio.comanda.caja.pago;

import co.edu.uniquindio.comanda.caja.MetodoPago;
import co.edu.uniquindio.comanda.caja.Pago;

/**
 * Patrón <b>Strategy</b>: cada método de pago tiene su propio procesador. El servicio de caja
 * elige la estrategia por {@link #metodo()} en lugar de usar un {@code switch}
 * (refactorización <i>Replace Conditional with Polymorphism</i>). Agregar un método nuevo
 * (p. ej. transferencia) es agregar una clase: principio abierto/cerrado.
 */
public interface ProcesadorPago {

    MetodoPago metodo();

    /**
     * Procesa una parte del pago.
     *
     * @throws co.edu.uniquindio.comanda.common.NegocioException si el pago no es válido o se rechaza
     */
    Pago procesar(SolicitudPago solicitud, String referencia);

    /** Deshace el efecto externo de un pago ya procesado (por defecto no hay nada que deshacer). */
    default void revertir(Pago pago) {
    }
}
