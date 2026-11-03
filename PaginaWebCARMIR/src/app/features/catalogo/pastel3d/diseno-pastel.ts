import { CaracteristicaPersonalizable, OpcionElegida, OpcionPersonalizable } from '../modelos/catalogo.models';
import { FormaPastel } from './formas-pastel';

/**
 * Traduce lo que elige el cliente (características y opciones del Panel) a un DISEÑO del pastel 3D.
 *
 * Las características y opciones se reconocen por su NOMBRE, así funcionan también las que
 * se agreguen después en el Panel Administrativo:
 *  - Tamaño:     chico / individual → 1 piso;  mediano → 2 pisos;  grande / familiar → 3 pisos.
 *  - Forma:      redondo, cuadrado, corazón, hexágono, estrella, flor, ovalado.
 *  - Sabor:      color del bizcocho (o de la gelatina) y de su cobertura.
 *  - Relleno:    color de la capa de en medio (se ve con «Ver por dentro»).
 *  - Cobertura:  fondant liso, betún, chantilly, ganache o naked.
 *  - Decoración: moño, listón, flores, velas, perlas, fresas, fruta, crema, chispas, goteo.
 * Si algo no se reconoce, el modelo usa un valor normal y la página funciona igual que antes.
 */

export type TipoPostre3d = 'pastel' | 'gelatina';
export type TipoCobertura = 'lisa' | 'betun' | 'chantilly' | 'ganache' | 'naked';
export type TipoCaracteristica = 'tamano' | 'pisos' | 'forma' | 'sabor' | 'relleno' | 'cobertura' | 'decoracion' | 'otra';

// Cuántas piezas de cada decoración van en el 3D (la cantidad que eligió el cliente).
// Si la decoración no es de selección múltiple, la cantidad es 1.
export interface DecoracionesPastel {
  mono: number;
  liston: number;
  flores: number;
  perlas: number;
  chispas: number;
  goteo: number;
  rosetas: number;
  velas: number;
  fresas: number;
  frutaPicada: number;
}

export interface DisenoPastel {
  tipo: TipoPostre3d;
  forma: FormaPastel;
  // Número de pisos (1 a 4)
  pisos: number;
  // Ancho del piso de abajo en centímetros
  diametro: number;
  // Alto de cada piso en centímetros
  alturaPiso: number;
  colorBizcocho: string;
  colorCobertura: string;
  colorRelleno: string;
  // Gelatina mosaico: cubitos de colores
  mosaico: boolean;
  cobertura: TipoCobertura;
  decoraciones: DecoracionesPastel;
  dedicatoria: string;
}

// Cómo se ve una opción en los botones de la página
export interface AparienciaOpcion {
  tipo: 'color' | 'forma' | 'tamano' | 'pisos' | 'ninguna';
  color: string;
  forma: FormaPastel;
  // 1 = chico, 2 = mediano, 3 = grande, 4 = extra grande
  nivel: number;
}

interface ColoresSabor {
  palabras: string[];
  bizcocho: string;
  cobertura: string;
}

