package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** RF-09 · Transacción de caja: fecha, hora, monto, método de pago y factura asociada. */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id")
    private Factura factura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @Column(precision = 12, scale = 2)
    private BigDecimal cambio;

    /** Código de autorización de la pasarela (solo tarjeta). */
    @Column(length = 40)
    private String autorizacion;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected Pago() {
    }

    public Pago(MetodoPago metodo, BigDecimal monto, BigDecimal montoRecibido, BigDecimal cambio,
            String autorizacion, String ultimosDigitos) {
        this.metodo = metodo;
        this.monto = monto;
        this.montoRecibido = montoRecibido;
        this.cambio = cambio;
        this.autorizacion = autorizacion;
        this.ultimosDigitos = ultimosDigitos;
        this.fechaHora = LocalDateTime.now();
    }

    void asociar(Factura factura) {
        this.factura = factura;
    }

    public Long getId() {
        return id;
    }

    public Factura getFactura() {
        return factura;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public String getAutorizacion() {
        return autorizacion;
    }

    public String getUltimosDigitos() {
        return ultimosDigitos;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
