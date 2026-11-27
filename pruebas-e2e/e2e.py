# Prueba de extremo a extremo de la aplicación desplegada en Tomcat 9 + SQL Server.
import re, subprocess, sys, threading, http.cookiejar, urllib.parse, urllib.request
sys.stdout.reconfigure(encoding="utf-8")
BASE = "http://localhost:8080/cafeteria"
DB = "CafeteriaDB_Prueba"
resultados = []

class Navegador:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.url = None
    def get(self, ruta):
        r = self.op.open(BASE + ruta, timeout=30); self.url = r.geturl(); return r.read().decode("utf-8")
    def post(self, ruta, **datos):
        d = urllib.parse.urlencode(datos).encode()
        r = self.op.open(BASE + ruta, d, timeout=30); self.url = r.geturl(); return r.read().decode("utf-8")

def sql(q):
    if DB != "CafeteriaDB_Prueba":
        raise RuntimeError("Solo se permite la base de pruebas")

    proceso = subprocess.run(
        [
            "docker", "exec", "-i", "cafeteria-sqlserver",
            "sh", "-c",
            'SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" exec /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -W -h -1 -d "$1"',
            "sh", DB
        ],
        input="SET NOCOUNT ON; " + q + "\nGO\n",
        capture_output=True,
        text=True,
        encoding="utf-8",
        timeout=30,
        check=True
    )
    return proceso.stdout.strip()

def ok(nombre, condicion, detalle=""):
    resultados.append((nombre, bool(condicion)))
    print(("PASA  " if condicion else "FALLA ") + nombre + ("" if condicion else "  -> " + detalle[:300]))

def texto(html):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html))

# ---------------- HU-01 Menú ----------------
an = Navegador()
h = texto(an.get("/menu"))
ok("HU-01 E1 menú muestra productos con precio $3.00", "Sándwich" in h and "$3.00" in h, h)
ok("HU-01 E2 oculta sin unidades (Almuerzo) y no disponibles (Brownie)", "Almuerzo" not in h and "Brownie" not in h, h)

# ---------------- HU-10 Acceso de personal ----------------
pe = Navegador()
pe.get("/admin/productos")
ok("HU-10 E4 ruta administrativa sin sesión redirige a «Acceso de personal»", pe.url.endswith("/personal/acceso"), pe.url)
h = texto(pe.post("/personal/acceso", codigo="CAFE-2026", contrasena="mala"))
ok("HU-10 E2 credenciales incorrectas", "Código de acceso o contraseña incorrectos" in h, h)
h = texto(pe.post("/personal/acceso", codigo="", contrasena=""))
ok("HU-10 E3 campos vacíos", "Debe ingresar el código de acceso y la contraseña" in h, h)
h = texto(pe.post("/personal/acceso", codigo="CAFE-2026", contrasena="Personal#2026"))
ok("HU-10 E1 acceso válido abre «Administración del menú»", pe.url.endswith(("/admin/productos", "/admin/pedidos")) and "Administración del menú" in h and "Gestión de pedidos" in h, pe.url)
html_productos = pe.get("/admin/productos")
h = texto(html_productos)
ok("HU-06 productos sin disponibilidad en gris con leyenda", h.count("Sin disponibilidad") == 2 and html_productos.count('class="gris"') == 2, h)

# ---------------- HU-06 Administrar el menú ----------------
h = texto(pe.post("/admin/productos", accion="nuevo", codigo="P009", nombre="Té", precio="1.00", unidades="5", disponible="true"))
ok("HU-06 E1 registra producto válido", "Producto «Té» registrado" in h and sql("SELECT COUNT(*) FROM dbo.Producto WHERE codigo='P009'") == "1", h)
h = texto(pe.post("/admin/productos", accion="nuevo", codigo="P010", nombre="Malo", precio="-2.00", unidades="5", disponible="true"))
ok("HU-06 E8 rechaza precio negativo", "El precio ingresado no es válido" in h and sql("SELECT COUNT(*) FROM dbo.Producto WHERE codigo='P010'") == "0", h)
h = texto(pe.post("/admin/productos", accion="nuevo", codigo="P010", nombre="Malo", precio="1.00", unidades="2.5", disponible="true"))
ok("HU-06 E9 rechaza unidades decimales", "Las unidades deben ser un número entero mayor o igual a cero" in h, h)
pe.post("/admin/productos", accion="editar", codigo="P009", nombre="Té", precio="1.00", unidades="5", disponible="false")
ok("HU-06 E4 deshabilitar oculta del menú", "Té" not in texto(an.get("/menu")))
pe.post("/admin/productos", accion="editar", codigo="P009", nombre="Té", precio="1.00", unidades="5", disponible="true")
ok("HU-06 E5 habilitar vuelve a mostrar en el menú", "Té" in texto(an.get("/menu")))
pe.post("/admin/productos", accion="editar", codigo="P007", nombre="Almuerzo del día", precio="4.50", unidades="15", disponible="true")
ok("HU-06 E6 reponer unidades vuelve a mostrar el producto", "Almuerzo del día" in texto(an.get("/menu")))
pe.post("/admin/productos", accion="editar", codigo="P008", nombre="Brownie", precio="1.50", unidades="30", disponible="false")
ok("HU-01 reponer unidades no muestra un producto que sigue no disponible", "Brownie" not in texto(an.get("/menu")))

