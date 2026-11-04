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

// ===================== Capas del pastel (PostreOpcion.NumeroCapa) =====================

prueba.test('por capas: cada capa paga su parte del sabor y la decoración se suma', function () {
  const chocolate = opcion(5, 2, 'Chocolate', 94.5, true, 1, 1, 1);
  // Base vainilla y capa 2 chocolate: cada sabor paga la mitad
  const elegidas = [
    { opcion: chico, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 1 },
    { opcion: chocolate, cantidad: 1, capa: 2 },
    { opcion: flores, cantidad: 1, capa: 1 },
    { opcion: flores, cantidad: 1, capa: 2 }
  ];
  // 138 + (15.3 + 94.5) / 2 + 52 + 52 = 296.9
  assert.strictEqual(reglas.calcularPrecio(elegidas, caracteristicas), 296.9);
  // El mismo sabor en las dos capas cuesta lo mismo que un pastel de una capa
  const igual = [
    { opcion: chico, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 2 }
  ];
  assert.strictEqual(reglas.calcularPrecio(igual, caracteristicas), reglas.calcularPrecio([igual[0], igual[1]], caracteristicas));
});

prueba.test('por capas: selección única, presupuesto y exclusiones se revisan en cada capa', function () {
  const chocolate = opcion(5, 2, 'Chocolate', 94.5, true, 1, 1, 1);
  // Un sabor por capa es correcto
  const bien = reglas.validar(caracteristicas, exclusiones, [
    { opcion: chico, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 1 },
    { opcion: tresLeches, cantidad: 1, capa: 2 }
  ]);
  assert.strictEqual(bien.length, 0, JSON.stringify(bien));
  // Dos sabores en la capa 2 no
  const dos = reglas.validar(caracteristicas, [], [
    { opcion: chico, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 2 },
    { opcion: chocolate, cantidad: 1, capa: 2 }
  ]);
  assert.strictEqual(dos.length, 1);
  assert.strictEqual(dos[0].tipo, 'seleccionUnica');
  assert.strictEqual(dos[0].capa, 2);
  assert.match(dos[0].mensaje, /de la capa 2/);
  // Tres leches y vainilla en la MISMA capa sí chocan
  const chocan = reglas.validar(caracteristicas, exclusiones, [
    { opcion: chico, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 2 },
    { opcion: tresLeches, cantidad: 1, capa: 2 }
  ]);
  let hayExclusion = false;
  for (let i = 0; i < chocan.length; i++) {
    if (chocan[i].tipo === 'exclusion') {
      hayExclusion = true;
    }
  }
  assert.ok(hayExclusion);
  // El presupuesto de decoración es por capa: 2 listones en una capa (4 puntos) se pasa de 3
  const presupuesto = reglas.validar(caracteristicas, [], [
    { opcion: chico, cantidad: 1, capa: 1 },
    { opcion: vainilla, cantidad: 1, capa: 1 },
    { opcion: liston, cantidad: 1, capa: 1 },
    { opcion: liston, cantidad: 2, capa: 2 }
  ]);
  assert.strictEqual(presupuesto.length, 1);
  assert.strictEqual(presupuesto[0].tipo, 'presupuesto');
  assert.strictEqual(presupuesto[0].capa, 2);
});
