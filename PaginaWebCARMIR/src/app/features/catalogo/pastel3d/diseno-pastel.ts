import { CaracteristicaPersonalizable, OpcionElegida, OpcionPersonalizable } from '../modelos/catalogo.models';
import { FormaPastel } from './formas-pastel';

/**
 * RF-5 · Pasos 2 y 3: traduce lo que elige el cliente (características y opciones del Panel) a un DISEÑO del pastel 3D.
 *
 * Las características y opciones se reconocen por su NOMBRE, así funcionan también las que
 * se agreguen después en el Panel Administrativo:
 *  - Tamaño:     chico, mediano, grande, extra grande (en postres chicos: mini, mediano, grande, jumbo).
 *  - Pisos:      1 a 5 pisos (solo pasteles).
 *  - Forma:      redondo, cuadrado, corazón, hexágono, estrella, flor, ovalado.
 *                Gelatinas: molde de gajos, rosca, domo, corazón, estrella, flor, cuadrada.
 *  - Sabor:      color del bizcocho (o de la gelatina) y de su cobertura.
 *  - Relleno:    color de la capa de en medio (se ve con «Ver por dentro»).
 *  - Cobertura:  fondant liso, betún, buttercream, rústica, chantilly, ganache, naked, mármol, degradado
 *                o sin cobertura. Las chispas ya no son cobertura: son una decoración.
 *  - Frosting:   la parte de arriba del cupcake (remolino de betún, chantilly, domo de fondant...).
 *                El pan del cupcake es el Sabor. Si el producto no tiene «Frosting», el cupcake va sin nada arriba.
 *  - Color de cobertura: rosa, azul cielo, dorado, verde menta...
 *  - Base:       base dorada, base plateada, plato blanco, tabla de madera, pizarra, pedestal de cristal.
 *  - Tipo de postre (por categoría o nombre): pastel, gelatina, pay, cheesecake, cupcake, galleta,
 *                brownie, dona, rosca, macaron.
 *  - Decoración: moño, listón, flores, velas, perlas, fresas, frutos rojos, fruta, crema, chispas,
 *                cubierta de chispas (costados y arriba),
 *                goteo, hoja de oro y topper.
 * Capas: en los pasteles de 2 o más pisos, el Sabor, la Cobertura, el Color de cobertura y la Decoración
 * se eligen por separado en cada capa (Base, Capa 2... Superior). Cada opción elegida trae su número de capa.
 * Los mismos nombres salen como sugerencias en el Panel Administrativo (PresetsCatalogo.java, RF-32).
 * RF-5 · Excepción paso 3: si algo no se reconoce, el modelo usa un valor normal y la página funciona igual que antes.
 */

export type TipoPostre3d =
  | 'pastel'
  | 'gelatina'
  | 'pay'
  | 'cheesecake'
  | 'cupcake'
  | 'galleta'
  | 'brownie'
  | 'dona'
  | 'rosca'
  | 'macaron';
// Dónde se presenta el postre
// "ninguna" = el producto deja elegir base y el cliente eligió «Sin base»
export type TipoBase = 'dorada' | 'plateada' | 'blanca' | 'madera' | 'pizarra' | 'cristal' | 'ninguna';
// "ninguna" = sin cobertura: solo se ven las capas de pan y relleno
// "degradado" = betún que va de un color abajo a blanco arriba
// "buttercream" = betún con franjas a lo alto; "rustica" = betún con pinceladas sueltas
export type TipoCobertura = 'lisa' | 'betun' | 'buttercream' | 'rustica' | 'chantilly' | 'ganache' | 'naked' | 'marmol' | 'degradado' | 'ninguna';
// Molde de la gelatina: "figura" = el contorno de la Forma (corazón, estrella, flor, cuadrada...)
export type MoldeGelatina = 'gajos' | 'rosca' | 'domo' | 'figura';
export type TipoCaracteristica = 'tamano' | 'pisos' | 'forma' | 'sabor' | 'relleno' | 'cobertura' | 'frosting' | 'color' | 'base' | 'decoracion' | 'otra';

// Cuántas piezas de cada decoración van en el 3D (la cantidad que eligió el cliente).
// Si la decoración no es de selección múltiple, la cantidad es 1.
export interface DecoracionesPastel {
  mono: number;
  liston: number;
  flores: number;
  perlas: number;
  chispas: number;
  // Cubierta de chispas: chispas pegadas en los costados y arriba (antes era una cobertura)
  chispasTodo: number;
  goteo: number;
  rosetas: number;
  velas: number;
  fresas: number;
  frutaPicada: number;
  // Moras, frambuesas y arándanos (cada unidad es un puñito de 3)
  frutosRojos: number;
  // Hojuelas de oro comestible (un piso por unidad, como las chispas)
  hojaOro: number;
}

// Lo que cambia en cada capa (piso) del pastel
export interface CapaPastel {
  colorBizcocho: string;
  colorCobertura: string;
  colorRelleno: string;
  cobertura: TipoCobertura;
  decoraciones: DecoracionesPastel;
}

export interface DisenoPastel {
  tipo: TipoPostre3d;
  forma: FormaPastel;
  // Molde de la gelatina (en los demás postres no se usa)
  molde: MoldeGelatina;
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
  // Plato, tabla o pedestal donde se presenta
  base: TipoBase;
  decoraciones: DecoracionesPastel;
  dedicatoria: string;
  // Texto del topper (letrero de pie arriba del postre); vacío = sin topper
  topper: string;
  // ¿Cada piso tiene su propio sabor, cobertura y decoración? (pasteles de 2 o más pisos)
  porCapas: boolean;
  // Una capa por piso, de abajo hacia arriba. La primera es la Base (igual a los colores de arriba)
  capas: CapaPastel[];
}

// Cómo se ve una opción en los botones de la página
export interface AparienciaOpcion {
  tipo: 'color' | 'forma' | 'tamano' | 'pisos' | 'ninguna';
  color: string;
  forma: FormaPastel;
  // Gelatinas: molde del botón de forma ("figura" = se dibuja la forma)
  molde: MoldeGelatina;
  // 1 = chico, 2 = mediano, 3 = grande, 4 = extra grande
  nivel: number;
}

interface ColoresSabor {
  palabras: string[];
  // Color del pan (o de la gelatina)
  bizcocho: string;
  // Color del betún que combina con el sabor
  cobertura: string;
  // Color del relleno de ese sabor (mermelada, crema, cajeta...)
  relleno: string;
}

