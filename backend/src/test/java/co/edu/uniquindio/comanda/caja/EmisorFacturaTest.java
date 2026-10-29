package co.edu.uniquindio.comanda.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.uniquindio.comanda.caja.dian.ServicioFacturacionElectronica;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;

/**
 * Hallazgo H-01: al extraer la emisión de la factura, el número, el CUFE y la respuesta de la DIAN
 * se prueban sin armar el flujo completo de cobro (sin pasarela, mesa ni eventos).
 */
class EmisorFacturaTest {

    private final FacturaRepository facturas = mock(FacturaRepository.class);
    private final ServicioFacturacionElectronica dian = mock(ServicioFacturacionElectronica.class);

    private EmisorFactura emisor;
    private Pedido pedido;
    private final Desglose desglose = Desglose.de(new BigDecimal("28000"), new BigDecimal("0.08"));
    private final List<Pago> pagos = List.of(
            new Pago(MetodoPago.EFECTIVO, new BigDecimal("28000"), new BigDecimal("30000"), new BigDecimal("2000"), null, null));

    @BeforeEach
    void preparar() {
        ComandaProperties props = new ComandaProperties(null, null, null, null, null,
                new ComandaProperties.Facturacion("900123456", "FEBS", new BigDecimal("0.08"), "INC", "clave", 2));
        emisor = new EmisorFactura(facturas, dian, props);
        pedido = Fixtures.pedido(20L, EstadoPedido.ENTREGADO, "28000", Fixtures.mesa(5L, 5));
        when(facturas.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            ReflectionTestUtils.setField(f, "id", 15L);
            return f;
        });
    }

    @Test
    void numeraConElPrefijoYElIdYGuardaElCufeDe96Caracteres() {
        when(dian.validarFactura(any())).thenReturn(new ServicioFacturacionElectronica.Respuesta(true, "ok"));

        Factura f = emisor.emitir(pedido, Fixtures.usuario(4L), desglose, pagos);

        assertThat(f.getNumero()).isEqualTo("FEBS-15");
        assertThat(f.getCufe()).hasSize(96);
        assertThat(f.getEstadoDian()).isEqualTo(EstadoFactura.ACEPTADA);
        assertThat(f.getTotal()).isEqualByComparingTo("28000");
        assertThat(f.getPagos()).hasSize(1);
    }

    @Test
    void siLaDianRechazaLanzaDianRechazoYNoMarcaLaFacturaComoAceptada() {
        when(dian.validarFactura(any())).thenReturn(new ServicioFacturacionElectronica.Respuesta(false, "CUFE inválido"));

        assertThatThrownBy(() -> emisor.emitir(pedido, Fixtures.usuario(4L), desglose, pagos))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("DIAN_RECHAZO");
    }
}
