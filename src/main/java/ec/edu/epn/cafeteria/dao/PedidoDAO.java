package ec.edu.epn.cafeteria.dao;

import ec.edu.epn.cafeteria.config.ConexionBD;
import ec.edu.epn.cafeteria.modelo.Cliente;
import ec.edu.epn.cafeteria.modelo.DetallePedido;
import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.modelo.Pedido;
import ec.edu.epn.cafeteria.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registro y consulta de pedidos (dbo.Pedido y dbo.DetallePedido) y actualización de su estado.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-04: guarda el pedido y sus detalles dentro de la transacción de confirmación (ADR-04).</li>
 * <li>HU-05 y HU-07: consultas por código de retiro, por cliente y en orden de llegada (RN-03).</li>
 * <li>HU-08: cambio de estado condicionado al estado anterior (RN-NEG-06, RNF-03).</li>
 * <li>RN-NEG-05: código de retiro único.</li>
 * <li>RNF-01 y ADR-03: persistencia con DAO y JDBC.</li>
 * </ul>
 */
public class PedidoDAO {

    private static final String SELECT_PEDIDO =
            "SELECT p.id_pedido, p.codigo_retiro, p.fecha_registro, p.estado, p.total,"
            + " c.codigo_cliente, c.nombre, c.apellido"
            + " FROM dbo.Pedido p JOIN dbo.Cliente c ON c.codigo_cliente = p.codigo_cliente";

    private final ConexionBD conexionBD;

    /** Crea el DAO con la conexión por defecto. */
    public PedidoDAO() {
        this(new ConexionBD());
    }

