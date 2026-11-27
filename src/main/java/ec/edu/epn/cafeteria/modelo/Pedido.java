package ec.edu.epn.cafeteria.modelo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido confirmado por un cliente universitario.
 * Contiene uno o varios detalles y un código de retiro único.
 * <p><b>Trazabilidad</b></p>
 * <ul>
 * <li>HU-03 Total del pedido: {@link #calcularTotal()}.</li>
 * <li>HU-04 Confirmar pedido: se registra con fecha, total, estado PENDIENTE y código de retiro.</li>
 * <li>HU-05, HU-07 y HU-08: consulta, gestión y entrega por parte del cliente y del personal.</li>
 * <li>RN-NEG-01: un pedido tiene al menos un producto.</li>
 * <li>RN-NEG-03 y RNF-02: el total es la suma de los subtotales con dos decimales.</li>
 * <li>RN-NEG-05: el código de retiro de 6 caracteres es único.</li>
 * <li>RN-NEG-06: el estado sigue el ciclo definido en {@link EstadoPedido}.</li>
 * <li>Tabla dbo.Pedido del modelo de datos.</li>
 * </ul>
 */
public class Pedido implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String codigoRetiro;
    private LocalDateTime fechaRegistro;
    private BigDecimal total;
    private EstadoPedido estado;
    private Cliente cliente;
    private List<DetallePedido> detalles = new ArrayList<>();

    /**
     * Crea un pedido vacío, sin detalles; sus datos se asignan con los métodos set.
     */
    public Pedido() {
        // Constructor vacío: los detalles se agregan después.
    }

    /**
     * Calcula el total del pedido como la suma de los subtotales de sus detalles (RN-NEG-03).
     * <p>Trazabilidad: HU-03, RN-NEG-03, RNF-02.</p>
     *
     * @return total con dos decimales; cero si el pedido no tiene detalles
     */
    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO.setScale(2);
        for (DetallePedido detalle : detalles) {
            suma = suma.add(detalle.calcularSubtotal());
        }
        return suma;
    }

    /**
     * Devuelve el identificador del pedido.
     *
     * @return el identificador del pedido
     */
    public long getId() {
        return id;
    }

    /**
     * Asigna el identificador del pedido.
     *
     * @param id el identificador del pedido
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Devuelve el código de retiro del pedido.
     *
     * @return el código de retiro del pedido
     */
    public String getCodigoRetiro() {
        return codigoRetiro;
    }

    /**
     * Asigna el código de retiro del pedido.
     *
     * @param codigoRetiro el código de retiro del pedido
     */
    public void setCodigoRetiro(String codigoRetiro) {
        this.codigoRetiro = codigoRetiro;
    }

    /**
     * Devuelve la fecha de registro del pedido.
     *
     * @return la fecha de registro del pedido
     */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Asigna la fecha de registro del pedido.
     *
     * @param fechaRegistro la fecha de registro del pedido
     */
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    /**
     * Devuelve el importe total del pedido.
     *
     * @return el importe total del pedido
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Asigna el importe total del pedido.
     *
     * @param total el importe total del pedido
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    /**
     * Devuelve el estado del pedido.
     *
     * @return el estado del pedido
     */
    public EstadoPedido getEstado() {
        return estado;
    }

    /**
     * Asigna el estado del pedido.
     *
     * @param estado el estado del pedido
     */
    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    /**
     * Devuelve el cliente del pedido.
     *
     * @return el cliente del pedido
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Asigna el cliente del pedido.
     *
     * @param cliente el cliente del pedido
     */
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    /**
     * Devuelve los detalles del pedido.
     *
     * @return los detalles del pedido
     */
    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    /**
     * Asigna los detalles del pedido.
     *
     * @param detalles los detalles del pedido
     */
    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }
}
