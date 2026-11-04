import * as THREE from 'three';
import { DisenoPastel, TipoBase } from './diseno-pastel';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { FormaPastel, Punto2d, estaDentro, puntosForma } from './formas-pastel';

/**
 * RF-5 · Paso 4: construye el pastel o la gelatina en 3D (three.js) a partir de un DisenoPastel.
 * Medidas en centímetros. El piso de abajo se apoya en una base (plato).
 *
 * Coordenadas: la forma 2D (x, y) se acuesta en el piso; "y" de la forma queda hacia -z del mundo.
 * La cámara mira desde +z, así que el frente del pastel es el lado con "y" negativa.
 */

// Punto del contorno con la dirección hacia afuera (normal)
interface PuntoContorno {
  x: number;
  y: number;
  nx: number;
  ny: number;
}

export interface PastelConstruido {
  grupo: THREE.Group;
  alturaTotal: number;
  anchoTotal: number;
  // Llamas de las velas (se mueven en cada cuadro)
  llamas: THREE.Mesh[];
}

// Rebanada que se quita con «Ver por dentro» (ángulos de la forma 2D, en radianes)
export const CORTE_INICIO = -Math.PI / 2 + 0.3;
export const CORTE_FIN = -Math.PI / 2 + 1.7;

// Ancho de la banda del listón, en centímetros
const ANCHO_LISTON = 2.2;

export const COLORES_CHISPAS = ['#f06292', '#4fc3f7', '#ffd54f', '#81c784', '#ba68c8', '#ffffff', '#ff8a65'];
const COLORES_MOSAICO = ['#e2334f', '#9fd356', '#f59a2c', '#f4d03f', '#8e2f7a'];
// Fruta picada: fresa, kiwi, mango, piña y melón (en cubitos) y moras o uvas (bolitas)
export const COLORES_FRUTA = ['#d7263d', '#7cb342', '#f6a21a', '#f4d03f', '#f3a96b'];
const COLORES_BAYAS = ['#2f3a78', '#4a1f45', '#9bbf4a'];

// ===================== Utilidades =====================

// Números pseudoaleatorios repetibles (el pastel se ve igual cada vez que se arma)
export class Aleatorio {
  private semilla: number;

  constructor(semilla: number) {
    this.semilla = semilla;
  }

  siguiente(): number {
    this.semilla = (this.semilla * 16807) % 2147483647;
    return this.semilla / 2147483647;
  }
}

export function material(color: string, aspereza: number, metal: number): THREE.MeshStandardMaterial {
  return new THREE.MeshStandardMaterial({ color: color, roughness: aspereza, metalness: metal });
}

export function conSombras(objeto: THREE.Object3D): void {
  objeto.traverse(marcarSombra);
}

function marcarSombra(objeto: THREE.Object3D): void {
  const malla = objeto as THREE.Mesh;
  if (malla.isMesh) {
    // El cristal deja pasar la luz: no echa sombra
    malla.castShadow = malla.userData['sinSombra'] !== true;
    malla.receiveShadow = true;
  }
}

// Forma de three.js a partir de los puntos
function formaThree(puntos: Punto2d[]): THREE.Shape {
  const vectores: THREE.Vector2[] = [];
  for (const punto of puntos) {
    vectores.push(new THREE.Vector2(punto.x, punto.y));
  }
  return new THREE.Shape(vectores);
}

// Ángulo entre 0 y 2π
function anguloPositivo(angulo: number): number {
  let resultado = angulo % (Math.PI * 2);
  if (resultado < 0) {
    resultado = resultado + Math.PI * 2;
  }
  return resultado;
}

function dentroDelCorte(angulo: number): boolean {
  // Ángulo medido desde el inicio del corte (así no importa si el corte cruza el 0)
  const desdeInicio = anguloPositivo(angulo - CORTE_INICIO);
  return desdeInicio > 0 && desdeInicio < CORTE_FIN - CORTE_INICIO;
}

/**
 * Planos que limitan la rebanada de «Ver por dentro», en coordenadas del mundo.
 * Con clipIntersection, three.js borra solo lo que queda detrás de los DOS planos,
 * es decir, lo que está dentro de la rebanada. Los planos pasan por el eje del pastel,
 * así que siguen sirviendo aunque el pastel crezca o se achique (animación de aparecer).
 */
function planosDelCorte(): THREE.Plane[] {
  const inicio = new THREE.Plane(new THREE.Vector3(Math.sin(CORTE_INICIO), 0, Math.cos(CORTE_INICIO)), 0);
  const fin = new THREE.Plane(new THREE.Vector3(-Math.sin(CORTE_FIN), 0, -Math.cos(CORTE_FIN)), 0);
  return [inicio, fin];
}

// ¿El punto del mundo (x, z) cae dentro de la rebanada? (la "y" de la forma es -z del mundo)
function puntoDentroDelCorte(x: number, z: number): boolean {
  return dentroDelCorte(Math.atan2(-z, x));
}

// Junta todas las piezas visibles (mallas, brillos y luces) de un grupo
function juntarPiezas(objeto: THREE.Object3D, lista: THREE.Object3D[]): void {
  const pieza = objeto as THREE.Mesh & THREE.Sprite & THREE.Light;
  if (pieza.isMesh || pieza.isSprite || pieza.isLight) {
    lista.push(objeto);
  }
  for (const hijo of objeto.children) {
    juntarPiezas(hijo, lista);
  }
}

/**
 * «Ver por dentro»: lo que está en la rebanada se quita o se recorta, para que no quede nada flotando.
 * - Las piezas chicas (velas, fresas, flores, moños...) que caen dentro de la rebanada se quitan completas.
 * - Las piezas largas (listón, goteo, mensaje, chispas...) se recortan justo en la orilla del corte.
 * - El cuerpo del piso y sus paredes del corte ya vienen cortados (userData.sinRecorte).
 */
export function recortarRebanada(piso: THREE.Object3D): void {
  piso.updateMatrixWorld(true);
  const planos = planosDelCorte();
  const piezas: THREE.Object3D[] = [];
  juntarPiezas(piso, piezas);
  for (const pieza of piezas) {
    if (pieza.userData['sinRecorte'] === true) {
      continue;
    }
    const malla = pieza as THREE.InstancedMesh;
    if (malla.isInstancedMesh) {
      // Copias chicas (perlas, chispas, fresas...): se quitan las que caen en la rebanada
      quitarInstanciasDelCorte(malla);
      continue;
    }
    // Centro y medio ancho de la pieza vista desde arriba
    const centro = new THREE.Vector3();
    let radio = 0;
    const conGeometria = pieza as THREE.Mesh;
    if (conGeometria.isMesh) {
      conGeometria.geometry.computeBoundingBox();
      const caja = (conGeometria.geometry.boundingBox as THREE.Box3).clone().applyMatrix4(pieza.matrixWorld);
      caja.getCenter(centro);
      const medida = caja.getSize(new THREE.Vector3());
      radio = Math.hypot(medida.x, medida.z) / 2;
    } else {
      pieza.getWorldPosition(centro);
      radio = pieza.getWorldScale(new THREE.Vector3()).x / 2;
    }
    const distanciaAlEje = Math.hypot(centro.x, centro.z);
    const esChica = radio < 4.5;
    if (radio < 1.5 && distanciaAlEje < 0.6) {
      // Pieza justo en el centro (una vela sola): se queda completa, no cortada a la mitad
      continue;
    }
    if (esChica && distanciaAlEje > 0.6 && puntoDentroDelCorte(centro.x, centro.z)) {
      pieza.removeFromParent();
      continue;
    }
    if (conGeometria.isMesh) {
      ponerRecorte(conGeometria.material, planos);
    }
  }
}

// Quita las copias (perlas, chispas, fresas...) cuyo centro cae dentro de la rebanada
function quitarInstanciasDelCorte(malla: THREE.InstancedMesh): void {
  const matriz = new THREE.Matrix4();
  const posicion = new THREE.Vector3();
  const color = new THREE.Color();
  let quedan = 0;
  for (let i = 0; i < malla.count; i++) {
    malla.getMatrixAt(i, matriz);
    posicion.setFromMatrixPosition(matriz).applyMatrix4(malla.matrixWorld);
    if (Math.hypot(posicion.x, posicion.z) > 0.6 && puntoDentroDelCorte(posicion.x, posicion.z)) {
      continue;
    }
    malla.setMatrixAt(quedan, matriz);
    if (malla.instanceColor !== null) {
      malla.getColorAt(i, color);
      malla.setColorAt(quedan, color);
    }
    quedan = quedan + 1;
  }
  malla.count = quedan;
  malla.instanceMatrix.needsUpdate = true;
  if (malla.instanceColor !== null) {
    malla.instanceColor.needsUpdate = true;
  }
}

function ponerRecorte(materiales: THREE.Material | THREE.Material[], planos: THREE.Plane[]): void {
  let lista: THREE.Material[] = [];
  if (Array.isArray(materiales)) {
    lista = materiales;
  } else {
    lista = [materiales];
  }
  for (const uno of lista) {
    uno.clippingPlanes = planos;
    uno.clipIntersection = true;
    // La sombra también se recorta (si no, se vería la sombra de lo que ya no está)
    uno.clipShadows = true;
  }
}

// Contorno con la rebanada quitada (el centro queda como un vértice)
function puntosConCorte(forma: FormaPastel, tamano: number): Punto2d[] {
  const puntos = puntosForma(forma, tamano, 180);
  // Se empieza justo después del corte para que la rebanada quede al final
  let inicio = 0;
  for (let i = 0; i < puntos.length; i++) {
    const angulo = Math.atan2(puntos[i].y, puntos[i].x);
    const anterior = puntos[(i - 1 + puntos.length) % puntos.length];
    const anguloAnterior = Math.atan2(anterior.y, anterior.x);
    if (!dentroDelCorte(angulo) && dentroDelCorte(anguloAnterior)) {
      inicio = i;
    }
  }
  const resultado: Punto2d[] = [];
  for (let k = 0; k < puntos.length; k++) {
    const punto = puntos[(inicio + k) % puntos.length];
    if (!dentroDelCorte(Math.atan2(punto.y, punto.x))) {
      resultado.push(punto);
    }
  }
  resultado.push({ x: 0, y: 0 });
  return resultado;
}

// Punto del contorno en la dirección de un ángulo
function puntoEnAngulo(forma: FormaPastel, tamano: number, angulo: number): Punto2d {
  const puntos = puntosForma(forma, tamano, 360);
  let mejor = puntos[0];
  let diferenciaMenor = Infinity;
  for (const punto of puntos) {
    let diferencia = Math.abs(anguloPositivo(Math.atan2(punto.y, punto.x)) - anguloPositivo(angulo));
    diferencia = Math.min(diferencia, Math.PI * 2 - diferencia);
    if (diferencia < diferenciaMenor) {
      diferenciaMenor = diferencia;
      mejor = punto;
    }
  }
  return mejor;
}

// Puntos repartidos a la misma distancia por el contorno, con su normal hacia afuera
function contornoEspaciado(forma: FormaPastel, tamano: number, separacion: number): PuntoContorno[] {
  const puntos = puntosForma(forma, tamano, 360);
  const largos: number[] = [0];
  for (let i = 1; i <= puntos.length; i++) {
    const a = puntos[i - 1];
    const b = puntos[i % puntos.length];
    largos.push(largos[i - 1] + Math.hypot(b.x - a.x, b.y - a.y));
  }
  const largoTotal = largos[largos.length - 1];
  const cantidad = Math.max(8, Math.round(largoTotal / separacion));
  const resultado: PuntoContorno[] = [];
  let tramo = 0;
  for (let k = 0; k < cantidad; k++) {
    const objetivo = (k / cantidad) * largoTotal;
    while (largos[tramo + 1] < objetivo) {
      tramo = tramo + 1;
    }
    const a = puntos[tramo];
    const b = puntos[(tramo + 1) % puntos.length];
    const parte = (objetivo - largos[tramo]) / Math.max(0.0001, largos[tramo + 1] - largos[tramo]);
    const tx = b.x - a.x;
    const ty = b.y - a.y;
    const largo = Math.hypot(tx, ty) || 1;
    // Contorno en sentido contrario a las manecillas: la normal hacia afuera es (ty, -tx)
    resultado.push({ x: a.x + tx * parte, y: a.y + ty * parte, nx: ty / largo, ny: -tx / largo });
  }
  return resultado;
}

// Posición en el mundo de un punto del contorno (separado "separacion" hacia afuera)
function posicionMundo(punto: PuntoContorno, separacion: number): THREE.Vector2 {
  return new THREE.Vector2(punto.x + punto.nx * separacion, -(punto.y + punto.ny * separacion));
}

function giroMundo(punto: PuntoContorno): number {
  return Math.atan2(punto.nx, -punto.ny);
}

// ===================== Reparto de piezas (una pieza por cada unidad elegida) =====================

// Punto del contorno (con su normal) más cercano a un ángulo
function puntoContorno(forma: FormaPastel, tamano: number, angulo: number): PuntoContorno {
  const contorno = contornoEspaciado(forma, tamano, 0.5);
  let mejor = contorno[0];
  let diferenciaMenor = Infinity;
  for (const punto of contorno) {
    let diferencia = Math.abs(anguloPositivo(Math.atan2(punto.y, punto.x)) - anguloPositivo(angulo));
    diferencia = Math.min(diferencia, Math.PI * 2 - diferencia);
    if (diferencia < diferenciaMenor) {
      diferenciaMenor = diferencia;
      mejor = punto;
    }
  }
  return mejor;
}

/**
 * Ángulos para "cantidad" piezas alrededor del pastel. La primera queda en "inicio"
 * (el frente). Con "evitarCorte" las piezas se reparten solo fuera de la rebanada.
 */
function angulosRepartidos(cantidad: number, inicio: number, evitarCorte: boolean): number[] {
  const angulos: number[] = [];
  if (evitarCorte) {
    const largo = Math.PI * 2 - (CORTE_FIN - CORTE_INICIO) - 0.5;
    for (let k = 0; k < cantidad; k++) {
      angulos.push(CORTE_FIN + 0.25 + ((k + 0.5) / cantidad) * largo);
    }
    return angulos;
  }
  for (let k = 0; k < cantidad; k++) {
    angulos.push(inicio + (k / cantidad) * Math.PI * 2);
  }
  return angulos;
}

// Cuántas piezas le tocan a un piso si se reparten de una en una entre todos los pisos
function piezasDelPiso(total: number, indice: number, pisos: number): number {
  let piezas = Math.floor(total / pisos);
  if (indice < total % pisos) {
    piezas = piezas + 1;
  }
  return piezas;
}

interface LugarArriba {
  x: number;
  z: number;
  angulo: number;
}

/**
 * Lugares sobre la superficie para "cantidad" piezas: una sola va al centro; varias
 * forman un anillo y, si no caben, se abre otro anillo más adentro.
 * Con "enOrilla" el anillo empieza en la orilla (para dejar el centro libre al mensaje).
 */
export function lugaresEnAnillos(cantidad: number, radioMaximo: number, separacion: number, enOrilla: boolean): LugarArriba[] {
  const lugares: LugarArriba[] = [];
  if (cantidad === 1 && enOrilla) {
    // Una sola pieza en la orilla de atrás
    lugares.push({ x: 0, z: -radioMaximo, angulo: -Math.PI / 2 });
    return lugares;
  }
  if (cantidad === 1) {
    lugares.push({ x: 0, z: 0, angulo: 0 });
    return lugares;
  }
  let faltan = cantidad;
  let radio = Math.min(radioMaximo, Math.max(separacion * 0.8, (cantidad * separacion) / (Math.PI * 2)));
  if (enOrilla) {
    radio = radioMaximo;
  }
  let vuelta = 0;
  while (faltan > 0) {
    let caben = Math.max(1, Math.floor((Math.PI * 2 * radio) / separacion));
    if (radio < separacion * 0.6) {
      caben = 1;
      radio = 0;
    }
    const enEsteAnillo = Math.min(faltan, caben);
    // En la orilla se recorre medio lugar: así ninguna pieza queda justo al frente tapando el mensaje
    let giro = vuelta * 0.4;
    if (enOrilla) {
      giro = Math.PI / enEsteAnillo;
    }
    for (let k = 0; k < enEsteAnillo; k++) {
      const angulo = (k / enEsteAnillo) * Math.PI * 2 + Math.PI / 2 + giro;
      lugares.push({ x: Math.cos(angulo) * radio, z: Math.sin(angulo) * radio, angulo: angulo });
    }
    faltan = faltan - enEsteAnillo;
    radio = radio - separacion;
    vuelta = vuelta + 1;
  }
  return lugares;
}

// ===================== Repisa de los pisos de abajo =====================

/**
 * Repisa: la franja que queda libre arriba de un piso de abajo, entre su orilla
 * y la pared del piso de encima. Ahí se acomodan fresas y fruta.
 */
export interface Repisa {
  afuera: PuntoContorno[];
  adentro: PuntoContorno[];
}

