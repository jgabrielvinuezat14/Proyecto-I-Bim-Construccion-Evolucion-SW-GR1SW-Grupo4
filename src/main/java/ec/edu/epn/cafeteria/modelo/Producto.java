package ec.edu.epn.cafeteria.modelo;

import java.math.BigDecimal;

/**
 * Producto del menú de la cafetería (alimento o bebida).
 * Se muestra al cliente solo si está disponible y tiene al menos una unidad.
 */
public class Producto {

    private String codigo;
    private String nombre;
    private BigDecimal precio;
    private boolean disponible;
    private int cantidadDisponible;

    /** Crea un producto vacío. */
    public Producto() {
    }

    /**
     * Crea un producto con todos sus datos.
     *
     * @param codigo             código del producto, por ejemplo «P001»
     * @param nombre             nombre que ve el cliente
     * @param precio             precio vigente con dos decimales
     * @param disponible         {@code true} si el personal lo marcó como disponible
     * @param cantidadDisponible unidades que se pueden pedir
     */
    public Producto(String codigo, String nombre, BigDecimal precio, boolean disponible, int cantidadDisponible) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.disponible = disponible;
        this.cantidadDisponible = cantidadDisponible;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }
}
