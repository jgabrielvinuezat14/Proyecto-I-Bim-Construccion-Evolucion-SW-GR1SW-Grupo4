<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Iniciar sesión</h1>
<section class="tarjeta angosta">
    <h2>Acceso de clientes</h2>
    <form method="post" action="${pageContext.request.contextPath}/login" class="formulario">
        <label>Correo institucional
            <input type="text" name="correo" maxlength="120" value="<c:out value='${correo}'/>">
        </label>
        <label>Contraseña
            <input type="password" name="contrasena" maxlength="72">
        </label>
        <button type="submit">Iniciar sesión</button>
    </form>
    <p>¿No tiene una cuenta? <a href="${pageContext.request.contextPath}/registro">Registrarse</a></p>
</section>
<%@ include file="pie.jspf" %>
