<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Menú</h1>
<h2>Menú de la cafetería</h2>
<c:choose>
    <c:when test="${empty productos}">
        <p class="aviso">No hay productos disponibles en el menú en este momento</p>
        <table class="tabla"><thead><tr><th>Código</th><th>Nombre</th><th>Precio</th></tr></thead></table>
    </c:when>
    <c:otherwise>
        <table class="tabla">
            <thead><tr><th>Código</th><th>Nombre</th><th>Precio</th></tr></thead>
            <tbody>
            <c:forEach var="p" items="${productos}">
                <tr>
                    <td><c:out value="${p.codigo}"/></td>
                    <td><c:out value="${p.nombre}"/></td>
                    <td>$<fmt:formatNumber value="${p.precio}" pattern="0.00"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <p><a class="boton" href="${pageContext.request.contextPath}/mi-pedido">Ir a «Mi pedido»</a></p>
    </c:otherwise>
</c:choose>
<%@ include file="pie.jspf" %>
