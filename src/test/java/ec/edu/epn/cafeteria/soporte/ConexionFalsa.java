package ec.edu.epn.cafeteria.soporte;

import ec.edu.epn.cafeteria.config.ConexionBD;

import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Conexión de prueba: registra si la transacción se confirmó o se deshizo,
 * sin conectarse a SQL Server.
 */
public class ConexionFalsa extends ConexionBD {

    public int confirmaciones;
    public int reversiones;

    @Override
    public Connection obtenerConexion() {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, metodo, argumentos) -> {
                    switch (metodo.getName()) {
                        case "commit" -> confirmaciones++;
                        case "rollback" -> reversiones++;
                        case "getAutoCommit", "isClosed" -> {
                            return false;
                        }
                        default -> {
                            // setAutoCommit y close no necesitan hacer nada en las pruebas
                        }
                    }
                    return null;
                });
    }
}
