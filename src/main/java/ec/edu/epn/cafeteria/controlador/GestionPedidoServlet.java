package ec.edu.epn.cafeteria.controlador;

import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.modelo.Pedido;
import ec.edu.epn.cafeteria.servicio.ConsultaPedidoService;
import ec.edu.epn.cafeteria.servicio.GestionPedidoService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Atiende la gestión de pedidos del personal: consulta y filtro por estado,
 * avance de estado y registro de la entrega con el código de retiro.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-07 Consultar pedidos por estado.</li>
 * <li>HU-08 Avanzar estado y registrar la entrega.</li>
 * <li>RN-03, RN-04, RN-NEG-06 y RN-NEG-07.</li>
 * <li>HU-10: ruta protegida por {@link FiltroAcceso}.</li>
 * </ul>
 */
@WebServlet("/admin/pedidos")
public class GestionPedidoServlet extends HttpServlet {

    private final transient ConsultaPedidoService consultaPedidoService = new ConsultaPedidoService();
    private final transient GestionPedidoService gestionPedidoService = new GestionPedidoService();

    /**
     * Muestra la lista de pedidos filtrada por estado y, si se pide, el detalle de un pedido.
     * <p>Trazabilidad: HU-07, RN-03, RN-NEG-06.</p>
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

    /**
     * Avanza el estado de un pedido («Iniciar preparación», «Marcar como listo») o registra
     * su entrega («Registrar entrega»).
     * <p>Trazabilidad: HU-08, RN-04, RN-NEG-06, RN-NEG-07.</p>
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @throws IOException si falla la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String destino = request.getContextPath() + "/admin/pedidos";
        try {
            if ("entregar".equals(request.getParameter("accion"))) {
                String codigo = request.getParameter("codigoRetiro");
                gestionPedidoService.registrarEntrega(codigo);
                request.getSession().setAttribute("mensaje",
                        "Se registró la entrega del pedido «" + codigo.trim().toUpperCase() + "».");
                destino += "?estado=ENTREGADO";
            } else {
                gestionPedidoService.avanzarEstado(Long.parseLong(request.getParameter("id")));
                String codigo = request.getParameter("codigo");
                destino += "?estado=" + request.getParameter("estado")
                        + "&detalle=" + URLEncoder.encode(codigo, StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.getSession().setAttribute("error", e.getMessage());
        }
        response.sendRedirect(destino);
    }

    /**
     * Convierte el estado elegido en el filtro; si no es válido usa PENDIENTE.
     * <p>Trazabilidad: HU-07, RN-NEG-06.</p>
     *
     * @param valor nombre del estado enviado en la solicitud
     * @return el estado correspondiente
     */
    private EstadoPedido leerEstado(String valor) {
        try {
            return valor == null ? EstadoPedido.PENDIENTE : EstadoPedido.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return EstadoPedido.PENDIENTE;
        }
    }
}