// Colores de cada sabor: el primero que coincida con el nombre de la opción
const SABORES: ColoresSabor[] = [
  { palabras: ['red velvet', 'terciopelo'], bizcocho: '#9b1b2a', cobertura: '#fbf5ee', relleno: '#fbf3e6' },
  { palabras: ['tres leches', '3 leches'], bizcocho: '#f6e2b3', cobertura: '#fff8ea', relleno: '#fff1d2' },
  { palabras: ['oreo', 'galleta', 'cookies'], bizcocho: '#3b2b26', cobertura: '#f4efe6', relleno: '#f4efe6' },
  { palabras: ['nutella', 'avellana', 'ferrero', 'kinder'], bizcocho: '#5c3420', cobertura: '#8a5534', relleno: '#5e3219' },
  { palabras: ['chocolate blanco'], bizcocho: '#f3e6c8', cobertura: '#fbf5ee', relleno: '#f8efd9' },
  { palabras: ['chocolate', 'choco', 'cacao', 'brownie', 'ganache'], bizcocho: '#4e2a1a', cobertura: '#6b3a22', relleno: '#3d1f12' },
  { palabras: ['cereza'], bizcocho: '#f1c0c4', cobertura: '#f7c6cf', relleno: '#8f1026' },
  { palabras: ['fresa', 'frutos rojos'], bizcocho: '#f4b8c0', cobertura: '#f7b2c1', relleno: '#c3203a' },
  { palabras: ['frambuesa', 'mora', 'zarzamora', 'uva', 'arandano'], bizcocho: '#d48aa8', cobertura: '#dba4c4', relleno: '#8e1846' },
  { palabras: ['limon', 'lima'], bizcocho: '#f3e08a', cobertura: '#f3f6c6', relleno: '#efd34a' },
  { palabras: ['naranja', 'mandarina'], bizcocho: '#f5c07a', cobertura: '#ffd9a6', relleno: '#f08a1c' },
  { palabras: ['mango', 'durazno', 'maracuya'], bizcocho: '#f6c25a', cobertura: '#ffd98a', relleno: '#f5a01e' },
  { palabras: ['pina'], bizcocho: '#f5df7d', cobertura: '#fff2b3', relleno: '#f2c230' },
  { palabras: ['cafe', 'moka', 'capuchino'], bizcocho: '#9c6b46', cobertura: '#d2ae8a', relleno: '#6f4a33' },
  { palabras: ['cajeta', 'caramelo', 'dulce de leche'], bizcocho: '#d7a46a', cobertura: '#e8bf8a', relleno: '#a35d24' },
  { palabras: ['zanahoria'], bizcocho: '#d9893e', cobertura: '#f6ecd9', relleno: '#f6ecd9' },
  { palabras: ['nuez', 'almendra'], bizcocho: '#c79a66', cobertura: '#f4e6cf', relleno: '#8a5a32' },
  { palabras: ['coco'], bizcocho: '#f7f1e3', cobertura: '#ffffff', relleno: '#fbf7ee' },
  { palabras: ['matcha', 'te verde'], bizcocho: '#a5c26a', cobertura: '#dbe8b8', relleno: '#8fae55' },
  { palabras: ['pistache'], bizcocho: '#bccf88', cobertura: '#e1ebc4', relleno: '#9db66a' },
  { palabras: ['menta'], bizcocho: '#bfe6d2', cobertura: '#d8f1e4', relleno: '#8fd3b4' },
  { palabras: ['mosaico'], bizcocho: '#fbf6ee', cobertura: '#fbf6ee', relleno: '#fbf6ee' },
  { palabras: ['platano', 'banana'], bizcocho: '#efd58f', cobertura: '#fbf0c8', relleno: '#f3dc8a' },
  { palabras: ['manzana', 'canela', 'horchata', 'chai'], bizcocho: '#d9a86c', cobertura: '#f3e2c4', relleno: '#c98a45' },
  { palabras: ['calabaza'], bizcocho: '#e3954a', cobertura: '#f6e4cc', relleno: '#d97b2a' },
  { palabras: ['guayaba'], bizcocho: '#f4b6a8', cobertura: '#f8cfc6', relleno: '#e8707a' },
  { palabras: ['mazapan', 'cacahuate'], bizcocho: '#e6c79a', cobertura: '#f3e3c6', relleno: '#c99a5e' },
  { palabras: ['elote'], bizcocho: '#f2d36b', cobertura: '#fbefc4', relleno: '#f2d36b' },
  { palabras: ['crema pastelera', 'pastelera', 'natilla'], bizcocho: '#f3dda7', cobertura: '#fbf2dc', relleno: '#f6dc8c' },
  { palabras: ['leche', 'rompope', 'vainilla', 'queso'], bizcocho: '#f3d9a0', cobertura: '#fbf2dc', relleno: '#fff3d6' },
];

// Colores de gelatina (más intensos que el bizcocho)
const SABORES_GELATINA: ColoresSabor[] = [
  { palabras: ['fresa', 'frutos rojos', 'cereza'], bizcocho: '#e0283f', cobertura: '#e0283f', relleno: '#e0283f' },
  { palabras: ['frambuesa', 'mora', 'uva', 'zarzamora'], bizcocho: '#8a2378', cobertura: '#8a2378', relleno: '#8a2378' },
  { palabras: ['limon', 'lima', 'menta'], bizcocho: '#8fd14a', cobertura: '#8fd14a', relleno: '#8fd14a' },
  { palabras: ['naranja', 'mandarina', 'mango', 'durazno'], bizcocho: '#f7941d', cobertura: '#f7941d', relleno: '#f7941d' },
  { palabras: ['pina'], bizcocho: '#f6d02f', cobertura: '#f6d02f', relleno: '#f6d02f' },
  { palabras: ['mosaico'], bizcocho: '#fbf6ee', cobertura: '#fbf6ee', relleno: '#fbf6ee' },
  { palabras: ['chocolate', 'cafe'], bizcocho: '#6b3b26', cobertura: '#6b3b26', relleno: '#6b3b26' },
  { palabras: ['cajeta', 'rompope', 'leche', 'vainilla', 'coco', 'queso'], bizcocho: '#f3e3c3', cobertura: '#f3e3c3', relleno: '#f3e3c3' },
];