function repisaDelPiso(forma: FormaPastel, tamano: number, tamanoEncima: number): Repisa {
  return { afuera: contornoEspaciado(forma, tamano, 0.5), adentro: contornoEspaciado(forma, tamanoEncima, 0.5) };
}

// Punto del contorno más cercano al ángulo (medido como en el contorno: el frente es -90°)
function puntoMasCercano(contorno: PuntoContorno[], angulo: number): PuntoContorno {
  let mejor = contorno[0];
  let diferenciaMenor = Infinity;
  for (const punto of contorno) {
    let diferencia = Math.abs(anguloPositivo(Math.atan2(punto.y, punto.x)) - anguloPositivo(angulo));
    diferencia = Math.min(diferencia, Math.PI * 2 - diferencia);
    if (diferencia < diferenciaMenor) {
      diferenciaMenor = diferencia;
      mejor = punto;
    }
  }
  return mejor;
}

/**
 * Distancia al centro para una pieza suelta en la repisa. "anguloMundo" es el de la posición
 * en x, z (el contorno usa y = -z), "azar" va de 0 a 1 y "margen" la separa de las dos paredes.
 */
function distanciaEnRepisa(repisa: Repisa, anguloMundo: number, azar: number, margen: number): number {
  const afuera = puntoMasCercano(repisa.afuera, -anguloMundo);
  const adentro = puntoMasCercano(repisa.adentro, -anguloMundo);
  const hasta = Math.hypot(afuera.x, afuera.y) - margen;
  const desde = Math.min(hasta, Math.hypot(adentro.x, adentro.y) + margen);
  // Raíz: así no se amontonan en la pared de adentro
  return Math.sqrt(desde * desde + azar * (hasta * hasta - desde * desde));
}

// Ancho más angosto de la repisa (para que las piezas quepan en toda la vuelta)
function anchoMinimoRepisa(repisa: Repisa): number {
  let minimo = Infinity;
  for (let k = 0; k < 36; k++) {
    const angulo = (k / 36) * Math.PI * 2;
    const afuera = puntoMasCercano(repisa.afuera, angulo);
    const adentro = puntoMasCercano(repisa.adentro, angulo);
    minimo = Math.min(minimo, Math.hypot(afuera.x, afuera.y) - Math.hypot(adentro.x, adentro.y));
  }
  return minimo;
}

/**
 * Ángulos (del contorno) para las fresas de la repisa. Si el piso tiene rosetas,
 * las fresas se reparten en los huecos entre ellas para no encimarse.
 */
function angulosFresasRepisa(diseno: DisenoPastel, tamano: number, cantidad: number, rosetasAqui: number, desdeArriba: number, conCorte: boolean): number[] {
  if (rosetasAqui <= 0) {
    // Sin rosetas: vuelta pareja; en pisos alternos se recorren medio lugar para no quedar en columna
    let inicio = -Math.PI / 2;
    if (desdeArriba % 2 === 0) {
      inicio = -Math.PI / 2 + Math.PI / cantidad;
    }
    return angulosRepartidos(cantidad, inicio, conCorte);
  }
  const acomodo = acomodoRosetas(diseno.forma, tamano, rosetasAqui);
  const angulosRosetas: number[] = [];
  for (const indice of acomodo.elegidos) {
    const punto = acomodo.contorno[indice];
    angulosRosetas.push(anguloPositivo(Math.atan2(punto.y, punto.x)));
  }
  angulosRosetas.sort(compararNumeros);
  const huecos = angulosRosetas.length;
  const angulos: number[] = [];
  for (let j = 0; j < huecos; j++) {
    const desde = angulosRosetas[j];
    let hasta = angulosRosetas[0] + Math.PI * 2;
    if (j + 1 < huecos) {
      hasta = angulosRosetas[j + 1];
    }
    // Cuántas fresas caen en este hueco (repartidas parejo entre todos)
    const enHueco = Math.floor(((j + 1) * cantidad) / huecos) - Math.floor((j * cantidad) / huecos);
    for (let t = 0; t < enHueco; t++) {
      angulos.push(desde + ((t + 1) / (enHueco + 1)) * (hasta - desde));
    }
  }
  return angulos;
}

function compararNumeros(a: number, b: number): number {
  return a - b;
}

// Fresas paradas en la repisa de un piso de abajo, a la mitad entre la orilla y el piso de encima
function fresasEnRepisa(diseno: DisenoPastel, tamano: number, repisa: Repisa, cantidad: number, rosetasAqui: number, altura: number, desdeArriba: number, conCorte: boolean): THREE.Group {
  const angulos = angulosFresasRepisa(diseno, tamano, cantidad, rosetasAqui, desdeArriba, conCorte);
  const lugares: LugarArriba[] = [];
  let vuelta = 0;
  for (const angulo of angulos) {
    const afuera = puntoMasCercano(repisa.afuera, angulo);
    const adentro = puntoMasCercano(repisa.adentro, angulo);
    const x = (afuera.x + adentro.x) / 2;
    const z = -(afuera.y + adentro.y) / 2;
    lugares.push({ x: x, z: z, angulo: Math.atan2(z, x) });
    vuelta = vuelta + Math.hypot(x, z);
  }
  // Tamaño: que quepan a lo ancho de la repisa y a lo largo de la vuelta (junto con las rosetas)
  const radioMedio = vuelta / Math.max(1, lugares.length);
  const espacioPorPieza = (Math.PI * 2 * radioMedio) / (cantidad + rosetasAqui);
  let escala = Math.min(1.2, (anchoMinimoRepisa(repisa) - 0.3) / 2.2, espacioPorPieza / 2.5);
  escala = Math.max(0.5, escala);
  return mallaFresas(lugares, altura, escala);
}

// ===================== Texturas dibujadas en un lienzo =====================

export function lienzo(ancho: number, alto: number): CanvasRenderingContext2D {
  const elemento = document.createElement('canvas');
  elemento.width = ancho;
  elemento.height = alto;
  return elemento.getContext('2d') as CanvasRenderingContext2D;
}

// Relieve del betún (manchas suaves)
export function texturaRelieve(semilla: number): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  contexto.fillStyle = '#808080';
  contexto.fillRect(0, 0, 256, 256);
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < 420; i++) {
    const x = azar.siguiente() * 256;
    const y = azar.siguiente() * 256;
    const radio = 4 + azar.siguiente() * 14;
    const gradiente = contexto.createRadialGradient(x, y, 0, x, y, radio);
    let tono = 'rgba(255,255,255,0.18)';
    if (azar.siguiente() > 0.5) {
      tono = 'rgba(0,0,0,0.18)';
    }
    gradiente.addColorStop(0, tono);
    gradiente.addColorStop(1, 'rgba(128,128,128,0)');
    contexto.fillStyle = gradiente;
    contexto.fillRect(x - radio, y - radio, radio * 2, radio * 2);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  return textura;
}

// Rayas que deja la espátula en los costados del betún (relieve)
export function texturaEspatula(semilla: number): THREE.CanvasTexture {
  const contexto = lienzo(512, 256);
  contexto.fillStyle = '#808080';
  contexto.fillRect(0, 0, 512, 256);
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < 60; i++) {
    const y = azar.siguiente() * 256;
    let tono = 'rgba(255,255,255,0.22)';
    if (azar.siguiente() > 0.5) {
      tono = 'rgba(0,0,0,0.2)';
    }
    contexto.strokeStyle = tono;
    contexto.lineWidth = 2 + azar.siguiente() * 9;
    const fase = azar.siguiente() * 6;
    const onda = 2 + azar.siguiente() * 5;
    contexto.beginPath();
    // La onda da vueltas enteras en el ancho: así la textura se repite sin costura
    for (let x = 0; x <= 512; x = x + 8) {
      const altura = y + Math.sin((x / 512) * Math.PI * 4 + fase) * onda;
      if (x === 0) {
        contexto.moveTo(x, altura);
      } else {
        contexto.lineTo(x, altura);
      }
    }
    contexto.stroke();
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  return textura;
}

// Vueltas de espátula en la cara de arriba (círculos alrededor del centro)
export function texturaGiro(semilla: number): THREE.CanvasTexture {
  const contexto = lienzo(512, 512);
  contexto.fillStyle = '#808080';
  contexto.fillRect(0, 0, 512, 512);
  const azar = new Aleatorio(semilla);
  let radio = 6;
  while (radio < 360) {
    let tono = 'rgba(255,255,255,0.2)';
    if (azar.siguiente() > 0.5) {
      tono = 'rgba(0,0,0,0.18)';
    }
    contexto.strokeStyle = tono;
    contexto.lineWidth = 2 + azar.siguiente() * 7;
    const inicio = azar.siguiente() * Math.PI * 2;
    contexto.beginPath();
    contexto.arc(256 + (azar.siguiente() - 0.5) * 6, 256 + (azar.siguiente() - 0.5) * 6, radio, inicio, inicio + Math.PI * (1.2 + azar.siguiente() * 0.8));
    contexto.stroke();
    radio = radio + 4 + azar.siguiente() * 10;
  }
  return new THREE.CanvasTexture(contexto.canvas);
}

/**
 * Mármol: el color de la cobertura con vetas finas que serpentean.
 * Sobre una base clara las vetas son grises; sobre una base oscura, claras.
 */
export function texturaMarmol(colorBase: string, semilla: number): THREE.CanvasTexture {
  const lado = 1024;
  const contexto = lienzo(lado, lado);
  contexto.fillStyle = colorBase;
  contexto.fillRect(0, 0, lado, lado);
  const base = new THREE.Color(colorBase);
  const luz = base.r * 0.3 + base.g * 0.59 + base.b * 0.11;
  let vetaFuerte = 'rgba(55, 55, 64, 0.85)';
  let vetaSuave = 'rgba(110, 110, 120, 0.35)';
  let mancha = 'rgba(120, 120, 128, 0.08)';
  if (luz < 0.45) {
    vetaFuerte = 'rgba(245, 238, 228, 0.9)';
    vetaSuave = 'rgba(245, 238, 228, 0.4)';
    mancha = 'rgba(245, 238, 228, 0.08)';
  }
  const azar = new Aleatorio(semilla);
  // Manchas suaves de fondo
  for (let i = 0; i < 30; i++) {
    const x = azar.siguiente() * lado;
    const y = azar.siguiente() * lado;
    const radio = 60 + azar.siguiente() * 180;
    const degradado = contexto.createRadialGradient(x, y, 0, x, y, radio);
    degradado.addColorStop(0, mancha);
    degradado.addColorStop(1, 'rgba(0,0,0,0)');
    contexto.fillStyle = degradado;
    contexto.fillRect(x - radio, y - radio, radio * 2, radio * 2);
  }
  // Vetas: caminos que avanzan en diagonal y se desvían poco a poco
  contexto.lineCap = 'round';
  contexto.lineJoin = 'round';
  for (let v = 0; v < 12; v++) {
    let x = azar.siguiente() * lado;
    let y = azar.siguiente() * lado;
    let direccion = -0.6 + azar.siguiente() * 0.5;
    let fuerte = vetaSuave;
    let grueso = 1.5 + azar.siguiente() * 2;
    if (v % 2 === 0) {
      fuerte = vetaFuerte;
      grueso = 2.5 + azar.siguiente() * 3.5;
    }
    contexto.strokeStyle = fuerte;
    contexto.lineWidth = grueso;
    contexto.beginPath();
    contexto.moveTo(x, y);
    for (let paso = 0; paso < 140; paso++) {
      direccion = direccion + (azar.siguiente() - 0.5) * 0.5;
      x = x + Math.cos(direccion) * 12;
      y = y + Math.sin(direccion) * 12;
      contexto.lineTo(x, y);
      // A veces la veta se abre en una ramita
      if (azar.siguiente() < 0.04) {
        contexto.stroke();
        contexto.lineWidth = 0.6 + azar.siguiente() * 1.5;
        contexto.beginPath();
        contexto.moveTo(x, y);
      }
    }
    contexto.stroke();
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  textura.anisotropy = 4;
  return textura;
}

// Cara de arriba de un pan sin cobertura: costra horneada con migajas
export function texturaCostra(colorPan: string): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  const tostado = new THREE.Color(colorPan).lerp(new THREE.Color('#8a5426'), 0.18);
  contexto.fillStyle = '#' + tostado.getHexString();
  contexto.fillRect(0, 0, 256, 256);
  dibujarMigajas(contexto, 256, 256, 21);
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  return textura;
}

// Pone una textura en la cara de arriba de un piso: una sola vez, centrada
export function centrarArriba(textura: THREE.Texture, tamano: number): void {
  textura.repeat.set(1 / tamano, 1 / tamano);
  textura.offset.set(0.5, 0.5);
}

// Migajas del bizcocho: poros claros y oscuros
export function dibujarMigajas(contexto: CanvasRenderingContext2D, ancho: number, alto: number, semilla: number): void {
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < ancho * alto * 0.012; i++) {
    let tono = 'rgba(0,0,0,0.12)';
    if (azar.siguiente() > 0.55) {
      tono = 'rgba(255,255,255,0.14)';
    }
    contexto.fillStyle = tono;
    contexto.beginPath();
    contexto.ellipse(azar.siguiente() * ancho, azar.siguiente() * alto, 1 + azar.siguiente() * 2.5, 0.8 + azar.siguiente() * 1.6, azar.siguiente() * 3, 0, Math.PI * 2);
    contexto.fill();
  }
}

// Capa de relleno ondulada a lo ancho del lienzo
function dibujarCapaRelleno(contexto: CanvasRenderingContext2D, ancho: number, centro: number, grueso: number, color: string, fase: number): void {
  contexto.fillStyle = color;
  contexto.beginPath();
  contexto.moveTo(0, centro - grueso / 2);
  for (let x = 0; x <= ancho; x = x + 8) {
    contexto.lineTo(x, centro - grueso / 2 + Math.sin((x / ancho) * Math.PI * 6 + fase) * grueso * 0.18);
  }
  for (let x = ancho; x >= 0; x = x - 8) {
    contexto.lineTo(x, centro + grueso / 2 + Math.sin((x / ancho) * Math.PI * 4 + fase * 2) * grueso * 0.18);
  }
  contexto.closePath();
  contexto.fill();
}

/**
 * Lado "naked": se ven las capas de pan y relleno, con betún untado a medias.
 * Con "conBetun" en false (sin cobertura) solo se ven el pan y el relleno.
 */
function texturaNaked(diseno: DisenoPastel, conBetun: boolean): THREE.CanvasTexture {
  const contexto = lienzo(256, 512);
  contexto.fillStyle = diseno.colorBizcocho;
  contexto.fillRect(0, 0, 256, 512);
  dibujarMigajas(contexto, 256, 512, 5);
  dibujarCapaRelleno(contexto, 256, 512 / 3, 22, diseno.colorRelleno, 0.4);
  dibujarCapaRelleno(contexto, 256, (512 * 2) / 3, 22, diseno.colorRelleno, 1.7);
  // Betún raspado: rayas horizontales semitransparentes
  const azar = new Aleatorio(15);
  contexto.fillStyle = diseno.colorCobertura;
  let rayas = 34;
  if (!conBetun) {
    rayas = 0;
  }
  for (let i = 0; i < rayas; i++) {
    contexto.globalAlpha = 0.2 + azar.siguiente() * 0.4;
    const x = azar.siguiente() * 256;
    const y = azar.siguiente() * 512;
    const largo = 40 + azar.siguiente() * 140;
    const grueso = 2 + azar.siguiente() * 6;
    contexto.fillRect(x, y, largo, grueso);
    // Lo que se sale por la derecha entra por la izquierda (sin costura)
    contexto.fillRect(x - 256, y, largo, grueso);
  }
  contexto.globalAlpha = 1;
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  return textura;
}

