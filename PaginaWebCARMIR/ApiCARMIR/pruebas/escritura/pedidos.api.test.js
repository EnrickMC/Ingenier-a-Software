/*
 * Pruebas del Portal de Pedidos (Módulo 2) que GUARDAN pedidos de prueba en la base.
 * ¡Úsalas solo en una base de PRUEBA! (dejan pedidos, clientes y pagos de prueba)
 *
 * Requisitos: base con CARMIR_BaseDeDatos.sql + MIGRACION_COMPLETA.sql y las semillas 3, 5 y 1;
 * el .env con DB_ESCRITURA_USUARIO (sql/crear_usuario_pedidos.sql) y al menos un código postal
 * en CodigoPostalCobertura (la prueba usa el primero que encuentre).
 * Ejecutar con:  npm run test:pedidos
 */
// Para que el límite de 10 pedidos por hora no frene las pruebas
process.env.LIMITE_PEDIDOS_HORA = '200';

const prueba = require('node:test');
const assert = require('node:assert');
const crypto = require('crypto');
const crearApp = require('../../src/app');
const baseDatos = require('../../src/config/baseDatos');
const reglasEntrega = require('../../src/modulos/pedidos/reglasEntrega');

let servidor = null;
let base = '';
let reglas = null;
let codigoPostal = '';
let costoEnvio = 0;

prueba.before(async function () {
  const app = crearApp();
  servidor = app.listen(0, '127.0.0.1');
  await new Promise(function (resolver) {
    servidor.on('listening', resolver);
  });
  base = 'http://127.0.0.1:' + servidor.address().port + '/api';
  reglas = reglasEntrega.armarReglas(await baseDatos.consultar('SELECT Clave, Valor FROM Configuracion'));
  const coberturas = await baseDatos.consultar('SELECT CodigoPostal, CostoEnvio FROM CodigoPostalCobertura LIMIT 1');
  if (coberturas.length > 0) {
    codigoPostal = coberturas[0].CodigoPostal;
    costoEnvio = Number(coberturas[0].CostoEnvio);
  }
});

prueba.after(async function () {
  servidor.close();
  await baseDatos.cerrar();
});

// ===================== Apoyo =====================

async function enviar(ruta, datos) {
  const respuesta = await fetch(base + ruta, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos)
  });
  return { estado: respuesta.status, cuerpo: await respuesta.json() };
}

async function contar(sql, valores) {
  const filas = await baseDatos.consultar(sql, valores);
  return Number(filas[0].Total);
}

// Teléfono de 10 dígitos que no existe todavía
function telefonoNuevo() {
  return '9' + String(Math.floor(Math.random() * 1000000000)).padStart(9, '0');
}

// Fecha laborable (a partir de "dias" desde hoy) que todavía tenga lugar para "lugares" pedidos
async function fechaConLugar(dias, lugares) {
  let fecha = reglasEntrega.sumarDias(reglasEntrega.hoy(), dias);
  for (let i = 0; i < 60; i++) {
    if (reglasEntrega.esDiaLaborable(reglas, fecha)) {
      const carga = await contar(
        "SELECT COUNT(*) AS Total FROM Pedido p WHERE p.FechaEntrega = ? AND p.EstadoPedido <> 'Entregado' " +
          'AND NOT EXISTS (SELECT 1 FROM Cancelacion c WHERE c.Folio = p.Folio) ' +
          'AND NOT EXISTS (SELECT 1 FROM BajaPedido b WHERE b.Folio = p.Folio)',
        [fecha]
      );
      if (carga + lugares <= reglas.capacidadDiaria) {
        return { fecha: fecha, carga: carga };
      }
    }
    fecha = reglasEntrega.sumarDias(fecha, 1);
  }
  throw new Error('No hay fechas libres para la prueba');
}

