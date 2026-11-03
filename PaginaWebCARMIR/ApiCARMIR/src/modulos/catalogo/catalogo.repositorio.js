/*
 * Repositorio del Catálogo Público: aquí están TODAS las consultas SQL del módulo.
 * Solo hay SELECT (el catálogo público no modifica nada).
 * Todas usan "?" para los valores: el texto del cliente nunca se pega dentro del SQL.
 *
 * Tablas y vistas que lee: CategoriaPostre, Postre, PostreOpcion, Caracteristica,
 * OpcionCaracteristica, ExclusionOpciones, Receta, RecetaInsumo, Insumo,
 * vw_CostoReceta y vw_CostoPrecioPostre.
 *
 * Producto del catálogo = plantilla base: un Postre con nombre, visible en la tienda
 * (MostrarEnTienda = 1), de una categoría activa y con precio mayor a 0.
 */
const baseDatos = require('../../config/baseDatos');

// Condición que debe cumplir un postre para verse en el catálogo público (RF-1, paso 3)
const CONDICION_PRODUCTO_VISIBLE =
  "p.MostrarEnTienda = 1 " +
  "AND p.NombrePostre IS NOT NULL AND TRIM(p.NombrePostre) <> '' " +
  "AND c.EstadoCategoria = 'Activa' " +
  'AND v.PrecioVenta IS NOT NULL AND v.PrecioVenta > 0';

const SELECT_PRODUCTO =
  'SELECT p.IdPostre, p.NombrePostre, p.DescripcionPostre, p.LinkFotografia, ' +
  'p.IdCategoria, c.NombreCategoria, v.PrecioVenta ' +
  'FROM Postre p ' +
  'JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria ' +
  'JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre ' +
  'WHERE ' + CONDICION_PRODUCTO_VISIBLE;

// El orden NUNCA se arma con texto del cliente: solo se elige uno de estos
const ORDENES = {
  nombre: ' ORDER BY p.NombrePostre ASC',
  precio_asc: ' ORDER BY v.PrecioVenta ASC, p.NombrePostre ASC',
  precio_desc: ' ORDER BY v.PrecioVenta DESC, p.NombrePostre ASC'
};

// Escapa % y _ para que el buscador los tome como letras normales
function patronBusqueda(texto) {
  const escapado = texto.replace(/\\/g, '\\\\').replace(/%/g, '\\%').replace(/_/g, '\\_');
  return '%' + escapado + '%';
}

// ===================== Categorías =====================

// Categorías activas que tienen al menos un producto visible
async function listarCategorias() {
  const sql =
    'SELECT c.IdCategoria, c.NombreCategoria, c.DescripcionCategoria, COUNT(*) AS TotalProductos ' +
    'FROM Postre p ' +
    'JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria ' +
    'JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre ' +
    'WHERE ' + CONDICION_PRODUCTO_VISIBLE + ' ' +
    'GROUP BY c.IdCategoria, c.NombreCategoria, c.DescripcionCategoria ' +
    'ORDER BY c.NombreCategoria';
  return baseDatos.consultar(sql);
}

// Cuántos productos se ven en el catálogo (lo usa /api/salud)
async function contarProductosVisibles() {
  const filas = await baseDatos.consultar(
    'SELECT COUNT(*) AS Total FROM Postre p ' +
    'JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria ' +
    'JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre ' +
    'WHERE ' + CONDICION_PRODUCTO_VISIBLE
  );
  return Number(filas[0].Total);
}

// Una categoría por id (activa o no); null si no existe
async function buscarCategoria(idCategoria) {
  const filas = await baseDatos.consultar(
    'SELECT IdCategoria, NombreCategoria, EstadoCategoria FROM CategoriaPostre WHERE IdCategoria = ?',
    [idCategoria]
  );
  if (filas.length === 0) {
    return null;
  }
  return filas[0];
}

// ===================== Productos (plantillas base) =====================

// Productos visibles con filtro opcional de categoría y búsqueda
// RF-1 · Pasos 2 y 6: productos visibles filtrados por categoría y búsqueda.
async function listarProductos(idCategoria, buscar, orden) {
  let sql = SELECT_PRODUCTO;
  const parametros = [];

  if (idCategoria > 0) {
    sql = sql + ' AND p.IdCategoria = ?';
    parametros.push(idCategoria);
  }
  if (buscar !== '') {
    sql = sql + ' AND (p.NombrePostre LIKE ? OR p.DescripcionPostre LIKE ? OR c.NombreCategoria LIKE ?)';
    const patron = patronBusqueda(buscar);
    parametros.push(patron);
    parametros.push(patron);
    parametros.push(patron);
  }

  let ordenSql = ORDENES[orden];
  if (ordenSql === undefined) {
    ordenSql = ORDENES.nombre;
  }
  sql = sql + ordenSql;

  return baseDatos.consultar(sql, parametros);
}

// Un producto visible por id; null si no existe o ya no está disponible
// RF-2 / RF-3 / RF-4 · Paso 2: valida que el producto exista, esté activo y visible en la tienda.
async function buscarProductoVisible(idPostre) {
  const filas = await baseDatos.consultar(SELECT_PRODUCTO + ' AND p.IdPostre = ?', [idPostre]);
  if (filas.length === 0) {
    return null;
  }
  return filas[0];
}

