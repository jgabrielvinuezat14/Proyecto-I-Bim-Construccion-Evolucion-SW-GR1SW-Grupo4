package ec.edu.epn.cafeteria.soporte;

import ec.edu.epn.cafeteria.dao.PedidoDAO;
import ec.edu.epn.cafeteria.modelo.Cliente;
import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.modelo.Pedido;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PedidoDAO en memoria para las pruebas unitarias.
 */
public class PedidoDAOEnMemoria extends PedidoDAO {

    private final List<Pedido> pedidos = new ArrayList<>();
    private long siguienteId = 1;

    public PedidoDAOEnMemoria() {
        super(new ConexionFalsa());
    }

    /** Agrega un pedido de prueba con el estado indicado y lo devuelve. */
    public Pedido agregar(String codigoRetiro, String codigoCliente, EstadoPedido estado) {
        Pedido p = new Pedido();
        Cliente c = new Cliente();
        c.setCodigo(codigoCliente);
        p.setCliente(c);
        p.setCodigoRetiro(codigoRetiro);
        p.setEstado(estado);
        p.setTotal(new BigDecimal("6.00"));
        p.setFechaRegistro(LocalDateTime.now());
        p.setId(siguienteId++);
        pedidos.add(p);
        return p;
    }

    public List<Pedido> todos() {
        return pedidos;
    }

    @Override
    public synchronized void guardar(Pedido pedido, Connection conexion) {
        pedido.setId(siguienteId++);
        pedidos.add(pedido);
    }

    @Override
    public Optional<Pedido> buscarPorCodigoRetiro(String codigo) {
        return pedidos.stream().filter(p -> p.getCodigoRetiro().equals(codigo)).findFirst();
    }

    @Override
    public List<Pedido> listarPorCliente(String codigoCliente) {
        return pedidos.stream().filter(p -> p.getCliente().getCodigo().equals(codigoCliente)).toList();
    }

    @Override
    public List<Pedido> listarTodos() {
        return List.copyOf(pedidos);
    }

    @Override
    public synchronized boolean actualizarEstado(long id, EstadoPedido anterior, EstadoPedido nuevo) {
        for (Pedido p : pedidos) {
            if (p.getId() == id && p.getEstado() == anterior) {
                p.setEstado(nuevo);
                return true;
            }
        }
        return false;
    }
}
