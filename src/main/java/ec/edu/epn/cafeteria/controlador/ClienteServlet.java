package ec.edu.epn.cafeteria.controlador;

import ec.edu.epn.cafeteria.modelo.Cliente;
import ec.edu.epn.cafeteria.servicio.ClienteService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Atiende el registro, el inicio de sesión y el cierre de sesión del cliente universitario.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-09 Registro e inicio de sesión del cliente.</li>
 * <li>RNF-04 Seguridad básica: el cliente autenticado se guarda en la sesión.</li>
 * <li>ADR-01: controlador de la capa web (MVC).</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/login", "/registro", "/logout"})
public class ClienteServlet extends HttpServlet {

    private static final String RUTA_MENU = "/menu";
    private static final String CORREO = "correo";
    private static final String MENSAJE = "mensaje";

    private final transient ClienteService clienteService;

    /** Crea el servlet con el servicio por defecto. */
    public ClienteServlet() {
        this(new ClienteService());
    }

    /**
     * Crea el servlet con un servicio concreto.
     *
     * @param clienteService servicio de clientes
     */
    public ClienteServlet(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * Muestra el formulario de inicio de sesión o de registro, o cierra la sesión activa.
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la redirección
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String ruta = request.getServletPath();
        if ("/logout".equals(ruta)) {
            cerrarSesion(request, response);
            return;
        }
        HttpSession sesionActual = request.getSession(false);
        if (sesionActual != null && sesionActual.getAttribute("cliente") != null) {
            response.sendRedirect(request.getContextPath() + RUTA_MENU);
            return;
        }
        if ("/registro".equals(ruta)) {
            request.getRequestDispatcher("/WEB-INF/vistas/registro.jsp").forward(request, response);
            return;
        }
        request.getRequestDispatcher("/WEB-INF/vistas/login.jsp").forward(request, response);
    }

    /**
     * Procesa el inicio de sesión, el registro de un nuevo cliente o el cierre de sesión.
     *
     * @param request  solicitud HTTP con los campos del formulario
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String ruta = request.getServletPath();
        if ("/logout".equals(ruta)) {
            cerrarSesion(request, response);
            return;
        }
        if ("/registro".equals(ruta)) {
            procesarRegistro(request, response);
            return;
        }
        procesarLogin(request, response);
    }

    private void procesarLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String correo = leerParametro(request, CORREO, "correoInstitucional");
        String contrasena = request.getParameter("contrasena");
        request.setAttribute(CORREO, correo == null ? "" : correo.trim());
        try {
            Cliente autenticado = clienteService.iniciarSesion(correo, contrasena);
            HttpSession sesion = iniciarSesionHttp(request, autenticado);
            sesion.setAttribute(MENSAJE, "Sesión iniciada como " + autenticado.getNombre()
                    + " (" + autenticado.getCodigo() + ").");
            response.sendRedirect(request.getContextPath() + RUTA_MENU);
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/vistas/login.jsp").forward(request, response);
        }
    }

    private void procesarRegistro(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Cliente datos = leerFormularioRegistro(request);
        String contrasena = request.getParameter("contrasena");
        request.setAttribute("clienteRegistro", datos);
        try {
            Cliente registrado = clienteService.registrarCliente(datos, contrasena);
            HttpSession anterior = request.getSession(false);
            if (anterior != null) {
                anterior.invalidate();
            }
            HttpSession sesion = request.getSession(true);
            sesion.setAttribute(MENSAJE, "Cuenta registrada correctamente. Su código de cliente es "
                    + registrado.getCodigo() + ".");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/vistas/registro.jsp").forward(request, response);
        }
    }

    private void cerrarSesion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        request.getSession(true).setAttribute(MENSAJE, "Ha cerrado sesión correctamente.");
        response.sendRedirect(request.getContextPath() + RUTA_MENU);
    }

    private HttpSession iniciarSesionHttp(HttpServletRequest request, Cliente cliente) {
        HttpSession sesion = request.getSession(true);
        sesion.removeAttribute("personal");
        Cliente clienteSesion = new Cliente();
        clienteSesion.setCodigo(cliente.getCodigo());
        clienteSesion.setNombre(cliente.getNombre());
        clienteSesion.setApellido(cliente.getApellido());
        clienteSesion.setCedula(cliente.getCedula());
        clienteSesion.setCelular(cliente.getCelular());
        clienteSesion.setCorreoInstitucional(cliente.getCorreoInstitucional());
        sesion.setAttribute("cliente", clienteSesion);
        return sesion;
    }

    private Cliente leerFormularioRegistro(HttpServletRequest request) {
        Cliente cliente = new Cliente();
        cliente.setNombre(recortar(request.getParameter("nombre")));
        cliente.setApellido(recortar(request.getParameter("apellido")));
        cliente.setCedula(recortar(request.getParameter("cedula")));
        cliente.setCelular(recortar(request.getParameter("celular")));
        cliente.setCorreoInstitucional(recortar(leerParametro(request, CORREO, "correoInstitucional")));
        return cliente;
    }

    private String leerParametro(HttpServletRequest request, String principal, String alternativo) {
        String valor = request.getParameter(principal);
        return valor != null ? valor : request.getParameter(alternativo);
    }

    private String recortar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
