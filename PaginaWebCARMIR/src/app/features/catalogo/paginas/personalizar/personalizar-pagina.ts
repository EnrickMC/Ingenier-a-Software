import { CurrencyPipe, Location } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { ArticuloCarrito, OpcionDelArticulo } from '../../../../core/modelos/articulo-carrito';
import { AvisosService } from '../../../../core/servicios/avisos.service';
import { CarritoService } from '../../../../core/servicios/carrito.service';
import { esSinConexion, explicarFalla, mensajeDeError, tieneCodigo } from '../../../../core/utilidades/errores';
import { Cargando } from '../../../../shared/componentes/cargando/cargando';
import { ImagenPostre } from '../../../../shared/componentes/imagen-postre/imagen-postre';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { GaleriaPlantillas } from '../../componentes/galeria-plantillas/galeria-plantillas';
import { SelectorCaracteristica } from '../../componentes/selector-caracteristica/selector-caracteristica';
import { DisenoPastel, disenoDesdeSeleccion, esGelatina } from '../../pastel3d/diseno-pastel';
import { VisorPastel3d } from '../../pastel3d/visor-pastel3d/visor-pastel3d';
import {
  CaracteristicaPersonalizable,
  Cotizacion,
  DatosPersonalizacion,
  ErrorConfiguracion,
  OpcionElegida,
  Plantilla,
} from '../../modelos/catalogo.models';
import { CatalogoApiService } from '../../servicios/catalogo-api.service';
import {
  CANTIDAD_MAXIMA,
  LARGO_COMENTARIO,
  LARGO_DEDICATORIA,
  buscarOpcion,
  calcularPrecio,
  puntosTotales,
  redondear,
  validarCantidad,
  validarConfiguracion,
  validarTexto,
} from '../../utilidades/reglas-configuracion';

type EstadoPantalla = 'cargando' | 'listo' | 'error';
// Resultado de «Agregar al carrito»
type TipoResultado = '' | 'error' | 'aviso' | 'exito';

const MENSAJE_PRODUCTO_NO_DISPONIBLE = 'Este producto ya no está disponible';

/**
 * RF-3 · Personalizar el postre a partir de su plantilla base, y
 * RF-4 · Elegir otra plantilla de diseño de la misma categoría.
 *
 * Flujo:
 *  1. Se cargan las características, opciones y exclusiones; la plantilla precarga sus opciones.
 *  2. Con cada cambio se recalcula el precio y los puntos disponibles (en el navegador).
 *  3. «Agregar al carrito» valida aquí, luego la API valida otra vez y confirma el precio vigente.
 *
 * Módulo 2 (RF-7): con ?editar=<id del renglón> la página abre un postre que ya está en el carrito
 * con sus opciones, cantidad y mensaje; el botón cambia a «Guardar cambios» y reemplaza ese renglón.
 */
