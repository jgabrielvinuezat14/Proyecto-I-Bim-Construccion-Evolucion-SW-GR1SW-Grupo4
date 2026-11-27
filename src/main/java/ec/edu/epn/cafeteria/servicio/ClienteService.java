package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.dao.ClienteDAO;
import ec.edu.epn.cafeteria.modelo.Cliente;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Registro e inicio de sesión de clientes universitarios.
 * Las contraseñas se almacenan únicamente como hash BCrypt.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-09 Registro e inicio de sesión del cliente.</li>
 * <li>RNF-01: el cliente se guarda en la base de datos.</li>
 * <li>RNF-04: validación de datos, cédula y correo únicos, contraseña con BCrypt.</li>
 * <li>ADR-05: el código de cliente lo asigna una secuencia de la base de datos.</li>
 * <li>ADR-01: lógica de negocio en la capa de servicio.</li>
 * </ul>
 */
public class ClienteService {

    static final String MSG_CAMPOS_OBLIGATORIOS = "Todos los campos son obligatorios.";
    static final String MSG_LONGITUD_DATOS = "Uno o más campos superan la longitud máxima permitida.";
    static final String MSG_CEDULA = "La cédula debe contener exactamente 10 dígitos numéricos.";
    static final String MSG_CELULAR = "El celular debe contener exactamente 10 dígitos numéricos.";
    static final String MSG_CORREO = "El correo institucional ingresado no es válido.";
    static final String MSG_CONTRASENA_LARGA = "La contraseña supera la longitud máxima permitida.";
    static final String MSG_CORREO_DUPLICADO = "Ya existe una cuenta registrada con ese correo institucional.";
    static final String MSG_CEDULA_DUPLICADA = "Ya existe un cliente registrado con esa cédula.";
    static final String MSG_CREDENCIALES_OBLIGATORIAS = "Debe ingresar su correo institucional y contraseña.";
    static final String MSG_CREDENCIALES_INVALIDAS = "Correo institucional o contraseña incorrectos.";

    private static final Pattern DIEZ_DIGITOS = Pattern.compile("^\\d{10}$");
    private static final Pattern FORMATO_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int COSTO_BCRYPT = 10;
    private static final int MAX_BYTES_CONTRASENA = 72;

    private final ClienteDAO clienteDAO;

    /** Crea el servicio con el DAO por defecto. */
    public ClienteService() {
        this(new ClienteDAO());
    }

    /**
     * Crea el servicio con un DAO concreto.
     *
     * @param clienteDAO acceso a datos de clientes
     */
    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    /**
     * Valida los datos del cliente, verifica que no existan duplicados de correo o cédula,
     * genera el hash BCrypt de la contraseña y registra al cliente (HU-09, RNF-04).
     * <p>Trazabilidad: HU-09, RNF-04, ADR-05.</p>
     *
     * @param datos      datos personales del cliente
     * @param contrasena contraseña en texto plano ingresada en el formulario
     * @return cliente registrado con su código único asignado
     * @throws IllegalArgumentException si algún campo es inválido o ya está registrado
     */
    public Cliente registrarCliente(Cliente datos, String contrasena) {
        if (datos == null) {
            throw new IllegalArgumentException(MSG_CAMPOS_OBLIGATORIOS);
        }
        return registrarCliente(
                datos.getNombre(),
                datos.getApellido(),
                datos.getCedula(),
                datos.getCelular(),
                datos.getCorreoInstitucional(),
                contrasena
        );
    }

