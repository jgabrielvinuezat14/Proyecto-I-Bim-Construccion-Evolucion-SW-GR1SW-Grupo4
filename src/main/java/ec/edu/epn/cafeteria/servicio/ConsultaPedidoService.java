package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.dao.PedidoDAO;
import ec.edu.epn.cafeteria.modelo.Pedido;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Consultas de pedidos para el cliente (HU-05) y para el personal (HU-07).
 */
public class ConsultaPedidoService {

    private final PedidoDAO pedidoDAO;

    /** Crea el servicio con el DAO por defecto. */
    public ConsultaPedidoService() {
        this(new PedidoDAO());
    }

    /**
     * Crea el servicio con un DAO concreto.
     *
     * @param pedidoDAO acceso a datos de pedidos
     */
    public ConsultaPedidoService(PedidoDAO pedidoDAO) {
        this.pedidoDAO = pedidoDAO;
    }

    /**
     * Consulta un pedido por su código de retiro. Si se indica el código del cliente,
     * solo devuelve el pedido cuando le pertenece, sin revelar pedidos de otros clientes.
     *
     * @param codigoCliente código del cliente con sesión, o {@code null} para no filtrar
     * @param codigoRetiro  código de retiro ingresado (no distingue mayúsculas)
     * @return el pedido con sus detalles, o vacío si no existe o pertenece a otro cliente
     */
    public Optional<Pedido> consultarPedidoDelCliente(String codigoCliente, String codigoRetiro) {
        if (codigoRetiro == null || codigoRetiro.isBlank()) {
            return Optional.empty();
        }
        try {
            return pedidoDAO.buscarPorCodigoRetiro(codigoRetiro.trim().toUpperCase())
                    .filter(p -> codigoCliente == null || codigoCliente.equals(p.getCliente().getCodigo()));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el pedido. Intente de nuevo.", e);
        }
    }

    /**
     * Lista los pedidos de un cliente, del más reciente al más antiguo.
     *
     * @param codigoCliente código del cliente
     * @return pedidos del cliente
     */
    public List<Pedido> listarPedidosDelCliente(String codigoCliente) {
        try {
            return pedidoDAO.listarPorCliente(codigoCliente);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar sus pedidos. Intente de nuevo.", e);
        }
    }

    /**
     * Lista todos los pedidos recibidos para que el personal los filtre por estado (HU-07).
     *
     * @return pedidos en orden de llegada
     */
    public List<Pedido> listarPedidosParaPersonal() {
        try {
            return pedidoDAO.listarTodos();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los pedidos. Intente de nuevo.", e);
        }
    }
}
