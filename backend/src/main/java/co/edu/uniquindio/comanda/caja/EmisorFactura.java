package co.edu.uniquindio.comanda.caja;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.caja.dian.CalculadoraCufe;
import co.edu.uniquindio.comanda.caja.dian.ServicioFacturacionElectronica;
import co.edu.uniquindio.comanda.caja.dian.ServicioFacturacionElectronica.DocumentoFactura;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.pedido.Pedido;

/**
 * Emisión de la factura electrónica: construye y guarda la factura, le asigna el número y el CUFE
 * y la valida ante la DIAN (IS-02).
 *
 * Refactorización <b>Extract Class</b> (hallazgo H-01): estas reglas vivían en
 * {@code CajaService}, que también cobra, compensa y libera la mesa. Ahora cambian aquí cuando
 * cambia la normativa de la DIAN, sin tocar el flujo de cobro. Participa de la transacción del
 * llamador: si la DIAN rechaza, la excepción deshace el guardado de la factura.
 */
@Component
public class EmisorFactura {

    private final FacturaRepository facturas;
    private final ServicioFacturacionElectronica dian;
    private final ComandaProperties.Facturacion config;

    public EmisorFactura(FacturaRepository facturas, ServicioFacturacionElectronica dian, ComandaProperties props) {
        this.facturas = facturas;
        this.dian = dian;
        this.config = props.facturacion();
    }

    /**
     * @return la factura numerada y aceptada por la DIAN
     * @throws NegocioException {@code DIAN_RECHAZO} (502) si la DIAN no la acepta
     */
    public Factura emitir(Pedido pedido, Usuario cajero, Desglose desglose, List<Pago> pagos) {
        Factura.Builder builder = Factura.builder().pedido(pedido).cajero(cajero).desglose(desglose);
        pagos.forEach(builder::pago);
        Factura factura = facturas.save(builder.build());

        String numero = config.prefijo() + "-" + factura.getId();
        String cufe = CalculadoraCufe.calcular(numero, factura.getFechaEmision(), factura.getSubtotal(),
                factura.getImpuesto(), factura.getTotal(), config.nitEmisor(), config.claveTecnica(),
                config.ambiente());
        factura.numerar(numero, cufe);

        ServicioFacturacionElectronica.Respuesta respuesta = dian.validarFactura(new DocumentoFactura(numero,
                factura.getFechaEmision(), factura.getSubtotal(), factura.getImpuesto(), factura.getTotal(), cufe));
        if (!respuesta.aceptada()) {
            throw new NegocioException(HttpStatus.BAD_GATEWAY, "DIAN_RECHAZO", respuesta.mensaje());
        }
        factura.registrarValidacion(EstadoFactura.ACEPTADA, respuesta.mensaje());
        return factura;
    }
}