// Opciones de un producto del catálogo tal como vienen en su plantilla
async function opcionesDe(idPostre) {
  const respuesta = await fetch(base + '/catalogo/productos/' + idPostre + '/personalizacion');
  const datos = await respuesta.json();
  return { opciones: datos.seleccionInicial, precio: datos.producto.precio, datos: datos };
}

function articulo(idPostre, opciones, cantidad, precioUnitario, dedicatoria) {
  return {
    idPostre: idPostre,
    opciones: opciones,
    cantidad: cantidad,
    dedicatoria: dedicatoria,
    comentario: '',
    precioUnitario: precioUnitario
  };
}

function solicitud(articulos, telefono, fecha, domicilio, total) {
  const datos = {
    llaveIdempotencia: crypto.randomUUID(),
    articulos: articulos,
    cliente: { nombre: 'Cliente de Prueba', telefono: telefono, correo: 'prueba@correo.com' },
    entrega: { modalidad: 'Recoger en tienda', fecha: fecha, horario: '12:00', direccion: null },
    pago: { metodo: 'Efectivo', modalidad: 'Anticipo' },
    totalMostrado: total
  };
  if (domicilio) {
    datos.entrega.modalidad = 'Envio a domicilio';
    datos.entrega.direccion = {
      calle: 'Calle de Prueba',
      numeroExterior: '10',
      numeroInterior: '',
      colonia: 'Centro',
      codigoPostal: codigoPostal,
      referencias: 'Portón azul'
    };
  }
  return datos;
}

function redondear(valor) {
  return Math.round(valor * 100) / 100;
}

// ===================== Pruebas =====================

prueba.test('reglas de entrega: fecha mínima, horarios y métodos de pago', async function () {
  const respuesta = await fetch(base + '/entrega/reglas');
  const datos = await respuesta.json();
  assert.strictEqual(respuesta.status, 200);
  assert.ok(datos.fechaMinima >= reglasEntrega.sumarDias(reglasEntrega.hoy(), datos.diasAnticipacionMinima));
  assert.ok(datos.horarios.length > 0);
  assert.strictEqual(typeof datos.aceptaTransferencia, 'boolean');
});

prueba.test('cobertura: código postal sin envío', async function () {
  const respuesta = await fetch(base + '/entrega/cobertura/00001');
  const datos = await respuesta.json();
  assert.strictEqual(respuesta.status, 404);
  assert.strictEqual(datos.mensaje, 'Por el momento no tenemos envío a tu zona');
});

prueba.test('validar carrito: precio vigente, precio cambiado y producto que no existe', async function () {
  const plantilla = await opcionesDe(1);
  const r = await enviar('/carrito/validar', {
    articulos: [
      articulo(1, plantilla.opciones, 1, plantilla.precio, ''),
      articulo(1, plantilla.opciones, 1, 1, 'Otro mensaje'),
      articulo(999999, plantilla.opciones, 1, 10, '')
    ]
  });
  assert.strictEqual(r.estado, 200);
  assert.strictEqual(r.cuerpo.articulos[0].estado, 'disponible');
  assert.strictEqual(r.cuerpo.articulos[0].precioCambio, false);
  // Mismo postre con otro mensaje: la tabla DetallePedido no lo permite en un mismo pedido
  assert.strictEqual(r.cuerpo.articulos[1].estado, 'repetido');
  assert.strictEqual(r.cuerpo.articulos[2].estado, 'no_disponible');
  assert.strictEqual(r.cuerpo.puedeContinuar, false);
});

