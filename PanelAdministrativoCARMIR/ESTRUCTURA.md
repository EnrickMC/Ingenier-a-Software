# Panel Administrativo CARMIR — Estructura

JavaFX 21 + Maven, patrón **MVC** con capa DAO para MySQL (`CARMIR_DB`).
El código general del panel está en `com.pac` y cada módulo tiene su propia carpeta en `com.pac.modulos`.

```
src/main/java/com/pac/
├── CarmirApp.java / Launcher.java     Arranque (abre la pantalla de bloqueo)
│
│   ── Partes generales (las usan todos los módulos) ──
├── controller/   PanelPrincipal, Dashboard, SeccionEnConstruccion
├── dao/          DAOFactory (crea los DAO de todos los módulos), DAOException
│   └── conexion/ ConexionBD            ← Singleton (JDBC, lee config/db.properties)
├── session/      SesionUsuario         ← Singleton + Observer (sujeto); MonitorInactividad (Singleton)
│                 SesionObserver        ← Observer, TipoAcceso
├── navigation/   Navegador             ← Singleton + Observer; revisa permisos antes de abrir un apartado
│                 Vista, SeccionPanel (apartados del menú ↔ módulos 3‑7),
│                 NavegacionObserver, ControladorSeccion
├── view/menu/    ElementoMenu, ElementoMenuBase, DecoradorElementoMenu,
│                 DecoradorPermiso, DecoradorInsignia ← Decorator; FabricaMenu
├── util/         ConfiguracionApp (Singleton), HashContrasena, Formatos, Alertas
│
│   ── Módulos ──
└── modulos/
    ├── usuarios/     Módulo 7 · Gestión de Usuarios  (TERMINADO)
    │   ├── model/       Usuario, Permiso, EstadoUsuario, Permisos (nombres de permisos)
    │   ├── dao/         UsuarioDAO, PermisoDAO
    │   │   └── mysql/   UsuarioDAOMySQL, PermisoDAOMySQL
    │   ├── service/     AutenticacionService (login), UsuarioService (CRUD),
    │   │                ControlAcceso (permisos por apartado), ResultadoOperacion,
    │   │                ResultadoAutenticacion, ResultadoAcceso
    │   └── controller/  Bloqueo, LoginPersonal, Usuarios, FormularioUsuario, FilaUsuario
    ├── catalogo/     Módulo 3 · Catálogo de Personalización  (TERMINADO)
    │   ├── model/       Categoria, Caracteristica, Opcion, Exclusion, Producto, OpcionElegida, EstadoRegistro
    │   ├── dao/         CategoriaDAO, CaracteristicaDAO, OpcionDAO, ExclusionDAO, ProductoDAO
    │   │   └── mysql/   sus implementaciones + ApoyoMySQL (consultas comunes)
    │   ├── service/     CategoriaService, CaracteristicaService, OpcionService, ExclusionService,
    │   │                ProductoService, ConfiguracionPostre (reglas y precio), PermisosCatalogo,
    │   │                ValidacionCatalogo, ResultadoCatalogo
    │   └── controller/  Catalogo (pestañas), Categorias, Caracteristicas, Opciones, Exclusiones,
    │                    Productos, Formulario*, Fila*, SelectorOpcion, ApoyoVista
    ├── pedidos/      Módulo 4 · Gestor de Pedidos y Producción  (TERMINADO)
    │   ├── model/       Pedido, LineaPedido, Cliente, Direccion, Pago, Cancelacion, BajaPedido,
    │   │                Modificacion, CambioEstado, Notificacion, FiltroPedidos, AgendaPeriodo,
    │   │                EstadoPedido, EstadoPago, MetodoPago, ModalidadPago, ModalidadEntrega
    │   ├── dao/         PedidoDAO, ClienteDAO, PagoDAO, BitacoraPedidoDAO, NotificacionDAO
    │   │   └── mysql/   sus implementaciones JDBC + ApoyoPedidosMySQL
    │   ├── service/     PedidoService (lista, detalle, aprobar, cancelar, baja), EstadoPedidoService
    │   │                (avance de estados + RF-45), PagoService, ModificacionPedidoService, AgendaService,
    │   │                MonitorPedidos (Singleton + Observer), PedidosObserver, PermisosPedidos,
    │   │                ApoyoTransaccion, ResultadoPedido
    │   ├── notificacion/ NotificacionService (Singleton), CanalEnvio ← Strategy (CanalSimulado,
    │   │                CanalWhatsApp, CanalCorreo), PlantillasMensaje, EnvioNotificaciones
    │   └── controller/  Pedidos, DetallePedido, Agenda, Pagos, Formulario* (aprobación, motivo, pago,
    │                    modificación, personalización), EditorLineaPedido, TicketPedido, Fila*, Celda*
    ├── inventario/   Módulo 5 · Inventario y Recetas  (TERMINADO)
    │   ├── model/       Insumo, Lote, Movimiento, AlertaInventario, Receta, IngredienteReceta,
    │   │                OpcionReceta, OpcionPedido, NecesidadInsumo, UnidadMedida, TipoMovimiento, TipoAlerta
    │   ├── dao/         InsumoDAO, LoteDAO, MovimientoDAO, AlertaDAO, RecetaDAO, ConsumoPedidoDAO
    │   │   └── mysql/   sus implementaciones JDBC
    │   ├── service/     InsumoService, InventarioService (compras, mermas, PEPS), AlertaService,
    │   │                MonitorAlertas (Singleton + Observer), RecetaService, DescuentoInsumosService (RF-45),
    │   │                PermisosInventario, ResultadoInventario, ResultadoDescuento, AlertasObserver
    │   └── controller/  Inventario (pestañas), Insumos, Movimientos, Alertas, Recetas,
    │                    FormularioInsumo/Entrada/Merma/Receta, Fila*, Renglon*, EstilosInventario
    ├── reportes/     Módulo 6 · Reportes Financieros  (TERMINADO)
    │   ├── model/       ReporteIngresosGastos, RenglonDia, MovimientoRevisar, ReporteVentas, RenglonVenta
    │   ├── dao/         ReporteDAO
    │   │   └── mysql/   ReporteDAOMySQL (todas las consultas del módulo)
    │   ├── service/     ReporteService (periodo, avisos, gráfica), PermisosReportes, RevisionPeriodo
    │   ├── exportacion/ ExportadorReportes, LibroExcel, HojaExcel, CeldaExcel (.xlsx sin librerías externas)
    │   └── controller/  Reportes, IngresosGastos, MasVendidos, PestanaReporte, ConvertidorFecha, Fila*
    └── configuracion/ Configuración del sistema  (TERMINADO)
        ├── model/       Parametro, ClavesConfiguracion, CambioConfiguracion, CodigoPostalCobertura
        ├── dao/         ConfiguracionDAO, CoberturaDAO
        │   └── mysql/   sus implementaciones JDBC
        ├── service/     ConfiguracionService (validación + historial), CoberturaService,
        │                RespaldoService (mysqldump / mysql), PermisosConfiguracion, ResultadoConfiguracion
        └── controller/  Configuracion (pestañas), Parametros, Cobertura, Historial, Respaldos,
                         AccesoConfiguracion, Fila*

src/main/resources/com/pac/
├── view/   (V) panel-principal.fxml, dashboard.fxml, seccion-en-construccion.fxml
│   ├── usuarios/  bloqueo.fxml, login-personal.fxml, usuarios.fxml, usuario-formulario.fxml
│   ├── catalogo/  catalogo.fxml (5 pestañas), categorias, caracteristicas, opciones, exclusiones,
│   │              productos.fxml y formulario-*.fxml
│   ├── pedidos/   pedidos.fxml, detalle-pedido.fxml, agenda.fxml, pagos.fxml y formulario-*.fxml
│   ├── inventario/ inventario.fxml (3 pestañas: insumos, movimientos, alertas), recetas.fxml
│   │              y formulario-insumo/entrada/merma/receta.fxml
│   ├── reportes/  reportes.fxml (2 pestañas), ingresos-gastos.fxml, mas-vendidos.fxml
│   └── configuracion/ configuracion.fxml (4 pestañas), parametros, cobertura, historial, respaldos.fxml
├── css/    carmir-base.css (paleta), login.css, panel.css, usuarios.css, catalogo.css, inventario.css,
│           pedidos.css, reportes.css (también lo usa Configuración)
└── config/ app.properties (valores de respaldo si falta la tabla Configuracion), db.properties (conexión MySQL),
            notificaciones.properties (WhatsApp y correo; por defecto simuladas)
```

