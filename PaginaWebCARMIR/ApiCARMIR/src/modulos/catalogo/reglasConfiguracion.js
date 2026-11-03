/*
 * Reglas para armar un postre (equivalente a ConfiguracionPostre.java del Panel).
 * La página web tiene una copia en Angular para calcular el precio en vivo;
 * esta versión del servidor es la que decide al final (RF-3, paso 7).
 *
 * Datos que usa:
 *  - caracteristicas: [{ idCaracteristica, nombre, obligatoria, seleccionMultiple, presupuestoSeleccion, disponible }]
 *  - exclusiones:     [{ idOpcion, idOpcionExcluida, nombreOpcion, nombreOpcionExcluida, motivo }]
 *  - elegidas:        [{ opcion: {...}, cantidad }]
 *    donde opcion = { idOpcion, idCaracteristica, nombre, disponible, puntosSeleccion,
 *                     multiplicadorPresupuesto, multiplicadorIngredientes, seEscala, precioReferencia }
 *    precioReferencia = CostoReceta × MultiplicadorCosto (precio de la opción sin escalar)
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

// Revisa si una opción está entre las elegidas
function estaElegida(elegidas, idOpcion) {
  for (let i = 0; i < elegidas.length; i++) {
    if (elegidas[i].opcion.idOpcion === idOpcion) {
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

// Factor de escala = producto de MultiplicadorIngredientes de las opciones que NO se escalan (tamaño, pisos)
function factorEscala(elegidas) {
  let factor = 1;
  for (let i = 0; i < elegidas.length; i++) {
    if (!elegidas[i].opcion.seEscala) {
      factor = factor * elegidas[i].opcion.multiplicadorIngredientes;
    }
  }
  return factor;
}

// Precio = suma de precioReferencia × cantidad × (factor si la opción se escala).
// Es la misma cuenta que la vista vw_CostoPrecioPostre, redondeada a 2 decimales al final.
function calcularPrecio(elegidas) {
  const factor = factorEscala(elegidas);
  let precio = 0;
  for (let i = 0; i < elegidas.length; i++) {
    const opcion = elegidas[i].opcion;
    let precioOpcion = opcion.precioReferencia * elegidas[i].cantidad;
    if (opcion.seEscala) {
      precioOpcion = precioOpcion * factor;
    }
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

  // 2. Reglas de cada característica
  const factor = factorPresupuesto(elegidas);
  for (let i = 0; i < caracteristicas.length; i++) {
    const caracteristica = caracteristicas[i];
    if (!caracteristica.disponible) {
      continue;
    }
    let opcionesElegidas = 0;
    let piezas = 0;
    let puntos = 0;
    for (let j = 0; j < elegidas.length; j++) {
      if (elegidas[j].opcion.idCaracteristica === caracteristica.idCaracteristica) {
        opcionesElegidas = opcionesElegidas + 1;
        piezas = piezas + elegidas[j].cantidad;
        puntos = puntos + elegidas[j].opcion.puntosSeleccion * elegidas[j].cantidad;
      }
    }

    // RF-3 · Excepción paso 6: característica obligatoria sin opción.
    if (caracteristica.obligatoria && opcionesElegidas === 0) {
      errores.push({
        tipo: 'obligatoria',
        idCaracteristica: caracteristica.idCaracteristica,
        mensaje: 'Falta elegir una opción en «' + caracteristica.nombre + '» (es obligatoria)'
      });
    }

    if (!caracteristica.seleccionMultiple) {
      // RF-3 · Excepción paso 6: más de una opción en una característica de selección única.
      if (piezas > 1) {
        errores.push({
          tipo: 'seleccionUnica',
          idCaracteristica: caracteristica.idCaracteristica,
          mensaje: 'En «' + caracteristica.nombre + '» solo se puede elegir una opción'
        });
      }
    } else {
      const disponibles = caracteristica.presupuestoSeleccion * factor;
      // RF-3 · Excepción paso 6: se excede el presupuesto de selección (indica la característica y los puntos).
      if (puntos > disponibles + MARGEN) {
        errores.push({
          tipo: 'presupuesto',
          idCaracteristica: caracteristica.idCaracteristica,
          mensaje: '«' + caracteristica.nombre + '» excede su presupuesto de selección: usa ' +
            numero(puntos) + ' de ' + numero(disponibles) + ' puntos disponibles'
        });
      }
    }
  }

  // 3. Exclusiones: dos opciones elegidas que no se pueden combinar
  for (let i = 0; i < exclusiones.length; i++) {
    const exclusion = exclusiones[i];
    // RF-3 · Excepción paso 7: opciones elegidas que se excluyen entre sí (con su motivo).
    if (estaElegida(elegidas, exclusion.idOpcion) && estaElegida(elegidas, exclusion.idOpcionExcluida)) {
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
  redondear: redondear
};
