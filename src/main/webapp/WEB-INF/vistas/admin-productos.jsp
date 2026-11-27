<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Administración del menú</h1>
<p><a class="boton" href="${pageContext.request.contextPath}/admin/productos?nuevo=1">Nuevo producto</a></p>

<c:if test="${not empty formulario}">
    <section class="tarjeta">
        <h2>${formulario == 'nuevo' ? 'Nuevo producto' : 'Editar producto'}</h2>
        <form method="post" action="${pageContext.request.contextPath}/admin/productos" class="formulario">
            <input type="hidden" name="accion" value="${formulario}">
            <label>Código
                <input type="text" name="codigo" maxlength="10" value="<c:out value='${productoEditar.codigo}'/>"
                       ${formulario == 'editar' ? 'readonly' : ''}>
            </label>
            <label>Nombre <input type="text" name="nombre" maxlength="100" value="<c:out value='${productoEditar.nombre}'/>"></label>
            <label>Precio <input type="text" name="precio" value="${productoEditar.precio}"></label>
            <label>Unidades disponibles <input type="text" name="unidades" value="${empty productoEditar ? 0 : productoEditar.cantidadDisponible}"></label>
            <label>Estado
                <select name="disponible">
                    <option value="true" ${empty productoEditar or productoEditar.disponible ? 'selected' : ''}>Disponible</option>
                    <option value="false" ${not empty productoEditar and not productoEditar.disponible ? 'selected' : ''}>No disponible</option>
                </select>
            </label>
            <button type="submit">Guardar</button>
            <a href="${pageContext.request.contextPath}/admin/productos">Cancelar</a>
        </form>
    </section>
</c:if>

<table class="tabla">
    <thead><tr><th>Código</th><th>Nombre</th><th>Precio</th><th>Unidades disponibles</th><th>Estado</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="p" items="${productos}">
        <c:set var="sinDisponibilidad" value="${not p.disponible or p.cantidadDisponible == 0}"/>
        <tr class="${sinDisponibilidad ? 'gris' : ''}">
            <td><c:out value="${p.codigo}"/></td>
            <td><c:out value="${p.nombre}"/></td>
            <td>$<fmt:formatNumber value="${p.precio}" pattern="0.00"/></td>
            <td>${p.cantidadDisponible}</td>
            <td>
                ${p.disponible ? 'Disponible' : 'No disponible'}
                <c:if test="${sinDisponibilidad}"><span class="leyenda">Sin disponibilidad</span></c:if>
            </td>
            <td><a class="boton" href="${pageContext.request.contextPath}/admin/productos?editar=${p.codigo}">Editar</a></td>
        </tr>
    </c:forEach>
    </tbody>
</table>
<%@ include file="pie.jspf" %>
