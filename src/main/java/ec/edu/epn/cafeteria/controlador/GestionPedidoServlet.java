package ec.edu.epn.cafeteria.controlador;

import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.modelo.Pedido;
import ec.edu.epn.cafeteria.servicio.ConsultaPedidoService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Atiende la gestión de pedidos del personal: consulta y filtro por estado (HU-07).
 */
@WebServlet("/admin/pedidos")
public class GestionPedidoServlet extends HttpServlet {

    private final transient ConsultaPedidoService consultaPedidoService = new ConsultaPedidoService();

    /**
     * Muestra la lista de pedidos filtrada por estado y, si se pide, el detalle de un pedido.
     *
     * @param request  solicitud HTTP (parámetros opcionales «estado» y «detalle»)
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EstadoPedido estado = leerEstado(request.getParameter("estado"));
        request.setAttribute("estadoFiltro", estado);
        request.setAttribute("estados", EstadoPedido.values());
        try {
            List<Pedido> pedidos = consultaPedidoService.listarPedidosParaPersonal().stream()
                    .filter(p -> p.getEstado() == estado)
                    .toList();
            request.setAttribute("pedidos", pedidos);
            String detalle = request.getParameter("detalle");
            if (detalle != null) {
                consultaPedidoService.consultarPedidoDelCliente(null, detalle)
                        .ifPresent(p -> request.setAttribute("pedidoDetalle", p));
            }
        } catch (IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
        }
        request.getRequestDispatcher("/WEB-INF/vistas/admin-pedidos.jsp").forward(request, response);
    }

    private EstadoPedido leerEstado(String valor) {
        try {
            return valor == null ? EstadoPedido.PENDIENTE : EstadoPedido.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return EstadoPedido.PENDIENTE;
        }
    }
}
