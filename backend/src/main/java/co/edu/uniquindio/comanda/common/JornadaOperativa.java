package co.edu.uniquindio.comanda.common;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;

/**
 * Abstracción del estado de la jornada (modo M-02 / estado E-08 del SyRS).
 *
 * Principio de <b>inversión de dependencias</b>: el módulo de pedidos pregunta si la caja
 * está cerrada sin depender del módulo de caja, que es quien implementa esta interfaz.
 */
public interface JornadaOperativa {

    boolean cajaCerrada(LocalDate fecha);

    /** Lanza un error de negocio si el cierre de caja del día ya se ejecutó. */
    default void exigirCajaAbierta() {
        if (cajaCerrada(LocalDate.now())) {
            throw new NegocioException(HttpStatus.CONFLICT, "CAJA_CERRADA",
                    "La caja de hoy ya se cerró: no se aceptan nuevos pedidos ni cobros");
        }
    }
}