prueba.test('cliente nuevo, recoger en tienda: se guarda todo lo que el Panel necesita', async function () {
  const plantilla = await opcionesDe(1);
  const libre = await fechaConLugar(10, 1);
  const telefono = telefonoNuevo();
  const total = redondear(plantilla.precio * 2);
  const r = await enviar('/pedidos', solicitud([articulo(1, plantilla.opciones, 2, plantilla.precio, 'Feliz día')], telefono, libre.fecha, false, total));
  assert.strictEqual(r.estado, 201, JSON.stringify(r.cuerpo));
  const folio = r.cuerpo.folio;
  assert.ok(folio > 0);
  assert.strictEqual(r.cuerpo.montoTotal, total);
  assert.strictEqual(r.cuerpo.anticipoRequerido, reglasEntrega.calcularAnticipo(reglas, total, 'Anticipo'));

  const pedido = await baseDatos.consultar('SELECT * FROM Pedido WHERE Folio = ?', [folio]);
  assert.strictEqual(pedido[0].EstadoPedido, 'En proceso de aceptacion');
  assert.strictEqual(pedido[0].ModalidadEntrega, 'Recoger en tienda');
  assert.strictEqual(Number(pedido[0].MontoTotal), total);
  const detalle = await baseDatos.consultar('SELECT * FROM DetallePedido WHERE Folio = ?', [folio]);
  assert.strictEqual(detalle.length, 1);
  assert.strictEqual(detalle[0].IdPostre, 1);
  assert.strictEqual(Number(detalle[0].PrecioUnitario), plantilla.precio);
  assert.ok(Number(detalle[0].CostoUnitario) > 0);
  const historial = await baseDatos.consultar('SELECT * FROM HistorialEstado WHERE Folio = ?', [folio]);
  assert.strictEqual(historial.length, 1);
  assert.strictEqual(historial[0].IdUsuario, null);
  assert.strictEqual(historial[0].EstadoAnterior, null);
  const avisos = await baseDatos.consultar("SELECT * FROM Notificacion WHERE Folio = ? AND ResultadoEnvio = 'Pendiente'", [folio]);
  assert.strictEqual(avisos.length, 2);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM EntregaDomicilio WHERE Folio = ?', [folio]), 0);
});

prueba.test('cliente existente, envío a domicilio y postre personalizado reutilizado', async function () {
  if (codigoPostal === '') {
    throw new Error('Agrega un código postal a CodigoPostalCobertura para esta prueba');
  }
  const plantilla = await opcionesDe(1);
  // Se cambia la opción de la primera característica por otra disponible (postre personalizado)
  const caracteristica = plantilla.datos.caracteristicas[0];
  const opciones = plantilla.opciones.slice();
  let otra = null;
  for (let i = 0; i < caracteristica.opciones.length; i++) {
    let yaEsta = false;
    for (let j = 0; j < opciones.length; j++) {
      if (opciones[j].idOpcion === caracteristica.opciones[i].idOpcion) {
        yaEsta = true;
      }
    }
    if (!yaEsta && otra === null) {
      otra = caracteristica.opciones[i];
    }
  }
  for (let j = 0; j < opciones.length; j++) {
    for (let k = 0; k < caracteristica.opciones.length; k++) {
      if (opciones[j].idOpcion === caracteristica.opciones[k].idOpcion) {
        opciones[j] = { idOpcion: otra.idOpcion, cantidad: 1 };
      }
    }
  }
  const cotizacion = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: opciones, cantidad: 1 });
  assert.strictEqual(cotizacion.cuerpo.valido, true, JSON.stringify(cotizacion.cuerpo));
  const precio = cotizacion.cuerpo.precioUnitario;
  const telefono = telefonoNuevo();
  const libre = await fechaConLugar(12, 2);

  const primero = await enviar('/pedidos', solicitud([articulo(1, opciones, 1, precio, '')], telefono, libre.fecha, true, redondear(precio + costoEnvio)));
  assert.strictEqual(primero.estado, 201, JSON.stringify(primero.cuerpo));
  const postresAntes = await contar('SELECT COUNT(*) AS Total FROM Postre', []);

  // Segundo pedido del MISMO cliente con el MISMO postre personalizado (otro horario para no ser doble envío)
  const segundo = solicitud([articulo(1, opciones, 1, precio, '')], telefono, libre.fecha, true, redondear(precio + costoEnvio));
  segundo.entrega.horario = '15:00';
  segundo.cliente.nombre = 'Otro Nombre';
  const r = await enviar('/pedidos', segundo);
  assert.strictEqual(r.estado, 201, JSON.stringify(r.cuerpo));
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Postre', []), postresAntes);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Cliente WHERE TelefonoWhatsApp = ?', [telefono]), 1);
  // El nombre de un cliente que ya existe no se cambia desde la web
  assert.strictEqual(r.cuerpo.cliente.nombre, 'Cliente de Prueba');
  assert.strictEqual(r.cuerpo.costoEnvio, costoEnvio);

  const lineas = await baseDatos.consultar(
    'SELECT d.IdPostre, p.NombrePostre, p.MostrarEnTienda FROM DetallePedido d JOIN Postre p ON p.IdPostre = d.IdPostre WHERE d.Folio = ?',
    [r.cuerpo.folio]
  );
  assert.strictEqual(lineas[0].NombrePostre, null);
  assert.strictEqual(lineas[0].MostrarEnTienda, 0);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM EntregaDomicilio WHERE Folio = ?', [r.cuerpo.folio]), 1);
});

