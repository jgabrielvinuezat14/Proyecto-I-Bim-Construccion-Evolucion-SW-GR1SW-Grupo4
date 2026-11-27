package ec.edu.epn.cafeteria.soporte;

import ec.edu.epn.cafeteria.dao.ProductoDAO;
import ec.edu.epn.cafeteria.modelo.Producto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ProductoDAO en memoria para las pruebas unitarias. Aplica las mismas condiciones
 * que las sentencias SQL del DAO real.
 */
public class ProductoDAOEnMemoria extends ProductoDAO {

    private final Map<String, Producto> productos = new LinkedHashMap<>();

    public ProductoDAOEnMemoria() {
        super(new ConexionFalsa());
    }

    /** Agrega un producto de prueba y lo devuelve. */
    public Producto agregar(String codigo, String nombre, String precio, boolean disponible, int unidades) {
        Producto p = new Producto(codigo, nombre, new BigDecimal(precio), disponible, unidades);
        productos.put(codigo, p);
        return copia(p);
    }

    /** Devuelve el producto guardado (no una copia) para verificar cambios. */
    public Producto guardado(String codigo) {
        return productos.get(codigo);
    }

    @Override
    public List<Producto> listarDisponibles() {
        return productos.values().stream()
                .filter(p -> p.isDisponible() && p.getCantidadDisponible() > 0)
                .sorted(Comparator.comparing(Producto::getNombre))
                .map(this::copia).toList();
    }

    @Override
    public List<Producto> listarTodos() {
        return new ArrayList<>(productos.values().stream().map(this::copia).toList());
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(productos.get(codigo)).map(this::copia);
    }

    @Override
    public void guardar(Producto producto) {
        productos.put(producto.getCodigo(), copia(producto));
    }

    @Override
    public void actualizar(Producto producto) {
        productos.put(producto.getCodigo(), copia(producto));
    }

    @Override
    public synchronized boolean descontarUnidades(String codigo, int cantidad, Connection conexion) {
        Producto p = productos.get(codigo);
        if (p == null || !p.isDisponible() || p.getCantidadDisponible() < cantidad) {
            return false;
        }
        p.setCantidadDisponible(p.getCantidadDisponible() - cantidad);
        return true;
    }

    private Producto copia(Producto p) {
        return new Producto(p.getCodigo(), p.getNombre(), p.getPrecio(), p.isDisponible(), p.getCantidadDisponible());
    }
}
