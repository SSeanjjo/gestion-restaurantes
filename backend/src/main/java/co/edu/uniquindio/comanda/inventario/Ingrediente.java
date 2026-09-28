package co.edu.uniquindio.comanda.inventario;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingrediente")
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /** g | ml | und */
    @Column(nullable = false, length = 10)
    private String unidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    protected Ingrediente() {
    }

    public boolean alcanzaPara(BigDecimal cantidad) {
        return stock.compareTo(cantidad) >= 0;
    }

    public boolean enNivelMinimo() {
        return stock.compareTo(stockMinimo) <= 0;
    }

    public void descontar(BigDecimal cantidad) {
        if (!alcanzaPara(cantidad)) {
            throw new IllegalStateException("Stock insuficiente de " + nombre);
        }
        this.stock = stock.subtract(cantidad);
    }

    public void sumar(BigDecimal cantidad) {
        this.stock = stock.add(cantidad);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUnidad() {
        return unidad;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }
}
