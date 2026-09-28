package co.edu.uniquindio.comanda.menu;

import java.math.BigDecimal;

import co.edu.uniquindio.comanda.inventario.Ingrediente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Línea del catálogo de recetas: cuánto de un ingrediente lleva una unidad de un plato. */
@Entity
@Table(name = "receta_ingrediente")
public class RecetaIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private ProductoMenu producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id")
    private Ingrediente ingrediente;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    protected RecetaIngrediente() {
    }

    public Long getId() {
        return id;
    }

    public ProductoMenu getProducto() {
        return producto;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }
}
