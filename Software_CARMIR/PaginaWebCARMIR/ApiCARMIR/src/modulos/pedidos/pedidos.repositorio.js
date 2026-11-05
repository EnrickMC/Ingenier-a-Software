/*
 * Repositorio del Portal de Pedidos (Módulo 2): aquí están TODAS las consultas SQL del módulo.
 * Todas usan "?" para los valores: el texto del cliente nunca se pega dentro del SQL.
 *
 * Hay dos tipos de funciones:
 *  - Las que reciben "conexion": se usan DENTRO de una transacción del usuario de escritura
 *    (registrar el pedido o un comprobante). Si algo falla, la transacción deshace todo.
 *  - Las que no la reciben: solo leen con el usuario de lectura (reglas, cobertura, seguimiento).
 *
 * Mismas reglas que el Panel (Módulo 4):
 *  - Un pedido "activo" no está cancelado (Cancelacion) ni dado de baja (BajaPedido).
 *  - La carga de un día = pedidos activos que no se han entregado con esa FechaEntrega
 *    (igual que PedidoDAOMySQL.contarEntregasDelDia del Panel).
 */
const baseDatos = require('../../config/baseDatos');

// Igual que ApoyoPedidosMySQL.SIN_CANCELAR_NI_BAJA del Panel
const SIN_CANCELAR_NI_BAJA =
  'NOT EXISTS (SELECT 1 FROM Cancelacion cx WHERE cx.Folio = p.Folio) ' +
  'AND NOT EXISTS (SELECT 1 FROM BajaPedido bx WHERE bx.Folio = p.Folio)';

// Primera fila o null
function primera(filas) {
  if (filas.length === 0) {
    return null;
  }
  return filas[0];
}

// ===================== Configuración y agenda (lectura) =====================

// Parámetros del negocio (tabla Configuracion de MIGRACION_COMPLETA.sql)
async function leerConfiguracion() {
  try {
    return await baseDatos.consultar('SELECT Clave, Valor FROM Configuracion');
  } catch (error) {
    // Sin la migración la tabla no existe: se usan los valores por defecto
    if (error.code === 'ER_NO_SUCH_TABLE') {
      console.error('[pedidos] Falta la tabla Configuracion (MIGRACION_COMPLETA.sql); se usan valores por defecto');
      return [];
    }
    throw error;
  }
}

// Pedidos activos por día entre dos fechas: [{ Fecha: '2026-11-10', Total: 3 }]
async function contarEntregasPorDia(desde, hasta) {
  const sql =
    "SELECT DATE_FORMAT(p.FechaEntrega, '%Y-%m-%d') AS Fecha, COUNT(*) AS Total " +
    'FROM Pedido p ' +
    "WHERE p.FechaEntrega BETWEEN ? AND ? AND p.EstadoPedido <> 'Entregado' AND " + SIN_CANCELAR_NI_BAJA + ' ' +
    'GROUP BY p.FechaEntrega';
  return baseDatos.consultar(sql, [desde, hasta]);
}

// Costo de envío de un código postal; null si no hay cobertura
async function buscarCobertura(codigoPostal) {
  const filas = await baseDatos.consultar(
    'SELECT CodigoPostal, CostoEnvio FROM CodigoPostalCobertura WHERE CodigoPostal = ?',
    [codigoPostal]
  );
  return primera(filas);
}

// ===================== Registrar el pedido (dentro de la transacción) =====================

// Misma cuenta que contarEntregasPorDia, pero para un solo día y dentro de la transacción
async function contarEntregasDelDia(conexion, fecha) {
  const sql =
    'SELECT COUNT(*) AS Total FROM Pedido p ' +
    "WHERE p.FechaEntrega = ? AND p.EstadoPedido <> 'Entregado' AND " + SIN_CANCELAR_NI_BAJA;
  const filas = await baseDatos.ejecutar(conexion, sql, [fecha]);
  return Number(filas[0].Total);
}

async function buscarCoberturaEnTransaccion(conexion, codigoPostal) {
  const filas = await baseDatos.ejecutar(
    conexion,
    'SELECT CodigoPostal, CostoEnvio FROM CodigoPostalCobertura WHERE CodigoPostal = ?',
    [codigoPostal]
  );
  return primera(filas);
}

