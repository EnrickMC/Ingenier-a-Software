import {
  CaracteristicaPersonalizable,
  ErrorConfiguracion,
  Exclusion,
  OpcionElegida,
  OpcionPersonalizable,
} from '../modelos/catalogo.models';

/**
 * Reglas para armar un postre en el navegador (copia de reglasConfiguracion.js de la API
 * y de ConfiguracionPostre.java del Panel).
 * Sirven para recalcular el precio y avisar errores al instante (RF-3, paso 4: 1 segundo).
 * Al final la API vuelve a validar todo antes de agregar al carrito.
 *
 * Capas: en los pasteles de varios pisos el Sabor, la Cobertura, el Color y la Decoración se eligen por capa
 * (cada opción elegida trae "capa"). Una característica es «por capas» si alguna de sus opciones trae capa 2
 * o más. Así:
 *  - Cada capa revisa por separado la selección única, el presupuesto y las exclusiones.
 *  - Precio: en las de selección única cada capa paga su parte (si las 3 capas son de chocolate, se cobra
 *    lo mismo que un pastel de chocolate); en las de selección múltiple se suma lo de cada capa.
 */

// Margen para comparar números con decimales
const MARGEN = 0.0001;

// Una opción elegida junto con sus datos completos
interface Eleccion {
  opcion: OpcionPersonalizable;
  cantidad: number;
  // Capa del pastel (1 = Base)
  capa: number;
}

// Capa de una opción elegida (sin capa = Base)
export function capaElegida(elegida: OpcionElegida): number {
  if (elegida.capa === undefined || elegida.capa === null || elegida.capa < 1) {
    return 1;
  }
  return elegida.capa;
}

// Busca una opción por id dentro de todas las características; null si no está
export function buscarOpcion(
  caracteristicas: CaracteristicaPersonalizable[],
  idOpcion: number,
): OpcionPersonalizable | null {
  for (const caracteristica of caracteristicas) {
    for (const opcion of caracteristica.opciones) {
      if (opcion.idOpcion === idOpcion) {
        return opcion;
      }
    }
  }
  return null;
}

// Junta cada opción elegida con sus datos (se ignoran las que ya no existen)
function armarElecciones(
  caracteristicas: CaracteristicaPersonalizable[],
  seleccion: OpcionElegida[],
): Eleccion[] {
  const elecciones: Eleccion[] = [];
  for (const elegida of seleccion) {
    const opcion = buscarOpcion(caracteristicas, elegida.idOpcion);
    if (opcion !== null) {
      elecciones.push({ opcion: opcion, cantidad: elegida.cantidad, capa: capaElegida(elegida) });
    }
  }
  return elecciones;
}

// Características que se eligen por capas: las que tienen alguna opción en la capa 2 o más
function caracteristicasPorCapas(elecciones: Eleccion[]): number[] {
  const ids: number[] = [];
  for (const eleccion of elecciones) {
    if (eleccion.capa > 1 && ids.indexOf(eleccion.opcion.idCaracteristica) < 0) {
      ids.push(eleccion.opcion.idCaracteristica);
    }
  }
  return ids;
}

// Lo que cuenta para una capa: lo del pastel completo más lo elegido en esa capa
function eleccionesDeCapa(elecciones: Eleccion[], porCapas: number[], capa: number): Eleccion[] {
  const resultado: Eleccion[] = [];
  for (const eleccion of elecciones) {
    if (porCapas.indexOf(eleccion.opcion.idCaracteristica) < 0 || eleccion.capa === capa) {
      resultado.push(eleccion);
    }
  }
  return resultado;
}

// Número de capas elegidas (la capa más alta que trae alguna opción)
function capasElegidas(elecciones: Eleccion[]): number {
  let capas = 1;
  for (const eleccion of elecciones) {
    capas = Math.max(capas, eleccion.capa);
  }
  return capas;
}

// En cuántas filas (capas) está elegida una característica
function filasDe(elecciones: Eleccion[], idCaracteristica: number): number {
  let filas = 0;
  for (const eleccion of elecciones) {
    if (eleccion.opcion.idCaracteristica === idCaracteristica) {
      filas = filas + 1;
    }
  }
  return filas;
}

