package ec.edu.epn.cafeteria.dao;

import ec.edu.epn.cafeteria.config.ConexionBD;
import ec.edu.epn.cafeteria.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Operaciones de consulta y registro de clientes universitarios en la tabla dbo.Cliente (HU-09).
 */
public class ClienteDAO {

    private static final String COLUMNAS =
            "codigo_cliente, nombre, apellido, cedula, celular, correo_institucional, contrasena_hash";

    private final ConexionBD conexionBD;

    /** Crea el DAO con la conexión por defecto. */
    public ClienteDAO() {
        this(new ConexionBD());
    }

    /**
     * Crea el DAO con una fuente de conexiones concreta.
     *
     * @param conexionBD proveedor de conexiones
     */
    public ClienteDAO(ConexionBD conexionBD) {
        this.conexionBD = conexionBD;
    }

    /**
     * Busca un cliente por su correo institucional.
     *
     * @param correoInstitucional correo institucional del cliente
     * @return el cliente encontrado, o vacío si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorCorreo(String correoInstitucional) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Cliente WHERE correo_institucional = ?";
        return buscarUnico(sql, correoInstitucional);
    }

    /**
     * Busca un cliente por su número de cédula.
     *
     * @param cedula cédula de 10 dígitos
     * @return el cliente encontrado, o vacío si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorCedula(String cedula) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Cliente WHERE cedula = ?";
        return buscarUnico(sql, cedula);
    }

    /**
     * Busca un cliente por su código único.
     *
     * @param codigo código del cliente, por ejemplo «C001»
     * @return el cliente encontrado, o vacío si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM dbo.Cliente WHERE codigo_cliente = ?";
        return buscarUnico(sql, codigo);
    }

    /**
     * Registra un cliente nuevo en la base de datos. Si el cliente no trae código asignado,
     * SQL Server lo genera mediante la secuencia {@code dbo.seq_cliente} y se asigna al objeto.
     *
     * @param cliente cliente con sus datos y hash BCrypt de contraseña
     * @return el mismo cliente con su código asignado
     * @throws SQLException si existe duplicado o falla la inserción
     */
    public Cliente guardar(Cliente cliente) throws SQLException {
        if (cliente.getCodigo() == null || cliente.getCodigo().isBlank()) {
            String sql = "INSERT INTO dbo.Cliente"
                    + " (nombre, apellido, cedula, celular, correo_institucional, contrasena_hash)"
                    + " OUTPUT INSERTED.codigo_cliente"
                    + " VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection cn = conexionBD.obtenerConexion();
                 PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, cliente.getNombre());
                ps.setString(2, cliente.getApellido());
                ps.setString(3, cliente.getCedula());
                ps.setString(4, cliente.getCelular());
                ps.setString(5, cliente.getCorreoInstitucional());
                ps.setString(6, cliente.getContrasenaHash());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cliente.setCodigo(rs.getString(1));
                    }
                }
            }
            return cliente;
        }

        String sql = "INSERT INTO dbo.Cliente (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, cliente.getCodigo());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getApellido());
            ps.setString(4, cliente.getCedula());
            ps.setString(5, cliente.getCelular());
            ps.setString(6, cliente.getCorreoInstitucional());
            ps.setString(7, cliente.getContrasenaHash());
            ps.executeUpdate();
        }
        return cliente;
    }

    private Optional<Cliente> buscarUnico(String sql, String parametro) throws SQLException {
        try (Connection cn = conexionBD.obtenerConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setCodigo(rs.getString("codigo_cliente"));
        cliente.setNombre(rs.getString("nombre"));
        cliente.setApellido(rs.getString("apellido"));
        cliente.setCedula(rs.getString("cedula"));
        cliente.setCelular(rs.getString("celular"));
        cliente.setCorreoInstitucional(rs.getString("correo_institucional"));
        cliente.setContrasenaHash(rs.getString("contrasena_hash"));
        return cliente;
    }
}