async function buscarClientePorTelefono(conexion, telefono) {
  const filas = await baseDatos.ejecutar(
    conexion,
    'SELECT IdCliente, NombreCliente, TelefonoWhatsApp, CorreoCliente FROM Cliente WHERE TelefonoWhatsApp = ?',
    [telefono]
  );
  return primera(filas);
}

async function insertarCliente(conexion, cliente) {
  const resultado = await baseDatos.ejecutar(
    conexion,
    'INSERT INTO Cliente (NombreCliente, TelefonoWhatsApp, CorreoCliente) VALUES (?, ?, ?)',
    [cliente.nombre, cliente.telefono, textoONulo(cliente.correo)]
  );
  return resultado.insertId;
}

// Solo llena el correo si el cliente no tenía uno (nunca se reemplaza un dato que ya existe)
async function completarCorreo(conexion, idCliente, correo) {
  await baseDatos.ejecutar(
    conexion,
    "UPDATE Cliente SET CorreoCliente = ? WHERE IdCliente = ? AND (CorreoCliente IS NULL OR CorreoCliente = '')",
    [correo, idCliente]
  );
}

async function insertarDireccion(conexion, idCliente, direccion) {
  const resultado = await baseDatos.ejecutar(
    conexion,
    'INSERT INTO Direccion (IdCliente, Calle, NumeroExterior, NumeroInterior, Colonia, CodigoPostal, Referencias) ' +
      'VALUES (?, ?, ?, ?, ?, ?, ?)',
    [
      idCliente,
      direccion.calle,
      direccion.numeroExterior,
      textoONulo(direccion.numeroInterior),
      direccion.colonia,
      direccion.codigoPostal,
      textoONulo(direccion.referencias)
    ]
  );
  return resultado.insertId;
}

async function insertarEntregaDomicilio(conexion, folio, idDireccion) {
  await baseDatos.ejecutar(conexion, 'INSERT INTO EntregaDomicilio (Folio, IdDireccion) VALUES (?, ?)', [
    folio,
    idDireccion
  ]);
}

// Postre por su ClaveConfiguracion (cualquier postre: del catálogo o personalizado); null si no existe
async function buscarPostrePorClave(conexion, clave) {
  const filas = await baseDatos.ejecutar(
    conexion,
    'SELECT IdPostre, IdCategoria, NombrePostre FROM Postre WHERE ClaveConfiguracion = ?',
    [clave]
  );
  return primera(filas);
}

// Postre personalizado: sin nombre y oculto en la tienda (igual que ModificacionPedidoService del Panel)
async function insertarPostrePersonalizado(conexion, idCategoria, clave) {
  const resultado = await baseDatos.ejecutar(
    conexion,
    'INSERT INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, LinkFotografia, MostrarEnTienda) ' +
      'VALUES (?, ?, NULL, NULL, NULL, 0)',
    [idCategoria, clave]
  );
  return resultado.insertId;
}

// capa = NumeroCapa (1 = Base)
async function insertarPostreOpcion(conexion, idPostre, idOpcion, cantidad, capa) {
  await baseDatos.ejecutar(
    conexion,
    'INSERT INTO PostreOpcion (IdPostre, IdOpcion, NumeroCapa, CantidadElegida) VALUES (?, ?, ?, ?)',
    [idPostre, idOpcion, capa, cantidad]
  );
}

// Precio y costo vigentes del postre (la misma vista que usa el Panel); null si no tiene opciones
async function leerPrecioPostre(conexion, idPostre) {
  const filas = await baseDatos.ejecutar(
    conexion,
    'SELECT PrecioVenta, CostoIngredientes FROM vw_CostoPrecioPostre WHERE IdPostre = ?',
    [idPostre]
  );
  return primera(filas);
}

async function insertarPedido(conexion, pedido) {
  const resultado = await baseDatos.ejecutar(
    conexion,
    'INSERT INTO Pedido (IdCliente, FechaEntrega, HorarioEntrega, ModalidadEntrega, ModalidadPago, ' +
      "MontoTotal, AnticipoRequerido, EstadoPedido) VALUES (?, ?, ?, ?, ?, ?, ?, 'En proceso de aceptacion')",
    [
      pedido.idCliente,
      pedido.fechaEntrega,
      pedido.horarioEntrega + ':00',
      pedido.modalidadEntrega,
      pedido.modalidadPago,
      pedido.montoTotal,
      pedido.anticipoRequerido
    ]
  );
  return resultado.insertId;
}

