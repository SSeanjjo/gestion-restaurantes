package co.edu.uniquindio.comanda.common;

import java.time.OffsetDateTime;

public record ErrorRespuesta(String codigo, String mensaje, Object detalles, OffsetDateTime timestamp) {

    public static ErrorRespuesta de(String codigo, String mensaje, Object detalles) {
        return new ErrorRespuesta(codigo, mensaje, detalles, OffsetDateTime.now());
    }
}