prueba.test('precio cambiado: no se guarda y regresa el total vigente', async function () {
  const plantilla = await opcionesDe(1);
  const libre = await fechaConLugar(14, 1);
  const pedidosAntes = await contar('SELECT COUNT(*) AS Total FROM Pedido', []);
  const r = await enviar('/pedidos', solicitud([articulo(1, plantilla.opciones, 1, 1, '')], telefonoNuevo(), libre.fecha, false, 1));
  assert.strictEqual(r.estado, 409);
  assert.strictEqual(r.cuerpo.codigo, 'PRECIO_CAMBIO');
  assert.strictEqual(r.cuerpo.detalles.total, plantilla.precio);
  assert.strictEqual(r.cuerpo.detalles.articulos[0].precioCambio, true);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Pedido', []), pedidosAntes);
});

prueba.test('producto no disponible: no se guarda', async function () {
  const plantilla = await opcionesDe(1);
  const libre = await fechaConLugar(14, 1);
  const r = await enviar('/pedidos', solicitud([articulo(999999, plantilla.opciones, 1, 10, '')], telefonoNuevo(), libre.fecha, false, 10));
  assert.strictEqual(r.estado, 409);
  assert.strictEqual(r.cuerpo.codigo, 'PRODUCTOS_NO_DISPONIBLES');
  assert.strictEqual(r.cuerpo.detalles[0].estado, 'no_disponible');
});

prueba.test('doble envío: misma llave o mismo pedido no crean duplicados', async function () {
  const plantilla = await opcionesDe(1);
  const libre = await fechaConLugar(16, 1);
  const datos = solicitud([articulo(1, plantilla.opciones, 1, plantilla.precio, '')], telefonoNuevo(), libre.fecha, false, plantilla.precio);
  const respuestas = await Promise.all([enviar('/pedidos', datos), enviar('/pedidos', datos)]);
  assert.strictEqual(respuestas[0].cuerpo.folio, respuestas[1].cuerpo.folio);
  // Otra llave, mismos datos (por ejemplo, la API se reinició): regresa el mismo folio
  const otraLlave = Object.assign({}, datos, { llaveIdempotencia: crypto.randomUUID() });
  const tercera = await enviar('/pedidos', otraLlave);
  assert.strictEqual(tercera.estado, 200);
  assert.strictEqual(tercera.cuerpo.repetido, true);
  assert.strictEqual(tercera.cuerpo.folio, respuestas[0].cuerpo.folio);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Pedido WHERE Folio = ?', [tercera.cuerpo.folio]), 1);
});

