/*
 * Reglas para armar un postre (equivalente a ConfiguracionPostre.java del Panel).
 * La página web tiene una copia en Angular para calcular el precio en vivo;
 * esta versión del servidor es la que decide al final (RF-3, paso 7).
 *
 * Datos que usa:
 *  - caracteristicas: [{ idCaracteristica, nombre, obligatoria, seleccionMultiple, presupuestoSeleccion, disponible }]
 *  - exclusiones:     [{ idOpcion, idOpcionExcluida, nombreOpcion, nombreOpcionExcluida, motivo }]
 *  - elegidas:        [{ opcion: {...}, cantidad, capa }]   (capa: 1 = Base; si no viene, 1)
 *    donde opcion = { idOpcion, idCaracteristica, nombre, disponible, puntosSeleccion,
 *                     multiplicadorPresupuesto, multiplicadorIngredientes, seEscala, precioReferencia }
 *    precioReferencia = CostoReceta × MultiplicadorCosto (precio de la opción sin escalar)
 *
 * Capas (PostreOpcion.NumeroCapa): en un pastel de varios pisos el Sabor, la Cobertura, el Color
 * y la Decoración se eligen por capa. Una característica es «por capas» si alguna de sus opciones
 * trae capa 2 o más. Así:
 *  - Cada capa revisa por separado la selección única, el presupuesto y las exclusiones.
 *  - Precio: en las de selección única cada capa paga su parte (1 / número de capas con esa
 *    característica); en las de selección múltiple se suma lo de cada capa.
 *    Es la misma cuenta que la vista vw_CostoPrecioPostre (Parte 9 de la migración).
 */

const MARGEN = 0.0001;

// Busca una característica por id; null si no está
function buscarCaracteristica(caracteristicas, idCaracteristica) {
  for (let i = 0; i < caracteristicas.length; i++) {
    if (caracteristicas[i].idCaracteristica === idCaracteristica) {
      return caracteristicas[i];
    }
  }
  return null;
}

// Capa de una opción elegida (sin capa = 1, la Base)
function capaDe(elegida) {
  if (elegida.capa === undefined || elegida.capa === null || elegida.capa < 1) {
    return 1;
  }
  return elegida.capa;
}

// Características que se eligen por capas: las que tienen alguna opción en la capa 2 o más
function caracteristicasPorCapas(elegidas) {
  const ids = [];
  for (let i = 0; i < elegidas.length; i++) {
    const id = elegidas[i].opcion.idCaracteristica;
    if (capaDe(elegidas[i]) > 1 && ids.indexOf(id) < 0) {
      ids.push(id);
    }
  }
  return ids;
}

// Número de capas elegidas (la capa más alta que trae alguna opción)
function capasElegidas(elegidas) {
  let capas = 1;
  for (let i = 0; i < elegidas.length; i++) {
    if (capaDe(elegidas[i]) > capas) {
      capas = capaDe(elegidas[i]);
    }
  }
  return capas;
}

// Lo que cuenta para una capa: lo del pastel completo más lo elegido en esa capa
function elegidasDeCapa(elegidas, porCapas, capa) {
  const resultado = [];
  for (let i = 0; i < elegidas.length; i++) {
    if (porCapas.indexOf(elegidas[i].opcion.idCaracteristica) < 0 || capaDe(elegidas[i]) === capa) {
      resultado.push(elegidas[i]);
    }
  }
  return resultado;
}

// Cuántas filas (capas) hay elegidas de una característica
function filasDe(elegidas, idCaracteristica) {
  let filas = 0;
  for (let i = 0; i < elegidas.length; i++) {
    if (elegidas[i].opcion.idCaracteristica === idCaracteristica) {
      filas = filas + 1;
    }
  }
  return filas;
}

// Parte que paga cada fila: en selección única se reparte entre las capas; en múltiple, cada fila completa.
// Sin la lista de características (llamadas viejas) cada fila paga completa.
function parteDeFila(caracteristicas, elegidas, elegida) {
  if (caracteristicas === undefined || caracteristicas === null) {
    return 1;
  }
  const caracteristica = buscarCaracteristica(caracteristicas, elegida.opcion.idCaracteristica);
  if (caracteristica === null || caracteristica.seleccionMultiple) {
    return 1;
  }
  return 1 / Math.max(1, filasDe(elegidas, caracteristica.idCaracteristica));
}

