<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Gestión de pedidos</h1>

<section class="tarjeta">
    <form method="get" action="${pageContext.request.contextPath}/admin/pedidos" class="fila">
        <label>Estado
            <select name="estado" onchange="this.form.submit()">
                <c:forEach var="e" items="${estados}">
                    <option value="${e}" ${e == estadoFiltro ? 'selected' : ''}>${e}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit">Filtrar</button>
    </form>
    <c:choose>
        <c:when test="${empty pedidos}">
            <p class="aviso">No hay pedidos en ese estado.</p>
        </c:when>
        <c:otherwise>
            <table class="tabla">
                <thead><tr><th>Código de retiro</th><th>Fecha de registro</th><th>Cliente</th><th>Estado</th><th>Total</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="p" items="${pedidos}">
                    <tr>
                        <td><c:out value="${p.codigoRetiro}"/></td>
                        <td>${p.fechaRegistro.toString().replace('T', ' ')}</td>
                        <td><c:out value="${p.cliente.nombre} ${p.cliente.apellido}"/></td>
                        <td><span class="estado ${p.estado}">${p.estado}</span></td>
                        <td>$<fmt:formatNumber value="${p.total}" pattern="0.00"/></td>
                        <td><a class="boton" href="${pageContext.request.contextPath}/admin/pedidos?estado=${estadoFiltro}&amp;detalle=${p.codigoRetiro}">Ver detalle</a></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</section>

<c:if test="${not empty pedidoDetalle}">
    <section class="tarjeta">
        <h2>Detalle del pedido <c:out value="${pedidoDetalle.codigoRetiro}"/></h2>
        <p>Cliente: <c:out value="${pedidoDetalle.cliente.nombre} ${pedidoDetalle.cliente.apellido}"/> ·
           Estado: <strong class="estado ${pedidoDetalle.estado}">${pedidoDetalle.estado}</strong></p>
        <table class="tabla">
            <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
            <tbody>
            <c:forEach var="d" items="${pedidoDetalle.detalles}">
                <tr>
                    <td><c:out value="${d.producto.nombre}"/></td>
                    <td>${d.cantidad}</td>
                    <td>$<fmt:formatNumber value="${d.precioUnitario}" pattern="0.00"/></td>
                    <td>$<fmt:formatNumber value="${d.calcularSubtotal()}" pattern="0.00"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <p class="total">Total: $<fmt:formatNumber value="${pedidoDetalle.total}" pattern="0.00"/></p>
    </section>
</c:if>
<%@ include file="pie.jspf" %>
