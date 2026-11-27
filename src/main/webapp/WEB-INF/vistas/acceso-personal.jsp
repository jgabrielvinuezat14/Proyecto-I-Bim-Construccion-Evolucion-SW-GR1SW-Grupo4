<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Acceso de personal</h1>
<section class="tarjeta angosta">
    <h2>Ingreso del personal de cafetería</h2>
    <form method="post" action="${pageContext.request.contextPath}/personal/acceso" class="formulario">
        <label>Código de acceso
            <input type="text" name="codigo" value="<c:out value='${codigo}'/>">
        </label>
        <label>Contraseña
            <input type="password" name="contrasena">
        </label>
        <button type="submit">Ingresar</button>
    </form>
</section>
<%@ include file="pie.jspf" %>
