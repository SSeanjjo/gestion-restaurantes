package co.edu.uniquindio.comanda.mesa;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;
import co.edu.uniquindio.comanda.pedido.PedidoDto;
import co.edu.uniquindio.comanda.pedido.PedidoMapper;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

/**
 * Servicio del módulo de mesas. F-01: consultas (se separó de {@code PedidoService} con la
 * refactorización <b>Move Method</b>). Hallazgo H-02: también es el único lugar que libera una
 * mesa y avisa a las tablets, para que el cobro (CU-02) y la anulación (F-13) no repitan la regla.
 */
@Service
public class MesaService {

    private final MesaRepository mesas;
    private final PedidoRepository pedidos;
    private final ApplicationEventPublisher eventos;

    public MesaService(MesaRepository mesas, PedidoRepository pedidos, ApplicationEventPublisher eventos) {
        this.mesas = mesas;
        this.pedidos = pedidos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<MesaDto> listar() {
        Map<Long, Pedido> activosPorMesa = pedidos.findByEstadoIn(EstadoPedido.ACTIVOS).stream()
                .collect(Collectors.toMap(p -> p.getMesa().getId(), Function.identity(), (a, b) -> a));
        return mesas.findAllByOrderByNumeroAsc().stream()
                .map(m -> PedidoMapper.mesa(m, activosPorMesa.get(m.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PedidoDto> pedidoActivo(Integer numeroMesa) {
        if (mesas.findByNumero(numeroMesa).isEmpty()) {
            throw NegocioException.noEncontrado("MESA_NO_EXISTE", "No existe la mesa " + numeroMesa);
        }
        return pedidos.buscarPorMesaYEstados(numeroMesa, EstadoPedido.ACTIVOS).map(PedidoDto::de);
    }

    /**
     * La mesa vuelve a Disponible y se avisa a las tablets (F-13 y CU-02). Bloquea la mesa antes de
     * cambiarla; debe llamarse dentro de la transacción del llamador.
     */
    public void liberar(Long mesaId) {
        liberar(mesas.findByIdParaActualizar(mesaId).orElseThrow());
    }

    /** Variante para quien ya tiene la mesa bloqueada (por ejemplo, para respetar el orden de bloqueo). */
    public void liberar(Mesa mesa) {
        mesa.liberar();
        eventos.publishEvent(new Eventos.MesaCambiada(PedidoMapper.mesa(mesa, null)));
    }
}
