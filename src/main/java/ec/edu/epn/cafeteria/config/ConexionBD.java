package ec.edu.epn.cafeteria.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Proporciona las conexiones JDBC a SQL Server que usan las clases de acceso a datos.
 * Los datos de conexión se leen de las variables DB_URL, DB_USER y DB_PASSWORD
 * (variables de entorno o propiedades del sistema), nunca del código fuente.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>RNF-01 Persistencia: todos los datos se guardan en SQL Server.</li>
 * <li>RNF-04 Seguridad básica: las credenciales no se escriben en el código.</li>
 * <li>ADR-02: SQL Server ejecutado en Docker.</li>
 * <li>ADR-03: acceso a datos con DAO y JDBC.</li>
 * <li>ADR-04: entrega la conexión que comparte la transacción de confirmación.</li>
 * </ul>
 */
public class ConexionBD {

    private static final String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    /**
     * Abre una conexión nueva a la base CafeteriaDB.
     * Quien la recibe debe cerrarla (por ejemplo, con try-with-resources).
     * <p>Trazabilidad: RNF-01, ADR-03.</p>
     *
     * @return conexión abierta con autocommit activado
     * @throws SQLException          si SQL Server no responde o las credenciales son incorrectas
     * @throws IllegalStateException si falta alguna variable de conexión
     */
    public Connection obtenerConexion() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró el controlador JDBC de SQL Server.", e);
        }
        return DriverManager.getConnection(leer("DB_URL"), leer("DB_USER"), leer("DB_PASSWORD"));
    }

    private static String leer(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(nombre);
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la variable " + nombre
                    + ". Configúrela como indica el manual de instalación.");
        }
        return valor;
    }
}