// Revisa si una opción está elegida en esa capa (o para el pastel completo)
function elegidaEnCapa(elegidas, porCapas, idOpcion, capa) {
  for (let i = 0; i < elegidas.length; i++) {
    const elegida = elegidas[i];
    if (elegida.opcion.idOpcion !== idOpcion) {
      continue;
    }
    if (porCapas.indexOf(elegida.opcion.idCaracteristica) < 0 || capaDe(elegida) === capa) {
      return true;
    }
  }
  return false;
}

// Producto de los MultiplicadorPresupuesto de las opciones elegidas (por ejemplo tamaño grande = 2)
function factorPresupuesto(elegidas) {
  let factor = 1;
  for (let i = 0; i < elegidas.length; i++) {
    factor = factor * elegidas[i].opcion.multiplicadorPresupuesto;
  }
  return factor;
}

// Factor de escala = producto de MultiplicadorIngredientes de las opciones que NO se escalan (tamaño, pisos).
// Por capas, cada fila pone su parte: MultiplicadorIngredientes ^ (parte de la fila).
function factorEscala(elegidas, caracteristicas) {
  let factor = 1;
  for (let i = 0; i < elegidas.length; i++) {
    if (!elegidas[i].opcion.seEscala) {
      const parte = parteDeFila(caracteristicas, elegidas, elegidas[i]);
      factor = factor * Math.pow(elegidas[i].opcion.multiplicadorIngredientes, parte);
    }
  }
  return factor;
}

// Precio = suma de precioReferencia × cantidad × (factor si la opción se escala) × parte de la fila.
// Es la misma cuenta que la vista vw_CostoPrecioPostre, redondeada a 2 decimales al final.
// "caracteristicas" sirve para saber cuáles son de selección única (precio por capas).
function calcularPrecio(elegidas, caracteristicas) {
  const factor = factorEscala(elegidas, caracteristicas);
  let precio = 0;
  for (let i = 0; i < elegidas.length; i++) {
    const opcion = elegidas[i].opcion;
    let precioOpcion = opcion.precioReferencia * elegidas[i].cantidad;
    if (opcion.seEscala) {
      precioOpcion = precioOpcion * factor;
    }
    // Por capas: cada capa paga su parte (selección única)
    precioOpcion = precioOpcion * parteDeFila(caracteristicas, elegidas, elegidas[i]);
    precio = precio + precioOpcion;
  }
  return redondear(precio);
}

