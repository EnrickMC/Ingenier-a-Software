# Estructura de PaginaWebCARMIR

Organizada por módulos (`features/`): cada módulo tiene su propia carpeta.
Módulo 1 · Catálogo Público y Módulo 2 · Portal de Pedidos.

```
PaginaWebCARMIR/
├── ApiCARMIR/                     API REST (Node + Express). Única parte que habla con MySQL
│   ├── .env.example               Variables a llenar (copiar como .env; el .env NO se sube a Git)
│   ├── sql/crear_usuario_lectura.sql   Usuario MySQL de solo lectura (carmir_web): catálogo y consultas
│   ├── sql/crear_usuario_pedidos.sql   Usuario MySQL para GUARDAR pedidos (carmir_pedidos, solo INSERT)
│   ├── src/
│   │   ├── servidor.js            Arranque y apagado ordenado
│   │   ├── app.js                 Middlewares y rutas
│   │   ├── config/                configuracion.js (lee .env) · baseDatos.js (pool de lectura y de escritura)
│   │   ├── middlewares/           seguridad.js (helmet, CORS, límites por ruta) · manejoErrores.js
│   │   ├── utilidades/            ErrorApi.js · validacion.js
│   │   └── modulos/
│   │       ├── salud/             GET /api/salud
│   │       ├── catalogo/          rutas → controlador → servicio → repositorio (SQL) + reglasConfiguracion
│   │       └── pedidos/           MÓDULO 2: rutas → controlador → validacion → servicio → repositorio
│   │                              + reglasEntrega (fechas, horarios, anticipo) + mensajesPedido (textos)
│   └── pruebas/                   node:test (reglas y API contra la BD)
│       └── escritura/             Pruebas que GUARDAN pedidos: solo contra una base de prueba
├── public/img/postre-generico.svg Imagen para postres sin foto
└── src/
    ├── environments/              URL de la API (desarrollo / producción)
    ├── styles/                    variables (paleta), base, botones, formularios
    └── app/
        ├── core/                  Lo que se usa una sola vez en toda la app
        │   ├── layout/            encabezado · pie-pagina
        │   ├── interceptores/     errores-api (convierte errores en { codigo, mensaje, detalles })
        │   ├── modelos/           error-api · articulo-carrito
        │   ├── servicios/         carrito (localStorage, junta postres iguales) · avisos entre pantallas
        │   └── utilidades/        errores
        ├── shared/componentes/    Reutilizables: tarjeta-producto, imagen-postre, mensaje-estado, cargando,
        │                          dialogo-confirmacion (RNF-5)
        └── features/              Un folder por módulo
            ├── catalogo/          MÓDULO 1 · Catálogo Público
            │   ├── catalogo.routes.ts
            │   ├── modelos/       catalogo.models.ts
            │   ├── servicios/     catalogo-api.service.ts
            │   ├── utilidades/    reglas-configuracion.ts (precio y validación en vivo)
            │   ├── componentes/   filtros-catalogo · selector-caracteristica · galeria-plantillas
            │   └── paginas/       catalogo (RF-1) · ficha-producto (RF-2) · personalizar (RF-3, RF-4)
            ├── carrito/           MÓDULO 2 · carrito (RF-6 a RF-9)
            ├── pedidos/           MÓDULO 2 · Portal de Pedidos
            │   ├── pedidos.routes.ts · pedidos-comun.css (diseño "Carrito y Checkout")
            │   ├── modelos/       pedidos.models.ts
            │   ├── servicios/     pedidos-api.service.ts · pedido-en-curso.service.ts (datos guardados, RNF-7)
            │   ├── utilidades/    fechas.ts · validacion-pedido.ts
            │   ├── componentes/   pasos-pedido · resumen-pedido · calendario-entrega
            │   └── paginas/       entrega (RF-10) · pago (RF-11, RF-12) · confirmacion · mis-pedidos (RF-13)
            └── no-encontrado/
```

## Rutas

| Ruta | Página | Requerimiento |
|---|---|---|
| `/` | Catálogo (inicio) con filtros en la URL `?categoria=&buscar=&orden=` | RF-1 |
| `/producto/:id` | Ficha: incluye, ingredientes, qué se personaliza | RF-2 |
| `/producto/:id/personalizar` | Personalizador + galería de plantillas | RF-3, RF-4 |
| `/producto/:id/personalizar?editar=<id>` | Editar un postre que ya está en el carrito | RF-8 |
| `/carrito` | Carrito: revisa disponibilidad y precio, cantidades, editar, quitar, vaciar | RF-6 a RF-9 |
| `/pedido/entrega` | Contacto, recoger o envío (cobertura por C.P.), calendario con cupo, horario | RF-10 |
| `/pedido/pago` | Método (efectivo / transferencia), anticipo o total, enviar pedido | RF-11, RF-12 |
| `/pedido/confirmacion` | Folio, estado, total, entrega e instrucciones de pago | RF-12 |
| `/mis-pedidos` | Seguimiento con folio + teléfono y envío del comprobante de pago | RF-13 |