// Opciones que forman un producto (lo que trae la plantilla), con su característica
// RF-2 · Paso 3 / RF-4 · Paso 7: opciones que trae el producto (valores de la plantilla).
async function listarOpcionesDeProducto(idPostre) {
  const sql =
    'SELECT po.IdOpcion, po.CantidadElegida, o.NombreOpcion, o.Disponible AS OpcionDisponible, ' +
    'ca.IdCaracteristica, ca.NombreCaracteristica, ca.OrdenMostrar, ca.Disponible AS CaracteristicaDisponible ' +
    'FROM PostreOpcion po ' +
    'JOIN OpcionCaracteristica o ON o.IdOpcion = po.IdOpcion ' +
    'JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica ' +
    'WHERE po.IdPostre = ? ' +
    'ORDER BY ca.OrdenMostrar, ca.NombreCaracteristica, o.NombreOpcion';
  return baseDatos.consultar(sql, [idPostre]);
}

// Opciones de TODOS los productos visibles de una categoría (para la galería de plantillas)
// RF-4 · Paso 2: opciones de las plantillas de la categoría.
async function listarOpcionesDePlantillas(idCategoria) {
  const sql =
    'SELECT po.IdPostre, po.IdOpcion, po.CantidadElegida ' +
    'FROM PostreOpcion po ' +
    'JOIN Postre p ON p.IdPostre = po.IdPostre ' +
    "WHERE p.IdCategoria = ? AND p.MostrarEnTienda = 1 AND p.NombrePostre IS NOT NULL";
  return baseDatos.consultar(sql, [idCategoria]);
}

// Nombres de los ingredientes del producto según las recetas activas de sus opciones (RF-2)
// RF-2 · Paso 3: ingredientes según las recetas de las opciones.
async function listarIngredientesDeProducto(idPostre) {
  const sql =
    'SELECT DISTINCT i.NombreInsumo ' +
    'FROM PostreOpcion po ' +
    'JOIN Receta r ON r.IdOpcion = po.IdOpcion ' +
    'JOIN RecetaInsumo ri ON ri.IdReceta = r.IdReceta ' +
    'JOIN Insumo i ON i.IdInsumo = ri.IdInsumo ' +
    "WHERE po.IdPostre = ? AND r.EstadoReceta = 'Activa' " +
    'ORDER BY i.NombreInsumo';
  return baseDatos.consultar(sql, [idPostre]);
}

// ===================== Personalización =====================

// Características de una categoría (todas; el servicio decide cuáles se muestran)
// RF-3 · Paso 2: características de la categoría.
async function listarCaracteristicas(idCategoria) {
  const sql =
    'SELECT IdCaracteristica, NombreCaracteristica, DescripcionCaracteristica, Obligatoria, ' +
    'SeleccionMultiple, PresupuestoSeleccion, OrdenMostrar, Disponible ' +
    'FROM Caracteristica WHERE IdCategoria = ? ' +
    'ORDER BY OrdenMostrar, NombreCaracteristica';
  return baseDatos.consultar(sql, [idCategoria]);
}

// Opciones de una categoría con el costo de su receta (vw_CostoReceta).
// El costo NO se manda al navegador: el servicio lo convierte en precio.
// RF-3 · Paso 2: opciones disponibles con su costo de receta.
async function listarOpciones(idCategoria) {
  const sql =
    'SELECT o.IdOpcion, o.IdCaracteristica, o.NombreOpcion, o.DescripcionOpcion, o.LinkImagen, ' +
    'o.MultiplicadorIngredientes, o.MultiplicadorCosto, o.SeEscala, o.Disponible, ' +
    'o.PuntosSeleccion, o.MultiplicadorPresupuesto, cr.CostoReceta ' +
    'FROM OpcionCaracteristica o ' +
    'JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica ' +
    'LEFT JOIN vw_CostoReceta cr ON cr.IdOpcion = o.IdOpcion ' +
    'WHERE ca.IdCategoria = ? ' +
    'ORDER BY ca.OrdenMostrar, o.IdOpcion';
  return baseDatos.consultar(sql, [idCategoria]);
}

// Exclusiones activas entre opciones de una categoría
// RF-3 · Paso 2: exclusiones activas entre opciones.
async function listarExclusiones(idCategoria) {
  const sql =
    'SELECT e.IdExclusion, e.IdOpcion, e.IdOpcionExcluida, e.MotivoExclusion, ' +
    'o1.NombreOpcion AS NombreOpcion, o2.NombreOpcion AS NombreOpcionExcluida ' +
    'FROM ExclusionOpciones e ' +
    'JOIN OpcionCaracteristica o1 ON o1.IdOpcion = e.IdOpcion ' +
    'JOIN Caracteristica c1 ON c1.IdCaracteristica = o1.IdCaracteristica ' +
    'JOIN OpcionCaracteristica o2 ON o2.IdOpcion = e.IdOpcionExcluida ' +
    "WHERE e.EstadoExclusion = 'Activa' AND c1.IdCategoria = ?";
  return baseDatos.consultar(sql, [idCategoria]);
}

module.exports = {
  listarCategorias: listarCategorias,
  contarProductosVisibles: contarProductosVisibles,
  buscarCategoria: buscarCategoria,
  listarProductos: listarProductos,
  buscarProductoVisible: buscarProductoVisible,
  listarOpcionesDeProducto: listarOpcionesDeProducto,
  listarOpcionesDePlantillas: listarOpcionesDePlantillas,
  listarIngredientesDeProducto: listarIngredientesDeProducto,
  listarCaracteristicas: listarCaracteristicas,
  listarOpciones: listarOpciones,
  listarExclusiones: listarExclusiones
};
