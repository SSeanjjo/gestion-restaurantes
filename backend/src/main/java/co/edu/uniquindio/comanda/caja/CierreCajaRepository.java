package co.edu.uniquindio.comanda.caja;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    boolean existsByFecha(LocalDate fecha);

    @EntityGraph(attributePaths = { "ejecutadoPor" })
    List<CierreCaja> findTop30ByOrderByFechaDesc();
}
