package ec.edu.epn.cafeteria.modelo;

/**
 * Estados por los que pasa un pedido confirmado (RN-NEG-06).
 * El orden permitido es PENDIENTE, EN_PREPARACION, LISTO_PARA_RETIRAR y ENTREGADO.
 */
public enum EstadoPedido {
    /** Pedido confirmado que aún no se empieza a preparar. */
    PENDIENTE,
    /** Pedido que el personal está preparando. */
    EN_PREPARACION,
    /** Pedido listo; el cliente puede retirarlo presentando su código. */
    LISTO_PARA_RETIRAR,
    /** Pedido entregado al cliente después de verificar su código. */
    ENTREGADO
}
