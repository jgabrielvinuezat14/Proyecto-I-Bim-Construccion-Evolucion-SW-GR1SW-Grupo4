package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.soporte.PedidoDAOEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaPedidoServiceTest {

    private ConsultaPedidoService servicio;

    @BeforeEach
    void preparar() {
        PedidoDAOEnMemoria dao = new PedidoDAOEnMemoria();
        dao.agregar("A1B2C3", "C001", EstadoPedido.EN_PREPARACION);
        dao.agregar("D4E5F6", "C002", EstadoPedido.PENDIENTE);
        servicio = new ConsultaPedidoService(dao);
    }

    @Test
    @DisplayName("HU-05 E1: el cliente consulta su pedido y ve su estado")
    void consultaPedidoPropio() {
        assertEquals(EstadoPedido.EN_PREPARACION,
                servicio.consultarPedidoDelCliente("C001", "a1b2c3").orElseThrow().getEstado());
    }

    @Test
    @DisplayName("HU-05 E2: un código inexistente no devuelve ningún pedido")
    void codigoInexistente() {
        assertTrue(servicio.consultarPedidoDelCliente("C001", "Z9Y8X7").isEmpty());
    }

    @Test
    @DisplayName("HU-09 T09.3: un cliente no ve el pedido de otro cliente")
    void noMuestraPedidoAjeno() {
        assertTrue(servicio.consultarPedidoDelCliente("C001", "D4E5F6").isEmpty());
    }

    @Test
    @DisplayName("HU-07 E1: el personal recibe todos los pedidos para filtrarlos por estado")
    void listaParaPersonal() {
        assertEquals(1, servicio.listarPedidosParaPersonal().stream()
                .filter(p -> p.getEstado() == EstadoPedido.PENDIENTE).count());
    }
}