prueba.test('capacidad diaria llena: rechaza la fecha y deshace todo (rollback)', async function () {
  const plantilla = await opcionesDe(1);
  const libre = await fechaConLugar(30, reglas.capacidadDiaria);
  // Se llena el día
  for (let i = libre.carga; i < reglas.capacidadDiaria; i++) {
    const r = await enviar('/pedidos', solicitud([articulo(1, plantilla.opciones, 1, plantilla.precio, '')], telefonoNuevo(), libre.fecha, false, plantilla.precio));
    assert.strictEqual(r.estado, 201, JSON.stringify(r.cuerpo));
  }
  // Pedido con un postre NUEVO y un cliente NUEVO: al fallar la fecha no debe quedar nada guardado
  const opciones = plantilla.opciones.slice();
  const ultima = plantilla.datos.caracteristicas[plantilla.datos.caracteristicas.length - 1];
  const extra = { idOpcion: ultima.opciones[ultima.opciones.length - 1].idOpcion, cantidad: 1 };
  let yaEsta = false;
  for (let i = 0; i < opciones.length; i++) {
    if (opciones[i].idOpcion === extra.idOpcion) {
      yaEsta = true;
    }
  }
  if (!yaEsta) {
    opciones.push(extra);
  }
  const cotizacion = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: opciones, cantidad: 1 });
  const telefono = telefonoNuevo();
  const postresAntes = await contar('SELECT COUNT(*) AS Total FROM Postre', []);
  const r = await enviar('/pedidos', solicitud([articulo(1, opciones, 1, cotizacion.cuerpo.precioUnitario, '')], telefono, libre.fecha, false, cotizacion.cuerpo.precioUnitario));
  assert.strictEqual(r.estado, 400, JSON.stringify(r.cuerpo));
  assert.match(r.cuerpo.mensaje, /Ya no tenemos cupo/);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Postre', []), postresAntes);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Cliente WHERE TelefonoWhatsApp = ?', [telefono]), 0);
  // El día lleno ya no se ofrece en el calendario
  const reglasWeb = await (await fetch(base + '/entrega/reglas')).json();
  assert.ok(reglasWeb.fechasLlenas.indexOf(libre.fecha) >= 0);
});

prueba.test('seguimiento con folio y teléfono o correo, y comprobante solo después de aprobar', async function () {
  const plantilla = await opcionesDe(1);
  const libre = await fechaConLugar(18, 1);
  const telefono = telefonoNuevo();
  const r = await enviar('/pedidos', solicitud([articulo(1, plantilla.opciones, 1, plantilla.precio, '')], telefono, libre.fecha, false, plantilla.precio));
  const folio = r.cuerpo.folio;

  const consulta = await enviar('/pedidos/consulta', { folio: String(folio), telefono: telefono });
  assert.strictEqual(consulta.estado, 200);
  assert.strictEqual(consulta.cuerpo.pasos.length, 6);
  assert.strictEqual(consulta.cuerpo.pasos[0].actual, true);
  assert.strictEqual(consulta.cuerpo.saldo, plantilla.precio);
  assert.strictEqual(consulta.cuerpo.puedeEnviarComprobante, false);

  // También se encuentra con el correo (sin importar mayúsculas)
  const conCorreo = await enviar('/pedidos/consulta', { folio: String(folio), correo: 'Prueba@Correo.com' });
  assert.strictEqual(conCorreo.estado, 200, JSON.stringify(conCorreo.cuerpo));
  assert.strictEqual(conCorreo.cuerpo.folio, folio);
  // Correo mal escrito: error de formato (no gasta intentos)
  const correoMalo = await enviar('/pedidos/consulta', { folio: String(folio), correo: 'prueba@' });
  assert.strictEqual(correoMalo.estado, 400);

  const comprobante = {
    folio: folio,
    telefono: telefono,
    monto: 10,
    metodo: 'Transferencia',
    referencia: 'PRUEBA-' + folio,
    linkComprobante: 'https://ejemplo.com/comprobante.jpg'
  };
  const sinAprobar = await enviar('/pedidos/comprobante', comprobante);
  assert.strictEqual(sinAprobar.estado, 409);
  assert.strictEqual(await contar('SELECT COUNT(*) AS Total FROM Pago WHERE Folio = ?', [folio]), 0);
});