// Colores de cada sabor: el primero que coincida con el nombre de la opción
const SABORES: ColoresSabor[] = [
  { palabras: ['red velvet', 'terciopelo'], bizcocho: '#a3202c', cobertura: '#fbf5ee' },
  { palabras: ['tres leches', '3 leches'], bizcocho: '#f3dfae', cobertura: '#fff8ea' },
  { palabras: ['chocolate', 'choco', 'cacao', 'brownie', 'ganache'], bizcocho: '#5a3221', cobertura: '#7a4529' },
  { palabras: ['fresa', 'frutos rojos'], bizcocho: '#f5c3c6', cobertura: '#f4a7b9' },
  { palabras: ['frambuesa', 'mora', 'zarzamora', 'uva', 'arandano'], bizcocho: '#b4568a', cobertura: '#d9a3c4' },
  { palabras: ['limon', 'lima'], bizcocho: '#f4df78', cobertura: '#e9f5b0' },
  { palabras: ['naranja', 'mandarina'], bizcocho: '#f7b267', cobertura: '#ffd29a' },
  { palabras: ['mango', 'durazno', 'maracuya'], bizcocho: '#f7b84a', cobertura: '#ffd27a' },
  { palabras: ['pina'], bizcocho: '#f6dd6b', cobertura: '#fff1a8' },
  { palabras: ['cajeta', 'caramelo', 'dulce de leche', 'cafe', 'moka'], bizcocho: '#c98c4a', cobertura: '#e3b47a' },
  { palabras: ['nuez', 'almendra', 'zanahoria'], bizcocho: '#b07a45', cobertura: '#f4e6cf' },
  { palabras: ['coco'], bizcocho: '#f6efe2', cobertura: '#ffffff' },
  { palabras: ['matcha', 'pistache', 'menta'], bizcocho: '#a9c46a', cobertura: '#d4e4a8' },
  { palabras: ['mosaico'], bizcocho: '#fbf6ee', cobertura: '#fbf6ee' },
  { palabras: ['leche', 'rompope', 'vainilla', 'queso'], bizcocho: '#f1d79a', cobertura: '#fbf2dc' },
];

// Colores de gelatina (más intensos que el bizcocho)
const SABORES_GELATINA: ColoresSabor[] = [
  { palabras: ['fresa', 'frutos rojos', 'cereza'], bizcocho: '#e2334f', cobertura: '#e2334f' },
  { palabras: ['frambuesa', 'mora', 'uva', 'zarzamora'], bizcocho: '#8e2f7a', cobertura: '#8e2f7a' },
  { palabras: ['limon', 'lima', 'menta'], bizcocho: '#9fd356', cobertura: '#9fd356' },
  { palabras: ['naranja', 'mandarina', 'mango', 'durazno'], bizcocho: '#f59a2c', cobertura: '#f59a2c' },
  { palabras: ['pina'], bizcocho: '#f4d03f', cobertura: '#f4d03f' },
  { palabras: ['mosaico'], bizcocho: '#fbf6ee', cobertura: '#fbf6ee' },
  { palabras: ['chocolate', 'cafe'], bizcocho: '#6b3b26', cobertura: '#6b3b26' },
  { palabras: ['cajeta', 'rompope', 'leche', 'vainilla', 'coco', 'queso'], bizcocho: '#f3e3c3', cobertura: '#f3e3c3' },
];

const COLOR_NORMAL: ColoresSabor = { palabras: [], bizcocho: '#f1d79a', cobertura: '#fbf2dc' };
const COLOR_GELATINA_NORMAL: ColoresSabor = { palabras: [], bizcocho: '#e2334f', cobertura: '#e2334f' };

// ===================== Texto =====================

// Minúsculas y sin acentos, para comparar nombres
export function normalizar(texto: string): string {
  return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
}

function contieneAlguna(texto: string, palabras: string[]): boolean {
  for (const palabra of palabras) {
    if (texto.indexOf(palabra) >= 0) {
      return true;
    }
  }
  return false;
}

// ===================== Reconocer características y opciones =====================

export function tipoDeCaracteristica(nombre: string): TipoCaracteristica {
  const texto = normalizar(nombre);
  // Pisos y tamaño son cosas distintas: los pisos agregan niveles, el tamaño hace más grande el pastel
  if (contieneAlguna(texto, ['piso', 'nivel'])) {
    return 'pisos';
  }
  if (contieneAlguna(texto, ['tamano', 'porcion', 'personas', 'medida'])) {
    return 'tamano';
  }
  if (contieneAlguna(texto, ['forma', 'figura', 'molde'])) {
    return 'forma';
  }
  if (contieneAlguna(texto, ['relleno'])) {
    return 'relleno';
  }
  if (contieneAlguna(texto, ['sabor', 'bizcocho', 'pan'])) {
    return 'sabor';
  }
  if (contieneAlguna(texto, ['cobertura', 'betun', 'acabado', 'cubierta', 'glaseado', 'frosting', 'fondant', 'decorado exterior'])) {
    return 'cobertura';
  }
  if (contieneAlguna(texto, ['decoracion', 'adorno', 'topping', 'extra'])) {
    return 'decoracion';
  }
  return 'otra';
}

