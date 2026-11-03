import { AfterViewInit, Component, ElementRef, OnDestroy, effect, input, signal, viewChild } from '@angular/core';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { PastelConstruido, construirPastel, liberarPastel } from '../constructor-pastel';
import { DisenoPastel } from '../diseno-pastel';

/**
 * Visor del pastel 3D (RF-3 · Paso 4): se vuelve a armar cada vez que el cliente cambia una opción.
 * El cliente puede girarlo con el mouse o el dedo, acercarlo, ver el corte por dentro y guardar una imagen.
 * Si el navegador no tiene WebGL, se muestra la foto de la plantilla (la página sigue funcionando).
 */
@Component({
  selector: 'app-visor-pastel3d',
  templateUrl: './visor-pastel3d.html',
  styleUrl: './visor-pastel3d.css',
})
export class VisorPastel3d implements AfterViewInit, OnDestroy {
  readonly diseno = input.required<DisenoPastel>();
  readonly nombre = input('postre');

  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');

  protected readonly sinWebgl = signal(false);
  protected readonly girando = signal(true);
  protected readonly verDentro = signal(false);

  private renderizador: THREE.WebGLRenderer | null = null;
  private escena = new THREE.Scene();
  private camara = new THREE.PerspectiveCamera(32, 1, 1, 600);
  private controles: OrbitControls | null = null;
  private pastel: PastelConstruido | null = null;
  private observador: ResizeObserver | null = null;
  private observadorVista: IntersectionObserver | null = null;
  // Si el visor no está en pantalla no se dibuja (ahorra batería y procesador)
  private visible = true;
  private cuadro = 0;
  // Texto del último diseño dibujado (si no cambia, no se vuelve a armar)
  private ultimoDiseno = '';
  // Animación de "aparecer" al cambiar una opción
  private inicioAparecer = 0;
  // Último tamaño del lienzo (para no recalcular si no cambió)
  private ultimoAncho = 0;
  private ultimoAlto = 0;

  constructor() {
    // effect() necesita una función; bind(this) evita escribir una flecha
    effect(this.alCambiarDiseno.bind(this));
  }