// Parte que paga cada fila: en selección única se reparte entre las capas; en múltiple, cada fila completa
function parteDeFila(caracteristicas: CaracteristicaPersonalizable[], elecciones: Eleccion[], eleccion: Eleccion): number {
  for (const caracteristica of caracteristicas) {
    if (caracteristica.idCaracteristica === eleccion.opcion.idCaracteristica && !caracteristica.seleccionMultiple) {
      return 1 / Math.max(1, filasDe(elecciones, caracteristica.idCaracteristica));
    }
  }
  return 1;
}

// Una opción elegida en la misma capa (o en el pastel completo) que "capa"
function elegidaEnCapa(elecciones: Eleccion[], porCapas: number[], idOpcion: number, capa: number): boolean {
  for (const eleccion of elecciones) {
    if (eleccion.opcion.idOpcion !== idOpcion || eleccion.cantidad <= 0) {
      continue;
    }
    if (porCapas.indexOf(eleccion.opcion.idCaracteristica) < 0 || capa === 0 || eleccion.capa === capa) {
      return true;
    }
  }
  return false;
}

// Cuántas veces está elegida una opción (0 = no está elegida)
export function cantidadElegida(seleccion: OpcionElegida[], idOpcion: number): number {
  for (const elegida of seleccion) {
    if (elegida.idOpcion === idOpcion) {
      return elegida.cantidad;
    }
  }
  return 0;
}

// Producto de los MultiplicadorPresupuesto de lo elegido (por ejemplo tamaño grande = 2)
function factorPresupuesto(elecciones: Eleccion[]): number {
  let factor = 1;
  for (const eleccion of elecciones) {
    factor = factor * eleccion.opcion.multiplicadorPresupuesto;
  }
  return factor;
}

// Factor de escala = producto de MultiplicadorIngredientes de las opciones que NO se escalan
// (una opción por capas cuenta solo su parte, para no multiplicarla una vez por cada capa)
function factorEscala(caracteristicas: CaracteristicaPersonalizable[], elecciones: Eleccion[]): number {
  let factor = 1;
  for (const eleccion of elecciones) {
    if (!eleccion.opcion.seEscala) {
      factor = factor * Math.pow(eleccion.opcion.multiplicadorIngredientes, parteDeFila(caracteristicas, elecciones, eleccion));
    }
  }
  return factor;
}