// Corte por dentro: capas de bizcocho y relleno, con la cobertura por fuera y arriba
export function texturaInterior(diseno: DisenoPastel): THREE.CanvasTexture {
  const contexto = lienzo(512, 512);
  contexto.fillStyle = diseno.colorBizcocho;
  contexto.fillRect(0, 0, 512, 512);
  dibujarMigajas(contexto, 512, 512, 9);
  // Dos capas de relleno con una línea de crema
  dibujarCapaRelleno(contexto, 512, 512 / 3, 30, diseno.colorRelleno, 0.6);
  dibujarCapaRelleno(contexto, 512, (512 * 2) / 3, 30, diseno.colorRelleno, 2.1);
  // Cobertura arriba y por fuera (lado derecho = orilla del pastel)
  if (diseno.cobertura !== 'naked' && diseno.cobertura !== 'ninguna') {
    contexto.fillStyle = diseno.colorCobertura;
    contexto.fillRect(0, 0, 512, 26);
    contexto.fillRect(486, 0, 26, 512);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

// Mensaje escrito sobre el pastel (hasta 3 renglones, la letra se achica si no cabe)
const ANCHO_TEXTO = 2048;
const ALTO_TEXTO = 1024;

function letra(tamano: number): string {
  return 'italic 700 ' + tamano + 'px "Brush Script MT", "Segoe Script", "Dancing Script", cursive';
}

// Reparte las palabras en renglones que quepan en el ancho
function renglones(contexto: CanvasRenderingContext2D, texto: string, ancho: number): string[] {
  const palabras = texto.split(' ');
  const lista: string[] = [];
  let actual = '';
  for (const palabra of palabras) {
    let prueba = palabra;
    if (actual !== '') {
      prueba = actual + ' ' + palabra;
    }
    if (contexto.measureText(prueba).width > ancho && actual !== '') {
      lista.push(actual);
      actual = palabra;
    } else {
      actual = prueba;
    }
  }
  if (actual !== '') {
    lista.push(actual);
  }
  return lista;
}

export function texturaDedicatoria(texto: string, color: string): THREE.CanvasTexture {
  const contexto = lienzo(ANCHO_TEXTO, ALTO_TEXTO);
  contexto.clearRect(0, 0, ANCHO_TEXTO, ALTO_TEXTO);
  contexto.fillStyle = color;
  contexto.textAlign = 'center';
  contexto.textBaseline = 'middle';
  let tamano = 300;
  contexto.font = letra(tamano);
  let lineas = renglones(contexto, texto, ANCHO_TEXTO * 0.94);
  while ((lineas.length > 3 || lineas.length * tamano * 1.1 > ALTO_TEXTO) && tamano > 60) {
    tamano = tamano - 10;
    contexto.font = letra(tamano);
    lineas = renglones(contexto, texto, ANCHO_TEXTO * 0.94);
  }
  // Sombra suave para que se lea sobre cualquier color de cobertura
  contexto.shadowColor = 'rgba(0, 0, 0, 0.3)';
  contexto.shadowBlur = 10;
  contexto.shadowOffsetY = 6;
  const altoRenglon = tamano * 1.1;
  const inicio = ALTO_TEXTO / 2 - ((lineas.length - 1) * altoRenglon) / 2;
  for (let i = 0; i < lineas.length; i++) {
    contexto.fillText(lineas[i], ANCHO_TEXTO / 2, inicio + i * altoRenglon);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.anisotropy = 8;
  return textura;
}

// Lado del lienzo cuadrado del mensaje en arco
const LADO_ARCO = 2048;

// Ancho en el lienzo de un renglón con la letra actual
function anchoTexto(contexto: CanvasRenderingContext2D, texto: string): number {
  return contexto.measureText(texto).width;
}

// Parte el mensaje en dos renglones de largo parecido
function partirEnDos(texto: string): string[] {
  const palabras = texto.split(' ');
  if (palabras.length < 2) {
    return [texto];
  }
  let mejor = 1;
  let diferenciaMenor = Infinity;
  for (let corte = 1; corte < palabras.length; corte++) {
    const primero = palabras.slice(0, corte).join(' ');
    const segundo = palabras.slice(corte).join(' ');
    const diferencia = Math.abs(primero.length - segundo.length);
    if (diferencia < diferenciaMenor) {
      diferenciaMenor = diferencia;
      mejor = corte;
    }
  }
  return [palabras.slice(0, mejor).join(' '), palabras.slice(mejor).join(' ')];
}

/**
 * Dibuja un renglón siguiendo un círculo. "arriba" = arco por detrás del objeto (∩);
 * si no, arco por delante (∪). En los dos casos se lee de izquierda a derecha desde el frente.
 */
function renglonEnArco(contexto: CanvasRenderingContext2D, texto: string, radioInterior: number, radioExterior: number, arriba: boolean): void {
  const centro = LADO_ARCO / 2;
  let radio = radioInterior;
  if (!arriba) {
    // Abajo la letra crece hacia el centro: la línea base va por fuera
    radio = radioExterior;
  }
  const total = anchoTexto(contexto, texto);
  let recorrido = 0;
  for (const caracter of texto) {
    const ancho = anchoTexto(contexto, caracter);
    const mitad = (recorrido + ancho / 2 - total / 2) / radio;
    let angulo = -Math.PI / 2 + mitad;
    let giro = angulo + Math.PI / 2;
    if (!arriba) {
      angulo = Math.PI / 2 - mitad;
      giro = angulo - Math.PI / 2;
    }
    contexto.save();
    contexto.translate(centro + Math.cos(angulo) * radio, centro + Math.sin(angulo) * radio);
    contexto.rotate(giro);
    contexto.fillText(caracter, 0, 0);
    contexto.restore();
    recorrido = recorrido + ancho;
  }
}

/**
 * Mensaje en arco alrededor del objeto del centro. Un renglón va por delante del objeto;
 * si no cabe, se parte en dos: uno por detrás y otro por delante.
 * "radioInterior" y "radioExterior" están en pixeles del lienzo.
 */
function texturaDedicatoriaArco(texto: string, color: string, radioInterior: number, radioExterior: number): THREE.CanvasTexture {
  const contexto = lienzo(LADO_ARCO, LADO_ARCO);
  contexto.clearRect(0, 0, LADO_ARCO, LADO_ARCO);
  contexto.fillStyle = color;
  contexto.textAlign = 'center';
  contexto.textBaseline = 'alphabetic';
  contexto.shadowColor = 'rgba(0, 0, 0, 0.3)';
  contexto.shadowBlur = 8;
  contexto.shadowOffsetY = 5;
  // Alto de la letra = ancho de la franja; un arco puede abarcar hasta 200°
  const letraMaxima = (radioExterior - radioInterior) * 1.25;
  const arcoArriba = radioInterior * Math.PI * 1.1;
  const arcoAbajo = radioExterior * Math.PI * 1.1;
  let tamano = letraMaxima;
  contexto.font = letra(tamano);
  // Primero se intenta en un solo renglón por delante
  if (anchoTexto(contexto, texto) <= arcoAbajo || texto.indexOf(' ') < 0) {
    while (anchoTexto(contexto, texto) > arcoAbajo && tamano > 50) {
      tamano = tamano - 5;
      contexto.font = letra(tamano);
    }
    renglonEnArco(contexto, texto, radioInterior, radioExterior, false);
  } else {
    const partes = partirEnDos(texto);
    while ((anchoTexto(contexto, partes[0]) > arcoArriba || anchoTexto(contexto, partes[1]) > arcoAbajo) && tamano > 50) {
      tamano = tamano - 5;
      contexto.font = letra(tamano);
    }
    renglonEnArco(contexto, partes[0], radioInterior, radioExterior, true);
    renglonEnArco(contexto, partes[1], radioInterior, radioExterior, false);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.anisotropy = 8;
  return textura;
}

// ===================== Cuerpo de cada piso =====================

/**
 * Materiales de la cobertura: "arriba" para la cara de arriba y "lados" para los costados.
 * Cada acabado tiene su textura: betún con rayas de espátula, chantilly esponjoso,
 * fondant liso y satinado, ganache brillante y naked con las capas a la vista.
 */
// Color de arriba de la cobertura degradada (casi blanco)
export const COLOR_ARRIBA_DEGRADADO = '#fdf9f4';

/**
 * Pinta una figura con un degradado de abajo hacia arriba (colores por vértice).
 * Se usa en la cobertura «Degradado»: el color elegido abajo y casi blanco arriba.
 */
export function pintarDegradado(geometria: THREE.BufferGeometry, colorAbajo: string, colorArriba: string): void {
  geometria.computeBoundingBox();
  const caja = geometria.boundingBox as THREE.Box3;
  const alto = Math.max(0.001, caja.max.y - caja.min.y);
  const posiciones = geometria.attributes['position'];
  const colores = new Float32Array(posiciones.count * 3);
  const color = new THREE.Color();
  for (let i = 0; i < posiciones.count; i++) {
    const parte = (posiciones.getY(i) - caja.min.y) / alto;
    // Abajo se queda con su color un rato y luego se aclara suave
    mezclarColores(color, colorAbajo, colorArriba, THREE.MathUtils.smoothstep(parte, 0.25, 1));
    colores[i * 3] = color.r;
    colores[i * 3 + 1] = color.g;
    colores[i * 3 + 2] = color.b;
  }
  geometria.setAttribute('color', new THREE.BufferAttribute(colores, 3));
}

/**
 * Mezcla dos colores como se ven en pantalla (sRGB) y deja el resultado en "destino".
 * Mezclar en sRGB da un degradado parejo; en los valores internos de three se aclara de golpe.
 */
export function mezclarColores(destino: THREE.Color, colorA: string, colorB: string, parte: number): void {
  const a = new THREE.Color(colorA);
  const b = new THREE.Color(colorB);
  const ra = { r: 0, g: 0, b: 0 };
  const rb = { r: 0, g: 0, b: 0 };
  a.getRGB(ra, THREE.SRGBColorSpace);
  b.getRGB(rb, THREE.SRGBColorSpace);
  destino.setRGB(ra.r + (rb.r - ra.r) * parte, ra.g + (rb.g - ra.g) * parte, ra.b + (rb.b - ra.b) * parte, THREE.SRGBColorSpace);
}

function materialesCobertura(diseno: DisenoPastel, tamano: number, alto: number): { arriba: THREE.Material; lados: THREE.Material } {
  const color = diseno.colorCobertura;
  if (diseno.cobertura === 'degradado') {
    // Betún con el color en degradado: el color lo dan los vértices (pintarDegradado)
    const giroDegradado = texturaGiro(4);
    centrarArriba(giroDegradado, tamano);
    const espatulaDegradado = texturaEspatula(7);
    espatulaDegradado.repeat.set(1 / 14, 1 / 5);
    return {
      arriba: new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.6, bumpMap: giroDegradado, bumpScale: 2 }),
      lados: new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.6, bumpMap: espatulaDegradado, bumpScale: 2.2 }),
    };
  }
  if (diseno.cobertura === 'ganache') {
    const brillo = new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.14, clearcoat: 1, clearcoatRoughness: 0.08 });
    return { arriba: brillo, lados: brillo };
  }
  if (diseno.cobertura === 'lisa') {
    // Fondant: liso y satinado, sin marcas
    const satin = new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.42, sheen: 0.8, sheenRoughness: 0.5, sheenColor: new THREE.Color('#ffffff') });
    return { arriba: satin, lados: satin };
  }
  if (diseno.cobertura === 'marmol') {
    // Mármol: liso y satinado con vetas (sin piezas extra)
    const vetasLados = texturaMarmol(color, 31);
    vetasLados.repeat.set(1 / 26, 1 / 26);
    const vetasArriba = texturaMarmol(color, 37);
    centrarArriba(vetasArriba, tamano);
    return {
      arriba: new THREE.MeshPhysicalMaterial({ map: vetasArriba, roughness: 0.32, clearcoat: 0.35, clearcoatRoughness: 0.3 }),
      lados: new THREE.MeshPhysicalMaterial({ map: vetasLados, roughness: 0.32, clearcoat: 0.35, clearcoatRoughness: 0.3 }),
    };
  }
  if (diseno.cobertura === 'ninguna') {
    // Sin cobertura: a los lados se ven las capas de pan y relleno; arriba, la costra del pan
    const capas = texturaNaked(diseno, false);
    capas.repeat.set(1 / 16, 1 / alto);
    const costra = texturaCostra(diseno.colorBizcocho);
    costra.repeat.set(1 / 6, 1 / 6);
    return {
      arriba: new THREE.MeshStandardMaterial({ map: costra, roughness: 0.9 }),
      lados: new THREE.MeshStandardMaterial({ map: capas, roughness: 0.9 }),
    };
  }
  const giro = texturaGiro(4);
  centrarArriba(giro, tamano);
  if (diseno.cobertura === 'chantilly') {
    // Chantilly: esponjoso, con bolitas suaves
    const esponja = texturaRelieve(3);
    esponja.repeat.set(1 / 5, 1 / 5);
    const esponjaArriba = texturaRelieve(6);
    esponjaArriba.repeat.set(1 / 5, 1 / 5);
    return {
      arriba: new THREE.MeshStandardMaterial({ color: color, roughness: 0.92, bumpMap: esponjaArriba, bumpScale: 3 }),
      lados: new THREE.MeshStandardMaterial({ color: color, roughness: 0.92, bumpMap: esponja, bumpScale: 3 }),
    };
  }
  const arriba = new THREE.MeshStandardMaterial({ color: color, roughness: 0.6, bumpMap: giro, bumpScale: 2 });
  if (diseno.cobertura === 'naked') {
    const capas = texturaNaked(diseno, true);
    capas.repeat.set(1 / 16, 1 / alto);
    return { arriba: arriba, lados: new THREE.MeshStandardMaterial({ map: capas, roughness: 0.85 }) };
  }
  // Betún de mantequilla (también es la base de la cobertura de chispas)
  const espatula = texturaEspatula(7);
  espatula.repeat.set(1 / 14, 1 / 5);
  return { arriba: arriba, lados: new THREE.MeshStandardMaterial({ color: color, roughness: 0.6, bumpMap: espatula, bumpScale: 2.2 }) };
}

function cuerpoPiso(diseno: DisenoPastel, tamano: number, alto: number, conCorte: boolean): THREE.Group {
  const grupo = new THREE.Group();
  // El fondant tiene la orilla más redondeada
  let biselado = Math.min(1, alto * 0.1);
  if (diseno.cobertura === 'lisa' || diseno.cobertura === 'marmol') {
    biselado = Math.min(1.6, alto * 0.15);
  }
  if (diseno.cobertura === 'ninguna') {
    // Pan sin cobertura: orilla casi recta
    biselado = Math.min(0.4, alto * 0.05);
  }
  let puntos = puntosForma(diseno.forma, tamano - 2 * biselado, 180);
  if (conCorte) {
    // Sin orilla redondeada: así las paredes del corte quedan justo donde se ven las capas
    biselado = 0;
    puntos = puntosConCorte(diseno.forma, tamano);
  }
  // El degradado necesita cortes a lo alto para que el color cambie suave
  let pasos = 1;
  if (diseno.cobertura === 'degradado') {
    pasos = 10;
  }
  const geometria = new THREE.ExtrudeGeometry(formaThree(puntos), {
    depth: alto - 2 * biselado,
    steps: pasos,
    bevelEnabled: biselado > 0,
    bevelThickness: biselado,
    bevelSize: biselado,
    bevelSegments: 4,
    curveSegments: 12,
  });
  geometria.translate(0, 0, biselado);
  geometria.rotateX(-Math.PI / 2);
  if (diseno.cobertura === 'degradado') {
    pintarDegradado(geometria, diseno.colorCobertura, COLOR_ARRIBA_DEGRADADO);
  }

  const materiales = materialesCobertura(diseno, tamano, alto);
  const cuerpo = new THREE.Mesh(geometria, [materiales.arriba, materiales.lados]);
  // El cuerpo ya trae la rebanada quitada: no se recorta otra vez
  cuerpo.userData['sinRecorte'] = true;
  grupo.add(cuerpo);
  if (diseno.cobertura === 'chispas') {
    grupo.add(chispasDeCobertura(diseno, tamano, alto, conCorte));
  }

  if (conCorte) {
    grupo.add(paredCorte(diseno, tamano, alto, CORTE_INICIO, 1));
    grupo.add(paredCorte(diseno, tamano, alto, CORTE_FIN, -1));
  }
  return grupo;
}

/**
 * Cobertura de chispas: chispas de colores pegadas en toda la cobertura, a los lados y arriba.
 * Es parte de la cobertura (no es la decoración «Chispas», que solo va arriba).
 */
function chispasDeCobertura(diseno: DisenoPastel, tamano: number, alto: number, conCorte: boolean): THREE.InstancedMesh {
  const contorno = contornoEspaciado(diseno.forma, tamano, 0.2);
  const perimetro = contorno.length * 0.2;
  const enLados = Math.min(1400, Math.round(perimetro * alto * 0.9));
  const enArriba = Math.min(900, Math.round(tamano * tamano * 0.5));
  const malla = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.11, 0.42, 2, 6), material('#ffffff', 0.45, 0), enLados + enArriba);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(17);
  let puestas = 0;
  // Costados: pegadas a la pared, cada una con su propio giro
  for (let i = 0; i < enLados; i++) {
    const punto = contorno[Math.floor(azar.siguiente() * contorno.length)];
    if (conCorte && dentroDelCorte(Math.atan2(punto.y, punto.x))) {
      continue;
    }
    const posicion = posicionMundo(punto, 0.06);
    auxiliar.position.set(posicion.x, 0.35 + azar.siguiente() * (alto - 0.7), posicion.y);
    auxiliar.rotation.set(0, giroMundo(punto), azar.siguiente() * Math.PI * 2, 'YXZ');
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    malla.setColorAt(puestas, color.set(COLORES_CHISPAS[Math.floor(azar.siguiente() * COLORES_CHISPAS.length)]));
    puestas = puestas + 1;
  }
  // Arriba: acostadas sobre la cara de arriba
  const orilla = puntosForma(diseno.forma, tamano - 0.6, 120);
  let intentos = 0;
  let arriba = 0;
  while (arriba < enArriba && intentos < enArriba * 6) {
    intentos = intentos + 1;
    const x = (azar.siguiente() - 0.5) * tamano;
    const y = (azar.siguiente() - 0.5) * tamano;
    if (!estaDentro(x, y, orilla)) {
      continue;
    }
    if (conCorte && Math.hypot(x, y) > 0.3 && dentroDelCorte(Math.atan2(y, x))) {
      continue;
    }
    auxiliar.position.set(x, alto + 0.1, -y);
    auxiliar.rotation.set(0, azar.siguiente() * Math.PI * 2, Math.PI / 2, 'YXZ');
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    malla.setColorAt(puestas, color.set(COLORES_CHISPAS[Math.floor(azar.siguiente() * COLORES_CHISPAS.length)]));
    puestas = puestas + 1;
    arriba = arriba + 1;
  }
  malla.count = puestas;
  // Ya viene sin la rebanada: no se recorta otra vez
  malla.userData['sinRecorte'] = true;
  return malla;
}