// Forma que nombra una opción (null si no nombra ninguna)
export function formaDeOpcion(nombre: string): FormaPastel | null {
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['corazon'])) {
    return 'corazon';
  }
  if (contieneAlguna(texto, ['cuadrad', 'rectangul'])) {
    return 'cuadrado';
  }
  if (contieneAlguna(texto, ['hexagon'])) {
    return 'hexagono';
  }
  if (contieneAlguna(texto, ['estrella'])) {
    return 'estrella';
  }
  if (contieneAlguna(texto, ['flor'])) {
    return 'flor';
  }
  if (contieneAlguna(texto, ['oval'])) {
    return 'ovalado';
  }
  if (contieneAlguna(texto, ['redond', 'circul'])) {
    return 'redondo';
  }
  return null;
}

// Nivel de tamaño por nombre (0 si no se reconoce)
function nivelPorNombre(nombre: string): number {
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['extra', 'gigante', 'jumbo'])) {
    return 4;
  }
  if (contieneAlguna(texto, ['grande', 'familiar'])) {
    return 3;
  }
  if (contieneAlguna(texto, ['median'])) {
    return 2;
  }
  if (contieneAlguna(texto, ['chic', 'individual', 'mini', 'pequen'])) {
    return 1;
  }
  return 0;
}

// Nivel de tamaño de una opción: por nombre o, si no, por su lugar en la lista
export function nivelDeTamano(caracteristica: CaracteristicaPersonalizable, opcion: OpcionPersonalizable): number {
  const porNombre = nivelPorNombre(opcion.nombre);
  if (porNombre > 0) {
    return porNombre;
  }
  const posicion = caracteristica.opciones.indexOf(opcion);
  return Math.min(4, Math.max(1, posicion + 1));
}

// Número de pisos de una opción: «2 pisos», «Dos pisos», «Tres niveles»... o su lugar en la lista
export function pisosDeOpcion(caracteristica: CaracteristicaPersonalizable, opcion: OpcionPersonalizable): number {
  const texto = normalizar(opcion.nombre);
  const numero = texto.match(/[1-9]/);
  if (numero !== null) {
    return Math.min(5, Number(numero[0]));
  }
  const palabras = ['un', 'dos', 'tres', 'cuatro', 'cinco'];
  const partes = texto.split(' ');
  for (let i = palabras.length - 1; i >= 0; i--) {
    if (partes.indexOf(palabras[i]) >= 0 || (i === 0 && partes.indexOf('uno') >= 0)) {
      return i + 1;
    }
  }
  if (contieneAlguna(texto, ['sencillo', 'simple'])) {
    return 1;
  }
  const posicion = caracteristica.opciones.indexOf(opcion);
  return Math.min(5, Math.max(1, posicion + 1));
}

function coloresDe(nombre: string, gelatina: boolean): ColoresSabor {
  const texto = normalizar(nombre);
  let lista = SABORES;
  let normal = COLOR_NORMAL;
  if (gelatina) {
    lista = SABORES_GELATINA;
    normal = COLOR_GELATINA_NORMAL;
  }
  for (const sabor of lista) {
    if (contieneAlguna(texto, sabor.palabras)) {
      return sabor;
    }
  }
  return normal;
}

// Color que representa una opción de sabor o relleno (null si no se reconoce)
export function colorDeOpcion(nombre: string, gelatina: boolean): string | null {
  const colores = coloresDe(nombre, gelatina);
  if (colores.palabras.length === 0) {
    return null;
  }
  return colores.bizcocho;
}

