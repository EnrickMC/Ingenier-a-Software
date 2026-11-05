/*
 * Pruebas de la API contra una base de datos real (la del archivo .env).
 * Requisitos: base CARMIR_DB con la semilla del Módulo 3 y 5, y la del Módulo 1
 * (semilla_modulo1_catalogo_publico.sql), que deja visibles los productos.
 * Ejecutar con:  npm test
 */
const prueba = require('node:test');
const assert = require('node:assert');
const crearApp = require('../src/app');
const baseDatos = require('../src/config/baseDatos');

let servidor = null;
let base = '';

prueba.before(async function () {
  const app = crearApp();
  servidor = app.listen(0, '127.0.0.1');
  await new Promise(function (resolver) {
    servidor.on('listening', resolver);
  });
  base = 'http://127.0.0.1:' + servidor.address().port + '/api';
});

prueba.after(async function () {
  servidor.close();
  await baseDatos.cerrar();
});

async function obtener(ruta) {
  const respuesta = await fetch(base + ruta);
  const cuerpo = await respuesta.json();
  return { estado: respuesta.status, cuerpo: cuerpo };
}

async function enviar(ruta, datos) {
  const respuesta = await fetch(base + ruta, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(datos)
  });
  const cuerpo = await respuesta.json();
  return { estado: respuesta.status, cuerpo: cuerpo };
}

prueba.test('salud: la base de datos responde', async function () {
  const r = await obtener('/salud');
  assert.strictEqual(r.estado, 200);
  assert.strictEqual(r.cuerpo.baseDatos, 'conectada');
  assert.ok(r.cuerpo.productosVisibles >= 1);
});

prueba.test('categorías activas con productos', async function () {
  const r = await obtener('/catalogo/categorias');
  assert.strictEqual(r.estado, 200);
  assert.ok(r.cuerpo.categorias.length >= 2);
});

prueba.test('productos visibles, con precio y ordenados', async function () {
  const r = await obtener('/catalogo/productos?orden=precio_asc');
  assert.strictEqual(r.estado, 200);
  const productos = r.cuerpo.productos;
  assert.ok(productos.length >= 3);
  for (let i = 0; i < productos.length; i++) {
    assert.ok(productos[i].precio > 0);
    if (i > 0) {
      assert.ok(productos[i].precio >= productos[i - 1].precio);
    }
  }
});

prueba.test('búsqueda sin resultados y comodines tratados como texto', async function () {
  const r = await obtener('/catalogo/productos?buscar=%25');
  assert.strictEqual(r.estado, 200);
  assert.strictEqual(r.cuerpo.productos.length, 0);
});

prueba.test('categoría inexistente', async function () {
  const r = await obtener('/catalogo/productos?categoria=9999');
  assert.strictEqual(r.estado, 404);
  assert.strictEqual(r.cuerpo.mensaje, 'La categoría seleccionada ya no está disponible');
});

prueba.test('datos inválidos en la URL', async function () {
  const r1 = await obtener('/catalogo/productos?orden=nombre;DROP');
  assert.strictEqual(r1.estado, 400);
  const r2 = await obtener('/catalogo/productos/abc');
  assert.strictEqual(r2.estado, 400);
  const r3 = await obtener('/catalogo/productos?categoria=1&categoria=2');
  assert.strictEqual(r3.estado, 400);
});

prueba.test('producto inexistente', async function () {
  const r = await obtener('/catalogo/productos/999999');
  assert.strictEqual(r.estado, 404);
  assert.strictEqual(r.cuerpo.mensaje, 'Este producto ya no está disponible');
});

prueba.test('ficha con ingredientes y opciones incluidas', async function () {
  const lista = await obtener('/catalogo/productos');
  const id = lista.cuerpo.productos[0].idPostre;
  const r = await obtener('/catalogo/productos/' + id);
  assert.strictEqual(r.estado, 200);
  assert.ok(r.cuerpo.incluye.length > 0);
  assert.ok(Array.isArray(r.cuerpo.ingredientes));
  assert.strictEqual(typeof r.cuerpo.sePuedePersonalizar, 'boolean');
});

prueba.test('personalización: la plantilla precarga sus opciones y el costo no se expone', async function () {
  const lista = await obtener('/catalogo/productos');
  const producto = lista.cuerpo.productos[0];
  const r = await obtener('/catalogo/productos/' + producto.idPostre + '/personalizacion');
  assert.strictEqual(r.estado, 200);
  assert.ok(r.cuerpo.seleccionInicial.length > 0);
  const texto = JSON.stringify(r.cuerpo);
  assert.ok(texto.indexOf('CostoReceta') < 0);
  assert.ok(texto.indexOf('costo') < 0);

  // Cotizar la selección inicial da el mismo precio que el catálogo (vw_CostoPrecioPostre)
  const c = await enviar('/catalogo/cotizar', { idPostre: producto.idPostre, opciones: r.cuerpo.seleccionInicial, cantidad: 1 });
  assert.strictEqual(c.estado, 200);
  assert.strictEqual(c.cuerpo.valido, true);
  assert.strictEqual(c.cuerpo.precioUnitario, producto.precio);
});

prueba.test('galería de plantillas de la misma categoría', async function () {
  const lista = await obtener('/catalogo/productos');
  const producto = lista.cuerpo.productos[0];
  const r = await obtener('/catalogo/productos/' + producto.idPostre + '/plantillas');
  assert.strictEqual(r.estado, 200);
  for (let i = 0; i < r.cuerpo.plantillas.length; i++) {
    assert.strictEqual(r.cuerpo.plantillas[i].idCategoria, producto.idCategoria);
    assert.ok(r.cuerpo.plantillas[i].opciones.length > 0);
  }
});

prueba.test('cotizar rechaza datos mal formados', async function () {
  const r1 = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: 'x' });
  assert.strictEqual(r1.estado, 400);
  const r2 = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: [{ idOpcion: 1, cantidad: 1 }, { idOpcion: 1, cantidad: 1 }] });
  assert.strictEqual(r2.estado, 400);
  const r3 = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: [], cantidad: 50 });
  assert.strictEqual(r3.estado, 400);
  const r4 = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: [], dedicatoria: '<script>' });
  assert.strictEqual(r4.estado, 400);
  // La misma opción en la misma capa también se rechaza; la capa debe ser un número válido
  const r5 = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: [{ idOpcion: 1, cantidad: 1, capa: 2 }, { idOpcion: 1, cantidad: 1, capa: 2 }] });
  assert.strictEqual(r5.estado, 400);
  const r6 = await enviar('/catalogo/cotizar', { idPostre: 1, opciones: [{ idOpcion: 1, cantidad: 1, capa: 0 }] });
  assert.strictEqual(r6.estado, 400);
});

prueba.test('la conexión es de solo lectura', async function () {
  let error = null;
  try {
    await baseDatos.consultar('UPDATE CategoriaPostre SET NombreCategoria = NombreCategoria WHERE IdCategoria = ?', [1]);
  } catch (e) {
    error = e;
  }
  assert.notStrictEqual(error, null);
});

prueba.test('ruta inexistente', async function () {
  const r = await obtener('/no-existe');
  assert.strictEqual(r.estado, 404);
});
