<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Consultar pedido</h1>

<section class="tarjeta">
    <h2>Buscar por código de retiro</h2>
    <form method="get" action="${pageContext.request.contextPath}/consultar-pedido" class="fila">
        <label>Código de retiro
            <input type="text" name="codigo" maxlength="6" value="<c:out value='${param.codigo}'/>">
        </label>
        <button type="submit">Consultar</button>
    </form>
</section>

<c:if test="${not empty pedido}">
    <section class="tarjeta">
        <h2>Detalle del pedido <c:out value="${pedido.codigoRetiro}"/></h2>
        <p>Código de retiro: <strong class="codigo"><c:out value="${pedido.codigoRetiro}"/></strong></p>
        <p>Fecha de registro: ${pedido.fechaRegistro.toString().replace('T', ' ')} ·
           Estado: <strong class="estado ${pedido.estado}">${pedido.estado}</strong></p>
        <table class="tabla">
            <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
            <tbody>
            <c:forEach var="d" items="${pedido.detalles}">
                <tr>
                    <td><c:out value="${d.producto.nombre}"/></td>
                    <td>${d.cantidad}</td>
                    <td>$<fmt:formatNumber value="${d.precioUnitario}" pattern="0.00"/></td>
                    <td>$<fmt:formatNumber value="${d.calcularSubtotal()}" pattern="0.00"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <p class="total">Total: $<fmt:formatNumber value="${pedido.total}" pattern="0.00"/></p>
    </section>
</c:if>

<section class="tarjeta">
    <h2>Mis pedidos</h2>
    <c:choose>
        <c:when test="${empty misPedidos}">
            <p class="aviso">No tiene pedidos registrados.</p>
        </c:when>
        <c:otherwise>
            <table class="tabla">
                <thead><tr><th>Código de retiro</th><th>Fecha de registro</th><th>Estado</th><th>Total</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="p" items="${misPedidos}">
                    <tr>
                        <td><c:out value="${p.codigoRetiro}"/></td>
                        <td>${p.fechaRegistro.toString().replace('T', ' ')}</td>
                        <td><span class="estado ${p.estado}">${p.estado}</span></td>
                        <td>$<fmt:formatNumber value="${p.total}" pattern="0.00"/></td>
                        <td><a class="boton" href="${pageContext.request.contextPath}/consultar-pedido?codigo=${p.codigoRetiro}">Ver detalle</a></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</section>
<%@ include file="pie.jspf" %>