async function insertarDetalle(conexion, folio, linea) {
  await baseDatos.ejecutar(
    conexion,
    'INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, CostoUnitario) ' +
      'VALUES (?, ?, ?, ?, ?, ?, ?)',
    [
      folio,
      linea.idPostre,
      linea.cantidad,
      textoONulo(linea.dedicatoria),
      textoONulo(linea.comentario),
      linea.precioUnitario,
      linea.costoUnitario
    ]
  );
}

// Primer registro del historial: lo hizo el cliente desde la web (IdUsuario vacío)
async function insertarHistorialInicial(conexion, folio) {
  await baseDatos.ejecutar(
    conexion,
    'INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo) ' +
      "VALUES (?, NULL, NULL, 'En proceso de aceptacion')",
    [folio]
  );
}

/*
 * Doble envío (RF-12): busca un pedido IGUAL del mismo cliente registrado en los últimos minutos
 * (misma entrega, mismo total y aún sin revisar). Sirve aunque la API se haya reiniciado.
 * Regresa el folio o 0.
 */
async function buscarPedidoReciente(conexion, idCliente, pedido, minutos) {
  const sql =
    'SELECT p.Folio, ' +
    "  (SELECT GROUP_CONCAT(CONCAT(d.IdPostre, ':', d.Cantidad) ORDER BY d.IdPostre SEPARATOR '|') " +
    '     FROM DetallePedido d WHERE d.Folio = p.Folio) AS Lineas ' +
    'FROM Pedido p ' +
    'WHERE p.IdCliente = ? AND p.FechaEntrega = ? AND p.HorarioEntrega = ? AND p.ModalidadEntrega = ? ' +
    "AND p.ModalidadPago = ? AND p.MontoTotal = ? AND p.EstadoPedido = 'En proceso de aceptacion' " +
    'AND p.FechaSolicitud >= DATE_SUB(NOW(), INTERVAL ? MINUTE) AND ' + SIN_CANCELAR_NI_BAJA + ' ' +
    'ORDER BY p.Folio DESC';
  const filas = await baseDatos.ejecutar(conexion, sql, [
    idCliente,
    pedido.fechaEntrega,
    pedido.horarioEntrega + ':00',
    pedido.modalidadEntrega,
    pedido.modalidadPago,
    pedido.montoTotal,
    minutos
  ]);
  for (let i = 0; i < filas.length; i++) {
    if (filas[i].Lineas === pedido.huellaLineas) {
      return filas[i].Folio;
    }
  }
  return 0;
}

// ===================== Notificaciones (RF-47) =====================

/*
 * Deja el aviso en la bitácora como "Pendiente": el Panel (que tiene las credenciales de WhatsApp
 * y correo) lo envía en segundo plano y lo marca "Enviado" o "Fallido".
 */
async function insertarNotificacion(conexion, aviso) {
  await baseDatos.ejecutar(
    conexion,
    'INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, ResultadoEnvio, MotivoFallo) ' +
      "VALUES (?, ?, ?, ?, ?, 'Pendiente', NULL)",
    [aviso.folio, aviso.tipoEvento, aviso.canal, aviso.destino, aviso.mensaje]
  );
}

// ===================== Seguimiento (RF-13, lectura) =====================