const COLOR_NORMAL: ColoresSabor = { palabras: [], bizcocho: '#f3d9a0', cobertura: '#fbf2dc', relleno: '#e8657a' };
const COLOR_GELATINA_NORMAL: ColoresSabor = { palabras: [], bizcocho: '#e0283f', cobertura: '#e0283f', relleno: '#e0283f' };

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
  if (contieneAlguna(texto, ['tamano', 'porcion', 'personas', 'medida', 'pieza', 'cantidad'])) {
    return 'tamano';
  }
  if (contieneAlguna(texto, ['forma', 'figura', 'molde'])) {
    return 'forma';
  }
  if (contieneAlguna(texto, ['relleno'])) {
    return 'relleno';
  }
  // «Color de cobertura» va antes que «Cobertura» (pero «Color de vela» no cambia la cobertura)
  const esColorSolo = texto === 'color' || texto === 'colores';
  if (contieneAlguna(texto, ['color']) && (esColorSolo || contieneAlguna(texto, ['cobertura', 'frosting', 'copete', 'betun', 'fondant', 'glaseado', 'pastel', 'crema']))) {
    return 'color';
  }
  if (contieneAlguna(texto, ['base', 'plato', 'presentacion', 'charola', 'soporte', 'pedestal', 'stand'])) {
    return 'base';
  }
  // Frosting: lo que va arriba del cupcake (en México también «copete» de betún)
  if (contieneAlguna(texto, ['frosting', 'copete', 'remolino'])) {
    return 'frosting';
  }
  if (contieneAlguna(texto, ['sabor', 'bizcocho', 'pan'])) {
    return 'sabor';
  }
  if (contieneAlguna(texto, ['cobertura', 'betun', 'acabado', 'cubierta', 'glaseado', 'fondant', 'decorado exterior'])) {
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

// Molde de gelatina que nombra una opción (null si no nombra ninguno)
export function moldeDeOpcion(nombre: string): MoldeGelatina | null {
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['rosca', 'corona', 'aro', 'bundt', 'savarin'])) {
    return 'rosca';
  }
  if (contieneAlguna(texto, ['domo', 'media esfera', 'semiesfera', 'bola', 'cupula', 'iglu'])) {
    return 'domo';
  }
  if (contieneAlguna(texto, ['gajo', 'tradicional', 'clasic'])) {
    return 'gajos';
  }
  const forma = formaDeOpcion(nombre);
  if (forma === 'redondo') {
    // La gelatina redonda de siempre es la del molde de gajos
    return 'gajos';
  }
  if (forma !== null) {
    return 'figura';
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

// Color que representa una opción de sabor o de relleno (null si no se reconoce)
export function colorDeOpcion(nombre: string, gelatina: boolean, esRelleno: boolean): string | null {
  const colores = coloresDe(nombre, gelatina);
  if (colores.palabras.length === 0) {
    return null;
  }
  if (esRelleno) {
    return colores.relleno;
  }
  return colores.bizcocho;
}

// Colores por su nombre (para «Color de cobertura» o «Fondant rosa»). Los nombres largos van primero.
const COLORES_POR_NOMBRE: { palabras: string[]; color: string }[] = [
  { palabras: ['azul marino'], color: '#24346b' },
  { palabras: ['azul cielo', 'azul bebe', 'celeste'], color: '#a9d4f0' },
  { palabras: ['azul rey'], color: '#2f56b8' },
  { palabras: ['azul'], color: '#7fb2e5' },
  { palabras: ['turquesa', 'aqua'], color: '#5cc8c2' },
  { palabras: ['verde menta', 'menta'], color: '#b5e5cf' },
  { palabras: ['verde'], color: '#8cc084' },
  { palabras: ['rosa mexicano', 'fiusha', 'fucsia'], color: '#d6337f' },
  { palabras: ['rosa palo', 'rosa pastel'], color: '#f4c7cf' },
  { palabras: ['rosa'], color: '#f4a7b9' },
  { palabras: ['rojo'], color: '#c8102e' },
  { palabras: ['vino', 'guinda'], color: '#7b1e33' },
  { palabras: ['frambuesa'], color: '#c2185b' },
  { palabras: ['lila', 'lavanda'], color: '#c9b6e4' },
  { palabras: ['morado', 'purpura', 'violeta'], color: '#7e57c2' },
  { palabras: ['amarillo'], color: '#f7dc6f' },
  { palabras: ['naranja'], color: '#f39c4a' },
  { palabras: ['durazno', 'melocoton'], color: '#f8c9a6' },
  { palabras: ['coral'], color: '#f88379' },
  { palabras: ['dorado', 'oro'], color: '#d4af37' },
  { palabras: ['plateado', 'plata'], color: '#c0c0c8' },
  { palabras: ['cobre', 'bronce'], color: '#b87333' },
  { palabras: ['negro'], color: '#232323' },
  { palabras: ['gris'], color: '#9aa0a6' },
  { palabras: ['chocolate', 'cafe'], color: '#5a3220' },
  { palabras: ['marfil', 'hueso', 'champan'], color: '#f6f0dc' },
  { palabras: ['beige', 'arena', 'nude'], color: '#e8d8b8' },
  { palabras: ['blanco'], color: '#fbf8f3' },
];

// Color que nombra un texto («Rosa pastel», «Fondant azul»...); null si no nombra ninguno
export function colorDeNombre(nombre: string): string | null {
  const texto = normalizar(nombre);
  for (const grupo of COLORES_POR_NOMBRE) {
    for (const palabra of grupo.palabras) {
      if (tienePalabra(texto, palabra)) {
        return grupo.color;
      }
    }
  }
  return null;
}

// Base que nombra una opción («Base dorada», «Plato blanco», «Pedestal de cristal»...)
export function baseDeOpcion(nombre: string): TipoBase | null {
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['cristal', 'vidrio', 'pedestal', 'stand', 'base alta'])) {
    return 'cristal';
  }
  if (contieneAlguna(texto, ['madera', 'tabla', 'rustic'])) {
    return 'madera';
  }
  if (contieneAlguna(texto, ['pizarra', 'negra', 'piedra'])) {
    return 'pizarra';
  }
  if (contieneAlguna(texto, ['platead', 'plata'])) {
    return 'plateada';
  }
  if (contieneAlguna(texto, ['dorad', 'oro'])) {
    return 'dorada';
  }
  if (contieneAlguna(texto, ['plato', 'blanc', 'ceramica', 'porcelana', 'charola'])) {
    return 'blanca';
  }
  return null;
}

// Color con el que se dibuja cada base en su botón
const COLOR_DE_BASE: { base: TipoBase; color: string }[] = [
  { base: 'dorada', color: '#c9952f' },
  { base: 'plateada', color: '#c4c8ce' },
  { base: 'blanca', color: '#f7f5f0' },
  { base: 'madera', color: '#c99a6b' },
  { base: 'pizarra', color: '#3a3d42' },
  { base: 'cristal', color: '#d9eef5' },
];

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
  if (acabado === 'degradado') {
    // Degradado: rosa abajo que se aclara hacia arriba (si no se dice otro color)
    return '#f4a7b9';
  }
  if (acabado === 'marmol') {
    // Fondo blanco: las vetas del mármol resaltan
    return '#fbf7f1';
  }
  if (posicion < 0) {
    // Betún, buttercream, rústica y fondant: color crema (en el pastel toman el color del sabor)
    return '#fbf2dc';
  }
  return COLORES_COBERTURA[posicion % COLORES_COBERTURA.length];
}

