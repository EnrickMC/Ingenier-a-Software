/*
 * Pruebas de las reglas para armar un postre (no necesitan base de datos).
 * Ejecutar con:  npm test
 */
const prueba = require('node:test');
const assert = require('node:assert');
const reglas = require('../src/modulos/catalogo/reglasConfiguracion');

// Datos de ejemplo parecidos a la semilla del Módulo 3
const tamano = { idCaracteristica: 1, nombre: 'Tamaño', obligatoria: true, seleccionMultiple: false, presupuestoSeleccion: 1, disponible: true };
const sabor = { idCaracteristica: 2, nombre: 'Sabor', obligatoria: true, seleccionMultiple: false, presupuestoSeleccion: 1, disponible: true };
const decoracion = { idCaracteristica: 4, nombre: 'Decoración', obligatoria: false, seleccionMultiple: true, presupuestoSeleccion: 3, disponible: true };
const caracteristicas = [tamano, sabor, decoracion];

function opcion(idOpcion, idCaracteristica, nombre, precioReferencia, seEscala, multIngredientes, puntos, multPresupuesto) {
  return {
    idOpcion: idOpcion,
    idCaracteristica: idCaracteristica,
    nombre: nombre,
    disponible: true,
    precioReferencia: precioReferencia,
    seEscala: seEscala,
    multiplicadorIngredientes: multIngredientes,
    puntosSeleccion: puntos,
    multiplicadorPresupuesto: multPresupuesto
  };
}

const chico = opcion(1, 1, 'Chico', 138, false, 1, 1, 1);
const grande = opcion(3, 1, 'Grande', 276, false, 2, 1, 2);
const vainilla = opcion(4, 2, 'Vainilla', 15.3, true, 1, 1, 1);
const tresLeches = opcion(6, 2, 'Tres leches', 207.375, true, 1, 1, 1);
const liston = opcion(11, 4, 'Listón', 24, false, 1, 2, 1);
const flores = opcion(12, 4, 'Flores de azúcar', 52, true, 1, 1, 1);

const exclusiones = [{ idOpcion: 6, idOpcionExcluida: 4, nombreOpcion: 'Tres leches', nombreOpcionExcluida: 'Vainilla', motivo: 'Prueba' }];

prueba.test('una configuración correcta no tiene errores', function () {
  const errores = reglas.validar(caracteristicas, exclusiones, [{ opcion: chico, cantidad: 1 }, { opcion: vainilla, cantidad: 1 }]);
  assert.strictEqual(errores.length, 0);
});

prueba.test('falta una característica obligatoria', function () {
  const errores = reglas.validar(caracteristicas, [], [{ opcion: chico, cantidad: 1 }]);
  assert.strictEqual(errores.length, 1);
  assert.strictEqual(errores[0].tipo, 'obligatoria');
  assert.strictEqual(errores[0].idCaracteristica, 2);
});

prueba.test('selección única no acepta dos opciones', function () {
  const errores = reglas.validar(caracteristicas, [], [{ opcion: chico, cantidad: 1 }, { opcion: grande, cantidad: 1 }, { opcion: vainilla, cantidad: 1 }]);
  assert.strictEqual(errores[0].tipo, 'seleccionUnica');
});

prueba.test('el presupuesto se excede con tamaño chico y se amplía con tamaño grande', function () {
  // Listón (2 puntos) × 2 = 4 puntos > 3 disponibles
  const conChico = reglas.validar(caracteristicas, [], [{ opcion: chico, cantidad: 1 }, { opcion: vainilla, cantidad: 1 }, { opcion: liston, cantidad: 2 }]);
  assert.strictEqual(conChico.length, 1);
  assert.strictEqual(conChico[0].tipo, 'presupuesto');
  assert.match(conChico[0].mensaje, /usa 4 de 3 puntos/);
  // Grande multiplica el presupuesto × 2 = 6 puntos
  const conGrande = reglas.validar(caracteristicas, [], [{ opcion: grande, cantidad: 1 }, { opcion: vainilla, cantidad: 1 }, { opcion: liston, cantidad: 2 }]);
  assert.strictEqual(conGrande.length, 0);
});

prueba.test('detecta opciones que se excluyen', function () {
  const errores = reglas.validar(caracteristicas, exclusiones, [{ opcion: chico, cantidad: 1 }, { opcion: tresLeches, cantidad: 1 }, { opcion: vainilla, cantidad: 1 }]);
  let hayExclusion = false;
  for (let i = 0; i < errores.length; i++) {
    if (errores[i].tipo === 'exclusion') {
      hayExclusion = true;
    }
  }
  assert.ok(hayExclusion);
});

prueba.test('opción no disponible', function () {
  const agotada = opcion(13, 4, 'Velas', 5, false, 1, 1, 1);
  agotada.disponible = false;
  const errores = reglas.validar(caracteristicas, [], [{ opcion: chico, cantidad: 1 }, { opcion: vainilla, cantidad: 1 }, { opcion: agotada, cantidad: 1 }]);
  assert.strictEqual(errores[0].tipo, 'disponibilidad');
});

prueba.test('el precio sigue la misma cuenta que vw_CostoPrecioPostre', function () {
  // Pastel tres leches de fiesta de la semilla: Grande + Tres leches + Cajeta + Listón + 2 Flores = 1102.75
  const cajeta = opcion(9, 3, 'Cajeta', 90, true, 1, 1, 1);
  const elegidas = [
    { opcion: grande, cantidad: 1 },
    { opcion: tresLeches, cantidad: 1 },
    { opcion: cajeta, cantidad: 1 },
    { opcion: liston, cantidad: 1 },
    { opcion: flores, cantidad: 2 }
  ];
  // 276 + (207.375 + 90 + 52×2) × 2 + 24 = 1102.75
  assert.strictEqual(reglas.calcularPrecio(elegidas), 1102.75);
});