## Flujo
1. **Pantalla de bloqueo**: solo la Administradora. Su usuario y contraseña están fijos en
   `AdministradoraSistema.java` (por defecto `admin` / `admin`); no está en la base de datos,
   no aparece en la lista de usuarios y no se puede modificar ni dar de baja.
2. Enlace a **Acceso del Personal**: solo usuarios de la tabla Usuario.
3. **Panel principal** con menú lateral; cada apartado carga su FXML dentro del panel.
   Antes de abrirlo, `ControlAcceso` vuelve a leer los permisos de la BD (RNF-15).

## Base de datos
Solo hacen falta **dos scripts**, en este orden:
1. `DBCarmir/CARMIR_BaseDeDatos.sql` (la base original, sin cambios).
2. `DBCarmir/migraciones/MIGRACION_COMPLETA.sql`: junta en un solo archivo todos los cambios que necesita el panel.
   Se puede ejecutar más de una vez sin duplicar nada (revisa cada columna, llave, índice y tabla antes de crearla).
   - Parte 1 (antes V2): `Postre.EsPlantilla`.
   - Parte 2 (antes V3, Módulo 5): `MovimientoInventario.IdUsuario` acepta NULL y `vw_CostoReceta` solo cuenta recetas activas.
   - Parte 3 (antes V4, Módulo 4): `HistorialEstado.IdUsuario` acepta NULL, `Cancelacion.IdUsuario`, tablas
     `BajaPedido` y `ModificacionPedido`, y `Notificacion` con estado 'Pendiente' y `MotivoFallo`.
   - Parte 4 (nuevo): tablas `Configuracion`, `CambioConfiguracion` y `CodigoPostalCobertura`, los 13 parámetros
     de fábrica y tres índices para que los reportes sean rápidos.
   Los archivos `PROPUESTA_V2/V3/V4` quedan solo como historial: **no** hay que ejecutarlos.