function redondear(valor) {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

// Muestra un número sin decimales de sobra (3 en vez de 3.00)
function numero(valor) {
  return String(redondear(valor));
}

// Valida una configuración. Regresa la lista de errores (vacía = todo bien).
// Cada error es { tipo, idCaracteristica, mensaje } para que la página marque el campo.
// RF-3 · Pasos 6 y 7 (API): mismas reglas que la página, revisadas otra vez con los datos de la BD.
function validar(caracteristicas, exclusiones, elegidas) {
  const errores = [];

  // RF-3 · Excepción paso 6: no se eligió ninguna opción.
  if (elegidas.length === 0) {
    errores.push({ tipo: 'opciones', idCaracteristica: 0, mensaje: 'Elige al menos una opción para tu postre' });
    return errores;
  }

  // 1. Cada opción debe ser de la categoría y estar disponible
  for (let i = 0; i < elegidas.length; i++) {
    const opcion = elegidas[i].opcion;
    const suCaracteristica = buscarCaracteristica(caracteristicas, opcion.idCaracteristica);
    // RF-3 · Excepción paso 7: la opción ya no está disponible.
    if (suCaracteristica === null || !opcion.disponible || !suCaracteristica.disponible) {
      errores.push({
        tipo: 'disponibilidad',
        idCaracteristica: opcion.idCaracteristica,
        mensaje: 'La opción seleccionada ya no está disponible: ' + opcion.nombre
      });
    }
  }

  // 2. Reglas de cada característica (las que son por capas se revisan en cada capa)
  const porCapas = caracteristicasPorCapas(elegidas);
  const capas = capasElegidas(elegidas);
  for (let i = 0; i < caracteristicas.length; i++) {
    const caracteristica = caracteristicas[i];
    if (!caracteristica.disponible) {
      continue;
    }
    const esPorCapas = porCapas.indexOf(caracteristica.idCaracteristica) >= 0;
    let capasARevisar = 1;
    if (esPorCapas) {
      capasARevisar = capas;
    }
    for (let capa = 1; capa <= capasARevisar; capa++) {
      const deCapa = elegidasDeCapa(elegidas, porCapas, capa);
      const factor = factorPresupuesto(deCapa);
      let opcionesElegidas = 0;
      let piezas = 0;
      let puntos = 0;
      for (let j = 0; j < deCapa.length; j++) {
        if (deCapa[j].opcion.idCaracteristica === caracteristica.idCaracteristica) {
          opcionesElegidas = opcionesElegidas + 1;
          piezas = piezas + deCapa[j].cantidad;
          puntos = puntos + deCapa[j].opcion.puntosSeleccion * deCapa[j].cantidad;
        }
      }
      // Texto de la capa para los mensajes («de la capa 2»)
      let enCapa = '';
      if (esPorCapas && capas > 1) {
        enCapa = ' de la capa ' + capa;
      }

      // RF-3 · Excepción paso 6: característica obligatoria sin opción.
      if (caracteristica.obligatoria && opcionesElegidas === 0) {
        errores.push({
          tipo: 'obligatoria',
          idCaracteristica: caracteristica.idCaracteristica,
          mensaje: 'Falta elegir una opción en «' + caracteristica.nombre + '»' + enCapa + ' (es obligatoria)',
          capa: capa
        });
      }

      if (!caracteristica.seleccionMultiple) {
        // RF-3 · Excepción paso 6: más de una opción en una característica de selección única.
        if (piezas > 1) {
          errores.push({
            tipo: 'seleccionUnica',
            idCaracteristica: caracteristica.idCaracteristica,
            mensaje: 'En «' + caracteristica.nombre + '»' + enCapa + ' solo se puede elegir una opción',
            capa: capa
          });
        }
      } else {
        const disponibles = caracteristica.presupuestoSeleccion * factor;
        // RF-3 · Excepción paso 6: se excede el presupuesto de selección (indica la característica y los puntos).
        if (puntos > disponibles + MARGEN) {
          errores.push({
            tipo: 'presupuesto',
            idCaracteristica: caracteristica.idCaracteristica,
            mensaje: '«' + caracteristica.nombre + '»' + enCapa + ' excede su presupuesto de selección: usa ' +
              numero(puntos) + ' de ' + numero(disponibles) + ' puntos disponibles',
            capa: capa
          });
        }
      }
    }
  }

  // 3. Exclusiones: dos opciones elegidas en la misma capa (o del pastel completo) que no se pueden combinar
  for (let i = 0; i < exclusiones.length; i++) {
    const exclusion = exclusiones[i];
    let chocan = false;
    for (let capa = 1; capa <= capas; capa++) {
      if (elegidaEnCapa(elegidas, porCapas, exclusion.idOpcion, capa) &&
          elegidaEnCapa(elegidas, porCapas, exclusion.idOpcionExcluida, capa)) {
        chocan = true;
      }
    }
    // RF-3 · Excepción paso 7: opciones elegidas que se excluyen entre sí (con su motivo).
    if (chocan) {
      let motivo = exclusion.motivo;
      if (motivo === null || motivo === '') {
        motivo = 'combinación no permitida';
      }
      errores.push({
        tipo: 'exclusion',
        idCaracteristica: 0,
        mensaje: 'No se pueden combinar «' + exclusion.nombreOpcion + '» y «' + exclusion.nombreOpcionExcluida +
          '»: ' + motivo + '. Cambia una de las dos opciones'
      });
    }
  }
  return errores;
}

module.exports = {
  validar: validar,
  calcularPrecio: calcularPrecio,
  factorPresupuesto: factorPresupuesto,
  factorEscala: factorEscala,
  capaDe: capaDe,
  redondear: redondear
};
