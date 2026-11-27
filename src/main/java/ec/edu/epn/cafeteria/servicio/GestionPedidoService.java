package ec.edu.epn.cafeteria.servicio;

import ec.edu.epn.cafeteria.config.ConexionBD;
import ec.edu.epn.cafeteria.dao.PedidoDAO;
import ec.edu.epn.cafeteria.dao.ProductoDAO;
import ec.edu.epn.cafeteria.modelo.Cliente;
import ec.edu.epn.cafeteria.modelo.DetallePedido;
import ec.edu.epn.cafeteria.modelo.EstadoPedido;
import ec.edu.epn.cafeteria.modelo.Pedido;
import ec.edu.epn.cafeteria.modelo.Producto;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cálculo y confirmación de pedidos, avance de estado y registro de la entrega.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-03 Total del pedido: {@link #calcularTotal(java.util.List)}.</li>
 * <li>HU-04 Confirmar pedido: {@link #confirmarPedido(String, java.util.List)}.</li>
 * <li>HU-08 Avance y entrega: {@link #avanzarEstado(long)} y {@link #registrarEntrega(String)}.</li>
 * <li>RN-NEG-01 a RN-NEG-07, RN-03 y RN-04.</li>
 * <li>RNF-01, RNF-02 y RNF-03 (control de concurrencia al descontar unidades y cambiar estado).</li>
 * <li>CA-04.4: se usa el precio vigente al confirmar.</li>
 * <li>ADR-04: la confirmación se hace en una sola transacción.</li>
 * </ul>
 */
public class GestionPedidoService {

    static final String MSG_VACIO = "El pedido debe incluir al menos un producto.";
    static final String MSG_CANTIDAD = "La cantidad debe ser un número entero positivo.";
    static final String MSG_CODIGO_INCORRECTO = "El código no corresponde a ningún pedido listo para retirar.";
    static final String MSG_NO_LISTO = "El pedido aún no está listo para retirar.";
    static final String MSG_YA_ENTREGADO = "El pedido ya fue entregado.";

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INTENTOS_CODIGO = 5;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final ProductoDAO productoDAO;
    private final PedidoDAO pedidoDAO;
    private final ConexionBD conexionBD;

    /** Crea el servicio con los DAO y la conexión por defecto. */
    public GestionPedidoService() {
        this(new ProductoDAO(), new PedidoDAO(), new ConexionBD());
    }

    /**
     * Crea el servicio con dependencias concretas.
     *
     * @param productoDAO acceso a datos de productos
     * @param pedidoDAO   acceso a datos de pedidos
     * @param conexionBD  proveedor de la conexión de la transacción de confirmación
     */
    public GestionPedidoService(ProductoDAO productoDAO, PedidoDAO pedidoDAO, ConexionBD conexionBD) {
        this.productoDAO = productoDAO;
        this.pedidoDAO = pedidoDAO;
        this.conexionBD = conexionBD;
    }

    /**
     * Calcula el total de una selección (RN-NEG-03).
     * <p>Trazabilidad: HU-03, RN-NEG-03, RNF-02.</p>
     *
     * @param detalles productos seleccionados con su cantidad y precio unitario
     * @return suma de los subtotales; cero si la selección está vacía
     */
    public BigDecimal calcularTotal(List<DetallePedido> detalles) {
        Pedido pedido = new Pedido();
        if (detalles != null) {
            pedido.setDetalles(detalles);
        }
        return pedido.calcularTotal();
    }

    /**
     * Confirma la selección del cliente como un pedido en estado PENDIENTE (HU-04).
     * Antes de registrar comprueba disponibilidad y precio vigente; si un precio cambió
     * desde la revisión, actualiza la selección y no confirma (CA-04.4). El descuento de
     * unidades y el registro del pedido se hacen en una sola transacción (RNF-03, ADR-04).
     * <p>Trazabilidad: HU-04, RN-NEG-01 a RN-NEG-05, CA-04.4, RNF-03, ADR-04.</p>
     *
     * @param codigoCliente código del cliente que confirma
     * @param detalles      productos seleccionados
     * @return pedido registrado con su código de retiro
     * @throws IllegalArgumentException si la selección está vacía o tiene cantidades inválidas
     * @throws IllegalStateException    si un producto no está disponible, no tiene unidades
     *                                  suficientes o cambió de precio
     */
    public Pedido confirmarPedido(String codigoCliente, List<DetallePedido> detalles) {
        validarSeleccion(detalles);
        try {
            comprobarProductos(detalles);
            Pedido pedido = new Pedido();
            Cliente cliente = new Cliente();
            cliente.setCodigo(codigoCliente);
            pedido.setCliente(cliente);
            pedido.setDetalles(new ArrayList<>(detalles));
            pedido.setEstado(EstadoPedido.PENDIENTE);
            pedido.setFechaRegistro(LocalDateTime.now().withNano(0));
            pedido.setTotal(pedido.calcularTotal());
            registrarEnTransaccion(pedido);
            return pedido;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo registrar el pedido. Intente de nuevo.", e);
        }
    }

    /**
     * Avanza un pedido al siguiente estado: PENDIENTE a EN_PREPARACION o
     * EN_PREPARACION a LISTO_PARA_RETIRAR (HU-08). La entrega se registra con
     * {@link #registrarEntrega(String)}.
     * <p>Trazabilidad: HU-08, RN-NEG-06, RNF-03.</p>
     *
     * @param idPedido identificador del pedido
     * @throws IllegalStateException si el pedido no puede avanzar desde su estado actual
     */
    public void avanzarEstado(long idPedido) {
        try {
            if (pedidoDAO.actualizarEstado(idPedido, EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION)) {
                return;
            }
            if (pedidoDAO.actualizarEstado(idPedido, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO_PARA_RETIRAR)) {
                return;
            }
            throw new IllegalStateException("El pedido no puede avanzar desde su estado actual.");
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el pedido. Intente de nuevo.", e);
        }
    }

    /**
     * Registra la entrega del pedido que corresponde al código presentado por el cliente.
     * Solo se entrega un pedido LISTO_PARA_RETIRAR (RN-NEG-07).
     * <p>Trazabilidad: HU-08, RN-04, RN-NEG-07.</p>
     *
     * @param codigoRetiro código que presenta el cliente
     * @throws IllegalStateException si el código no corresponde a un pedido listo para retirar
     */
    public void registrarEntrega(String codigoRetiro) {
        String codigo = codigoRetiro == null ? "" : codigoRetiro.trim().toUpperCase();
        if (codigo.isEmpty()) {
            throw new IllegalArgumentException("Ingrese el código de retiro.");
        }
        try {
            Optional<Pedido> encontrado = pedidoDAO.buscarPorCodigoRetiro(codigo);
            Pedido pedido = encontrado.orElseThrow(() -> new IllegalStateException(MSG_CODIGO_INCORRECTO));
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
                throw new IllegalStateException(MSG_YA_ENTREGADO);
            }
            if (pedido.getEstado() != EstadoPedido.LISTO_PARA_RETIRAR) {
                throw new IllegalStateException(MSG_NO_LISTO);
            }
            if (!pedidoDAO.actualizarEstado(pedido.getId(), EstadoPedido.LISTO_PARA_RETIRAR, EstadoPedido.ENTREGADO)) {
                throw new IllegalStateException(MSG_YA_ENTREGADO);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo registrar la entrega. Intente de nuevo.", e);
        }
    }

    /**
     * Verifica que el pedido tenga al menos un producto y que cada cantidad sea mayor que cero.
     * <p>Trazabilidad: HU-04, RN-NEG-01.</p>
     *
     * @param detalles productos elegidos
     * @throws IllegalArgumentException si la selección está vacía o tiene cantidades no válidas
     */
    private void validarSeleccion(List<DetallePedido> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException(MSG_VACIO);
        }
        for (DetallePedido detalle : detalles) {
            if (detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException(MSG_CANTIDAD);
            }
        }
    }

    /**
     * Comprueba que cada producto siga disponible, tenga unidades suficientes y mantenga su precio.
     * Si un precio cambió, actualiza el detalle y pide al cliente volver a confirmar.
     * <p>Trazabilidad: HU-04, RN-NEG-02, RN-NEG-04, CA-04.4.</p>
     *
     * @param detalles productos elegidos
     * @throws IllegalStateException si un producto no está disponible, no tiene unidades o cambió de precio
     * @throws SQLException si falla la consulta
     */
    private void comprobarProductos(List<DetallePedido> detalles) throws SQLException {
        List<String> cambiados = new ArrayList<>();
        for (DetallePedido detalle : detalles) {
            String nombre = detalle.getProducto().getNombre();
            Optional<Producto> actual = productoDAO.buscarPorCodigo(detalle.getProducto().getCodigo());
            if (actual.isEmpty() || !actual.get().isDisponible()) {
                throw new IllegalStateException("«" + nombre + "» ya no está disponible.");
            }
            Producto producto = actual.get();
            if (producto.getCantidadDisponible() < detalle.getCantidad()) {
                throw new IllegalStateException("No hay unidades suficientes de «" + nombre
                        + "». Unidades disponibles: " + producto.getCantidadDisponible() + ".");
            }
            if (producto.getPrecio().compareTo(detalle.getPrecioUnitario()) != 0) {
                detalle.setPrecioUnitario(producto.getPrecio());
                detalle.getProducto().setPrecio(producto.getPrecio());
                cambiados.add("«" + nombre + "»");
            }
        }
        if (!cambiados.isEmpty()) {
            throw new IllegalStateException("El precio de " + String.join(", ", cambiados)
                    + " cambió. Revise el total actualizado y vuelva a confirmar.");
        }
    }

    /**
     * Descuenta las unidades y guarda el pedido en una sola transacción; si algo falla, deshace todo.
     * <p>Trazabilidad: HU-04, RNF-01, RNF-03, ADR-04.</p>
     *
     * @param pedido pedido que se registra
     * @throws IllegalStateException si otro pedido se llevó las unidades
     * @throws SQLException si falla la base de datos
     */
    private void registrarEnTransaccion(Pedido pedido) throws SQLException {
        try (Connection cn = conexionBD.obtenerConexion()) {
            cn.setAutoCommit(false);
            try {
                for (DetallePedido detalle : pedido.getDetalles()) {
                    Producto producto = detalle.getProducto();
                    if (!productoDAO.descontarUnidades(producto.getCodigo(), detalle.getCantidad(), cn)) {
                        throw new IllegalStateException("No hay unidades suficientes de «" + producto.getNombre() + "».");
                    }
                }
                guardarConCodigoUnico(pedido, cn);
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    /**
     * Guarda el pedido generando un código de retiro; si el código ya existe, lo genera otra vez.
     * <p>Trazabilidad: HU-04, RN-NEG-05.</p>
     *
     * @param pedido pedido que se registra
     * @param cn conexión de la transacción en curso
     * @throws SQLException si falla la inserción o se agotan los intentos
     */
    private void guardarConCodigoUnico(Pedido pedido, Connection cn) throws SQLException {
        for (int intento = 1; ; intento++) {
            pedido.setCodigoRetiro(generarCodigoRetiro());
            try {
                pedidoDAO.guardar(pedido, cn);
                return;
            } catch (SQLException e) {
                boolean repetido = e.getMessage() != null && e.getMessage().contains("UQ_Pedido_codigo_retiro");
                if (!repetido || intento >= INTENTOS_CODIGO) {
                    throw e;
                }
            }
        }
    }

    /**
     * Genera un código de retiro aleatorio de 6 caracteres.
     * <p>Trazabilidad: HU-04, RN-NEG-05.</p>
     *
     * @return el código generado
     */
    private String generarCodigoRetiro() {
        StringBuilder codigo = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            codigo.append(CARACTERES.charAt(ALEATORIO.nextInt(CARACTERES.length())));
        }
        return codigo.toString();
    }
}
