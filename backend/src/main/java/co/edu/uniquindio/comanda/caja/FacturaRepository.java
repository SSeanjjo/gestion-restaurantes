package co.edu.uniquindio.comanda.caja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByPedidoId(Long pedidoId);

    /** Facturas de la jornada con sus pagos (conciliación del cierre de caja). */
    @EntityGraph(attributePaths = { "pagos", "pedido", "pedido.mesa" })
    @Query("select f from Factura f where f.fechaEmision >= :desde and f.fechaEmision < :hasta order by f.id asc")
    List<Factura> deLaJornada(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @EntityGraph(attributePaths = { "pagos", "pedido", "pedido.mesa" })
    @Query("select f from Factura f where f.id = :id")
    Optional<Factura> buscarCompleta(@Param("id") Long id);
}