  ngAfterViewInit(): void {
    try {
      this.iniciarEscena();
    } catch {
      // RF-3 · Excepción: sin WebGL se muestra la foto; la personalización funciona igual
      this.sinWebgl.set(true);
      return;
    }
    this.armar();
    this.cuadro = requestAnimationFrame(this.dibujar.bind(this));
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.cuadro);
    if (this.observador !== null) {
      this.observador.disconnect();
    }
    if (this.observadorVista !== null) {
      this.observadorVista.disconnect();
    }
    if (this.pastel !== null) {
      liberarPastel(this.pastel.grupo);
    }
    if (this.controles !== null) {
      this.controles.dispose();
    }
    if (this.renderizador !== null) {
      this.renderizador.dispose();
    }
  }

  // ===================== Escena =====================

  private iniciarEscena(): void {
    const caja = this.contenedor().nativeElement;
    const renderizador = new THREE.WebGLRenderer({ antialias: true, alpha: true, preserveDrawingBuffer: true });
    renderizador.setPixelRatio(this.densidadPixeles());
    renderizador.shadowMap.enabled = true;
    renderizador.shadowMap.type = THREE.PCFShadowMap;
    renderizador.toneMapping = THREE.ACESFilmicToneMapping;
    renderizador.toneMappingExposure = 1.05;
    // «Ver por dentro» recorta las decoraciones con planos (ver constructor-pastel.ts)
    renderizador.localClippingEnabled = true;
    renderizador.domElement.setAttribute('aria-hidden', 'true');
    // El lienzo se crea desde código, así que el CSS del componente no lo alcanza:
    // sus estilos se ponen aquí. Va encima de todo el contenedor y nunca lo empuja.
    const estilo = renderizador.domElement.style;
    estilo.position = 'absolute';
    estilo.top = '0';
    estilo.left = '0';
    estilo.width = '100%';
    estilo.height = '100%';
    estilo.display = 'block';
    // Se pone primero para que los letreros queden encima
    caja.prepend(renderizador.domElement);
    this.renderizador = renderizador;

    // Luz de ambiente de "cuarto" para reflejos suaves
    const generador = new THREE.PMREMGenerator(renderizador);
    this.escena.environment = generador.fromScene(new RoomEnvironment(), 0.04).texture;
    this.escena.environmentIntensity = 0.55;
    generador.dispose();

    const solar = new THREE.DirectionalLight('#fff4e6', 2.2);
    solar.position.set(30, 60, 40);
    solar.castShadow = true;
    solar.shadow.mapSize.set(1024, 1024);
    solar.shadow.camera.left = -40;
    solar.shadow.camera.right = 40;
    solar.shadow.camera.top = 40;
    solar.shadow.camera.bottom = -40;
    solar.shadow.bias = -0.0005;
    this.escena.add(solar);
    this.escena.add(new THREE.HemisphereLight('#fff7ee', '#d8b89a', 0.6));

    // Piso que solo recibe la sombra
    const piso = new THREE.Mesh(new THREE.PlaneGeometry(300, 300), new THREE.ShadowMaterial({ opacity: 0.18 }));
    piso.rotation.x = -Math.PI / 2;
    piso.receiveShadow = true;
    this.escena.add(piso);

    const controles = new OrbitControls(this.camara, renderizador.domElement);
    controles.enableDamping = true;
    controles.enablePan = false;
    controles.autoRotate = true;
    controles.autoRotateSpeed = 1.4;
    controles.minPolarAngle = 0.25;
    controles.maxPolarAngle = Math.PI / 2 - 0.05;
    this.controles = controles;

    this.observador = new ResizeObserver(this.ajustarTamano.bind(this));
    this.observador.observe(caja);
    this.observadorVista = new IntersectionObserver(this.alCambiarVisibilidad.bind(this));
    this.observadorVista.observe(caja);
    this.ajustarTamano();
  }

  private alCambiarVisibilidad(entradas: IntersectionObserverEntry[]): void {
    const ultima = entradas[entradas.length - 1];
    this.visible = ultima.isIntersecting;
  }

  // Pantallas de alta densidad: hasta 1.5 se ve nítido sin cargar de más la tarjeta de video
  private densidadPixeles(): number {
    return Math.min(window.devicePixelRatio, 1.5);
  }

  // Se llama cada vez que cambia el tamaño del contenedor (ventana, zoom o giro del celular)
  private ajustarTamano(): void {
    if (this.renderizador === null) {
      return;
    }
    const caja = this.contenedor().nativeElement;
    const ancho = Math.max(1, Math.round(caja.clientWidth));
    const alto = Math.max(1, Math.round(caja.clientHeight));
    // El zoom de Windows o del navegador cambia la densidad de pixeles
    this.renderizador.setPixelRatio(this.densidadPixeles());
    if (ancho === this.ultimoAncho && alto === this.ultimoAlto) {
      return;
    }
    this.ultimoAncho = ancho;
    this.ultimoAlto = alto;
    // false: el tamaño en pantalla lo pone el CSS, aquí solo la resolución
    this.renderizador.setSize(ancho, alto, false);
    this.camara.aspect = ancho / alto;
    this.camara.updateProjectionMatrix();
    // Con otra proporción se vuelve a calcular la distancia para que el postre quepa
    this.acercarParaVerTodo();
  }

  // ===================== Armado =====================

  private alCambiarDiseno(): void {
    const diseno = this.diseno();
    const verDentro = this.verDentro();
    const texto = JSON.stringify(diseno) + verDentro;
    if (texto === this.ultimoDiseno) {
      return;
    }
    if (this.renderizador !== null) {
      this.armar();
    }
  }

  private armar(): void {
    const diseno = this.diseno();
    const verDentro = this.verDentro();
    const anterior = this.pastel;
    let cambioTamano = true;
    if (anterior !== null) {
      this.escena.remove(anterior.grupo);
      liberarPastel(anterior.grupo);
    }
    const nuevo = construirPastel(diseno, verDentro);
    if (anterior !== null) {
      cambioTamano = Math.abs(anterior.alturaTotal - nuevo.alturaTotal) > 0.5 || Math.abs(anterior.anchoTotal - nuevo.anchoTotal) > 0.5;
    }
    this.pastel = nuevo;
    this.escena.add(nuevo.grupo);
    this.ultimoDiseno = JSON.stringify(diseno) + verDentro;
    this.inicioAparecer = performance.now();
    if (cambioTamano) {
      this.encuadrar();
    }
  }

  // Coloca la cámara de frente, un poco desde arriba, apuntando al centro del postre
  private encuadrar(): void {
    if (this.pastel === null || this.controles === null) {
      return;
    }
    const centro = this.medirPastel().getCenter(new THREE.Vector3());
    this.controles.target.copy(centro);
    // Dirección desde el centro hacia la cámara (un poco desde arriba para que se lea el mensaje)
    const direccion = new THREE.Vector3(0, 0.78, 1).normalize();
    const distancia = this.distanciaParaVerTodo();
    this.camara.position.copy(centro).addScaledVector(direccion, distancia);
    this.controles.update();
  }

  // Deja la cámara en la misma dirección pero a la distancia en la que cabe todo el postre
  private acercarParaVerTodo(): void {
    if (this.pastel === null || this.controles === null) {
      return;
    }
    const direccion = this.camara.position.clone().sub(this.controles.target);
    if (direccion.lengthSq() === 0) {
      this.encuadrar();
      return;
    }
    direccion.normalize();
    this.camara.position.copy(this.controles.target).addScaledVector(direccion, this.distanciaParaVerTodo());
    this.controles.update();
  }

  // Caja que ocupa el postre a tamaño normal (sin la animación de "aparecer")
  private medirPastel(): THREE.Box3 {
    const caja = new THREE.Box3();
    if (this.pastel === null) {
      return caja;
    }
    const grupo = this.pastel.grupo;
    const escalaAnterior = grupo.scale.x;
    grupo.scale.setScalar(1);
    grupo.updateMatrixWorld(true);
    caja.setFromObject(grupo);
    grupo.scale.setScalar(escalaAnterior);
    grupo.updateMatrixWorld(true);
    return caja;
  }

  // Distancia de la cámara para que el postre quepa a lo ancho y a lo alto del lienzo
  private distanciaParaVerTodo(): number {
    const esfera = this.medirPastel().getBoundingSphere(new THREE.Sphere());
    // Radio mínimo fijo: así un pastel chico se ve chico y uno grande se ve grande
    const radio = Math.max(esfera.radius, 21);
    const mitadVertical = THREE.MathUtils.degToRad(this.camara.fov) / 2;
    // En un lienzo angosto (celular) lo que limita es el ancho
    const mitadHorizontal = Math.atan(Math.tan(mitadVertical) * this.camara.aspect);
    const mitadMenor = Math.min(mitadVertical, mitadHorizontal);
    // 1.08 deja un pequeño margen alrededor
    return (radio / Math.sin(mitadMenor)) * 1.08;
  }

  private dibujar(): void {
    this.cuadro = requestAnimationFrame(this.dibujar.bind(this));
    if (this.renderizador === null || this.controles === null || !this.visible) {
      return;
    }
    const tiempo = performance.now() / 1000;
    if (this.pastel !== null) {
      // Pequeño "salto" al cambiar una opción
      const avance = Math.min(1, (performance.now() - this.inicioAparecer) / 350);
      const escala = 0.94 + 0.06 * (1 - Math.pow(1 - avance, 3));
      this.pastel.grupo.scale.setScalar(escala);
      // Las llamas de las velas parpadean
      for (let i = 0; i < this.pastel.llamas.length; i++) {
        const llama = this.pastel.llamas[i];
        llama.scale.y = 2.1 + Math.sin(tiempo * 9 + i * 1.37) * 0.25;
        llama.scale.x = 1 + Math.sin(tiempo * 13 + i) * 0.08;
      }
    }
    this.controles.update();
    this.renderizador.render(this.escena, this.camara);
  }

  // Pisos que se muestran sobre el 3D (las gelatinas no llevan este texto)
  textoPisos(): string {
    const diseno = this.diseno();
    if (diseno.tipo !== 'pastel') {
      return '';
    }
    if (diseno.pisos === 1) {
      return '1 piso';
    }
    return diseno.pisos + ' pisos';
  }

  // ===================== Botones =====================

  alternarGiro(): void {
    this.girando.set(!this.girando());
    if (this.controles !== null) {
      this.controles.autoRotate = this.girando();
    }
  }

  alternarDentro(): void {
    this.verDentro.set(!this.verDentro());
    if (this.verDentro()) {
      // Para ver el corte se detiene el giro y se mira de frente
      this.girando.set(false);
      if (this.controles !== null) {
        this.controles.autoRotate = false;
      }
      this.encuadrar();
    }
  }

  centrar(): void {
    this.encuadrar();
  }

  // Descarga una imagen PNG de cómo se ve el postre
  guardarImagen(): void {
    if (this.renderizador === null) {
      return;
    }
    this.renderizador.render(this.escena, this.camara);
    const enlace = document.createElement('a');
    enlace.href = this.renderizador.domElement.toDataURL('image/png');
    enlace.download = 'mi-postre-carmir.png';
    enlace.click();
  }
}