// Pared del corte: muestra las capas de bizcocho y relleno
function paredCorte(diseno: DisenoPastel, tamano: number, alto: number, angulo: number, lado: number): THREE.Mesh {
  const orilla = puntoEnAngulo(diseno.forma, tamano, angulo);
  // Se separa un poquito hacia la rebanada vacía para que no se encime con el cuerpo
  const separacion = 0.04 * lado;
  const sx = -Math.sin(angulo) * separacion;
  const sy = Math.cos(angulo) * separacion;
  const x1 = sx;
  const z1 = -sy;
  const x2 = orilla.x + sx;
  const z2 = -(orilla.y + sy);
  const geometria = new THREE.BufferGeometry();
  const vertices = new Float32Array([x1, 0, z1, x2, 0, z2, x2, alto, z2, x1, alto, z1]);
  const uv = new Float32Array([0, 0, 1, 0, 1, 1, 0, 1]);
  geometria.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
  geometria.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
  geometria.setIndex([0, 1, 2, 0, 2, 3]);
  geometria.computeVertexNormals();
  const textura = texturaInterior(diseno);
  textura.flipY = true;
  const malla = new THREE.Mesh(geometria, new THREE.MeshStandardMaterial({ map: textura, roughness: 0.9, side: THREE.DoubleSide }));
  // La pared está justo en la orilla del corte: no se recorta
  malla.userData['sinRecorte'] = true;
  return malla;
}

// ===================== Decoraciones =====================

function perlas(diseno: DisenoPastel, tamano: number, color: string): THREE.InstancedMesh {
  const contorno = contornoEspaciado(diseno.forma, tamano, 1.5);
  const geometria = new THREE.SphereGeometry(0.7, 16, 12);
  // Perla: nacarada, con reflejos de colores
  const nacar = new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.18, metalness: 0.1, clearcoat: 1, iridescence: 0.7, iridescenceIOR: 1.6 });
  const malla = new THREE.InstancedMesh(geometria, nacar, contorno.length);
  const auxiliar = new THREE.Object3D();
  for (let i = 0; i < contorno.length; i++) {
    const posicion = posicionMundo(contorno[i], 0.35);
    auxiliar.position.set(posicion.x, 0.7, posicion.y);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

/**
 * Banda de tela que rodea el piso siguiendo su forma, con las orillas redondeadas.
 * "ancho" es lo alto de la banda, "grueso" lo grueso de la tela y "separacion" cuánto se despega del pastel.
 */
function geometriaBanda(forma: FormaPastel, tamano: number, ancho: number, grueso: number, separacion: number): THREE.BufferGeometry {
  const contorno = contornoEspaciado(forma, tamano, 0.4);
  // Corte de la tela [hacia afuera, altura], de abajo hacia arriba con las dos orillas redondeadas
  const perfil: number[][] = [];
  const pasosOrilla = 5;
  const mitad = grueso / 2;
  for (let k = 0; k <= pasosOrilla; k++) {
    const angulo = Math.PI + (k / pasosOrilla) * Math.PI;
    perfil.push([mitad + Math.cos(angulo) * mitad, mitad + Math.sin(angulo) * mitad]);
  }
  for (let k = 0; k <= pasosOrilla; k++) {
    const angulo = (k / pasosOrilla) * Math.PI;
    perfil.push([mitad + Math.cos(angulo) * mitad, ancho - mitad + Math.sin(angulo) * mitad]);
  }
  const vertices: number[] = [];
  const uv: number[] = [];
  const indices: number[] = [];
  let recorrido = 0;
  // Se repite el primer punto al final para cerrar la vuelta
  for (let i = 0; i <= contorno.length; i++) {
    const punto = contorno[i % contorno.length];
    if (i > 0) {
      const anterior = contorno[i - 1];
      recorrido = recorrido + Math.hypot(punto.x - anterior.x, punto.y - anterior.y);
    }
    for (const par of perfil) {
      const fuera = separacion + par[0];
      vertices.push(punto.x + punto.nx * fuera, par[1], -(punto.y + punto.ny * fuera));
      uv.push(recorrido / ancho, par[1] / ancho);
    }
  }
  const columnas = perfil.length;
  for (let i = 0; i < contorno.length; i++) {
    for (let j = 0; j < columnas - 1; j++) {
      const a = i * columnas + j;
      const b = (i + 1) * columnas + j;
      indices.push(a, b, a + 1, b, b + 1, a + 1);
    }
  }
  const geometria = new THREE.BufferGeometry();
  geometria.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3));
  geometria.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  geometria.setIndex(indices);
  geometria.computeVertexNormals();
  return geometria;
}

// Tela del listón: orillas con costura y un rayado muy fino (como el listón de satín de verdad)
function texturaListon(): THREE.CanvasTexture {
  const lado = 256;
  const contexto = lienzo(lado, lado);
  contexto.fillStyle = '#ffffff';
  contexto.fillRect(0, 0, lado, lado);
  // Rayado fino a lo largo del listón
  contexto.fillStyle = 'rgba(0, 0, 0, 0.035)';
  for (let y = 0; y < lado; y = y + 6) {
    contexto.fillRect(0, y, lado, 2);
  }
  // Orilla un poco más oscura y costura punteada arriba y abajo
  contexto.fillStyle = 'rgba(0, 0, 0, 0.12)';
  contexto.fillRect(0, 0, lado, 10);
  contexto.fillRect(0, lado - 10, lado, 10);
  contexto.fillStyle = 'rgba(255, 255, 255, 0.55)';
  for (let x = 0; x < lado; x = x + 16) {
    contexto.fillRect(x, 20, 9, 3);
    contexto.fillRect(x, lado - 23, 9, 3);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  textura.anisotropy = 4;
  return textura;
}

/**
 * Color del listón y los moños: un tono de la misma familia que la cobertura, más fuerte,
 * para que combine con el pastel. Sobre coberturas oscuras el listón se aclara.
 */
export function colorListon(diseno: DisenoPastel): string {
  const color = new THREE.Color(diseno.colorCobertura);
  const hsl = { h: 0, s: 0, l: 0 };
  color.getHSL(hsl);
  // Coberturas blancas o crema (vainilla, fondant blanco): se usa el rosa de la casa
  if (hsl.s < 0.3 && hsl.l > 0.6) {
    return '#cf4d74';
  }
  let luz = hsl.l - 0.22;
  if (hsl.l < 0.35) {
    luz = hsl.l + 0.3;
  }
  color.setHSL(hsl.h, Math.min(0.85, hsl.s + 0.25), Math.min(0.85, Math.max(0.2, luz)));
  return '#' + color.getHexString();
}

// Listón alrededor del piso: "altura" es donde queda su orilla de abajo
function liston(diseno: DisenoPastel, tamano: number, color: string, altura: number): THREE.Mesh {
  const tela = new THREE.MeshPhysicalMaterial({
    color: color,
    map: texturaListon(),
    roughness: 0.38,
    sheen: 1,
    sheenRoughness: 0.3,
    sheenColor: new THREE.Color('#ffffff'),
    side: THREE.DoubleSide,
  });
  // Pegado al pastel: apenas se separa lo necesario para no encimarse con la cobertura
  const malla = new THREE.Mesh(geometriaBanda(diseno.forma, tamano, ANCHO_LISTON, 0.18, 0.1), tela);
  malla.position.y = altura;
  return malla;
}

// Tela de listón: satinada (brilla suave al girar)
export function telaSatinada(color: string): THREE.MeshPhysicalMaterial {
  return new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.35, sheen: 1, sheenRoughness: 0.3, sheenColor: new THREE.Color('#ffffff'), side: THREE.DoubleSide });
}

/**
 * Cinta que sigue un camino por el costado del pastel (plano x, y). La tela queda de cara
 * al que mira: el ancho se mide a los lados del camino y la cinta se despega un poco (z)
 * para que el lazo tenga volumen. Se angosta en las puntas, donde la tela entra al nudo.
 */
function geometriaCinta(camino: THREE.Curve<THREE.Vector3>, pasos: number, ancho: number, bulto: number): THREE.BufferGeometry {
  const vertices: number[] = [];
  const uv: number[] = [];
  const indices: number[] = [];
  const punto = new THREE.Vector3();
  const adelante = new THREE.Vector3();
  for (let i = 0; i <= pasos; i++) {
    const t = i / pasos;
    camino.getPoint(t, punto);
    camino.getTangent(t, adelante);
    // Perpendicular al camino, acostada sobre el costado del pastel
    const lx = -adelante.y;
    const ly = adelante.x;
    const largo = Math.hypot(lx, ly) || 1;
    const mitad = (ancho / 2) * (0.45 + 0.55 * Math.sin(Math.PI * t));
    const fuera = punto.z + bulto * Math.sin(Math.PI * t);
    vertices.push(punto.x - (lx / largo) * mitad, punto.y - (ly / largo) * mitad, fuera);
    vertices.push(punto.x + (lx / largo) * mitad, punto.y + (ly / largo) * mitad, fuera);
    uv.push(t * 3, 0, t * 3, 1);
    if (i < pasos) {
      const a = i * 2;
      indices.push(a, a + 2, a + 1, a + 1, a + 2, a + 3);
    }
  }
  const geometria = new THREE.BufferGeometry();
  geometria.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3));
  geometria.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  geometria.setIndex(indices);
  geometria.computeVertexNormals();
  return geometria;
}

// Lazo del moño: sale del nudo, se abre hacia un lado y regresa al nudo ("lado" 1 o -1)
function geometriaLazo(lado: number): THREE.BufferGeometry {
  const puntos = [
    [0, 0.15, 0.1],
    [1.1, 1.25, 0.35],
    [2.5, 1.5, 0.5],
    [3.4, 0.5, 0.35],
    [3.1, -0.7, 0.3],
    [1.8, -1.0, 0.45],
    [0.6, -0.5, 0.3],
    [0, -0.1, 0.1],
  ];
  const vectores: THREE.Vector3[] = [];
  for (const punto of puntos) {
    vectores.push(new THREE.Vector3(punto[0] * lado, punto[1], punto[2]));
  }
  return geometriaCinta(new THREE.CatmullRomCurve3(vectores), 56, 1.5, 0.35);
}

// Cola del moño: tira de tela con la punta cortada en V
function geometriaCola(largo: number): THREE.BufferGeometry {
  const forma = new THREE.Shape();
  forma.moveTo(-0.5, 0);
  forma.lineTo(0.5, 0);
  forma.lineTo(0.6, -largo);
  forma.lineTo(0, -largo + 0.45);
  forma.lineTo(-0.4, -largo);
  forma.lineTo(-0.5, 0);
  return new THREE.ShapeGeometry(forma);
}

/**
 * Un moño pegado al costado del piso, en el ángulo pedido (-π/2 es el frente).
 * Hecho de cinta: dos lazos, el nudo y dos colas con la punta en V.
 * Si el piso tiene listón, el moño se amarra sobre él (misma tela y mismo color).
 */
export function mono(diseno: DisenoPastel, tamano: number, altura: number, color: string, angulo: number, escala: number): THREE.Group {
  const grupo = new THREE.Group();
  const tela = telaSatinada(color);
  const izquierdo = new THREE.Mesh(geometriaLazo(-1), tela);
  const derecho = new THREE.Mesh(geometriaLazo(1), tela);
  const nudo = new THREE.Mesh(new THREE.SphereGeometry(0.72, 18, 14), tela);
  nudo.scale.set(0.95, 1.1, 0.85);
  nudo.position.z = 0.4;
  // Las colas no pasan del plato: si el moño va abajo, se hacen más cortas
  const largoColas = Math.max(0.9, Math.min(3.4, (altura - 0.4) / escala));
  const cola = geometriaCola(largoColas);
  const cola1 = new THREE.Mesh(cola, tela);
  cola1.position.set(-0.3, -0.1, 0.12);
  cola1.rotation.set(0.12, 0, -0.3);
  const cola2 = new THREE.Mesh(cola, tela);
  cola2.position.set(0.3, -0.1, 0.12);
  cola2.rotation.set(0.12, 0, 0.3);
  grupo.add(cola1, cola2, izquierdo, derecho, nudo);
  const punto = puntoContorno(diseno.forma, tamano, angulo);
  const posicion = posicionMundo(punto, 0.4);
  grupo.position.set(posicion.x, altura, posicion.y);
  grupo.rotation.y = giroMundo(punto);
  grupo.scale.setScalar(escala);
  return grupo;
}

// Un moño en cada ángulo indicado
function monos(diseno: DisenoPastel, tamano: number, altura: number, angulos: number[], lugaresTotales: number, color: string): THREE.Group {
  const grupo = new THREE.Group();
  const perimetro = Math.PI * tamano;
  // Si hay muchas piezas alrededor, los moños se hacen más chicos para que no se encimen
  const escala = Math.max(0.5, Math.min(1, perimetro / lugaresTotales / 7.5));
  for (const angulo of angulos) {
    grupo.add(mono(diseno, tamano, altura, color, angulo, escala));
  }
  return grupo;
}

// Roseta de crema hecha con duya de estrella: un cordón con estrías que sube en espiral
export function geometriaRoseta(): THREE.BufferGeometry {
  const puntos: THREE.Vector3[] = [];
  const pasos = 60;
  for (let i = 0; i <= pasos; i++) {
    const t = i / pasos;
    const angulo = t * Math.PI * 2 * 2.2;
    const radio = 1.0 * (1 - t) + 0.08;
    puntos.push(new THREE.Vector3(Math.cos(angulo) * radio, 0.38 + t * 1.45, Math.sin(angulo) * radio));
  }
  const camino = new THREE.CatmullRomCurve3(puntos);
  const segmentos = 90;
  const lados = 16;
  const tubo = new THREE.TubeGeometry(camino, segmentos, 0.44, lados, false);
  // Estrías de la duya y punta más delgada
  const posiciones = tubo.attributes['position'];
  const centro = new THREE.Vector3();
  const punto = new THREE.Vector3();
  for (let i = 0; i <= segmentos; i++) {
    camino.getPointAt(i / segmentos, centro);
    const adelgazar = 1 - 0.6 * Math.pow(i / segmentos, 2);
    for (let j = 0; j <= lados; j++) {
      const indice = i * (lados + 1) + j;
      punto.fromBufferAttribute(posiciones, indice);
      const estria = 1 + 0.24 * Math.cos((j / lados) * Math.PI * 2 * 6);
      punto.sub(centro).multiplyScalar(estria * adelgazar).add(centro);
      posiciones.setXYZ(indice, punto.x, punto.y, punto.z);
    }
  }
  tubo.computeVertexNormals();
  return tubo;
}

/**
 * Rosetas de crema sobre la orilla del piso. Con "cantidad" 0 se llena toda la orilla
 * (cobertura chantilly); si no, se ponen solo esas piezas, repartidas desde el frente.
 */
interface AcomodoRosetas {
  contorno: PuntoContorno[];
  // Índices del contorno donde va una roseta
  elegidos: number[];
  escala: number;
}

// Dónde van las rosetas en la orilla de un piso (la primera al frente)
function acomodoRosetas(forma: FormaPastel, tamano: number, cantidad: number): AcomodoRosetas {
  // Si piden más rosetas de las que caben, se juntan y se hacen más chicas
  let separacion = 3.1;
  let escala = 1;
  const caben = contornoEspaciado(forma, tamano, 3.1).length;
  if (cantidad > caben) {
    separacion = Math.max(1.6, (caben * 3.1) / cantidad);
    escala = separacion / 3.1;
  }
  const contorno = contornoEspaciado(forma, tamano, separacion);
  let frente = 0;
  for (let i = 0; i < contorno.length; i++) {
    if (contorno[i].y < contorno[frente].y) {
      frente = i;
    }
  }
  const elegidos: number[] = [];
  if (cantidad <= 0 || cantidad >= contorno.length) {
    for (let i = 0; i < contorno.length; i++) {
      elegidos.push(i);
    }
  } else {
    for (let k = 0; k < cantidad; k++) {
      elegidos.push((frente + Math.round((k * contorno.length) / cantidad)) % contorno.length);
    }
  }
  return { contorno: contorno, elegidos: elegidos, escala: escala };
}

