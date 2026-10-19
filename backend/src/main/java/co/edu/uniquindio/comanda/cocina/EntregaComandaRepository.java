package co.edu.uniquindio.comanda.cocina;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaComandaRepository extends JpaRepository<EntregaComanda, Long> {

    long countByConfirmadaEnIsNull();

    List<EntregaComanda> findByConfirmadaEnIsNullAndPublicadaEnBefore(LocalDateTime limite);

    List<EntregaComanda> findByPedidoIdInAndConfirmadaEnIsNull(Collection<Long> pedidoIds);
}
