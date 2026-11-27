package ec.edu.epn.cafeteria.soporte;

import ec.edu.epn.cafeteria.dao.ClienteDAO;
import ec.edu.epn.cafeteria.modelo.Cliente;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ClienteDAO en memoria para las pruebas unitarias. Simula la secuencia dbo.seq_cliente
 * y las búsquedas por correo, cédula y código sin conectarse a SQL Server.
 */
public class ClienteDAOEnMemoria extends ClienteDAO {

    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private int secuencia = 0;

    public ClienteDAOEnMemoria() {
        super(new ConexionFalsa());
    }

    /** Agrega un cliente de prueba con código explícito y lo devuelve. */
    public Cliente agregar(String codigo, String nombre, String apellido, String cedula,
                           String celular, String correoInstitucional, String contrasenaHash) {
        Cliente c = new Cliente();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setApellido(apellido);
        c.setCedula(cedula);
        c.setCelular(celular);
        c.setCorreoInstitucional(correoInstitucional.toLowerCase(Locale.ROOT));
        c.setContrasenaHash(contrasenaHash);
        clientes.put(codigo, c);
        actualizarSecuencia(codigo);
        return copia(c);
    }

    /** Devuelve el cliente guardado (no una copia) para verificar sus datos almacenados. */
    public Cliente guardado(String codigo) {
        return clientes.get(codigo);
    }

    @Override
    public Optional<Cliente> buscarPorCorreo(String correoInstitucional) {
        if (correoInstitucional == null) {
            return Optional.empty();
        }
        return clientes.values().stream()
                .filter(c -> c.getCorreoInstitucional().equalsIgnoreCase(correoInstitucional))
                .findFirst()
                .map(this::copia);
    }

    @Override
    public Optional<Cliente> buscarPorCedula(String cedula) {
        if (cedula == null) {
            return Optional.empty();
        }
        return clientes.values().stream()
                .filter(c -> c.getCedula().equals(cedula))
                .findFirst()
                .map(this::copia);
    }

    @Override
    public Optional<Cliente> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(clientes.get(codigo)).map(this::copia);
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        if (cliente.getCodigo() == null || cliente.getCodigo().isBlank()) {
            String codigoGenerado;
            do {
                codigoGenerado = String.format("C%03d", ++secuencia);
            } while (clientes.containsKey(codigoGenerado));
            cliente.setCodigo(codigoGenerado);
        } else {
            actualizarSecuencia(cliente.getCodigo());
        }
        Cliente almacenado = copia(cliente);
        clientes.put(almacenado.getCodigo(), almacenado);
        return copia(almacenado);
    }

    private void actualizarSecuencia(String codigo) {
        if (codigo != null && codigo.startsWith("C")) {
            try {
                int numero = Integer.parseInt(codigo.substring(1));
                if (numero > secuencia) {
                    secuencia = numero;
                }
            } catch (NumberFormatException ignored) {
                // Códigos personalizados no alteran la secuencia numérica
            }
        }
    }

    private Cliente copia(Cliente origen) {
        Cliente c = new Cliente();
        c.setCodigo(origen.getCodigo());
        c.setNombre(origen.getNombre());
        c.setApellido(origen.getApellido());
        c.setCedula(origen.getCedula());
        c.setCelular(origen.getCelular());
        c.setCorreoInstitucional(origen.getCorreoInstitucional());
        c.setContrasenaHash(origen.getContrasenaHash());
        return c;
    }
}
