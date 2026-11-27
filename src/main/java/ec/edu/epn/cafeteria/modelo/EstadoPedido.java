package ec.edu.epn.cafeteria.modelo;

/**
 * Estados por los que pasa un pedido confirmado (RN-NEG-06).
 * El orden permitido es PENDIENTE, EN_PREPARACION, LISTO_PARA_RETIRAR y ENTREGADO.
 */
public enum EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    LISTO_PARA_RETIRAR,
    ENTREGADO
}
