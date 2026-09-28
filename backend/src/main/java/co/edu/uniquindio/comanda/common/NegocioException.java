package co.edu.uniquindio.comanda.common;

import org.springframework.http.HttpStatus;

/**
 * Error de regla de negocio. Se traduce a una respuesta HTTP con un código
 * estable que el frontend puede interpretar.
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final transient Object detalles;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        this(status, codigo, mensaje, null);
    }

    public NegocioException(HttpStatus status, String codigo, String mensaje, Object detalles) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public static NegocioException noEncontrado(String codigo, String mensaje) {
        return new NegocioException(HttpStatus.NOT_FOUND, codigo, mensaje);
    }

    public static NegocioException conflicto(String codigo, String mensaje) {
        return new NegocioException(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public Object getDetalles() {
        return detalles;
    }
}
