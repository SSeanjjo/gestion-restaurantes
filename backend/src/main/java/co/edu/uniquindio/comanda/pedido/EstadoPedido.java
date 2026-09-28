package co.edu.uniquindio.comanda.pedido;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida del pedido (RD-03 y diagrama de estados de la clase Pedido):
 *
 * <pre>
 * REGISTRADO → EN_PREPARACION → LISTO_PARA_SERVIR → ENTREGADO → FACTURADO
 *      └────→ CANCELADO
 * </pre>
 *
 * No se permiten saltos ni retrocesos.
 */
public enum EstadoPedido {
    REGISTRADO,
    EN_PREPARACION,
    LISTO_PARA_SERVIR,
    ENTREGADO,
    FACTURADO,
    CANCELADO;

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            REGISTRADO, EnumSet.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, EnumSet.of(LISTO_PARA_SERVIR),
            LISTO_PARA_SERVIR, EnumSet.of(ENTREGADO),
            ENTREGADO, EnumSet.of(FACTURADO),
            FACTURADO, EnumSet.noneOf(EstadoPedido.class),
            CANCELADO, EnumSet.noneOf(EstadoPedido.class));

    /** Estados en los que el pedido ocupa la mesa. */
    public static final Set<EstadoPedido> ACTIVOS = EnumSet.of(REGISTRADO, EN_PREPARACION, LISTO_PARA_SERVIR,
            ENTREGADO);

    /** Estados que se muestran en la pantalla de cocina (IU-02). */
    public static final Set<EstadoPedido> EN_COCINA = EnumSet.of(REGISTRADO, EN_PREPARACION, LISTO_PARA_SERVIR);

    public boolean puedePasarA(EstadoPedido destino) {
        return TRANSICIONES.get(this).contains(destino);
    }
}
