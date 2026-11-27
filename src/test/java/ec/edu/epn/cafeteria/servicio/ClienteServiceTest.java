package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.modelo.Cliente;
import ec.edu.epn.cafeteria.soporte.ClienteDAOEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteServiceTest {

    private static final String HASH_SEMILLA_PRUEBA =
            "$2a$10$aj8rWCC3gNXBtRqihgzBguVMP41J3HNlPLiR57mfEWvmr8kOyVMv6";

    private ClienteDAOEnMemoria dao;
    private ClienteService servicio;

    @BeforeEach
    void preparar() {
        dao = new ClienteDAOEnMemoria();
        servicio = new ClienteService(dao);
        dao.agregar("C001", "Cliente", "Prueba", "1700000001", "0990000001",
                "cliente.prueba@epn.edu.ec", HASH_SEMILLA_PRUEBA);
    }

    @Test
    @DisplayName("HU-09 / RNF-04: registra un cliente válido con código único y contraseña como hash BCrypt")
    void registraClienteValidoConHashBCrypt() {
        Cliente registrado = servicio.registrarCliente(
                "Ana", "Pérez", "1723456789", "0987654321", "ana.perez@epn.edu.ec", "ClaveSegura#2026");

        assertEquals("C002", registrado.getCodigo());
        Cliente guardado = dao.guardado("C002");
        assertNotNull(guardado);
        assertNotEquals("ClaveSegura#2026", guardado.getContrasenaHash());
        assertTrue(guardado.getContrasenaHash().startsWith("$2a$"));
        assertTrue(BCrypt.checkpw("ClaveSegura#2026", guardado.getContrasenaHash()));
    }

    @Test
    @DisplayName("HU-09: asigna códigos únicos secuenciales a nuevos clientes")
    void generaCodigosUnicosSecuenciales() {
        Cliente primero = servicio.registrarCliente(
                "Luis", "Mora", "1711111111", "0991111111", "luis.mora@epn.edu.ec", "Clave#1");
        Cliente segundo = servicio.registrarCliente(
                "Carla", "Vega", "1722222222", "0992222222", "carla.vega@epn.edu.ec", "Clave#2");

        assertEquals("C002", primero.getCodigo());
        assertEquals("C003", segundo.getCodigo());
    }

    @Test
    @DisplayName("HU-09: rechaza el registro cuando falta algún campo obligatorio")
    void rechazaCamposObligatoriosVacios() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarCliente("  ", "Pérez", "1723456789", "0987654321",
                        "ana.perez@epn.edu.ec", "Clave#2026"));
        assertEquals(ClienteService.MSG_CAMPOS_OBLIGATORIOS, e.getMessage());
    }

    @Test
    @DisplayName("HU-09: rechaza cédula que no tenga exactamente 10 dígitos numéricos")
    void rechazaCedulaInvalida() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarCliente("Ana", "Pérez", "17234AB789", "0987654321",
                        "ana.perez@epn.edu.ec", "Clave#2026"));
        assertEquals(ClienteService.MSG_CEDULA, e.getMessage());
    }

    @Test
    @DisplayName("HU-09: rechaza celular que no tenga exactamente 10 dígitos numéricos")
    void rechazaCelularInvalido() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarCliente("Ana", "Pérez", "1723456789", "098765",
                        "ana.perez@epn.edu.ec", "Clave#2026"));
        assertEquals(ClienteService.MSG_CELULAR, e.getMessage());
    }

    @Test
    @DisplayName("HU-09: rechaza correo institucional con formato inválido")
    void rechazaCorreoInvalido() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarCliente("Ana", "Pérez", "1723456789", "0987654321",
                        "correo-sin-arroba", "Clave#2026"));
        assertEquals(ClienteService.MSG_CORREO, e.getMessage());
    }

    @Test
    @DisplayName("HU-09: rechaza registro con correo institucional duplicado")
    void rechazaCorreoDuplicado() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarCliente("Otro", "Usuario", "1799999999", "0999999999",
                        "CLIENTE.PRUEBA@epn.edu.ec", "Clave#2026"));
        assertEquals(ClienteService.MSG_CORREO_DUPLICADO, e.getMessage());
    }

    @Test
    @DisplayName("HU-09: rechaza registro con cédula duplicada")
    void rechazaCedulaDuplicada() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.registrarCliente("Otro", "Usuario", "1700000001", "0999999999",
                        "otro.usuario@epn.edu.ec", "Clave#2026"));
        assertEquals(ClienteService.MSG_CEDULA_DUPLICADA, e.getMessage());
    }

    @Test
    @DisplayName("HU-09: autentica correctamente al cliente de datos_iniciales.sql con su hash BCrypt")
    void iniciaSesionConClienteSemilla() {
        Cliente autenticado = servicio.iniciarSesion("  CLIENTE.PRUEBA@epn.edu.ec ", "Prueba#2026");
        assertEquals("C001", autenticado.getCodigo());
        assertEquals("Cliente", autenticado.getNombre());
    }

    @Test
    @DisplayName("HU-09: devuelve el mismo mensaje genérico ante correo inexistente o contraseña incorrecta")
    void mensajeGenericoAnteCredencialesIncorrectas() {
        IllegalArgumentException errorClave = assertThrows(IllegalArgumentException.class, () ->
                servicio.iniciarSesion("cliente.prueba@epn.edu.ec", "ClaveIncorrecta"));
        IllegalArgumentException errorCorreo = assertThrows(IllegalArgumentException.class, () ->
                servicio.iniciarSesion("no.existe@epn.edu.ec", "Prueba#2026"));

        assertEquals(ClienteService.MSG_CREDENCIALES_INVALIDAS, errorClave.getMessage());
        assertEquals(errorClave.getMessage(), errorCorreo.getMessage());
    }

    @Test
    @DisplayName("HU-09: rechaza inicio de sesión con credenciales vacías")
    void rechazaLoginConCamposVacios() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                servicio.iniciarSesion("", ""));
        assertEquals(ClienteService.MSG_CREDENCIALES_OBLIGATORIAS, e.getMessage());
    }
}
