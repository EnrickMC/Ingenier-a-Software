/*
 * Pruebas de los enlaces de fotos (archivo de la computadora o enlace de internet).
 * Ejecutar con:  npm test
 */
const prueba = require('node:test');
const assert = require('node:assert');
const path = require('path');
const enlaceImagen = require('../src/utilidades/enlaceImagen');

prueba('Un enlace de internet se manda tal cual', function () {
  assert.strictEqual(enlaceImagen.enlaceParaWeb('https://sitio.mx/pastel.jpg', 'postre', 3), 'https://sitio.mx/pastel.jpg');
});

prueba('Sin foto se manda null', function () {
  assert.strictEqual(enlaceImagen.enlaceParaWeb(null, 'postre', 3), null);
  assert.strictEqual(enlaceImagen.enlaceParaWeb('   ', 'postre', 3), null);
});

prueba('Un archivo de la computadora se sirve desde la API', function () {
  assert.strictEqual(enlaceImagen.enlaceParaWeb('C:\\Fotos\\pastel.jpg', 'postre', 3), '/api/imagenes/postre/3');
  assert.strictEqual(enlaceImagen.enlaceParaWeb('C:\\Fotos\\fresa.png', 'opcion', 8), '/api/imagenes/opcion/8');
});

prueba('Solo se aceptan rutas absolutas con formato de imagen', function () {
  const absoluta = path.resolve('fotos', 'pastel.JPG');
  assert.strictEqual(enlaceImagen.rutaArchivo(absoluta), absoluta);
  assert.strictEqual(enlaceImagen.rutaArchivo('/etc/passwd'), null);
  assert.strictEqual(enlaceImagen.rutaArchivo('fotos/pastel.jpg'), null);
  assert.strictEqual(enlaceImagen.rutaArchivo('https://sitio.mx/pastel.jpg'), null);
  assert.strictEqual(enlaceImagen.rutaArchivo(path.resolve('notas.txt')), null);
});
