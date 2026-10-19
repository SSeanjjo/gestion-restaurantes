package co.edu.uniquindio.comanda.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import co.edu.uniquindio.comanda.auth.Usuario;
import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.menu.ProductoMenu;
import co.edu.uniquindio.comanda.mesa.Mesa;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mesero_id")
    private Usuario mesero;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Última vez que se avisó al mesero que el pedido está listo (para el reenvío a los 5 min). */
    @Column(name = "notificado_en")
    private LocalDateTime notificadoEn;

    @Column(name = "entregado_en")
    private LocalDateTime entregadoEn;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por")
    private Usuario anuladoPor;

    @Column(name = "motivo_anulacion", length = 255)
    private String motivoAnulacion;

    /** F-08: identificador que envía la tablet para que un reintento no duplique el pedido. */
    @Column(name = "clave_idempotencia", length = 36, unique = true)
    private String claveIdempotencia;

    /** Estado E-07 (cuenta pendiente): el mesero avisó que el cliente pidió la cuenta. */
    @Column(name = "cuenta_solicitada_en")
    private LocalDateTime cuentaSolicitadaEn;

    @Column(name = "facturado_en")
    private LocalDateTime facturadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetallePedido> detalles = new ArrayList<>();

    protected Pedido() {
    }

    /** Contrato 1 (crearPedido): el pedido nace en estado REGISTRADO con la fecha y hora actual. */
    public Pedido(Mesa mesa, Usuario mesero, String claveIdempotencia) {
        this.mesa = mesa;
        this.mesero = mesero;
        this.claveIdempotencia = claveIdempotencia;
        this.fechaHora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        this.estado = EstadoPedido.REGISTRADO;
    }

    public void agregarDetalle(ProductoMenu producto, int cantidad) {
        DetallePedido detalle = new DetallePedido(this, producto, cantidad);
        detalles.add(detalle);
        total = total.add(detalle.getSubtotal());
    }

    /** Aplica una transición respetando RD-03. */
    public void cambiarEstado(EstadoPedido destino) {
        if (!estado.puedePasarA(destino)) {
            throw new NegocioException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA",
                    "No se puede pasar el pedido de " + estado + " a " + destino);
        }
        this.estado = destino;
    }

    public void marcarListo() {
        cambiarEstado(EstadoPedido.LISTO_PARA_SERVIR);
        detalles.forEach(d -> d.setPreparado(true));
        this.notificadoEn = LocalDateTime.now();
    }

    public void marcarEntregado() {
        cambiarEstado(EstadoPedido.ENTREGADO);
        this.entregadoEn = LocalDateTime.now();
    }

    public void anular(Usuario usuario, String motivo) {
        cambiarEstado(EstadoPedido.CANCELADO);
        this.anuladoPor = usuario;
        this.anuladoEn = LocalDateTime.now();
        this.motivoAnulacion = motivo;
    }

    public void marcarItemPreparado(Long detalleId, boolean preparado) {
        if (estado != EstadoPedido.EN_PREPARACION) {
            throw NegocioException.conflicto("PEDIDO_NO_EN_PREPARACION",
                    "Solo se pueden marcar platos de un pedido en preparación");
        }
        DetallePedido detalle = detalles.stream().filter(d -> d.getId().equals(detalleId)).findFirst()
                .orElseThrow(() -> NegocioException.noEncontrado("DETALLE_NO_EXISTE",
                        "El plato no pertenece a este pedido"));
        detalle.setPreparado(preparado);
    }

    public void registrarRecordatorio() {
        this.notificadoEn = LocalDateTime.now();
    }

    /** F-14: el cliente pidió la cuenta (estado E-07). Repetir la solicitud no cambia la hora original. */
    public void solicitarCuenta() {
        if (!EstadoPedido.ACTIVOS.contains(estado)) {
            throw NegocioException.conflicto("PEDIDO_NO_ACTIVO", "El pedido ya no está activo");
        }
        if (cuentaSolicitadaEn == null) {
            this.cuentaSolicitadaEn = LocalDateTime.now();
        }
    }

    /** RD-05: solo se cobra un pedido entregado. */
    public boolean esCobrable() {
        return estado == EstadoPedido.ENTREGADO;
    }

    /** Contrato 3 (registrarFactura): el pedido pasa de manera definitiva a FACTURADO. */
    public void marcarFacturado() {
        cambiarEstado(EstadoPedido.FACTURADO);
        this.facturadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public Usuario getMesero() {
        return mesero;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getNotificadoEn() {
        return notificadoEn;
    }

    public LocalDateTime getEntregadoEn() {
        return entregadoEn;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public LocalDateTime getCuentaSolicitadaEn() {
        return cuentaSolicitadaEn;
    }

    public LocalDateTime getFacturadoEn() {
        return facturadoEn;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }
}