# ---------------- HU-09 Registro e inicio de sesión ----------------
cl = Navegador()
cl.get("/mi-pedido")
ok("HU-09 T09.3 «Mi pedido» sin sesión redirige a «Iniciar sesión»", cl.url.endswith("/login"), cl.url)
h = texto(cl.post("/registro", nombre="Ana", apellido="Pérez", cedula="1723456789", celular="0991234567",
                  correo="ana.perez@epn.edu.ec", contrasena="Segura#2026"))
ok("HU-09 E1 registro exitoso con código generado y redirección a login", "Cuenta registrada correctamente" in h and "C003" in h and cl.url.endswith("/login") and sql("SELECT COUNT(*) FROM dbo.Cliente WHERE codigo_cliente='C003'") == "1", h)
ok("RNF-04 la contraseña se guarda como hash", sql("SELECT LEFT(contrasena_hash,4) FROM dbo.Cliente WHERE codigo_cliente='C003'") == "$2a$")
h = texto(cl.post("/registro", nombre="Ana", apellido="Pérez", cedula="1723456789", celular="0991234567",
                  correo="ana.perez@epn.edu.ec", contrasena="Segura#2026"))
ok("HU-09 E2 rechaza correo o cédula duplicados", ("Ya existe una cuenta registrada" in h or "Ya existe un cliente registrado" in h) and sql("SELECT COUNT(*) FROM dbo.Cliente WHERE codigo_cliente='C003'") == "1", h)
h = texto(cl.post("/registro", nombre="", apellido="Pérez", cedula="1723456780", celular="0991234567",
                  correo="otra@epn.edu.ec", contrasena="Segura#2026"))
ok("HU-09 E3 rechaza campos vacíos", "Todos los campos son obligatorios" in h and sql("SELECT COUNT(*) FROM dbo.Cliente") == "3", h)
h = texto(cl.post("/login", correo="ana.perez@epn.edu.ec", contrasena="mala"))
ok("HU-09 E5 credenciales incorrectas", "Correo institucional o contraseña incorrectos" in h, h)
h = texto(cl.post("/login", correo="ana.perez@epn.edu.ec", contrasena="Segura#2026"))
ok("HU-09 E4 inicio de sesión exitoso", cl.url.endswith("/menu") and "Ana (C003)" in h, cl.url)
cl.get("/admin/pedidos")
ok("HU-10 E4 un cliente no entra a «Gestión de pedidos»", cl.url.endswith("/personal/acceso"), cl.url)

# ---------------- HU-02 / HU-03 Selección y total ----------------
h = texto(cl.post("/mi-pedido", accion="agregar", producto="P001", cantidad="0"))
ok("HU-02 E3 rechaza cantidad 0", "La cantidad debe ser un número entero positivo" in h, h)
h = texto(cl.post("/mi-pedido", accion="agregar", producto="P001", cantidad="1.5"))
ok("HU-02 E3 rechaza cantidad decimal", "La cantidad debe ser un número entero positivo" in h, h)
h = texto(cl.post("/mi-pedido", accion="revisar"))
ok("HU-02 E5 no revisa un pedido vacío", "El pedido debe incluir al menos un producto" in h, h)
h = texto(cl.post("/mi-pedido", accion="agregar", producto="P001", cantidad="25"))
ok("HU-02 E10 no agrega más unidades de las disponibles", "Solo hay 20 unidades disponibles de «Sándwich»" in h, h)
cl.post("/mi-pedido", accion="agregar", producto="P001", cantidad="2")
h = texto(cl.post("/mi-pedido", accion="revisar"))
ok("HU-03 E1 subtotal $6.00 y total $6.00", "Revisión del pedido" in h and "Total: $6.00" in h, h)
h = texto(cl.post("/mi-pedido", accion="agregar", producto="P002", cantidad="1"))
ok("HU-03 E2 total de dos productos $8.00", "Total: $8.00" in h, h)
h = texto(cl.post("/mi-pedido", accion="actualizar", codigo="P001", cantidad="3"))
ok("HU-03 E3 actualizar cantidad recalcula total $11.00", "Total: $11.00" in h and "$9.00" in h, h)
h = texto(cl.post("/mi-pedido", accion="actualizar", codigo="P001", cantidad="-1"))
ok("HU-03 E4 cantidad inválida conserva total $11.00", "La cantidad debe ser un número entero positivo" in h and "Total: $11.00" in h, h)
h = texto(cl.post("/mi-pedido", accion="quitar", codigo="P002"))
ok("HU-03 E5 quitar producto recalcula total $9.00", "Total: $9.00" in h and "Jugo" not in h.split("Productos seleccionados")[1], h)

