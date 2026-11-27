package ec.edu.epn.cafeteria.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Producto del menú de la cafetería (alimento o bebida).
 * Se muestra al cliente solo si está disponible y tiene al menos una unidad.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-01 Consultar menú: el cliente ve solo productos disponibles con unidades.</li>
 * <li>HU-02 Agregar productos: la cantidad pedida no puede superar {@code cantidadDisponible}.</li>
 * <li>HU-06 Administrar menú: el personal registra, edita y cambia la disponibilidad.</li>
 * <li>RN-02 y RN-NEG-02: solo se ofrecen productos disponibles y con existencias.</li>
 * <li>RN-NEG-03 y RNF-02: el precio se maneja con {@link java.math.BigDecimal} y dos decimales.</li>
 * <li>Tabla dbo.Producto del modelo de datos.</li>
 * </ul>
 */
public class Producto implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * Devuelve el código del producto.
     *
     * @return el código del producto
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Asigna el código del producto.
     *
     * @param codigo el código del producto
     */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve el nombre del producto.
     *
     * @return el nombre del producto
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna el nombre del producto.
     *
     * @param nombre el nombre del producto
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve el precio del producto.
     *
     * @return el precio del producto
     */
    public BigDecimal getPrecio() {
        return precio;
    }

    /**
     * Asigna el precio del producto.
     *
     * @param precio el precio del producto
     */
    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    /**
     * Devuelve la disponibilidad del producto.
     *
     * @return la disponibilidad del producto
     */
    public boolean isDisponible() {
        return disponible;
    }

    /**
     * Asigna la disponibilidad del producto.
     *
     * @param disponible la disponibilidad del producto
     */
    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    /**
     * Devuelve la cantidad disponible del producto.
     *
     * @return la cantidad disponible del producto
     */
    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    /**
     * Asigna la cantidad disponible del producto.
     *
     * @param cantidadDisponible la cantidad disponible del producto
     */
    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }
}