// RF-13: foto del comprobante subida desde los archivos del cliente (necesita la PARTE 7 de MIGRACION_COMPLETA.sql)
// Va antes de la prueba de bloqueo: después de 12 intentos fallidos la API frena las consultas y los comprobantes.
prueba.test('comprobante con foto: se guarda el archivo, se rechazan depósito y archivos falsos', async function (contexto) {
  // Un pedido aprobado de la semilla 4 que todavía acepte comprobantes
  const candidatos = await baseDatos.consultar(
    'SELECT p.Folio, c.TelefonoWhatsApp FROM Pedido p JOIN Cliente c ON c.IdCliente = p.IdCliente ' +
      "WHERE p.EstadoPedido = 'Aprobado' ORDER BY p.Folio"
  );
  let elegido = null;
  for (const fila of candidatos) {
    const consulta = await enviar('/pedidos/consulta', { folio: String(fila.Folio), telefono: fila.TelefonoWhatsApp });
    if (consulta.estado === 200 && consulta.cuerpo.puedeEnviarComprobante && consulta.cuerpo.saldo - consulta.cuerpo.porValidar >= 1) {
      elegido = { folio: fila.Folio, telefono: fila.TelefonoWhatsApp };
      break;
    }
  }
  if (elegido === null) {
    contexto.skip('No hay un pedido aprobado que acepte comprobantes (ejecuta la semilla 4)');
    return;
  }
  const png = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
  const referencia = 'FOTO-' + crypto.randomUUID().substring(0, 8);
  const datos = {
    folio: elegido.folio,
    telefono: elegido.telefono,
    monto: 1,
    metodo: 'Transferencia',
    referencia: referencia,
    archivo: { nombre: 'comprobante.png', tipo: 'image/png', contenidoBase64: png }
  };

  // Depósito ya no se acepta
  const deposito = await enviar('/pedidos/comprobante', Object.assign({}, datos, { metodo: 'Deposito' }));
  assert.strictEqual(deposito.estado, 400);
  // Un archivo que no es imagen ni PDF
  const falso = await enviar('/pedidos/comprobante', Object.assign({}, datos, {
    archivo: { nombre: 'foto.jpg', tipo: 'image/jpeg', contenidoBase64: Buffer.from('MZ programa').toString('base64') }
  }));
  assert.strictEqual(falso.estado, 400);
  assert.strictEqual(falso.cuerpo.detalles[0].campo, 'archivo');

  // Foto válida: queda el pago «Pendiente» sin enlace y con su archivo
  const correcto = await enviar('/pedidos/comprobante', datos);
  assert.strictEqual(correcto.estado, 201);
  const pagos = await baseDatos.consultar(
    'SELECT IdPago, LinkComprobante, EstadoValidacion FROM Pago WHERE Referencia = ?',
    [referencia]
  );
  assert.strictEqual(pagos.length, 1);
  assert.strictEqual(pagos[0].LinkComprobante, null);
  assert.strictEqual(pagos[0].EstadoValidacion, 'Pendiente');
});

prueba.test('seguimiento: datos que no coinciden y bloqueo tras 12 intentos', async function () {
  let ultima = null;
  for (let i = 0; i < 13; i++) {
    ultima = await enviar('/pedidos/consulta', { folio: '1', telefono: '0000000000' });
    if (i < 12) {
      assert.strictEqual(ultima.estado, 404);
      assert.strictEqual(ultima.cuerpo.mensaje, 'No encontramos un pedido con esos datos');
    }
  }
  assert.strictEqual(ultima.estado, 429);
  assert.match(ultima.cuerpo.mensaje, /Espera \d+ segundos/);
  assert.ok(ultima.cuerpo.detalles.segundosEspera <= 30);
});