function coberturaDeOpcion(nombre: string): TipoCobertura | null {
  const texto = normalizar(nombre);
  // Primero "sin cobertura": el nombre puede decir «sin betún» y no debe tomarse como betún
  if (contieneAlguna(texto, ['sin cobertura', 'sin betun', 'sin glaseado', 'sin frosting', 'sin copete', 'ninguna', 'ninguno', 'sin nada', 'al natural', 'solo pan', 'desnudo'])) {
    return 'ninguna';
  }
  if (contieneAlguna(texto, ['marmol', 'marmoleado', 'marmolado', 'marble'])) {
    return 'marmol';
  }
  if (contieneAlguna(texto, ['degradado', 'ombre', 'difuminado'])) {
    return 'degradado';
  }
  if (contieneAlguna(texto, ['naked', 'semi desnudo', 'semidesnudo'])) {
    return 'naked';
  }
  if (contieneAlguna(texto, ['fondant', 'liso', 'lisa', 'glaseado', 'glase', 'royal'])) {
    return 'lisa';
  }
  if (contieneAlguna(texto, ['chantilly', 'crema batida', 'nata'])) {
    return 'chantilly';
  }
  if (contieneAlguna(texto, ['ganache', 'espejo'])) {
    return 'ganache';
  }
  // Rústica: betún con pinceladas sueltas de espátula
  if (contieneAlguna(texto, ['rustic', 'pincelada', 'texturizad'])) {
    return 'rustica';
  }
  // Buttercream: betún de mantequilla peinado en franjas
  if (contieneAlguna(texto, ['buttercream', 'butter cream', 'mantequilla', 'peinad', 'franja'])) {
    return 'buttercream';
  }
  // La cobertura «de siempre» de la pastelería: betún de la casa
  if (contieneAlguna(texto, ['betun', 'merengue', 'queso crema', 'crema', 'defecto', 'predeterminad', 'normal', 'clasic', 'tradicional', 'sencill', 'basic', 'estandar', 'de la casa', 'original'])) {
    return 'betun';
  }
  // Una cobertura que solo dice «chispas» (de antes) se dibuja con betún y la cubierta de chispas
  if (contieneAlguna(texto, PALABRAS_CHISPAS)) {
    return 'betun';
  }
  return null;
}

// Palabras de las chispas (sirven para la decoración y para las coberturas «con chispas» de antes)
const PALABRAS_CHISPAS = ['chispa', 'sprinkle', 'grajea', 'confeti', 'granillo'];

// ¿El nombre de una cobertura o frosting trae chispas? («Betún con chispas», «Remolino con chispas»)
// Las chispas son una decoración: se dibujan como la cubierta de chispas de esa capa.
function coberturaConChispas(nombre: string): boolean {
  return contieneAlguna(normalizar(nombre), PALABRAS_CHISPAS);
}

// Acabado y color de una opción de cobertura. Siempre da algo, para que cada opción cambie el 3D.
// Acabado y color de una cobertura. "colorPropio" = el nombre ya dice su color (o la cobertura siempre es
// de un color, como la ganache); si no, en el pastel toma el color que combina con el sabor de su capa.
interface AspectoCobertura {
  acabado: TipoCobertura;
  color: string;
  colorPropio: boolean;
}

function aspectoCobertura(caracteristica: CaracteristicaPersonalizable, opcion: OpcionPersonalizable): AspectoCobertura {
  let posicion = -1;
  let acabado = coberturaDeOpcion(opcion.nombre);
  let colorPropio = false;
  if (acabado === null) {
    // Nombre que no se reconoce: betún liso con un color distinto por su lugar (nunca agrega piezas)
    acabado = 'betun';
    posicion = Math.max(0, caracteristica.opciones.indexOf(opcion));
    colorPropio = true;
  }
  // Estas coberturas tienen su propio color (el degradado es rosa y el mármol blanco si no se pide otro)
  if (acabado === 'ganache' || acabado === 'chantilly' || acabado === 'naked' || acabado === 'ninguna' || acabado === 'degradado' || acabado === 'marmol') {
    colorPropio = true;
  }
  let color = colorDeAcabado(acabado, posicion);
  // Si el nombre dice un sabor («Betún de fresa», «Ganache de chocolate blanco»), se usa su color
  const sabor = coloresDe(opcion.nombre, false);
  if (sabor.palabras.length > 0) {
    color = sabor.cobertura;
    colorPropio = true;
  }
  // Si el nombre dice un color («Fondant rosa», «Betún azul cielo»), manda ese color
  const nombrado = colorDeNombre(opcion.nombre);
  if (nombrado !== null && sabor.palabras.length === 0) {
    color = nombrado;
    colorPropio = true;
  }
  if (normalizar(opcion.nombre).indexOf('blanco') >= 0) {
    color = '#fbf5ee';
    colorPropio = true;
  }
  return { acabado: acabado, color: color, colorPropio: colorPropio };
}

// Palabras con las que se reconoce cada tipo de postre
const PALABRAS_POSTRE: { tipo: TipoPostre3d; palabras: string[] }[] = [
  { tipo: 'gelatina', palabras: ['gelatina', 'flan', 'jalea'] },
  { tipo: 'cheesecake', palabras: ['cheesecake', 'cheese cake', 'chescake', 'tarta de queso'] },
  { tipo: 'macaron', palabras: ['macaron', 'macarron', 'macaroon'] },
  { tipo: 'cupcake', palabras: ['cupcake', 'cup cake', 'panque', 'magdalena', 'muffin', 'mantecada'] },
  { tipo: 'galleta', palabras: ['galleta', 'cookie', 'polvoron'] },
  { tipo: 'brownie', palabras: ['brownie', 'blondie'] },
  { tipo: 'dona', palabras: ['dona', 'donut', 'dona glaseada', 'rosquilla'] },
  { tipo: 'rosca', palabras: ['rosca', 'bundt', 'savarin', 'roscon'] },
  { tipo: 'pay', palabras: ['pay', 'pie', 'tarta', 'tartaleta', 'kuchen', 'strudel'] },
  { tipo: 'pastel', palabras: ['pastel', 'torta', 'cake'] },
];

// ¿El texto trae la palabra completa? («pie» no debe encontrarse dentro de «pieza»)
function tienePalabra(texto: string, palabra: string): boolean {
  const partes = (' ' + texto + ' ').replace(/[^a-z0-9]+/g, ' ');
  return partes.indexOf(' ' + palabra + ' ') >= 0 || partes.indexOf(' ' + palabra + 's ') >= 0 || partes.indexOf(' ' + palabra + 'es ') >= 0;
}

function tipoEnTexto(texto: string): TipoPostre3d | null {
  const limpio = normalizar(texto);
  for (const grupo of PALABRAS_POSTRE) {
    for (const palabra of grupo.palabras) {
      if (tienePalabra(limpio, palabra)) {
        return grupo.tipo;
      }
    }
  }
  return null;
}

