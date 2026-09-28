package co.edu.uniquindio.comanda.inventario;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.comanda.common.NegocioException;
import co.edu.uniquindio.comanda.tiemporeal.Eventos;

@Service
public class InventarioService {

    private final IngredienteRepository ingredientes;
    private final MovimientoInventarioRepository movimientos;
    private final ApplicationEventPublisher eventos;

    public InventarioService(IngredienteRepository ingredientes, MovimientoInventarioRepository movimientos,
            ApplicationEventPublisher eventos) {
        this.ingredientes = ingredientes;
        this.movimientos = movimientos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<IngredienteDto> listar() {
        return ingredientes.findAllByOrderByNombreAsc().stream().map(IngredienteDto::de).toList();
    }

    /**
     * Suma una entrada de mercancía al stock (RF-17, versión mínima).
     * Al cambiar el stock puede cambiar la disponibilidad del menú, así que se avisa a las tablets.
     */
    @Transactional
    public IngredienteDto registrarEntrada(Long ingredienteId, BigDecimal cantidad, String observacion,
            Long usuarioId) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA",
                    "La cantidad de la entrada debe ser mayor que cero");
        }
        Ingrediente ingrediente = ingredientes.bloquearPorIds(List.of(ingredienteId)).stream().findFirst()
                .orElseThrow(() -> NegocioException.noEncontrado("INGREDIENTE_NO_EXISTE",
                        "No existe el ingrediente " + ingredienteId));
        ingrediente.sumar(cantidad);
        movimientos.save(new MovimientoInventario(ingrediente, TipoMovimiento.ENTRADA, cantidad, null, usuarioId,
                observacion));
        eventos.publishEvent(new Eventos.MenuActualizado());
        return IngredienteDto.de(ingrediente);
    }
}
