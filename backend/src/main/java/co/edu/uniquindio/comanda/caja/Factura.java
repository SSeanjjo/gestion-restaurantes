package co.edu.uniquindio.comanda.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.pedido.Pedido;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * RF-07 · Factura electrónica de venta (Contrato 3 · registrarFactura).
 *
 * Se construye con el patrón <b>Builder</b>: la factura necesita muchos datos obligatorios
 * (pedido, cajero, desglose, pagos) y el builder valida que estén completos antes de crearla.
 */
@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Prefijo de la resolución + consecutivo, p. ej. FEBS-15. Se asigna tras obtener el id. */
    @Column(unique = true, length = 30)
    private String numero;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cajero_id")
    private Usuario cajero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impuesto;

    @Column(name = "tarifa_impuesto", nullable = false, precision = 5, scale = 4)
    private BigDecimal tarifaImpuesto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /** Código Único de Factura Electrónica (SHA-384, 96 caracteres hexadecimales). */
    @Column(length = 96)
    private String cufe;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_dian", length = 20)
    private EstadoFactura estadoDian;

    @Column(name = "mensaje_dian", length = 255)
    private String mensajeDian;

    @Column(name = "validada_en")
    private LocalDateTime validadaEn;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Pago> pagos = new ArrayList<>();

    protected Factura() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Asigna el consecutivo y el CUFE calculado por el emisor. */
    public void numerar(String numero, String cufe) {
        this.numero = numero;
        this.cufe = cufe;
    }

    /** Registra la respuesta síncrona del servicio de la DIAN. */
    public void registrarValidacion(EstadoFactura estado, String mensaje) {
        this.estadoDian = estado;
        this.mensajeDian = mensaje;
        this.validadaEn = LocalDateTime.now();
    }

    public boolean validadaPorDian() {
        return estadoDian == EstadoFactura.ACEPTADA && cufe != null && !cufe.isBlank();
    }

    public BigDecimal totalPagado() {
        return pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Usuario getCajero() {
        return cajero;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getImpuesto() {
        return impuesto;
    }

    public BigDecimal getTarifaImpuesto() {
        return tarifaImpuesto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCufe() {
        return cufe;
    }

    public EstadoFactura getEstadoDian() {
        return estadoDian;
    }

    public String getMensajeDian() {
        return mensajeDian;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    /** Builder de {@link Factura}. */
    public static final class Builder {

        private Pedido pedido;
        private Usuario cajero;
        private Desglose desglose;
        private final List<Pago> pagos = new ArrayList<>();

        private Builder() {
        }

        public Builder pedido(Pedido pedido) {
            this.pedido = pedido;
            return this;
        }

        public Builder cajero(Usuario cajero) {
            this.cajero = cajero;
            return this;
        }

        public Builder desglose(Desglose desglose) {
            this.desglose = desglose;
            return this;
        }

        public Builder pago(Pago pago) {
            this.pagos.add(pago);
            return this;
        }

        public Factura build() {
            Objects.requireNonNull(pedido, "pedido");
            Objects.requireNonNull(cajero, "cajero");
            Objects.requireNonNull(desglose, "desglose");
            if (pagos.isEmpty()) {
                throw new IllegalStateException("La factura debe tener al menos un pago");
            }
            Factura f = new Factura();
            f.pedido = pedido;
            f.cajero = cajero;
            f.fechaEmision = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            f.subtotal = desglose.base();
            f.impuesto = desglose.impuesto();
            f.tarifaImpuesto = desglose.tarifa();
            f.total = desglose.total();
            pagos.forEach(p -> {
                p.asociar(f);
                f.pagos.add(p);
            });
            return f;
        }
    }
}