// Acabados en orden, para las coberturas cuyo nombre no se reconoce (cada opción se ve distinta)
const ACABADOS: TipoCobertura[] = ['betun', 'lisa', 'chantilly', 'ganache', 'naked'];
const COLORES_COBERTURA = ['#fbf2dc', '#f4a7b9', '#ffffff', '#4a2617', '#cfe3f3', '#d9a3c4', '#ffe9a8'];

// Color propio de cada acabado (cuando el nombre no dice un sabor)
function colorDeAcabado(acabado: TipoCobertura, posicion: number): string {
  if (acabado === 'ganache') {
    return '#4a2617';
  }
  if (acabado === 'chantilly') {
    return '#ffffff';
  }
  if (acabado === 'naked') {
    // Naked: capa delgada de betún crema que deja ver el pan
    return '#fbf2dc';
  }
  return COLORES_COBERTURA[posicion % COLORES_COBERTURA.length];
}

function coberturaDeOpcion(nombre: string): TipoCobertura | null {
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['fondant', 'liso', 'lisa'])) {
    return 'lisa';
  }
  if (contieneAlguna(texto, ['chantilly', 'crema batida', 'nata'])) {
    return 'chantilly';
  }
  if (contieneAlguna(texto, ['ganache', 'espejo'])) {
    return 'ganache';
  }
  if (contieneAlguna(texto, ['naked', 'desnudo', 'sin cobertura'])) {
    return 'naked';
  }
  if (contieneAlguna(texto, ['betun', 'buttercream', 'merengue', 'queso crema', 'mantequilla', 'crema'])) {
    return 'betun';
  }
  return null;
}

// Acabado y color de una opción de cobertura. Siempre da algo, para que cada opción cambie el 3D.
function aspectoCobertura(caracteristica: CaracteristicaPersonalizable, opcion: OpcionPersonalizable): { acabado: TipoCobertura; color: string } {
  const posicion = Math.max(0, caracteristica.opciones.indexOf(opcion));
  let acabado = coberturaDeOpcion(opcion.nombre);
  if (acabado === null) {
    acabado = ACABADOS[posicion % ACABADOS.length];
  }
  let color = colorDeAcabado(acabado, posicion);
  // Si el nombre dice un sabor («Betún de fresa», «Ganache de chocolate blanco»), se usa su color
  const sabor = coloresDe(opcion.nombre, false);
  if (sabor.palabras.length > 0) {
    color = sabor.cobertura;
  }
  if (normalizar(opcion.nombre).indexOf('blanco') >= 0) {
    color = '#fbf5ee';
  }
  return { acabado: acabado, color: color };
}

// ¿El postre es una gelatina? (por su categoría o su nombre)
export function esGelatina(categoria: string, nombre: string): boolean {
  return contieneAlguna(normalizar(categoria + ' ' + nombre), ['gelatina', 'flan', 'jalea']);
}

// Cómo se dibuja una opción en su botón (color, forma o tamaño)
export function aparienciaDeOpcion(
  caracteristica: CaracteristicaPersonalizable,
  opcion: OpcionPersonalizable,
  gelatina: boolean,
): AparienciaOpcion {
  const apariencia: AparienciaOpcion = { tipo: 'ninguna', color: '', forma: 'redondo', nivel: 0 };
  const tipo = tipoDeCaracteristica(caracteristica.nombre);
  if (tipo === 'tamano') {
    apariencia.tipo = 'tamano';
    apariencia.nivel = nivelDeTamano(caracteristica, opcion);
    return apariencia;
  }
  if (tipo === 'pisos') {
    apariencia.tipo = 'pisos';
    apariencia.nivel = pisosDeOpcion(caracteristica, opcion);
    return apariencia;
  }
  if (tipo === 'cobertura') {
    apariencia.tipo = 'color';
    apariencia.color = aspectoCobertura(caracteristica, opcion).color;
    return apariencia;
  }
  if (tipo === 'forma') {
    const forma = formaDeOpcion(opcion.nombre);
    if (forma !== null) {
      apariencia.tipo = 'forma';
      apariencia.forma = forma;
    }
    return apariencia;
  }
  if (tipo === 'sabor' || tipo === 'relleno') {
    const color = colorDeOpcion(opcion.nombre, gelatina && tipo === 'sabor');
    if (color !== null) {
      apariencia.tipo = 'color';
      apariencia.color = color;
    }
  }
  return apariencia;
}

