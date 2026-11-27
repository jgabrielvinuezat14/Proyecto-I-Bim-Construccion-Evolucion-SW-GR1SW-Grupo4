package ec.edu.epn.cafeteria.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.UnaryOperator;

/**
 * Autenticación del personal de cafetería mediante código de acceso y contraseña
 * configurados en variables de entorno (HU-10, RNF-04).
 */
public class AccesoPersonalService {

    /** Nombre de la variable de entorno con el código de acceso del personal. */
    public static final String VAR_CODIGO_ACCESO = "STAFF_ACCESS_CODE";

    /** Nombre de la variable de entorno con la contraseña del personal. */
    public static final String VAR_CONTRASENA = "STAFF_PASSWORD";

    static final String MSG_CAMPOS_OBLIGATORIOS = "Debe ingresar el código de acceso y la contraseña.";
    static final String MSG_CREDENCIALES_INVALIDAS = "Código de acceso o contraseña incorrectos.";
    static final String MSG_CONFIGURACION =
            "Falta configurar las variables " + VAR_CODIGO_ACCESO + " y " + VAR_CONTRASENA + ".";

    private final UnaryOperator<String> lectorVariables;

    /**
     * Crea el servicio leyendo las credenciales desde las variables de entorno del sistema
     * (o propiedades de la JVM si se definieron al iniciar el servidor).
     */
    public AccesoPersonalService() {
        this(AccesoPersonalService::leerVariableSistema);
    }

    /**
     * Crea el servicio con un proveedor explícito de variables de configuración.
     *
     * @param lectorVariables función que devuelve el valor configurado para cada nombre de variable
     */
    public AccesoPersonalService(UnaryOperator<String> lectorVariables) {
        this.lectorVariables = lectorVariables;
    }

    /**
     * Verifica el código de acceso y la contraseña ingresados por el personal contra
     * los valores configurados en el entorno, sin exponer cuál de los dos falló (HU-10, RNF-04).
     *
     * @param codigoAcceso código de acceso ingresado
     * @param contrasena   contraseña ingresada
     * @return {@code true} si las credenciales son válidas
     * @throws IllegalArgumentException si algún campo está vacío o las credenciales son incorrectas
     * @throws IllegalStateException    si el servidor no tiene configuradas las variables de entorno
     */
    public boolean autenticar(String codigoAcceso, String contrasena) {
        if (esVacio(codigoAcceso) || esVacio(contrasena)) {
            throw new IllegalArgumentException(MSG_CAMPOS_OBLIGATORIOS);
        }

        String codigoEsperado = lectorVariables.apply(VAR_CODIGO_ACCESO);
        String contrasenaEsperada = lectorVariables.apply(VAR_CONTRASENA);
        if (esVacio(codigoEsperado) || esVacio(contrasenaEsperada)) {
            throw new IllegalStateException(MSG_CONFIGURACION);
        }

        boolean codigoCoincide = igualesEnTiempoConstante(codigoAcceso.trim(), codigoEsperado.trim());
        boolean contrasenaCoincide = igualesEnTiempoConstante(contrasena, contrasenaEsperada);
        if (!(codigoCoincide & contrasenaCoincide)) {
            throw new IllegalArgumentException(MSG_CREDENCIALES_INVALIDAS);
        }
        return true;
    }

    private static String leerVariableSistema(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(nombre);
        }
        return valor;
    }

    private boolean igualesEnTiempoConstante(String recibido, String esperado) {
        return MessageDigest.isEqual(
                recibido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
