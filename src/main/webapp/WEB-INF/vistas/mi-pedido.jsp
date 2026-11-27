<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Mi pedido</h1>

<c:if test="${not empty pedidoConfirmado}">
    <section class="tarjeta exito">
        <h2>Pedido confirmado</h2>
        <p>Código de retiro: <strong class="codigo"><c:out value="${pedidoConfirmado.codigoRetiro}"/></strong></p>
        <p>Estado: <strong>Pendiente</strong></p>
        <table class="tabla">
            <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
            <tbody>
            <c:forEach var="d" items="${pedidoConfirmado.detalles}">
                <tr>
                    <td><c:out value="${d.producto.nombre}"/></td>
                    <td>${d.cantidad}</td>
                    <td>$<fmt:formatNumber value="${d.precioUnitario}" pattern="0.00"/></td>
                    <td>$<fmt:formatNumber value="${d.calcularSubtotal()}" pattern="0.00"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <p class="total">Total: $<fmt:formatNumber value="${pedidoConfirmado.total}" pattern="0.00"/></p>
        <p>Presente el código de retiro en la cafetería cuando su pedido esté listo.</p>
    </section>
</c:if>

<section class="tarjeta">
    <h2>Agregar productos</h2>
    <form method="post" action="${pageContext.request.contextPath}/mi-pedido" class="fila">
        <input type="hidden" name="accion" value="agregar">
        <label>Producto
            <select name="producto" required>
                <c:forEach var="p" items="${productos}">
                    <option value="${p.codigo}"><c:out value="${p.nombre}"/> — $<fmt:formatNumber value="${p.precio}" pattern="0.00"/></option>
                </c:forEach>
            </select>
        </label>
        <label>Cantidad <input type="text" name="cantidad" value="1" size="4"></label>
        <button type="submit">Agregar al pedido</button>
    </form>
</section>

<section class="tarjeta">
    <h2>Productos seleccionados</h2>
    <c:choose>
        <c:when test="${empty sessionScope.seleccion}">
            <p class="aviso">Su pedido no tiene productos.</p>
        </c:when>
        <c:otherwise>
            <table class="tabla">
                <thead><tr><th>Producto</th><th>Precio unitario</th><th>Cantidad</th><th>Subtotal</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="d" items="${sessionScope.seleccion}">
                    <tr>
                        <td><c:out value="${d.producto.nombre}"/></td>
                        <td>$<fmt:formatNumber value="${d.precioUnitario}" pattern="0.00"/></td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/mi-pedido" class="fila">
                                <input type="hidden" name="accion" value="actualizar">
                                <input type="hidden" name="codigo" value="${d.producto.codigo}">
                                <input type="text" name="cantidad" value="${d.cantidad}" size="4" aria-label="Cantidad">
                                <button type="submit">Actualizar cantidad</button>
                            </form>
                        </td>
                        <td>$<fmt:formatNumber value="${d.calcularSubtotal()}" pattern="0.00"/></td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/mi-pedido">
                                <input type="hidden" name="accion" value="quitar">
                                <input type="hidden" name="codigo" value="${d.producto.codigo}">
                                <button type="submit" class="secundario">Quitar</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
    <form method="post" action="${pageContext.request.contextPath}/mi-pedido">
        <input type="hidden" name="accion" value="revisar">
        <button type="submit">Revisar pedido</button>
    </form>
</section>

<c:if test="${sessionScope.revision and not empty sessionScope.seleccion}">
    <section class="tarjeta">
        <h2>Revisión del pedido</h2>
        <table class="tabla">
            <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
            <tbody>
            <c:forEach var="d" items="${sessionScope.seleccion}">
                <tr>
                    <td><c:out value="${d.producto.nombre}"/></td>
                    <td>${d.cantidad}</td>
                    <td>$<fmt:formatNumber value="${d.precioUnitario}" pattern="0.00"/></td>
                    <td>$<fmt:formatNumber value="${d.calcularSubtotal()}" pattern="0.00"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <p class="total">Total: $<fmt:formatNumber value="${total}" pattern="0.00"/></p>
        <form method="post" action="${pageContext.request.contextPath}/mi-pedido">
            <input type="hidden" name="accion" value="confirmar">
            <button type="submit">Confirmar pedido</button>
        </form>
    </section>
</c:if>
<%@ include file="pie.jspf" %>
