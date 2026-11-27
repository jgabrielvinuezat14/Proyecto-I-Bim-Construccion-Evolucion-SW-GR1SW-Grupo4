<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="encabezado.jspf" %>
<h1>Registro de cliente</h1>
<section class="tarjeta angosta">
    <h2>Crear cuenta</h2>
    <form method="post" action="${pageContext.request.contextPath}/registro" class="formulario">
        <label>Nombre
            <input type="text" name="nombre" maxlength="60" value="<c:out value='${clienteRegistro.nombre}'/>">
        </label>
        <label>Apellido
            <input type="text" name="apellido" maxlength="60" value="<c:out value='${clienteRegistro.apellido}'/>">
        </label>
        <label>Cédula
            <input type="text" name="cedula" maxlength="10" value="<c:out value='${clienteRegistro.cedula}'/>">
        </label>
        <label>Celular
            <input type="text" name="celular" maxlength="10" value="<c:out value='${clienteRegistro.celular}'/>">
        </label>
        <label>Correo institucional
            <input type="text" name="correo" maxlength="120" value="<c:out value='${clienteRegistro.correoInstitucional}'/>">
        </label>
        <label>Contraseña
            <input type="password" name="contrasena" maxlength="72">
        </label>
        <button type="submit">Registrarse</button>
    </form>
    <p>¿Ya tiene una cuenta? <a href="${pageContext.request.contextPath}/login">Iniciar sesión</a></p>
</section>
<%@ include file="pie.jspf" %>
