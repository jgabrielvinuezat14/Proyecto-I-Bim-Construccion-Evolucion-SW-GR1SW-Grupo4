package ec.edu.epn.cafeteria.controlador;

import ec.edu.epn.cafeteria.modelo.Cliente;
import ec.edu.epn.cafeteria.modelo.DetallePedido;
import ec.edu.epn.cafeteria.modelo.Pedido;
import ec.edu.epn.cafeteria.modelo.Producto;
import ec.edu.epn.cafeteria.servicio.ConsultaPedidoService;
import ec.edu.epn.cafeteria.servicio.GestionPedidoService;
import ec.edu.epn.cafeteria.servicio.ProductoService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Atiende las solicitudes del cliente sobre su pedido.
 * La selección se guarda en la sesión y no en la base de datos hasta confirmar.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-02 Agregar y quitar productos.</li>
 * <li>HU-03 Revisar el total.</li>
 * <li>HU-04 Confirmar el pedido y mostrar el código de retiro.</li>
 * <li>HU-05 Consultar el estado de sus pedidos.</li>
 * <li>HU-09: usa el cliente de la sesión.</li>
 * <li>RN-NEG-01 y RN-NEG-02: validaciones de la selección.</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/mi-pedido", "/consultar-pedido"})
public class PedidoServlet extends HttpServlet {

    private static final String SELECCION = "seleccion";
    private static final String REVISION = "revision";
    private static final String CODIGO = "codigo";
    private static final String PEDIDO_CONFIRMADO = "pedidoConfirmado";
    private static final String ERROR = "error";
    private static final String MSG_CANTIDAD = "La cantidad debe ser un número entero positivo.";

    private final transient ProductoService productoService = new ProductoService();
    private final transient GestionPedidoService gestionPedidoService = new GestionPedidoService();
    private final transient ConsultaPedidoService consultaPedidoService = new ConsultaPedidoService();

