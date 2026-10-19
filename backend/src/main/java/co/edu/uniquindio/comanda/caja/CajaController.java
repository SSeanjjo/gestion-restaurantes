package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.comanda.auth.UsuarioActual;
import co.edu.uniquindio.comanda.caja.CajaDtos.CierreDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.CuentaDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.DetalleCuentaDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.FacturaDto;
import co.edu.uniquindio.comanda.caja.CajaDtos.ResumenDiaDto;
import jakarta.validation.Valid;

/** CU-02 · API del módulo de caja (IU-03) y del cierre de caja (IU-04). RNF-04: solo cajero y administrador. */
@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private final CajaService caja;
    private final CierreCajaService cierre;

    public CajaController(CajaService caja, CierreCajaService cierre) {
        this.caja = caja;
        this.cierre = cierre;
    }

    /** F-15: mesas con cuenta abierta. */
    @GetMapping("/cuentas")
    @PreAuthorize("hasAnyRole('CAJERO','ADMINISTRADOR')")
    public List<CuentaDto> cuentas() {
        return caja.cuentasAbiertas();
    }

    /** F-15: desglose de la cuenta con el total calculado (RF-06). */
    @GetMapping("/cuentas/{pedidoId}")
    @PreAuthorize("hasAnyRole('CAJERO','ADMINISTRADOR')")
    public DetalleCuentaDto cuenta(@PathVariable Long pedidoId) {
        return caja.detalle(pedidoId);
    }

    /** F-16 a F-19: registrar el pago (efectivo, tarjeta o dividido) y generar la factura electrónica. */
    @PostMapping("/cuentas/{pedidoId}/cobro")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CAJERO','ADMINISTRADOR')")
    public FacturaDto cobrar(@PathVariable Long pedidoId, @Valid @RequestBody CajaDtos.SolicitudCobro solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        return caja.cobrar(pedidoId, solicitud, UsuarioActual.id(jwt));
    }

    /** RF-09: registro de caja del día (facturas con sus pagos). */
    @GetMapping("/facturas")
    @PreAuthorize("hasAnyRole('CAJERO','ADMINISTRADOR')")
    public List<FacturaDto> facturas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return caja.facturasDelDia(fecha == null ? LocalDate.now() : fecha);
    }

    /** F-20: métricas del día y vista previa de la conciliación. */
    @GetMapping("/resumen-dia")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResumenDiaDto resumen(@RequestParam(required = false) BigDecimal efectivoContado) {
        return cierre.resumenDelDia(efectivoContado);
    }

    /** F-20: ejecutar el cierre de caja (RF-10: solo el administrador). */
    @PostMapping("/cierre")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public CierreDto cerrar(@Valid @RequestBody CajaDtos.SolicitudCierre solicitud, @AuthenticationPrincipal Jwt jwt) {
        return cierre.cerrar(solicitud, UsuarioActual.id(jwt));
    }

    @GetMapping("/cierres")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<CierreDto> cierres() {
        return cierre.historial();
    }
}