@Component({
  selector: 'app-personalizar-pagina',
  imports: [RouterLink, CurrencyPipe, ImagenPostre, MensajeEstado, Cargando, SelectorCaracteristica, GaleriaPlantillas, VisorPastel3d],
  templateUrl: './personalizar-pagina.html',
  styleUrl: './personalizar-pagina.css',
})
export default class PersonalizarPagina implements OnInit {
  private readonly api = inject(CatalogoApiService);
  private readonly carrito = inject(CarritoService);
  private readonly avisos = inject(AvisosService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  // Límites para la vista
  protected readonly cantidadMaxima = CANTIDAD_MAXIMA;
  protected readonly largoDedicatoria = LARGO_DEDICATORIA;
  protected readonly largoComentario = LARGO_COMENTARIO;

  // Datos de la API
  protected readonly datos = signal<DatosPersonalizacion | null>(null);
  protected readonly estado = signal<EstadoPantalla>('cargando');
  protected readonly detalleError = signal('');

  // Lo que va eligiendo el cliente
  protected readonly seleccion = signal<OpcionElegida[]>([]);
  protected readonly cantidad = signal(1);
  protected readonly dedicatoria = signal('');
  protected readonly comentario = signal('');

  // Errores y resultado
  protected readonly erroresConfiguracion = signal<ErrorConfiguracion[]>([]);
  protected readonly errorCantidad = signal('');
  protected readonly errorDedicatoria = signal('');
  protected readonly errorComentario = signal('');
  protected readonly mensajeResultado = signal('');
  protected readonly tipoResultado = signal<TipoResultado>('');
  protected readonly enviando = signal(false);
  // Después del primer intento de agregar, los errores se actualizan con cada cambio
  private intentoAgregar = false;

  // Galería de plantillas (RF-4)
  protected readonly galeriaAbierta = signal(false);
  protected readonly cargandoGaleria = signal(false);
  protected readonly aplicandoPlantilla = signal(false);
  protected readonly plantillas = signal<Plantilla[]>([]);
  protected readonly mensajeGaleria = signal('');
  protected readonly mensajeDentroGaleria = signal('');

  // Producto con el que se entró (para «Regresar» a su ficha)
  private idProductoOriginal = 0;

  // RF-7: renglón del carrito que se está editando (null = postre nuevo)
  protected readonly editando = signal<ArticuloCarrito | null>(null);

  // RF-3 · Paso 4: diseño del pastel 3D. computed() solo lo recalcula cuando cambia la selección o el mensaje.
  protected readonly diseno3d = computed(this.calcularDiseno3d.bind(this));

  // RF-3 (Registro de pedido personalizado): inicia aquí. Paso 1: el cliente presionó «Personalizar pedido» en la ficha.
  ngOnInit(): void {
    // bind(this): dentro de alCambiarRuta "this" sigue siendo esta página
    this.ruta.paramMap.subscribe(this.alCambiarRuta.bind(this));
  }

  private alCambiarRuta(parametros: ParamMap): void {
    this.idProductoOriginal = Number(parametros.get('id'));
    this.buscarRenglonEditado();
    this.cargar();
  }

  // RF-7 · Pasos 1 y 2: el cliente pidió modificar un postre del carrito; se busca el renglón.
  // RF-7: si la dirección trae ?editar=id y ese postre sigue en el carrito, se edita
  private buscarRenglonEditado(): void {
    this.editando.set(null);
    const id = this.ruta.snapshot.queryParamMap.get('editar');
    if (id === null || id === '') {
      return;
    }
    const articulo = this.carrito.buscar(id);
    // RF-7 · Excepción paso 2: el producto ya no está en el carrito.
    if (articulo === null || articulo.idPostre !== this.idProductoOriginal) {
      this.mostrarResultado('aviso', 'Este producto ya no está en tu carrito. Puedes agregarlo como nuevo.');
      return;
    }
    this.editando.set(articulo);
  }

  // ===================== Carga inicial (RF-3 pasos 2 y 3) =====================

  async cargar(): Promise<void> {
    // RF-3 · Excepción paso 2: el id no es válido, se trata como producto no disponible.
    if (!Number.isInteger(this.idProductoOriginal) || this.idProductoOriginal <= 0) {
      this.regresarAlCatalogo();
      return;
    }
    this.estado.set('cargando');
    try {
      // RF-3 · Paso 2: valida que el producto siga activo y trae características, opciones y exclusiones.
      const datos = await this.api.obtenerPersonalizacion(this.idProductoOriginal);
      this.usarDatos(datos);
      this.precargarRenglonEditado();
      // RF-3 · Paso 3: despliega el formulario de personalización.
      this.estado.set('listo');
    } catch (error) {
      // RF-3 · Excepción paso 2: el producto ya no existe o no está activo, «Este producto ya no está disponible» y regresa al catálogo.
      if (tieneCodigo(error, 'PRODUCTO_NO_DISPONIBLE') || tieneCodigo(error, 'DATOS_INVALIDOS')) {
        this.regresarAlCatalogo();
        return;
      }
      // RF-3 · Excepción paso 2: sin conexión, «No fue posible cargar las opciones de personalización».
      this.detalleError.set(explicarFalla(error));
      this.estado.set('error');
    }
  }

  // Guarda los datos de una plantilla y precarga sus opciones
  private usarDatos(datos: DatosPersonalizacion): void {
    this.datos.set(datos);
    this.seleccion.set(datos.seleccionInicial.slice());
    this.erroresConfiguracion.set([]);
    this.intentoAgregar = false;
  }

  // Pone en la pantalla lo que el postre del carrito ya tenía (solo opciones que siguen existiendo)
  // RF-7 · Paso 3: muestra el formulario con la configuración actual del postre.
  private precargarRenglonEditado(): void {
    const articulo = this.editando();
    if (articulo === null) {
      return;
    }
    const seleccion: OpcionElegida[] = [];
    let faltan = false;
    for (const opcion of articulo.opciones) {
      if (buscarOpcion(this.caracteristicas(), opcion.idOpcion) !== null) {
        seleccion.push({ idOpcion: opcion.idOpcion, cantidad: opcion.cantidad });
      } else {
        faltan = true;
      }
    }
    this.seleccion.set(seleccion);
    this.cantidad.set(articulo.cantidad);
    this.dedicatoria.set(articulo.dedicatoria);
    this.comentario.set(articulo.comentario);
    // Si algo ya no existe se marcan de una vez los errores para que el cliente lo corrija
    // RF-7 · Excepción paso 6: opciones que ya no están disponibles; se marcan para que el cliente las cambie.
    if (faltan) {
      this.intentoAgregar = true;
      this.validarConfiguracionActual();
      this.mostrarResultado('aviso', 'Algunas opciones de tu postre ya no están disponibles. Elige otras y guarda los cambios.');
    }
  }

  private regresarAlCatalogo(): void {
    this.avisos.dejarAviso(MENSAJE_PRODUCTO_NO_DISPONIBLE);
    this.router.navigate(['/']);
  }

  // ===================== Datos para la vista =====================

  caracteristicas(): CaracteristicaPersonalizable[] {
    const datos = this.datos();
    if (datos === null) {
      return [];
    }
    return datos.caracteristicas;
  }

  // ¿El postre es una gelatina? (el 3D y los colores de sabor cambian)
  esGelatina(): boolean {
    const datos = this.datos();
    if (datos === null) {
      return false;
    }
    return esGelatina(datos.producto.nombreCategoria, datos.producto.nombre);
  }

  // RF-3 · Paso 4: el modelo 3D refleja lo elegido (no cambia el precio ni las reglas).
  private calcularDiseno3d(): DisenoPastel {
    const datos = this.datos();
    let categoria = '';
    let nombre = '';
    if (datos !== null) {
      categoria = datos.producto.nombreCategoria;
      nombre = datos.producto.nombre;
    }
    return disenoDesdeSeleccion(this.caracteristicas(), this.seleccion(), categoria, nombre, this.dedicatoria());
  }

  // RF-3 · Paso 4: el precio se recalcula con cada cambio de la selección.
  precioUnitario(): number {
    return calcularPrecio(this.caracteristicas(), this.seleccion());
  }

  total(): number {
    return redondear(this.precioUnitario() * this.cantidad());
  }

  // RF-3 · Paso 4: puntos disponibles de cada característica.
  puntosDe(caracteristica: CaracteristicaPersonalizable): number {
    return puntosTotales(this.caracteristicas(), caracteristica, this.seleccion());
  }

  // Error de una característica en particular (para marcarla en rojo)
  errorDe(caracteristica: CaracteristicaPersonalizable): string {
    for (const error of this.erroresConfiguracion()) {
      if (error.idCaracteristica === caracteristica.idCaracteristica) {
        return error.mensaje;
      }
    }
    return '';
  }

  // Errores que no son de una sola característica (exclusiones, disponibilidad)
  erroresGenerales(): ErrorConfiguracion[] {
    const generales: ErrorConfiguracion[] = [];
    for (const error of this.erroresConfiguracion()) {
      if (error.idCaracteristica === 0) {
        generales.push(error);
      }
    }
    return generales;
  }

  // ===================== Cambios del cliente (RF-3 paso 4) =====================

  // RF-3 · Paso 4: el cliente selecciona las opciones deseadas.
  // Selección única: se quita lo que había en la característica y se pone la nueva opción
  elegirUnica(caracteristica: CaracteristicaPersonalizable, idOpcion: number): void {
    const nueva: OpcionElegida[] = [];
    for (const elegida of this.seleccion()) {
      const opcion = buscarOpcion(this.caracteristicas(), elegida.idOpcion);
      if (opcion === null || opcion.idCaracteristica !== caracteristica.idCaracteristica) {
        nueva.push(elegida);
      }
    }
    if (idOpcion > 0) {
      nueva.push({ idOpcion: idOpcion, cantidad: 1 });
    }
    this.cambiarSeleccion(nueva);
  }

  // Selección múltiple: cambia la cantidad de una opción (0 = se quita)
  cambiarCantidadOpcion(cambio: OpcionElegida): void {
    const nueva: OpcionElegida[] = [];
    let encontrada = false;
    for (const elegida of this.seleccion()) {
      if (elegida.idOpcion === cambio.idOpcion) {
        encontrada = true;
        if (cambio.cantidad > 0) {
          nueva.push({ idOpcion: cambio.idOpcion, cantidad: cambio.cantidad });
        }
      } else {
        nueva.push(elegida);
      }
    }
    if (!encontrada && cambio.cantidad > 0) {
      nueva.push({ idOpcion: cambio.idOpcion, cantidad: cambio.cantidad });
    }
    this.cambiarSeleccion(nueva);
  }

  private cambiarSeleccion(nueva: OpcionElegida[]): void {
    this.seleccion.set(nueva);
    this.limpiarResultado();
    if (this.intentoAgregar) {
      this.validarConfiguracionActual();
    }
  }

  cambiarCantidad(texto: string): void {
    this.cantidad.set(Number(texto));
    this.limpiarResultado();
    if (this.intentoAgregar) {
      this.errorCantidad.set(validarCantidad(this.cantidad()));
    }
  }

  sumarPieza(): void {
    if (this.cantidad() < CANTIDAD_MAXIMA) {
      this.cambiarCantidad(String(this.cantidad() + 1));
    }
  }

  restarPieza(): void {
    if (this.cantidad() > 1) {
      this.cambiarCantidad(String(this.cantidad() - 1));
    }
  }

  // RF-3 · Excepción paso 6: la dedicatoria se revisa al escribir (largo y caracteres permitidos).
  cambiarDedicatoria(texto: string): void {
    this.dedicatoria.set(texto);
    this.errorDedicatoria.set(validarTexto(texto, 'La dedicatoria', LARGO_DEDICATORIA));
  }

  cambiarComentario(texto: string): void {
    this.comentario.set(texto);
    this.errorComentario.set(validarTexto(texto, 'El comentario', LARGO_COMENTARIO));
  }

  private limpiarResultado(): void {
    if (this.tipoResultado() !== 'aviso') {
      this.mensajeResultado.set('');
      this.tipoResultado.set('');
    }
  }

  // ===================== Validaciones (RF-3 paso 6) =====================

  private validarConfiguracionActual(): boolean {
    const datos = this.datos();
    if (datos === null) {
      return false;
    }
    // RF-3 · Excepción paso 6: obligatorias sin opción, presupuesto de puntos excedido u opciones excluidas.
    const errores = validarConfiguracion(datos.caracteristicas, datos.exclusiones, this.seleccion());
    this.erroresConfiguracion.set(errores);
    return errores.length === 0;
  }

  private validarTodo(): boolean {
    const configuracionValida = this.validarConfiguracionActual();
    // RF-3 · Excepción paso 6: cantidad fuera de rango o dedicatoria inválida; se marca el campo y se conserva lo demás.
    this.errorCantidad.set(validarCantidad(this.cantidad()));
    this.errorDedicatoria.set(validarTexto(this.dedicatoria(), 'La dedicatoria', LARGO_DEDICATORIA));
    this.errorComentario.set(validarTexto(this.comentario(), 'El comentario', LARGO_COMENTARIO));
    return (
      configuracionValida &&
      this.errorCantidad() === '' &&
      this.errorDedicatoria() === '' &&
      this.errorComentario() === ''
    );
  }

  // ===================== Agregar al carrito (RF-3 pasos 5 a 8) =====================

  // El formulario no recarga la página: solo llama a agregarAlCarrito
  alEnviar(evento: Event): void {
    evento.preventDefault();
    this.agregarAlCarrito();
  }

  // RF-3 · Paso 5: el cliente eligió «Agregar al carrito» (también RF-6 y RF-7 al guardar cambios).
  async agregarAlCarrito(): Promise<void> {
    const datos = this.datos();
    if (datos === null || this.enviando()) {
      return;
    }
    this.intentoAgregar = true;
    this.mensajeResultado.set('');
    this.tipoResultado.set('');

    // Paso 6: validación en el navegador (resalta los campos y conserva lo demás)
    if (!this.validarTodo()) {
      // RF-3 · Excepción paso 6: resalta los campos con error y regresa al paso 4.
      this.mostrarResultado('error', 'Revisa los campos marcados en rojo.');
      return;
    }

    // Paso 7: la API valida contra la base de datos y confirma el precio vigente
    this.enviando.set(true);
    try {
      const cotizacion = await this.api.cotizar({
        idPostre: datos.producto.idPostre,
        opciones: this.seleccion(),
        cantidad: this.cantidad(),
        dedicatoria: this.dedicatoria().trim(),
        comentario: this.comentario().trim(),
        precioMostrado: this.precioUnitario(),
      });
      await this.revisarCotizacion(cotizacion);
    } catch (error) {
      // RF-3 · Excepción paso 7: el producto ya no está disponible al validar.
      if (tieneCodigo(error, 'PRODUCTO_NO_DISPONIBLE')) {
        this.regresarAlCatalogo();
        return;
      }
      // RF-3 · Excepción paso 7: sin conexión con la API al validar.
      let mensaje = mensajeDeError(error, 'No fue posible validar tu postre. Revisa tu conexión e inténtalo de nuevo.');
      if (esSinConexion(error)) {
        mensaje = 'No fue posible validar tu postre. Revisa tu conexión e inténtalo de nuevo.';
      }
      this.mostrarResultado('error', mensaje);
    } finally {
      this.enviando.set(false);
    }
  }

  private async revisarCotizacion(cotizacion: Cotizacion): Promise<void> {
    // RF-3 · Excepción paso 7: «La opción seleccionada ya no está disponible», la retira y recalcula el precio.
    // Una o más opciones ya no están disponibles: se retiran y se recalcula el precio
    if (cotizacion.opcionesNoDisponibles.length > 0) {
      const restantes: OpcionElegida[] = [];
      for (const elegida of this.seleccion()) {
        if (cotizacion.opcionesNoDisponibles.indexOf(elegida.idOpcion) < 0) {
          restantes.push(elegida);
        }
      }
      await this.recargarReglas(restantes);
      this.mostrarResultado('error', 'La opción seleccionada ya no está disponible. La quitamos y recalculamos el precio.');
      return;
    }

    // RF-3 · Excepción paso 7: opciones que se excluyen entre sí o presupuesto excedido (motivo que manda la API).
    // Reglas que no se cumplen (presupuesto, exclusiones...)
    if (!cotizacion.valido) {
      this.erroresConfiguracion.set(cotizacion.errores);
      this.mostrarResultado('error', 'Revisa los campos marcados en rojo.');
      return;
    }

    // RF-3 · Excepción paso 7: el precio no es el vigente, se actualiza en pantalla y se avisa (regresa al paso 5).
    // El precio cambió desde que se cargó la página: se actualiza y el cliente confirma otra vez
    if (cotizacion.precioCambio) {
      await this.recargarReglas(this.seleccion());
      this.mostrarResultado(
        'aviso',
        'El precio de tu postre se actualizó. Revisa el nuevo total y vuelve a presionar «Agregar al carrito».',
      );
      return;
    }

    // RF-3 · Paso 8: registra la configuración con su precio y la agrega al carrito (sigue RF-6).
    // Paso 8: todo bien, se agrega al carrito
    const datos = this.datos();
    if (datos === null) {
      return;
    }
    const nuevo = {
      idPostre: datos.producto.idPostre,
      nombre: datos.producto.nombre,
      linkFotografia: datos.producto.linkFotografia,
      opciones: this.opcionesParaCarrito(),
      cantidad: this.cantidad(),
      dedicatoria: this.dedicatoria().trim(),
      comentario: this.comentario().trim(),
      precioUnitario: cotizacion.precioUnitario,
    };

    // RF-7 · Pasos 7 y 8: guarda el postre editado y regresa al carrito con el aviso de éxito.
    // RF-7: se reemplaza el renglón que se estaba editando y se regresa al carrito
    const editado = this.editando();
    if (editado !== null) {
      // RF-7 · Excepción paso 2: el postre ya no estaba en el carrito; se agrega como nuevo.
      if (this.carrito.reemplazar(editado.id, nuevo)) {
        this.avisos.dejarAviso('Actualizamos tu postre «' + nuevo.nombre + '».');
      } else {
        this.carrito.agregar(nuevo);
        this.avisos.dejarAviso('Ese producto ya no estaba en tu carrito, así que lo agregamos como nuevo.');
      }
      this.router.navigate(['/carrito']);
      return;
    }

    // RF-6 · Pasos 3 a 7: agrega al carrito (carrito.service.ts) y muestra «Ver carrito» y «Seguir comprando».
    const resultado = this.carrito.agregar(nuevo);
    let mensaje = '¡Listo! Agregamos tu postre al carrito.';
    if (resultado.juntado) {
      mensaje = '¡Listo! Ya tenías este mismo postre en tu carrito: sumamos las piezas.';
    }
    // RF-6 · Excepción paso 5: la cantidad excedía el máximo por producto y se ajustó.
    if (resultado.cantidadAjustada) {
      mensaje = mensaje + ' Solo se permiten ' + CANTIDAD_MAXIMA + ' piezas por producto, así que ajustamos la cantidad.';
    }
    this.mostrarResultado('exito', mensaje);
  }

  // Vuelve a pedir características y precios a la API conservando la selección indicada
  private async recargarReglas(seleccion: OpcionElegida[]): Promise<void> {
    const datos = this.datos();
    if (datos === null) {
      return;
    }
    try {
      const nuevos = await this.api.obtenerPersonalizacion(datos.producto.idPostre);
      this.datos.set(nuevos);
    } catch {
      // Si no se pudo recargar se conservan los datos anteriores
    }
    // Solo se conservan las opciones que siguen existiendo
    const conservadas: OpcionElegida[] = [];
    for (const elegida of seleccion) {
      if (buscarOpcion(this.caracteristicas(), elegida.idOpcion) !== null) {
        conservadas.push(elegida);
      }
    }
    this.seleccion.set(conservadas);
    this.validarConfiguracionActual();
  }

  private opcionesParaCarrito(): OpcionDelArticulo[] {
    const opciones: OpcionDelArticulo[] = [];
    for (const elegida of this.seleccion()) {
      const opcion = buscarOpcion(this.caracteristicas(), elegida.idOpcion);
      if (opcion !== null) {
        opciones.push({ idOpcion: elegida.idOpcion, nombre: opcion.nombre, cantidad: elegida.cantidad });
      }
    }
    return opciones;
  }

  private mostrarResultado(tipo: TipoResultado, mensaje: string): void {
    this.tipoResultado.set(tipo);
    this.mensajeResultado.set(mensaje);
  }

  // RF-3 · Excepción paso 5: el cliente eligió «Regresar»; se descartan las selecciones y el carrito no cambia.
  // «Regresar»: descarta las selecciones y vuelve a la ficha (o al carrito si se estaba editando)
  regresar(): void {
    if (this.editando() !== null) {
      this.router.navigate(['/carrito']);
      return;
    }
    this.router.navigate(['/producto', this.idProductoOriginal]);
  }

  // ===================== Galería de plantillas (RF-4) =====================

  // RF-4 (Selección de plantilla personalizada): inicia aquí. Paso 1: el cliente pide ver la galería de plantillas.
  async abrirGaleria(): Promise<void> {
    const datos = this.datos();
    if (datos === null || this.cargandoGaleria()) {
      return;
    }
    this.mensajeGaleria.set('');
    this.mensajeDentroGaleria.set('');
    this.cargandoGaleria.set(true);
    try {
      // RF-4 · Paso 2: valida que el producto siga activo y consulta sus plantillas activas.
      const todas = await this.api.listarPlantillas(datos.producto.idPostre);
      // Se quita la plantilla que ya está aplicada
      const otras: Plantilla[] = [];
      for (const plantilla of todas) {
        if (plantilla.idPostre !== datos.producto.idPostre) {
          otras.push(plantilla);
        }
      }
      // RF-4 · Excepción paso 3: «Este producto no cuenta con plantillas de diseño»; no se abre la galería.
      if (otras.length === 0) {
        this.mensajeGaleria.set('Este producto no cuenta con plantillas de diseño');
        return;
      }
      // RF-4 · Paso 4: muestra la galería con la vista previa y el precio de cada plantilla.
      this.plantillas.set(otras);
      this.galeriaAbierta.set(true);
    } catch (error) {
      // RF-4 · Excepción paso 2: el producto en personalización ya no está activo, regresa al catálogo.
      if (tieneCodigo(error, 'PRODUCTO_NO_DISPONIBLE')) {
        this.regresarAlCatalogo();
        return;
      }
      // RF-4 · Excepción paso 2: sin conexión, «No fue posible cargar las plantillas»; se conserva el formulario.
      this.mensajeGaleria.set('No fue posible cargar las plantillas');
    } finally {
      this.cargandoGaleria.set(false);
    }
  }

  // RF-4 · Excepción paso 5: el cliente cierra la galería sin confirmar; se conservan las selecciones previas.
  cerrarGaleria(): void {
    this.galeriaAbierta.set(false);
  }

  // RF-4 pasos 6 a 8: revisa que la plantilla siga activa, precarga sus opciones y recalcula
  // RF-4 · Paso 5: el cliente confirmó una plantilla.
  async aplicarPlantilla(plantilla: Plantilla): Promise<void> {
    this.aplicandoPlantilla.set(true);
    this.mensajeDentroGaleria.set('');
    try {
      // RF-4 · Paso 6: valida en la BD que la plantilla siga activa.
      const datos = await this.api.obtenerPersonalizacion(plantilla.idPostre);
      // RF-4 · Paso 7: precarga los valores de diseño de la plantilla y recalcula el precio.
      this.usarDatos(datos);
      // RF-4 · Paso 8: cierra la galería y muestra el formulario con la plantilla aplicada.
      this.galeriaAbierta.set(false);
      this.mostrarResultado('aviso', 'Aplicamos la plantilla «' + datos.producto.nombre + '». Puedes cambiar sus opciones.');
      // La dirección cambia para que al recargar se vea la misma plantilla (sin volver a cargar la página)
      let direccion = '/producto/' + datos.producto.idPostre + '/personalizar';
      const editado = this.editando();
      if (editado !== null) {
        direccion = direccion + '?editar=' + encodeURIComponent(editado.id);
      }
      this.location.replaceState(direccion);
    } catch (error) {
      if (tieneCodigo(error, 'PRODUCTO_NO_DISPONIBLE')) {
        // RF-4 · Excepción paso 6: «La plantilla seleccionada ya no está disponible» y se retira de la galería.
        this.mensajeDentroGaleria.set('La plantilla seleccionada ya no está disponible');
        this.quitarPlantilla(plantilla.idPostre);
      } else {
        this.mensajeDentroGaleria.set('No fue posible cargar las plantillas');
      }
    } finally {
      this.aplicandoPlantilla.set(false);
    }
  }

  private quitarPlantilla(idPostre: number): void {
    const restantes: Plantilla[] = [];
    for (const plantilla of this.plantillas()) {
      if (plantilla.idPostre !== idPostre) {
        restantes.push(plantilla);
      }
    }
    this.plantillas.set(restantes);
  }
}