const SELECT_PEDIDO_CLIENTE =
  'SELECT p.Folio, p.FechaSolicitud, p.FechaEntrega, p.HorarioEntrega, p.ModalidadEntrega, p.ModalidadPago, ' +
  'p.MontoTotal, p.AnticipoRequerido, p.EstadoPedido, c.NombreCliente, c.TelefonoWhatsApp, ' +
  'COALESCE(s.TotalPagado, 0) AS TotalPagado, ' +
  "(SELECT COALESCE(SUM(pg.MontoPago), 0) FROM Pago pg WHERE pg.Folio = p.Folio AND pg.EstadoValidacion = 'Pendiente') AS PorValidar, " +
  '(SELECT COALESCE(SUM(d.Cantidad * d.PrecioUnitario), 0) FROM DetallePedido d WHERE d.Folio = p.Folio) AS SumaProductos, ' +
  'cn.MotivoCancelacion, cn.FechaHoraCancelacion, cn.MontoReembolso, cn.EstadoReembolso, ' +
  'b.MotivoBaja, b.FechaHoraBaja, ' +
  'dir.Calle, dir.NumeroExterior, dir.NumeroInterior, dir.Colonia, dir.CodigoPostal, dir.Referencias ' +
  'FROM Pedido p ' +
  'JOIN Cliente c ON c.IdCliente = p.IdCliente ' +
  'LEFT JOIN vw_SaldoPedido s ON s.Folio = p.Folio ' +
  'LEFT JOIN Cancelacion cn ON cn.Folio = p.Folio ' +
  'LEFT JOIN BajaPedido b ON b.Folio = p.Folio ' +
  'LEFT JOIN EntregaDomicilio ed ON ed.Folio = p.Folio ' +
  'LEFT JOIN Direccion dir ON dir.IdDireccion = ed.IdDireccion ';

// Condición del dato de contacto: teléfono de WhatsApp o correo (el correo sin importar mayúsculas)
function condicionContacto(contacto) {
  if (contacto.telefono !== '') {
    return { sql: 'WHERE p.Folio = ? AND c.TelefonoWhatsApp = ?', valor: contacto.telefono };
  }
  return { sql: 'WHERE p.Folio = ? AND LOWER(c.CorreoCliente) = LOWER(?)', valor: contacto.correo };
}

// El pedido solo se encuentra si el folio Y el teléfono (o el correo) coinciden (RF-13)
// "contacto" = { telefono, correo }: se usa el que traiga texto
async function buscarPedidoDeCliente(folio, contacto) {
  const condicion = condicionContacto(contacto);
  const filas = await baseDatos.consultar(SELECT_PEDIDO_CLIENTE + condicion.sql, [folio, condicion.valor]);
  return primera(filas);
}

async function listarLineas(folio) {
  const sql =
    'SELECT d.IdPostre, d.Cantidad, d.Dedicatoria, d.Comentario, d.PrecioUnitario, ' +
    'po.NombrePostre, po.LinkFotografia, cat.NombreCategoria ' +
    'FROM DetallePedido d ' +
    'JOIN Postre po ON po.IdPostre = d.IdPostre ' +
    'JOIN CategoriaPostre cat ON cat.IdCategoria = po.IdCategoria ' +
    'WHERE d.Folio = ? ORDER BY d.IdPostre';
  return baseDatos.consultar(sql, [folio]);
}

// Opciones de todos los postres de un pedido (para mostrar su personalización)
async function listarOpcionesDeLineas(folio) {
  const sql =
    'SELECT po.IdPostre, po.NumeroCapa, po.CantidadElegida, o.NombreOpcion, ca.NombreCaracteristica ' +
    'FROM DetallePedido d ' +
    'JOIN PostreOpcion po ON po.IdPostre = d.IdPostre ' +
    'JOIN OpcionCaracteristica o ON o.IdOpcion = po.IdOpcion ' +
    'JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica ' +
    'WHERE d.Folio = ? ORDER BY ca.OrdenMostrar, ca.NombreCaracteristica, po.NumeroCapa, o.NombreOpcion';
  return baseDatos.consultar(sql, [folio]);
}

async function listarHistorial(folio) {
  const sql =
    'SELECT EstadoAnterior, EstadoNuevo, FechaHoraCambio FROM HistorialEstado ' +
    'WHERE Folio = ? ORDER BY FechaHoraCambio, IdHistorial';
  return baseDatos.consultar(sql, [folio]);
}

async function listarPagos(folio) {
  const sql =
    'SELECT MontoPago, FechaPago, MetodoPago, Referencia, EstadoValidacion, MotivoRechazo FROM Pago ' +
    'WHERE Folio = ? ORDER BY FechaPago, IdPago';
  return baseDatos.consultar(sql, [folio]);
}

// ===================== Comprobante de pago (dentro de la transacción) =====================

