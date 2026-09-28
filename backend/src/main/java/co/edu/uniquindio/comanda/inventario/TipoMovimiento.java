package co.edu.uniquindio.comanda.inventario;

public enum TipoMovimiento {
    /** Llegada de mercancía (RF-17). */
    ENTRADA,
    /** Descuento automático al confirmar un pedido (F-06). */
    SALIDA_PEDIDO,
    /** Devolución al anular un pedido que no se alcanzó a preparar (F-13). */
    REINTEGRO
}
