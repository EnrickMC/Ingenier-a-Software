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
  private cuadro = 0;
  // Texto del último diseño dibujado (si no cambia, no se vuelve a armar)
  private ultimoDiseno = '';
  // Animación de "aparecer" al cambiar una opción
  private inicioAparecer = 0;

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
    renderizador.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderizador.shadowMap.enabled = true;
    renderizador.shadowMap.type = THREE.PCFShadowMap;
    renderizador.toneMapping = THREE.ACESFilmicToneMapping;
    renderizador.toneMappingExposure = 1.05;
    renderizador.domElement.setAttribute('aria-hidden', 'true');
    caja.appendChild(renderizador.domElement);
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
    this.ajustarTamano();
  }

  private ajustarTamano(): void {
    if (this.renderizador === null) {
      return;
    }
    const caja = this.contenedor().nativeElement;
    const ancho = Math.max(1, caja.clientWidth);
    const alto = Math.max(1, caja.clientHeight);
    this.renderizador.setSize(ancho, alto, false);
    this.camara.aspect = ancho / alto;
    this.camara.updateProjectionMatrix();
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

  // Coloca la cámara para que se vea todo el postre
  private encuadrar(): void {
    if (this.pastel === null || this.controles === null) {
      return;
    }
    const alto = this.pastel.alturaTotal;
    const ancho = this.pastel.anchoTotal;
    // Distancia mínima fija: así un pastel chico se ve chico y uno grande se ve grande
    const distancia = Math.max(alto * 2.1, ancho * 1.8, 72);
    this.controles.target.set(0, alto * 0.45, 0);
    // Un poco desde arriba para que se lea el mensaje
    this.camara.position.set(0, alto * 0.45 + distancia * 0.78, distancia);
    this.controles.update();
  }

  private dibujar(): void {
    this.cuadro = requestAnimationFrame(this.dibujar.bind(this));
    if (this.renderizador === null || this.controles === null) {
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
