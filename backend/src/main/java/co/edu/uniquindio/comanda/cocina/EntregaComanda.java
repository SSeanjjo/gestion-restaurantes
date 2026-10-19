package co.edu.uniquindio.comanda.cocina;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * F-08 · Registro persistente de entrega de una comanda a la pantalla de cocina
 * (patrón <b>Transactional Outbox</b>). Se crea en la misma transacción que el pedido y queda
 * pendiente hasta que la cocina confirma la recepción (ACK).
 */
@Entity
@Table(name = "entrega_comanda")
public class EntregaComanda {

    @Id
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "publicada_en", nullable = false)
    private LocalDateTime publicadaEn;

    @Column(name = "confirmada_en")
    private LocalDateTime confirmadaEn;

    @Column(nullable = false)
    private int intentos = 1;

    protected EntregaComanda() {
    }

    public EntregaComanda(Long pedidoId) {
        this.pedidoId = pedidoId;
        this.publicadaEn = LocalDateTime.now();
    }

    public void registrarReenvio() {
        this.intentos++;
        this.publicadaEn = LocalDateTime.now();
    }

    /** Idempotente: confirmar dos veces conserva la primera hora de recepción. */
    public void confirmar() {
        if (confirmadaEn == null) {
            this.confirmadaEn = LocalDateTime.now();
        }
    }

    public boolean isConfirmada() {
        return confirmadaEn != null;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public LocalDateTime getPublicadaEn() {
        return publicadaEn;
    }

    public LocalDateTime getConfirmadaEn() {
        return confirmadaEn;
    }

    public int getIntentos() {
        return intentos;
    }
}