# ---------------- HU-04 Confirmar ----------------
h = texto(cl.post("/mi-pedido", accion="confirmar"))
m = re.search(r"Código de retiro: ([A-Z0-9]{6})", h)
ok("HU-04 E1 confirma pedido con código de 6 caracteres y estado Pendiente", m and "Pendiente" in h and "Total: $9.00" in h, h)
codigo1 = m.group(1) if m else ""
ok("HU-04 E1 descuenta unidades (20 → 17)", sql("SELECT cantidad_disponible FROM dbo.Producto WHERE codigo='P001'") == "17")
ok("HU-04 E1 pedido guardado PENDIENTE con su cliente", sql(f"SELECT estado+'|'+codigo_cliente FROM dbo.Pedido WHERE codigo_retiro='{codigo1}'") == "PENDIENTE|C003")

# Cambio de precio entre revisión y confirmación (CA-04.4)
cl.post("/mi-pedido", accion="agregar", producto="P003", cantidad="2")
h = texto(cl.post("/mi-pedido", accion="revisar"))
ok("HU-04 E9 preparación: total revisado $7.00", "Total: $7.00" in h, h)
pe.post("/admin/productos", accion="editar", codigo="P003", nombre="Ensalada", precio="4.00", unidades="10", disponible="true")
h = texto(cl.post("/mi-pedido", accion="confirmar"))
ok("HU-04 E9 precio cambió: no confirma y muestra total actualizado $8.00", "cambió" in h and "Total: $8.00" in h and "Pedido confirmado" not in h, h)
h = texto(cl.post("/mi-pedido", accion="confirmar"))
m2 = re.search(r"Código de retiro: ([A-Z0-9]{6})", h)
ok("HU-04 E10 confirma con el total actualizado $8.00", m2 and "Total: $8.00" in h, h)
codigo2 = m2.group(1) if m2 else ""
ok("HU-04 E1 códigos distintos", codigo1 and codigo2 and codigo1 != codigo2)

# Producto que deja de estar disponible antes de confirmar (HU-04 E6)
cl.post("/mi-pedido", accion="agregar", producto="P004", cantidad="1")
pe.post("/admin/productos", accion="editar", codigo="P004", nombre="Café", precio="1.25", unidades="30", disponible="false")
h = texto(cl.post("/mi-pedido", accion="confirmar"))
ok("HU-04 E6 rechaza producto que dejó de estar disponible", "«Café» ya no está disponible" in h and "Pedido confirmado" not in h, h)
cl.post("/mi-pedido", accion="quitar", codigo="P004")
pe.post("/admin/productos", accion="editar", codigo="P004", nombre="Café", precio="1.25", unidades="30", disponible="true")

# ---------------- HU-05 Consultar ----------------
h = texto(cl.get("/consultar-pedido?codigo=" + codigo1.lower()))
ok("HU-05 E1 consulta su pedido y ve estado PENDIENTE", ("Detalle del pedido " + codigo1) in h and ("Estado: PENDIENTE" in h) and "PENDIENTE" in h, h)
h = texto(cl.get("/consultar-pedido?codigo=Z9Y8X7"))
ok("HU-05 E2 código inexistente", "No se encontró el pedido" in h, h)
ok("HU-09 «Mis pedidos» lista los dos pedidos del cliente", codigo1 in h and codigo2 in h, h)
otro = Navegador()
otro.post("/login", correo="docente.prueba@epn.edu.ec", contrasena="Prueba#2026")
h = texto(otro.get("/consultar-pedido?codigo=" + codigo1))
ok("HU-09 T09.3 otro cliente no ve un pedido ajeno", "No se encontró el pedido" in h, h)