/**
 * RF-5 · Paso 2: tipo de postre para el 3D. Manda la categoría (así un «Pastel de galleta» sigue siendo pastel);
 * si la categoría no se reconoce, se busca en el nombre del producto. Si nada coincide: pastel.
 */
export function tipoDePostre(categoria: string, nombre: string): TipoPostre3d {
  const porCategoria = tipoEnTexto(categoria);
  if (porCategoria !== null) {
    return porCategoria;
  }
  const porNombre = tipoEnTexto(nombre);
  if (porNombre !== null) {
    return porNombre;
  }
  return 'pastel';
}

// ¿El postre es una gelatina? (por su categoría o su nombre)
export function esGelatina(categoria: string, nombre: string): boolean {
  return tipoDePostre(categoria, nombre) === 'gelatina';
}

// Postres chicos de una pieza (galleta, brownie, dona, macaron): se ve uno solo y el tamaño lo hace
// más grande, igual que el cupcake (que se arma aparte por su capacillo y su frosting).
export function esPiezaChica(tipo: TipoPostre3d): boolean {
  return tipo === 'galleta' || tipo === 'brownie' || tipo === 'dona' || tipo === 'macaron';
}

// Cómo se dibuja una opción en su botón (color, forma o tamaño)
export function aparienciaDeOpcion(
  caracteristica: CaracteristicaPersonalizable,
  opcion: OpcionPersonalizable,
  gelatina: boolean,
): AparienciaOpcion {
  const apariencia: AparienciaOpcion = { tipo: 'ninguna', color: '', forma: 'redondo', nivel: 0, molde: 'figura' };
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
  if (tipo === 'cobertura' || tipo === 'frosting') {
    apariencia.tipo = 'color';
    apariencia.color = aspectoCobertura(caracteristica, opcion).color;
    return apariencia;
  }
  if (tipo === 'color') {
    const color = colorDeNombre(opcion.nombre);
    if (color !== null) {
      apariencia.tipo = 'color';
      apariencia.color = color;
    }
    return apariencia;
  }
  if (tipo === 'base') {
    const base = baseDeOpcion(opcion.nombre);
    for (const muestra of COLOR_DE_BASE) {
      if (muestra.base === base) {
        apariencia.tipo = 'color';
        apariencia.color = muestra.color;
      }
    }
    return apariencia;
  }
  if (tipo === 'forma') {
    const forma = formaDeOpcion(opcion.nombre);
    if (forma !== null) {
      apariencia.tipo = 'forma';
      apariencia.forma = forma;
    }
    if (gelatina) {
      // Gelatinas: el botón dibuja el molde (rosca, domo, gajos) o la figura
      const molde = moldeDeOpcion(opcion.nombre);
      if (molde !== null) {
        apariencia.tipo = 'forma';
        apariencia.molde = molde;
      }
    }
    return apariencia;
  }
  if (tipo === 'sabor' || tipo === 'relleno') {
    const color = colorDeOpcion(opcion.nombre, gelatina && tipo === 'sabor', tipo === 'relleno');
    if (color !== null) {
      apariencia.tipo = 'color';
      apariencia.color = color;
    }
  }
  return apariencia;
}

// ===================== Decoraciones =====================

function decoracionesVacias(): DecoracionesPastel {
  return {
    mono: 0,
    liston: 0,
    flores: 0,
    perlas: 0,
    chispas: 0,
    chispasTodo: 0,
    goteo: 0,
    rosetas: 0,
    velas: 0,
    fresas: 0,
    frutaPicada: 0,
    frutosRojos: 0,
    hojaOro: 0,
  };
}

// Texto del topper: el nombre sin la palabra «topper» («Topper Feliz cumpleaños» → «Feliz cumpleaños»)
export function textoTopper(nombre: string): string {
  let texto = nombre.replace(/cake\s*topper|topper|letrero/gi, ' ').replace(/\s+/g, ' ').trim();
  texto = texto.replace(/^(de|con)\s+/i, '').replace(/^[-:·]\s*/, '').trim();
  if (texto === '') {
    return '¡Felicidades!';
  }
  return texto;
}

// Límite de piezas por decoración (para que el 3D no se sature)
const MAXIMO_PIEZAS = 40;

function sumar(actual: number, cantidad: number): number {
  return Math.min(MAXIMO_PIEZAS, actual + cantidad);
}

// Decoración que cubre toda la capa de chispas (antes «Cobertura de chispas»)
const PALABRAS_CHISPAS_TODO = ['cubierta de chispa', 'cubierto de chispa', 'cobertura de chispa', 'con chispa', 'chispas en todo', 'chispas alrededor', 'chispas a los lados', 'todo de chispa'];