## API del catálogo (solo lectura, sin inicio de sesión)

| Método y ruta | Qué regresa |
|---|---|
| `GET /api/salud` | Si MySQL responde |
| `GET /api/catalogo/categorias` | Categorías activas con productos visibles |
| `GET /api/catalogo/productos?categoria=&buscar=&orden=nombre\|precio_asc\|precio_desc` | Productos (plantillas base) |
| `GET /api/catalogo/productos/:id` | Ficha |
| `GET /api/catalogo/productos/:id/personalizacion` | Características, opciones, exclusiones y la selección de la plantilla |
| `GET /api/catalogo/productos/:id/plantillas` | Otras plantillas de la misma categoría |
| `POST /api/catalogo/cotizar` | Valida una configuración y da el precio vigente (no guarda nada) |

## API del Portal de Pedidos (Módulo 2, sin inicio de sesión)

| Método y ruta | Qué hace | Límite |
|---|---|---|
| `POST /api/carrito/validar` | Disponibilidad y precio vigente de cada postre del carrito (no guarda) | 60/min |
| `GET /api/entrega/reglas` | Días, horarios, fechas llenas, anticipo y si hay transferencia | — |
| `GET /api/entrega/cobertura/:cp` | Si hay envío a ese C.P. y su costo | 30/min |
| `POST /api/pedidos` | Registra el pedido (201) o regresa el mismo folio si fue doble envío (200) | 10/hora (`LIMITE_PEDIDOS_HORA`) |
| `POST /api/pedidos/consulta` | Seguimiento con folio + teléfono | 5 fallos / 15 min |
| `POST /api/pedidos/comprobante` | Enlace del comprobante de pago (queda «Pendiente» para el Panel) | 10/hora |

Cómo se conecta con el Panel (sin cambiar tablas):

- El pedido entra como `En proceso de aceptacion` con su primer `HistorialEstado` (sin usuario) y aparece en
  «Por revisar» del Panel. La capacidad diaria se cuenta igual que en la agenda (sin entregados, cancelados ni bajas).
- Los precios y costos se congelan desde `vw_CostoPrecioPostre`; cada postre personalizado se guarda como un `Postre`
  oculto (`MostrarEnTienda = 0`) con su `ClaveConfiguracion`, igual que en el Panel (si ya existe se reutiliza).
- El **costo de envío no tiene columna**: va dentro de `Pedido.MontoTotal`. El Panel lo muestra como
  total − suma de los postres (`Pedido.getCostoEnvio()`).
- El aviso «Pedido registrado» se guarda en `Notificacion` como `Pendiente`; el Panel lo envía por WhatsApp/correo
  en su revisión de cada minuto (la página no tiene las credenciales de esos servicios).
- Los comprobantes se guardan en `Pago` como `Pendiente` y se validan en el Panel (apartado Pagos).

## Plantillas = productos base

Un **producto del catálogo** es un `Postre` con `NombrePostre`, `MostrarEnTienda = 1`, categoría activa y precio > 0.
Ese producto **es la plantilla**: sus filas de `PostreOpcion` se precargan en el personalizador y el cliente
las cambia. La galería de plantillas muestra los demás productos base de la misma categoría.
No se usa la columna `Postre.EsPlantilla` de la migración.

## Seguridad de la conexión

- Usuario y contraseña de MySQL solo en `ApiCARMIR/.env` (ignorado por Git).
- Dos pools `mysql2`: lectura (`carmir_web`, cada conexión `READ ONLY`) y escritura (`carmir_pedidos`, solo
  `INSERT` en las tablas del pedido y `UPDATE` del correo del cliente; no puede borrar ni leer usuarios).
- Consultas parametrizadas (`?`) en todo; el orden se elige de una lista fija.
- Cada pedido se guarda en **una transacción** con candado (`GET_LOCK`) para que dos clientes no ocupen el último
  cupo del día al mismo tiempo; si algo falla no queda nada a medias.
- La API **nunca confía en el navegador**: vuelve a validar todo y recalcula precios, envío y anticipo.
  Si el total cambió responde `PRECIO_CAMBIO` y el cliente confirma otra vez.
- Llave contra doble envío (`llaveIdempotencia`) + revisión de pedido igual en los últimos 15 minutos.
- CORS solo para `http://localhost:4200` (configurable), helmet, límites por ruta, cuerpo JSON máx. 32 KB.
- Validación de toda entrada; los errores internos no se mandan al navegador (solo un número de referencia).
- El costo de las recetas nunca sale de la API: solo precios.

## Reglas de estilo

Sin ternarios, funciones flecha solo si son necesarias (no hay ninguna), `bind(this)` comentado
donde se pasa un método como callback, comentarios en español, estado de pantalla con `signal`.
