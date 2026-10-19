package co.edu.uniquindio.comanda.pedido;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarCompleto(@Param("id") Long id);

    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    @Query("select p from Pedido p where p.mesa.numero = :numero and p.estado in :estados")
    Optional<Pedido> buscarPorMesaYEstados(@Param("numero") Integer numero,
            @Param("estados") Collection<EstadoPedido> estados);

    /** F-08: pedido ya registrado con la misma clave (reintento de la tablet). */
    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    Optional<Pedido> findByClaveIdempotencia(String claveIdempotencia);

    /** F-08: comandas que se deben reenviar, en orden de confirmación. */
    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    @Query("select p from Pedido p where p.id in :ids order by p.fechaHora asc, p.id asc")
    List<Pedido> buscarCompletosPorIds(@Param("ids") Collection<Long> ids);

    /** CU-02: bloquea el pedido para que dos cajeros no lo cobren a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquearPorId(@Param("id") Long id);

    /** CU-02: cuentas abiertas (pedidos activos) para el módulo de caja, por hora de llegada. */
    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    @Query("select p from Pedido p where p.estado in :estados order by p.fechaHora asc")
    List<Pedido> cuentasAbiertas(@Param("estados") Collection<EstadoPedido> estados);

    /** Pedidos activos (para pintar la cuadrícula de mesas). */
    @EntityGraph(attributePaths = { "mesa" })
    List<Pedido> findByEstadoIn(Collection<EstadoPedido> estados);

    /** IU-02: comandas activas ordenadas por hora de llegada. */
    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    @Query("select p from Pedido p where p.estado in :estados order by p.fechaHora asc")
    List<Pedido> comandasEnCocina(@Param("estados") Collection<EstadoPedido> estados);

    /** Pedidos listos de un mesero, para recuperar sus notificaciones pendientes al recargar. */
    @EntityGraph(attributePaths = { "mesa", "detalles", "detalles.producto" })
    List<Pedido> findByMeseroIdAndEstadoOrderByNotificadoEnAsc(Long meseroId, EstadoPedido estado);

    /** Curso alterno 6a: pedidos listos cuya última alerta se envió antes de {@code limite}. */
    @EntityGraph(attributePaths = { "mesa", "mesero", "detalles", "detalles.producto" })
    List<Pedido> findByEstadoAndNotificadoEnBefore(EstadoPedido estado, LocalDateTime limite);
}
