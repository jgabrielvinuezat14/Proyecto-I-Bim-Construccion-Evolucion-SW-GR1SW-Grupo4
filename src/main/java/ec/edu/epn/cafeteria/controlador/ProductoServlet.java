package ec.edu.epn.cafeteria.controlador;

import ec.edu.epn.cafeteria.modelo.Producto;
import ec.edu.epn.cafeteria.servicio.ProductoService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Atiende la consulta del menú del cliente (HU-01) y la administración del menú
 * por parte del personal (HU-06).
 */
@WebServlet(urlPatterns = {"/menu", "/admin/productos"})
public class ProductoServlet extends HttpServlet {

    private final transient ProductoService productoService = new ProductoService();

    /**
     * Muestra el menú del cliente o la tabla de administración de productos.
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/menu".equals(request.getServletPath())) {
                request.setAttribute("productos", productoService.consultarDisponibles());
                request.getRequestDispatcher("/WEB-INF/vistas/menu.jsp").forward(request, response);
                return;
            }
            mostrarAdministracion(request, response, null);
        } catch (IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/vistas/menu.jsp").forward(request, response);
        }
    }

    /**
     * Registra un producto nuevo o guarda los cambios de uno existente.
     *
     * @param request  solicitud HTTP con los campos del formulario
     * @param response respuesta HTTP
     * @throws ServletException si falla el despacho a la vista
     * @throws IOException      si falla la redirección
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean nuevo = "nuevo".equals(request.getParameter("accion"));
        try {
            Producto producto = leerFormulario(request);
            if (nuevo) {
                productoService.registrarProducto(producto);
                request.getSession().setAttribute("mensaje", "Producto «" + producto.getNombre() + "» registrado.");
            } else {
                productoService.actualizarProducto(producto);
                request.getSession().setAttribute("mensaje", "Producto «" + producto.getNombre() + "» actualizado.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/productos");
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            mostrarAdministracion(request, response, nuevo ? "nuevo" : "editar");
        }
    }

    private void mostrarAdministracion(HttpServletRequest request, HttpServletResponse response, String formulario)
            throws ServletException, IOException {
        request.setAttribute("productos", productoService.consultarTodos());
        String editar = request.getParameter("editar");
        if (formulario == null && editar != null) {
            productoService.consultarTodos().stream()
                    .filter(p -> p.getCodigo().equals(editar))
                    .findFirst()
                    .ifPresent(p -> request.setAttribute("productoEditar", p));
            formulario = "editar";
        }
        if (formulario == null && request.getParameter("nuevo") != null) {
            formulario = "nuevo";
        }
        request.setAttribute("formulario", formulario);
        request.getRequestDispatcher("/WEB-INF/vistas/admin-productos.jsp").forward(request, response);
    }

    private Producto leerFormulario(HttpServletRequest request) {
        Producto producto = new Producto();
        String codigo = request.getParameter("codigo");
        producto.setCodigo(codigo == null ? null : codigo.trim().toUpperCase());
        String nombre = request.getParameter("nombre");
        producto.setNombre(nombre == null ? null : nombre.trim());
        try {
            producto.setPrecio(new BigDecimal(request.getParameter("precio").trim()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("El precio ingresado no es válido.");
        }
        try {
            producto.setCantidadDisponible(Integer.parseInt(request.getParameter("unidades").trim()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Las unidades deben ser un número entero mayor o igual a cero.");
        }
        producto.setDisponible("true".equals(request.getParameter("disponible")));
        request.setAttribute("productoEditar", producto);
        return producto;
    }
}
