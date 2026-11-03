import * as THREE from 'three';
import { DisenoPastel } from './diseno-pastel';
import { FormaPastel, Punto2d, estaDentro, puntosForma } from './formas-pastel';

/**
 * Construye el pastel 3D (three.js) a partir de un DisenoPastel.
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
const CORTE_INICIO = -Math.PI / 2 + 0.3;
const CORTE_FIN = -Math.PI / 2 + 1.7;

const COLORES_CHISPAS = ['#f06292', '#4fc3f7', '#ffd54f', '#81c784', '#ba68c8', '#ffffff', '#ff8a65'];
const COLORES_MOSAICO = ['#e2334f', '#9fd356', '#f59a2c', '#f4d03f', '#8e2f7a'];
const COLORES_FRUTA = ['#e2334f', '#9fd356', '#f59a2c', '#f4d03f', '#7a3a8c'];

// ===================== Utilidades =====================

// Números pseudoaleatorios repetibles (el pastel se ve igual cada vez que se arma)
class Aleatorio {
  private semilla: number;

  constructor(semilla: number) {
    this.semilla = semilla;
  }

  siguiente(): number {
    this.semilla = (this.semilla * 16807) % 2147483647;
    return this.semilla / 2147483647;
  }
}

function material(color: string, aspereza: number, metal: number): THREE.MeshStandardMaterial {
  return new THREE.MeshStandardMaterial({ color: color, roughness: aspereza, metalness: metal });
}

function conSombras(objeto: THREE.Object3D): void {
  objeto.traverse(marcarSombra);
}

function marcarSombra(objeto: THREE.Object3D): void {
  const malla = objeto as THREE.Mesh;
  if (malla.isMesh) {
    malla.castShadow = true;
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
function lugaresEnAnillos(cantidad: number, radioMaximo: number, separacion: number, enOrilla: boolean): LugarArriba[] {
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

// ===================== Texturas dibujadas en un lienzo =====================

function lienzo(ancho: number, alto: number): CanvasRenderingContext2D {
  const elemento = document.createElement('canvas');
  elemento.width = ancho;
  elemento.height = alto;
  return elemento.getContext('2d') as CanvasRenderingContext2D;
}

// Relieve del betún (manchas suaves)
function texturaRelieve(semilla: number): THREE.CanvasTexture {
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

// Lado "naked": bizcocho con betún untado a medias
function texturaNaked(bizcocho: string, cobertura: string): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  contexto.fillStyle = bizcocho;
  contexto.fillRect(0, 0, 256, 256);
  const azar = new Aleatorio(5);
  contexto.fillStyle = cobertura;
  for (let i = 0; i < 70; i++) {
    contexto.globalAlpha = 0.25 + azar.siguiente() * 0.4;
    contexto.fillRect(azar.siguiente() * 256, azar.siguiente() * 256, 20 + azar.siguiente() * 60, 3 + azar.siguiente() * 8);
  }
  contexto.globalAlpha = 1;
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  return textura;
}

// Corte por dentro: capas de bizcocho y relleno, con la cobertura por fuera y arriba
function texturaInterior(diseno: DisenoPastel): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  contexto.fillStyle = diseno.colorBizcocho;
  contexto.fillRect(0, 0, 256, 256);
  // Dos capas de relleno
  contexto.fillStyle = diseno.colorRelleno;
  contexto.fillRect(0, 88, 256, 16);
  contexto.fillRect(0, 160, 256, 16);
  // Migajas del bizcocho
  const azar = new Aleatorio(9);
  contexto.fillStyle = 'rgba(0,0,0,0.08)';
  for (let i = 0; i < 500; i++) {
    contexto.fillRect(azar.siguiente() * 256, azar.siguiente() * 256, 2, 2);
  }
  // Cobertura arriba y por fuera (lado derecho = orilla del pastel)
  if (diseno.cobertura !== 'naked') {
    contexto.fillStyle = diseno.colorCobertura;
    contexto.fillRect(0, 0, 256, 14);
    contexto.fillRect(242, 0, 14, 256);
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

function texturaDedicatoria(texto: string, color: string): THREE.CanvasTexture {
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

function materialCobertura(diseno: DisenoPastel): THREE.MeshStandardMaterial {
  if (diseno.cobertura === 'ganache') {
    return new THREE.MeshPhysicalMaterial({ color: diseno.colorCobertura, roughness: 0.22, clearcoat: 0.8, clearcoatRoughness: 0.2 });
  }
  if (diseno.cobertura === 'lisa') {
    // Fondant: liso y satinado
    return new THREE.MeshPhysicalMaterial({ color: diseno.colorCobertura, roughness: 0.35, sheen: 0.6, sheenRoughness: 0.4 });
  }
  const relieve = texturaRelieve(3);
  let aspereza = 0.7;
  let fuerza = 0.6;
  if (diseno.cobertura === 'chantilly') {
    aspereza = 0.85;
    fuerza = 0.9;
  }
  return new THREE.MeshStandardMaterial({ color: diseno.colorCobertura, roughness: aspereza, bumpMap: relieve, bumpScale: fuerza });
}

function cuerpoPiso(diseno: DisenoPastel, tamano: number, alto: number, conCorte: boolean): THREE.Group {
  const grupo = new THREE.Group();
  let biselado = Math.min(1, alto * 0.1);
  let puntos = puntosForma(diseno.forma, tamano - 2 * biselado, 180);
  if (conCorte) {
    // Sin orilla redondeada: así las paredes del corte quedan justo donde se ven las capas
    biselado = 0;
    puntos = puntosConCorte(diseno.forma, tamano);
  }
  const geometria = new THREE.ExtrudeGeometry(formaThree(puntos), {
    depth: alto - 2 * biselado,
    bevelEnabled: biselado > 0,
    bevelThickness: biselado,
    bevelSize: biselado,
    bevelSegments: 4,
    curveSegments: 12,
  });
  geometria.translate(0, 0, biselado);
  geometria.rotateX(-Math.PI / 2);

  const arriba = materialCobertura(diseno);
  let lados = arriba;
  if (diseno.cobertura === 'naked') {
    const textura = texturaNaked(diseno.colorBizcocho, diseno.colorCobertura);
    textura.repeat.set(3, 1);
    lados = new THREE.MeshStandardMaterial({ map: textura, roughness: 0.85 });
  }
  const cuerpo = new THREE.Mesh(geometria, [arriba, lados]);
  grupo.add(cuerpo);

  if (conCorte) {
    grupo.add(paredCorte(diseno, tamano, alto, CORTE_INICIO, 1));
    grupo.add(paredCorte(diseno, tamano, alto, CORTE_FIN, -1));
  }
  return grupo;
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
  return malla;
}

// ===================== Decoraciones =====================

function perlas(diseno: DisenoPastel, tamano: number, color: string): THREE.InstancedMesh {
  const contorno = contornoEspaciado(diseno.forma, tamano, 1.5);
  const geometria = new THREE.SphereGeometry(0.7, 16, 12);
  const malla = new THREE.InstancedMesh(geometria, material(color, 0.22, 0.25), contorno.length);
  const auxiliar = new THREE.Object3D();
  for (let i = 0; i < contorno.length; i++) {
    const posicion = posicionMundo(contorno[i], 0.35);
    auxiliar.position.set(posicion.x, 0.7, posicion.y);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

function liston(diseno: DisenoPastel, tamano: number, color: string, altura: number): THREE.Mesh {
  const geometria = new THREE.ExtrudeGeometry(formaThree(puntosForma(diseno.forma, tamano + 0.5, 180)), {
    depth: 2.2,
    bevelEnabled: false,
    curveSegments: 12,
  });
  geometria.rotateX(-Math.PI / 2);
  const malla = new THREE.Mesh(geometria, material(color, 0.35, 0.05));
  malla.position.y = altura;
  return malla;
}

// Un moño pegado al costado del piso, en el ángulo pedido (-π/2 es el frente)
function mono(diseno: DisenoPastel, tamano: number, altura: number, color: string, angulo: number, escala: number): THREE.Group {
  const grupo = new THREE.Group();
  const tela = material(color, 0.4, 0.05);
  const lazo = new THREE.TorusGeometry(1.6, 0.45, 12, 28);
  const izquierdo = new THREE.Mesh(lazo, tela);
  izquierdo.scale.set(1.2, 0.75, 0.5);
  izquierdo.position.set(-1.7, 0, 0);
  const derecho = new THREE.Mesh(lazo, tela);
  derecho.scale.set(1.2, 0.75, 0.5);
  derecho.position.set(1.7, 0, 0);
  const nudo = new THREE.Mesh(new THREE.SphereGeometry(0.85, 16, 12), tela);
  nudo.scale.set(1, 1, 0.7);
  const colaGeometria = new THREE.BoxGeometry(0.9, 3, 0.25);
  const cola1 = new THREE.Mesh(colaGeometria, tela);
  cola1.position.set(-0.8, -1.8, 0);
  cola1.rotation.z = 0.35;
  const cola2 = new THREE.Mesh(colaGeometria, tela);
  cola2.position.set(0.8, -1.8, 0);
  cola2.rotation.z = -0.35;
  grupo.add(izquierdo, derecho, nudo, cola1, cola2);
  const punto = puntoContorno(diseno.forma, tamano, angulo);
  const posicion = posicionMundo(punto, 0.9 * escala);
  grupo.position.set(posicion.x, altura, posicion.y);
  grupo.rotation.y = giroMundo(punto);
  grupo.scale.setScalar(escala);
  return grupo;
}

// Un moño en cada ángulo indicado
function monos(diseno: DisenoPastel, tamano: number, altura: number, angulos: number[], lugaresTotales: number): THREE.Group {
  const grupo = new THREE.Group();
  const perimetro = Math.PI * tamano;
  // Si hay muchas piezas alrededor, los moños se hacen más chicos para que no se encimen
  const escala = Math.max(0.5, Math.min(1, perimetro / lugaresTotales / 7.5));
  for (const angulo of angulos) {
    grupo.add(mono(diseno, tamano, altura, '#e889a8', angulo, escala));
  }
  return grupo;
}

// Cordón de betún alrededor de la base del piso (conchitas pegadas)
function cordonBase(diseno: DisenoPastel, tamano: number, color: string): THREE.InstancedMesh {
  const contorno = contornoEspaciado(diseno.forma, tamano, 1.1);
  const geometria = new THREE.SphereGeometry(0.75, 12, 10);
  const relieve = texturaRelieve(8);
  const malla = new THREE.InstancedMesh(geometria, new THREE.MeshStandardMaterial({ color: color, roughness: 0.75, bumpMap: relieve, bumpScale: 0.8 }), contorno.length);
  const auxiliar = new THREE.Object3D();
  for (let i = 0; i < contorno.length; i++) {
    const posicion = posicionMundo(contorno[i], 0.1);
    auxiliar.position.set(posicion.x, 0.55, posicion.y);
    auxiliar.rotation.set(0, giroMundo(contorno[i]), 0.5);
    auxiliar.scale.set(1.25, 0.8, 0.9);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

function geometriaRoseta(): THREE.LatheGeometry {
  const perfil: THREE.Vector2[] = [];
  for (let i = 0; i <= 24; i++) {
    const t = i / 24;
    const radio = 1.3 * Math.pow(1 - t, 0.7) * (1 + 0.16 * Math.sin(t * Math.PI * 7));
    perfil.push(new THREE.Vector2(Math.max(0.001, radio), t * 2));
  }
  return new THREE.LatheGeometry(perfil, 18);
}

/**
 * Rosetas de crema sobre la orilla del piso. Con "cantidad" 0 se llena toda la orilla
 * (cobertura chantilly); si no, se ponen solo esas piezas, repartidas desde el frente.
 */
