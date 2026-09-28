package co.edu.uniquindio.comanda.mesa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findAllByOrderByNumeroAsc();

    Optional<Mesa> findByNumero(Integer numero);

    /** Bloquea la fila de la mesa para que dos meseros no abran pedido en la misma mesa a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.numero = :numero")
    Optional<Mesa> findByNumeroParaActualizar(@Param("numero") Integer numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m where m.id = :id")
    Optional<Mesa> findByIdParaActualizar(@Param("id") Long id);
}
