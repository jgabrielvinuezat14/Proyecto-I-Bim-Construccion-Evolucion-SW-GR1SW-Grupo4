package ec.edu.epn.cafeteria.controlador;

import ec.edu.epn.cafeteria.servicio.AccesoPersonalService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Atiende el inicio y cierre de sesión del personal de cafetería (HU-10).
 */
@WebServlet(urlPatterns = {"/personal/acceso", "/personal/salir"})
public class AccesoPersonalServlet extends HttpServlet {

    private final transient AccesoPersonalService accesoPersonalService;

    /** Crea el servlet con el servicio por defecto. */
    public AccesoPersonalServlet() {
        this(new AccesoPersonalService());
    }

    /**
     * Crea el servlet con un servicio concreto.
     *
     * @param accesoPersonalService servicio de autenticación del personal
     */
    public AccesoPersonalServlet(AccesoPersonalService accesoPersonalService) {
        this.accesoPersonalService = accesoPersonalService;
    }

    /**
     * Muestra el formulario de acceso del personal o cierra la sesión activa si se solicita salir.
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la redirección
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/personal/salir".equals(request.getServletPath())) {
            cerrarSesion(request, response);
            return;
        }
        HttpSession sesion = request.getSession(false);
        if (sesion != null && Boolean.TRUE.equals(sesion.getAttribute("personal"))) {
            response.sendRedirect(request.getContextPath() + "/admin/pedidos");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/vistas/acceso-personal.jsp").forward(request, response);
    }

    /**
     * Procesa la autenticación del personal o el cierre de su sesión.
     *
     * @param request  solicitud HTTP con las credenciales del formulario
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/personal/salir".equals(request.getServletPath())) {
            cerrarSesion(request, response);
            return;
        }

        String codigo = leerParametro(request, "codigo", "codigoAcceso");
        String contrasena = request.getParameter("contrasena");
        request.setAttribute("codigo", codigo == null ? "" : codigo.trim());

        try {
            accesoPersonalService.autenticar(codigo, contrasena);
            HttpSession anterior = request.getSession(false);
            if (anterior != null) {
                anterior.invalidate();
            }
            HttpSession nuevaSesion = request.getSession(true);
            nuevaSesion.setAttribute("personal", Boolean.TRUE);
            nuevaSesion.setAttribute("mensaje", "Sesión de personal iniciada correctamente.");
            response.sendRedirect(request.getContextPath() + "/admin/pedidos");
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/vistas/acceso-personal.jsp").forward(request, response);
        }
    }

    private void cerrarSesion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        request.getSession(true).setAttribute("mensaje", "Ha cerrado la sesión de personal.");
        response.sendRedirect(request.getContextPath() + "/menu");
    }

    private String leerParametro(HttpServletRequest request, String principal, String alternativo) {
        String valor = request.getParameter(principal);
        return valor != null ? valor : request.getParameter(alternativo);
    }
}
