package co.edu.uniquindio.comanda.inventario;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {

    List<Ingrediente> findAllByOrderByNombreAsc();

    /**
     * SWR-05: bloquea (SELECT ... FOR UPDATE) los ingredientes que va a consumir un pedido.
     * Se ordenan por id para que dos transacciones siempre bloqueen en el mismo orden
     * y no se produzcan interbloqueos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingrediente i where i.id in :ids order by i.id")
    List<Ingrediente> bloquearPorIds(@Param("ids") Collection<Long> ids);
}