export function redondear(valor: number): number {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

// Precio de una pieza: misma cuenta que la vista vw_CostoPrecioPostre
// RF-3 · Paso 4: precio de una pieza que se recalcula con cada cambio.
export function calcularPrecio(
  caracteristicas: CaracteristicaPersonalizable[],
  seleccion: OpcionElegida[],
): number {
  const elecciones = armarElecciones(caracteristicas, seleccion);
  const factor = factorEscala(caracteristicas, elecciones);
  let precio = 0;
  for (const eleccion of elecciones) {
    let precioOpcion = eleccion.opcion.precioReferencia * eleccion.cantidad;
    if (eleccion.opcion.seEscala) {
      precioOpcion = precioOpcion * factor;
    }
    // Por capas: cada capa paga su parte (selección única)
    precioOpcion = precioOpcion * parteDeFila(caracteristicas, elecciones, eleccion);
    precio = precio + precioOpcion;
  }
  return redondear(precio);
}

// Puntos que ya se usaron en una característica de selección múltiple
export function puntosUsados(caracteristica: CaracteristicaPersonalizable, seleccion: OpcionElegida[]): number {
  let puntos = 0;
  for (const opcion of caracteristica.opciones) {
    puntos = puntos + opcion.puntosSeleccion * cantidadElegida(seleccion, opcion.idOpcion);
  }
  return redondear(puntos);
}

// Puntos totales de una característica: presupuesto × multiplicadores de lo elegido
// RF-3 · Paso 4: puntos disponibles de cada característica.
export function puntosTotales(
  caracteristicas: CaracteristicaPersonalizable[],
  caracteristica: CaracteristicaPersonalizable,
  seleccion: OpcionElegida[],
): number {
  const elecciones = armarElecciones(caracteristicas, seleccion);
  return redondear(caracteristica.presupuestoSeleccion * factorPresupuesto(elecciones));
}

/**
 * Si la opción no se puede elegir porque choca con otra ya elegida, regresa el motivo.
 * Si se puede elegir, regresa texto vacío.
 */
// RF-3 · Paso 4: motivo por el que una opción queda deshabilitada (exclusión).
export function motivoExclusion(idOpcion: number, seleccion: OpcionElegida[], exclusiones: Exclusion[]): string {
  for (const exclusion of exclusiones) {
    let otraOpcion = 0;
    let nombreOtra = '';
    if (exclusion.idOpcion === idOpcion) {
      otraOpcion = exclusion.idOpcionExcluida;
      nombreOtra = exclusion.nombreOpcionExcluida;
    } else if (exclusion.idOpcionExcluida === idOpcion) {
      otraOpcion = exclusion.idOpcion;
      nombreOtra = exclusion.nombreOpcion;
    }
    if (otraOpcion > 0 && cantidadElegida(seleccion, otraOpcion) > 0) {
      let motivo = 'combinación no permitida';
      if (exclusion.motivo !== null && exclusion.motivo !== '') {
        motivo = exclusion.motivo;
      }
      return 'No combina con «' + nombreOtra + '»: ' + motivo;
    }
  }
  return '';
}

// Valida la configuración completa. Lista vacía = todo bien.
// RF-3 · Paso 6: valida la configuración en el navegador antes de llamar a la API.
// "capasObligatorias" e "idsPorCapas": capas del pastel y características que se eligen por capa
// (así una capa que se quedó sin sabor también se marca). Sin capas: 1 y [].
export function validarConfiguracion(
  caracteristicas: CaracteristicaPersonalizable[],
  exclusiones: Exclusion[],
  seleccion: OpcionElegida[],
  capasObligatorias: number = 1,
  idsPorCapas: number[] = [],
): ErrorConfiguracion[] {
  const errores: ErrorConfiguracion[] = [];
  const elecciones = armarElecciones(caracteristicas, seleccion);

  // RF-3 · Excepción paso 6: no se eligió ninguna opción.
  if (elecciones.length === 0) {
    errores.push({ tipo: 'opciones', idCaracteristica: 0, mensaje: 'Elige al menos una opción para tu postre' });
    return errores;
  }

  const porCapas = caracteristicasPorCapas(elecciones);
  // Las que la página dice que son por capa también cuentan (aunque solo tengan algo en la Base)
  if (capasObligatorias > 1) {
    for (const id of idsPorCapas) {
      if (porCapas.indexOf(id) < 0) {
        porCapas.push(id);
      }
    }
  }
  const capas = Math.max(capasElegidas(elecciones), capasObligatorias);
  for (const caracteristica of caracteristicas) {
    // Las características por capas se revisan en cada capa; las demás, una vez
    let capasARevisar = 1;
    const esPorCapas = porCapas.indexOf(caracteristica.idCaracteristica) >= 0;
    if (esPorCapas) {
      capasARevisar = capas;
    }
    for (let capa = 1; capa <= capasARevisar; capa++) {
      const deCapa = eleccionesDeCapa(elecciones, porCapas, capa);
      const factor = factorPresupuesto(deCapa);
      let opcionesElegidas = 0;
      let piezas = 0;
      let puntos = 0;
      for (const eleccion of deCapa) {
        if (eleccion.opcion.idCaracteristica === caracteristica.idCaracteristica) {
          opcionesElegidas = opcionesElegidas + 1;
          piezas = piezas + eleccion.cantidad;
          puntos = puntos + eleccion.opcion.puntosSeleccion * eleccion.cantidad;
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
          capa: capa,
        });
      }

      if (!caracteristica.seleccionMultiple) {
        // RF-3 · Excepción paso 6: más de una opción en una característica de selección única.
        if (piezas > 1) {
          errores.push({
            tipo: 'seleccionUnica',
            idCaracteristica: caracteristica.idCaracteristica,
            mensaje: 'En «' + caracteristica.nombre + '»' + enCapa + ' solo se puede elegir una opción',
            capa: capa,
          });
        }
      } else {
        const disponibles = caracteristica.presupuestoSeleccion * factor;
        // RF-3 · Excepción paso 6: se excede el presupuesto de selección (indica la característica y los puntos).
        if (puntos > disponibles + MARGEN) {
          errores.push({
            tipo: 'presupuesto',
            idCaracteristica: caracteristica.idCaracteristica,
            mensaje:
              '«' + caracteristica.nombre + '»' + enCapa + ' excede su presupuesto de selección: usa ' +
              redondear(puntos) + ' de ' + redondear(disponibles) + ' puntos disponibles',
            capa: capa,
          });
        }
      }
    }
  }

  for (const exclusion of exclusiones) {
    // Por capas: dos opciones chocan si están en la misma capa (o si alguna es del pastel completo)
    let chocan = false;
    for (let capa = 1; capa <= capas; capa++) {
      if (elegidaEnCapa(elecciones, porCapas, exclusion.idOpcion, capa) && elegidaEnCapa(elecciones, porCapas, exclusion.idOpcionExcluida, capa)) {
        chocan = true;
      }
    }
    // RF-3 · Excepción paso 7: opciones elegidas que se excluyen entre sí.
    if (chocan) {
      let motivo = 'combinación no permitida';
      if (exclusion.motivo !== null && exclusion.motivo !== '') {
        motivo = exclusion.motivo;
      }
      errores.push({
        tipo: 'exclusion',
        idCaracteristica: 0,
        mensaje:
          'No se pueden combinar «' + exclusion.nombreOpcion + '» y «' + exclusion.nombreOpcionExcluida +
          '»: ' + motivo + '. Cambia una de las dos opciones',
      });
    }
  }
  return errores;
}

