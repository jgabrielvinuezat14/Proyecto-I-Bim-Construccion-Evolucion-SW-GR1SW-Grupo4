package ec.edu.epn.cafeteria.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PedidoTest {

    private final Producto sandwich = new Producto("P001", "Sándwich", new BigDecimal("3.00"), true, 20);
    private final Producto jugo = new Producto("P002", "Jugo", new BigDecimal("2.00"), true, 15);

    @Test
    @DisplayName("HU-03 E1: 2 sándwiches a $3.00 tienen subtotal $6.00")
    void subtotalDeUnDetalle() {
        assertEquals(new BigDecimal("6.00"), new DetallePedido(sandwich, 2).calcularSubtotal());
    }

    @Test
    @DisplayName("HU-03 E2: 2 sándwiches y 1 jugo suman un total de $8.00")
    void totalDeDosProductos() {
        Pedido pedido = new Pedido();
        pedido.setDetalles(List.of(new DetallePedido(sandwich, 2), new DetallePedido(jugo, 1)));
        assertEquals(new BigDecimal("8.00"), pedido.calcularTotal());
    }

    @Test
    @DisplayName("HU-04 E8: el detalle conserva su precio aunque cambie el precio del producto")
    void precioUnitarioSeConserva() {
        DetallePedido detalle = new DetallePedido(sandwich, 2);
        sandwich.setPrecio(new BigDecimal("4.00"));
        assertEquals(new BigDecimal("3.00"), detalle.getPrecioUnitario());
        assertEquals(new BigDecimal("6.00"), detalle.calcularSubtotal());
    }

    @Test
    @DisplayName("Un pedido sin detalles tiene total $0.00")
    void pedidoVacioTotalCero() {
        assertEquals(new BigDecimal("0.00"), new Pedido().calcularTotal());
    }
}
