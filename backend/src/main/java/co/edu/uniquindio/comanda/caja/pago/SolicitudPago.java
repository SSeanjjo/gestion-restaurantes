package co.edu.uniquindio.comanda.caja.pago;

import java.math.BigDecimal;

import co.edu.uniquindio.comanda.caja.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Una parte del pago de la cuenta. Un cobro puede tener varias (curso alterno 4a: dividir
 * entre efectivo y tarjeta).
 *
 * @param montoRecibido solo efectivo: dinero entregado por el cliente
 * @param ultimosDigitos solo tarjeta: últimos 4 dígitos (el sistema nunca recibe el número completo)
 */
public record SolicitudPago(
        @NotNull MetodoPago metodo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        BigDecimal montoRecibido,
        @Pattern(regexp = "\\d{4}", message = "debe tener 4 dígitos") String ultimosDigitos) {
}