// Datos del pedido para registrar un pago; FOR UPDATE no se usa: el candado con nombre ya ordena las peticiones
async function buscarPedidoParaPago(conexion, folio, contacto) {
  const condicion = condicionContacto(contacto);
  const filas = await baseDatos.ejecutar(conexion, SELECT_PEDIDO_CLIENTE + condicion.sql, [folio, condicion.valor]);
  return primera(filas);
}

// Misma regla que PagoDAOMySQL.referenciaEnUso del Panel: no cuenta los pagos rechazados
async function referenciaEnUso(conexion, referencia) {
  const filas = await baseDatos.ejecutar(
    conexion,
    "SELECT COUNT(*) AS Total FROM Pago WHERE Referencia = ? AND EstadoValidacion <> 'Rechazado'",
    [referencia]
  );
  return Number(filas[0].Total) > 0;
}

async function contarPagosPendientes(conexion, folio) {
  const filas = await baseDatos.ejecutar(
    conexion,
    "SELECT COUNT(*) AS Total FROM Pago WHERE Folio = ? AND EstadoValidacion = 'Pendiente'",
    [folio]
  );
  return Number(filas[0].Total);
}

// El pago queda "Pendiente" y sin usuario: lo valida o rechaza la Administradora en el Panel
async function insertarPago(conexion, pago) {
  const resultado = await baseDatos.ejecutar(
    conexion,
    'INSERT INTO Pago (Folio, IdUsuario, MontoPago, MetodoPago, Referencia, LinkComprobante, EstadoValidacion) ' +
      "VALUES (?, NULL, ?, ?, ?, ?, 'Pendiente')",
    [pago.folio, pago.monto, pago.metodo, pago.referencia, pago.linkComprobante]
  );
  return resultado.insertId;
}

// RF-14 · Paso 7: foto o PDF del comprobante que el cliente subió desde sus archivos.
// Se guarda en la BD (no en una carpeta pública) con el mismo IdPago. El Panel todavía no muestra este
// archivo en «Validación de pagos» (solo el enlace).
async function insertarArchivoComprobante(conexion, idPago, archivo) {
  await baseDatos.ejecutar(
    conexion,
    'INSERT INTO ComprobanteArchivo (IdPago, NombreArchivo, TipoContenido, TamanoBytes, Contenido) VALUES (?, ?, ?, ?, ?)',
    [idPago, archivo.nombre, archivo.tipo, archivo.tamano, archivo.contenido]
  );
}

// ===================== Apoyo =====================

// Texto vacío = NULL en la base de datos
function textoONulo(texto) {
  if (texto === undefined || texto === null || texto === '') {
    return null;
  }
  return texto;
}

module.exports = {
  leerConfiguracion: leerConfiguracion,
  contarEntregasPorDia: contarEntregasPorDia,
  buscarCobertura: buscarCobertura,
  contarEntregasDelDia: contarEntregasDelDia,
  buscarCoberturaEnTransaccion: buscarCoberturaEnTransaccion,
  buscarClientePorTelefono: buscarClientePorTelefono,
  insertarCliente: insertarCliente,
  completarCorreo: completarCorreo,
  insertarDireccion: insertarDireccion,
  insertarEntregaDomicilio: insertarEntregaDomicilio,
  buscarPostrePorClave: buscarPostrePorClave,
  insertarPostrePersonalizado: insertarPostrePersonalizado,
  insertarPostreOpcion: insertarPostreOpcion,
  leerPrecioPostre: leerPrecioPostre,
  insertarPedido: insertarPedido,
  insertarDetalle: insertarDetalle,
  insertarHistorialInicial: insertarHistorialInicial,
  buscarPedidoReciente: buscarPedidoReciente,
  insertarNotificacion: insertarNotificacion,
  buscarPedidoDeCliente: buscarPedidoDeCliente,
  listarLineas: listarLineas,
  listarOpcionesDeLineas: listarOpcionesDeLineas,
  listarHistorial: listarHistorial,
  listarPagos: listarPagos,
  buscarPedidoParaPago: buscarPedidoParaPago,
  referenciaEnUso: referenciaEnUso,
  contarPagosPendientes: contarPagosPendientes,
  insertarPago: insertarPago,
  insertarArchivoComprobante: insertarArchivoComprobante
};
