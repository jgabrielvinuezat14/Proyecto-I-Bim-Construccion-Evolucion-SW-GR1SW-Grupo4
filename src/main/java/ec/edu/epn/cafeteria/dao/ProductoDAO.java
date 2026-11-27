package ec.edu.epn.cafeteria.dao;

import ec.edu.epn.cafeteria.config.ConexionBD;
import ec.edu.epn.cafeteria.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operaciones de consulta y actualización de productos en la tabla dbo.Producto.
 */
public class ProductoDAO {

    private static final String COLUMNAS = "codigo, nombre, precio, cantidad_disponible, disponible";

    private final ConexionBD conexionBD;

    /** Crea el DAO con la conexión por defecto. */
    public ProductoDAO() {
        this(new ConexionBD());
    }

    /**
     * Crea el DAO con una fuente de conexiones concreta.
     *
     * @param conexionBD proveedor de conexiones
     */
    public ProductoDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /**
     * Lista los productos que el cliente puede ver: disponibles y con al menos una unidad (HU-01).
     *
     * @return productos ordenados por nombre
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listarDisponibles() throws SQLException {
        return listar("SELECT " + COLUMNAS + " FROM dbo.Producto"
                + " WHERE disponible = 1 AND cantidad_disponible > 0 ORDER BY nombre");
    }

    /**
     * Lista todos los productos, incluidos los no disponibles (HU-06).
     *
     * @return productos ordenados por código
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listarTodos() throws SQLException {
        return listar("SELECT " + COLUMNAS + " FROM dbo.Producto ORDER BY codigo");
    }

    /**
     * Busca un producto por su código.
     *
     * @param codigo código del producto, por ejemplo «P001»
     * @return el producto, o vacío si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<Producto> buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Producto WHERE codigo = ?";
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Registra un producto nuevo.
     *
     * @param producto producto con todos sus datos
     * @throws SQLException si el código ya existe o falla la inserción
     */
    public void guardar(Producto producto) throws SQLException {
        String sql = "INSERT INTO dbo.Producto (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setBigDecimal(3, producto.getPrecio());
            ps.setInt(4, producto.getCantidadDisponible());
            ps.setBoolean(5, producto.isDisponible());
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza nombre, precio, unidades y disponibilidad de un producto existente.
     *
     * @param producto producto con los datos nuevos
     * @throws SQLException si falla la actualización
     */
    public void actualizar(Producto producto) throws SQLException {
        String sql = "UPDATE dbo.Producto SET nombre = ?, precio = ?, cantidad_disponible = ?, disponible = ?"
                + " WHERE codigo = ?";
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setBigDecimal(2, producto.getPrecio());
            ps.setInt(3, producto.getCantidadDisponible());
            ps.setBoolean(4, producto.isDisponible());
            ps.setString(5, producto.getCodigo());
            ps.executeUpdate();
        }
    }

    /**
     * Descuenta unidades solo si el producto sigue disponible y tiene unidades suficientes.
     * Se ejecuta dentro de la transacción de confirmación del pedido (RNF-03, ADR-04).
     *
     * @param codigo   código del producto
     * @param cantidad unidades a descontar
     * @param conexion conexión de la transacción en curso; no se cierra aquí
     * @return {@code true} si se descontaron las unidades; {@code false} si no había suficientes
     * @throws SQLException si falla la actualización
     */
    public boolean descontarUnidades(String codigo, int cantidad, Connection conexion) throws SQLException {
        String sql = "UPDATE dbo.Producto SET cantidad_disponible = cantidad_disponible - ?"
                + " WHERE codigo = ? AND disponible = 1 AND cantidad_disponible >= ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setString(2, codigo);
            ps.setInt(3, cantidad);
            return ps.executeUpdate() == 1;
        }
    }

    private List<Producto> listar(String sql) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapear(rs));
            }
        }
        return productos;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"),
                rs.getBoolean("disponible"), rs.getInt("cantidad_disponible"));
    }
}