function rosetas(diseno: DisenoPastel, tamano: number, altura: number, color: string, cantidad: number, evitarCorte: boolean): THREE.InstancedMesh {
  const acomodo = acomodoRosetas(diseno.forma, tamano, cantidad);
  const contorno = acomodo.contorno;
  const elegidos = acomodo.elegidos;
  const escala = acomodo.escala;
  const malla = new THREE.InstancedMesh(geometriaRoseta(), material(color, 0.7, 0), elegidos.length);
  const auxiliar = new THREE.Object3D();
  let puestas = 0;
  for (const indice of elegidos) {
    const punto = contorno[indice];
    if (evitarCorte && dentroDelCorte(Math.atan2(punto.y, punto.x))) {
      continue;
    }
    const posicion = posicionMundo(punto, -1.6 * escala);
    auxiliar.position.set(posicion.x, altura - 0.1, posicion.y);
    auxiliar.rotation.set(0, indice * 1.7, 0);
    auxiliar.scale.setScalar(escala);
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    puestas = puestas + 1;
  }
  malla.count = puestas;
  return malla;
}

/**
 * Goteo de chocolate: capa arriba y gotas por la orilla. Las gotas no bajan de "limite"
 * (la orilla de arriba del listón), así el chocolate nunca atraviesa la tela.
 */
function goteo(diseno: DisenoPastel, tamano: number, altura: number, color: string, limite: number): THREE.Group {
  const grupo = new THREE.Group();
  const chocolate = new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.25, clearcoat: 0.7 });
  const tapa = new THREE.ExtrudeGeometry(formaThree(puntosForma(diseno.forma, tamano + 0.2, 180)), {
    depth: 0.3,
    bevelEnabled: true,
    bevelThickness: 0.2,
    bevelSize: 0.2,
    bevelSegments: 3,
    curveSegments: 12,
  });
  tapa.rotateX(-Math.PI / 2);
  const mallaTapa = new THREE.Mesh(tapa, chocolate);
  mallaTapa.position.y = altura - 0.2;
  grupo.add(mallaTapa);

  const largos = [0.6, 1.4, 2.2, 3.0, 4.0];
  // La gota más larga posible: su punta queda arriba del límite
  let largoMaximo = 4;
  if (limite > 0) {
    largoMaximo = Math.max(0.2, altura - 1.2 - limite);
  }
  for (let i = 0; i < largos.length; i++) {
    largos[i] = Math.min(largos[i], largoMaximo);
  }
  const gotas: THREE.CapsuleGeometry[] = [];
  for (const largo of largos) {
    gotas.push(new THREE.CapsuleGeometry(0.5, largo, 4, 10));
  }
  const azar = new Aleatorio(7);
  for (const punto of contornoEspaciado(diseno.forma, tamano, 1.6)) {
    const indice = Math.floor(azar.siguiente() * largos.length);
    const gota = new THREE.Mesh(gotas[indice], chocolate);
    const posicion = posicionMundo(punto, 0.2);
    gota.position.set(posicion.x, altura + 0.1 - (largos[indice] / 2 + 0.5), posicion.y);
    gota.rotation.y = giroMundo(punto);
    gota.scale.set(1, 1, 0.55);
    grupo.add(gota);
  }
  return grupo;
}

// Chispas sobre la cara de arriba; dentro de "radioLibre" (donde va el mensaje) no caen chispas
function chispas(diseno: DisenoPastel, tamano: number, tamanoSiguiente: number, altura: number, radioLibre: number): THREE.InstancedMesh {
  const exterior = puntosForma(diseno.forma, tamano - 2, 120);
  let hueco: Punto2d[] = [];
  if (tamanoSiguiente > 0) {
    hueco = puntosForma(diseno.forma, tamanoSiguiente + 0.5, 120);
  }
  const cantidad = Math.min(900, Math.round(tamano * tamano * 0.35));
  const malla = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.13, 0.5, 2, 6), material('#ffffff', 0.45, 0), cantidad);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(11);
  let puestas = 0;
  let intentos = 0;
  while (puestas < cantidad && intentos < cantidad * 8) {
    intentos = intentos + 1;
    const x = (azar.siguiente() - 0.5) * tamano;
    const y = (azar.siguiente() - 0.5) * tamano;
    if (!estaDentro(x, y, exterior)) {
      continue;
    }
    if (hueco.length > 0 && estaDentro(x, y, hueco)) {
      continue;
    }
    if (Math.hypot(x, y) < radioLibre) {
      continue;
    }
    auxiliar.position.set(x, altura + 0.13, -y);
    auxiliar.rotation.set(0, azar.siguiente() * Math.PI * 2, Math.PI / 2, 'YXZ');
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    malla.setColorAt(puestas, color.set(COLORES_CHISPAS[Math.floor(azar.siguiente() * COLORES_CHISPAS.length)]));
    puestas = puestas + 1;
  }
  malla.count = puestas;
  return malla;
}

// Agrega a una geometría un color por vértice (para juntar varias piezas en una sola malla)
function conColor(geometria: THREE.BufferGeometry, color: THREE.Color): THREE.BufferGeometry {
  const cantidad = geometria.attributes['position'].count;
  const colores = new Float32Array(cantidad * 3);
  for (let i = 0; i < cantidad; i++) {
    colores[i * 3] = color.r;
    colores[i * 3 + 1] = color.g;
    colores[i * 3 + 2] = color.b;
  }
  geometria.setAttribute('color', new THREE.BufferAttribute(colores, 3));
  return geometria;
}

/**
 * Rosa de azúcar: pétalos curvos en 4 vueltas (los de adentro cerrados, los de afuera
 * abiertos) y dos hojas. Todo es una sola geometría para que muchas flores no pesen.
 * La flor mira hacia +z.
 */
export function geometriaRosa(color: string): THREE.BufferGeometry {
  const piezas: THREE.BufferGeometry[] = [];
  const base = new THREE.Color(color);
  const capas = [
    { cantidad: 3, radio: 0.06, apertura: 0.12, tamano: 0.5, aclarar: -0.12 },
    { cantidad: 5, radio: 0.2, apertura: 0.5, tamano: 0.72, aclarar: 0 },
    { cantidad: 6, radio: 0.34, apertura: 0.9, tamano: 0.92, aclarar: 0.12 },
    { cantidad: 8, radio: 0.46, apertura: 1.3, tamano: 1.05, aclarar: 0.22 },
  ];
  for (let c = 0; c < capas.length; c++) {
    const capa = capas[c];
    const tono = base.clone();
    if (capa.aclarar < 0) {
      tono.lerp(new THREE.Color('#000000'), -capa.aclarar);
    } else {
      tono.lerp(new THREE.Color('#ffffff'), capa.aclarar);
    }
    for (let k = 0; k < capa.cantidad; k++) {
      // Pétalo: un pedazo de esfera, angosto abajo y ancho arriba, curvo hacia adentro
      const petalo = new THREE.SphereGeometry(1, 10, 6, 0, Math.PI, Math.PI / 2, Math.PI / 2);
      petalo.translate(0, 1, 0);
      petalo.scale(0.55 * capa.tamano, capa.tamano, 0.42 * capa.tamano);
      // Se para sobre el eje de la flor y se abre hacia afuera
      petalo.rotateX(Math.PI / 2 + capa.apertura);
      petalo.translate(0, -capa.radio, (capas.length - c) * 0.04);
      petalo.rotateZ((k / capa.cantidad) * Math.PI * 2 + c * 0.7);
      piezas.push(conColor(petalo, tono));
    }
  }
  // Dos hojas verdes detrás de la flor
  const verde = new THREE.Color('#5f8f45');
  const angulosHojas = [2.3, 3.9];
  for (const angulo of angulosHojas) {
    const hoja = new THREE.SphereGeometry(1, 10, 6);
    hoja.scale(0.32, 0.8, 0.06);
    hoja.translate(0, -1.25, -0.1);
    hoja.rotateZ(angulo);
    piezas.push(conColor(hoja, verde));
  }
  const unida = mergeGeometries(piezas, false);
  for (const pieza of piezas) {
    pieza.dispose();
  }
  return unida;
}

export function materialRosa(): THREE.MeshStandardMaterial {
  return new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.55, side: THREE.DoubleSide });
}

/**
 * Una flor pegada al costado del piso en cada ángulo indicado, a dos alturas.
 * Una sola flor va a media altura.
 */
function floresLaterales(diseno: DisenoPastel, tamano: number, alto: number, angulos: number[], lugaresTotales: number): THREE.Group {
  const grupo = new THREE.Group();
  const cantidad = angulos.length;
  const perimetro = Math.PI * tamano;
  let escala = 1.6;
  if (lugaresTotales > 1) {
    // Más piezas alrededor = flores más chicas, para que quepan todas
    escala = Math.max(0.6, Math.min(1.45, perimetro / lugaresTotales / 3));
  }
  // Todas las flores comparten la misma geometría y material
  const geometria = geometriaRosa('#e86a92');
  const petalos = materialRosa();
  for (let k = 0; k < angulos.length; k++) {
    const punto = puntoContorno(diseno.forma, tamano, angulos[k]);
    let alturaRelativa = 0.5;
    if (cantidad > 1 && k % 2 === 0) {
      alturaRelativa = 0.62;
    } else if (cantidad > 1) {
      alturaRelativa = 0.36;
    }
    const nueva = new THREE.Mesh(geometria, petalos);
    nueva.scale.setScalar(escala);
    const posicion = posicionMundo(punto, 0.35);
    nueva.position.set(posicion.x, alto * alturaRelativa, posicion.y);
    nueva.rotation.y = giroMundo(punto);
    nueva.rotateX(-0.12);
    grupo.add(nueva);
  }
  return grupo;
}

// Piel de la fresa: rojo con semillitas amarillas
function texturaFresa(): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  const degradado = contexto.createLinearGradient(0, 0, 0, 256);
  degradado.addColorStop(0, '#b3122b');
  degradado.addColorStop(0.6, '#d7263d');
  degradado.addColorStop(1, '#e8505e');
  contexto.fillStyle = degradado;
  contexto.fillRect(0, 0, 256, 256);
  for (let fila = 0; fila < 12; fila++) {
    for (let columna = 0; columna < 10; columna++) {
      let x = columna * 25.6 + 6;
      if (fila % 2 === 1) {
        x = x + 12.8;
      }
      const y = fila * 21 + 10;
      contexto.fillStyle = 'rgba(120,10,20,0.45)';
      contexto.beginPath();
      contexto.ellipse(x, y, 4, 5, 0, 0, Math.PI * 2);
      contexto.fill();
      contexto.fillStyle = '#f2d16b';
      contexto.beginPath();
      contexto.ellipse(x, y, 1.8, 2.8, 0, 0, Math.PI * 2);
      contexto.fill();
    }
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

/**
 * Una fresa por cada unidad elegida, en anillos sobre el pastel.
 * Si son muchas, se hacen más chicas para que quepan todas sin encimarse.
 */
export function fresas(cantidad: number, altura: number, radio: number, enOrilla: boolean): THREE.Group {
  const grupo = new THREE.Group();
  let escala = 1.2;
  if (enOrilla && cantidad > 1) {
    escala = Math.max(0.55, Math.min(1.2, (Math.PI * 2 * radio) / cantidad / 2.5));
  } else if (cantidad > 1) {
    escala = Math.max(0.55, Math.min(1.2, Math.sqrt((Math.PI * radio * radio) / cantidad) / 2.6));
  }
  const lugares = lugaresEnAnillos(cantidad, radio, 2.4 * escala, enOrilla);
  grupo.add(mallaFresas(lugares, altura, escala));
  return grupo;
}

// Las fresas ya acomodadas (todas comparten geometría y material)
function mallaFresas(lugares: LugarArriba[], altura: number, escala: number): THREE.Group {
  const grupo = new THREE.Group();
  const perfil = [
    [0, 0],
    [0.8, 0.1],
    [1.1, 0.55],
    [1.05, 1.1],
    [0.78, 1.7],
    [0.35, 2.15],
    [0, 2.3],
  ];
  const puntos: THREE.Vector2[] = [];
  for (const par of perfil) {
    puntos.push(new THREE.Vector2(Math.max(0.001, par[0]), par[1]));
  }
  const cuerpo = new THREE.LatheGeometry(puntos, 24);
  // Hojitas (sépalos) en estrella alrededor de la base
  const hojitas: THREE.BufferGeometry[] = [];
  for (let k = 0; k < 6; k++) {
    const hojita = new THREE.SphereGeometry(1, 8, 6);
    hojita.scale(0.22, 0.07, 0.75);
    hojita.translate(0, 0.12, 0.85);
    hojita.rotateX(0.25);
    hojita.rotateY((k / 6) * Math.PI * 2);
    hojitas.push(hojita);
  }
  const corona = mergeGeometries(hojitas, false);
  const rojo = new THREE.MeshPhysicalMaterial({ map: texturaFresa(), roughness: 0.32, clearcoat: 0.7, clearcoatRoughness: 0.2 });
  const verde = new THREE.MeshStandardMaterial({ color: '#4f8a3a', roughness: 0.6 });
  const cuerpos = new THREE.InstancedMesh(cuerpo, rojo, lugares.length);
  const coronas = new THREE.InstancedMesh(corona, verde, lugares.length);
  const auxiliar = new THREE.Object3D();
  for (let i = 0; i < lugares.length; i++) {
    auxiliar.position.set(lugares[i].x, altura + 0.05, lugares[i].z);
    auxiliar.rotation.set(0.2, lugares[i].angulo, 0);
    auxiliar.scale.setScalar(escala);
    auxiliar.updateMatrix();
    cuerpos.setMatrixAt(i, auxiliar.matrix);
    coronas.setMatrixAt(i, auxiliar.matrix);
  }
  grupo.add(cuerpos, coronas);
  return grupo;
}

/**
 * Fruta picada sobre la superficie, entre "radioInterior" y "radio": cubitos de fruta
 * y, de vez en cuando, una mora o uva. Con mucha fruta los pedazos son más chicos.
 */
export function cubitos(cantidad: number, colores: string[], radio: number, radioInterior: number, altura: number, semilla: number, repisa: Repisa | null = null): THREE.Group {
  const grupo = new THREE.Group();
  // Cada pedazo ocupa una parte de la superficie: con mucha fruta o poco espacio, pedazos más chicos
  const superficie = Math.PI * (radio * radio - radioInterior * radioInterior);
  const escala = Math.max(0.45, Math.min(1, Math.sqrt(superficie / cantidad) / 1.7));
  const brillo = new THREE.MeshPhysicalMaterial({ roughness: 0.25, clearcoat: 0.7 });
  const cubos = new THREE.InstancedMesh(new THREE.BoxGeometry(1.1, 1.1, 1.1), brillo, cantidad);
  const bayas = new THREE.InstancedMesh(new THREE.SphereGeometry(0.6, 14, 10), brillo, cantidad);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(semilla);
  // Con radioInterior el centro queda libre (para el mensaje)
  const parteLibre = (radioInterior * radioInterior) / (radio * radio);
  let totalCubos = 0;
  let totalBayas = 0;
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    let distancia = Math.sqrt(parteLibre + azar.siguiente() * (1 - parteLibre)) * radio;
    if (repisa !== null) {
      // En la repisa de un piso de abajo la orilla depende de la forma del pastel
      distancia = distanciaEnRepisa(repisa, angulo, azar.siguiente(), 0.6 * escala);
    }
    auxiliar.position.set(Math.cos(angulo) * distancia, altura + 0.5 * escala, Math.sin(angulo) * distancia);
    auxiliar.rotation.set(azar.siguiente(), azar.siguiente() * 3, azar.siguiente());
    auxiliar.scale.setScalar(escala);
    auxiliar.updateMatrix();
    if (i % 4 === 3) {
      bayas.setMatrixAt(totalBayas, auxiliar.matrix);
      bayas.setColorAt(totalBayas, color.set(COLORES_BAYAS[totalBayas % COLORES_BAYAS.length]));
      totalBayas = totalBayas + 1;
    } else {
      cubos.setMatrixAt(totalCubos, auxiliar.matrix);
      cubos.setColorAt(totalCubos, color.set(colores[totalCubos % colores.length]));
      totalCubos = totalCubos + 1;
    }
  }
  cubos.count = totalCubos;
  bayas.count = totalBayas;
  grupo.add(cubos);
  if (totalBayas > 0) {
    grupo.add(bayas);
  }
  return grupo;
}

