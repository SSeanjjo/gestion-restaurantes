package co.edu.uniquindio.comanda.caja.pago;

import java.math.BigDecimal;

/**
 * IS-01 · Puerto hacia la API REST de la pasarela de pago bancaria.
 *
 * Patrón <b>Adapter</b> (arquitectura de puertos y adaptadores): el dominio solo conoce esta
 * interfaz. Hoy la implementa {@link PasarelaPagoSimulada}; integrar un banco real consiste en
 * escribir otro adaptador sin tocar la lógica de cobro.
 */
public interface PasarelaPago {

    record Autorizacion(boolean aprobada, String codigo, String mensaje) {
    }

    Autorizacion autorizar(BigDecimal monto, String ultimosDigitos, String referencia);

    /** Compensación: anula una autorización ya aprobada (si el cobro no se completa). */
    void reversar(String codigoAutorizacion);
}