// Suma una decoración elegida "cantidad" veces: cada unidad es una pieza en el 3D
function agregarDecoracion(diseno: DisenoPastel, nombre: string, cantidad: number): void {
  const decoraciones = diseno.decoraciones;
  const texto = normalizar(nombre);
  if (contieneAlguna(texto, ['topper', 'letrero'])) {
    diseno.topper = textoTopper(nombre);
  } else if (contieneAlguna(texto, ['vela'])) {
    decoraciones.velas = sumar(decoraciones.velas, cantidad);
  } else if (contieneAlguna(texto, ['mono'])) {
    decoraciones.mono = sumar(decoraciones.mono, cantidad);
  } else if (contieneAlguna(texto, ['liston', 'cinta'])) {
    decoraciones.liston = sumar(decoraciones.liston, cantidad);
  } else if (contieneAlguna(texto, ['flor'])) {
    decoraciones.flores = sumar(decoraciones.flores, cantidad);
  } else if (contieneAlguna(texto, ['perla'])) {
    decoraciones.perlas = sumar(decoraciones.perlas, cantidad);
  } else if (contieneAlguna(texto, ['hoja de oro', 'hojas de oro', 'oro comestible', 'laminilla', 'pan de oro', 'hojuela'])) {
    decoraciones.hojaOro = sumar(decoraciones.hojaOro, cantidad);
  } else if (contieneAlguna(texto, PALABRAS_CHISPAS_TODO)) {
    // Cubierta de chispas: chispas en los costados y arriba de la capa
    decoraciones.chispasTodo = sumar(decoraciones.chispasTodo, cantidad);
  } else if (contieneAlguna(texto, PALABRAS_CHISPAS)) {
    decoraciones.chispas = sumar(decoraciones.chispas, cantidad);
  } else if (contieneAlguna(texto, ['goteo', 'ganache', 'drip'])) {
    decoraciones.goteo = sumar(decoraciones.goteo, cantidad);
  } else if (contieneAlguna(texto, ['frutos rojos', 'frutos del bosque', 'mora', 'arandano', 'frambuesa', 'blueberr'])) {
    decoraciones.frutosRojos = sumar(decoraciones.frutosRojos, cantidad);
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

// ===================== Capas (pasteles de 2 o más pisos) =====================

/**
 * RF-5 · Paso 3: características que se eligen por separado en cada capa del pastel:
 * Sabor, Cobertura, Color de cobertura y Decoración. Las demás (Tamaño, Pisos, Forma, Relleno, Base)
 * son del pastel completo.
 */
export function esPorCapa(caracteristica: CaracteristicaPersonalizable): boolean {
  const tipo = tipoDeCaracteristica(caracteristica.nombre);
  return tipo === 'sabor' || tipo === 'cobertura' || tipo === 'color' || tipo === 'decoracion';
}

// Capa de una opción elegida (las que no traen capa son de la Base)
export function capaDe(elegida: OpcionElegida): number {
  if (elegida.capa === undefined || elegida.capa === null || elegida.capa < 1) {
    return 1;
  }
  return elegida.capa;
}

// Nombre de una capa para los botones: Base, Capa 2, Capa 3... y Superior (la de hasta arriba)
export function nombreCapa(capa: number, total: number): string {
  if (total <= 1) {
    return 'Única';
  }
  if (capa === 1) {
    return 'Base';
  }
  if (capa === total) {
    return 'Superior';
  }
  return 'Capa ' + capa;
}

/**
 * RF-5 · Paso 3: cuántas capas se editan por separado: los pisos elegidos en los pasteles
 * (1 en los demás postres, que no tienen pisos).
 */
export function capasDelPostre(
  caracteristicas: CaracteristicaPersonalizable[],
  seleccion: OpcionElegida[],
  categoria: string,
  nombreProducto: string,
): number {
  if (tipoDePostre(categoria, nombreProducto) !== 'pastel') {
    return 1;
  }
  let pisos = 1;
  for (const caracteristica of caracteristicas) {
    if (tipoDeCaracteristica(caracteristica.nombre) !== 'pisos') {
      continue;
    }
    for (const elegida of seleccion) {
      const opcion = buscarOpcionEn(caracteristica, elegida.idOpcion);
      if (opcion !== null && elegida.cantidad > 0) {
        pisos = pisosDeOpcion(caracteristica, opcion);
      }
    }
  }
  return pisos;
}

/**
 * Lo elegido que le toca a una capa: todo lo del pastel completo (tamaño, pisos, forma...) más lo que
 * se eligió en esa capa (sabor, cobertura, color y decoración).
 */
export function seleccionDeCapa(
  caracteristicas: CaracteristicaPersonalizable[],
  seleccion: OpcionElegida[],
  capa: number,
): OpcionElegida[] {
  const resultado: OpcionElegida[] = [];
  for (const elegida of seleccion) {
    let porCapa = false;
    for (const caracteristica of caracteristicas) {
      if (buscarOpcionEn(caracteristica, elegida.idOpcion) !== null && esPorCapa(caracteristica)) {
        porCapa = true;
      }
    }
    if (!porCapa || capaDe(elegida) === capa) {
      resultado.push(elegida);
    }
  }
  return resultado;
}

// ===================== Diseño completo =====================

// Diseño con los valores normales de cada postre (antes de ver lo que eligió el cliente)
function disenoInicial(tipo: TipoPostre3d, dedicatoria: string): DisenoPastel {
  const gelatina = tipo === 'gelatina';
  let colores = COLOR_NORMAL;
  if (gelatina) {
    colores = COLOR_GELATINA_NORMAL;
  }
  const diseno: DisenoPastel = {
    tipo: tipo,
    forma: 'redondo',
    molde: 'gajos',
    pisos: 1,
    diametro: 24,
    alturaPiso: 10,
    colorBizcocho: colores.bizcocho,
    colorCobertura: colores.cobertura,
    colorRelleno: '#e8657a',
    mosaico: false,
    cobertura: 'betun',
    base: baseNormal(tipo),
    decoraciones: decoracionesVacias(),
    dedicatoria: dedicatoria.trim(),
    topper: '',
    porCapas: false,
    capas: [],
  };
  if (gelatina) {
    diseno.pisos = 1;
    diseno.diametro = 20;
    diseno.alturaPiso = 9;
    diseno.cobertura = 'lisa';
  }
  if (tipo === 'pay') {
    // En el pay el "relleno" es lo que se ve arriba: por defecto, de queso
    diseno.diametro = 24;
    diseno.alturaPiso = 4;
    diseno.colorRelleno = '#f4e2b8';
  }
  if (tipo === 'cheesecake') {
    // Cheesecake: alto, de queso, con base de galleta
    diseno.diametro = 20;
    diseno.alturaPiso = 7;
    diseno.colorRelleno = '#f6e7c4';
  }
  if (tipo === 'rosca') {
    diseno.diametro = 24;
    diseno.alturaPiso = 8;
  }
  if (tipo === 'cupcake') {
    // En el cupcake el "diámetro" es lo ancho del capacillo
    diseno.diametro = DIAMETROS_CUPCAKE[1];
  }
  if (tipo === 'macaron') {
    diseno.colorBizcocho = '#f4b8c8';
    diseno.colorRelleno = '#fbf3e6';
  }
  if (tipo === 'brownie') {
    diseno.colorBizcocho = '#4a2717';
  }
  if (tipo === 'galleta') {
    diseno.colorBizcocho = '#e0b072';
  }
  if (esPiezaChica(tipo)) {
    // Ancho de la pieza mediana
    diseno.diametro = ANCHOS_PIEZA_CHICA[tipo][1];
  }
  return diseno;
}

// Lo que cambia en una capa, tomado de un diseño ya armado
function capaDesde(diseno: DisenoPastel): CapaPastel {
  return {
    colorBizcocho: diseno.colorBizcocho,
    colorCobertura: diseno.colorCobertura,
    colorRelleno: diseno.colorRelleno,
    cobertura: diseno.cobertura,
    decoraciones: diseno.decoraciones,
  };
}

// Suma las decoraciones de todas las capas (las velas y el topper van siempre hasta arriba)
function sumarDecoraciones(capas: CapaPastel[]): DecoracionesPastel {
  const total = decoracionesVacias();
  for (const capa of capas) {
    const d = capa.decoraciones;
    total.mono = sumar(total.mono, d.mono);
    total.liston = sumar(total.liston, d.liston);
    total.flores = sumar(total.flores, d.flores);
    total.perlas = sumar(total.perlas, d.perlas);
    total.chispas = sumar(total.chispas, d.chispas);
    total.chispasTodo = sumar(total.chispasTodo, d.chispasTodo);
    total.goteo = sumar(total.goteo, d.goteo);
    total.rosetas = sumar(total.rosetas, d.rosetas);
    total.velas = sumar(total.velas, d.velas);
    total.fresas = sumar(total.fresas, d.fresas);
    total.frutaPicada = sumar(total.frutaPicada, d.frutaPicada);
    total.frutosRojos = sumar(total.frutosRojos, d.frutosRojos);
    total.hojaOro = sumar(total.hojaOro, d.hojaOro);
  }
  return total;
}

/**
 * RF-5 · Paso 3: arma el diseño 3D con lo que el cliente tiene elegido.
 * No cambia nada de la selección ni del precio: solo se usa para dibujar.
 * En los pasteles de 2 o más pisos cada capa se arma con lo que se eligió en ella.
 */
export function disenoDesdeSeleccion(
  caracteristicas: CaracteristicaPersonalizable[],
  seleccion: OpcionElegida[],
  categoria: string,
  nombreProducto: string,
  dedicatoria: string,
  mensajeEnTopper: boolean = false,
): DisenoPastel {
  const tipo = tipoDePostre(categoria, nombreProducto);
  const capas = capasDelPostre(caracteristicas, seleccion, categoria, nombreProducto);

  // La Base (o el postre completo si no tiene pisos)
  const diseno = disenoInicial(tipo, dedicatoria);
  aplicarSeleccion(diseno, caracteristicas, seleccionDeCapa(caracteristicas, seleccion, 1));
  diseno.capas = [capaDesde(diseno)];
  diseno.porCapas = capas > 1;

  // Las demás capas: cada una con su sabor, cobertura, color y decoración
  for (let capa = 2; capa <= capas; capa++) {
    const otra = disenoInicial(tipo, '');
    aplicarSeleccion(otra, caracteristicas, seleccionDeCapa(caracteristicas, seleccion, capa));
    diseno.capas.push(capaDesde(otra));
  }
  if (diseno.porCapas) {
    diseno.decoraciones = sumarDecoraciones(diseno.capas);
  }

  // RF-5 · Excepción paso 4: el mensaje va en un topper (letrero de pie) si el cliente lo pidió o si el
  // postre solo lo admite así
  if (diseno.dedicatoria !== '' && mensajeVaEnTopper(tipo, mensajeEnTopper)) {
    diseno.topper = diseno.dedicatoria;
    diseno.dedicatoria = '';
  }
  return diseno;
}

// Aplica al diseño las opciones elegidas (de una capa o del postre completo)
function aplicarSeleccion(diseno: DisenoPastel, caracteristicas: CaracteristicaPersonalizable[], seleccion: OpcionElegida[]): void {
  const tipo = diseno.tipo;
  const gelatina = tipo === 'gelatina';
  let coberturaElegida = false;
  let rellenoElegido = false;
  // Color pedido con la característica «Color de cobertura» (se aplica al final)
  let colorPedido: string | null = null;
  // Color de betún que combina con el sabor elegido, y si la cobertura elegida lo usa (como en la
  // captura: «Color sugerido para chocolate»)
  let colorDelSabor: string | null = null;
  let coberturaSinColor = false;
  // ¿El producto deja elegir base? Si la deja y no se eligió ninguna («Sin base»), no se dibuja base
  let tieneBase = false;
  let baseElegida = false;
  // ¿El producto deja elegir cobertura? Si la deja y no se eligió ninguna, el postre va sin cobertura
  let tieneCobertura = false;
  for (const caracteristica of caracteristicas) {
    const tipoCaracteristica = tipoDeCaracteristica(caracteristica.nombre);
    if (tipoCaracteristica === 'cobertura' || tipoCaracteristica === 'frosting') {
      tieneCobertura = true;
    }
    if (tipoCaracteristica === 'base') {
      tieneBase = true;
    }
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
        if (tipo === 'pastel') {
          diseno.pisos = pisosDeOpcion(caracteristica, opcion);
        }
      } else if (tipoCaracteristica === 'forma') {
        const forma = formaDeOpcion(opcion.nombre);
        if (forma !== null) {
          diseno.forma = forma;
        }
        if (gelatina) {
          const molde = moldeDeOpcion(opcion.nombre);
          if (molde !== null) {
            diseno.molde = molde;
          }
        }
      } else if (tipoCaracteristica === 'sabor') {
        const sabor = coloresDe(opcion.nombre, gelatina);
        diseno.colorBizcocho = sabor.bizcocho;
        colorDelSabor = sabor.cobertura;
        if (!coberturaElegida) {
          diseno.colorCobertura = sabor.cobertura;
        }
        diseno.mosaico = normalizar(opcion.nombre).indexOf('mosaico') >= 0;
        if ((tipo === 'pay' || tipo === 'cheesecake') && !rellenoElegido) {
          // Pay de fresa, de limón, de queso...: el sabor se ve en el relleno
          // (en el cheesecake, en la capa de fruta de arriba)
          diseno.colorRelleno = sabor.relleno;
        }
        if (tipo === 'macaron') {
          // Las tapas del macaron toman el color pastel del sabor
          diseno.colorBizcocho = sabor.cobertura;
          diseno.colorRelleno = sabor.relleno;
        }
      } else if (tipoCaracteristica === 'relleno') {
        diseno.colorRelleno = coloresDe(opcion.nombre, false).relleno;
        rellenoElegido = true;
      } else if (tipoCaracteristica === 'cobertura' || tipoCaracteristica === 'frosting') {
        // En el cupcake el «Frosting» es lo de arriba; en los demás postres funciona como cobertura
        const aspecto = aspectoCobertura(caracteristica, opcion);
        diseno.cobertura = aspecto.acabado;
        diseno.colorCobertura = aspecto.color;
        coberturaElegida = true;
        coberturaSinColor = !aspecto.colorPropio;
        if (coberturaConChispas(opcion.nombre)) {
          // Las chispas son una decoración: una cobertura «con chispas» de antes se ve como cubierta de chispas
          diseno.decoraciones.chispasTodo = sumar(diseno.decoraciones.chispasTodo, 1);
        }
      } else if (tipoCaracteristica === 'color') {
        colorPedido = colorDeNombre(opcion.nombre);
      } else if (tipoCaracteristica === 'base') {
        const base = baseDeOpcion(opcion.nombre);
        if (base !== null) {
          diseno.base = base;
          baseElegida = true;
        }
      } else if (tipoCaracteristica === 'decoracion') {
        agregarDecoracion(diseno, opcion.nombre, piezas);
      } else {
        // RF-5 · Excepción paso 3: característica sin tipo reconocido: si la opción nombra una forma o
        // decoración, se usa; si no, no se dibuja (sí cuenta para el precio y el pedido)
        const forma = formaDeOpcion(opcion.nombre);
        if (forma !== null) {
          diseno.forma = forma;
        } else {
          agregarDecoracion(diseno, opcion.nombre, piezas);
        }
      }
    }
  }

  // La ganache es de chocolate aunque el sabor sea otro
  if (diseno.cobertura === 'ganache' && !coberturaElegida) {
    diseno.colorCobertura = '#4a2617';
  }
  // RF-5 · Excepción paso 3: si no se eligió cobertura, se usa el acabado por defecto del postre
  aplicarCoberturaPorDefecto(diseno, tieneCobertura, coberturaElegida);
  if (tieneBase && !baseElegida) {
    diseno.base = 'ninguna';
  }
  // Betún, buttercream, rústica o fondant sin color en el nombre: el color que combina con el sabor
  if (coberturaSinColor && colorDelSabor !== null && tipo !== 'gelatina') {
    diseno.colorCobertura = colorDelSabor;
  }
  if (colorPedido !== null && diseno.cobertura !== 'ninguna') {
    diseno.colorCobertura = colorPedido;
  }
  if (tipo === 'macaron' && colorPedido !== null) {
    // En los macarons el color pedido pinta las tapas
    diseno.colorBizcocho = colorPedido;
  }
}


