package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.auth.UsuarioRepository;
import co.edu.uniquindio.comanda.caja.CajaDtos.CierreDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.ResumenDiaDto;
import co.edu.uniquindio.comanda.caja.dian.ServicioFacturacionElectronica;
import co.edu.uniquindio.comanda.common.JornadaOperativa;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

/**
 * CU-02 (segunda parte) y CU-06 · Conciliación y cierre de caja diario (RF-10, RF-11).
 * Implementa {@link JornadaOperativa}: una vez cerrada la caja no se aceptan pedidos ni cobros.
 */
@Service
public class CierreCajaService implements JornadaOperativa {

    private final CierreCajaRepository cierres;
    private final FacturaRepository facturas;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final ServicioFacturacionElectronica dian;
    private final ApplicationEventPublisher eventos;

    public CierreCajaService(CierreCajaRepository cierres, FacturaRepository facturas, PedidoRepository pedidos,
            UsuarioRepository usuarios, ServicioFacturacionElectronica dian, ApplicationEventPublisher eventos) {
        this.cierres = cierres;
        this.facturas = facturas;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.dian = dian;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean cajaCerrada(LocalDate fecha) {
        return cierres.existsByFecha(fecha);
    }

    /** Vista previa del cierre / métricas del día del panel de administración. */
    @Transactional(readOnly = true)
    public ResumenDiaDto resumenDelDia(BigDecimal efectivoContado) {
        LocalDate hoy = LocalDate.now();
        return new ResumenDiaDto(conciliar(hoy, efectivoContado), cajaCerrada(hoy));
    }

    /**
     * Ejecuta el cierre: si hay inconsistencias el administrador debe revisarlas y confirmarlas
     * (curso alterno 9a); luego se guarda el cierre y se envía el reporte consolidado a la DIAN.
     */
    @Transactional
    public CierreDto cerrar(CajaDtos.SolicitudCierre solicitud, Long administradorId) {
        LocalDate hoy = LocalDate.now();
        if (cajaCerrada(hoy)) {
            throw NegocioException.conflicto("CAJA_YA_CERRADA", "La caja de hoy ya fue cerrada");
        }
        ResumenJornada resumen = conciliar(hoy, solicitud.efectivoContado());
        if (!resumen.cuadra() && !solicitud.confirmarInconsistencias()) {
            throw new NegocioException(HttpStatus.CONFLICT, "INCONSISTENCIAS",
                    "Revise y confirme las inconsistencias antes de cerrar la caja", resumen.inconsistencias());
        }
        Usuario admin = usuarios.findById(administradorId)
                .orElseThrow(() -> NegocioException.noEncontrado("USUARIO_NO_EXISTE", "Usuario no encontrado"));

        CierreCaja cierre = cierres.save(new CierreCaja(hoy, admin, resumen, solicitud.observacion()));
        cierre.registrarComprobante(dian.enviarReporteDiario(new ServicioFacturacionElectronica.ReporteDiario(hoy,
                resumen.numeroFacturas(), resumen.totalVentas(), resumen.totalImpuesto())));
        eventos.publishEvent(new Eventos.CajaActualizada("CAJA_CERRADA", null));
        return CierreDto.de(cierre);
    }

    @Transactional(readOnly = true)
    public List<CierreDto> historial() {
        return cierres.findTop30ByOrderByFechaDesc().stream().map(CierreDto::de).toList();
    }

    private ResumenJornada conciliar(LocalDate fecha, BigDecimal efectivoContado) {
        List<Factura> delDia = facturas.deLaJornada(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        long sinCobrar = pedidos.findByEstadoIn(EstadoPedido.ACTIVOS).size();
        return ResumenJornada.calcular(fecha, delDia, sinCobrar, efectivoContado);
    }
}
