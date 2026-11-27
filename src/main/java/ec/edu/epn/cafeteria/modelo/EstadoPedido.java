package ec.edu.epn.cafeteria.modelo;

/**
 * Estados por los que pasa un pedido confirmado.
 * El orden permitido es PENDIENTE, EN_PREPARACION, LISTO_PARA_RETIRAR y ENTREGADO.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-05 Consultar estado del pedido.</li>
 * <li>HU-07 Consultar pedidos por estado.</li>
 * <li>HU-08 Avanzar estado y registrar la entrega.</li>
 * <li>RN-NEG-06: el estado solo avanza en el orden indicado, sin saltos ni retrocesos.</li>
 * <li>RN-NEG-07: solo un pedido LISTO_PARA_RETIRAR puede pasar a ENTREGADO.</li>
 * </ul>
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
