import { Component, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { AvisosService } from '../../../../core/servicios/avisos.service';
import { explicarFalla, tieneCodigo } from '../../../../core/utilidades/errores';
import { Cargando } from '../../../../shared/componentes/cargando/cargando';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { TarjetaProducto } from '../../../../shared/componentes/tarjeta-producto/tarjeta-producto';
import { Categoria, FiltrosCatalogo, OrdenCatalogo, ProductoCatalogo } from '../../modelos/catalogo.models';
import { CatalogoApiService } from '../../servicios/catalogo-api.service';
import { FiltrosCatalogoService } from '../../servicios/filtros-catalogo.service';

// Estados de la pantalla
type EstadoPantalla = 'cargando' | 'listo' | 'error';

const MENSAJE_CATEGORIA_NO_DISPONIBLE = 'La categoría seleccionada ya no está disponible';

/**
 * RF-1 · Visualización del catálogo de productos.
 * Es la página de inicio. Muestra las categorías y la cuadrícula de productos (plantillas base).
 * Los filtros viven en la dirección (?categoria=1&buscar=tres&orden=precio_asc)
 * para poder compartir el enlace y usar el botón "atrás" del navegador.
 */
@Component({
  selector: 'app-catalogo-pagina',
  imports: [TarjetaProducto, MensajeEstado, Cargando],
  templateUrl: './catalogo-pagina.html',
  styleUrl: './catalogo-pagina.css',
})
export default class CatalogoPagina implements OnInit, OnDestroy {
  private readonly api = inject(CatalogoApiService);
  private readonly ruta = inject(ActivatedRoute);
  // Los filtros se dibujan en el encabezado (menú de arriba); este servicio los comparte
  private readonly filtrosArriba = inject(FiltrosCatalogoService);
  private readonly avisos = inject(AvisosService);

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly productos = signal<ProductoCatalogo[]>([]);
  protected readonly estado = signal<EstadoPantalla>('cargando');
  protected readonly aviso = signal('');
  // Causa del error de carga (API apagada, base de datos, etc.)
  protected readonly detalleError = signal('');
  protected readonly filtros = signal<FiltrosCatalogo>({ idCategoria: 0, buscar: '', orden: 'nombre' });

  // Número de la última consulta: si llegan respuestas viejas, se ignoran
  private numeroConsulta = 0;

  constructor() {
    // Cada vez que cambian los filtros se pasan al encabezado (buscador de arriba).
    // effect() necesita una función; se usa bind(this) para no escribir una flecha.
    effect(this.compartirConEncabezado.bind(this));
  }

  private compartirConEncabezado(): void {
    this.filtrosArriba.filtros.set(this.filtros());
  }

  // RF-1 (Visualización del catálogo de productos): inicia aquí. Paso 1: el cliente entra al portal; el catálogo es la página de inicio.
  ngOnInit(): void {
    // Aviso que dejó otra pantalla (por ejemplo "Este producto ya no está disponible")
    this.aviso.set(this.avisos.tomarAviso());
    // Muestra los filtros en el menú de arriba mientras esta página está abierta
    this.filtrosArriba.visibles.set(true);
    this.cargarCategorias();
    // Cada vez que cambian los filtros de la dirección se vuelve a consultar.
    // bind(this) hace que dentro de alCambiarDireccion "this" siga siendo esta página.
    this.ruta.queryParamMap.subscribe(this.alCambiarDireccion.bind(this));
  }

  // Al salir del catálogo los filtros desaparecen del menú de arriba
  ngOnDestroy(): void {
    this.filtrosArriba.visibles.set(false);
  }

  // ===================== Lectura de filtros =====================

  private alCambiarDireccion(parametros: ParamMap): void {
    let idCategoria = Number(parametros.get('categoria'));
    if (!Number.isInteger(idCategoria) || idCategoria < 0) {
      idCategoria = 0;
    }

    let buscar = parametros.get('buscar');
    if (buscar === null) {
      buscar = '';
    }
    buscar = buscar.trim().substring(0, 60);

    let orden: OrdenCatalogo = 'nombre';
    const ordenTexto = parametros.get('orden');
    if (ordenTexto === 'precio_asc' || ordenTexto === 'precio_desc') {
      orden = ordenTexto;
    }

    this.filtros.set({ idCategoria: idCategoria, buscar: buscar, orden: orden });
    this.cargarProductos();
  }

  // Cambio de filtros desde esta página («Limpiar búsqueda»); el encabezado usa el mismo servicio
  // RF-1 · Paso 5: el cliente selecciona una categoría, escribe un criterio de búsqueda o cambia el orden.
  aplicarFiltros(nuevos: FiltrosCatalogo): void {
    this.aviso.set('');
    this.filtrosArriba.aplicar(nuevos);
  }

  // RF-1 · Paso 5: el cliente elige una categoría (botones junto al total de postres).
  elegirCategoria(idCategoria: number): void {
    const actuales = this.filtros();
    this.aplicarFiltros({ idCategoria: idCategoria, buscar: actuales.buscar, orden: actuales.orden });
  }

  // RF-1 · Excepción paso 6: ningún producto coincide; «Limpiar búsqueda» regresa a «Todos los postres».
  // "Limpiar búsqueda": regresa a "Todos los postres"
  limpiarFiltros(): void {
    this.aplicarFiltros({ idCategoria: 0, buscar: '', orden: this.filtros().orden });
  }

  // ===================== Consultas =====================

  // RF-1 · Paso 2: consulta las categorías activas.
  async cargarCategorias(): Promise<void> {
    try {
      this.categorias.set(await this.api.listarCategorias());
    } catch {
      // RF-1 · Excepción paso 2: sin conexión no hay categorías (el aviso sale al cargar los productos).
      // Si fallan, la cuadrícula mostrará el error de conexión al cargar los productos
      this.categorias.set([]);
    }
  }

  // RF-1 · Pasos 2 a 4 (y paso 6 al filtrar): consulta los productos activos y los muestra.
  async cargarProductos(): Promise<void> {
    this.numeroConsulta = this.numeroConsulta + 1;
    const miConsulta = this.numeroConsulta;
    this.estado.set('cargando');

    try {
      const productos = await this.api.listarProductos(this.filtros());
      if (miConsulta !== this.numeroConsulta) {
        return;
      }
      // RF-1 · Paso 4 / Paso 6: muestra la cuadrícula con los productos que coinciden.
      this.productos.set(productos);
      this.estado.set('listo');
    } catch (error) {
      if (miConsulta !== this.numeroConsulta) {
        return;
      }
      // RF-1 paso 6: la categoría ya no existe o se desactivó → aviso y "Todos los postres"
      if (tieneCodigo(error, 'CATEGORIA_NO_DISPONIBLE')) {
        this.cargarCategorias();
        this.aplicarFiltros({ idCategoria: 0, buscar: this.filtros().buscar, orden: this.filtros().orden });
        this.aviso.set(MENSAJE_CATEGORIA_NO_DISPONIBLE);
        return;
      }
      // RF-1 paso 2: sin conexión con el servidor o la base de datos
      this.productos.set([]);
      this.detalleError.set(explicarFalla(error));
      this.estado.set('error');
    }
  }

  // RF-1 · Excepción paso 2: botón «Reintentar» después de «No fue posible cargar el catálogo».
  // "Reintentar"
  reintentar(): void {
    this.cargarCategorias();
    this.cargarProductos();
  }

  // ===================== Apoyo para la vista =====================

  hayFiltros(): boolean {
    const filtros = this.filtros();
    return filtros.idCategoria > 0 || filtros.buscar !== '';
  }

  // Título de la sección según la categoría elegida
  tituloSeccion(): string {
    const idCategoria = this.filtros().idCategoria;
    for (const categoria of this.categorias()) {
      if (categoria.idCategoria === idCategoria) {
        return categoria.nombre;
      }
    }
    return 'Todos los postres';
  }

  textoTotal(): string {
    const total = this.productos().length;
    if (total === 1) {
      return '1 postre';
    }
    return total + ' postres';
  }

  // Lista fija para dibujar 6 tarjetas "esqueleto" mientras carga
  protected readonly esqueletos = [1, 2, 3, 4, 5, 6];
}
