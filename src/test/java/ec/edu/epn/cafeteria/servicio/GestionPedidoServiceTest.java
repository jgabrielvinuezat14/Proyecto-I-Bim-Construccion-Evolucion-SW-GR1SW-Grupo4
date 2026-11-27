package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.modelo.DetallePedido;
import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.modelo.Pedido;
import ec.edu.epn.cafeteria.modelo.Producto;
import ec.edu.epn.cafeteria.soporte.ConexionFalsa;
import ec.edu.epn.cafeteria.soporte.PedidoDAOEnMemoria;
import ec.edu.epn.cafeteria.soporte.ProductoDAOEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestionPedidoServiceTest {

    private ProductoDAOEnMemoria productos;
    private PedidoDAOEnMemoria pedidos;
    private ConexionFalsa conexion;
    private GestionPedidoService servicio;
    private Producto sandwich;
    private Producto jugo;

    @BeforeEach
    void preparar() {
        productos = new ProductoDAOEnMemoria();
        pedidos = new PedidoDAOEnMemoria();
        conexion = new ConexionFalsa();
        servicio = new GestionPedidoService(productos, pedidos, conexion);
        sandwich = productos.agregar("P001", "Sándwich", "3.00", true, 10);
        jugo = productos.agregar("P002", "Jugo", "2.00", true, 10);
    }

    private List<DetallePedido> seleccion(Object... productoYCantidad) {
        List<DetallePedido> lista = new ArrayList<>();
        for (int i = 0; i < productoYCantidad.length; i += 2) {
            lista.add(new DetallePedido((Producto) productoYCantidad[i], (Integer) productoYCantidad[i + 1]));
        }
        return lista;
    }

    @Test
    @DisplayName("HU-03 E2: calcula el total de la selección ($8.00)")
    void calculaTotal() {
        assertEquals(new BigDecimal("8.00"), servicio.calcularTotal(seleccion(sandwich, 2, jugo, 1)));
    }

    @Test
    @DisplayName("HU-04 E1: confirma un pedido PENDIENTE, con código de 6 caracteres, y descuenta unidades")
    void confirmaPedidoValido() {
        Pedido pedido = servicio.confirmarPedido("C001", seleccion(sandwich, 2));
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertTrue(pedido.getCodigoRetiro().matches("[A-Z0-9]{6}"));
        assertEquals(new BigDecimal("6.00"), pedido.getTotal());
        assertEquals(8, productos.guardado("P001").getCantidadDisponible());
        assertEquals(1, conexion.confirmaciones);
    }

    @Test
    @DisplayName("HU-04 E1: dos pedidos reciben códigos de retiro distintos")
    void codigosDistintos() {
        Pedido a = servicio.confirmarPedido("C001", seleccion(sandwich, 1));
        Pedido b = servicio.confirmarPedido("C002", seleccion(jugo, 1));
        assertNotEquals(a.getCodigoRetiro(), b.getCodigoRetiro());
    }

    @Test
    @DisplayName("HU-04 E4: rechaza confirmar sin productos")
    void rechazaSeleccionVacia() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.confirmarPedido("C001", new ArrayList<>()));
        assertEquals(GestionPedidoService.MSG_VACIO, e.getMessage());
        assertTrue(pedidos.todos().isEmpty());
    }

    @Test
    @DisplayName("HU-04 E5: rechaza una cantidad igual a cero")
    void rechazaCantidadCero() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> servicio.confirmarPedido("C001", seleccion(sandwich, 0)));
        assertEquals(GestionPedidoService.MSG_CANTIDAD, e.getMessage());
    }

    @Test
    @DisplayName("HU-04 E6: rechaza un producto que dejó de estar disponible")
    void rechazaProductoNoDisponible() {
        List<DetallePedido> sel = seleccion(sandwich, 1);
        productos.guardado("P001").setDisponible(false);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.confirmarPedido("C001", sel));
        assertEquals("«Sándwich» ya no está disponible.", e.getMessage());
        assertTrue(pedidos.todos().isEmpty());
    }

    @Test
    @DisplayName("HU-02 E10 / HU-04 E7: rechaza más unidades de las disponibles")
    void rechazaUnidadesInsuficientes() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> servicio.confirmarPedido("C001", seleccion(sandwich, 11)));
        assertTrue(e.getMessage().startsWith("No hay unidades suficientes de «Sándwich»"));
        assertEquals(10, productos.guardado("P001").getCantidadDisponible());
    }

    @Test
    @DisplayName("HU-04 E9: si el precio cambió desde la revisión, no confirma y actualiza el total")
    void avisaCambioDePrecio() {
        List<DetallePedido> sel = seleccion(sandwich, 2);
        productos.guardado("P001").setPrecio(new BigDecimal("3.50"));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.confirmarPedido("C001", sel));
        assertTrue(e.getMessage().contains("cambió"));
        assertEquals(new BigDecimal("7.00"), servicio.calcularTotal(sel));
        assertTrue(pedidos.todos().isEmpty());
        // HU-04 E10: al confirmar de nuevo con el total actualizado, el pedido se registra por $7.00
        assertEquals(new BigDecimal("7.00"), servicio.confirmarPedido("C001", sel).getTotal());
    }

    @Test
    @DisplayName("HU-04 E7 / RNF-03: con 2 unidades, de dos confirmaciones simultáneas (2 y 1) solo una se registra")
    void confirmacionesSimultaneas() throws InterruptedException {
        productos.guardado("P001").setCantidadDisponible(2);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        Runnable confirmar2 = () -> intentar(salida, exitos, rechazos, 2);
        Runnable confirmar1 = () -> intentar(salida, exitos, rechazos, 1);
        Thread a = new Thread(confirmar2);
        Thread b = new Thread(confirmar1);
        a.start();
        b.start();
        salida.countDown();
        a.join();
        b.join();
        int restantes = productos.guardado("P001").getCantidadDisponible();
        assertEquals(1, exitos.get(), "solo una confirmación debe registrarse");
        assertEquals(1, rechazos.get(), "la otra debe rechazarse por unidades insuficientes");
        assertEquals(1, pedidos.todos().size());
        assertTrue(restantes >= 0, "las unidades nunca quedan negativas");
        assertEquals(2 - restantes, pedidos.todos().get(0).getDetalles().get(0).getCantidad(),
                "solo se descuentan las unidades del pedido confirmado");
    }

    private void intentar(CountDownLatch salida, AtomicInteger exitos, AtomicInteger rechazos, int cantidad) {
        try {
            salida.await();
            Producto copia = new Producto("P001", "Sándwich", new BigDecimal("3.00"), true, 2);
            servicio.confirmarPedido("C00" + cantidad, List.of(new DetallePedido(copia, cantidad)));
            exitos.incrementAndGet();
        } catch (IllegalStateException e) {
            rechazos.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("HU-08 E1 y E2: avanza de PENDIENTE a EN_PREPARACION y luego a LISTO_PARA_RETIRAR")
    void avanzaEstados() {
        Pedido p = pedidos.agregar("A1B2C3", "C001", EstadoPedido.PENDIENTE);
        servicio.avanzarEstado(p.getId());
        assertEquals(EstadoPedido.EN_PREPARACION, p.getEstado());
        servicio.avanzarEstado(p.getId());
        assertEquals(EstadoPedido.LISTO_PARA_RETIRAR, p.getEstado());
        assertThrows(IllegalStateException.class, () -> servicio.avanzarEstado(p.getId()));
    }

    @Test
    @DisplayName("HU-08 E7: registra la entrega con el código correcto")
    void entregaConCodigoCorrecto() {
        Pedido p = pedidos.agregar("A1B2C3", "C001", EstadoPedido.LISTO_PARA_RETIRAR);
        servicio.registrarEntrega("a1b2c3");
        assertEquals(EstadoPedido.ENTREGADO, p.getEstado());
    }

    @Test
    @DisplayName("HU-08 E5: no entrega un pedido que no está listo")
    void noEntregaPedidoNoListo() {
        Pedido p = pedidos.agregar("A1B2C3", "C001", EstadoPedido.EN_PREPARACION);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.registrarEntrega("A1B2C3"));
        assertEquals(GestionPedidoService.MSG_NO_LISTO, e.getMessage());
        assertEquals(EstadoPedido.EN_PREPARACION, p.getEstado());
    }

    @Test
    @DisplayName("HU-08 E6: no registra dos veces la entrega")
    void noEntregaDosVeces() {
        pedidos.agregar("A1B2C3", "C001", EstadoPedido.ENTREGADO);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.registrarEntrega("A1B2C3"));
        assertEquals(GestionPedidoService.MSG_YA_ENTREGADO, e.getMessage());
    }

    @Test
    @DisplayName("HU-08 E8: rechaza un código que no corresponde a ningún pedido listo")
    void rechazaCodigoIncorrecto() {
        Pedido p = pedidos.agregar("A1B2C3", "C001", EstadoPedido.LISTO_PARA_RETIRAR);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> servicio.registrarEntrega("Z9Y8X7"));
        assertEquals(GestionPedidoService.MSG_CODIGO_INCORRECTO, e.getMessage());
        assertEquals(EstadoPedido.LISTO_PARA_RETIRAR, p.getEstado());
    }
}
