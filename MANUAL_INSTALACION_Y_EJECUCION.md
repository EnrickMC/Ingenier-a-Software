# Guía Rápida para Ejecutar el Sistema CARMIR

El sistema **CARMIR (Delicias de Postres)** incluye:
1. **Tienda en Línea para Clientes** (página web en Angular + servidor API en Node.js).
2. **Panel Administrativo de Escritorio** (aplicación de escritorio en JavaFX).
3. **Base de Datos (`CARMIR_DB`)** (en MySQL / MariaDB).

No es necesario escribir comandos ni instalar librerías a mano: todo se realiza mediante **dos archivos automáticos (`.bat`)**.

---

## Cómo ejecutar el proyecto (Solo 2 pasos)

### Paso 1: Hacer doble clic en `1_INSTALAR_PRIMERA_VEZ.bat` *(solo la primera vez)*

1. Asegúrese de tener **conexión a Internet**.
2. Haga **doble clic** en el archivo:
**`1_INSTALAR_PRIMERA_VEZ.bat`**

> **Avisos normales de Windows:**
> * Si aparece una ventana azul diciendo *"Windows protegió su PC"*, haga clic en **"Más información"** y luego en **"Ejecutar de todas formas"**.
> * Si falta instalar algún programa en su computadora y Windows abre un recuadro preguntando *"¿Quieres permitir que esta aplicación realice cambios en tu dispositivo?"*, haga clic en **"Sí"**.

#### ¿Qué hace automáticamente `1_INSTALAR_PRIMERA_VEZ.bat`?
* **Revisa e instala de Internet lo que haga falta** (usando el gestor oficial `winget` de Windows):
  * **Node.js (22 LTS o superior):** Si no lo tiene (o si tiene una versión antigua menor a 22), lo descarga e instala solo.
  * **Java JDK 25:** Si no lo tiene, descarga e instala **JDK 25** automáticamente. **No borra ni afecta otras versiones de Java** (como Java 8, 17 o 21) que ya tenga en su equipo.
  * **XAMPP / MySQL:**
    * Si ya tiene **XAMPP**, lo enciende solo y lo usa sin borrar sus demás bases de datos.
    * Si ya tiene **MySQL Server 8.0+ (Workbench)** con contraseña en `root`, lo detecta, le pide su contraseña de `root` en la pantalla y se conecta a su propio MySQL sin instalar XAMPP.
    * Si no tiene ni MySQL ni XAMPP, descarga e instala **XAMPP** automáticamente.
* **Crea y llena la base de datos `CARMIR_DB`** con todas las tablas, productos, recetas, insumos, usuarios y pedidos de prueba.
* **Descarga las librerías** de Angular, Express, Maven y JavaFX (no necesita instalar nada aparte ni abrir Visual Studio Code).
* Al finalizar mostrará: **`INSTALACION COMPLETADA CON EXITO`**.

---

### Paso 2: Hacer doble clic en `2_INICIAR_SISTEMA.bat` *(para abrir el sistema)*

Cada vez que desee abrir y revisar el sistema, haga **doble clic** en:
**`2_INICIAR_SISTEMA.bat`**

Automáticamente se abrirán:
1. **La Tienda en Línea** en su navegador web: http://localhost:4200
2. **El Panel Administrativo de Escritorio** en su propia ventana (tarda unos segundos en aparecer).

*(Nota: Se abrirán 3 ventanas negras de consola que mantienen encendidos los servidores. **No las cierre** mientras esté revisando el sistema; cuando termine, solo cierre esas ventanas).*

---

## Usuarios y datos de prueba para revisar el sistema

### 1. Panel Administrativo (Ventana de escritorio)

* **Cuenta de Administradora (acceso total a todo el sistema):**
  * **Usuario:** `admin`
  * **Contraseña:** `admin`

* **Cuentas de empleados por rol (haciendo clic abajo en «Acceso del Personal»):**

| Usuario | Contraseña | Permisos asignados |
|---|---|---|
| `maria` | `Maria2026` | Pedidos, agenda de producción, insumos y recetas |
| `jose` | `Jose2026` | Agenda de producción, insumos y recetas |
| `lucia` | `Lucia2026` | Pedidos, validar pagos y reportes financieros |
| `pedro` | `Pedro2026` | Cuenta **Bloqueada** (para probar cómo la Administradora `admin` la desbloquea) |

### 2. Tienda en Línea (http://localhost:4200)

* **Catálogo:** Incluye **7 postres visibles** listos para personalizar (sabores, tamaños, rellenos, exclusiones y precio calculado al instante).
* **Códigos postales con envío a domicilio para pruebas:** `37238`, `37000`, `37160`, `37360` y `37500`.
* **Reportes financieros (Módulo 6 del Panel):** Puede consultar **Agosto de 2026**, que ya cuenta con movimientos de prueba con resultados conocidos (Ingresos: `$1,300.00`, Gastos: `$850.00`, Utilidad: `$450.00`).

---

## Anexo: Enlaces manuales y comandos (solo por referencia)

En caso de que en algún equipo escolar esté deshabilitado el instalador automático de Windows (`winget`), los 3 programas base pueden descargarse directamente de:
* **Java JDK 25 (x64):** https://www.oracle.com/java/technologies/downloads/#java25-windows
* **Node.js LTS (x64):** https://nodejs.org/
* **XAMPP (x64):** https://www.apachefriends.org/es/index.html

Si se desea abrir el código fuente para inspección (opcional), puede abrirse la carpeta en **Visual Studio Code** o **IntelliJ IDEA**.
