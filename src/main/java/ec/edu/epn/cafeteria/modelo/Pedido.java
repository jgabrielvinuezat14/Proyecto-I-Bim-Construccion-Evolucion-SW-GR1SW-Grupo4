package ec.edu.epn.cafeteria.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido confirmado por un cliente universitario.
 * Contiene uno o varios detalles y un código de retiro único (RN-NEG-05).
 */
public class Pedido {

    private long id;
    private String codigoRetiro;
    private LocalDateTime fechaRegistro;
    private BigDecimal total;
    private EstadoPedido estado;
    private Cliente cliente;
    private List<DetallePedido> detalles = new ArrayList<>();

    /**
     * Calcula el total del pedido como la suma de los subtotales de sus detalles (RN-NEG-03).
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCodigoRetiro() {
        return codigoRetiro;
    }

    public void setCodigoRetiro(String codigoRetiro) {
        this.codigoRetiro = codigoRetiro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }
}
