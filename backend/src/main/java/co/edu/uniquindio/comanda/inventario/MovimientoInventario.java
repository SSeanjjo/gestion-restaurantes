package co.edu.uniquindio.comanda.inventario;

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

@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id")
    private Ingrediente ingrediente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 255)
    private String observacion;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(Ingrediente ingrediente, TipoMovimiento tipo, BigDecimal cantidad,
            Long pedidoId, Long usuarioId, String observacion) {
        this.ingrediente = ingrediente;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.observacion = observacion;
        this.fechaHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