    /**
     * Registra un cliente nuevo a partir de sus datos individuales (HU-09, RNF-04).
     * <p>Trazabilidad: HU-09, RNF-04.</p>
     *
     * @param nombre              nombre del cliente
     * @param apellido            apellido del cliente
     * @param cedula              cédula de 10 dígitos
     * @param celular             celular de 10 dígitos
     * @param correoInstitucional correo institucional único
     * @param contrasena          contraseña en texto plano
     * @return cliente registrado con su código único asignado
     * @throws IllegalArgumentException si algún dato es inválido o existe duplicado
     */
    public Cliente registrarCliente(String nombre, String apellido, String cedula, String celular,
                                    String correoInstitucional, String contrasena) {
        if (esVacio(nombre) || esVacio(apellido) || esVacio(cedula)
                || esVacio(celular) || esVacio(correoInstitucional) || esVacio(contrasena)) {
            throw new IllegalArgumentException(MSG_CAMPOS_OBLIGATORIOS);
        }

        String nombreLimpio = nombre.trim();
        String apellidoLimpio = apellido.trim();
        String cedulaLimpia = cedula.trim();
        String celularLimpio = celular.trim();
        String correoLimpio = correoInstitucional.trim().toLowerCase(Locale.ROOT);

        if (nombreLimpio.length() > 60 || apellidoLimpio.length() > 60 || correoLimpio.length() > 120) {
            throw new IllegalArgumentException(MSG_LONGITUD_DATOS);
        }
        if (!DIEZ_DIGITOS.matcher(cedulaLimpia).matches()) {
            throw new IllegalArgumentException(MSG_CEDULA);
        }
        if (!DIEZ_DIGITOS.matcher(celularLimpio).matches()) {
            throw new IllegalArgumentException(MSG_CELULAR);
        }
        if (!FORMATO_CORREO.matcher(correoLimpio).matches()) {
            throw new IllegalArgumentException(MSG_CORREO);
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENA) {
            throw new IllegalArgumentException(MSG_CONTRASENA_LARGA);
        }

        try {
            if (clienteDAO.buscarPorCorreo(correoLimpio).isPresent()) {
                throw new IllegalArgumentException(MSG_CORREO_DUPLICADO);
            }
            if (clienteDAO.buscarPorCedula(cedulaLimpia).isPresent()) {
                throw new IllegalArgumentException(MSG_CEDULA_DUPLICADA);
            }

            Cliente nuevo = new Cliente();
            nuevo.setNombre(nombreLimpio);
            nuevo.setApellido(apellidoLimpio);
            nuevo.setCedula(cedulaLimpia);
            nuevo.setCelular(celularLimpio);
            nuevo.setCorreoInstitucional(correoLimpio);
            nuevo.setContrasenaHash(BCrypt.hashpw(contrasena, BCrypt.gensalt(COSTO_BCRYPT)));
            return clienteDAO.guardar(nuevo);
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) {
                throw new IllegalArgumentException(MSG_CORREO_DUPLICADO, e);
            }
            throw errorBD(e);
        }
    }

    /**
     * Autentica a un cliente verificando su correo institucional y su contraseña contra
     * el hash BCrypt almacenado. Ante cualquier discrepancia devuelve un mensaje genérico (HU-09).
     * <p>Trazabilidad: HU-09, RNF-04.</p>
     *
     * @param correoInstitucional correo institucional ingresado
     * @param contrasena          contraseña ingresada
     * @return cliente autenticado
     * @throws IllegalArgumentException si faltan credenciales o son incorrectas
     */
    public Cliente iniciarSesion(String correoInstitucional, String contrasena) {
        if (esVacio(correoInstitucional) || esVacio(contrasena)) {
            throw new IllegalArgumentException(MSG_CREDENCIALES_OBLIGATORIAS);
        }
        String correoLimpio = correoInstitucional.trim().toLowerCase(Locale.ROOT);
        try {
            Optional<Cliente> encontrado = clienteDAO.buscarPorCorreo(correoLimpio);
            if (encontrado.isEmpty() || !verificarContrasena(contrasena, encontrado.get().getContrasenaHash())) {
                throw new IllegalArgumentException(MSG_CREDENCIALES_INVALIDAS);
            }
            return encontrado.get();
        } catch (SQLException e) {
            throw errorBD(e);
        }
    }

    /**
     * Comprueba la contraseña ingresada contra el hash BCrypt guardado.
     * <p>Trazabilidad: HU-09, RNF-04.</p>
     *
     * @param contrasenaPlana contraseña ingresada
     * @param hashAlmacenado hash BCrypt guardado en la base de datos
     * @return {@code true} si la contraseña es correcta
     */
    private boolean verificarContrasena(String contrasenaPlana, String hashAlmacenado) {
        if (hashAlmacenado == null || hashAlmacenado.isBlank()) {
            return false;
        }
        if (contrasenaPlana.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENA) {
            return false;
        }
        try {
            return BCrypt.checkpw(contrasenaPlana, hashAlmacenado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Indica si un texto es nulo o está en blanco.
     *
     * @param texto texto a revisar
     * @return {@code true} si está vacío
     */
    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /**
     * Crea el error que se muestra cuando falla el acceso a la base de datos.
     * <p>Trazabilidad: RNF-01.</p>
     *
     * @param e error original de SQL
     * @return excepción con un mensaje comprensible para el usuario
     */
    private IllegalStateException errorBD(SQLException e) {
        return new IllegalStateException("No se pudo acceder a la base de datos. Intente de nuevo.", e);
    }
}
