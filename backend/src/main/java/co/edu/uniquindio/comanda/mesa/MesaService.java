package co.edu.uniquindio.comanda.mesa;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.pedido.EstadoPedido;
import co.edu.uniquindio.comanda.pedido.Pedido;
import co.edu.uniquindio.comanda.pedido.PedidoDto;
import co.edu.uniquindio.comanda.pedido.PedidoMapper;
import co.edu.uniquindio.comanda.pedido.PedidoRepository;

/**
 * F-01: consultas de mesas. Se separó de {@code PedidoService} (refactorización
 * <b>Move Method</b>) porque el controlador de mesas solo necesita leer, no gestionar pedidos.
 */
@Service
public class MesaService {

    private final MesaRepository mesas;
    private final PedidoRepository pedidos;

    public MesaService(MesaRepository mesas, PedidoRepository pedidos) {
        this.mesas = mesas;
        this.pedidos = pedidos;
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
}
