package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.modelo.Producto;
import ec.edu.epn.cafeteria.soporte.ProductoDAOEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoServiceTest {

    private ProductoDAOEnMemoria dao;
    private ProductoService servicio;

    @BeforeEach
    void preparar() {
        dao = new ProductoDAOEnMemoria();
        servicio = new ProductoService(dao);
        dao.agregar("P001", "Sándwich", "3.00", true, 10);
        dao.agregar("P002", "Jugo", "2.00", true, 0);
        dao.agregar("P003", "Ensalada", "3.50", false, 10);
    }

    @Test
    @DisplayName("HU-01 E2: el menú oculta los productos no disponibles y los que no tienen unidades")
    void menuSoloMuestraDisponiblesConUnidades() {
        List<Producto> menu = servicio.consultarDisponibles();
        assertEquals(1, menu.size());
        assertEquals("Sándwich", menu.get(0).getNombre());
    }

    @Test
    @DisplayName("HU-06 E1: registra un producto válido")
    void registraProductoValido() {
        servicio.registrarProducto(new Producto("P009", "Té", new BigDecimal("1.00"), true, 20));
        assertTrue(dao.buscarPorCodigo("P009").isPresent());
    }

    @Test
    @DisplayName("HU-06 E8: rechaza un precio negativo")
    void rechazaPrecioNegativo() {
        Producto p = new Producto("P010", "Té", new BigDecimal("-2.00"), true, 5);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.registrarProducto(p));
        assertEquals(ProductoService.MSG_PRECIO, e.getMessage());
        assertFalse(dao.buscarPorCodigo("P010").isPresent());
    }

    @Test
    @DisplayName("HU-06 E9: rechaza unidades negativas")
    void rechazaUnidadesNegativas() {
        Producto p = new Producto("P011", "Té", new BigDecimal("1.00"), true, -5);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> servicio.registrarProducto(p));
        assertEquals(ProductoService.MSG_UNIDADES, e.getMessage());
    }

    @Test
    @DisplayName("HU-06: no permite registrar dos productos con el mismo código")
    void rechazaCodigoRepetido() {
        Producto p = new Producto("P001", "Otro", new BigDecimal("1.00"), true, 1);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarProducto(p));
    }

    @Test
    @DisplayName("HU-06 E3: actualiza el precio de un producto")
    void actualizaPrecio() {
        Producto p = dao.buscarPorCodigo("P001").orElseThrow();
        p.setPrecio(new BigDecimal("4.00"));
        servicio.actualizarProducto(p);
        assertEquals(new BigDecimal("4.00"), dao.guardado("P001").getPrecio());
    }

    @Test
    @DisplayName("HU-06 E4 y E5: deshabilita y vuelve a habilitar un producto")
    void cambiaDisponibilidad() {
        servicio.cambiarDisponibilidad("P001", false);
        assertTrue(servicio.consultarDisponibles().isEmpty());
        servicio.cambiarDisponibilidad("P001", true);
        assertEquals(1, servicio.consultarDisponibles().size());
    }

    @Test
    @DisplayName("HU-06 E6: reponer unidades vuelve a mostrar el producto; si sigue no disponible, no")
    void reponerUnidades() {
        Producto jugo = dao.buscarPorCodigo("P002").orElseThrow();
        jugo.setCantidadDisponible(15);
        servicio.actualizarProducto(jugo);
        assertEquals(2, servicio.consultarDisponibles().size());

        Producto ensalada = dao.buscarPorCodigo("P003").orElseThrow();
        ensalada.setCantidadDisponible(20);
        servicio.actualizarProducto(ensalada);
        assertEquals(2, servicio.consultarDisponibles().size());
    }
}
