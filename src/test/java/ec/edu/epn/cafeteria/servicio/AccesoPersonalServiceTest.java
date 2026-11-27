package ec.edu.epn.cafeteria.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccesoPersonalServiceTest {

    private AccesoPersonalService servicio;

    @BeforeEach
    void preparar() {
        Map<String, String> entornoPrueba = Map.of(
                AccesoPersonalService.VAR_CODIGO_ACCESO, "CODIGO-PRUEBA",
                AccesoPersonalService.VAR_CONTRASENA, "ClavePrueba#123"
        );
        servicio = new AccesoPersonalService(entornoPrueba::get);
    }

    @Test
    @DisplayName("HU-10: autentica al personal cuando el código de acceso y la contraseña coinciden")
    void autenticaCredencialesValidas() {
        assertTrue(servicio.autenticar("  CODIGO-PRUEBA  ", "ClavePrueba#123"));
    }

    @Test
    @DisplayName("HU-10: rechaza código o contraseña incorrectos con el mismo mensaje genérico")
    void rechazaCredencialesIncorrectasConMensajeGenerico() {
        IllegalArgumentException errorCodigo = assertThrows(IllegalArgumentException.class, () ->
                servicio.autenticar("OTRO-CODIGO", "ClavePrueba#123"));
        IllegalArgumentException errorContrasena = assertThrows(IllegalArgumentException.class, () ->
                servicio.autenticar("CODIGO-PRUEBA", "ClaveIncorrecta"));

        assertEquals("Código de acceso o contraseña incorrectos.", errorCodigo.getMessage());
        assertEquals(errorCodigo.getMessage(), errorContrasena.getMessage());
    }

    @Test
    @DisplayName("HU-10: rechaza el acceso cuando el código o la contraseña están vacíos")
    void rechazaCamposVacios() {
        IllegalArgumentException errorCodigoVacio = assertThrows(IllegalArgumentException.class, () ->
                servicio.autenticar("   ", "ClavePrueba#123"));
        IllegalArgumentException errorClaveVacia = assertThrows(IllegalArgumentException.class, () ->
                servicio.autenticar("CODIGO-PRUEBA", ""));

        assertEquals(AccesoPersonalService.MSG_CAMPOS_OBLIGATORIOS, errorCodigoVacio.getMessage());
        assertEquals(AccesoPersonalService.MSG_CAMPOS_OBLIGATORIOS, errorClaveVacia.getMessage());
    }

    @Test
    @DisplayName("HU-10 / RNF-04: falla si las variables STAFF_ACCESS_CODE o STAFF_PASSWORD no están configuradas")
    void fallaSinVariablesConfiguradas() {
        AccesoPersonalService servicioSinVariables = new AccesoPersonalService(nombre -> null);
        assertThrows(IllegalStateException.class, () ->
                servicioSinVariables.autenticar("CODIGO-PRUEBA", "ClavePrueba#123"));
    }
}
