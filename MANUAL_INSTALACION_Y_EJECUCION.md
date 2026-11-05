# Guía Rápida para Ejecutar el Sistema CARMIR

El proyecto **Sistema CARMIR (Delicias de Postres)** está organizado de la siguiente manera:

* `1_INSTALAR_PRIMERA_VEZ.bat` — Configurador e instalador automático inicial.
* `2_INICIAR_SISTEMA.bat` — Lanzador automático de todo el sistema.
* `3_RESTAURAR_BASE_DE_DATOS.bat` — *(Opcional)* Restaura la base de datos a su estado limpio de ejemplo (con confirmación de seguridad).
* `Software_CARMIR/` — Carpeta principal del código fuente:
  1. `DBCarmir/` — Scripts de Base de Datos (`CARMIR_DB`) en MySQL / MariaDB.
  2. `PaginaWebCARMIR/` — Tienda en Línea para Clientes (Angular + API REST `ApiCARMIR` en Node.js).
  3. `PanelAdministrativoCARMIR/` — Panel Administrativo de Escritorio (JavaFX + Maven).

No es necesario escribir comandos en consola ni configurar credenciales a mano: todo se realiza mediante los archivos automáticos (`.bat`).

---

## Cómo ejecutar el proyecto (Solo 2 pasos)

> **Importante si descargó el proyecto desde Google Drive:**
> Si Google Drive descargó la carpeta como un archivo comprimido `.zip`, primero haga **clic derecho sobre el `.zip` -> «Extraer todo...»** y abra la carpeta ya descomprimida antes de ejecutar los archivos `.bat`.

### Paso 1: Hacer doble clic en `1_INSTALAR_PRIMERA_VEZ.bat` *(solo la primera vez)*

1. Asegúrese de tener **conexión a Internet**.
2. Haga **doble clic** en el archivo:
**`1_INSTALAR_PRIMERA_VEZ.bat`**

> **Avisos normales de Windows:**
> * Si aparece una ventana azul diciendo *"Windows protegió su PC"*, haga clic en **"Más información"** y luego en **"Ejecutar de todas formas"**.
> * Si falta instalar algún programa en su computadora y Windows abre un recuadro preguntando *"¿Quieres permitir que esta aplicación realice cambios en tu dispositivo?"*, haga clic en **"Sí"**.

#### ¿Qué hace automáticamente `1_INSTALAR_PRIMERA_VEZ.bat`?
* **Revisa e instala de Internet lo que haga falta** (usando el gestor oficial `winget` de Windows):
  * **Node.js (22 LTS o superior):** Si no lo tiene (o si tiene una versión menor a 22), lo descarga e instala solo.
  * **Java JDK (25 o superior):** Si no lo tiene, descarga e instala **JDK 25** automáticamente sin borrar ni afectar otras versiones de Java (como Java 8, 17 o 21) que ya tenga en su equipo.
  * **MySQL / XAMPP:**
    * Si ya tiene **MySQL Server 8.0+ (Workbench)** o **MariaDB**, lo detecta, enciende su servicio si estaba detenido y configura sus credenciales locales automáticamente (o le pide su contraseña de `root` si usa una personalizada) sin instalar XAMPP.
    * Si ya tiene **XAMPP**, lo enciende solo y lo usa sin afectar sus demás bases de datos.
    * Si no tiene ni MySQL ni XAMPP, descarga e instala **XAMPP** automáticamente.
* **Configura la base de datos `CARMIR_DB` sin conflictos:**
  * En equipos nuevos, crea `CARMIR_DB` completa y la llena con las 7 categorías, opciones 3D, reglas de exclusión, productos, recetas, insumos, usuarios y pedidos de demostración.
  * Si en el equipo ya existía una base `CARMIR_DB` con datos previos, permite **conservar todos los datos actuales** aplicando únicamente las actualizaciones nuevas de estructura (`ACTUALIZAR_SIN_BORRAR.sql`), o reinstalar desde cero si así se elige.
* **Descarga y compila las librerías** de Express, Angular, Maven y JavaFX.
* Al finalizar mostrará: **`INSTALACION Y CONFIGURACION COMPLETADAS CON EXITO`**.

---

### Paso 2: Hacer doble clic en `2_INICIAR_SISTEMA.bat` *(para abrir el sistema)*

Cada vez que desee abrir y revisar el sistema, haga **doble clic** en:
**`2_INICIAR_SISTEMA.bat`**

Automáticamente verificará que MySQL esté encendido y abrirá:
1. **La Tienda en Línea** en su navegador web: http://localhost:4200
2. **El Panel Administrativo de Escritorio** en su propia ventana (tarda unos segundos en aparecer).

*(Nota: Todos los servidores se ejecutan juntos **en esa misma ventana de consola**, sin abrir terminales adicionales. Manténgala abierta mientras revisa el sistema y, al terminar, simplemente presione **`Enter`** en esa consola para detener y cerrar todo).*

---

### Opcional: Restaurar la base de datos de fábrica (`3_RESTAURAR_BASE_DE_DATOS.bat`)

Si después de realizar pruebas desea **borrar los cambios hechos y volver a dejar la base de datos limpia** exactamente con los datos originales de ejemplo:
1. Haga doble clic en **`3_RESTAURAR_BASE_DE_DATOS.bat`**.
2. Por seguridad, el programa mostrará una advertencia y pedirá escribir **`SI`** y presionar Enter para confirmar (si solo presiona Enter o escribe cualquier otra cosa, se cancela sin borrar nada).

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

* **Catálogo completo:** Incluye **7 categorías** (`Pastel`, `Gelatina`, `Cupcakes`, `Cheesecakes`, `Pays`, `Brownies` y `Donas`), **43 características**, **199 opciones** compatibles con el visor 3D (pisos y capas individuales, formas, sabores, rellenos, coberturas, frostings, colores, bases y decoraciones), **47 reglas de exclusión** para impedir combinaciones incompatibles y **20 productos visibles** listos para personalizar con precio calculado al instante.
* **Códigos postales con envío a domicilio para pruebas:** `37238`, `37000`, `37160`, `37360` y `37500`.
* **Reportes financieros (Módulo 6 del Panel):** Puede consultar **Agosto de 2026**, que ya cuenta con movimientos de prueba con resultados conocidos (Ingresos: `$1,300.00`, Gastos: `$850.00`, Utilidad: `$450.00`).

---

## Anexo: Enlaces manuales (solo por referencia)

En caso de que en algún equipo institucional esté deshabilitado el instalador automático de Windows (`winget`), los 3 programas base pueden descargarse directamente de:
* **Java JDK 25 (x64):** https://www.oracle.com/java/technologies/downloads/#java25-windows
* **Node.js LTS (x64):** https://nodejs.org/
* **XAMPP (x64):** https://www.apachefriends.org/es/index.html
