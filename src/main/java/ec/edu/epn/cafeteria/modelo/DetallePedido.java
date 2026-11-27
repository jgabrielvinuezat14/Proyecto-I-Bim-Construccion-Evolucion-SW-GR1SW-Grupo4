package ec.edu.epn.cafeteria.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Producto incluido en un pedido con su cantidad y el precio unitario aplicado.
 * El precio unitario se conserva aunque después cambie el precio del menú (RN-NEG-04).
 */
public class DetallePedido implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private int cantidad;
    private BigDecimal precioUnitario;
    private Producto producto;

    /** Crea un detalle vacío. */
    public DetallePedido() {
    }

    /**
     * Crea un detalle con el precio vigente del producto.
     *
     * @param producto producto solicitado
     * @param cantidad cantidad solicitada
     */
    public DetallePedido(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
    }

    /**
     * Calcula el importe de este detalle.
     *
     * @return cantidad multiplicada por el precio unitario, con dos decimales
     */
    public BigDecimal calcularSubtotal() {
        if (precioUnitario == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Devuelve el identificador del detalle del pedido.
     *
     * @return el identificador del detalle del pedido
     */
    public long getId() {
        return id;
    }

    /**
     * Asigna el identificador del detalle del pedido.
     *
     * @param id el identificador del detalle del pedido
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Devuelve la cantidad del detalle del pedido.
     *
     * @return la cantidad del detalle del pedido
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Asigna la cantidad del detalle del pedido.
     *
     * @param cantidad la cantidad del detalle del pedido
     */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Devuelve el precio unitario registrado al confirmar del detalle del pedido.
     *
     * @return el precio unitario registrado al confirmar del detalle del pedido
     */
    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    /**
     * Asigna el precio unitario registrado al confirmar del detalle del pedido.
     *
     * @param precioUnitario el precio unitario registrado al confirmar del detalle del pedido
     */
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /**
     * Devuelve el producto del detalle del pedido.
     *
     * @return el producto del detalle del pedido
     */
    public Producto getProducto() {
        return producto;
    }

    /**
     * Asigna el producto del detalle del pedido.
     *
     * @param producto el producto del detalle del pedido
     */
    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}