function rosetas(diseno: DisenoPastel, tamano: number, altura: number, color: string, cantidad: number, evitarCorte: boolean): THREE.InstancedMesh {
  const contorno = contornoEspaciado(diseno.forma, tamano, 3.1);
  let frente = 0;
  for (let i = 0; i < contorno.length; i++) {
    if (contorno[i].y < contorno[frente].y) {
      frente = i;
    }
  }
  // Índices del contorno donde va una roseta
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
  const malla = new THREE.InstancedMesh(geometriaRoseta(), material(color, 0.75, 0), elegidos.length);
  const auxiliar = new THREE.Object3D();
  let puestas = 0;
  for (const indice of elegidos) {
    const punto = contorno[indice];
    if (evitarCorte && dentroDelCorte(Math.atan2(punto.y, punto.x))) {
      continue;
    }
    const posicion = posicionMundo(punto, -1.6);
    auxiliar.position.set(posicion.x, altura - 0.1, posicion.y);
    auxiliar.rotation.set(0, indice * 1.7, 0);
    auxiliar.updateMatrix();
    malla.setMatrixAt(puestas, auxiliar.matrix);
    puestas = puestas + 1;
  }
  malla.count = puestas;
  return malla;
}

function goteo(diseno: DisenoPastel, tamano: number, altura: number, color: string): THREE.Group {
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

function chispas(diseno: DisenoPastel, tamano: number, tamanoSiguiente: number, altura: number): THREE.InstancedMesh {
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

function flor(color: string): THREE.Group {
  const grupo = new THREE.Group();
  const esfera = new THREE.SphereGeometry(1, 14, 10);
  const petalo = material(color, 0.55, 0);
  const petaloClaro = material('#' + new THREE.Color(color).lerp(new THREE.Color('#ffffff'), 0.35).getHexString(), 0.55, 0);
  for (let i = 0; i < 5; i++) {
    const angulo = (i / 5) * Math.PI * 2;
    const p = new THREE.Mesh(esfera, petalo);
    p.scale.set(0.75, 0.48, 0.22);
    p.position.set(Math.cos(angulo) * 0.72, Math.sin(angulo) * 0.72, 0);
    p.rotation.z = angulo;
    grupo.add(p);
    const q = new THREE.Mesh(esfera, petaloClaro);
    q.scale.set(0.45, 0.3, 0.2);
    q.position.set(Math.cos(angulo + 0.6) * 0.42, Math.sin(angulo + 0.6) * 0.42, 0.18);
    q.rotation.z = angulo + 0.6;
    grupo.add(q);
  }
  const centro = new THREE.Mesh(esfera, material('#f6c945', 0.7, 0));
  centro.scale.set(0.32, 0.32, 0.25);
  centro.position.z = 0.25;
  grupo.add(centro);
  return grupo;
}

/**
 * Una flor pegada al costado del piso en cada ángulo indicado, a dos alturas.
 * Una sola flor va a media altura.
 */
function floresLaterales(diseno: DisenoPastel, tamano: number, alto: number, angulos: number[], lugaresTotales: number): THREE.Group {
  const grupo = new THREE.Group();
  const cantidad = angulos.length;
  const perimetro = Math.PI * tamano;
  let escala = 2;
  if (lugaresTotales > 1) {
    // Más piezas alrededor = flores más chicas, para que quepan todas
    escala = Math.max(0.9, Math.min(1.8, perimetro / lugaresTotales / 2.4));
  }
  for (let k = 0; k < angulos.length; k++) {
    const punto = puntoContorno(diseno.forma, tamano, angulos[k]);
    let alturaRelativa = 0.5;
    if (cantidad > 1 && k % 2 === 0) {
      alturaRelativa = 0.62;
    } else if (cantidad > 1) {
      alturaRelativa = 0.36;
    }
    const nueva = flor('#e86a92');
    nueva.scale.setScalar(escala);
    const posicion = posicionMundo(punto, 0.35);
    nueva.position.set(posicion.x, alto * alturaRelativa, posicion.y);
    nueva.rotation.y = giroMundo(punto);
    nueva.rotateX(-0.12);
    grupo.add(nueva);
  }
  return grupo;
}

// Una fresa por cada unidad elegida, en anillos sobre el pastel
function fresas(cantidad: number, altura: number, radio: number, enOrilla: boolean): THREE.Group {
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
  const cuerpo = new THREE.LatheGeometry(puntos, 20);
  const hojas = new THREE.ConeGeometry(1.2, 0.35, 7, 1, true);
  const rojo = new THREE.MeshPhysicalMaterial({ color: '#d7263d', roughness: 0.3, clearcoat: 0.6 });
  const verde = new THREE.MeshStandardMaterial({ color: '#4f8a3a', roughness: 0.6, side: THREE.DoubleSide });
  for (const lugar of lugaresEnAnillos(cantidad, radio, 2.9, enOrilla)) {
    const fresa = new THREE.Group();
    const parteRoja = new THREE.Mesh(cuerpo, rojo);
    const parteVerde = new THREE.Mesh(hojas, verde);
    parteVerde.position.y = 0.12;
    parteVerde.rotation.x = Math.PI;
    fresa.add(parteRoja, parteVerde);
    fresa.position.set(lugar.x, altura + 0.05, lugar.z);
    fresa.rotation.set(0.2, lugar.angulo, 0);
    fresa.scale.setScalar(1.2);
    grupo.add(fresa);
  }
  return grupo;
}

// Cubitos de fruta (o de gelatina) sobre la superficie, entre "radioInterior" y "radio"
function cubitos(cantidad: number, colores: string[], radio: number, radioInterior: number, altura: number, semilla: number): THREE.InstancedMesh {
  const malla = new THREE.InstancedMesh(new THREE.BoxGeometry(1.1, 1.1, 1.1), new THREE.MeshPhysicalMaterial({ roughness: 0.25, clearcoat: 0.6 }), cantidad);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    // Con radioInterior el centro queda libre (para el mensaje)
    const parteLibre = (radioInterior * radioInterior) / (radio * radio);
    const distancia = Math.sqrt(parteLibre + azar.siguiente() * (1 - parteLibre)) * radio;
    auxiliar.position.set(Math.cos(angulo) * distancia, altura + 0.5, Math.sin(angulo) * distancia);
    auxiliar.rotation.set(azar.siguiente(), azar.siguiente() * 3, azar.siguiente());
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
    malla.setColorAt(i, color.set(colores[i % colores.length]));
  }
  return malla;
}

// Una vela por cada unidad elegida: una sola va al centro; varias, en anillos
function velas(cantidad: number, radio: number, altura: number, llamas: THREE.Mesh[], enOrilla: boolean): THREE.Group {
  const grupo = new THREE.Group();
  const cera = new THREE.CylinderGeometry(0.38, 0.38, 7, 18);
  const pabilo = new THREE.CylinderGeometry(0.06, 0.06, 0.8, 6);
  const fuego = new THREE.SphereGeometry(0.32, 14, 10);
  const colores = ['#7fb8e6', '#f48fb1', '#ffd54f', '#a5d6a7'];
  const fuegoExterior = new THREE.MeshBasicMaterial({ color: '#ffae42' });
  const fuegoInterior = new THREE.MeshBasicMaterial({ color: '#fff4c2' });
  const lugares = lugaresEnAnillos(cantidad, radio, 2.3, enOrilla);
  for (let i = 0; i < lugares.length; i++) {
    const vela = new THREE.Group();
    const cuerpo = new THREE.Mesh(cera, material(colores[i % colores.length], 0.45, 0));
    cuerpo.position.y = 3.5;
    const mecha = new THREE.Mesh(pabilo, material('#2b2320', 0.8, 0));
    mecha.position.y = 7.35;
    const llama = new THREE.Mesh(fuego, fuegoExterior);
    llama.position.y = 8.1;
    llama.scale.set(1, 2.1, 1);
    const centro = new THREE.Mesh(fuego, fuegoInterior);
    centro.position.y = 7.95;
    centro.scale.set(0.5, 1.1, 0.5);
    vela.add(cuerpo, mecha, llama, centro);
    vela.position.set(lugares[i].x, altura, lugares[i].z);
    grupo.add(vela);
    llamas.push(llama);
  }
  const luz = new THREE.PointLight('#ffb35c', 30, 45, 2);
  luz.position.set(0, altura + 9, 0);
  grupo.add(luz);
  return grupo;
}

function colorDedicatoria(diseno: DisenoPastel): string {
  if (diseno.colorCobertura === '#4a2617' || diseno.colorCobertura === '#7a4529' || diseno.colorCobertura === '#6b3b26') {
    return '#fdf3e3';
  }
  return '#5a3221';
}

/**
 * Mensaje recto en el centro de la cara de arriba. "radioLibre" es cuánto espacio queda
 * libre en el centro (las piezas que rodean el pastel quedan fuera de ese círculo).
 */
function dedicatoria(diseno: DisenoPastel, altura: number, radioLibre: number): THREE.Mesh {
  // El renglón más ancho cabe en el círculo libre
  const ancho = radioLibre * 1.9;
  const plano = new THREE.Mesh(
    new THREE.PlaneGeometry(ancho, ancho / 2),
    new THREE.MeshStandardMaterial({ map: texturaDedicatoria(diseno.dedicatoria, colorDedicatoria(diseno)), transparent: true, roughness: 0.4 }),
  );
  plano.rotation.x = -Math.PI / 2;
  plano.position.set(0, altura + 0.08, 0);
  return plano;
}

/**
 * Mensaje en arco alrededor del objeto que está en el centro, dentro de la franja
 * entre "radioInterior" y "radioExterior" (medidas del pastel).
 */
function dedicatoriaEnArco(diseno: DisenoPastel, tamano: number, altura: number, radioInterior: number, radioExterior: number): THREE.Mesh {
  const lado = tamano * 0.86;
  const pixeles = LADO_ARCO / lado;
  const textura = texturaDedicatoriaArco(diseno.dedicatoria, colorDedicatoria(diseno), radioInterior * pixeles, radioExterior * pixeles);
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
  const grupo = new THREE.Group();
  if (diseno.tipo === 'gelatina') {
    const radio = tamano / 2 + 4;
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
    const plato = new THREE.Mesh(new THREE.LatheGeometry(perfil, 64), new THREE.MeshStandardMaterial({ color: '#f7f5f0', roughness: 0.3, side: THREE.DoubleSide }));
    grupo.add(plato);
    conSombras(grupo);
    return { grupo: grupo, arriba: 1.25 };
  }
  const geometria = new THREE.ExtrudeGeometry(formaThree(puntosForma(diseno.forma, tamano + 7, 180)), {
    depth: 1,
    bevelEnabled: true,
    bevelThickness: 0.2,
    bevelSize: 0.3,
    bevelSegments: 2,
    curveSegments: 12,
  });
  geometria.rotateX(-Math.PI / 2);
  const dorado = new THREE.Mesh(geometria, material('#c9952f', 0.3, 0.9));
  dorado.position.y = 0.2;
  grupo.add(dorado);
  conSombras(grupo);
  return { grupo: grupo, arriba: 1.4 };
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

// ===================== Armado principal =====================

/**
 * Arma todo el postre. "verDentro" quita una rebanada del piso de arriba para ver
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
      for (const lugar of lugaresEnAnillos(decoraciones.rosetas, radioArriba, 2.7, false)) {
        const roseta = new THREE.Mesh(geometria, crema);
        roseta.position.set(lugar.x, arriba - 0.5, lugar.z);
        anillo.add(roseta);
      }
      raiz.add(anillo);
    }
    if (decoraciones.frutaPicada > 0) {
      raiz.add(cubitos(decoraciones.frutaPicada, COLORES_FRUTA, radioArriba * 0.8, 0, arriba - 0.6, 3));
    }
    if (decoraciones.fresas > 0) {
      raiz.add(fresas(decoraciones.fresas, arriba - 0.4, radioArriba * 0.7, false));
    }
    let alturaVelas = 0;
    if (decoraciones.velas > 0) {
      raiz.add(velas(decoraciones.velas, radioArriba * 0.6, arriba - 0.3, llamas, false));
      alturaVelas = 8.5;
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

  // Rosetas sueltas: se llena primero la orilla del piso de arriba y lo que sobra baja
  const rosetasPorPiso: number[] = [];
  for (let i = 0; i < diseno.pisos; i++) {
    rosetasPorPiso.push(0);
  }
  let rosetasPendientes = decoraciones.rosetas;
  for (let i = diseno.pisos - 1; i >= 0; i--) {
    let aqui = Math.min(contornoEspaciado(diseno.forma, tamanos[i], 3.1).length, rosetasPendientes);
    if (i === 0) {
      // El piso de abajo se queda con las que sobren
      aqui = rosetasPendientes;
    }
    rosetasPorPiso[i] = aqui;
    rosetasPendientes = rosetasPendientes - aqui;
  }

  let altura = plato.arriba;
  let alturaVelas = 0;
  for (let i = 0; i < diseno.pisos; i++) {
    const tamano = tamanos[i];
    const esArriba = i === diseno.pisos - 1;
    // Posición del piso contando desde arriba (0 = el de hasta arriba)
    const desdeArriba = diseno.pisos - 1 - i;
    const conCorte = verDentro && esArriba;
    const piso = new THREE.Group();
    piso.position.y = altura;
    piso.add(cuerpoPiso(diseno, tamano, alto, conCorte));
    if (esArriba && decoraciones.velas > 0) {
      // Las velas miden 8 cm: cuentan para encuadrar la cámara
      alturaVelas = 8.5;
    }

    let arribaPiso = alto;
    // Goteo: un piso por unidad, empezando por el de arriba. Ganache: siempre en el de arriba
    const conGanache = diseno.cobertura === 'ganache' && esArriba;
    if (desdeArriba < decoraciones.goteo || conGanache) {
      let colorGoteo = '#3b1f14';
      if (conGanache) {
        colorGoteo = diseno.colorCobertura;
      }
      piso.add(goteo(diseno, tamano, alto, colorGoteo));
      arribaPiso = alto + 0.3;
    }

    // Listones y collares de perlas: uno por unidad, de abajo hacia arriba (si sobran, otra vuelta más arriba)
    const listones = Math.min(2, piezasDelPiso(decoraciones.liston, i, diseno.pisos));
    const collares = Math.min(3, piezasDelPiso(decoraciones.perlas, i, diseno.pisos));
    for (let k = 0; k < listones; k++) {
      let alturaListon = 0.15;
      if (k === 1) {
        alturaListon = alto * 0.5 - 1.1;
      }
      piso.add(liston(diseno, tamano, '#c9476b', alturaListon));
    }
    let alturaPerlas = 0;
    if (listones > 0) {
      alturaPerlas = 2.4;
    }
    for (let k = 0; k < collares; k++) {
      const collar = perlas(diseno, tamano, '#f4efe6');
      collar.position.y = alturaPerlas + k * 1.5;
      piso.add(collar);
    }
    // Betún: cordón de betún en la base de cada piso (si no hay perlas ni listón en ese lugar)
    if (diseno.cobertura === 'betun' && listones === 0 && collares === 0) {
      piso.add(cordonBase(diseno, tamano, diseno.colorCobertura));
    }

    // Rosetas: chantilly llena la orilla; si no, solo las que se pidieron
    if (diseno.cobertura === 'chantilly') {
      piso.add(rosetas(diseno, tamano, arribaPiso, diseno.colorCobertura, 0, conCorte));
    } else if (rosetasPorPiso[i] > 0) {
      let colorCrema = diseno.colorCobertura;
      if (diseno.cobertura === 'ganache' || diseno.cobertura === 'naked') {
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
        piso.add(monos(diseno, tamano, alto * 0.55, angulosMonos, lugares));
      }
      if (angulosFlores.length > 0) {
        piso.add(floresLaterales(diseno, tamano, alto, angulosFlores, lugares));
      }
    }
    // Chispas: un piso por unidad, empezando por el de arriba
    if (desdeArriba < decoraciones.chispas) {
      let siguiente = 0;
      if (!esArriba) {
        siguiente = tamanos[i + 1];
      }
      piso.add(chispas(diseno, tamano, siguiente, arribaPiso));
    }

    if (esArriba) {
      // Cómo se acomoda la cara de arriba para que el mensaje y las piezas no se tapen:
      // - una sola pieza en el centro (una vela, o una fresa) → el mensaje va en arco alrededor
      // - varias piezas → van en anillo por la orilla y el mensaje va recto en el centro
      const conMensaje = diseno.dedicatoria !== '';
      const velaAlCentro = decoraciones.velas === 1;
      const fresaAlCentro = decoraciones.fresas === 1 && !velaAlCentro;
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
      if (decoraciones.fresas > 1 || (decoraciones.fresas === 1 && velaAlCentro)) {
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

      if (decoraciones.fresas > 0) {
        piso.add(fresas(decoraciones.fresas, arribaPiso, radioFresas, piezasEnOrilla && !fresaAlCentro));
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
      if (decoraciones.frutaPicada > 0) {
        // Con mensaje, la fruta se queda alrededor y deja el centro libre
        let radioSinFruta = 0;
        if (conMensaje && hayObjetoCentral) {
          radioSinFruta = arcoExterior + 0.6;
        } else if (conMensaje) {
          radioSinFruta = radioLibre * 0.75;
        }
        piso.add(cubitos(decoraciones.frutaPicada, COLORES_FRUTA, tamano * 0.38, radioSinFruta, arribaPiso - 0.4, 3));
      }
      if (conMensaje && hayObjetoCentral) {
        piso.add(dedicatoriaEnArco(diseno, tamano, arribaPiso, arcoInterior, arcoExterior));
      } else if (conMensaje) {
        piso.add(dedicatoria(diseno, arribaPiso, Math.max(3, radioLibre)));
      }
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
