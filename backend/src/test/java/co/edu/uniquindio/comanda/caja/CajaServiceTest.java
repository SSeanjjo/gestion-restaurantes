package co.edu.uniquindio.comanda.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.uniquindio.comanda.auth.UsuarioRepository;
import co.edu.uniquindio.comanda.caja.CajaDtos.FacturaDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.SolicitudCobro;
import co.edu.uniquindio.comanda.caja.dian.ServicioFacturacionElectronica;
import co.edu.uniquindio.comanda.caja.pago.PagoEfectivo;
import co.edu.uniquindio.comanda.caja.pago.PagoTarjeta;
import co.edu.uniquindio.comanda.caja.pago.PasarelaPago;
import co.edu.uniquindio.comanda.caja.pago.SolicitudPago;
import co.edu.uniquindio.comanda.common.JornadaOperativa;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.config.ComandaProperties;
import co.edu.uniquindio.comanda.mesa.EstadoMesa;
import co.edu.uniquindio.comanda.mesa.Mesa;
import co.edu.uniquindio.comanda.mesa.MesaRepository;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;

/** Contrato 3 · registrarFactura: cobro, factura DIAN, liberación de mesa y compensación. */
class CajaServiceTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final MesaRepository mesas = mock(MesaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final FacturaRepository facturas = mock(FacturaRepository.class);
    private final ServicioFacturacionElectronica dian = mock(ServicioFacturacionElectronica.class);
    private final JornadaOperativa jornada = mock(JornadaOperativa.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final PasarelaPago pasarela = mock(PasarelaPago.class);

    private CajaService caja;
    private Mesa mesa;
    private Pedido pedido;

    @BeforeEach
    void preparar() {
        ComandaProperties props = new ComandaProperties(null, null, null, null, null,
                new ComandaProperties.Facturacion("900123456", "FEBS", new BigDecimal("0.08"), "INC", "clave", 2));
        caja = new CajaService(pedidos, mesas, usuarios, facturas, dian, jornada, eventos, props,
                List.of(new PagoEfectivo(), new PagoTarjeta(pasarela)));

        mesa = Fixtures.mesa(5L, 5);
        pedido = Fixtures.pedido(20L, EstadoPedido.ENTREGADO, "28000", mesa);
        when(pedidos.bloquearPorId(20L)).thenReturn(Optional.of(pedido));
        when(mesas.findByIdParaActualizar(5L)).thenReturn(Optional.of(mesa));
        when(usuarios.findById(4L)).thenReturn(Optional.of(Fixtures.usuario(4L)));
        when(facturas.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            ReflectionTestUtils.setField(f, "id", 15L);
            return f;
        });
        when(pasarela.autorizar(any(), anyString(), anyString()))
                .thenReturn(new PasarelaPago.Autorizacion(true, "AUT-123456", "Aprobada"));
    }

    @Test
    void cobroDivididoEmiteLaFacturaYLiberaLaMesa() {
        when(dian.validarFactura(any())).thenReturn(new ServicioFacturacionElectronica.Respuesta(true, "ok"));

        FacturaDto f = caja.cobrar(20L, new SolicitudCobro(List.of(
                new SolicitudPago(MetodoPago.EFECTIVO, new BigDecimal("10000"), new BigDecimal("20000"), null),
                new SolicitudPago(MetodoPago.TARJETA, new BigDecimal("18000"), null, "4242"))), 4L);

        assertThat(f.numero()).isEqualTo("FEBS-15");
        assertThat(f.cufe()).hasSize(96);
        assertThat(f.estadoDian()).isEqualTo(EstadoFactura.ACEPTADA);
        assertThat(f.pagos()).hasSize(2);
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.FACTURADO);
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.DISPONIBLE);
    }

    @Test
    void rechazaPagosQueNoSumanElTotal() {
        assertThatThrownBy(() -> caja.cobrar(20L, new SolicitudCobro(List.of(
                new SolicitudPago(MetodoPago.TARJETA, new BigDecimal("20000"), null, "4242"))), 4L))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("PAGOS_NO_CUADRAN");
        verify(pasarela, never()).autorizar(any(), anyString(), anyString());
    }

    @Test
    void siLaDianRechazaSeReversaLaTarjeta() {
        when(dian.validarFactura(any())).thenReturn(new ServicioFacturacionElectronica.Respuesta(false, "CUFE inválido"));

        assertThatThrownBy(() -> caja.cobrar(20L, new SolicitudCobro(List.of(
                new SolicitudPago(MetodoPago.TARJETA, new BigDecimal("28000"), null, "4242"))), 4L))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("DIAN_RECHAZO");
        verify(pasarela).reversar("AUT-123456");
    }

    @Test
    void noCobraUnPedidoQueNoSeHaEntregado() {
        ReflectionTestUtils.setField(pedido, "estado", EstadoPedido.EN_PREPARACION);

        assertThatThrownBy(() -> caja.cobrar(20L, new SolicitudCobro(List.of(
                new SolicitudPago(MetodoPago.EFECTIVO, new BigDecimal("28000"), null, null))), 4L))
                .isInstanceOf(NegocioException.class)
                .extracting("codigo").isEqualTo("PEDIDO_NO_ENTREGADO");
    }
}