    /**
     * Muestra la pestaña «Mi pedido» o «Consultar pedido».
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/consultar-pedido".equals(request.getServletPath())) {
            mostrarConsulta(request, response);
        } else {
            mostrarMiPedido(request, response);
        }
    }

    /**
     * Ejecuta la acción del botón pulsado en «Mi pedido»: «Agregar al pedido»,
     * «Actualizar cantidad», «Quitar», «Revisar pedido» o «Confirmar pedido».
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession sesion = request.getSession();
        List<DetallePedido> seleccion = obtenerSeleccion(sesion);
        String accion = request.getParameter("accion");
        try {
            switch (accion == null ? "" : accion) {
                case "agregar" -> agregar(request, seleccion);
                case "actualizar" -> actualizar(request, seleccion);
                case "quitar" -> seleccion.removeIf(d -> d.getProducto().getCodigo().equals(request.getParameter(CODIGO)));
                case "revisar" -> revisar(sesion, seleccion);
                case "confirmar" -> {
                    Pedido pedido = gestionPedidoService.confirmarPedido(codigoCliente(sesion), seleccion);
                    sesion.removeAttribute(SELECCION);
                    sesion.removeAttribute(REVISION);
                    sesion.setAttribute(PEDIDO_CONFIRMADO, pedido);
                }
                default -> throw new IllegalArgumentException("Acción no reconocida.");
            }
            if (seleccion.isEmpty()) {
                sesion.removeAttribute(REVISION);
            }
            response.sendRedirect(request.getContextPath() + "/mi-pedido");
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute(ERROR, e.getMessage());
            mostrarMiPedido(request, response);
        }
    }

    private void agregar(HttpServletRequest request, List<DetallePedido> seleccion) {
        int cantidad = leerCantidad(request);
        String codigo = request.getParameter("producto");
        Producto producto = buscarDisponible(codigo).orElseThrow(
                () -> new IllegalStateException("El producto ya no está disponible para nuevos pedidos."));
        Optional<DetallePedido> existente = buscarEnSeleccion(seleccion, codigo);
        int total = cantidad + existente.map(DetallePedido::getCantidad).orElse(0);
        validarUnidades(producto, total);
        if (existente.isPresent()) {
            existente.get().setCantidad(total);
        } else {
            seleccion.add(new DetallePedido(producto, cantidad));
        }
    }

    private void actualizar(HttpServletRequest request, List<DetallePedido> seleccion) {
        int cantidad = leerCantidad(request);
        String codigo = request.getParameter(CODIGO);
        DetallePedido detalle = buscarEnSeleccion(seleccion, codigo).orElseThrow(
                () -> new IllegalArgumentException("El producto no está en su selección."));
        Producto producto = buscarDisponible(codigo).orElseThrow(
                () -> new IllegalStateException("«" + detalle.getProducto().getNombre() + "» ya no está disponible."));
        validarUnidades(producto, cantidad);
        detalle.setCantidad(cantidad);
    }

    private void revisar(HttpSession sesion, List<DetallePedido> seleccion) {
        if (seleccion.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto.");
        }
        for (DetallePedido detalle : seleccion) {
            buscarDisponible(detalle.getProducto().getCodigo()).ifPresent(p -> {
                detalle.setPrecioUnitario(p.getPrecio());
                detalle.getProducto().setPrecio(p.getPrecio());
            });
        }
        sesion.setAttribute(REVISION, Boolean.TRUE);
    }

    private void validarUnidades(Producto producto, int cantidad) {
        if (cantidad > producto.getCantidadDisponible()) {
            throw new IllegalStateException("Solo hay " + producto.getCantidadDisponible()
                    + " unidades disponibles de «" + producto.getNombre() + "».");
        }
    }

    private int leerCantidad(HttpServletRequest request) {
        try {
            int cantidad = Integer.parseInt(request.getParameter("cantidad").trim());
            if (cantidad <= 0) {
                throw new IllegalArgumentException(MSG_CANTIDAD);
            }
            return cantidad;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(MSG_CANTIDAD);
        }
    }

    private Optional<Producto> buscarDisponible(String codigo) {
        return productoService.consultarDisponibles().stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst();
    }

    private Optional<DetallePedido> buscarEnSeleccion(List<DetallePedido> seleccion, String codigo) {
        return seleccion.stream().filter(d -> d.getProducto().getCodigo().equals(codigo)).findFirst();
    }

    private void mostrarMiPedido(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession sesion = request.getSession();
        List<DetallePedido> seleccion = obtenerSeleccion(sesion);
        Object confirmado = sesion.getAttribute(PEDIDO_CONFIRMADO);
        if (confirmado != null) {
            request.setAttribute(PEDIDO_CONFIRMADO, confirmado);
            sesion.removeAttribute(PEDIDO_CONFIRMADO);
        }
        try {
            request.setAttribute("productos", productoService.consultarDisponibles());
        } catch (IllegalStateException e) {
            request.setAttribute(ERROR, e.getMessage());
        }
        request.setAttribute("total", gestionPedidoService.calcularTotal(seleccion));
        request.getRequestDispatcher("/WEB-INF/vistas/mi-pedido.jsp").forward(request, response);
    }

    private void mostrarConsulta(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String codigoCliente = codigoCliente(request.getSession());
        String codigoRetiro = request.getParameter(CODIGO);
        try {
            if (codigoRetiro != null) {
                Optional<Pedido> pedido = consultaPedidoService.consultarPedidoDelCliente(codigoCliente, codigoRetiro);
                if (pedido.isPresent()) {
                    request.setAttribute("pedido", pedido.get());
                } else {
                    request.setAttribute(ERROR, "No se encontró el pedido.");
                }
            }
            request.setAttribute("misPedidos", consultaPedidoService.listarPedidosDelCliente(codigoCliente));
        } catch (IllegalStateException e) {
            request.setAttribute(ERROR, e.getMessage());
        }
        request.getRequestDispatcher("/WEB-INF/vistas/consultar-pedido.jsp").forward(request, response);
    }

    @SuppressWarnings("unchecked")
    private List<DetallePedido> obtenerSeleccion(HttpSession sesion) {
        List<DetallePedido> seleccion = (List<DetallePedido>) sesion.getAttribute(SELECCION);
        if (seleccion == null) {
            ArrayList<DetallePedido> nueva = new ArrayList<>();
            sesion.setAttribute(SELECCION, nueva);
            seleccion = nueva;
        }
        return seleccion;
    }

    private String codigoCliente(HttpSession sesion) {
        Cliente cliente = (Cliente) sesion.getAttribute("cliente");
        return cliente == null ? null : cliente.getCodigo();
    }
}
