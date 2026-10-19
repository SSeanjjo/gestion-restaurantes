package co.edu.uniquindio.comanda.mesa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mesa")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMesa estado = EstadoMesa.DISPONIBLE;

    protected Mesa() {
    }

    public Long getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public boolean estaOcupada() {
        return estado == EstadoMesa.OCUPADA;
    }

    /** Contrato 1 (crearPedido): la mesa pasa a Ocupada. */
    public void ocupar() {
        this.estado = EstadoMesa.OCUPADA;
    }

    /** F-13 (anulación) y CU-02 (pedido facturado): la mesa vuelve a estar Disponible. */
    public void liberar() {
        this.estado = EstadoMesa.DISPONIBLE;
    }
}
