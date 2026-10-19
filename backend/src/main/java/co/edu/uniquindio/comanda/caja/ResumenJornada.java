package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-10 / RF-11 · Conciliación de la jornada: compara facturas con transacciones de caja y
 * detecta inconsistencias antes del cierre. Es un cálculo puro (sin base de datos) para poder
 * probarlo de forma unitaria.
 */
public record ResumenJornada(
        LocalDate fecha,
        int numeroFacturas,
        int numeroTransacciones,
        BigDecimal totalVentas,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        BigDecimal totalImpuesto,
        BigDecimal efectivoContado,
        long pedidosSinCobrar,
        List<String> inconsistencias) {

    /**
     * @param facturas         facturas emitidas en la jornada (con sus pagos)
     * @param pedidosSinCobrar pedidos activos que todavía no se han facturado
     * @param efectivoContado  efectivo que el administrador contó en caja (opcional)
     */
    public static ResumenJornada calcular(LocalDate fecha, List<Factura> facturas, long pedidosSinCobrar,
            BigDecimal efectivoContado) {
        List<String> inconsistencias = new ArrayList<>();
        BigDecimal ventas = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        int transacciones = 0;

        for (Factura f : facturas) {
            ventas = ventas.add(f.getTotal());
            impuesto = impuesto.add(f.getImpuesto());
            transacciones += f.getPagos().size();
            for (Pago p : f.getPagos()) {
                if (p.getMetodo() == MetodoPago.EFECTIVO) {
                    efectivo = efectivo.add(p.getMonto());
                } else {
                    tarjeta = tarjeta.add(p.getMonto());
                }
            }
            if (!f.validadaPorDian()) {
                inconsistencias.add("La factura " + f.getNumero() + " no tiene CUFE validado por la DIAN");
            }
            if (f.getPagos().isEmpty()) {
                inconsistencias.add("La factura " + f.getNumero() + " no tiene pagos registrados");
            } else if (f.totalPagado().compareTo(f.getTotal()) != 0) {
                inconsistencias.add("La factura " + f.getNumero() + " no cuadra: total " + f.getTotal()
                        + " vs. pagado " + f.totalPagado());
            }
        }
        if (pedidosSinCobrar > 0) {
            inconsistencias.add("Hay " + pedidosSinCobrar + " pedido(s) activo(s) sin facturar");
        }
        if (efectivoContado != null && efectivoContado.compareTo(efectivo) != 0) {
            inconsistencias.add("El efectivo contado (" + efectivoContado + ") no coincide con el registrado ("
                    + efectivo + ")");
        }
        return new ResumenJornada(fecha, facturas.size(), transacciones, ventas, efectivo, tarjeta, impuesto,
                efectivoContado, pedidosSinCobrar, List.copyOf(inconsistencias));
    }

    public boolean cuadra() {
        return inconsistencias.isEmpty();
    }
}