// Cera de vela con rayas en espiral
function texturaVela(color: string): THREE.CanvasTexture {
  const contexto = lienzo(64, 256);
  contexto.fillStyle = '#fdfaf4';
  contexto.fillRect(0, 0, 64, 256);
  contexto.strokeStyle = color;
  contexto.lineWidth = 12;
  // Rayas inclinadas que dan vuelta completa: se ven como espiral en el cilindro
  for (let x = -256; x <= 64; x = x + 32) {
    contexto.beginPath();
    contexto.moveTo(x, 0);
    contexto.lineTo(x + 256, 256);
    contexto.stroke();
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

// Resplandor suave alrededor de la llama
function texturaResplandor(): THREE.CanvasTexture {
  const contexto = lienzo(128, 128);
  const degradado = contexto.createRadialGradient(64, 64, 0, 64, 64, 64);
  degradado.addColorStop(0, 'rgba(255,220,140,0.85)');
  degradado.addColorStop(0.35, 'rgba(255,170,70,0.35)');
  degradado.addColorStop(1, 'rgba(255,150,50,0)');
  contexto.fillStyle = degradado;
  contexto.fillRect(0, 0, 128, 128);
  return new THREE.CanvasTexture(contexto.canvas);
}

// Cuánto de la vela (mide 7 de alto) queda enterrado en el postre
const HUNDIDO_VELA = 2.6;

/**
 * Una vela por cada unidad elegida: una sola va al centro; varias, en anillos.
 * Con muchas velas, se hacen más delgadas y bajitas para que quepan.
 */
export function velas(cantidad: number, radio: number, altura: number, llamas: THREE.Mesh[], enOrilla: boolean): THREE.Group {
  const grupo = new THREE.Group();
  let escala = 1;
  if (cantidad > 16) {
    escala = 0.7;
  } else if (cantidad > 8) {
    escala = 0.85;
  }
  const cera = new THREE.CylinderGeometry(0.38, 0.38, 7, 18);
  const pabilo = new THREE.CylinderGeometry(0.06, 0.06, 0.8, 6);
  const fuego = new THREE.SphereGeometry(0.32, 14, 10);
  const colores = ['#5fa8e0', '#f06a9b', '#f5c033', '#76c27a'];
  const ceras: THREE.MeshStandardMaterial[] = [];
  for (const color of colores) {
    ceras.push(new THREE.MeshStandardMaterial({ map: texturaVela(color), roughness: 0.5 }));
  }
  const negro = material('#2b2320', 0.8, 0);
  const fuegoExterior = new THREE.MeshBasicMaterial({ color: '#ffae42' });
  const fuegoInterior = new THREE.MeshBasicMaterial({ color: '#fff4c2' });
  const resplandor = new THREE.SpriteMaterial({ map: texturaResplandor(), blending: THREE.AdditiveBlending, depthWrite: false, transparent: true });
  const lugares = lugaresEnAnillos(cantidad, radio, 2.3 * escala, enOrilla);
  for (let i = 0; i < lugares.length; i++) {
    const vela = new THREE.Group();
    const cuerpo = new THREE.Mesh(cera, ceras[i % ceras.length]);
    cuerpo.position.y = 3.5;
    const mecha = new THREE.Mesh(pabilo, negro);
    mecha.position.y = 7.35;
    const llama = new THREE.Mesh(fuego, fuegoExterior);
    llama.position.y = 8.1;
    llama.scale.set(1, 2.1, 1);
    const centro = new THREE.Mesh(fuego, fuegoInterior);
    centro.position.y = 7.95;
    centro.scale.set(0.5, 1.1, 0.5);
    const brillo = new THREE.Sprite(resplandor);
    brillo.position.y = 8.2;
    brillo.scale.set(2.4, 3.2, 1);
    vela.add(cuerpo, mecha, llama, centro, brillo);
    // La vela se clava en el postre: una parte de la cera queda enterrada
    vela.position.set(lugares[i].x, altura - HUNDIDO_VELA * escala, lugares[i].z);
    vela.scale.setScalar(escala);
    grupo.add(vela);
    llamas.push(llama);
  }
  const luz = new THREE.PointLight('#ffb35c', 30, 45, 2);
  luz.position.set(0, altura + 9, 0);
  grupo.add(luz);
  return grupo;
}

// Letra oscura sobre fondos claros y clara sobre fondos oscuros (cobertura o goteo de chocolate)
export function colorDedicatoria(colorFondo: string): string {
  const fondo = new THREE.Color(colorFondo);
  const luz = fondo.r * 0.3 + fondo.g * 0.59 + fondo.b * 0.11;
  if (luz < 0.35) {
    return '#fdf3e3';
  }
  return '#5a3221';
}

/**
 * Mensaje recto en el centro de la cara de arriba. "radioLibre" es cuánto espacio queda
 * libre en el centro (las piezas que rodean el pastel quedan fuera de ese círculo).
 */
export function dedicatoria(diseno: DisenoPastel, altura: number, radioLibre: number, colorFondo: string): THREE.Mesh {
  // El renglón más ancho cabe en el círculo libre
  const ancho = radioLibre * 1.9;
  const plano = new THREE.Mesh(
    new THREE.PlaneGeometry(ancho, ancho / 2),
    new THREE.MeshStandardMaterial({ map: texturaDedicatoria(diseno.dedicatoria, colorDedicatoria(colorFondo)), transparent: true, roughness: 0.4 }),
  );
  plano.rotation.x = -Math.PI / 2;
  plano.position.set(0, altura + 0.08, 0);
  return plano;
}

/**
 * Mensaje en arco alrededor del objeto que está en el centro, dentro de la franja
 * entre "radioInterior" y "radioExterior" (medidas del pastel).
 */
export function dedicatoriaEnArco(diseno: DisenoPastel, tamano: number, altura: number, radioInterior: number, radioExterior: number, colorFondo: string): THREE.Mesh {
  const lado = tamano * 0.86;
  const pixeles = LADO_ARCO / lado;
  const textura = texturaDedicatoriaArco(diseno.dedicatoria, colorDedicatoria(colorFondo), radioInterior * pixeles, radioExterior * pixeles);
  const plano = new THREE.Mesh(
    new THREE.PlaneGeometry(lado, lado),
    new THREE.MeshStandardMaterial({ map: textura, transparent: true, roughness: 0.4 }),
  );
  plano.rotation.x = -Math.PI / 2;
  plano.position.set(0, altura + 0.08, 0);
  return plano;
}

// ===================== Base (plato) =====================

function base(diseno: DisenoPastel, tamano: number): { grupo: THREE.Group; arriba: number } {
  if (diseno.tipo === 'gelatina') {
    // La gelatina va en un plato (o la base elegida) siempre redondo
    return basePostre(diseno.base, 'redondo', tamano + 8, 0);
  }
  return basePostre(diseno.base, diseno.forma, tamano + 7, 0);
}

// ===================== Bases (plato, tabla, pizarra, pedestal) =====================

// Contorno de la base: la forma del postre, o un rectángulo con esquinas redondas si "forma" es null
function contornoBase(forma: FormaPastel | null, ancho: number, fondo: number): THREE.Shape {
  if (forma !== null) {
    return formaThree(puntosForma(forma, ancho, 180));
  }
  const figura = new THREE.Shape();
  const radio = 1.5;
  const x = ancho / 2;
  const y = fondo / 2;
  figura.moveTo(-x + radio, -y);
  figura.lineTo(x - radio, -y);
  figura.quadraticCurveTo(x, -y, x, -y + radio);
  figura.lineTo(x, y - radio);
  figura.quadraticCurveTo(x, y, x - radio, y);
  figura.lineTo(-x + radio, y);
  figura.quadraticCurveTo(-x, y, -x, y - radio);
  figura.lineTo(-x, -y + radio);
  figura.quadraticCurveTo(-x, -y, -x + radio, -y);
  return figura;
}

// Placa acostada con orilla redondeada. Queda apoyada en y = 0 y su cara de arriba en grueso + 2 * orilla.
function losa(contorno: THREE.Shape, grueso: number, orilla: number, materialLosa: THREE.Material): THREE.Mesh {
  const geometria = new THREE.ExtrudeGeometry(contorno, {
    depth: grueso,
    bevelEnabled: true,
    bevelThickness: orilla,
    bevelSize: orilla * 1.5,
    bevelSegments: 2,
    curveSegments: 12,
  });
  geometria.rotateX(-Math.PI / 2);
  const malla = new THREE.Mesh(geometria, materialLosa);
  malla.position.y = orilla;
  return malla;
}

// Plato redondo de cerámica blanca
export function platoRedondo(radio: number): THREE.Mesh {
  const perfil = [
    new THREE.Vector2(0, 0),
    new THREE.Vector2(radio * 0.7, 0),
    new THREE.Vector2(radio * 0.75, 0.6),
    new THREE.Vector2(radio * 0.92, 0.9),
    new THREE.Vector2(radio, 1.8),
    new THREE.Vector2(radio * 0.98, 2),
    new THREE.Vector2(radio * 0.9, 1.3),
    new THREE.Vector2(0, 1.2),
  ];
  const porcelana = new THREE.MeshPhysicalMaterial({ color: '#f7f5f0', roughness: 0.25, clearcoat: 0.6, side: THREE.DoubleSide });
  return new THREE.Mesh(new THREE.LatheGeometry(perfil, 72), porcelana);
}

function materialMadera(): THREE.MeshStandardMaterial {
  const vetas = texturaEspatula(23);
  vetas.repeat.set(1 / 20, 1 / 6);
  return new THREE.MeshStandardMaterial({ color: '#c99a6b', roughness: 0.75, bumpMap: vetas, bumpScale: 1 });
}

export function materialCristal(): THREE.MeshPhysicalMaterial {
  return new THREE.MeshPhysicalMaterial({
    color: '#e8f5fa',
    roughness: 0.05,
    metalness: 0,
    clearcoat: 1,
    clearcoatRoughness: 0.05,
    transparent: true,
    opacity: 0.4,
    side: THREE.DoubleSide,
    depthWrite: false,
  });
}

// Pedestal de cristal: pie, tallo y plato arriba (el postre queda a la altura "alto")
function pedestal(radio: number, alto: number): THREE.Mesh {
  const perfil = [
    new THREE.Vector2(0, 0),
    new THREE.Vector2(radio * 0.5, 0),
    new THREE.Vector2(radio * 0.52, 0.3),
    new THREE.Vector2(radio * 0.2, 0.7),
    new THREE.Vector2(radio * 0.08, 1.4),
    new THREE.Vector2(radio * 0.07, alto - 1.6),
    new THREE.Vector2(radio * 0.14, alto - 0.9),
    new THREE.Vector2(radio * 0.6, alto - 0.45),
    new THREE.Vector2(radio, alto - 0.25),
    new THREE.Vector2(radio * 1.01, alto + 0.15),
    new THREE.Vector2(radio * 0.97, alto + 0.2),
    new THREE.Vector2(radio * 0.9, alto),
    new THREE.Vector2(0, alto),
  ];
  const malla = new THREE.Mesh(new THREE.LatheGeometry(perfil, 72), materialCristal());
  malla.userData['sinSombra'] = true;
  return malla;
}

/**
 * Base donde se presenta el postre según lo que eligió el cliente:
 *  dorada / plateada = cartón metálico con la forma del postre; madera = tabla; pizarra = piedra negra;
 *  blanca = plato de cerámica; cristal = pedestal (o charola de cristal para piezas).
 * "forma" null = base rectangular de "ancho" x "fondo" (para varias piezas).
 * Regresa la base y la altura donde se apoya el postre.
 */
export function basePostre(tipo: TipoBase, forma: FormaPastel | null, ancho: number, fondo: number): { grupo: THREE.Group; arriba: number } {
  const grupo = new THREE.Group();
  let arriba = 1.4;
  if (tipo === 'ninguna') {
    // «Sin base»: el postre se apoya directo
    return { grupo: grupo, arriba: 0 };
  }
  if (tipo === 'blanca' && forma !== null) {
    grupo.add(platoRedondo(ancho / 2));
    arriba = 1.25;
  } else if (tipo === 'blanca') {
    const porcelana = new THREE.MeshPhysicalMaterial({ color: '#f7f5f0', roughness: 0.25, clearcoat: 0.6 });
    grupo.add(losa(contornoBase(null, ancho, fondo), 0.6, 0.35, porcelana));
    arriba = 1.3;
  } else if (tipo === 'cristal' && forma !== null) {
    const alto = Math.max(4.5, Math.min(9, ancho * 0.32));
    grupo.add(pedestal(ancho / 2, alto));
    arriba = alto;
  } else if (tipo === 'cristal') {
    // Charola de cristal con patitas
    const charola = losa(contornoBase(null, ancho, fondo), 0.5, 0.25, materialCristal());
    charola.position.y = 2;
    charola.userData['sinSombra'] = true;
    grupo.add(charola);
    const patas = [-1, 1];
    for (const lx of patas) {
      for (const lz of patas) {
        const pata = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.6, 2, 16), materialCristal());
        pata.position.set(lx * (ancho / 2 - 2), 1, lz * (fondo / 2 - 2));
        pata.userData['sinSombra'] = true;
        grupo.add(pata);
      }
    }
    arriba = 3;
  } else if (tipo === 'madera') {
    let contorno = contornoBase(null, ancho, fondo);
    if (forma !== null) {
      // Tabla redonda (un poco más ancha que el postre)
      contorno = contornoBase('redondo', ancho, 0);
    }
    grupo.add(losa(contorno, 1.4, 0.25, materialMadera()));
    arriba = 1.9;
  } else if (tipo === 'pizarra') {
    let contorno = contornoBase(null, ancho, fondo);
    if (forma !== null) {
      contorno = contornoBase('redondo', ancho, 0);
    }
    const relieve = texturaRelieve(21);
    relieve.repeat.set(1 / 12, 1 / 12);
    const piedra = new THREE.MeshStandardMaterial({ color: '#34373c', roughness: 0.88, bumpMap: relieve, bumpScale: 0.6 });
    grupo.add(losa(contorno, 0.7, 0.2, piedra));
    arriba = 1.1;
  } else {
    // Cartón dorado o plateado con la forma del postre
    let metal = material('#c9952f', 0.3, 0.9);
    if (tipo === 'plateada') {
      metal = material('#d3d6db', 0.25, 0.95);
    }
    grupo.add(losa(contornoBase(forma, ancho, fondo), 1, 0.2, metal));
    arriba = 1.4;
  }
  conSombras(grupo);
  return { grupo: grupo, arriba: arriba };
}

// ===================== Topper, frutos rojos y hoja de oro =====================

/**
 * Topper: letrero de pie con el texto en dorado, sostenido por dos palitos.
 * Sale apoyado en y = 0 (los palitos se entierran un poco) y mirando al frente (+z).
 */
export function topper(texto: string, ancho: number): THREE.Group {
  const grupo = new THREE.Group();
  const medidas = medidasTopper(texto);
  const renglones = medidas.renglones;
  const tamanoLetra = medidas.tamanoLetra;
  const altoLienzo = medidas.altoLienzo;
  const contexto = lienzo(1024, altoLienzo);
  contexto.font = fuenteTopper(tamanoLetra);
  contexto.textAlign = 'center';
  contexto.textBaseline = 'middle';
  // Contorno grueso: así las letras quedan unidas como un acrílico recortado
  contexto.lineJoin = 'round';
  contexto.strokeStyle = '#b8892c';
  contexto.lineWidth = Math.max(14, tamanoLetra * 0.18);
  const separacion = tamanoLetra * 1.15;
  // Centro del primer renglón (el lienzo mide justo lo que ocupan los renglones más el margen)
  const primero = MARGEN_TOPPER + tamanoLetra * 0.62;
  for (let i = 0; i < renglones.length; i++) {
    contexto.strokeText(renglones[i], 512, primero + i * separacion);
  }
  const brillo = contexto.createLinearGradient(0, 0, 0, altoLienzo);
  brillo.addColorStop(0, '#fff1b8');
  brillo.addColorStop(0.5, '#e3b54a');
  brillo.addColorStop(1, '#a8761f');
  contexto.fillStyle = brillo;
  for (let i = 0; i < renglones.length; i++) {
    contexto.fillText(renglones[i], 512, primero + i * separacion);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  const alto = (ancho * altoLienzo) / 1024;
  const margen = (ancho * MARGEN_TOPPER) / 1024;
  const letrero = new THREE.Mesh(
    new THREE.PlaneGeometry(ancho, alto),
    new THREE.MeshStandardMaterial({ map: textura, transparent: true, alphaTest: 0.35, metalness: 0.55, roughness: 0.3, side: THREE.DoubleSide }),
  );
  // La orilla de abajo de las letras queda a la altura del palito (el margen transparente se encima)
  letrero.position.y = ALTO_PALITO_TOPPER + alto / 2 - margen;
  grupo.add(letrero);
  // Los palitos suben hasta la mitad del último renglón: así el letrero nunca se ve flotando
  const arribaPalito = ALTO_PALITO_TOPPER + (ancho * tamanoLetra * 0.6) / 1024;
  const abajoPalito = -1.5;
  const oro = material('#d4a645', 0.3, 0.8);
  const lados = [-ancho * 0.22, ancho * 0.22];
  for (const x of lados) {
    const palito = new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.11, arribaPalito - abajoPalito, 8), oro);
    palito.position.set(x, (arribaPalito + abajoPalito) / 2, -0.05);
    grupo.add(palito);
  }
  return grupo;
}

