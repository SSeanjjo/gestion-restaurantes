package co.edu.uniquindio.comanda.pedido;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.inventario.Ingrediente;
import co.edu.uniquindio.comanda.inventario.IngredienteDto;
import co.edu.uniquindio.comanda.inventario.IngredienteRepository;
import co.edu.uniquindio.comanda.inventario.MovimientoInventario;
import co.edu.uniquindio.comanda.inventario.MovimientoInventarioRepository;
import co.edu.uniquindio.comanda.inventario.TipoMovimiento;
import co.edu.uniquindio.comanda.menu.ProductoMenu;
import co.edu.uniquindio.comanda.menu.ProductoMenuRepository;
import co.edu.uniquindio.comanda.menu.ReglaDisponibilidad;

/**
 * Consumo de inventario de un pedido (F-05, F-06 y reintegro de F-13).
 *
 * Refactorización <b>Extract Class</b>: esta lógica estaba duplicada dentro de
 * {@code PedidoService.crearPedido} y {@code PedidoService.anular}. Ahora vive en un solo lugar
 * (DRY) y el servicio de pedidos queda con una única responsabilidad.
 *
 * Todas las operaciones exigen una transacción abierta ({@link Propagation#MANDATORY}) porque
 * los ingredientes quedan bloqueados ({@code SELECT … FOR UPDATE}) hasta el commit.
 */
@Component
public class ConsumoIngredientesPedido {

    /** Ingredientes bloqueados, productos con receta y consumo total calculado. */
    public record Reserva(Map<Long, Ingrediente> ingredientes, Map<Long, ProductoMenu> productos,
            Map<Long, BigDecimal> requerido) {

        /** Ids de los ingredientes cuyo stock no alcanza para el pedido. */
        public Set<Long> faltantes() {
            return requerido.entrySet().stream()
                    .filter(e -> !ingredientes.get(e.getKey()).alcanzaPara(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }

    private final ProductoMenuRepository productos;
    private final IngredienteRepository ingredientes;
    private final MovimientoInventarioRepository movimientos;
    private final ReglaDisponibilidad reglaDisponibilidad;

    public ConsumoIngredientesPedido(ProductoMenuRepository productos, IngredienteRepository ingredientes,
            MovimientoInventarioRepository movimientos, ReglaDisponibilidad reglaDisponibilidad) {
        this.productos = productos;
        this.ingredientes = ingredientes;
        this.movimientos = movimientos;
        this.reglaDisponibilidad = reglaDisponibilidad;
    }

    /**
     * Bloquea los ingredientes ANTES de cargar los productos, para leer el stock ya bloqueado,
     * y calcula el consumo total (cantidad de receta × unidades). Los ingredientes se bloquean
     * ordenados por id para evitar interbloqueos entre pedidos concurrentes.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reserva reservar(Map<Long, Integer> unidadesPorProducto) {
        if (unidadesPorProducto.isEmpty()) {
            return new Reserva(Map.of(), Map.of(), Map.of());
        }
        List<Long> idsIngredientes = productos.idsIngredientesDeProductos(unidadesPorProducto.keySet());
        Map<Long, Ingrediente> bloqueados = idsIngredientes.isEmpty() ? Map.of()
                : ingredientes.bloquearPorIds(idsIngredientes).stream()
                        .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));
        Map<Long, ProductoMenu> porId = productos.findConRecetaPorIds(unidadesPorProducto.keySet()).stream()
                .collect(Collectors.toMap(ProductoMenu::getId, Function.identity()));
        Map<Long, BigDecimal> requerido = porId.keySet().containsAll(unidadesPorProducto.keySet())
                ? reglaDisponibilidad.requerimientos(porId, unidadesPorProducto)
                : Map.of();
        return new Reserva(bloqueados, porId, requerido);
    }

    /**
     * SWR-06: descuenta el consumo y registra un movimiento por ingrediente.
     *
     * @return ingredientes que acaban de llegar a su nivel mínimo (para la alerta al administrador)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<IngredienteDto> descontar(Reserva reserva, Long pedidoId, Long usuarioId) {
        List<IngredienteDto> enMinimo = new ArrayList<>();
        reserva.requerido().forEach((ingredienteId, cantidad) -> {
            Ingrediente ingrediente = reserva.ingredientes().get(ingredienteId);
            boolean yaEstabaEnMinimo = ingrediente.enNivelMinimo();
            ingrediente.descontar(cantidad);
            movimientos.save(new MovimientoInventario(ingrediente, TipoMovimiento.SALIDA_PEDIDO, cantidad.negate(),
                    pedidoId, usuarioId, "Pedido #" + pedidoId));
            if (!yaEstabaEnMinimo && ingrediente.enNivelMinimo()) {
                enMinimo.add(IngredienteDto.de(ingrediente));
            }
        });
        return enMinimo;
    }

    /** SWR-13: devuelve al inventario lo que consumió un pedido anulado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reintegrar(Reserva reserva, Long pedidoId, Long usuarioId) {
        reserva.requerido().forEach((ingredienteId, cantidad) -> {
            Ingrediente ingrediente = reserva.ingredientes().get(ingredienteId);
            ingrediente.sumar(cantidad);
            movimientos.save(new MovimientoInventario(ingrediente, TipoMovimiento.REINTEGRO, cantidad, pedidoId,
                    usuarioId, "Anulación pedido #" + pedidoId));
        });
    }
}
