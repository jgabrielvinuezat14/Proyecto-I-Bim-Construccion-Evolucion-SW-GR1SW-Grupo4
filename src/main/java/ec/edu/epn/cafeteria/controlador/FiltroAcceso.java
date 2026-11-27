package ec.edu.epn.cafeteria.controlador;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filtro de control de acceso en el servidor.
 * Protege las rutas administrativas ({@code /admin/*}) exigiendo sesión activa de personal
 * e impidiendo el acceso a usuarios no autenticados o con sesión de cliente, y protege
 * las rutas de pedido del cliente ({@code /mi-pedido}, {@code /consultar-pedido}) exigiendo
 * sesión activa de cliente.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-09: rutas del cliente solo con sesión de cliente.</li>
 * <li>HU-10: protege HU-06, HU-07 y HU-08 con sesión de personal.</li>
 * <li>RNF-04 Seguridad básica: el control se hace en el servidor.</li>
 * </ul>
 */
@WebFilter(urlPatterns = {"/admin/*", "/mi-pedido", "/consultar-pedido"})
public class FiltroAcceso implements Filter {

    /**
     * No requiere inicialización.
     *
     * @param filterConfig configuración del filtro
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // Sin configuración adicional al iniciar el filtro
    }

    /**
     * Verifica la sesión activa según la ruta solicitada antes de continuar la cadena.
     * <p>Trazabilidad: HU-09, HU-10, RNF-04.</p>
     *
     * @param request  solicitud entrante
     * @param response respuesta saliente
     * @param chain    cadena de filtros y servlet destino
     * @throws IOException      si falla la redirección o escritura
     * @throws ServletException si falla el procesamiento posterior
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession sesion = httpRequest.getSession(false);
        String ruta = httpRequest.getServletPath();

        if (ruta != null && ruta.startsWith("/admin")) {
            boolean esPersonal = sesion != null
                    && Boolean.TRUE.equals(sesion.getAttribute("personal"))
                    && sesion.getAttribute("cliente") == null;
            if (!esPersonal) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/personal/acceso");
                return;
            }
            deshabilitarCache(httpResponse);
            chain.doFilter(request, response);
            return;
        }

        boolean esCliente = sesion != null && sesion.getAttribute("cliente") != null;
        if (!esCliente) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        deshabilitarCache(httpResponse);
        chain.doFilter(request, response);
    }

    /**
     * No tiene recursos que liberar.
     */
    @Override
    public void destroy() {
        // Sin recursos por liberar
    }

    /**
     * Evita que el navegador guarde en caché las páginas protegidas,
     * para que no se vean después de cerrar sesión.
     * <p>Trazabilidad: HU-09, HU-10, RNF-04.</p>
     *
     * @param response respuesta HTTP
     */
    private void deshabilitarCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