Semillas de prueba (opcionales), después de los dos scripts y en este orden:
`semilla_modulo7_usuarios.sql` (usuario `maria` / `Maria2026`) → `semilla_modulo3_catalogo.sql` →
`semilla_modulo5_inventario.sql` → `semilla_modulo4_pedidos.sql` → `semilla_modulo6_reportes.sql`
(movimientos de agosto de 2026 con resultados conocidos para revisar los reportes).

Por último, revisar usuario y contraseña de MySQL en `config/db.properties`.
Si falta la migración, el panel sigue abriendo: avisa en cada apartado que necesita las tablas nuevas.

## Modo demostración
`app.modoDemo=true` → cualquier contraseña entra y no se consulta MySQL.
`app.modoDemo=false` → se valida contra la tabla Usuario (la contraseña se compara con `HashContrasena`, PBKDF2).
El apartado "Usuarios y Permisos" siempre usa MySQL.

## Módulo 7 · Gestión de Usuarios (qué hace)
| Requerimiento | Dónde |
|---|---|
| RNF-11 Registro de usuarios | `FormularioUsuarioController` + `UsuarioService.registrar` (ofrece reactivar cuentas dadas de baja) |
| RNF-12 Baja de usuarios | `UsuariosController.onDarDeBaja` (confirmación doble) + `UsuarioService.darDeBaja` (baja lógica) |
| RNF-13 Visualización | `UsuariosController` (búsqueda, filtro por estado, detalle, mensajes de lista vacía) |
| RNF-14 Modificación | `FormularioUsuarioController` + `UsuarioService.modificar` (restablecer contraseña, no quitarse el propio permiso) |
| RNF-15 Control de acceso | `AutenticacionService` (bloqueo tras los intentos de Configuración → Parámetros) + `ControlAcceso` + `DecoradorPermiso` |
| RNF-16 Sesión única | **Pendiente**: necesita la tabla `SesionUsuario` (migración nueva en `DBCarmir/migraciones`) |

