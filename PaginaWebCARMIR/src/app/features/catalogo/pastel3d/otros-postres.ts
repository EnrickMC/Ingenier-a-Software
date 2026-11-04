import * as THREE from 'three';
import {
  Aleatorio,
  COLORES_CHISPAS,
  COLORES_FRUTA,
  COLOR_ARRIBA_DEGRADADO,
  CORTE_FIN,
  CORTE_INICIO,
  PastelConstruido,
  altoTopper,
  ALTO_PALITO_TOPPER,
  basePostre,
  centrarArriba,
  colorListon,
  colorDedicatoria,
  conSombras,
  cubitos,
  dedicatoria,
  dedicatoriaEnArco,
  dibujarMigajas,
  fresas,
  frutosRojos,
  geometriaHojuela,
  geometriaRosa,
  geometriaRoseta,
  lienzo,
  material,
  materialOro,
  mezclarColores,
  materialRosa,
  mono,
  pintarDegradado,
  recortarRebanada,
  telaSatinada,
  texturaDedicatoria,
  texturaEspatula,
  texturaGiro,
  texturaMarmol,
  texturaRelieve,
  topper,
  velas,
} from './constructor-pastel';
import { DisenoPastel, TipoCobertura } from './diseno-pastel';
import { FormaPastel } from './formas-pastel';

/**
 * RF-5 · Paso 4: modelos 3D de los postres que no son pastel ni gelatina.
 * RF-5 · Excepción paso 4: las decoraciones que no van en un postre (por ejemplo, el goteo) no se dibujan.
 *  - Pay (pay de queso, de fresa, tarta...): costra con orilla ondulada y relleno; se puede ver por dentro.
 *  - Cheesecake: base de galleta, cuerpo de queso y capa de fruta o cobertura; se puede ver por dentro.
 *  - Cupcake: siempre uno solo (el tamaño lo hace más grande), con remolino de betún hecho con duya.
 *  - Rosca: rosca horneada con estrías (tipo bundt) y glaseado escurrido.
 *  - Galleta, brownie, dona y macaron: se ve una sola pieza (como el cupcake); el tamaño la hace más grande.
 * Todos se apoyan en la base elegida (plato, tabla, pizarra, base dorada o plateada, pedestal de cristal).
 * La cobertura cambia la superficie de cada postre y nunca agrega piezas, salvo la de chispas.
 * Las decoraciones van sobre la pieza (en la dona, sobre el aro: nunca en el hoyo).
 * Medidas en centímetros. Igual que el pastel, el frente es +z del mundo.
 */

// Superficie de arriba de una pieza: dónde se pueden poner decoraciones
interface Tope {
  // Altura de la cara de arriba en el centro
  altura: number;
  // Radio útil de la cara de arriba
  radio: number;
  // Cuánto baja la superficie por cada centímetro que se aleja del centro (cupcake con remolino)
  caida: number;
  // Con remolino de duya las velas se entierran según la altura real del remolino
  remolino?: boolean;
  // Cuánto se recorren las velas hacia el frente (negativo = hacia atrás, para dejar el topper al frente)
  velasAdelante?: number;
  // Radio del hoyo del centro (dona): ahí no se pone nada, todo va sobre el aro
  hueco?: number;
}

// Hoyo del centro de la pieza (0 si no tiene)
function huecoDe(tope: Tope): number {
  if (tope.hueco === undefined) {
    return 0;
  }
  return tope.hueco;
}

// Distancia al centro al azar sobre la cara de arriba, sin caer en el hoyo (si lo hay)
function distanciaAlAzar(tope: Tope, azar: Aleatorio, desde: number): number {
  const hueco = Math.max(huecoDe(tope), desde * tope.radio);
  return hueco + Math.sqrt(azar.siguiente()) * (tope.radio - hueco);
}

// Cuántas decoraciones le tocan a una pieza
interface DecoracionPieza {
  velas: number;
  fresas: number;
  flores: number;
  rosetas: number;
  perlas: number;
  chispas: number;
  fruta: number;
  frutosRojos: number;
  hojaOro: number;
}


// ===================== Utilidades =====================

// Color horneado: el color de la masa un poco más tostado
function horneado(color: string, cuanto: number): string {
  return '#' + new THREE.Color(color).lerp(new THREE.Color('#8a4f1c'), cuanto).getHexString();
}

function luminosidad(color: string): number {
  const c = new THREE.Color(color);
  return c.r * 0.3 + c.g * 0.59 + c.b * 0.11;
}

// Textura de masa horneada (migajas) que se repite
function texturaMasa(color: string, semilla: number): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  contexto.fillStyle = color;
  contexto.fillRect(0, 0, 256, 256);
  dibujarMigajas(contexto, 256, 256, semilla);
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.wrapS = THREE.RepeatWrapping;
  textura.wrapT = THREE.RepeatWrapping;
  return textura;
}

/**
 * Material de la cobertura sobre una pieza chica (cupcake, galleta, brownie, dona, pay).
 * "tamano" es el ancho de la superficie, para centrar las texturas.
 * "medidasReales" es true cuando la figura mide su textura en centímetros (el brownie);
 * las figuras redondas la miden de 0 a 1.
 */
function materialCoberturaPieza(diseno: DisenoPastel, tamano: number, medidasReales = false): THREE.Material {
  const color = diseno.colorCobertura;
  const cobertura: TipoCobertura = diseno.cobertura;
  if (cobertura === 'degradado') {
    // El color lo ponen los vértices (ver "cubrir")
    const giroDegradado = texturaGiro(4);
    centrarArriba(giroDegradado, tamano);
    return new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.6, bumpMap: giroDegradado, bumpScale: 1.5 });
  }
  if (cobertura === 'ganache') {
    return new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.14, clearcoat: 1, clearcoatRoughness: 0.08 });
  }
  if (cobertura === 'lisa') {
    // Fondant o glaseado: liso y brillante
    return new THREE.MeshPhysicalMaterial({ color: color, roughness: 0.3, clearcoat: 0.6, clearcoatRoughness: 0.25 });
  }
  if (cobertura === 'marmol') {
    const vetas = texturaMarmol(color, 41);
    if (medidasReales) {
      vetas.repeat.set(1 / (tamano * 0.7), 1 / (tamano * 0.7));
    } else {
      // Figuras redondas: la textura da una vuelta y media para que se vean varias vetas
      vetas.repeat.set(1.5, 1);
    }
    return new THREE.MeshPhysicalMaterial({ map: vetas, roughness: 0.3, clearcoat: 0.4 });
  }
  if (cobertura === 'chantilly') {
    const esponja = texturaRelieve(6);
    esponja.repeat.set(1 / 3, 1 / 3);
    return new THREE.MeshStandardMaterial({ color: color, roughness: 0.92, bumpMap: esponja, bumpScale: 2 });
  }
  // Betún (también naked y la base de la cobertura de chispas)
  const giro = texturaGiro(4);
  centrarArriba(giro, tamano);
  return new THREE.MeshStandardMaterial({ color: color, roughness: 0.6, bumpMap: giro, bumpScale: 1.5 });
}

// ¿La cobertura se ve como crema untada o con duya (betún, chantilly, chispas, naked, degradado)?
function esCremosa(cobertura: TipoCobertura): boolean {
  return cobertura === 'betun' || cobertura === 'chantilly' || cobertura === 'chispas' || cobertura === 'naked' || cobertura === 'degradado';
}

// Degradado del centro (claro) a la orilla (color): para coberturas planas como la de la galleta
function pintarDegradadoRadial(geometria: THREE.BufferGeometry, colorOrilla: string, colorCentro: string): void {
  geometria.computeBoundingBox();
  const caja = geometria.boundingBox as THREE.Box3;
  const radio = Math.max(0.001, Math.max(caja.max.x - caja.min.x, caja.max.z - caja.min.z) / 2);
  const posiciones = geometria.attributes['position'];
  const colores = new Float32Array(posiciones.count * 3);
  const color = new THREE.Color();
  for (let i = 0; i < posiciones.count; i++) {
    const distancia = Math.hypot(posiciones.getX(i), posiciones.getZ(i)) / radio;
    mezclarColores(color, colorCentro, colorOrilla, THREE.MathUtils.smoothstep(distancia, 0.1, 0.9));
    colores[i * 3] = color.r;
    colores[i * 3 + 1] = color.g;
    colores[i * 3 + 2] = color.b;
  }
  geometria.setAttribute('color', new THREE.BufferAttribute(colores, 3));
}

/**
 * Malla con la cobertura del diseño. Si la cobertura es «Degradado» pinta la figura:
 * de abajo hacia arriba, o del centro a la orilla si es plana ("plana" = true).
 */
function cubrir(diseno: DisenoPastel, geometria: THREE.BufferGeometry, tamano: number, medidasReales: boolean, plana: boolean): THREE.Mesh {
  if (diseno.cobertura === 'degradado') {
    if (plana) {
      pintarDegradadoRadial(geometria, diseno.colorCobertura, COLOR_ARRIBA_DEGRADADO);
    } else {
      pintarDegradado(geometria, diseno.colorCobertura, COLOR_ARRIBA_DEGRADADO);
    }
  }
  return new THREE.Mesh(geometria, materialCoberturaPieza(diseno, tamano, medidasReales));
}

// Hojuelas de oro sobre la cara de arriba de una pieza
function hojuelasSobre(tope: Tope, cantidad: number, semilla: number): THREE.InstancedMesh {
  const malla = new THREE.InstancedMesh(geometriaHojuela(), materialOro(), cantidad);
  const auxiliar = new THREE.Object3D();
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    const distancia = distanciaAlAzar(tope, azar, 0);
    auxiliar.position.set(Math.cos(angulo) * distancia, tope.altura - distancia * tope.caida + 0.1, Math.sin(angulo) * distancia);
    // Acostadas siguiendo la caída de la superficie
    auxiliar.rotation.set(-Math.PI / 2, 0, azar.siguiente() * Math.PI * 2);
    auxiliar.scale.setScalar(0.5 + azar.siguiente() * 0.6);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

/**
 * Chispas de colores sobre una superficie redonda (cara de arriba de una pieza).
 * "caida" hace que bajen con la forma del remolino del cupcake.
 */
function chispasSobre(tope: Tope, cantidad: number, semilla: number): THREE.InstancedMesh {
  const malla = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.09, 0.34, 2, 6), material('#ffffff', 0.45, 0), cantidad);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    const distancia = distanciaAlAzar(tope, azar, 0);
    auxiliar.position.set(Math.cos(angulo) * distancia, tope.altura - distancia * tope.caida + 0.08, Math.sin(angulo) * distancia);
    auxiliar.rotation.set(0, azar.siguiente() * Math.PI * 2, Math.PI / 2 - Math.atan(tope.caida) * 0.8, 'YXZ');
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
    malla.setColorAt(i, color.set(COLORES_CHISPAS[i % COLORES_CHISPAS.length]));
  }
  return malla;
}