// Alto del palito del topper que queda a la vista (de donde se entierra a donde empiezan las letras)
export const ALTO_PALITO_TOPPER = 3.6;
// Margen transparente alrededor de las letras en el lienzo del topper (para el contorno)
const MARGEN_TOPPER = 40;

function fuenteTopper(tamanoLetra: number): string {
  return 'italic 700 ' + tamanoLetra + 'px Georgia, "Times New Roman", serif';
}

/**
 * Renglones, tamaño de letra y alto del lienzo del topper.
 * El lienzo mide justo lo que ocupan las letras: con dos o más renglones el letrero crece
 * solo lo necesario (antes quedaba espacio vacío abajo y el texto se veía muy arriba).
 */
function medidasTopper(texto: string): { renglones: string[]; tamanoLetra: number; altoLienzo: number } {
  const renglones = renglonesTopper(texto);
  const medidor = lienzo(1024, 16);
  let tamanoLetra = 170;
  medidor.font = fuenteTopper(tamanoLetra);
  while (renglonMasAncho(medidor, renglones) > 900 && tamanoLetra > 40) {
    tamanoLetra = tamanoLetra - 6;
    medidor.font = fuenteTopper(tamanoLetra);
  }
  const altoLienzo = Math.round(MARGEN_TOPPER * 2 + tamanoLetra * 1.24 + tamanoLetra * 1.15 * (renglones.length - 1));
  return { renglones: renglones, tamanoLetra: tamanoLetra, altoLienzo: altoLienzo };
}

// Parte el texto del topper en renglones cortos (máximo 4; lo que sobra va en el último)
function renglonesTopper(texto: string): string[] {
  const palabras = texto.split(' ');
  const renglones: string[] = [];
  let actual = '';
  for (const palabra of palabras) {
    if (palabra === '') {
      continue;
    }
    let junto = palabra;
    if (actual !== '') {
      junto = actual + ' ' + palabra;
    }
    if (junto.length > 16 && actual !== '' && renglones.length < 3) {
      renglones.push(actual);
      actual = palabra;
    } else {
      actual = junto;
    }
  }
  if (actual !== '' || renglones.length === 0) {
    renglones.push(actual);
  }
  return renglones;
}

// Ancho del renglón más largo con la letra actual
function renglonMasAncho(contexto: CanvasRenderingContext2D, renglones: string[]): number {
  let mayor = 0;
  for (const renglon of renglones) {
    mayor = Math.max(mayor, contexto.measureText(renglon).width);
  }
  return mayor;
}

// Altura total de un topper de cierto ancho (para encuadrar la cámara)
export function altoTopper(ancho: number, texto: string = ''): number {
  const medidas = medidasTopper(texto);
  return ALTO_PALITO_TOPPER + (ancho * (medidas.altoLienzo - MARGEN_TOPPER)) / 1024;
}

// Frambuesa o zarzamora: bolitas (drupas) juntas en forma de domo. Una sola geometría.
function geometriaDrupas(): THREE.BufferGeometry {
  const piezas: THREE.BufferGeometry[] = [];
  const total = 30;
  for (let i = 0; i < total; i++) {
    // Puntos repartidos en una esfera (espiral de Fibonacci), sin la parte de abajo
    const y = 1 - (i / (total - 1)) * 1.7;
    const radio = Math.sqrt(Math.max(0, 1 - y * y));
    const angulo = i * 2.39996;
    const bolita = new THREE.SphereGeometry(0.2, 8, 6);
    bolita.translate(Math.cos(angulo) * radio * 0.5, 0.55 + y * 0.62, Math.sin(angulo) * radio * 0.5);
    piezas.push(bolita);
  }
  const juntas = mergeGeometries(piezas);
  for (const pieza of piezas) {
    pieza.dispose();
  }
  return juntas;
}

/**
 * Frutos rojos: frambuesas, zarzamoras y arándanos. Cada unidad elegida es un puñito de 3.
 * Se reparten en un anillo (radioInterior deja libre el centro para el mensaje).
 */
export function frutosRojos(cantidad: number, radio: number, radioInterior: number, altura: number, semilla: number, repisa: Repisa | null = null): THREE.Group {
  const grupo = new THREE.Group();
  const total = Math.min(90, cantidad * 3);
  const superficie = Math.PI * (radio * radio - radioInterior * radioInterior);
  const escala = Math.max(0.55, Math.min(1, Math.sqrt(superficie / total) / 1.5));
  const brillo = new THREE.MeshPhysicalMaterial({ roughness: 0.35, clearcoat: 0.6, clearcoatRoughness: 0.4 });
  const drupas = new THREE.InstancedMesh(geometriaDrupas(), brillo, total);
  const arandanos = new THREE.InstancedMesh(new THREE.SphereGeometry(0.55, 16, 12), new THREE.MeshStandardMaterial({ color: '#3f4a86', roughness: 0.55 }), total);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(semilla);
  const parteLibre = (radioInterior * radioInterior) / Math.max(0.01, radio * radio);
  let totalDrupas = 0;
  let totalArandanos = 0;
  for (let i = 0; i < total; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    let distancia = Math.sqrt(parteLibre + azar.siguiente() * (1 - parteLibre)) * radio;
    if (repisa !== null) {
      distancia = distanciaEnRepisa(repisa, angulo, azar.siguiente(), 0.6 * escala);
    }
    auxiliar.position.set(Math.cos(angulo) * distancia, altura, Math.sin(angulo) * distancia);
    auxiliar.rotation.set((azar.siguiente() - 0.5) * 0.6, azar.siguiente() * 6, (azar.siguiente() - 0.5) * 0.6);
    auxiliar.scale.setScalar(escala);
    if (i % 3 === 2) {
      auxiliar.position.y = altura + 0.45 * escala;
      auxiliar.updateMatrix();
      arandanos.setMatrixAt(totalArandanos, auxiliar.matrix);
      totalArandanos = totalArandanos + 1;
    } else {
      auxiliar.updateMatrix();
      drupas.setMatrixAt(totalDrupas, auxiliar.matrix);
      // Frambuesa roja o zarzamora casi negra
      if (i % 3 === 0) {
        color.set('#c7243a');
      } else {
        color.set('#2b1030');
      }
      drupas.setColorAt(totalDrupas, color);
      totalDrupas = totalDrupas + 1;
    }
  }
  drupas.count = totalDrupas;
  arandanos.count = totalArandanos;
  grupo.add(drupas);
  if (totalArandanos > 0) {
    grupo.add(arandanos);
  }
  return grupo;
}

// Hojuela de oro: pedacito irregular (mira hacia +z)
export function geometriaHojuela(): THREE.BufferGeometry {
  const figura = new THREE.Shape();
  const azar = new Aleatorio(5);
  const lados = 9;
  for (let i = 0; i < lados; i++) {
    const angulo = (i / lados) * Math.PI * 2;
    const radio = 0.32 + azar.siguiente() * 0.38;
    const x = Math.cos(angulo) * radio;
    const y = Math.sin(angulo) * radio * 0.8;
    if (i === 0) {
      figura.moveTo(x, y);
    } else {
      figura.lineTo(x, y);
    }
  }
  return new THREE.ShapeGeometry(figura);
}

export function materialOro(): THREE.MeshStandardMaterial {
  return new THREE.MeshStandardMaterial({ color: '#e2b84a', metalness: 1, roughness: 0.28, side: THREE.DoubleSide });
}

/**
 * Hoja de oro en un piso: hojuelas pegadas en la parte alta de los lados y algunas arriba.
 * "radioLibre" deja sin hojuelas el centro (donde va el mensaje).
 */
function hojasDeOro(diseno: DisenoPastel, tamano: number, alto: number, arribaPiso: number, conCorte: boolean, radioLibre: number): THREE.InstancedMesh {
  const contorno = contornoEspaciado(diseno.forma, tamano, 0.3);
  const enLados = Math.min(260, Math.round(contorno.length * 0.3 * alto * 0.14));
  const enArriba = Math.min(120, Math.round(tamano * tamano * 0.05));
  const malla = new THREE.InstancedMesh(geometriaHojuela(), materialOro(), enLados + enArriba);
  const auxiliar = new THREE.Object3D();
  const azar = new Aleatorio(29);
  let puestas = 0;
  for (let i = 0; i < enLados; i++) {
    const punto = contorno[Math.floor(azar.siguiente() * contorno.length)];
    if (conCorte && dentroDelCorte(Math.atan2(punto.y, punto.x))) {
      continue;
    }
    const posicion = posicionMundo(punto, 0.08);
    // Más hojuelas cerca de la orilla de arriba
    const altura = alto * (0.4 + 0.58 * Math.sqrt(azar.siguiente()));
    auxiliar.position.set(posicion.x, altura, posicion.y);
    auxiliar.rotation.set(0, giroMundo(punto), azar.siguiente() * Math.PI * 2, 'YXZ');
    auxiliar.scale.setScalar(0.6 + azar.siguiente() * 0.9);
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    puestas = puestas + 1;
  }
  const orilla = puntosForma(diseno.forma, tamano - 1, 120);
  let intentos = 0;
  let arriba = 0;
  while (arriba < enArriba && intentos < enArriba * 8) {
    intentos = intentos + 1;
    const x = (azar.siguiente() - 0.5) * tamano;
    const y = (azar.siguiente() - 0.5) * tamano;
    if (!estaDentro(x, y, orilla) || Math.hypot(x, y) < radioLibre) {
      continue;
    }
    if (conCorte && dentroDelCorte(Math.atan2(y, x))) {
      continue;
    }
    auxiliar.position.set(x, arribaPiso + 0.06, -y);
    auxiliar.rotation.set(-Math.PI / 2, 0, azar.siguiente() * Math.PI * 2);
    auxiliar.scale.setScalar(0.6 + azar.siguiente() * 0.8);
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    puestas = puestas + 1;
    arriba = arriba + 1;
  }
  malla.count = puestas;
  malla.userData['sinRecorte'] = true;
  return malla;
}

// ===================== Gelatina =====================

function cuerpoGelatina(diseno: DisenoPastel): THREE.Group {
  const grupo = new THREE.Group();
  const radio = diseno.diametro / 2;
  const alto = diseno.alturaPiso;
  // Perfil de molde con escalones (de abajo hacia arriba)
  const perfil: THREE.Vector2[] = [];
  const pasos = 28;
  for (let i = 0; i <= pasos; i++) {
    const t = i / pasos;
    let r = radio * (1 - 0.32 * Math.pow(t, 1.4));
    // Escalones del molde
    r = r * (1 + 0.035 * Math.cos(t * Math.PI * 6));
    perfil.push(new THREE.Vector2(r, t * alto));
  }
  // Tapa redondeada
  for (let i = 1; i <= 8; i++) {
    const t = i / 8;
    perfil.push(new THREE.Vector2(radio * 0.68 * Math.cos((t * Math.PI) / 2), alto + Math.sin((t * Math.PI) / 2) * 1.2));
  }
  const geometria = new THREE.LatheGeometry(perfil, 96);
  // Gajos del molde: el radio sube y baja según el ángulo
  const posiciones = geometria.attributes['position'];
  for (let i = 0; i < posiciones.count; i++) {
    const x = posiciones.getX(i);
    const z = posiciones.getZ(i);
    const angulo = Math.atan2(z, x);
    const factor = 1 + 0.05 * Math.cos(angulo * 10);
    posiciones.setX(i, x * factor);
    posiciones.setZ(i, z * factor);
  }
  geometria.computeVertexNormals();

  let paso = 0.65;
  let rugosidad = 0.12;
  if (diseno.mosaico) {
    // Gelatina de leche: lechosa, deja ver los cubitos de colores por dentro
    paso = 0.75;
    rugosidad = 0.35;
  }
  const gel = new THREE.MeshPhysicalMaterial({
    color: diseno.colorBizcocho,
    roughness: rugosidad,
    transmission: paso,
    thickness: radio,
    ior: 1.33,
    clearcoat: 1,
    clearcoatRoughness: 0.1,
  });
  const malla = new THREE.Mesh(geometria, gel);
  malla.castShadow = true;
  grupo.add(malla);

  if (diseno.mosaico) {
    const cantidad = Math.round(radio * radio * 0.9);
    const malla2 = new THREE.InstancedMesh(new THREE.BoxGeometry(1.4, 1.4, 1.4), new THREE.MeshStandardMaterial({ roughness: 0.3 }), cantidad);
    const auxiliar = new THREE.Object3D();
    const color = new THREE.Color();
    const azar = new Aleatorio(21);
    for (let i = 0; i < cantidad; i++) {
      const altura = 0.8 + azar.siguiente() * (alto - 1.6);
      const limite = radio * (1 - 0.32 * Math.pow(altura / alto, 1.4)) - 1.4;
      const angulo = azar.siguiente() * Math.PI * 2;
      const distancia = Math.sqrt(azar.siguiente()) * Math.max(0.5, limite);
      auxiliar.position.set(Math.cos(angulo) * distancia, altura, Math.sin(angulo) * distancia);
      auxiliar.rotation.set(azar.siguiente(), azar.siguiente(), azar.siguiente());
      auxiliar.updateMatrix();
      malla2.setMatrixAt(i, auxiliar.matrix);
      malla2.setColorAt(i, color.set(COLORES_MOSAICO[i % COLORES_MOSAICO.length]));
    }
    grupo.add(malla2);
  }
  return grupo;
}

// Altura de la tapa redondeada de la gelatina a cierta distancia del centro
function alturaTapaGelatina(radio: number, arribaMolde: number, distancia: number): number {
  const parte = distancia / (radio * 0.68);
  return arribaMolde + 1.2 * Math.sqrt(Math.max(0, 1 - parte * parte));
}

// Flores acostadas sobre la tapa de la gelatina, en anillo (una sola va al centro)
function floresGelatina(cantidad: number, radio: number, arribaMolde: number): THREE.Group {
  const grupo = new THREE.Group();
  const geometria = geometriaRosa('#e86a92');
  const petalos = materialRosa();
  let anillo = radio * 0.42;
  if (cantidad === 1) {
    anillo = 0;
  }
  const escala = Math.max(0.55, Math.min(1.2, (Math.PI * 2 * Math.max(anillo, 2)) / cantidad / 3));
  for (let k = 0; k < cantidad; k++) {
    const angulo = (k / cantidad) * Math.PI * 2 + Math.PI / 2;
    const nueva = new THREE.Mesh(geometria, petalos);
    nueva.scale.setScalar(escala);
    nueva.position.set(Math.cos(angulo) * anillo, alturaTapaGelatina(radio, arribaMolde, anillo) + 0.2, Math.sin(angulo) * anillo);
    // La flor mira hacia arriba
    nueva.rotation.x = -Math.PI / 2;
    nueva.rotateZ(angulo);
    grupo.add(nueva);
  }
  return grupo;
}

// Chispas de colores sobre la tapa de la gelatina
function chispasGelatina(radio: number, arribaMolde: number): THREE.InstancedMesh {
  const cantidad = Math.min(500, Math.round(radio * radio * 1.4));
  const malla = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.11, 0.42, 2, 6), material('#ffffff', 0.45, 0), cantidad);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(13);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    const distancia = Math.sqrt(azar.siguiente()) * radio * 0.62;
    auxiliar.position.set(Math.cos(angulo) * distancia, alturaTapaGelatina(radio, arribaMolde, distancia) + 0.08, Math.sin(angulo) * distancia);
    auxiliar.rotation.set(0, azar.siguiente() * Math.PI * 2, Math.PI / 2, 'YXZ');
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
    malla.setColorAt(i, color.set(COLORES_CHISPAS[i % COLORES_CHISPAS.length]));
  }
  return malla;
}