## Módulo 3 · Catálogo de Personalización (qué hace)
Apartado **Catálogo de Postres** con 5 pestañas.

| Pestaña | Qué se hace | Dónde |
|---|---|---|
| Categorías | alta, búsqueda, modificación y baja (Activa / Inactiva) | `CategoriasController`, `CategoriaService` |
| Características | reglas por categoría: obligatoria, selección múltiple, presupuesto de puntos y orden (↑ ↓) | `CaracteristicasController`, `CaracteristicaService` |
| Opciones | multiplicadores de ingredientes, costo y presupuesto, puntos, si se escala, imagen | `OpcionesController`, `OpcionService` |
| Exclusiones | pares de opciones que no se pueden combinar (con motivo) | `ExclusionesController`, `ExclusionService` |
| Productos | postres con nombre: se arman eligiendo opciones; el precio se calcula en vivo | `ProductosController`, `ProductoService`, `ConfiguracionPostre` |

Reglas importantes:
- **Permisos**: entrar pide "Gestionar catálogo". Categorías, características, opciones y exclusiones solo las edita la Administradora; el personal las consulta. Productos: Administradora o personal con "Gestionar productos".
- **Bajas**: si el registro tiene historial (productos, recetas o pedidos) se desactiva; si no, se elimina. No se da de baja nada que esté en un pedido vigente (estado distinto de Entregado y sin cancelación).
- **Precio**: igual que la vista `vw_CostoPrecioPostre` (costo de receta × cantidad × factor de tamaño si la opción se escala, por el multiplicador de costo). Sin receta (Módulo 5) se muestra "Sin receta".
- **Clave de configuración**: `IdOpcion:Cantidad` separados por `|` y ordenados por id. Dos productos no pueden tener la misma.
- El SKU se muestra como `PST-0001` a partir de `IdPostre` (la tabla no tiene columna SKU).

## Módulo 5 · Inventario y Recetas (qué hace)
Apartados **Inventario de Insumos** (3 pestañas) y **Recetas**.

| Requerimiento | Dónde |
|---|---|
| RF-33 a RF-36 Insumos (ver, registrar, modificar, baja) | `InsumosController`, `FormularioInsumoController`, `InsumoService` |
| RF-37 Entrada (compra) | `FormularioEntradaController`, `InventarioService.registrarEntrada` (cada compra es un lote) |
| RF-38 Salida (merma) | `FormularioMermaController`, `InventarioService.registrarMerma` (PEPS o un lote elegido) |
| RF-39 / RF-40 Alertas | `AlertaService.revisar`, `MonitorAlertas`, `AlertasController`; contador en el menú y en el dashboard |
| RF-41 a RF-44 Recetas | `RecetasController`, `FormularioRecetaController`, `RecetaService` |
| RF-45 Descuento automático | `DescuentoInsumosService` (lo llama `EstadoPedidoService` del Módulo 4) |

