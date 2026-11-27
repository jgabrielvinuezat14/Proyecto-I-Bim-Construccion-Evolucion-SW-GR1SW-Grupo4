package ec.edu.epn.cafeteria.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Producto incluido en un pedido con su cantidad y el precio unitario aplicado.
 * El precio unitario se conserva aunque después cambie el precio del menú (RN-NEG-04).
 */
public class DetallePedido {

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}