// Perlitas (grageas plateadas) sobre la cara de arriba de una pieza
function perlitasSobre(tope: Tope, cantidad: number, semilla: number): THREE.InstancedMesh {
  const nacar = new THREE.MeshPhysicalMaterial({ color: '#efe9df', roughness: 0.15, metalness: 0.35, clearcoat: 1, iridescence: 0.6 });
  const malla = new THREE.InstancedMesh(new THREE.SphereGeometry(0.28, 12, 10), nacar, cantidad);
  const auxiliar = new THREE.Object3D();
  const azar = new Aleatorio(semilla);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    const distancia = distanciaAlAzar(tope, azar, 0.3);
    auxiliar.position.set(Math.cos(angulo) * distancia, tope.altura - distancia * tope.caida + 0.2, Math.sin(angulo) * distancia);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

/**
 * Pone sobre una pieza las decoraciones que le tocaron. Las piezas son chicas, así que
 * todo va a escala: una vela sola al centro, fresas y flores en un anillito, etc.
 */
function decorarPieza(grupo: THREE.Group, tope: Tope, cuenta: DecoracionPieza, llamas: THREE.Mesh[], semilla: number): void {
  const escala = 0.62;
  // En la dona todo va sobre el aro (a la mitad entre el hoyo y la orilla), nunca en el centro
  const hueco = huecoDe(tope);
  const conHoyo = hueco > 0;
  const aro = (hueco + tope.radio) / 2;
  // Lo que va en el centro sube hasta la punta; lo que va alrededor, un poco más abajo
  if (cuenta.chispas > 0) {
    grupo.add(chispasSobre(tope, 40, semilla));
  }
  if (cuenta.perlas > 0) {
    grupo.add(perlitasSobre(tope, Math.min(18, 6 * cuenta.perlas), semilla + 3));
  }
  if (cuenta.fruta > 0) {
    const fruta = cubitos(cuenta.fruta, COLORES_FRUTA, tope.radio * 0.8, hueco, 0, semilla + 5);
    fruta.position.y = tope.altura - tope.radio * 0.4 * tope.caida - 0.1;
    grupo.add(fruta);
  }
  if (cuenta.frutosRojos > 0) {
    const frutos = frutosRojos(cuenta.frutosRojos, tope.radio * 0.75, hueco, 0, semilla + 9);
    frutos.scale.setScalar(0.8);
    frutos.position.y = tope.altura - tope.radio * 0.45 * tope.caida - 0.05;
    grupo.add(frutos);
  }
  if (cuenta.hojaOro > 0) {
    grupo.add(hojuelasSobre(tope, Math.min(30, 10 * cuenta.hojaOro), semilla + 11));
  }
  if (cuenta.rosetas > 0) {
    // Una roseta de crema en el centro (si son más, alrededor)
    const crema = material('#ffffff', 0.7, 0);
    const geometria = geometriaRoseta();
    for (let k = 0; k < cuenta.rosetas; k++) {
      const roseta = new THREE.Mesh(geometria, crema);
      let distancia = 0;
      if (cuenta.rosetas > 1) {
        distancia = tope.radio * 0.55;
      }
      if (conHoyo) {
        distancia = aro;
      }
      const angulo = (k / cuenta.rosetas) * Math.PI * 2;
      roseta.scale.setScalar(0.7);
      roseta.position.set(Math.cos(angulo) * distancia, tope.altura - distancia * tope.caida - 0.15, Math.sin(angulo) * distancia);
      grupo.add(roseta);
    }
  }
  if (cuenta.flores > 0) {
    const geometria = geometriaRosa('#e86a92');
    const petalos = materialRosa();
    for (let k = 0; k < cuenta.flores; k++) {
      const flor = new THREE.Mesh(geometria, petalos);
      let distancia = 0;
      if (cuenta.flores > 1 || cuenta.velas > 0 || cuenta.fresas > 0) {
        distancia = tope.radio * 0.6;
      }
      if (conHoyo) {
        distancia = aro;
      }
      const angulo = (k / cuenta.flores) * Math.PI * 2 + Math.PI / 2;
      flor.scale.setScalar(0.75);
      flor.position.set(Math.cos(angulo) * distancia, tope.altura - distancia * tope.caida + 0.15, Math.sin(angulo) * distancia);
      // Mirando hacia arriba, un poco hacia afuera
      flor.rotation.x = -Math.PI / 2 + 0.35 * Math.sign(distancia);
      flor.rotateZ(angulo);
      grupo.add(flor);
    }
  }
  if (cuenta.fresas > 0) {
    let radio = tope.radio * 0.55;
    if (cuenta.velas > 0 && cuenta.fresas === 1) {
      radio = tope.radio * 0.6;
    }
    if (conHoyo) {
      radio = aro;
    }
    const grupoFresas = fresas(cuenta.fresas, 0, radio / escala, cuenta.velas > 0 || conHoyo);
    grupoFresas.scale.setScalar(escala);
    grupoFresas.position.y = tope.altura - (radio * 0.7) * tope.caida - 0.1;
    if (cuenta.fresas === 1 && cuenta.velas === 0 && !conHoyo) {
      grupoFresas.position.y = tope.altura - 0.15;
    }
    grupo.add(grupoFresas);
  }
  if (cuenta.velas > 0) {
    let radioVelas = tope.radio * 0.4;
    if (conHoyo) {
      radioVelas = aro;
    }
    const grupoVelas = velas(cuenta.velas, radioVelas / escala, 0, llamas, conHoyo);
    grupoVelas.scale.setScalar(escala);
    grupoVelas.position.y = tope.altura - 0.2;
    let adelante = 0;
    if (tope.velasAdelante !== undefined) {
      adelante = tope.velasAdelante;
    }
    grupoVelas.position.z = adelante;
    if (tope.remolino === true) {
      // Sobre el remolino la vela se entierra en el betún: entre más lejos del centro, más abajo
      let lejos = radioVelas + Math.abs(adelante);
      if (cuenta.velas === 1) {
        lejos = Math.abs(adelante);
      }
      grupoVelas.position.y = ALTURA_REMOLINO + alturaRemolino(lejos) - 0.45;
    }
    grupo.add(grupoVelas);
  }
}

// Decoraciones de una sola pieza (con tope para que no se sature: es chica)
function decoracionDeUnaPieza(diseno: DisenoPastel): DecoracionPieza {
  const d = diseno.decoraciones;
  return {
    velas: Math.min(3, d.velas),
    fresas: Math.min(3, d.fresas),
    flores: Math.min(3, d.flores),
    rosetas: Math.min(3, d.rosetas),
    perlas: Math.min(3, d.perlas),
    chispas: d.chispas,
    fruta: Math.min(8, d.frutaPicada),
    frutosRojos: Math.min(2, d.frutosRojos),
    hojaOro: Math.min(2, d.hojaOro),
  };
}

// ===================== Cupcake =====================

// Medidas del cupcake normal (el tamaño elegido lo escala)
const ANCHO_CAPACILLO = 6.6;
const ALTO_CAPACILLO = 3.6;
// Radio del capacillo a cierta altura (es más angosto abajo)
function radioCapacillo(altura: number): number {
  return 2.55 + (3.3 - 2.55) * (altura / ALTO_CAPACILLO);
}

// Onda en triángulo (de -1 a 1): da los pliegues marcados del papel
function ondaTriangulo(valor: number): number {
  const parte = valor - Math.floor(valor);
  return Math.abs(parte - 0.5) * 4 - 1;
}

/**
 * Papel del capacillo: franjas claras y oscuras por cada pliegue y bolitas.
 * Con betún claro el papel es rosa con bolitas blancas; con betún de color, blanco con bolitas del color.
 */
function texturaCapacillo(fondo: string, bolitas: string, pliegues: number): THREE.CanvasTexture {
  const contexto = lienzo(1024, 256);
  contexto.fillStyle = fondo;
  contexto.fillRect(0, 0, 1024, 256);
  const anchoPliegue = 1024 / pliegues;
  for (let i = 0; i < pliegues; i++) {
    // Media franja en sombra: así se notan los pliegues
    contexto.fillStyle = 'rgba(0,0,0,0.07)';
    contexto.fillRect(i * anchoPliegue + anchoPliegue / 2, 0, anchoPliegue / 2, 256);
  }
  contexto.fillStyle = bolitas;
  const azar = new Aleatorio(77);
  for (let fila = 0; fila < 5; fila++) {
    for (let columna = 0; columna < 26; columna++) {
      const x = columna * (1024 / 26) + (fila % 2) * (1024 / 52) + (azar.siguiente() - 0.5) * 4;
      const y = 30 + fila * 48;
      contexto.beginPath();
      contexto.arc(x, y, 7, 0, Math.PI * 2);
      contexto.fill();
    }
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  textura.anisotropy = 4;
  return textura;
}

// Capacillo de papel plisado, con la orilla de arriba un poco abierta
function capacilloMejorado(diseno: DisenoPastel): THREE.Mesh {
  const perfil: THREE.Vector2[] = [new THREE.Vector2(0, 0), new THREE.Vector2(2.5, 0), new THREE.Vector2(2.55, 0.04)];
  for (let k = 1; k <= 10; k++) {
    const altura = (k / 10) * ALTO_CAPACILLO;
    perfil.push(new THREE.Vector2(radioCapacillo(altura), altura));
  }
  // Orilla doblada hacia afuera
  perfil.push(new THREE.Vector2(radioCapacillo(ALTO_CAPACILLO) + 0.12, ALTO_CAPACILLO + 0.08));
  const pliegues = 34;
  const geometria = new THREE.LatheGeometry(perfil, pliegues * 6);
  const posiciones = geometria.attributes['position'];
  for (let i = 0; i < posiciones.count; i++) {
    const x = posiciones.getX(i);
    const z = posiciones.getZ(i);
    const y = posiciones.getY(i);
    if (y < 0.02) {
      continue;
    }
    const angulo = Math.atan2(z, x);
    const factor = 1 + 0.04 * ondaTriangulo((angulo / (Math.PI * 2)) * pliegues);
    posiciones.setX(i, x * factor);
    posiciones.setZ(i, z * factor);
  }
  geometria.computeVertexNormals();
  // Colores del papel según el betún
  let fondo = '#f3a6bf';
  let bolitas = '#ffffff';
  const betun = new THREE.Color(diseno.colorCobertura);
  const hsl = { h: 0, s: 0, l: 0 };
  betun.getHSL(hsl);
  if (diseno.cobertura !== 'ninguna' && (hsl.s > 0.3 || hsl.l < 0.6)) {
    fondo = '#fbf8f3';
    bolitas = diseno.colorCobertura;
  }
  let papel: THREE.Material = new THREE.MeshStandardMaterial({ map: texturaCapacillo(fondo, bolitas, pliegues), roughness: 0.75, side: THREE.DoubleSide });
  if (luminosidad(diseno.colorBizcocho) < 0.3 && diseno.cobertura !== 'ninguna') {
    // Cupcake de chocolate: capacillo dorado
    papel = new THREE.MeshStandardMaterial({ map: texturaCapacillo('#d9ae4f', '#f6e3a8', pliegues), metalness: 0.6, roughness: 0.35, side: THREE.DoubleSide });
  }
  return new THREE.Mesh(geometria, papel);
}

/**
 * Remolino de betún hecho con duya de estrella: un tubo con estrías que da 2.6 vueltas,
 * cada vuelta encima de la anterior y más cerrada, y termina en punta.
 * Sale apoyado en y = 0 y mide unos 3.5 cm de radio.
 */
function geometriaRemolino(): THREE.BufferGeometry {
  const pasos = 240;
  const lados = 28;
  const vueltas = 2.6;
  const posiciones: number[] = [];
  const uv: number[] = [];
  const indices: number[] = [];
  for (let i = 0; i <= pasos; i++) {
    const t = i / pasos;
    const angulo = t * vueltas * Math.PI * 2;
    const radioEspiral = 2.5 * (1 - t) + 0.05;
    // El tubo se adelgaza hacia arriba; al principio y al final se cierra
    let grosor = 1.05 * (1 - 0.5 * t);
    grosor = grosor * (0.35 + 0.65 * THREE.MathUtils.smoothstep(t, 0, 0.05));
    if (t > 0.88) {
      grosor = grosor * (1 - 0.92 * THREE.MathUtils.smoothstep(t, 0.88, 1));
    }
    const altura = 0.8 + t * 3.4;
    const cx = Math.cos(angulo) * radioEspiral;
    const cz = Math.sin(angulo) * radioEspiral;
    for (let j = 0; j <= lados; j++) {
      const giro = (j / lados) * Math.PI * 2;
      // 8 estrías de la duya de estrella
      const radio = grosor * (1 + 0.15 * Math.cos(giro * 8));
      const hacia = Math.cos(giro) * radio;
      posiciones.push(cx + Math.cos(angulo) * hacia, altura + Math.sin(giro) * radio * 0.85, cz + Math.sin(angulo) * hacia);
      uv.push(t, j / lados);
    }
  }
  for (let i = 0; i < pasos; i++) {
    for (let j = 0; j < lados; j++) {
      const a = i * (lados + 1) + j;
      const b = a + lados + 1;
      indices.push(a, a + 1, b, b, a + 1, b + 1);
    }
  }
  const geometria = new THREE.BufferGeometry();
  geometria.setAttribute('position', new THREE.Float32BufferAttribute(posiciones, 3));
  geometria.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  geometria.setIndex(indices);
  geometria.computeVertexNormals();
  return geometria;
}

// Altura donde empieza el remolino sobre el pan del cupcake
const ALTURA_REMOLINO = 4.55;

/**
 * Altura de la superficie del remolino a cierta distancia del centro (medida desde donde empieza).
 * Usa las mismas medidas que geometriaRemolino: busca la vuelta del tubo que pasa más alto por ese punto.
 */
function alturaRemolino(distancia: number): number {
  let mayor = 0;
  for (let i = 0; i <= 200; i++) {
    const t = i / 200;
    const radioEspiral = 2.5 * (1 - t) + 0.05;
    let grosor = 1.05 * (1 - 0.5 * t);
    if (t > 0.88) {
      grosor = grosor * (1 - 0.92 * THREE.MathUtils.smoothstep(t, 0.88, 1));
    }
    const separacion = Math.abs(radioEspiral - distancia);
    if (separacion < grosor) {
      const altura = 0.8 + t * 3.4 + Math.sqrt(grosor * grosor - separacion * separacion) * 0.85;
      mayor = Math.max(mayor, altura);
    }
  }
  return mayor;
}

/**
 * Un cupcake (sin base): capacillo, panquecito que se desborda y su cobertura.
 * Betún, chantilly, chispas y degradado van en remolino con duya; fondant, ganache y mármol, en domo liso.
 */
function cuerpoCupcake(diseno: DisenoPastel): { grupo: THREE.Group; tope: Tope } {
  const grupo = new THREE.Group();
  grupo.add(capacilloMejorado(diseno));
  const perfilPan = [
    new THREE.Vector2(0, 3.4),
    new THREE.Vector2(3.25, 3.45),
    new THREE.Vector2(3.6, 3.85),
    new THREE.Vector2(3.55, 4.4),
    new THREE.Vector2(3.0, 5.0),
    new THREE.Vector2(1.7, 5.4),
    new THREE.Vector2(0, 5.5),
  ];
  const relieve = texturaRelieve(14);
  relieve.repeat.set(3, 2);
  const pan = new THREE.Mesh(
    new THREE.LatheGeometry(perfilPan, 64),
    new THREE.MeshStandardMaterial({ map: texturaMasa(horneado(diseno.colorBizcocho, 0.3), 3), roughness: 0.85, bumpMap: relieve, bumpScale: 1.5 }),
  );
  grupo.add(pan);
  const tope: Tope = { altura: 5.5, radio: 2.5, caida: 0.1 };
  if (diseno.cobertura === 'ninguna') {
    return { grupo: grupo, tope: tope };
  }
  if (esCremosa(diseno.cobertura)) {
    const remolino = cubrir(diseno, geometriaRemolino(), 6, false, false);
    remolino.position.y = ALTURA_REMOLINO;
    grupo.add(remolino);
    tope.altura = ALTURA_REMOLINO + 4.6;
    tope.remolino = true;
    tope.radio = 2.5;
    tope.caida = 1.15;
    if (diseno.cobertura === 'chispas') {
      grupo.add(chispasSobre(tope, 110, 50));
    }
    return { grupo: grupo, tope: tope };
  }
  // Domo liso de cobertura sobre el pan
  const perfilDomo = [
    new THREE.Vector2(3.65, 4.25),
    new THREE.Vector2(3.6, 4.75),
    new THREE.Vector2(3.1, 5.4),
    new THREE.Vector2(1.8, 5.85),
    new THREE.Vector2(0, 5.95),
  ];
  const domo = cubrir(diseno, new THREE.LatheGeometry(perfilDomo, 64), 7, false, false);
  grupo.add(domo);
  tope.altura = 5.95;
  tope.radio = 2.7;
  tope.caida = 0.14;
  return { grupo: grupo, tope: tope };
}

/**
 * Un solo cupcake en su base. El tamaño lo hace más grande (mini, normal, grande, jumbo).
 * Todas las decoraciones van arriba; el listón rodea el capacillo y el moño va al frente.
 * La dedicatoria va en el topper, encajado al frente en el betún; las velas quedan detrás de él.
 */
function cupcakeSolo(diseno: DisenoPastel): PastelConstruido {
  const raiz = new THREE.Group();
  const llamas: THREE.Mesh[] = [];
  // Se muestra al doble de su tamaño real para que se vea bien en el visor (el visor no deja acercar tanto)
  const escala = (diseno.diametro / ANCHO_CAPACILLO) * 2;
  const conMensaje = diseno.dedicatoria !== '';
  let radioBase = 5.2 * escala;
  if (conMensaje) {
    radioBase = 6.6 * escala;
  }
  const base = basePostre(diseno.base, 'redondo', radioBase * 2, 0);
  raiz.add(base.grupo);

  const modelo = new THREE.Group();
  const hecho = cuerpoCupcake(diseno);
  modelo.add(hecho.grupo);
  const d = diseno.decoraciones;
  const cuenta: DecoracionPieza = {
    velas: Math.min(3, d.velas),
    fresas: Math.min(3, d.fresas),
    flores: Math.min(3, d.flores),
    rosetas: Math.min(3, d.rosetas),
    perlas: Math.min(3, d.perlas),
    chispas: d.chispas,
    fruta: Math.min(8, d.frutaPicada),
    frutosRojos: Math.min(2, d.frutosRojos),
    hojaOro: Math.min(2, d.hojaOro),
  };
  if (diseno.topper !== '') {
    // Las velas se recorren hacia atrás: así sobresalen detrás del letrero y no tapan el texto
    hecho.tope.velasAdelante = -1.0;
  }
  decorarPieza(modelo, hecho.tope, cuenta, llamas, 300);
  // Listón alrededor del capacillo y moño al frente
  const colorTela = colorListon(diseno);
  let alturaMono = ALTO_CAPACILLO * 0.55;
  if (d.liston > 0) {
    const abajo = 1.3;
    const arriba = 2.5;
    const banda = new THREE.Mesh(
      new THREE.CylinderGeometry(radioCapacillo(arriba) * 1.045 + 0.06, radioCapacillo(abajo) * 1.045 + 0.06, arriba - abajo, 72, 1, true),
      telaSatinada(colorTela),
    );
    banda.position.y = (abajo + arriba) / 2;
    modelo.add(banda);
    alturaMono = (abajo + arriba) / 2 - 0.4;
  }
  if (d.mono > 0) {
    // El moño se pone sobre la orilla del capacillo (como si fuera un pastel redondo chiquito)
    const redondo: DisenoPastel = Object.assign({}, diseno, { forma: 'redondo' });
    modelo.add(mono(redondo, radioCapacillo(alturaMono + 0.4) * 2.1, alturaMono, colorTela, -Math.PI / 2, 0.42));
  }
  let alturaTopper = 0;
  if (diseno.topper !== '') {
    // El letrero va al frente y casi pegado al betún: los palitos quedan enterrados y no se ve flotando
    const adelante = 1.0;
    let arribaBetun = hecho.tope.altura + 1.0;
    if (hecho.tope.remolino === true) {
      // Los palitos entran al remolino un poco lejos del centro, donde el betún está más abajo
      const separacionPalitos = 7 * 0.22;
      const distanciaPalitos = Math.sqrt(separacionPalitos * separacionPalitos + adelante * adelante);
      arribaBetun = ALTURA_REMOLINO + alturaRemolino(distanciaPalitos) + 1.2;
    }
    // Lo que hay del pie del topper a la orilla de abajo de las letras
    const pieLetrero = ALTO_PALITO_TOPPER;
    const letrero = topper(diseno.topper, 7);
    letrero.position.set(0, arribaBetun - pieLetrero, adelante);
    modelo.add(letrero);
    alturaTopper = arribaBetun - pieLetrero + altoTopper(7, diseno.topper) - hecho.tope.altura;
  }
  modelo.scale.setScalar(escala);
  modelo.position.y = base.arriba;
  raiz.add(modelo);

  if (conMensaje) {
    const placa = placaMensaje(diseno, 5 * escala);
    placa.position.set(0, base.arriba, 4.9 * escala);
    placa.rotation.x = -0.12;
    raiz.add(placa);
  }
  conSombras(raiz);
  let arribaTodo = hecho.tope.altura;
  if (cuenta.velas > 0) {
    arribaTodo = arribaTodo + 5.5;
  }
  arribaTodo = Math.max(arribaTodo, hecho.tope.altura + alturaTopper);
  return { grupo: raiz, alturaTotal: base.arriba + arribaTodo * escala, anchoTotal: radioBase * 2, llamas: llamas };
}

// ===================== Galleta =====================

// Una galleta: disco horneado con orilla irregular; sin cobertura lleva chispas de chocolate
function galleta(diseno: DisenoPastel, indice: number): { grupo: THREE.Group; tope: Tope } {
  const grupo = new THREE.Group();
  const perfil = [
    new THREE.Vector2(0, 0),
    new THREE.Vector2(3.85, 0),
    new THREE.Vector2(4.1, 0.3),
    new THREE.Vector2(4.0, 0.75),
    new THREE.Vector2(3.4, 1.05),
    new THREE.Vector2(1.8, 1.18),
    new THREE.Vector2(0, 1.2),
  ];
  const geometria = new THREE.LatheGeometry(perfil, 64);
  // Orilla irregular (cada galleta distinta)
  const posiciones = geometria.attributes['position'];
  for (let i = 0; i < posiciones.count; i++) {
    const x = posiciones.getX(i);
    const z = posiciones.getZ(i);
    const angulo = Math.atan2(z, x);
    const factor = 1 + 0.04 * Math.sin(angulo * 5 + indice * 1.7) + 0.02 * Math.sin(angulo * 11 + indice);
    posiciones.setX(i, x * factor);
    posiciones.setZ(i, z * factor);
  }
  geometria.computeVertexNormals();
  // La galleta se hornea más que el pan: queda dorada
  const masa = horneado(diseno.colorBizcocho, 0.6);
  const relieve = texturaRelieve(9 + indice);
  relieve.repeat.set(2, 2);
  grupo.add(new THREE.Mesh(geometria, new THREE.MeshStandardMaterial({ map: texturaMasa(masa, 7 + indice), roughness: 0.85, bumpMap: relieve, bumpScale: 2 })));
  const tope: Tope = { altura: 1.2, radio: 3.2, caida: 0.02 };
  if (diseno.cobertura === 'ninguna') {
    // Chispas de chocolate (blancas si la galleta es de chocolate)
    let colorChispa = '#3b2014';
    if (luminosidad(masa) < 0.3) {
      colorChispa = '#f3ead8';
    }
    const azar = new Aleatorio(60 + indice);
    const chispa = new THREE.ConeGeometry(0.42, 0.5, 8);
    const materialChispa = material(colorChispa, 0.5, 0);
    for (let k = 0; k < 12; k++) {
      const angulo = azar.siguiente() * Math.PI * 2;
      const distancia = Math.sqrt(azar.siguiente()) * 3.1;
      const pieza = new THREE.Mesh(chispa, materialChispa);
      pieza.position.set(Math.cos(angulo) * distancia, 1.2 - distancia * 0.05, Math.sin(angulo) * distancia);
      pieza.rotation.set(azar.siguiente() * 0.4, 0, azar.siguiente() * 0.4);
      grupo.add(pieza);
    }
    return { grupo: grupo, tope: tope };
  }
  // Betún o glaseado encima: una capa delgada que sigue la galleta
  const capa = cubrir(diseno, new THREE.CylinderGeometry(3.45, 3.55, 0.28, 48), 7, false, true);
  capa.position.y = 1.26;
  grupo.add(capa);
  tope.altura = 1.42;
  if (diseno.cobertura === 'chispas') {
    grupo.add(chispasSobre(tope, 55, 70 + indice));
  }
  return { grupo: grupo, tope: tope };
}

// ===================== Brownie =====================

// Costra del brownie: brillante y cuarteada
function texturaCuarteada(color: string, semilla: number): THREE.CanvasTexture {
  const contexto = lienzo(256, 256);
  contexto.fillStyle = color;
  contexto.fillRect(0, 0, 256, 256);
  const azar = new Aleatorio(semilla);
  contexto.strokeStyle = 'rgba(0,0,0,0.35)';
  for (let i = 0; i < 26; i++) {
    let x = azar.siguiente() * 256;
    let y = azar.siguiente() * 256;
    contexto.lineWidth = 1 + azar.siguiente() * 2;
    contexto.beginPath();
    contexto.moveTo(x, y);
    for (let paso = 0; paso < 6; paso++) {
      x = x + (azar.siguiente() - 0.5) * 50;
      y = y + (azar.siguiente() - 0.5) * 50;
      contexto.lineTo(x, y);
    }
    contexto.stroke();
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

// Cuadro de 6 cm con las orillas apenas redondeadas
function geometriaCuadro(lado: number, alto: number, orilla: number): THREE.ExtrudeGeometry {
  const mitad = lado / 2 - orilla;
  const forma = new THREE.Shape();
  forma.moveTo(-mitad, -mitad);
  forma.lineTo(mitad, -mitad);
  forma.lineTo(mitad, mitad);
  forma.lineTo(-mitad, mitad);
  forma.lineTo(-mitad, -mitad);
  const geometria = new THREE.ExtrudeGeometry(forma, {
    depth: alto - 2 * orilla,
    bevelEnabled: true,
    bevelThickness: orilla,
    bevelSize: orilla,
    bevelSegments: 3,
  });
  geometria.translate(0, 0, orilla);
  geometria.rotateX(-Math.PI / 2);
  return geometria;
}

function brownie(diseno: DisenoPastel, indice: number): { grupo: THREE.Group; tope: Tope } {
  const grupo = new THREE.Group();
  const lado = 6;
  const alto = 3;
  const costra = texturaCuarteada(horneado(diseno.colorBizcocho, 0.05), 80 + indice);
  costra.repeat.set(1 / lado, 1 / lado);
  costra.offset.set(0.5, 0.5);
  const migaja = texturaMasa(diseno.colorBizcocho, 90 + indice);
  migaja.wrapS = THREE.RepeatWrapping;
  migaja.repeat.set(1 / 3, 1 / 3);
  const cuerpo = new THREE.Mesh(geometriaCuadro(lado, alto, 0.3), [
    new THREE.MeshPhysicalMaterial({ map: costra, roughness: 0.45, clearcoat: 0.3 }),
    new THREE.MeshStandardMaterial({ map: migaja, roughness: 0.9 }),
  ]);
  grupo.add(cuerpo);
  const tope: Tope = { altura: alto, radio: 2.4, caida: 0 };
  if (diseno.cobertura === 'ninguna') {
    return { grupo: grupo, tope: tope };
  }
  // Capa de cobertura encima
  const capa = cubrir(diseno, geometriaCuadro(lado - 0.3, 0.5, 0.18), lado, true, true);
  capa.position.y = alto - 0.1;
  grupo.add(capa);
  tope.altura = alto + 0.4;
  if (diseno.cobertura === 'chispas') {
    grupo.add(chispasSobre({ altura: tope.altura, radio: 2.7, caida: 0 }, 55, 100 + indice));
  }
  return { grupo: grupo, tope: tope };
}

// ===================== Dona =====================

// Masa de la dona: dorada con la franja clara a la mitad
function texturaDona(color: string): THREE.CanvasTexture {
  const contexto = lienzo(64, 256);
  const degradado = contexto.createLinearGradient(0, 0, 0, 256);
  const claro = '#' + new THREE.Color(color).lerp(new THREE.Color('#fbf1dc'), 0.6).getHexString();
  degradado.addColorStop(0, color);
  degradado.addColorStop(0.42, color);
  degradado.addColorStop(0.5, claro);
  degradado.addColorStop(0.58, color);
  degradado.addColorStop(1, color);
  contexto.fillStyle = degradado;
  contexto.fillRect(0, 0, 64, 256);
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

// Medidas de la dona: radio del aro (al centro del tubo), ancho y alto del tubo
const RADIO_DONA = 3.25;
const ANCHO_TUBO_DONA = 1.6;
const ALTO_TUBO_DONA = 1.55;

/**
 * Corte del tubo de la dona: un óvalo esponjado, más plano abajo (donde se apoya al freírse).
 * "a" es el ángulo alrededor del tubo: π = orilla del hoyo, 3π/2 = abajo, 2π = orilla de afuera, 5π/2 = arriba.
 * "extra" agranda el corte (para el glaseado, que va encima de la masa).
 */
function puntoTuboDona(a: number, extra: number): THREE.Vector2 {
  let alto = ALTO_TUBO_DONA + extra;
  if (Math.sin(a) < 0) {
    // Abajo es más plano
    alto = ALTO_TUBO_DONA * 0.72 + extra;
  }
  const x = RADIO_DONA + (ANCHO_TUBO_DONA + extra) * Math.cos(a);
  const y = ALTO_TUBO_DONA * 0.72 + alto * Math.sin(a);
  return new THREE.Vector2(Math.max(0.05, x), y);
}

function dona(diseno: DisenoPastel, indice: number): { grupo: THREE.Group; tope: Tope } {
  const grupo = new THREE.Group();
  // Masa: el corte da toda la vuelta (de la orilla del hoyo, por abajo y afuera, hasta arriba y de regreso)
  const perfil: THREE.Vector2[] = [];
  for (let k = 0; k <= 48; k++) {
    perfil.push(puntoTuboDona(Math.PI + (k / 48) * Math.PI * 2, 0));
  }
  const relieve = texturaRelieve(31 + indice);
  relieve.repeat.set(4, 1);
  const masa = new THREE.Mesh(
    new THREE.LatheGeometry(perfil, 72),
    new THREE.MeshStandardMaterial({ map: texturaDona(horneado(diseno.colorBizcocho, 0.35)), roughness: 0.8, bumpMap: relieve, bumpScale: 1.4 }),
  );
  grupo.add(masa);
  const arriba = ALTO_TUBO_DONA * 0.72 + ALTO_TUBO_DONA;
  const tope: Tope = { altura: arriba, radio: RADIO_DONA + ANCHO_TUBO_DONA * 0.75, caida: 0, hueco: RADIO_DONA - ANCHO_TUBO_DONA * 0.75 };
  if (diseno.cobertura === 'ninguna') {
    // Sin glaseado: azúcar glas espolvoreada arriba
    const azucar = new THREE.InstancedMesh(new THREE.SphereGeometry(0.05, 5, 4), material('#fffdf8', 0.9, 0), 260);
    const auxiliar = new THREE.Object3D();
    const azar = new Aleatorio(140 + indice);
    for (let i = 0; i < 260; i++) {
      const vuelta = azar.siguiente() * Math.PI * 2;
      const punto = puntoTuboDona(Math.PI / 2 + (azar.siguiente() - 0.5) * 1.6, 0.03);
      auxiliar.position.set(Math.cos(vuelta) * punto.x, punto.y, Math.sin(vuelta) * punto.x);
      auxiliar.updateMatrix();
      azucar.setMatrixAt(i, auxiliar.matrix);
    }
    grupo.add(azucar);
    return { grupo: grupo, tope: tope };
  }
  // Glaseado: cubre la parte de arriba del tubo; las orillas bajan en escurridos
  const vueltas = 96;
  const pasos = 28;
  const posiciones: number[] = [];
  const uv: number[] = [];
  const indices: number[] = [];
  const azar = new Aleatorio(77 + indice);
  // Largo de cada escurrido por la orilla de afuera y la del hoyo (unos largos, otros cortos)
  const gotasAfuera: number[] = [];
  const gotasDentro: number[] = [];
  for (let k = 0; k < 9; k++) {
    gotasAfuera.push(0.15 + azar.siguiente() * 0.5);
    gotasDentro.push(0.1 + azar.siguiente() * 0.3);
  }
  for (let v = 0; v <= vueltas; v++) {
    const vuelta = (v / vueltas) * Math.PI * 2;
    // Escurridos: bultos suaves alrededor de la dona
    const fase = (vuelta / (Math.PI * 2)) * 9;
    const gota = Math.floor(fase) % 9;
    const dentroDeGota = Math.pow(Math.max(0, Math.sin((fase % 1) * Math.PI)), 3);
    const bajaAfuera = gotasAfuera[gota] * dentroDeGota;
    const bajaDentro = gotasDentro[gota] * dentroDeGota;
    for (let k = 0; k <= pasos; k++) {
      const t = k / pasos;
      // De la orilla de afuera (un poco abajo del ecuador) por arriba hasta la orilla del hoyo
      const inicio = 0.25 - bajaAfuera;
      const fin = Math.PI - 0.15 + bajaDentro;
      const a = inicio + t * (fin - inicio);
      const punto = puntoTuboDona(a, 0.09);
      posiciones.push(Math.cos(vuelta) * punto.x, punto.y, Math.sin(vuelta) * punto.x);
      uv.push(v / vueltas, t);
    }
  }
  for (let v = 0; v < vueltas; v++) {
    for (let k = 0; k < pasos; k++) {
      const a = v * (pasos + 1) + k;
      const b = a + pasos + 1;
      indices.push(a, a + 1, b, b, a + 1, b + 1);
    }
  }
  const glaseado = new THREE.BufferGeometry();
  glaseado.setAttribute('position', new THREE.Float32BufferAttribute(posiciones, 3));
  glaseado.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  glaseado.setIndex(indices);
  glaseado.computeVertexNormals();
  const capa = cubrir(diseno, glaseado, 8, false, false);
  // El glaseado se ve por los dos lados (por dentro del hoyo también)
  (capa.material as THREE.Material).side = THREE.DoubleSide;
  grupo.add(capa);
  tope.altura = arriba + 0.09;
  if (diseno.cobertura === 'chispas') {
    const malla = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.09, 0.34, 2, 6), material('#ffffff', 0.45, 0), 90);
    const auxiliar = new THREE.Object3D();
    const color = new THREE.Color();
    for (let i = 0; i < 90; i++) {
      const vuelta = azar.siguiente() * Math.PI * 2;
      const punto = puntoTuboDona(Math.PI / 2 + (azar.siguiente() - 0.5) * 2, 0.12);
      auxiliar.position.set(Math.cos(vuelta) * punto.x, punto.y, Math.sin(vuelta) * punto.x);
      auxiliar.rotation.set(0, azar.siguiente() * Math.PI * 2, Math.PI / 2, 'YXZ');
      auxiliar.updateMatrix();
      malla.setMatrixAt(i, auxiliar.matrix);
      malla.setColorAt(i, color.set(COLORES_CHISPAS[i % COLORES_CHISPAS.length]));
    }
    grupo.add(malla);
  }
  return { grupo: grupo, tope: tope };
}

// ===================== Una sola pieza =====================

// Placa de chocolate blanco con el mensaje, acostada al frente de la tabla
function placaMensaje(diseno: DisenoPastel, ancho: number): THREE.Group {
  const grupo = new THREE.Group();
  const placa = new THREE.Mesh(geometriaCuadro(1, 1, 0.08), new THREE.MeshPhysicalMaterial({ color: '#fbf5ee', roughness: 0.35, clearcoat: 0.4 }));
  placa.scale.set(ancho, 0.5, ancho / 2.6);
  grupo.add(placa);
  const texto = new THREE.Mesh(
    new THREE.PlaneGeometry(ancho * 0.95, ancho * 0.95 / 2),
    new THREE.MeshStandardMaterial({ map: texturaDedicatoria(diseno.dedicatoria, colorDedicatoria('#fbf5ee')), transparent: true, roughness: 0.4 }),
  );
  texto.rotation.x = -Math.PI / 2;
  texto.position.y = 0.52;
  grupo.add(texto);
  return grupo;
}

// Ancho de cada pieza tal como se modela (el tamaño elegido la escala)
function anchoNaturalDePieza(tipo: string): number {
  if (tipo === 'macaron') {
    return 4.4;
  }
  if (tipo === 'brownie') {
    return 6;
  }
  if (tipo === 'dona') {
    return (RADIO_DONA + ANCHO_TUBO_DONA) * 2;
  }
  return 8.2;
}

/**
 * Una sola galleta, brownie, dona o macaron en su base (como el cupcake).
 * El tamaño la hace más grande y se muestra al doble para que se vea bien en el visor.
 * El topper va de pie detrás de la pieza, con las letras un poco arriba de ella.
 */
function piezaSola(diseno: DisenoPastel): PastelConstruido {
  const raiz = new THREE.Group();
  const llamas: THREE.Mesh[] = [];
  const anchoNatural = anchoNaturalDePieza(diseno.tipo);
  // Se muestra más grande que su tamaño real para que se vea bien en el visor (no deja acercar tanto)
  const escala = (diseno.diametro / anchoNatural) * 2.5;
  const conTopper = diseno.topper !== '';
  // La base deja lugar atrás para el topper
  let anchoBase = anchoNatural + 2.2;
  if (conTopper) {
    anchoBase = anchoNatural + 3.6;
  }
  let forma: FormaPastel | null = 'redondo';
  if (diseno.tipo === 'brownie') {
    // El brownie va en una base cuadrada
    forma = null;
  }
  const base = basePostre(diseno.base, forma, anchoBase * escala, anchoBase * escala);
  raiz.add(base.grupo);

  const modelo = new THREE.Group();
  let hecha: { grupo: THREE.Group; tope: Tope };
  if (diseno.tipo === 'macaron') {
    hecha = macaron(diseno, 0);
  } else if (diseno.tipo === 'brownie') {
    hecha = brownie(diseno, 0);
  } else if (diseno.tipo === 'dona') {
    hecha = dona(diseno, 0);
  } else {
    hecha = galleta(diseno, 0);
  }
  // Un poco girada, para que se vea natural
  hecha.grupo.rotation.y = -0.3;
  const cuenta = decoracionDeUnaPieza(diseno);
  decorarPieza(hecha.grupo, hecha.tope, cuenta, llamas, 200);
  modelo.add(hecha.grupo);
  let arribaTodo = hecha.tope.altura;
  if (cuenta.velas > 0) {
    arribaTodo = arribaTodo + 3;
  }
  if (conTopper) {
    const anchoLetrero = Math.max(5, anchoNatural * 0.95);
    const letrero = topper(diseno.topper, anchoLetrero);
    // Las letras empiezan un poco arriba de la pieza; los palitos se clavan en la base
    const abajoLetras = hecha.tope.altura + 0.5;
    letrero.position.set(0, abajoLetras - ALTO_PALITO_TOPPER, -(anchoNatural / 2 + 0.8));
    modelo.add(letrero);
    arribaTodo = Math.max(arribaTodo, abajoLetras - ALTO_PALITO_TOPPER + altoTopper(anchoLetrero, diseno.topper));
  }
  modelo.scale.setScalar(escala);
  modelo.position.y = base.arriba;
  raiz.add(modelo);
  dejarUnaLuz(raiz);
  conSombras(raiz);
  return { grupo: raiz, alturaTotal: base.arriba + arribaTodo * escala, anchoTotal: anchoBase * escala, llamas: llamas };
}

// Cada juego de velas trae su luz: con muchas piezas se deja solo una (las luces pesan mucho)
function dejarUnaLuz(raiz: THREE.Object3D): void {
  const luces: THREE.Object3D[] = [];
  raiz.traverse(function (objeto: THREE.Object3D) {
    const luz = objeto as THREE.Light;
    if (luz.isLight) {
      luces.push(objeto);
    }
  });
  for (let i = 1; i < luces.length; i++) {
    luces[i].removeFromParent();
  }
  if (luces.length > 0) {
    // La que queda, arriba del centro
    luces[0].removeFromParent();
    luces[0].position.set(0, 18, 0);
    raiz.add(luces[0]);
  }
}

// ===================== Pay =====================

// Pared del corte del pay: costra abajo y a la orilla, relleno en medio y cobertura arriba
function texturaCortePay(diseno: DisenoPastel, colorCostra: string): THREE.CanvasTexture {
  const contexto = lienzo(512, 256);
  contexto.fillStyle = diseno.colorRelleno;
  contexto.fillRect(0, 0, 512, 256);
  dibujarMigajas(contexto, 512, 256, 33);
  contexto.fillStyle = colorCostra;
  // Base de costra (abajo) y orilla (lado derecho)
  contexto.fillRect(0, 205, 512, 51);
  contexto.fillRect(470, 0, 42, 256);
  if (diseno.cobertura !== 'ninguna' && diseno.cobertura !== 'naked') {
    contexto.fillStyle = diseno.colorCobertura;
    contexto.fillRect(0, 0, 470, 30);
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

function paredPay(textura: THREE.Texture, radio: number, alto: number, angulo: number): THREE.Mesh {
  const x2 = Math.cos(angulo) * radio;
  const z2 = -Math.sin(angulo) * radio;
  const geometria = new THREE.BufferGeometry();
  const vertices = new Float32Array([0, 0, 0, x2, 0, z2, x2, alto, z2, 0, alto, 0]);
  const uv = new Float32Array([0, 0, 1, 0, 1, 1, 0, 1]);
  geometria.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
  geometria.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
  geometria.setIndex([0, 1, 2, 0, 2, 3]);
  geometria.computeVertexNormals();
  const malla = new THREE.Mesh(geometria, new THREE.MeshStandardMaterial({ map: textura, roughness: 0.85, side: THREE.DoubleSide }));
  // La pared está justo en la orilla del corte: no se recorta
  malla.userData['sinRecorte'] = true;
  return malla;
}

/**
 * Pay: costra horneada con la orilla ondulada (pellizcada) y el relleno arriba.
 * El sabor da el color del relleno (fresa, limón, queso...). La cobertura cubre el relleno.
 * Las decoraciones van como en el pastel: fresas en anillo, velas al centro, flores, fruta...
 */
function pay(diseno: DisenoPastel, verDentro: boolean): PastelConstruido {
  const raiz = new THREE.Group();
  const llamas: THREE.Mesh[] = [];
  const radio = diseno.diametro / 2;
  const alto = diseno.alturaPiso;
  const plato = basePostre(diseno.base, 'redondo', diseno.diametro + 8, 0);
  raiz.add(plato.grupo);
  const base = Math.max(0, plato.arriba - 0.15);
  const piso = new THREE.Group();
  piso.position.y = base;
  raiz.add(piso);

  // Costra: chocolate si el sabor es oscuro (pay de oreo), dorada en los demás
  let colorCostra = '#d39a52';
  if (luminosidad(diseno.colorBizcocho) < 0.3) {
    colorCostra = '#3b2b26';
  }
  const ondas = Math.round(radio * 2.4);
  const perfil = [
    new THREE.Vector2(0, 0),
    new THREE.Vector2(radio - 0.6, 0),
    new THREE.Vector2(radio, 0.6),
    new THREE.Vector2(radio + 0.35, alto - 0.6),
    new THREE.Vector2(radio + 0.25, alto),
    new THREE.Vector2(radio - 0.35, alto + 0.2),
    new THREE.Vector2(radio - 0.9, alto - 0.3),
    new THREE.Vector2(radio - 0.85, 0.9),
    new THREE.Vector2(0, 0.9),
  ];
  const geometria = new THREE.LatheGeometry(perfil, ondas * 8);
  // Orilla pellizcada: el borde de arriba sube y baja
  const posiciones = geometria.attributes['position'];
  for (let i = 0; i < posiciones.count; i++) {
    const y = posiciones.getY(i);
    if (y > alto - 0.8) {
      const x = posiciones.getX(i);
      const z = posiciones.getZ(i);
      const onda = Math.abs(Math.cos(Math.atan2(z, x) * ondas / 2));
      posiciones.setY(i, y + onda * 0.45);
      const factor = 1 + onda * 0.025;
      posiciones.setX(i, x * factor);
      posiciones.setZ(i, z * factor);
    }
  }
  geometria.computeVertexNormals();
  const relieve = texturaRelieve(12);
  relieve.repeat.set(3, 3);
  const costra = new THREE.Mesh(geometria, new THREE.MeshStandardMaterial({ map: texturaMasa(colorCostra, 19), roughness: 0.8, bumpMap: relieve, bumpScale: 2 }));
  piso.add(costra);

  // Relleno: disco que llena la costra
  const alturaRelleno = alto - 0.35;
  const cobertura = diseno.cobertura;
  let materialRelleno: THREE.Material = new THREE.MeshPhysicalMaterial({ color: diseno.colorRelleno, roughness: 0.32, clearcoat: 0.5, clearcoatRoughness: 0.3 });
  const geometriaRelleno = new THREE.CylinderGeometry(radio - 0.75, radio - 0.85, alturaRelleno - 0.9, 72);
  if (cobertura !== 'ninguna' && cobertura !== 'naked') {
    materialRelleno = materialCoberturaPieza(diseno, diseno.diametro);
    if (cobertura === 'degradado') {
      pintarDegradadoRadial(geometriaRelleno, diseno.colorCobertura, COLOR_ARRIBA_DEGRADADO);
    }
  }
  const relleno = new THREE.Mesh(geometriaRelleno, [
    new THREE.MeshStandardMaterial({ color: diseno.colorRelleno, roughness: 0.6 }),
    materialRelleno,
    materialRelleno,
  ]);
  relleno.position.y = 0.9 + (alturaRelleno - 0.9) / 2;
  piso.add(relleno);
  const tope: Tope = { altura: alturaRelleno, radio: radio - 1.2, caida: 0 };
  if (cobertura === 'chispas') {
    piso.add(chispasSobre({ altura: alturaRelleno, radio: radio - 1, caida: 0 }, Math.round(radio * radio * 1.5), 120));
  }

  decorarPay(diseno, piso, tope, llamas);

  if (verDentro) {
    const corte = texturaCortePay(diseno, colorCostra);
    piso.add(paredPay(corte, radio + 0.2, alto, CORTE_INICIO));
    piso.add(paredPay(corte, radio + 0.2, alto, CORTE_FIN));
    recortarRebanada(piso);
  }
  conSombras(raiz);
  return { grupo: raiz, alturaTotal: base + alto + alturaArriba(diseno, tope.radio * 2), anchoTotal: diseno.diametro + 8, llamas: llamas };
}

// Lo que sobresale arriba del postre (velas o topper), para encuadrar la cámara
function alturaArriba(diseno: DisenoPastel, tamano: number): number {
  let altura = 0;
  if (diseno.decoraciones.velas > 0) {
    altura = 8.5;
  }
  if (diseno.topper !== '') {
    altura = Math.max(altura, altoTopper(anchoTopper(tamano), diseno.topper));
  }
  return altura;
}

function anchoTopper(tamano: number): number {
  return Math.min(16, tamano * 0.75);
}

// Hojuelas de oro sobre una cara plana, dejando libre el centro
function hojuelasEnAnillo(tope: Tope, radioLibre: number, cantidad: number, semilla: number): THREE.InstancedMesh {
  const malla = new THREE.InstancedMesh(geometriaHojuela(), materialOro(), cantidad);
  const auxiliar = new THREE.Object3D();
  const azar = new Aleatorio(semilla);
  const parteLibre = (radioLibre * radioLibre) / Math.max(0.01, tope.radio * tope.radio);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    const distancia = Math.sqrt(parteLibre + azar.siguiente() * (1 - parteLibre)) * tope.radio;
    auxiliar.position.set(Math.cos(angulo) * distancia, tope.altura + 0.06, Math.sin(angulo) * distancia);
    auxiliar.rotation.set(-Math.PI / 2, 0, azar.siguiente() * Math.PI * 2);
    auxiliar.scale.setScalar(0.6 + azar.siguiente() * 0.7);
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
  }
  return malla;
}

// Decoraciones del pay (y del cheesecake): como en la cara de arriba del pastel
function decorarPay(diseno: DisenoPastel, piso: THREE.Group, tope: Tope, llamas: THREE.Mesh[]): void {
  const d = diseno.decoraciones;
  const tamano = tope.radio * 2;
  const conMensaje = diseno.dedicatoria !== '';
  const velaAlCentro = d.velas === 1;
  const fresaAlCentro = d.fresas === 1 && !velaAlCentro;
  let radioLibre = tope.radio * 0.9;
  if (d.rosetas > 0) {
    // Rosetas por la orilla del relleno
    const crema = material('#ffffff', 0.7, 0);
    const geometria = geometriaRoseta();
    const anillo = tope.radio - 0.6;
    const escala = Math.max(0.45, Math.min(1, (Math.PI * 2 * anillo) / d.rosetas / 2.9));
    for (let k = 0; k < d.rosetas; k++) {
      const angulo = (k / d.rosetas) * Math.PI * 2 + Math.PI / 2;
      const roseta = new THREE.Mesh(geometria, crema);
      roseta.scale.setScalar(escala);
      roseta.position.set(Math.cos(angulo) * anillo, tope.altura - 0.1, Math.sin(angulo) * anillo);
      piso.add(roseta);
    }
    radioLibre = Math.min(radioLibre, anillo - 1.6);
  }
  if (d.perlas > 0) {
    // Perlas sobre la orilla de la costra
    const anillo = tope.radio + 0.75;
    const cantidad = Math.round((Math.PI * 2 * anillo) / 1.2);
    const nacar = new THREE.MeshPhysicalMaterial({ color: '#f4efe6', roughness: 0.18, metalness: 0.1, clearcoat: 1, iridescence: 0.7 });
    const collar = new THREE.InstancedMesh(new THREE.SphereGeometry(0.5, 14, 10), nacar, cantidad);
    const auxiliar = new THREE.Object3D();
    for (let k = 0; k < cantidad; k++) {
      const angulo = (k / cantidad) * Math.PI * 2;
      auxiliar.position.set(Math.cos(angulo) * anillo, diseno.alturaPiso + 0.65, Math.sin(angulo) * anillo);
      auxiliar.updateMatrix();
      collar.setMatrixAt(k, auxiliar.matrix);
    }
    piso.add(collar);
  }
  if (d.chispas > 0) {
    piso.add(chispasSobre({ altura: tope.altura, radio: tope.radio, caida: 0 }, Math.round(tope.radio * tope.radio * 1.2), 130));
  }
  if (d.frutaPicada > 0) {
    // Tarta de frutas: la fruta cubre el relleno (deja libre el centro si hay mensaje)
    let libre = 0;
    if (conMensaje) {
      libre = tope.radio * 0.55;
    }
    piso.add(cubitos(d.frutaPicada, COLORES_FRUTA, tope.radio * 0.92, libre, tope.altura - 0.4, 3));
  }
  if (d.frutosRojos > 0) {
    let libre = 0;
    if (conMensaje) {
      libre = tope.radio * 0.55;
    }
    piso.add(frutosRojos(d.frutosRojos, tope.radio * 0.92, libre, tope.altura - 0.1, 14));
  }
  if (d.hojaOro > 0) {
    let libreOro = 0;
    if (conMensaje) {
      libreOro = tope.radio * 0.5;
    }
    piso.add(hojuelasEnAnillo(tope, libreOro, Math.min(80, Math.round(tope.radio * tope.radio * 0.25 * d.hojaOro)), 15));
  }
  if (diseno.topper !== '') {
    const letrero = topper(diseno.topper, anchoTopper(tamano));
    letrero.position.set(0, tope.altura, -tope.radio * 0.62);
    piso.add(letrero);
  }
  if (d.flores > 0) {
    const geometria = geometriaRosa('#e86a92');
    const petalos = materialRosa();
    let anillo = tope.radio * 0.62;
    if (d.flores === 1 && d.velas === 0 && d.fresas === 0 && !conMensaje) {
      anillo = 0;
    }
    const escala = Math.max(0.55, Math.min(1.3, (Math.PI * 2 * Math.max(anillo, 2)) / d.flores / 3));
    for (let k = 0; k < d.flores; k++) {
      const angulo = (k / d.flores) * Math.PI * 2 + Math.PI / 2 + Math.PI / Math.max(1, d.flores);
      const flor = new THREE.Mesh(geometria, petalos);
      flor.scale.setScalar(escala);
      flor.position.set(Math.cos(angulo) * anillo, tope.altura + 0.2, Math.sin(angulo) * anillo);
      flor.rotation.x = -Math.PI / 2;
      flor.rotateZ(angulo);
      piso.add(flor);
    }
    if (anillo > 0) {
      radioLibre = Math.min(radioLibre, anillo - 2);
    }
  }
  if (d.fresas > 0) {
    const radioFresas = tope.radio * 0.8;
    piso.add(fresas(d.fresas, tope.altura, radioFresas, conMensaje && !fresaAlCentro));
    if (!fresaAlCentro) {
      radioLibre = Math.min(radioLibre, radioFresas - 1.6);
    }
  }
  if (d.velas > 0) {
    let radioVelas = tope.radio * 0.55;
    if (d.fresas > 1) {
      radioVelas = tope.radio * 0.5;
    }
    piso.add(velas(d.velas, radioVelas, tope.altura, llamas, conMensaje && !velaAlCentro));
    if (!velaAlCentro) {
      radioLibre = Math.min(radioLibre, radioVelas - 1.8);
    }
  }
  if (conMensaje) {
    let fondo = diseno.colorRelleno;
    if (diseno.cobertura !== 'ninguna' && diseno.cobertura !== 'naked') {
      fondo = diseno.colorCobertura;
    }
    if (velaAlCentro || fresaAlCentro) {
      piso.add(dedicatoriaEnArco(diseno, tamano, tope.altura, Math.max(2.2, tamano * 0.14), tamano * 0.28, fondo));
    } else {
      piso.add(dedicatoria(diseno, tope.altura, Math.max(3, radioLibre), fondo));
    }
  }
}

// ===================== Cheesecake =====================

// ¿El relleno es una salsa de color (fresa, frutos rojos, cajeta, chocolate) y no crema?
function esSalsa(color: string): boolean {
  const hsl = { h: 0, s: 0, l: 0 };
  new THREE.Color(color).getHSL(hsl);
  return hsl.l < 0.75;
}

// Corte del cheesecake: base de galleta abajo, queso en medio y la capa de arriba
function texturaCorteCheesecake(colorGalleta: string, colorQueso: string, colorArriba: string, altoArriba: number, alto: number): THREE.CanvasTexture {
  const contexto = lienzo(512, 256);
  contexto.fillStyle = colorQueso;
  contexto.fillRect(0, 0, 512, 256);
  dibujarMigajas(contexto, 512, 256, 41);
  // Base de galleta (1.4 cm)
  const altoGalleta = Math.round((1.4 / alto) * 256);
  contexto.fillStyle = colorGalleta;
  contexto.fillRect(0, 256 - altoGalleta, 512, altoGalleta);
  dibujarMigajas(contexto, 512, altoGalleta, 43);
  if (altoArriba > 0) {
    contexto.fillStyle = colorArriba;
    contexto.fillRect(0, 0, 512, Math.round((altoArriba / alto) * 256));
  }
  const textura = new THREE.CanvasTexture(contexto.canvas);
  textura.colorSpace = THREE.SRGBColorSpace;
  return textura;
}

/**
 * Cheesecake: base de galleta, cuerpo alto de queso y arriba:
 *  - una capa brillante de fruta (o cajeta, o chocolate) si el sabor la trae,
 *  - la cobertura elegida, o
 *  - la cara horneada, si va al natural.
 * Las decoraciones van como en el pay. Se puede ver por dentro.
 */
function cheesecake(diseno: DisenoPastel, verDentro: boolean): PastelConstruido {
  const raiz = new THREE.Group();
  const llamas: THREE.Mesh[] = [];
  const radio = diseno.diametro / 2;
  const alto = diseno.alturaPiso;
  const plato = basePostre(diseno.base, 'redondo', diseno.diametro + 8, 0);
  raiz.add(plato.grupo);
  const piso = new THREE.Group();
  piso.position.y = plato.arriba;
  raiz.add(piso);

  // Galleta oscura (tipo oreo) si el sabor es de chocolate
  let colorGalleta = '#b9844c';
  let colorQueso = '#f7ead0';
  if (luminosidad(diseno.colorBizcocho) < 0.3) {
    colorGalleta = '#33241f';
    colorQueso = '#7a4b33';
  }
  const migaja = texturaMasa(colorGalleta, 47);
  migaja.repeat.set(3, 1);
  const galleta = new THREE.Mesh(new THREE.CylinderGeometry(radio, radio + 0.05, 1.4, 72), new THREE.MeshStandardMaterial({ map: migaja, roughness: 0.95 }));
  galleta.position.y = 0.7;
  piso.add(galleta);

  // Capa de arriba
  const cobertura = diseno.cobertura;
  const conSalsa = cobertura === 'ninguna' && esSalsa(diseno.colorRelleno);
  let altoArriba = 0;
  if (conSalsa) {
    altoArriba = 0.7;
  } else if (cobertura !== 'ninguna' && cobertura !== 'naked') {
    altoArriba = 0.5;
  }
  const altoQueso = alto - 1.4 - altoArriba;
  const relieve = texturaRelieve(18);
  relieve.repeat.set(4, 2);
  const queso = new THREE.Mesh(
    new THREE.CylinderGeometry(radio - 0.05, radio - 0.05, altoQueso, 72),
    [
      new THREE.MeshStandardMaterial({ color: colorQueso, roughness: 0.7, bumpMap: relieve, bumpScale: 0.4 }),
      new THREE.MeshStandardMaterial({ color: horneado(colorQueso, 0.28), roughness: 0.7 }),
      new THREE.MeshStandardMaterial({ color: colorQueso, roughness: 0.7 }),
    ],
  );
  queso.position.y = 1.4 + altoQueso / 2;
  piso.add(queso);
  let colorArriba = horneado(colorQueso, 0.28);
  if (conSalsa) {
    // Capa de fruta brillante, un poco escurrida por la orilla
    const salsa = new THREE.Mesh(
      new THREE.CylinderGeometry(radio + 0.08, radio + 0.02, altoArriba, 72),
      new THREE.MeshPhysicalMaterial({ color: diseno.colorRelleno, roughness: 0.15, clearcoat: 1, clearcoatRoughness: 0.08 }),
    );
    salsa.position.y = alto - altoArriba / 2;
    piso.add(salsa);
    colorArriba = diseno.colorRelleno;
  } else if (altoArriba > 0) {
    const capa = cubrir(diseno, new THREE.CylinderGeometry(radio + 0.05, radio + 0.05, altoArriba, 72), diseno.diametro, false, true);
    capa.position.y = alto - altoArriba / 2;
    piso.add(capa);
    colorArriba = diseno.colorCobertura;
    if (cobertura === 'chispas') {
      piso.add(chispasSobre({ altura: alto, radio: radio - 0.4, caida: 0 }, Math.round(radio * radio * 1.5), 140));
    }
  }

  const tope: Tope = { altura: alto, radio: radio - 0.6, caida: 0 };
  decorarPay(diseno, piso, tope, llamas);

  if (verDentro) {
    const corte = texturaCorteCheesecake(colorGalleta, colorQueso, colorArriba, altoArriba, alto);
    piso.add(paredPay(corte, radio, alto, CORTE_INICIO));
    piso.add(paredPay(corte, radio, alto, CORTE_FIN));
    recortarRebanada(piso);
  }
  conSombras(raiz);
  return { grupo: raiz, alturaTotal: plato.arriba + alto + alturaArriba(diseno, tope.radio * 2), anchoTotal: diseno.diametro + 8, llamas: llamas };
}

// ===================== Rosca =====================

/**
 * Rosca horneada (tipo bundt): anillo con estrías hacia afuera y un hueco en medio.
 * Con cobertura lleva glaseado que escurre por los lados; las decoraciones van alrededor del anillo.
 */
function rosca(diseno: DisenoPastel): PastelConstruido {
  const raiz = new THREE.Group();
  const llamas: THREE.Mesh[] = [];
  const radio = diseno.diametro / 2;
  const alto = diseno.alturaPiso;
  const hueco = radio * 0.3;
  const medio = (radio + hueco) / 2;
  const plato = basePostre(diseno.base, 'redondo', diseno.diametro + 8, 0);
  raiz.add(plato.grupo);
  const piso = new THREE.Group();
  piso.position.y = plato.arriba;
  raiz.add(piso);

  // Corte del anillo: pared de afuera abombada, arriba redonda y el hueco de en medio
  const exterior = radio * 0.9;
  const perfil: THREE.Vector2[] = [
    new THREE.Vector2(hueco, 0),
    new THREE.Vector2(radio * 0.9, 0),
    new THREE.Vector2(radio * 0.97, alto * 0.12),
    new THREE.Vector2(radio, alto * 0.35),
    new THREE.Vector2(radio * 0.98, alto * 0.58),
  ];
  // Parte de arriba: medio círculo aplastado del lado de afuera al del hueco
  for (let k = 0; k <= 14; k++) {
    const angulo = (k / 14) * Math.PI;
    const x = medio + Math.cos(angulo) * (exterior - medio);
    const y = alto * 0.68 + Math.sin(angulo) * alto * 0.32;
    perfil.push(new THREE.Vector2(x, y));
  }
  perfil.push(new THREE.Vector2(hueco * 0.97, alto * 0.45));
  perfil.push(new THREE.Vector2(hueco, 0.02));
  const limpio = perfil;
  const estrias = 14;
  const geometria = new THREE.LatheGeometry(limpio, estrias * 8);
  const posiciones = geometria.attributes['position'];
  for (let i = 0; i < posiciones.count; i++) {
    const x = posiciones.getX(i);
    const z = posiciones.getZ(i);
    const distancia = Math.hypot(x, z);
    if (distancia < medio) {
      continue;
    }
    // Estrías: la pared de afuera sube y baja alrededor
    const angulo = Math.atan2(z, x);
    const fuerza = (distancia - medio) / (radio - medio);
    const factor = 1 + 0.05 * fuerza * Math.cos(angulo * estrias);
    posiciones.setX(i, x * factor);
    posiciones.setZ(i, z * factor);
  }
  geometria.computeVertexNormals();
  const masa = horneado(diseno.colorBizcocho, 0.4);
  const relieveMasa = texturaRelieve(25);
  relieveMasa.repeat.set(6, 2);
  piso.add(new THREE.Mesh(geometria, new THREE.MeshPhysicalMaterial({ map: texturaMasa(masa, 51), roughness: 0.6, clearcoat: 0.25, bumpMap: relieveMasa, bumpScale: 1.2 })));

  const cobertura = diseno.cobertura;
  const arribaAnillo = alto;
  if (cobertura !== 'ninguna' && cobertura !== 'naked') {
    // Glaseado: cubre la parte redonda de arriba y escurre por la pared de afuera
    const perfilGlaseado: THREE.Vector2[] = [new THREE.Vector2(radio + 0.12, alto * 0.3), new THREE.Vector2(radio * 0.98 + 0.12, alto * 0.58)];
    for (let k = 0; k <= 14; k++) {
      const angulo = (k / 14) * Math.PI;
      perfilGlaseado.push(new THREE.Vector2(medio + Math.cos(angulo) * (exterior - medio + 0.15), alto * 0.68 + Math.sin(angulo) * (alto * 0.32 + 0.12)));
    }
    perfilGlaseado.push(new THREE.Vector2(hueco * 0.97 - 0.12, alto * 0.5));
    const glaseado = new THREE.LatheGeometry(perfilGlaseado, estrias * 8);
    const puntos = glaseado.attributes['position'];
    for (let i = 0; i < puntos.count; i++) {
      const x = puntos.getX(i);
      const y = puntos.getY(i);
      const z = puntos.getZ(i);
      const distancia = Math.hypot(x, z);
      const angulo = Math.atan2(z, x);
      // Gotas: en la pared de afuera el glaseado baja más en unos lugares que en otros
      const limite = alto * (0.42 + 0.12 * Math.sin(angulo * 9) + 0.07 * Math.sin(angulo * 23));
      let nuevaY = y;
      if (distancia > medio && y < limite) {
        nuevaY = limite;
      }
      // Sigue las estrías de la rosca
      let factor = 1;
      if (distancia > medio) {
        factor = 1 + 0.05 * ((distancia - medio) / (radio - medio)) * Math.cos(angulo * estrias);
      }
      puntos.setXYZ(i, x * factor, nuevaY, z * factor);
    }
    glaseado.computeVertexNormals();
    const capa = cubrir(diseno, glaseado, diseno.diametro, false, false);
    piso.add(capa);
    if (cobertura === 'chispas') {
      piso.add(chispasEnAnillo(hueco + 0.8, radio - 0.8, arribaAnillo, Math.round(radio * radio * 1.2), 150));
    }
  }

  // Decoraciones sobre el anillo
  const d = diseno.decoraciones;
  const anchoAnillo = radio - hueco;
  if (d.chispas > 0) {
    piso.add(chispasEnAnillo(hueco + 0.8, radio - 0.8, arribaAnillo, Math.round(radio * radio * 1.0), 160));
  }
  if (d.frutaPicada > 0) {
    piso.add(cubitos(d.frutaPicada, COLORES_FRUTA, radio - 1.2, hueco + 1.2, arribaAnillo - 0.3, 17));
  }
  if (d.frutosRojos > 0) {
    piso.add(frutosRojos(d.frutosRojos, radio - 1.2, hueco + 1.2, arribaAnillo - 0.1, 18));
  }
  if (d.hojaOro > 0) {
    piso.add(hojuelasEnAnillo({ altura: arribaAnillo, radio: radio - 1, caida: 0 }, hueco + 1, Math.min(90, Math.round(radio * radio * 0.3 * d.hojaOro)), 19));
  }
  if (d.rosetas > 0) {
    const crema = material('#ffffff', 0.7, 0);
    const geometriaCrema = geometriaRoseta();
    const escala = Math.max(0.45, Math.min(1, (Math.PI * 2 * medio) / d.rosetas / 2.9));
    for (let k = 0; k < d.rosetas; k++) {
      const angulo = (k / d.rosetas) * Math.PI * 2 + Math.PI / 2;
      const roseta = new THREE.Mesh(geometriaCrema, crema);
      roseta.scale.setScalar(escala);
      roseta.position.set(Math.cos(angulo) * medio, arribaAnillo - 0.3, Math.sin(angulo) * medio);
      piso.add(roseta);
    }
  }
  if (d.perlas > 0) {
    // Collar de perlas en la base del anillo
    const anillo = radio + 0.1;
    const cantidad = Math.round((Math.PI * 2 * anillo) / 1.1);
    const nacar = new THREE.MeshPhysicalMaterial({ color: '#f4efe6', roughness: 0.18, metalness: 0.1, clearcoat: 1, iridescence: 0.7 });
    const collar = new THREE.InstancedMesh(new THREE.SphereGeometry(0.5, 14, 10), nacar, cantidad);
    const auxiliar = new THREE.Object3D();
    for (let k = 0; k < cantidad; k++) {
      const angulo = (k / cantidad) * Math.PI * 2;
      auxiliar.position.set(Math.cos(angulo) * anillo, 0.5, Math.sin(angulo) * anillo);
      auxiliar.updateMatrix();
      collar.setMatrixAt(k, auxiliar.matrix);
    }
    piso.add(collar);
  }
  if (d.flores > 0) {
    const geometriaFlor = geometriaRosa('#e86a92');
    const petalos = materialRosa();
    const escala = Math.max(0.5, Math.min(1.1, (Math.PI * 2 * medio) / d.flores / 3.2));
    for (let k = 0; k < d.flores; k++) {
      // Las flores se recorren medio lugar para no quedar sobre las rosetas
      const angulo = (k / d.flores) * Math.PI * 2 + Math.PI / 2 + Math.PI / d.flores;
      const flor = new THREE.Mesh(geometriaFlor, petalos);
      flor.scale.setScalar(escala);
      flor.position.set(Math.cos(angulo) * medio, arribaAnillo + 0.1, Math.sin(angulo) * medio);
      flor.rotation.x = -Math.PI / 2;
      flor.rotateZ(angulo);
      piso.add(flor);
    }
  }
  if (d.fresas > 0) {
    piso.add(fresas(d.fresas, arribaAnillo - 0.1, medio, true));
  }
  if (d.velas > 0) {
    piso.add(velas(d.velas, medio, arribaAnillo - 0.1, llamas, true));
  }
  let alturaTopper = 0;
  if (diseno.topper !== '') {
    const ancho = Math.min(16, diseno.diametro * 0.7);
    const letrero = topper(diseno.topper, ancho);
    letrero.position.set(0, arribaAnillo - 0.2, -medio);
    piso.add(letrero);
    alturaTopper = altoTopper(ancho, diseno.topper);
  }
  if (diseno.dedicatoria !== '') {
    // El mensaje va en una placa de chocolate blanco al frente del anillo
    const placa = placaMensaje(diseno, Math.min(12, anchoAnillo * 1.6));
    placa.position.set(0, arribaAnillo + 0.15, medio);
    placa.rotation.x = 0.1;
    piso.add(placa);
  }
  conSombras(raiz);
  let alturaVelas = 0;
  if (d.velas > 0) {
    alturaVelas = 8.5;
  }
  return { grupo: raiz, alturaTotal: plato.arriba + arribaAnillo + Math.max(alturaVelas, alturaTopper), anchoTotal: diseno.diametro + 8, llamas: llamas };
}

// Chispas sobre un anillo (entre "radioInterior" y "radioExterior")
function chispasEnAnillo(radioInterior: number, radioExterior: number, altura: number, cantidad: number, semilla: number): THREE.InstancedMesh {
  const malla = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.11, 0.42, 2, 6), material('#ffffff', 0.45, 0), cantidad);
  const auxiliar = new THREE.Object3D();
  const color = new THREE.Color();
  const azar = new Aleatorio(semilla);
  const parteLibre = (radioInterior * radioInterior) / (radioExterior * radioExterior);
  for (let i = 0; i < cantidad; i++) {
    const angulo = azar.siguiente() * Math.PI * 2;
    const distancia = Math.sqrt(parteLibre + azar.siguiente() * (1 - parteLibre)) * radioExterior;
    auxiliar.position.set(Math.cos(angulo) * distancia, altura + 0.1, Math.sin(angulo) * distancia);
    auxiliar.rotation.set(0, azar.siguiente() * Math.PI * 2, Math.PI / 2, 'YXZ');
    auxiliar.updateMatrix();
    malla.setMatrixAt(i, auxiliar.matrix);
    malla.setColorAt(i, color.set(COLORES_CHISPAS[i % COLORES_CHISPAS.length]));
  }
  return malla;
}

// ===================== Macaron =====================

// Una tapa del macaron: domo liso con el "pie" rugoso abajo. Sale apoyada en y = 0.
function tapaMacaron(semilla: number): THREE.BufferGeometry {
  const perfil = [
    new THREE.Vector2(0, 0),
    new THREE.Vector2(1.9, 0),
    new THREE.Vector2(2.12, 0.08),
    new THREE.Vector2(2.2, 0.22),
    new THREE.Vector2(2.18, 0.38),
    new THREE.Vector2(2.2, 0.5),
    new THREE.Vector2(2.12, 0.8),
    new THREE.Vector2(1.8, 1.12),
    new THREE.Vector2(1.1, 1.32),
    new THREE.Vector2(0, 1.38),
  ];
  const geometria = new THREE.LatheGeometry(perfil, 56);
  const posiciones = geometria.attributes['position'];
  const azar = new Aleatorio(semilla);
  const ruido: number[] = [];
  for (let k = 0; k < 57; k++) {
    ruido.push(azar.siguiente());
  }
  for (let i = 0; i < posiciones.count; i++) {
    const y = posiciones.getY(i);
    if (y > 0.04 && y < 0.6) {
      // El pie es rugoso: cada lugar sale un poco distinto
      const x = posiciones.getX(i);
      const z = posiciones.getZ(i);
      const lugar = Math.round(((Math.atan2(z, x) + Math.PI) / (Math.PI * 2)) * 56);
      const factor = 1 + (ruido[lugar] - 0.5) * 0.07;
      posiciones.setX(i, x * factor);
      posiciones.setZ(i, z * factor);
    }
  }
  geometria.computeVertexNormals();
  return geometria;
}

// Un macaron: dos tapas del color del sabor y el relleno en medio
function macaron(diseno: DisenoPastel, indice: number): { grupo: THREE.Group; tope: Tope } {
  const grupo = new THREE.Group();
  const cascara = new THREE.MeshPhysicalMaterial({ color: diseno.colorBizcocho, roughness: 0.5, sheen: 0.6, sheenRoughness: 0.6, sheenColor: new THREE.Color('#ffffff') });
  const abajo = new THREE.Mesh(tapaMacaron(indice * 3 + 1), cascara);
  // La tapa de abajo va volteada (el domo hacia abajo)
  abajo.rotation.x = Math.PI;
  abajo.position.y = 1.38;
  grupo.add(abajo);
  const relleno = new THREE.Mesh(new THREE.CylinderGeometry(1.95, 1.95, 0.6, 40), new THREE.MeshStandardMaterial({ color: diseno.colorRelleno, roughness: 0.55 }));
  relleno.position.y = 1.38 + 0.3;
  grupo.add(relleno);
  const arriba = new THREE.Mesh(tapaMacaron(indice * 3 + 2), cascara);
  arriba.position.y = 1.38 + 0.6;
  grupo.add(arriba);
  const tope: Tope = { altura: 1.38 + 0.6 + 1.38, radio: 1.5, caida: 0.15 };
  if (diseno.cobertura === 'chispas') {
    grupo.add(chispasSobre(tope, 22, 170 + indice));
  }
  return { grupo: grupo, tope: tope };
}

// ===================== Entrada =====================

/**
 * RF-5 · Paso 4: arma un postre que no es pastel ni gelatina (pay, cheesecake, cupcake, rosca, galleta,
 * brownie, dona o macaron).
 * "verDentro" solo aplica al pay (los demás se ven completos).
 */
export function construirOtroPostre(diseno: DisenoPastel, verDentro: boolean): PastelConstruido {
  if (diseno.tipo === 'pay') {
    return pay(diseno, verDentro);
  }
  if (diseno.tipo === 'cheesecake') {
    return cheesecake(diseno, verDentro);
  }
  if (diseno.tipo === 'cupcake') {
    return cupcakeSolo(diseno);
  }
  if (diseno.tipo === 'rosca') {
    return rosca(diseno);
  }
  return piezaSola(diseno);
}

// RF-5 · Excepción paso 7: ¿este postre se puede ver por dentro? (pastel, gelatina, pay y cheesecake sí; los demás no)
export function permiteVerDentro(diseno: DisenoPastel): boolean {
  return diseno.tipo === 'pastel' || diseno.tipo === 'gelatina' || diseno.tipo === 'pay' || diseno.tipo === 'cheesecake';
}