Reglas importantes:
- **Permisos**: Inventario pide "Gestionar insumos" y Recetas pide "Gestionar recetas". Compras, mermas y alertas: cualquiera con "Gestionar insumos". Alta, cambios y bajas de insumos y recetas: solo la Administradora; el personal consulta.
- **Existencia** = suma de `ExistenciaLote`. Toda salida usa **PEPS**: primero el lote que caduca antes (los que no caducan al final). Nunca queda negativa.
- **Alertas**: se revisan al iniciar sesión, al abrir el inventario, después de cada compra o merma y cada hora. No se duplican; se cierran solas cuando el problema se resuelve. Los días de aviso se cambian en Configuración → Parámetros.
- **Receta**: una por opción. Su costo = Σ cantidad × costo del insumo; **precio base de la opción** = costo × multiplicador de costo. El catálogo ya lo muestra (columna "Precio base" y botón "Ver / Registrar receta" en Opciones).
- "Requiere revisión" se calcula: una receta la requiere si usa algún insumo dado de baja (no hace falta columna nueva).
- **Bajas**: insumo o receta con historial se inhabilita; sin historial se elimina. Una receta no se da de baja si hay pedidos por preparar con esa opción.
- **RF-45**: `descontarPorPedido(folio, conexion, continuarSinReceta)` va dentro de la transacción del cambio "Pagado" → "En preparacion". No descuenta dos veces, avisa de opciones sin receta y si falta algo no descuenta nada.

## Módulo 4 · Gestor de Pedidos y Producción (qué hace)
Apartados **Pedidos**, **Agenda de Producción** y **Validación de Pagos**. El dashboard muestra los pedidos de hoy y los últimos 10.

| Requerimiento | Dónde |
|---|---|
| RF-21 / RF-22 Lista, filtros y pedidos nuevos | `PedidosController`, `PedidoService.buscar` ("#12" busca solo por folio), `MonitorPedidos` |
| RF-23 / RF-24 Detalle, revisión y aprobación | `DetallePedidoController`, `FormularioAprobacionController`, `PedidoService.aprobar` (capacidad diaria) |
| RF-25 Cancelación | `FormularioMotivoController`, `PedidoService.cancelar` (motivo y reembolso) |
| RF-26 / RF-27 Estados | `EstadoPedidoService.avanzar` (solo el estado siguiente) |
| RF-28 / RF-29 Pagos | `PagosController`, `FormularioPagoController`, `PagoService` (validar, rechazar, registrar) |
| RF-30 / RF-31 Agenda | `AgendaController`, `AgendaService` (día, semana, mes o rango; insumos estimados) |
| RF-32 Notificaciones | `NotificacionService` (WhatsApp/correo; si falla queda "Pendiente" y se reintenta) |
| RF-45 Descuento de insumos | al pasar "Pagado" → "En preparación", en la misma transacción |
| RF-49 Sin pago no se prepara | `EstadoPedidoService.revisarAvance` |
| Modificación y baja | `FormularioModificacionController`, `ModificacionPedidoService`, `PedidoService.darDeBaja` |

Reglas importantes:
- **Permisos**: Pedidos pide "Gestionar pedidos", Agenda "Ver agenda de producción" y Pagos "Validar pagos". El personal ve solo pedidos activos; modificar, dar de baja y el historial completo son de la Administradora.
- **Estados**: En proceso de aceptación → Aprobado → Pagado → En preparación → Pedido terminado → Entregado. "Pagado" llega solo al validar el anticipo; "Entregado" exige saldo $0.00.
- **Dos usuarios a la vez**: antes de guardar se bloquea el pedido y se compara con el que se abrió; si alguien lo cambió, se avisa y se recarga.
- **Notificaciones**: por defecto se simulan (se imprimen). Para enviarlas de verdad, llenar `config/notificaciones.properties`; es mejor poner el token y la contraseña en las variables de entorno `CARMIR_WHATSAPP_TOKEN` y `CARMIR_SMTP_CONTRASENA` para no subirlos a GitHub.

## Módulo 6 · Reportes Financieros (qué hace)
Apartado **Reportes Financieros** con 2 pestañas que comparten el periodo (por defecto el mes en curso).

| Requerimiento | Dónde |
|---|---|
| RF-46 Ingresos y gastos | `IngresosGastosController`, `ReporteService.generarIngresosGastos`, `ReporteDAOMySQL` |
| RF-47 Más vendidos | `MasVendidosController`, `ReporteService.generarVentas` (filtro por categoría, orden por unidades o utilidad) |
| RF-48 Exportar a Excel | `ReportesController.onExportar`, `ExportadorReportes` (.xlsx o .csv con el mismo contenido de la pantalla) |
| Dashboard | tarjeta "Ingresos del mes" (`ReporteService.ingresosDelMes`) |