/**
 * RF-3 · Paso 3: dónde puede ir el mensaje de cada postre (casilla «Ponerlo en un topper»):
 * - 'elegir': escrito sobre el postre (o en una placa) o en un topper, como quiera el cliente
 *   (pasteles, pays, cheesecakes y roscas)
 * - 'topper': solo en un topper (gelatinas, porque no se puede escribir sobre ellas, y los postres
 *   chicos: cupcakes, galletas, brownies, donas y macarons)
 */
export function lugarDelMensaje(tipo: TipoPostre3d): 'elegir' | 'topper' {
  if (tipo === 'gelatina' || tipo === 'cupcake' || esPiezaChica(tipo)) {
    return 'topper';
  }
  return 'elegir';
}

// ¿El mensaje se pone en un topper? (lo elegido por el cliente, si el postre deja elegir)
export function mensajeVaEnTopper(tipo: TipoPostre3d, elegidoTopper: boolean): boolean {
  if (lugarDelMensaje(tipo) === 'topper') {
    return true;
  }
  return elegidoTopper;
}

// Base de cada postre si el cliente no elige una
function baseNormal(tipo: TipoPostre3d): TipoBase {
  if (tipo === 'pastel') {
    return 'dorada';
  }
  if (esPiezaChica(tipo)) {
    return 'madera';
  }
  return 'blanca';
}

