package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.uniquindio.comanda.auth.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** RF-10 / RF-11 · Cierre de caja diario con su reporte de ventas (estado E-08). */
@Entity
@Table(name = "cierre_caja")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecutado_por")
    private Usuario ejecutadoPor;

    @Column(name = "ejecutado_en", nullable = false)
    private LocalDateTime ejecutadoEn;

    @Column(name = "numero_facturas", nullable = false)
    private int numeroFacturas;

    @Column(name = "numero_transacciones", nullable = false)
    private int numeroTransacciones;

    @Column(name = "total_ventas", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVentas;

    @Column(name = "total_efectivo", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEfectivo;

    @Column(name = "total_tarjeta", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTarjeta;

    @Column(name = "total_impuesto", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalImpuesto;

    @Column(name = "efectivo_contado", precision = 14, scale = 2)
    private BigDecimal efectivoContado;

    /** Inconsistencias revisadas y aceptadas por el administrador (una por línea). */
    @Column(length = 2000)
    private String inconsistencias;

    @Column(length = 255)
    private String observacion;

    /** Comprobante de recepción del reporte diario en la DIAN. */
    @Column(name = "comprobante_dian", length = 60)
    private String comprobanteDian;

    protected CierreCaja() {
    }

    public CierreCaja(LocalDate fecha, Usuario ejecutadoPor, ResumenJornada resumen, String observacion) {
        this.fecha = fecha;
        this.ejecutadoPor = ejecutadoPor;
        this.ejecutadoEn = LocalDateTime.now();
        this.numeroFacturas = resumen.numeroFacturas();
        this.numeroTransacciones = resumen.numeroTransacciones();
        this.totalVentas = resumen.totalVentas();
        this.totalEfectivo = resumen.totalEfectivo();
        this.totalTarjeta = resumen.totalTarjeta();
        this.totalImpuesto = resumen.totalImpuesto();
        this.efectivoContado = resumen.efectivoContado();
        this.inconsistencias = resumen.inconsistencias().isEmpty() ? null
                : String.join("\n", resumen.inconsistencias());
        this.observacion = observacion;
    }

    public void registrarComprobante(String comprobante) {
        this.comprobanteDian = comprobante;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Usuario getEjecutadoPor() {
        return ejecutadoPor;
    }

    public LocalDateTime getEjecutadoEn() {
        return ejecutadoEn;
    }

    public int getNumeroFacturas() {
        return numeroFacturas;
    }

    public int getNumeroTransacciones() {
        return numeroTransacciones;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public BigDecimal getTotalEfectivo() {
        return totalEfectivo;
    }

    public BigDecimal getTotalTarjeta() {
        return totalTarjeta;
    }

    public BigDecimal getTotalImpuesto() {
        return totalImpuesto;
    }

    public BigDecimal getEfectivoContado() {
        return efectivoContado;
    }

    public String getInconsistencias() {
        return inconsistencias;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getComprobanteDian() {
        return comprobanteDian;
    }
}