# ---------------- HU-07 / HU-08 Gestión ----------------
h = texto(pe.get("/admin/pedidos?estado=PENDIENTE"))
ok("HU-07 E1 lista pendientes con código, fecha y total", codigo1 in h and "$9.00" in h, h)
h = texto(pe.get("/admin/pedidos?estado=LISTO_PARA_RETIRAR"))
ok("HU-07 E3 estado sin pedidos muestra mensaje", "No hay pedidos en ese estado" in h, h)
h = texto(pe.get(f"/admin/pedidos?estado=PENDIENTE&detalle={codigo1}"))
detalle = h.split("Detalle del pedido")[1].split("Entregar pedido")[0] if "Detalle del pedido" in h else ""
ok("HU-07 E2 / HU-08 E4 detalle muestra solo «Iniciar preparación»", "Sándwich" in detalle and "Iniciar preparación" in detalle and "Marcar como listo" not in detalle and "Registrar entrega" not in detalle, h)
h = texto(pe.post("/admin/pedidos", accion="entregar", codigoRetiro=codigo1))
ok("HU-08 E5 no entrega pedido que no está listo", "El pedido aún no está listo para retirar" in h, h)
idp = sql(f"SELECT id_pedido FROM dbo.Pedido WHERE codigo_retiro='{codigo1}'")
pe.post("/admin/pedidos", accion="avanzar", id=idp, codigo=codigo1, estado="PENDIENTE")
ok("HU-08 E1 inicia preparación", sql(f"SELECT estado FROM dbo.Pedido WHERE id_pedido={idp}") == "EN_PREPARACION")
pe.post("/admin/pedidos", accion="avanzar", id=idp, codigo=codigo1, estado="EN_PREPARACION")
ok("HU-08 E2 marca como listo", sql(f"SELECT estado FROM dbo.Pedido WHERE id_pedido={idp}") == "LISTO_PARA_RETIRAR")
h = texto(cl.get("/consultar-pedido?codigo=" + codigo1))
ok("HU-05 E3 el cliente ve el estado actualizado", "LISTO_PARA_RETIRAR" in h, h)
h = texto(pe.post("/admin/pedidos", accion="entregar", codigoRetiro="Z9Y8X7"))
ok("HU-08 E8 código incorrecto", "El código no corresponde a ningún pedido listo para retirar" in h, h)
h = texto(pe.post("/admin/pedidos", accion="entregar", codigoRetiro=codigo1.lower()))
ok("HU-08 E7 registra la entrega", f"Se registró la entrega del pedido «{codigo1}»" in h and sql(f"SELECT estado FROM dbo.Pedido WHERE id_pedido={idp}") == "ENTREGADO", h)
h = texto(pe.post("/admin/pedidos", accion="entregar", codigoRetiro=codigo1))
ok("HU-08 E6 no registra dos veces la entrega", "El pedido ya fue entregado" in h, h)

# ---------------- RNF-03 Concurrencia real ----------------
sql("UPDATE dbo.Producto SET cantidad_disponible = 2, disponible = 1 WHERE codigo = 'P005'")
a, b = Navegador(), Navegador()
a.post("/login", correo="cliente.prueba@epn.edu.ec", contrasena="Prueba#2026")
b.post("/login", correo="docente.prueba@epn.edu.ec", contrasena="Prueba#2026")
a.post("/mi-pedido", accion="agregar", producto="P005", cantidad="2")
b.post("/mi-pedido", accion="agregar", producto="P005", cantidad="1")
salida = threading.Barrier(2); respuestas = {}
def confirmar(nav, clave):
    salida.wait(); respuestas[clave] = texto(nav.post("/mi-pedido", accion="confirmar"))
hilos = [threading.Thread(target=confirmar, args=(a, "a")), threading.Thread(target=confirmar, args=(b, "b"))]
[t.start() for t in hilos]; [t.join() for t in hilos]
exitos = sum("Pedido confirmado" in r for r in respuestas.values())
restantes = int(sql("SELECT cantidad_disponible FROM dbo.Producto WHERE codigo='P005'"))
vendidas = int(sql("SELECT ISNULL(SUM(cantidad),0) FROM dbo.DetallePedido WHERE codigo_producto='P005'"))
ok("HU-04 E7 / RNF-03 solo una de dos confirmaciones simultáneas se registra", exitos == 1, str(respuestas))
ok("RNF-03 nunca se vende más de lo disponible (vendidas + restantes = 2)", restantes >= 0 and vendidas + restantes == 2, f"{vendidas} {restantes}")

# ---------------- Cierre de sesión ----------------
cl.post("/logout")
cl.get("/mi-pedido")
ok("HU-09 E7 cerrar sesión redirige a «Iniciar sesión» y bloquea «Mi pedido»", cl.url.endswith("/login"), cl.url)
pe.post("/personal/salir")
pe.get("/admin/pedidos")
ok("HU-10 E5 cerrar sesión del personal bloquea la gestión", pe.url.endswith("/personal/acceso"), pe.url)

open(sys.argv[1], "w", encoding="utf-8").write(codigo1 + "\n")
print(f"\nRESUMEN: {sum(r for _, r in resultados)} de {len(resultados)} comprobaciones pasaron")

sys.exit(0 if all(r for _, r in resultados) else 1)