// ===================== Diseño completo =====================

function decoracionesVacias(): DecoracionesPastel {
  return {
    mono: 0,
    liston: 0,
    flores: 0,
    perlas: 0,
    chispas: 0,
    goteo: 0,
    rosetas: 0,
    velas: 0,
    fresas: 0,
    frutaPicada: 0,
  };
}

// Límite de piezas por decoración (para que el 3D no se sature)
const MAXIMO_PIEZAS = 40;

function sumar(actual: number, cantidad: number): number {
  return Math.min(MAXIMO_PIEZAS, actual + cantidad);
}

// Suma una decoración elegida "cantidad" veces: cada unidad es una pieza en el 3D
function agregarDecoracion(decoraciones: DecoracionesPastel, nombre: string, cantidad: number): void {
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['vela'])) {
    decoraciones.velas = sumar(decoraciones.velas, cantidad);
  } else if (contieneAlguna(texto, ['mono'])) {
    decoraciones.mono = sumar(decoraciones.mono, cantidad);
  } else if (contieneAlguna(texto, ['liston', 'cinta'])) {
    decoraciones.liston = sumar(decoraciones.liston, cantidad);
  } else if (contieneAlguna(texto, ['flor'])) {
    decoraciones.flores = sumar(decoraciones.flores, cantidad);
  } else if (contieneAlguna(texto, ['perla'])) {
    decoraciones.perlas = sumar(decoraciones.perlas, cantidad);
  } else if (contieneAlguna(texto, ['chispa', 'grajea', 'confeti'])) {
    decoraciones.chispas = sumar(decoraciones.chispas, cantidad);
  } else if (contieneAlguna(texto, ['goteo', 'ganache', 'drip'])) {
    decoraciones.goteo = sumar(decoraciones.goteo, cantidad);
  } else if (contieneAlguna(texto, ['fresa'])) {
    decoraciones.fresas = sumar(decoraciones.fresas, cantidad);
  } else if (contieneAlguna(texto, ['fruta', 'durazno', 'kiwi', 'mango'])) {
    decoraciones.frutaPicada = sumar(decoraciones.frutaPicada, cantidad);
  } else if (contieneAlguna(texto, ['crema', 'chantilly', 'roseta', 'merengue'])) {
    decoraciones.rosetas = sumar(decoraciones.rosetas, cantidad);
  }
}

function buscarOpcionEn(caracteristica: CaracteristicaPersonalizable, idOpcion: number): OpcionPersonalizable | null {
  for (const opcion of caracteristica.opciones) {
    if (opcion.idOpcion === idOpcion) {
      return opcion;
    }
  }
  return null;
}

/**
 * Arma el diseño 3D con lo que el cliente tiene elegido.
 * No cambia nada de la selección ni del precio: solo se usa para dibujar.
 */
