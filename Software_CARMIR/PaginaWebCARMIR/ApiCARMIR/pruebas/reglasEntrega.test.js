/*
 * Pruebas de las reglas del Portal de Pedidos que no necesitan base de datos:
 * fechas, horarios, días laborables, anticipo, clave de configuración y validación de datos.
 * Ejecutar con:  npm test
 */
const prueba = require('node:test');
const assert = require('node:assert');
const reglasEntrega = require('../src/modulos/pedidos/reglasEntrega');
const validacionPedido = require('../src/modulos/pedidos/pedidos.validacion');
const mensajes = require('../src/modulos/pedidos/mensajesPedido');

// Reglas de fábrica (las mismas de MIGRACION_COMPLETA.sql)
function reglasDeFabrica() {
  return reglasEntrega.armarReglas([]);
}

// Primer día laborable a partir de "desde" (incluido)
function siguienteLaborable(reglas, desde) {
  let fecha = desde;
  while (!reglasEntrega.esDiaLaborable(reglas, fecha)) {
    fecha = reglasEntrega.sumarDias(fecha, 1);
  }
  return fecha;
}

prueba.test('reglas de fábrica: 3 días, lunes a sábado, 09:00 a 19:00, anticipo 50 %', function () {
  const reglas = reglasDeFabrica();
  assert.strictEqual(reglas.diasAnticipacionMinima, 3);
  assert.deepStrictEqual(reglas.diasLaborables, ['L', 'M', 'X', 'J', 'V', 'S']);
  assert.strictEqual(reglas.horaApertura, '09:00');
  assert.strictEqual(reglas.horaCierre, '19:00');
  assert.strictEqual(reglas.porcentajeAnticipo, 50);
  assert.strictEqual(reglasEntrega.textoDiasLaborables(reglas), 'lunes a sábado');
});

prueba.test('valores inválidos en Configuracion se cambian por los de fábrica', function () {
  const reglas = reglasEntrega.armarReglas([
    { Clave: 'HoraApertura', Valor: '25:00' },
    { Clave: 'PorcentajeAnticipo', Valor: '150' },
    { Clave: 'DiasLaborables', Valor: 'Q,W' },
    { Clave: 'CapacidadDiaria', Valor: 'muchos' }
  ]);
  assert.strictEqual(reglas.horaApertura, '09:00');
  assert.strictEqual(reglas.porcentajeAnticipo, 50);
  assert.strictEqual(reglas.diasLaborables.length, 6);
  assert.strictEqual(reglas.capacidadDiaria, 6);
});

prueba.test('horarios cada 30 minutos entre la apertura y el cierre', function () {
  const reglas = reglasEntrega.armarReglas([
    { Clave: 'HoraApertura', Valor: '10:00' },
    { Clave: 'HoraCierre', Valor: '12:00' }
  ]);
  assert.deepStrictEqual(reglasEntrega.horariosDisponibles(reglas), ['10:00', '10:30', '11:00', '11:30', '12:00']);
});

prueba.test('fecha antes de la anticipación mínima: «Elige una fecha a partir del…»', function () {
  const reglas = reglasDeFabrica();
  const manana = reglasEntrega.sumarDias(reglasEntrega.hoy(), 1);
  const problema = reglasEntrega.revisarFechaYHorario(reglas, manana, '12:00', 0);
  assert.strictEqual(problema.campo, 'fecha');
  assert.match(problema.mensaje, /^Elige una fecha a partir del /);
});

prueba.test('domingo no es laborable', function () {
  const reglas = reglasDeFabrica();
  let fecha = reglasEntrega.sumarDias(reglasEntrega.hoy(), 7);
  while (reglasEntrega.textoAFecha(fecha).getDay() !== 0) {
    fecha = reglasEntrega.sumarDias(fecha, 1);
  }
  const problema = reglasEntrega.revisarFechaYHorario(reglas, fecha, '12:00', 0);
  assert.strictEqual(problema.campo, 'fecha');
});

prueba.test('día lleno (capacidad diaria) y horario fuera de atención', function () {
  const reglas = reglasDeFabrica();
  const fecha = siguienteLaborable(reglas, reglasEntrega.sumarDias(reglasEntrega.hoy(), 5));
  assert.strictEqual(reglasEntrega.revisarFechaYHorario(reglas, fecha, '12:00', 0), null);
  assert.match(reglasEntrega.revisarFechaYHorario(reglas, fecha, '12:00', 6).mensaje, /Ya no tenemos cupo/);
  assert.strictEqual(reglasEntrega.revisarFechaYHorario(reglas, fecha, '20:00', 0).campo, 'horario');
  assert.strictEqual(reglasEntrega.revisarFechaYHorario(reglas, fecha, '12:15', 0).campo, 'horario');
});

prueba.test('fechas que no existen se rechazan', function () {
  assert.strictEqual(reglasEntrega.textoAFecha('2026-02-31'), null);
  assert.strictEqual(reglasEntrega.textoAFecha('2026-13-01'), null);
  assert.notStrictEqual(reglasEntrega.textoAFecha('2028-02-29'), null);
});