    /**
     * Crea el DAO con una fuente de conexiones concreta.
     *
     * @param conexionBD proveedor de conexiones
     */
    public PedidoDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /**
     * Inserta el pedido y sus detalles con la conexión de la transacción de confirmación.
     * Asigna al objeto el identificador generado por la base de datos.
     * <p>Trazabilidad: HU-04, ADR-04.</p>
     *
     * @param pedido   pedido con cliente, código de retiro, estado, total y detalles
     * @param conexion conexión de la transacción en curso; no se cierra aquí
     * @throws SQLException si el código de retiro ya existe o falla la inserción
     */
    public void guardar(Pedido pedido, Connection conexion) throws SQLException {
        String sqlPedido = "INSERT INTO dbo.Pedido (codigo_retiro, codigo_cliente, fecha_registro, estado, total)"
                + " OUTPUT inserted.id_pedido VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sqlPedido)) {
            ps.setString(1, pedido.getCodigoRetiro());
            ps.setString(2, pedido.getCliente().getCodigo());
            ps.setTimestamp(3, Timestamp.valueOf(pedido.getFechaRegistro()));
            ps.setString(4, pedido.getEstado().name());
            ps.setBigDecimal(5, pedido.getTotal());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                pedido.setId(rs.getLong(1));
            }
        }
        String sqlDetalle = "INSERT INTO dbo.DetallePedido (id_pedido, codigo_producto, cantidad, precio_unitario)"
                + " OUTPUT inserted.id_detalle VALUES (?, ?, ?, ?)";
        for (DetallePedido detalle : pedido.getDetalles()) {
            try (PreparedStatement ps = conexion.prepareStatement(sqlDetalle)) {
                ps.setLong(1, pedido.getId());
                ps.setString(2, detalle.getProducto().getCodigo());
                ps.setInt(3, detalle.getCantidad());
                ps.setBigDecimal(4, detalle.getPrecioUnitario());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    detalle.setId(rs.getLong(1));
                }
            }
        }
    }

    /**
     * Busca un pedido por su código de retiro e incluye sus detalles.
     * <p>Trazabilidad: HU-05, HU-08.</p>
     *
     * @param codigo código de retiro de 6 caracteres
     * @return el pedido con sus detalles, o vacío si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<Pedido> buscarPorCodigoRetiro(String codigo) throws SQLException {
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_PEDIDO + " WHERE p.codigo_retiro = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Pedido pedido = mapear(rs);
                pedido.setDetalles(cargarDetalles(cn, pedido.getId()));
                return Optional.of(pedido);
            }
        }
    }

    /**
     * Lista los pedidos de un cliente, del más reciente al más antiguo (sin detalles).
     * <p>Trazabilidad: HU-05.</p>
     *
     * @param codigoCliente código del cliente
     * @return pedidos del cliente
     * @throws SQLException si falla la consulta
     */
    public List<Pedido> listarPorCliente(String codigoCliente) throws SQLException {
        return listar(SELECT_PEDIDO + " WHERE p.codigo_cliente = ? ORDER BY p.fecha_registro DESC", codigoCliente);
    }

    /**
     * Lista todos los pedidos en orden de llegada (sin detalles), para el personal (HU-07).
     * <p>Trazabilidad: HU-07, RN-03.</p>
     *
     * @return todos los pedidos
     * @throws SQLException si falla la consulta
     */
    public List<Pedido> listarTodos() throws SQLException {
        return listar(SELECT_PEDIDO + " ORDER BY p.fecha_registro, p.id_pedido", null);
    }

    /**
     * Cambia el estado de un pedido solo si su estado actual es el esperado.
     * Así dos usuarios no pueden avanzar el mismo pedido dos veces.
     * <p>Trazabilidad: HU-08, RN-NEG-06, RNF-03.</p>
     *
     * @param id       identificador del pedido
     * @param anterior estado que debe tener el pedido
     * @param nuevo    estado que se registra
     * @return {@code true} si se actualizó; {@code false} si el pedido no estaba en el estado esperado
     * @throws SQLException si falla la actualización
     */
    public boolean actualizarEstado(long id, EstadoPedido anterior, EstadoPedido nuevo) throws SQLException {
        String sql = "UPDATE dbo.Pedido SET estado = ? WHERE id_pedido = ? AND estado = ?";
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevo.name());
            ps.setLong(2, id);
            ps.setString(3, anterior.name());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Ejecuta una consulta de pedidos con un parámetro opcional.
     *
     * @param sql consulta de pedidos
     * @param parametro valor del parámetro, o {@code null} si no tiene
     * @return los pedidos encontrados
     * @throws SQLException si falla la consulta
     */
    private List<Pedido> listar(String sql, String parametro) throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setString(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(mapear(rs));
                }
            }
        }
        return pedidos;
    }

    /**
     * Carga los detalles de un pedido con su producto y el precio unitario registrado.
     * <p>Trazabilidad: HU-05, HU-07, RN-NEG-04.</p>
     *
     * @param cn conexión abierta
     * @param idPedido identificador del pedido
     * @return los detalles del pedido
     * @throws SQLException si falla la consulta
     */
    private List<DetallePedido> cargarDetalles(Connection cn, long idPedido) throws SQLException {
        String sql = "SELECT d.id_detalle, d.cantidad, d.precio_unitario, pr.codigo, pr.nombre, pr.precio,"
                + " pr.disponible, pr.cantidad_disponible"
                + " FROM dbo.DetallePedido d JOIN dbo.Producto pr ON pr.codigo = d.codigo_producto"
                + " WHERE d.id_pedido = ? ORDER BY d.id_detalle";
        List<DetallePedido> detalles = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto producto = new Producto(rs.getString("codigo"), rs.getString("nombre"),
                            rs.getBigDecimal("precio"), rs.getBoolean("disponible"), rs.getInt("cantidad_disponible"));
                    DetallePedido detalle = new DetallePedido();
                    detalle.setId(rs.getLong("id_detalle"));
                    detalle.setProducto(producto);
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    /**
     * Convierte una fila de dbo.Pedido y dbo.Cliente en un objeto {@link Pedido}.
     *
     * @param rs fila actual del resultado
     * @return el pedido leído
     * @throws SQLException si falla la consulta
     */
    private Pedido mapear(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setCodigo(rs.getString("codigo_cliente"));
        cliente.setNombre(rs.getString("nombre"));
        cliente.setApellido(rs.getString("apellido"));
        Pedido pedido = new Pedido();
        pedido.setId(rs.getLong("id_pedido"));
        pedido.setCodigoRetiro(rs.getString("codigo_retiro"));
        pedido.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
        pedido.setEstado(EstadoPedido.valueOf(rs.getString("estado")));
        pedido.setTotal(rs.getBigDecimal("total"));
        pedido.setCliente(cliente);
        return pedido;
    }
}