export function disenoDesdeSeleccion(
  caracteristicas: CaracteristicaPersonalizable[],
  seleccion: OpcionElegida[],
  categoria: string,
  nombreProducto: string,
  dedicatoria: string,
): DisenoPastel {
  const gelatina = esGelatina(categoria, nombreProducto);
  let tipo: TipoPostre3d = 'pastel';
  let colores = COLOR_NORMAL;
  if (gelatina) {
    tipo = 'gelatina';
    colores = COLOR_GELATINA_NORMAL;
  }
  const diseno: DisenoPastel = {
    tipo: tipo,
    forma: 'redondo',
    pisos: 1,
    diametro: 24,
    alturaPiso: 10,
    colorBizcocho: colores.bizcocho,
    colorCobertura: colores.cobertura,
    colorRelleno: '#e8657a',
    mosaico: false,
    cobertura: 'betun',
    decoraciones: decoracionesVacias(),
    dedicatoria: dedicatoria.trim(),
  };
  if (gelatina) {
    diseno.pisos = 1;
    diseno.diametro = 20;
    diseno.alturaPiso = 9;
    diseno.cobertura = 'lisa';
  }

  let coberturaElegida = false;
  for (const caracteristica of caracteristicas) {
    const tipoCaracteristica = tipoDeCaracteristica(caracteristica.nombre);
    for (const elegida of seleccion) {
      const opcion = buscarOpcionEn(caracteristica, elegida.idOpcion);
      if (opcion === null || elegida.cantidad <= 0) {
        continue;
      }
      // Piezas que se dibujan: la cantidad elegida, o una sola si la característica no es múltiple
      let piezas = 1;
      if (caracteristica.seleccionMultiple) {
        piezas = elegida.cantidad;
      }
      if (tipoCaracteristica === 'tamano') {
        const nivel = nivelDeTamano(caracteristica, opcion);
        aplicarTamano(diseno, nivel);
      } else if (tipoCaracteristica === 'pisos') {
        if (!gelatina) {
          diseno.pisos = pisosDeOpcion(caracteristica, opcion);
        }
      } else if (tipoCaracteristica === 'forma') {
        const forma = formaDeOpcion(opcion.nombre);
        if (forma !== null) {
          diseno.forma = forma;
        }
      } else if (tipoCaracteristica === 'sabor') {
        const sabor = coloresDe(opcion.nombre, gelatina);
        diseno.colorBizcocho = sabor.bizcocho;
        if (!coberturaElegida) {
          diseno.colorCobertura = sabor.cobertura;
        }
        diseno.mosaico = normalizar(opcion.nombre).indexOf('mosaico') >= 0;
      } else if (tipoCaracteristica === 'relleno') {
        diseno.colorRelleno = coloresDe(opcion.nombre, false).bizcocho;
      } else if (tipoCaracteristica === 'cobertura') {
        const aspecto = aspectoCobertura(caracteristica, opcion);
        diseno.cobertura = aspecto.acabado;
        diseno.colorCobertura = aspecto.color;
        coberturaElegida = true;
      } else if (tipoCaracteristica === 'decoracion') {
        agregarDecoracion(diseno.decoraciones, opcion.nombre, piezas);
      } else {
        // Característica sin tipo reconocido: si la opción nombra una forma o decoración, se usa
        const forma = formaDeOpcion(opcion.nombre);
        if (forma !== null) {
          diseno.forma = forma;
        } else {
          agregarDecoracion(diseno.decoraciones, opcion.nombre, piezas);
        }
      }
    }
  }

  // La ganache es de chocolate aunque el sabor sea otro
  if (diseno.cobertura === 'ganache' && !coberturaElegida) {
    diseno.colorCobertura = '#4a2617';
  }
  return diseno;
}

const DIAMETROS_PASTEL = [18, 24, 30, 36];
const ALTURAS_PASTEL = [9, 10, 11, 12];
const DIAMETROS_GELATINA = [12, 18, 24, 28];
const ALTURAS_GELATINA = [7, 9, 11, 12];

// Diámetro en centímetros de cada tamaño (1 = chico ... 4 = extra grande)
function diametroDeNivel(nivel: number, gelatina: boolean): number {
  if (gelatina) {
    return DIAMETROS_GELATINA[nivel - 1];
  }
  return DIAMETROS_PASTEL[nivel - 1];
}

// Medidas según el tamaño. El tamaño NO cambia los pisos.
function aplicarTamano(diseno: DisenoPastel, nivel: number): void {
  const gelatina = diseno.tipo === 'gelatina';
  diseno.diametro = diametroDeNivel(nivel, gelatina);
  if (gelatina) {
    diseno.alturaPiso = ALTURAS_GELATINA[nivel - 1];
  } else {
    diseno.alturaPiso = ALTURAS_PASTEL[nivel - 1];
  }
}