prueba.test('anticipo: 50 % del total o el total completo', function () {
  const reglas = reglasDeFabrica();
  assert.strictEqual(reglasEntrega.calcularAnticipo(reglas, 891.95, 'Anticipo'), 445.98);
  assert.strictEqual(reglasEntrega.calcularAnticipo(reglas, 891.95, 'Pago total'), 891.95);
});

prueba.test('transferencia solo con datos bancarios', function () {
  assert.strictEqual(reglasEntrega.aceptaTransferencia(reglasDeFabrica()), false);
  const conBanco = reglasEntrega.armarReglas([{ Clave: 'DatosBancarios', Valor: 'CLABE 0123' }]);
  assert.strictEqual(reglasEntrega.aceptaTransferencia(conBanco), true);
});

prueba.test('instrucciones de pago en sucursal y por transferencia', function () {
  const conBanco = reglasEntrega.armarReglas([{ Clave: 'DatosBancarios', Valor: 'CLABE 0123' }]);
  const efectivo = mensajes.instruccionesPago(conBanco, 'Efectivo', 'Anticipo', 100, 50);
  assert.strictEqual(efectivo.datosBancarios, '');
  assert.match(efectivo.pasos.join(' '), /\$50\.00/);
  const transferencia = mensajes.instruccionesPago(conBanco, 'Transferencia', 'Pago total', 1234.5, 617.25);
  assert.strictEqual(transferencia.datosBancarios, 'CLABE 0123');
  assert.match(transferencia.pasos.join(' '), /\$1,234\.50/);
});

prueba.test('datos de contacto: teléfono de 10 dígitos, correo válido y nombre sin símbolos', function () {
  assert.throws(function () {
    validacionPedido.leerConsulta({ folio: '12', telefono: '442-123-4567' });
  }, /10 dígitos/);
  assert.throws(function () {
    validacionPedido.leerConsulta({ folio: 'abc', telefono: '4421234567' });
  }, /folio/);
  const consulta = validacionPedido.leerConsulta({ folio: '#12', telefono: '4421234567' });
  assert.strictEqual(consulta.folio, 12);
});

prueba.test('comprobante: enlace http(s) y referencia obligatoria', function () {
  const base = { folio: 1, telefono: '4421234567', monto: 100, metodo: 'Transferencia', referencia: 'ABC', linkComprobante: 'https://x.com/a.jpg' };
  assert.strictEqual(validacionPedido.leerComprobante(base).monto, 100);
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { linkComprobante: 'javascript:alert(1)' }));
  }, /http/);
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { referencia: '' }));
  }, /referencia/);
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { metodo: 'Efectivo' }));
  }, /transferencia/);
  // «Depósito» ya no se acepta desde la página
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { metodo: 'Deposito' }));
  }, /transferencia/);
  // Sin enlace ni archivo
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { linkComprobante: '' }));
  }, /Sube la foto/);
});

// PNG mínimo de 1x1 píxel
const PNG_BASE64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

prueba.test('comprobante: archivo de foto o PDF desde el equipo del cliente', function () {
  const base = { folio: 1, telefono: '4421234567', monto: 100, referencia: 'ABC' };
  // Solo archivo (sin enlace): el enlace queda null y el método por omisión es transferencia
  const conArchivo = validacionPedido.leerComprobante(
    Object.assign({}, base, { archivo: { nombre: 'C:\\fotos\\mi recibo ñ.png', tipo: 'image/png', contenidoBase64: PNG_BASE64 } })
  );
  assert.strictEqual(conArchivo.linkComprobante, null);
  assert.strictEqual(conArchivo.metodo, 'Transferencia');
  assert.strictEqual(conArchivo.archivo.tipo, 'image/png');
  assert.strictEqual(conArchivo.archivo.nombre, 'mi_recibo_n.png');
  // PDF
  const pdf = Buffer.from('%PDF-1.4\n%prueba\n').toString('base64');
  assert.strictEqual(validacionPedido.leerComprobante(Object.assign({}, base, { archivo: { nombre: 'a.pdf', contenidoBase64: pdf } })).archivo.tipo, 'application/pdf');
  // Un programa renombrado como .jpg se rechaza por su contenido
  const programa = Buffer.from('MZ esto no es una imagen').toString('base64');
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { archivo: { nombre: 'foto.jpg', tipo: 'image/jpeg', contenidoBase64: programa } }));
  }, /JPG, PNG o WEBP/);
  // Más de 5 MB
  const grande = Buffer.alloc(5 * 1024 * 1024 + 10);
  grande[0] = 0xff;
  grande[1] = 0xd8;
  grande[2] = 0xff;
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { archivo: { nombre: 'g.jpg', contenidoBase64: grande.toString('base64') } }));
  }, /5 MB/);
  // Texto que no es Base64
  assert.throws(function () {
    validacionPedido.leerComprobante(Object.assign({}, base, { archivo: { nombre: 'x.png', contenidoBase64: '<script>' } }));
  }, /no es válido/);
});