/**
 * Cobertura cuando el cliente no eligió ninguna:
 *  - Si el producto tiene la característica «Cobertura» (o «Frosting») y no se eligió nada → sin cobertura.
 *  - Si el producto no tiene esa característica, lleva la de siempre de cada postre:
 *    pastel con betún de la casa, dona glaseada, y los demás al natural.
 *    El cupcake va sin nada arriba: su parte de arriba es la característica «Frosting».
 * "Sin cobertura" toma el color del pan, así todo lo demás (listón, mensaje) combina.
 */
function aplicarCoberturaPorDefecto(diseno: DisenoPastel, tieneCobertura: boolean, coberturaElegida: boolean): void {
  if (diseno.tipo === 'gelatina') {
    return;
  }
  if (!coberturaElegida) {
    if (tieneCobertura) {
      diseno.cobertura = 'ninguna';
    } else if (diseno.tipo === 'dona') {
      diseno.cobertura = 'lisa';
    } else if (diseno.tipo !== 'pastel') {
      // Cupcake, pay, cheesecake, galleta, brownie, rosca y macaron se ven al natural
      diseno.cobertura = 'ninguna';
    }
  }
  if (diseno.cobertura === 'ninguna') {
    diseno.colorCobertura = diseno.colorBizcocho;
  }
}

const DIAMETROS_PASTEL = [18, 24, 30, 36];
const ALTURAS_PASTEL = [9, 10, 11, 12];
const DIAMETROS_GELATINA = [12, 18, 24, 28];
const ALTURAS_GELATINA = [7, 9, 11, 12];
const DIAMETROS_PAY = [18, 22, 26, 30];
const DIAMETROS_CHEESECAKE = [16, 20, 24, 28];
const DIAMETROS_ROSCA = [18, 22, 26, 30];
// Ancho del capacillo del cupcake: mini, normal, grande, jumbo
const DIAMETROS_CUPCAKE = [5.4, 6.6, 7.6, 8.6];
// Ancho de cada postre chico de una pieza: mini, mediano, grande, jumbo
const ANCHOS_PIEZA_CHICA: Record<string, number[]> = {
  galleta: [7, 8.5, 10, 12],
  brownie: [5, 6, 7, 8.5],
  dona: [7, 8.5, 9.5, 11],
  macaron: [3.6, 4.4, 5.2, 6],
};

// Diámetro en centímetros de cada tamaño (1 = chico ... 4 = extra grande)
function diametroDeNivel(nivel: number, gelatina: boolean): number {
  if (gelatina) {
    return DIAMETROS_GELATINA[nivel - 1];
  }
  return DIAMETROS_PASTEL[nivel - 1];
}

// Medidas según el tamaño. El tamaño NO cambia los pisos.
function aplicarTamano(diseno: DisenoPastel, nivel: number): void {
  if (diseno.tipo === 'cupcake') {
    // Siempre es un solo cupcake: el tamaño lo hace más grande
    diseno.diametro = DIAMETROS_CUPCAKE[nivel - 1];
    return;
  }
  if (esPiezaChica(diseno.tipo)) {
    // Una sola galleta, brownie, dona o macaron: el tamaño la hace más grande
    diseno.diametro = ANCHOS_PIEZA_CHICA[diseno.tipo][nivel - 1];
    return;
  }
  if (diseno.tipo === 'cheesecake') {
    diseno.diametro = DIAMETROS_CHEESECAKE[nivel - 1];
    diseno.alturaPiso = 7;
    return;
  }
  if (diseno.tipo === 'rosca') {
    diseno.diametro = DIAMETROS_ROSCA[nivel - 1];
    diseno.alturaPiso = 7 + nivel * 0.5;
    return;
  }
  if (diseno.tipo === 'pay') {
    // El pay es bajito: el tamaño solo lo hace más ancho
    diseno.diametro = DIAMETROS_PAY[nivel - 1];
    diseno.alturaPiso = 4;
    return;
  }
  const gelatina = diseno.tipo === 'gelatina';
  diseno.diametro = diametroDeNivel(nivel, gelatina);
  if (gelatina) {
    diseno.alturaPiso = ALTURAS_GELATINA[nivel - 1];
  } else {
    diseno.alturaPiso = ALTURAS_PASTEL[nivel - 1];
  }
}
