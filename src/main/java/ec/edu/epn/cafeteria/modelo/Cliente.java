package ec.edu.epn.cafeteria.modelo;

/**
 * Cliente universitario (estudiante o docente) que realiza pedidos.
 * La contraseña nunca se guarda en texto plano: solo su hash BCrypt (RNF-04).
 */
public class Cliente {

    private String codigo;
    private String nombre;
    private String apellido;
    private String cedula;
    private String celular;
    private String correoInstitucional;
    private String contrasenaHash;

    /**
     * Crea un cliente vacío; sus datos se asignan con los métodos set.
     */
    public Cliente() {
    }

    /**
     * Devuelve el código del cliente.
     *
     * @return el código del cliente
     */
    public String getCodigo() {
        return codigo;
    }

    /**
     * Asigna el código del cliente.
     *
     * @param codigo el código del cliente
     */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve el nombre del cliente.
     *
     * @return el nombre del cliente
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna el nombre del cliente.
     *
     * @param nombre el nombre del cliente
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve el apellido del cliente.
     *
     * @return el apellido del cliente
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Asigna el apellido del cliente.
     *
     * @param apellido el apellido del cliente
     */
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /**
     * Devuelve la cédula del cliente.
     *
     * @return la cédula del cliente
     */
    public String getCedula() {
        return cedula;
    }

    /**
     * Asigna la cédula del cliente.
     *
     * @param cedula la cédula del cliente
     */
    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    /**
     * Devuelve el número de celular del cliente.
     *
     * @return el número de celular del cliente
     */
    public String getCelular() {
        return celular;
    }

    /**
     * Asigna el número de celular del cliente.
     *
     * @param celular el número de celular del cliente
     */
    public void setCelular(String celular) {
        this.celular = celular;
    }

    /**
     * Devuelve el correo institucional del cliente.
     *
     * @return el correo institucional del cliente
     */
    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    /**
     * Asigna el correo institucional del cliente.
     *
     * @param correoInstitucional el correo institucional del cliente
     */
    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    /**
     * Devuelve el hash BCrypt de la contraseña del cliente.
     *
     * @return el hash BCrypt de la contraseña del cliente
     */
    public String getContrasenaHash() {
        return contrasenaHash;
    }

    /**
     * Asigna el hash BCrypt de la contraseña del cliente.
     *
     * @param contrasenaHash el hash BCrypt de la contraseña del cliente
     */
    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }
}