Reglas importantes:
- **Permiso**: "Ver datos financieros". Se revisa otra vez al generar y al exportar.
- **Ingresos** = pagos validados (por fecha de pago) clasificados en anticipo, liquidación o pago total, menos los
  reembolsos ya hechos (por fecha de cancelación). Los pagos de pedidos cancelados con reembolso pendiente se muestran
  aparte y no se suman.
- **Gastos** = compras + mermas del inventario. El consumo de producción (RF-45) se muestra como dato informativo
  porque el insumo ya se pagó en la compra.
- **Movimientos por revisar**: pagos de pedidos dados de baja, compras con cantidad o costo en cero y mermas con
  signo incorrecto. No se suman y se listan para corregirlos.
- **Ventas**: pedidos Pagado o más avanzados, sin cancelación ni baja, por fecha de solicitud. Los personalizados se
  agrupan como "Personalizado · categoría". La utilidad usa el costo de receta guardado en cada renglón del pedido.
- **Periodo**: fechas dd/mm/aaaa; fin anterior al inicio = "Rango de fechas inválido"; un fin futuro se ajusta a hoy.
- La gráfica agrupa por semana si el periodo es de hasta 92 días; si no, por mes.

## Configuración (qué hace)
Apartado **Configuración** (permiso "Configurar sistema") con 4 pestañas.

| Pestaña | Qué se hace |
|---|---|
| Parámetros | nombre del negocio, capacidad diaria, días para preparar, agenda, horario, días laborables, anticipo, datos bancarios, aviso de caducidad, intentos de inicio de sesión y minutos de inactividad |
| Cobertura de envío | códigos postales con su costo de envío (alta, cambio de costo y baja) |
| Historial de cambios | quién cambió qué valor y cuándo (la Administradora aparece como tal) |
| Respaldos y sistema | datos de la conexión, modo de las notificaciones, generar respaldo (mysqldump) y restaurarlo (solo la Administradora, con doble confirmación) |

Reglas importantes:
- Los parámetros se leen de la tabla `Configuracion` al abrir el panel y al guardar; si la tabla no existe se usan los
  de `app.properties` y la pestaña queda solo para consulta.
- Al guardar se validan todos los valores (rangos, formato HH:mm, cierre después de apertura) y solo se guardan los que
  cambiaron, cada uno con su registro en `CambioConfiguracion`, en una sola transacción.
- **Minutos de inactividad**: si nadie mueve el ratón ni presiona una tecla en ese tiempo, la sesión se cierra (0 = nunca).
- **Respaldos**: el panel busca `mysqldump` y `mysql` en el PATH, en `C:\Program Files\MySQL\...\bin` y en XAMPP. Si no
  los encuentra, se elige la carpeta en pantalla o se escribe en `app.properties` (`respaldos.carpetaMysql`).
- Las cuentas de WhatsApp y correo siguen en `notificaciones.properties` (son secretos; no van en la base de datos).

## Agregar un módulo nuevo
1. Crear sus clases en `modulos/<modulo>/` con las carpetas `model`, `dao`, `dao/mysql`, `service`, `controller`.
2. Crear sus vistas en `resources/com/pac/view/<modulo>/`.
3. Agregar sus DAO en `DAOFactory`.
4. En `SeccionPanel`, poner la ruta del FXML (por ejemplo `"inventario/inventario.fxml"`) en lugar de `null`.
5. En `module-info.java`, agregar `opens ...controller to javafx.fxml, javafx.base;` y los `exports`.

## Reglas de estilo del código
- Código legible: solo `if` / `else` (sin el operador `? :`).
- Sin flechas `->` salvo donde JavaFX las necesita (fábrica de renglones de una tabla, Timeline).
- Sin tipos poco comunes (`Optional`, `record`, `var`, streams); se usan clases, listas y `null`.
- Cada clase y método lleva un comentario corto en español.
- El código se reparte en paquetes y cada módulo en su carpeta dentro de `modulos/`.