// Collar de perlas sobre el plato, alrededor de la gelatina
function perlasGelatina(radio: number, arribaPlato: number): THREE.InstancedMesh {
  const anillo = radio * 1.1 + 0.9;
  const cantidad = Math.round((Math.PI * 2 * anillo) / 1.5);
  const nacar = new THREE.MeshPhysicalMaterial({ color: '#f4efe6', roughness: 0.18, metalness: 0.1, clearcoat: 1, iridescence: 0.7, iridescenceIOR: 1.6 });
  const malla = new THREE.InstancedMesh(new THREE.SphereGeometry(0.7, 16, 12), nacar, cantidad);
  const auxiliar = new THREE.Object3D();
  for (let i = 0; i < cantidad; i++) {
    const angulo = (i / cantidad) * Math.PI * 2;
    auxiliar.position.set(Math.cos(angulo) * anillo, arribaPlato + 0.7, Math.sin(angulo) * anillo);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

// ===================== Armado principal =====================

/**
 * RF-5 · Paso 4: arma todo el postre. "verDentro" (RF-5 · Paso 8) quita una rebanada del piso de arriba para ver
 * el bizcocho y el relleno.
 */
export function construirPastel(diseno: DisenoPastel, verDentro: boolean): PastelConstruido {
  const raiz = new THREE.Group();
  const llamas: THREE.Mesh[] = [];
  // Cada decoración trae cuántas piezas eligió el cliente: se dibuja una pieza por unidad
  const decoraciones = diseno.decoraciones;

  if (diseno.tipo === 'gelatina') {
    const plato = base(diseno, diseno.diametro);
    raiz.add(plato.grupo);
    const gelatina = cuerpoGelatina(diseno);
    gelatina.position.y = plato.arriba;
    raiz.add(gelatina);
    const arriba = plato.arriba + diseno.alturaPiso + 1.2;
    const radioArriba = (diseno.diametro / 2) * 0.55;
    if (decoraciones.rosetas > 0) {
      const anillo = new THREE.Group();
      const geometria = geometriaRoseta();
      const crema = material('#ffffff', 0.8, 0);
      // Si son muchas o la gelatina es chica, las rosetas se achican para que quepan
      let escalaRoseta = 1;
      if (decoraciones.rosetas > 1) {
        escalaRoseta = Math.max(0.45, Math.min(1, (Math.PI * 2 * radioArriba) / decoraciones.rosetas / 2.7));
      }
      for (const lugar of lugaresEnAnillos(decoraciones.rosetas, radioArriba, 2.7 * escalaRoseta, false)) {
        const roseta = new THREE.Mesh(geometria, crema);
        roseta.scale.setScalar(escalaRoseta);
        roseta.position.set(lugar.x, arriba - 0.5, lugar.z);
        anillo.add(roseta);
      }
      raiz.add(anillo);
    }
    if (decoraciones.frutaPicada > 0) {
      raiz.add(cubitos(decoraciones.frutaPicada, COLORES_FRUTA, radioArriba * 0.8, 0, arriba - 0.6, 3));
    }
    if (decoraciones.frutosRojos > 0) {
      raiz.add(frutosRojos(decoraciones.frutosRojos, radioArriba * 0.8, 0, arriba - 0.6, 8));
    }
    if (decoraciones.fresas > 0) {
      raiz.add(fresas(decoraciones.fresas, arriba - 0.4, radioArriba * 0.7, false));
    }
    let alturaVelas = 0;
    if (decoraciones.velas > 0) {
      raiz.add(velas(decoraciones.velas, radioArriba * 0.6, arriba - 0.3, llamas, false));
      alturaVelas = 8.5;
    }
    // Las demás decoraciones también se acomodan en la gelatina
    const radioGelatina = diseno.diametro / 2;
    if (decoraciones.flores > 0) {
      raiz.add(floresGelatina(decoraciones.flores, radioGelatina, plato.arriba + diseno.alturaPiso));
    }
    if (decoraciones.chispas > 0) {
      raiz.add(chispasGelatina(radioGelatina, plato.arriba + diseno.alturaPiso));
    }
    if (decoraciones.perlas > 0) {
      raiz.add(perlasGelatina(radioGelatina, plato.arriba));
    }
    if (diseno.topper !== '') {
      const anchoLetrero = Math.min(16, diseno.diametro * 0.75);
      const letrero = topper(diseno.topper, anchoLetrero);
      letrero.position.set(0, arriba - 0.4, -radioArriba * 0.45);
      raiz.add(letrero);
      alturaVelas = Math.max(alturaVelas, altoTopper(anchoLetrero, diseno.topper));
    }
    conSombras(raiz);
    return { grupo: raiz, alturaTotal: arriba + alturaVelas, anchoTotal: diseno.diametro + 8, llamas: llamas };
  }

  // Medidas de cada piso: cada uno 26 % más chico que el de abajo
  const tamanos: number[] = [];
  for (let i = 0; i < diseno.pisos; i++) {
    tamanos.push(Math.max(10, diseno.diametro * (1 - 0.26 * i)));
  }
  const alto = diseno.alturaPiso;
  const plato = base(diseno, tamanos[0]);
  raiz.add(plato.grupo);

  // Rosetas, fresas y fruta se reparten entre todos los pisos (las que sobran van a los de arriba).
  // En los pisos de abajo se acomodan en la repisa que deja libre el piso de encima.
  const rosetasPorPiso: number[] = [];
  for (let i = 0; i < diseno.pisos; i++) {
    rosetasPorPiso.push(piezasDelPiso(decoraciones.rosetas, diseno.pisos - 1 - i, diseno.pisos));
  }

  let altura = plato.arriba;
  let alturaVelas = 0;
  for (let i = 0; i < diseno.pisos; i++) {
    const tamano = tamanos[i];
    const esArriba = i === diseno.pisos - 1;
    // Posición del piso contando desde arriba (0 = el de hasta arriba)
    const desdeArriba = diseno.pisos - 1 - i;
    // «Ver por dentro» quita la misma rebanada de todos los pisos (se ven las capas de cada uno)
    const conCorte = verDentro;
    const piso = new THREE.Group();
    piso.position.y = altura;
    piso.add(cuerpoPiso(diseno, tamano, alto, conCorte));
    // Lo que le toca a este piso de fresas y fruta
    const fresasAqui = piezasDelPiso(decoraciones.fresas, desdeArriba, diseno.pisos);
    const frutaAqui = piezasDelPiso(decoraciones.frutaPicada, desdeArriba, diseno.pisos);
    const frutosRojosAqui = piezasDelPiso(decoraciones.frutosRojos, desdeArriba, diseno.pisos);
    if (esArriba && decoraciones.velas > 0) {
      // Las velas miden 8 cm: cuentan para encuadrar la cámara
      alturaVelas = 8.5;
    }

    // Listones y collares de perlas: uno por unidad, de abajo hacia arriba (si sobran, otra vuelta más arriba)
    const listones = Math.min(2, piezasDelPiso(decoraciones.liston, i, diseno.pisos));
    const collares = Math.min(3, piezasDelPiso(decoraciones.perlas, i, diseno.pisos));
    // Primer listón en la base y el segundo a media altura
    const alturasListon = [0.15, alto * 0.5 - ANCHO_LISTON / 2];
    // Orilla de arriba del listón más alto (el goteo no baja de ahí) y el centro donde se amarra el moño
    let arribaListones = 0;
    let centroListon = alto * 0.55;
    const colorTela = colorListon(diseno);
    for (let k = 0; k < listones; k++) {
      piso.add(liston(diseno, tamano, colorTela, alturasListon[k]));
      arribaListones = alturasListon[k] + ANCHO_LISTON;
      centroListon = alturasListon[k] + ANCHO_LISTON / 2;
    }

    let arribaPiso = alto;
    // Color de la cara de arriba (el mensaje se escribe con el color que contraste)
    let colorArriba = diseno.colorCobertura;
    if (diseno.cobertura === 'degradado') {
      colorArriba = COLOR_ARRIBA_DEGRADADO;
    }
    // Goteo: un piso por unidad, empezando por el de arriba. Ganache: siempre en el de arriba
    const conGanache = diseno.cobertura === 'ganache' && esArriba;
    if (desdeArriba < decoraciones.goteo || conGanache) {
      let colorGoteo = '#3b1f14';
      if (conGanache) {
        colorGoteo = diseno.colorCobertura;
      }
      piso.add(goteo(diseno, tamano, alto, colorGoteo, arribaListones));
      arribaPiso = alto + 0.3;
      colorArriba = colorGoteo;
    }

    // Collares: el primero en la base (o sentado sobre el listón), el segundo en la orilla de arriba
    // y el tercero a la mitad (si a la mitad hay listón, se sienta sobre él)
    let alturaPerlas = 0;
    if (listones > 0) {
      alturaPerlas = alturasListon[0] + ANCHO_LISTON + 0.05;
    }
    let alturaTercero = alto * 0.5 - 0.7;
    if (listones > 1) {
      alturaTercero = alturasListon[1] + ANCHO_LISTON + 0.05;
    }
    const alturasCollar = [alturaPerlas, alto - 1.3, alturaTercero];
    for (let k = 0; k < collares; k++) {
      const collar = perlas(diseno, tamano, '#f4efe6');
      collar.position.y = alturasCollar[k];
      piso.add(collar);
    }
    // La cobertura nunca agrega piezas (cordón, rosetas...): solo cambia cómo se ve el pastel.
    // Las rosetas salen solo si se piden como decoración.
    if (rosetasPorPiso[i] > 0) {
      let colorCrema = diseno.colorCobertura;
      if (diseno.cobertura === 'ganache' || diseno.cobertura === 'naked' || diseno.cobertura === 'ninguna' || diseno.cobertura === 'marmol') {
        colorCrema = '#ffffff';
      }
      piso.add(rosetas(diseno, tamano, arribaPiso, colorCrema, rosetasPorPiso[i], conCorte));
    }

    // Moños (todos en el piso de arriba) y flores (repartidas entre los pisos, desde arriba)
    let monosAqui = 0;
    if (esArriba) {
      monosAqui = decoraciones.mono;
    }
    const floresAqui = piezasDelPiso(decoraciones.flores, desdeArriba, diseno.pisos);
    const lugares = monosAqui + floresAqui;
    if (lugares > 0) {
      // Moños y flores comparten la vuelta del piso: así nunca quedan una encima de otra
      let inicio = -Math.PI / 2;
      if (monosAqui === 0 && desdeArriba % 2 === 1) {
        // En los pisos de abajo las flores se recorren medio lugar para que no queden en columna
        inicio = -Math.PI / 2 + Math.PI / lugares;
      }
      const angulos = angulosRepartidos(lugares, inicio, conCorte);
      const angulosMonos: number[] = [];
      const angulosFlores: number[] = [];
      const esMono: boolean[] = [];
      for (let k = 0; k < lugares; k++) {
        esMono.push(false);
      }
      for (let k = 0; k < monosAqui; k++) {
        // Los moños quedan repartidos parejo entre todos los lugares, el primero al frente
        esMono[Math.round((k * lugares) / monosAqui)] = true;
      }
      for (let k = 0; k < lugares; k++) {
        if (esMono[k]) {
          angulosMonos.push(angulos[k]);
        } else {
          angulosFlores.push(angulos[k]);
        }
      }
      if (angulosMonos.length > 0) {
        // Con listón, el moño se amarra sobre él
        piso.add(monos(diseno, tamano, centroListon, angulosMonos, lugares, colorTela));
      }
      if (angulosFlores.length > 0) {
        piso.add(floresLaterales(diseno, tamano, alto, angulosFlores, lugares));
      }
    }
    // Círculo del centro donde va el mensaje (ahí no caen chispas)
    let radioMensaje = 0;
    if (esArriba) {
      // Cómo se acomoda la cara de arriba para que el mensaje y las piezas no se tapen:
      // - una sola pieza en el centro (una vela, o una fresa) → el mensaje va en arco alrededor
      // - varias piezas → van en anillo por la orilla y el mensaje va recto en el centro
      const conMensaje = diseno.dedicatoria !== '';
      const velaAlCentro = decoraciones.velas === 1;
      const fresaAlCentro = fresasAqui === 1 && !velaAlCentro;
      const hayObjetoCentral = velaAlCentro || fresaAlCentro;
      // Sin mensaje, las piezas se acomodan como siempre (anillos desde el centro)
      const piezasEnOrilla = conMensaje;
      // Franja del arco del mensaje alrededor del objeto central
      const arcoInterior = Math.max(2.2, tamano * 0.14);
      const arcoExterior = tamano * 0.28;

      let radioFresas = tamano * 0.36;
      let radioVelas = tamano * 0.32;
      if (conMensaje) {
        radioFresas = tamano * 0.41;
        radioVelas = tamano * 0.38;
      }
      if (fresasAqui > 1 || (fresasAqui === 1 && velaAlCentro)) {
        // Con fresas alrededor, las velas se quedan adentro del anillo de fresas
        radioVelas = tamano * 0.22;
        if (conMensaje) {
          radioVelas = tamano * 0.31;
        }
      } else if (decoraciones.velas > 0) {
        radioFresas = tamano * 0.4;
      }
      // Radio del círculo libre del centro (lo que no ocupan los anillos)
      let radioLibre = tamano * 0.43;

      if (fresasAqui > 0) {
        piso.add(fresas(fresasAqui, arribaPiso, radioFresas, piezasEnOrilla && !fresaAlCentro));
        if (!fresaAlCentro) {
          radioLibre = Math.min(radioLibre, radioFresas - 1.6);
        }
      }
      if (decoraciones.velas > 0) {
        piso.add(velas(decoraciones.velas, radioVelas, arribaPiso, llamas, piezasEnOrilla && !velaAlCentro));
        if (!velaAlCentro) {
          // Las velas son altas: desde el frente tapan un poco lo que está detrás, se deja más margen
          radioLibre = Math.min(radioLibre, radioVelas - 1.8);
        }
      }
      if (frutaAqui > 0) {
        // Con mensaje, la fruta se queda alrededor y deja el centro libre
        let radioSinFruta = 0;
        if (conMensaje && hayObjetoCentral) {
          radioSinFruta = arcoExterior + 0.6;
        } else if (conMensaje) {
          radioSinFruta = radioLibre * 0.75;
        }
        piso.add(cubitos(frutaAqui, COLORES_FRUTA, tamano * 0.38, radioSinFruta, arribaPiso - 0.4, 3, null));
      }
      if (frutosRojosAqui > 0) {
        // Igual que la fruta: alrededor del mensaje si hay
        let radioSinFrutos = 0;
        if (conMensaje && hayObjetoCentral) {
          radioSinFrutos = arcoExterior + 0.6;
        } else if (conMensaje) {
          radioSinFrutos = radioLibre * 0.75;
        }
        piso.add(frutosRojos(frutosRojosAqui, tamano * 0.38, radioSinFrutos, arribaPiso - 0.1, 9, null));
      }
      if (diseno.topper !== '') {
        // RF-5 · Excepción paso 4: el topper va de pie atrás, para no tapar el mensaje
        const anchoLetrero = Math.min(18, tamano * 0.72);
        const letrero = topper(diseno.topper, anchoLetrero);
        letrero.position.set(0, arribaPiso, -tamano * 0.3);
        piso.add(letrero);
        alturaVelas = Math.max(alturaVelas, altoTopper(anchoLetrero, diseno.topper));
      }
      if (conMensaje && hayObjetoCentral) {
        piso.add(dedicatoriaEnArco(diseno, tamano, arribaPiso, arcoInterior, arcoExterior, colorArriba));
        radioMensaje = arcoExterior + 0.4;
      } else if (conMensaje) {
        piso.add(dedicatoria(diseno, arribaPiso, Math.max(3, radioLibre), colorArriba));
        radioMensaje = Math.max(3, radioLibre);
      }
    } else {
      // Pisos de abajo: fresas y fruta en la repisa (entre la orilla y el piso de encima)
      const repisa = repisaDelPiso(diseno.forma, tamano, tamanos[i + 1]);
      if (fresasAqui > 0) {
        piso.add(fresasEnRepisa(diseno, tamano, repisa, fresasAqui, rosetasPorPiso[i], arribaPiso, desdeArriba, conCorte));
      }
      if (frutaAqui > 0) {
        piso.add(cubitos(frutaAqui, COLORES_FRUTA, tamano / 2, tamanos[i + 1] / 2, arribaPiso - 0.4, 3 + i, repisa));
      }
      if (frutosRojosAqui > 0) {
        piso.add(frutosRojos(frutosRojosAqui, tamano / 2, tamanos[i + 1] / 2, arribaPiso - 0.1, 9 + i, repisa));
      }
    }
    // Hoja de oro: un piso por unidad, empezando por el de arriba
    if (desdeArriba < decoraciones.hojaOro) {
      piso.add(hojasDeOro(diseno, tamano, alto, arribaPiso, conCorte, radioMensaje));
    }
    // Chispas: un piso por unidad, empezando por el de arriba
    if (desdeArriba < decoraciones.chispas) {
      let siguiente = 0;
      if (!esArriba) {
        siguiente = tamanos[i + 1];
      }
      piso.add(chispas(diseno, tamano, siguiente, arribaPiso, radioMensaje));
    }
    if (conCorte) {
      recortarRebanada(piso);
    }
    conSombras(piso);
    raiz.add(piso);
    altura = altura + arribaPiso;
  }
  return { grupo: raiz, alturaTotal: altura + alturaVelas, anchoTotal: tamanos[0] + 8, llamas: llamas };
}

// Libera la memoria de la tarjeta de video al quitar un pastel
export function liberarPastel(objeto: THREE.Object3D): void {
  objeto.traverse(liberarObjeto);
}

function liberarObjeto(objeto: THREE.Object3D): void {
  const malla = objeto as THREE.Mesh;
  if (!malla.isMesh) {
    return;
  }
  malla.geometry.dispose();
  let materiales: THREE.Material[] = [];
  if (Array.isArray(malla.material)) {
    materiales = malla.material;
  } else {
    materiales = [malla.material];
  }
  for (const uno of materiales) {
    const estandar = uno as THREE.MeshStandardMaterial;
    if (estandar.map) {
      estandar.map.dispose();
    }
    if (estandar.bumpMap) {
      estandar.bumpMap.dispose();
    }
    uno.dispose();
  }
}