// ===================== Dedicatoria, comentario y cantidad =====================

export const CANTIDAD_MAXIMA = 20;
export const LARGO_DEDICATORIA = 150;
export const LARGO_COMENTARIO = 255;

// Aviso que se agrega a las indicaciones cuando el mensaje va en un topper (así lo ve la repostería en el pedido)
export const AVISO_TOPPER = 'Mensaje en topper.';

/**
 * Indicaciones que se mandan a la API: si el mensaje va en un topper se avisa al inicio.
 * No hace falta cambiar la base de datos: la repostería lo lee en las indicaciones del pedido.
 */
export function comentarioConTopper(comentario: string, dedicatoria: string, mensajeEnTopper: boolean): string {
  const limpio = comentario.trim();
  if (!mensajeEnTopper || dedicatoria.trim() === '') {
    return limpio;
  }
  if (limpio === '') {
    return AVISO_TOPPER;
  }
  return AVISO_TOPPER + ' ' + limpio;
}

// Largo que le queda al cliente para sus indicaciones (el aviso del topper también ocupa lugar)
export function largoComentarioPermitido(dedicatoria: string, mensajeEnTopper: boolean): number {
  if (!mensajeEnTopper || dedicatoria.trim() === '') {
    return LARGO_COMENTARIO;
  }
  return LARGO_COMENTARIO - AVISO_TOPPER.length - 1;
}

// Mismos caracteres que acepta la API: letras, números y puntuación común (sin emojis)
const CARACTERES_PERMITIDOS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,;:¡!¿?'"()\-&#@/%$+*\n]*$/;

// Regresa el error del texto o vacío si está bien
// RF-3 · Paso 6: valida la dedicatoria y el comentario.
export function validarTexto(texto: string, nombreCampo: string, largoMaximo: number): string {
  const limpio = texto.trim();
  // RF-3 · Excepción paso 6: la dedicatoria excede el largo permitido.
  if (limpio.length > largoMaximo) {
    return nombreCampo + ' admite máximo ' + largoMaximo + ' caracteres';
  }
  // RF-3 · Excepción paso 6: la dedicatoria tiene caracteres no permitidos.
  if (!CARACTERES_PERMITIDOS.test(limpio)) {
    return nombreCampo + ' tiene caracteres no permitidos (no se permiten emojis ni símbolos especiales)';
  }
  return '';
}

// RF-3 · Paso 6: valida la cantidad de piezas.
export function validarCantidad(cantidad: number): string {
  // RF-3 · Excepción paso 6: cantidad fuera de rango.
  if (!Number.isInteger(cantidad) || cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
    return 'La cantidad debe estar entre 1 y ' + CANTIDAD_MAXIMA;
  }
  return '';
}
