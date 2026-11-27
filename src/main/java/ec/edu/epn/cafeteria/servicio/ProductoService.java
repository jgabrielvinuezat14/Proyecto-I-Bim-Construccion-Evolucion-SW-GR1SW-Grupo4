package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.dao.ProductoDAO;
import ec.edu.epn.cafeteria.modelo.Producto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Consulta y administración de los productos del menú.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-01 Consultar menú: {@link #consultarDisponibles()}.</li>
 * <li>HU-06 Administrar menú: registrar, actualizar y cambiar disponibilidad.</li>
 * <li>RN-02 y RN-NEG-02: solo se ofrecen productos disponibles con unidades.</li>
 * <li>RN-NEG-03 y RNF-02: precios mayores que cero con dos decimales.</li>
 * <li>RN-NEG-04: cambiar el precio no altera pedidos ya confirmados.</li>
 * <li>RNF-01 y RNF-03.</li>
 * </ul>
 */
public class ProductoService {

    static final String MSG_PRECIO = "El precio ingresado no es válido.";
    static final String MSG_UNIDADES = "Las unidades deben ser un número entero mayor o igual a cero.";
    static final String MSG_CODIGO = "El código es obligatorio y debe tener hasta 10 caracteres.";
    static final String MSG_NOMBRE = "El nombre del producto es obligatorio.";

    private final ProductoDAO productoDAO;

    /** Crea el servicio con el DAO por defecto. */
    public ProductoService() {
        this(new ProductoDAO());
    }

    /**
     * Crea el servicio con un DAO concreto.
     *
     * @param productoDAO acceso a datos de productos
     */
    public ProductoService(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    /**
     * Consulta el menú que ve el cliente: productos disponibles con al menos una unidad (HU-01).
     * <p>Trazabilidad: HU-01, RN-NEG-02.</p>
     *
     * @return productos disponibles
     */
    public List<Producto> consultarDisponibles() {
        try {
            return productoDAO.listarDisponibles();
        } catch (SQLException e) {
            throw errorBD(e);
        }
    }

    /**
     * Consulta todos los productos para la administración del menú (HU-06).
     * <p>Trazabilidad: HU-06.</p>
     *
     * @return todos los productos
     */
    public List<Producto> consultarTodos() {
        try {
            return productoDAO.listarTodos();
        } catch (SQLException e) {
            throw errorBD(e);
        }
    }

    /**
     * Registra un producto nuevo después de validar sus datos (CA-06.1 y CA-06.3).
     * <p>Trazabilidad: HU-06, RN-NEG-03, RNF-01.</p>
     *
     * @param producto producto a registrar
     * @throws IllegalArgumentException si algún dato es inválido o el código ya existe
     */
    public void registrarProducto(Producto producto) {
        validar(producto);
        try {
            if (productoDAO.buscarPorCodigo(producto.getCodigo()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un producto con el código " + producto.getCodigo() + ".");
            }
            productoDAO.guardar(producto);
        } catch (SQLException e) {
            throw errorBD(e);
        }
    }

    /**
     * Actualiza precio, unidades y disponibilidad de un producto existente (CA-06.2).
     * Los pedidos ya confirmados conservan su precio (RN-NEG-04).
     * <p>Trazabilidad: HU-06, RN-NEG-04.</p>
     *
     * @param producto producto con los datos nuevos
     * @throws IllegalArgumentException si algún dato es inválido o el producto no existe
     */
    public void actualizarProducto(Producto producto) {
        validar(producto);
        try {
            if (productoDAO.buscarPorCodigo(producto.getCodigo()).isEmpty()) {
                throw new IllegalArgumentException("No existe el producto " + producto.getCodigo() + ".");
            }
            productoDAO.actualizar(producto);
        } catch (SQLException e) {
            throw errorBD(e);
        }
    }

    /**
     * Habilita o deshabilita un producto sin cambiar sus demás datos.
     * <p>Trazabilidad: HU-06, RN-NEG-02.</p>
     *
     * @param codigo     código del producto
     * @param disponible nuevo estado de disponibilidad
     * @throws IllegalArgumentException si el producto no existe
     */
    public void cambiarDisponibilidad(String codigo, boolean disponible) {
        try {
            Optional<Producto> encontrado = productoDAO.buscarPorCodigo(codigo);
            Producto producto = encontrado.orElseThrow(
                    () -> new IllegalArgumentException("No existe el producto " + codigo + "."));
            producto.setDisponible(disponible);
            productoDAO.actualizar(producto);
        } catch (SQLException e) {
            throw errorBD(e);
        }
    }

    private void validar(Producto producto) {
        String codigo = producto.getCodigo();
        if (codigo == null || codigo.isBlank() || codigo.length() > 10) {
            throw new IllegalArgumentException(MSG_CODIGO);
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new IllegalArgumentException(MSG_NOMBRE);
        }
        BigDecimal precio = producto.getPrecio();
        if (precio == null || precio.signum() <= 0 || precio.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(MSG_PRECIO);
        }
        if (producto.getCantidadDisponible() < 0) {
            throw new IllegalArgumentException(MSG_UNIDADES);
        }
    }

    private IllegalStateException errorBD(SQLException e) {
        return new IllegalStateException("No se pudo acceder a la base de datos. Intente de nuevo.", e);
    }
}
