import { CurrencyPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { erroresDeCampo, mensajeDeError } from '../../../../core/utilidades/errores';
import { Cargando } from '../../../../shared/componentes/cargando/cargando';
import { ImagenPostre } from '../../../../shared/componentes/imagen-postre/imagen-postre';
import { UbicacionTienda } from '../../../../shared/componentes/ubicacion-tienda/ubicacion-tienda';
import {
  ArchivoComprobante,
  DireccionEntrega,
  PagoSeguimiento,
  PasoSeguimiento,
  SeguimientoPedido,
} from '../../modelos/pedidos.models';
import { PedidoEnCursoService } from '../../servicios/pedido-en-curso.service';
import { PedidosApiService } from '../../servicios/pedidos-api.service';
import { fechaHora, fechaLarga } from '../../utilidades/fechas';
import {
  ACEPTA_COMPROBANTE,
  bytesABase64,
  redondear,
  textoTamano,
  tipoDeArchivo,
  validarArchivoComprobante,
  validarEnlace,
  contactoConsulta,
  validarContactoConsulta,
  validarFolio,
  validarMontoComprobante,
  validarTextoCampo,
} from '../../utilidades/validacion-pedido';

const MENSAJE_SIN_CONEXION = 'No fue posible consultar tu pedido. Revisa tu conexión e inténtalo de nuevo.';
// Campos del formulario del comprobante
const CAMPOS_COMPROBANTE = ['monto', 'referencia', 'enlace', 'archivo'];
const MENSAJE_COMPROBANTE_FALLO = 'No fue posible enviar tu comprobante. Revisa tu conexión e inténtalo de nuevo.';

/**
 * RF-13 · Mis pedidos: seguimiento con folio y teléfono o correo (sin cuenta ni contraseña).
 *  - Barra de avance con los estados del pedido y la fecha de cada uno.
 *  - Productos, entrega, pagos registrados y saldo.
 *  - RF-14: si el pedido ya fue aprobado y se paga por transferencia, el cliente sube la foto o PDF
 *    de su comprobante desde sus archivos o pega un enlace; queda «Pendiente» hasta que el Panel lo valide.
 *  - El efectivo se paga en la tienda (se muestra su dirección).
 * RF-13 · Excepción paso 5: la API bloquea la búsqueda 30 segundos después de 12 intentos fallidos.
 */
@Component({
  selector: 'app-mis-pedidos-pagina',
  imports: [RouterLink, CurrencyPipe, Cargando, ImagenPostre, UbicacionTienda],
  templateUrl: './mis-pedidos-pagina.html',
  styleUrls: ['../../pedidos-comun.css', './mis-pedidos-pagina.css'],
})
export default class MisPedidosPagina implements OnInit, OnDestroy {
  private readonly api = inject(PedidosApiService);
  private readonly pedido = inject(PedidoEnCursoService);
  private readonly ruta = inject(ActivatedRoute);

  // Búsqueda
  protected readonly folio = signal('');
  // Teléfono de WhatsApp o correo (si trae @ es correo)
  protected readonly contacto = signal('');
  protected readonly errorFolio = signal('');
  protected readonly errorContacto = signal('');
  protected readonly buscando = signal(false);
  protected readonly errorBusqueda = signal('');
  protected readonly seguimiento = signal<SeguimientoPedido | null>(null);

  // Comprobante de pago
  protected readonly formularioAbierto = signal(false);
  // 'archivo' = sube la foto/PDF desde su equipo; 'enlace' = pega un enlace (como antes)
  protected readonly modoComprobante = signal<'archivo' | 'enlace'>('archivo');
  protected readonly archivo = signal<File | null>(null);
  // Dirección temporal (blob:) para ver la foto antes de enviarla; '' si es PDF o no hay archivo
  protected readonly vistaPrevia = signal('');
  protected readonly aceptaComprobante = ACEPTA_COMPROBANTE;
  protected readonly monto = signal('');
  protected readonly referencia = signal('');
  protected readonly enlace = signal('');
  protected readonly erroresComprobante = signal<Record<string, string>>({});
  protected readonly enviandoComprobante = signal(false);
  protected readonly mensajeComprobante = signal('');
  protected readonly errorComprobante = signal('');

  // RF-13 (Visualización de pedidos realizados): inicia aquí. Pasos 1 y 2: el cliente abrió «Mis pedidos» y se le pide folio y teléfono o correo.
  ngOnInit(): void {
    // Se llenan solos con el último pedido enviado (o con ?folio= de un enlace)
    const ultima = this.pedido.datos().ultimaConsulta;
    this.folio.set(ultima.folio);
    this.contacto.set(ultima.contacto);
    const folioEnlace = this.ruta.snapshot.queryParamMap.get('folio');
    if (folioEnlace !== null && validarFolio(folioEnlace) === '') {
      this.folio.set(folioEnlace);
    }
    if (this.folio() !== '' && this.contacto() !== '') {
      this.buscar();
    }
  }

  // Al salir de la página se libera la vista previa de la foto
  ngOnDestroy(): void {
    this.liberarVistaPrevia();
  }

  // ===================== Búsqueda =====================

  // RF-13 · Paso 3: el cliente captura el folio y su teléfono o correo.
  cambiarFolio(texto: string): void {
    this.folio.set(texto.replace(/[^0-9#]/g, ''));
    this.errorFolio.set('');
  }

  cambiarContacto(texto: string): void {
    this.contacto.set(texto);
    this.errorContacto.set('');
  }

  alBuscar(evento: Event): void {
    evento.preventDefault();
    this.buscar();
  }

  // RF-13 · Paso 3: el cliente eligió «Buscar».
  async buscar(): Promise<void> {
    if (this.buscando()) {
      return;
    }
    // RF-13 · Paso 4: valida que el folio y el teléfono (o el correo) tengan formato correcto.
    this.errorFolio.set(validarFolio(this.folio()));
    this.errorContacto.set(validarContactoConsulta(this.contacto()));
    // RF-13 · Excepción paso 4: resalta el campo con error y conserva lo capturado.
    if (this.errorFolio() !== '' || this.errorContacto() !== '') {
      return;
    }
    this.buscando.set(true);
    this.errorBusqueda.set('');
    const folio = this.folio().replace('#', '');
    try {
      // RF-13 · Pasos 5 y 6: la API busca el pedido con folio y teléfono (o correo) y recupera su detalle.
      const seguimiento = await this.api.consultarPedido(folio, contactoConsulta(this.contacto()));
      this.usarSeguimiento(seguimiento);
      this.pedido.guardarConsulta(folio, this.contacto().trim());
    } catch (error) {
      // RF-13 · Excepciones paso 5: «No encontramos un pedido con esos datos», búsqueda bloqueada por intentos o sin conexión («No fue posible consultar tu pedido»).
      this.seguimiento.set(null);
      this.errorBusqueda.set(mensajeDeError(error, MENSAJE_SIN_CONEXION));
    } finally {
      this.buscando.set(false);
    }
  }

  // RF-13 · Paso 7: muestra el detalle con la barra de avance.
  private usarSeguimiento(seguimiento: SeguimientoPedido): void {
    this.seguimiento.set(seguimiento);
    this.formularioAbierto.set(false);
    this.erroresComprobante.set({});
    this.errorComprobante.set('');
    // El monto sugerido es lo que falta para iniciar (o el saldo pendiente)
    this.monto.set(String(this.montoSugerido(seguimiento)));
  }

  // RF-13 · Excepción paso 3: «Buscar otro pedido» limpia el resultado.
  otraBusqueda(): void {
    this.seguimiento.set(null);
    this.mensajeComprobante.set('');
  }

  // ===================== Datos para la vista =====================

  fechaLarga(texto: string): string {
    return fechaLarga(texto);
  }

  fechaHora(texto: string | null): string {
    return fechaHora(texto);
  }

  textoDireccion(direccion: DireccionEntrega): string {
    let texto = direccion.calle + ' ' + direccion.numeroExterior;
    if (direccion.numeroInterior !== null && direccion.numeroInterior !== '') {
      texto = texto + ' int. ' + direccion.numeroInterior;
    }
    return texto + ', ' + direccion.colonia + ', C.P. ' + direccion.codigoPostal;
  }

  // Lo que falta por cubrir sin contar comprobantes en revisión
  porCubrir(seguimiento: SeguimientoPedido): number {
    return redondear(seguimiento.montoTotal - seguimiento.totalPagado - seguimiento.porValidar);
  }

  private montoSugerido(seguimiento: SeguimientoPedido): number {
    const faltaParaIniciar = redondear(seguimiento.montoParaIniciar - seguimiento.totalPagado - seguimiento.porValidar);
    if (faltaParaIniciar > 0) {
      return faltaParaIniciar;
    }
    return Math.max(this.porCubrir(seguimiento), 0);
  }

  // Los lectores de pantalla anuncian cuál es el estado actual
  marcaPaso(paso: PasoSeguimiento): string | null {
    if (paso.actual) {
      return 'step';
    }
    return null;
  }

  claseEstadoPago(pago: PagoSeguimiento): string {
    if (pago.estado === 'Validado') {
      return 'validado';
    }
    if (pago.estado === 'Rechazado') {
      return 'rechazado';
    }
    return 'pendiente';
  }

  textoEstadoPago(pago: PagoSeguimiento): string {
    if (pago.estado === 'Pendiente') {
      return 'En revisión';
    }
    return pago.estado;
  }

  // ===================== Comprobante de pago (RF-14) =====================

  // RF-14 (Envío de comprobante de pago): inicia aquí. Paso 1: el cliente eligió «Enviar comprobante de transferencia».
  // RF-14 · Paso 2: muestra el formulario con el monto sugerido (después lo valida RF-42 en el Panel).
  abrirFormulario(): void {
    this.mensajeComprobante.set('');
    this.formularioAbierto.set(true);
  }

  // RF-14 · Excepción paso 3: «Cancelar» cierra el formulario sin registrar nada.
  cerrarFormulario(): void {
    this.formularioAbierto.set(false);
  }

  // RF-14 · Paso 3: el cliente elige si sube un archivo o pega un enlace
  elegirModoComprobante(modo: 'archivo' | 'enlace'): void {
    this.modoComprobante.set(modo);
    const errores = { ...this.erroresComprobante() };
    delete errores['archivo'];
    delete errores['enlace'];
    this.erroresComprobante.set(errores);
  }

  // RF-14 · Pasos 3 y 4: el cliente eligió una foto o PDF de sus archivos; se revisa tipo y tamaño antes de aceptarlo.
  alElegirArchivo(campo: HTMLInputElement): void {
    const errores = { ...this.erroresComprobante() };
    delete errores['archivo'];
    if (campo.files === null || campo.files.length === 0) {
      this.erroresComprobante.set(errores);
      return;
    }
    const elegido = campo.files[0];
    const error = validarArchivoComprobante(tipoDeArchivo(elegido.name, elegido.type), elegido.size);
    // RF-14 · Excepción paso 4: «Solo se aceptan fotos JPG, PNG o WEBP, o un PDF» o «El archivo pesa más de 5 MB…».
    if (error !== '') {
      errores['archivo'] = error;
      this.erroresComprobante.set(errores);
      campo.value = '';
      return;
    }
    this.erroresComprobante.set(errores);
    this.liberarVistaPrevia();
    this.archivo.set(elegido);
    if (tipoDeArchivo(elegido.name, elegido.type) !== 'application/pdf') {
      this.vistaPrevia.set(URL.createObjectURL(elegido));
    }
  }

  quitarArchivo(campo: HTMLInputElement): void {
    campo.value = '';
    this.archivo.set(null);
    this.liberarVistaPrevia();
  }

  private liberarVistaPrevia(): void {
    if (this.vistaPrevia() !== '') {
      URL.revokeObjectURL(this.vistaPrevia());
      this.vistaPrevia.set('');
    }
  }

  textoTamano(bytes: number): string {
    return textoTamano(bytes);
  }

  // Lee el archivo elegido y lo convierte a Base64 para mandarlo a la API
  private async leerArchivo(): Promise<ArchivoComprobante | null> {
    const elegido = this.archivo();
    if (this.modoComprobante() !== 'archivo' || elegido === null) {
      return null;
    }
    const bytes = new Uint8Array(await elegido.arrayBuffer());
    return {
      nombre: elegido.name,
      tipo: tipoDeArchivo(elegido.name, elegido.type),
      contenidoBase64: bytesABase64(bytes),
    };
  }

  cambiarCampoComprobante(campo: string, valor: string): void {
    if (campo === 'monto') {
      this.monto.set(valor);
    } else if (campo === 'referencia') {
      this.referencia.set(valor);
    } else if (campo === 'enlace') {
      this.enlace.set(valor);
    }
    const errores = { ...this.erroresComprobante() };
    delete errores[campo];
    this.erroresComprobante.set(errores);
  }

  errorDe(campo: string): string {
    const mensaje = this.erroresComprobante()[campo];
    if (mensaje === undefined) {
      return '';
    }
    return mensaje;
  }

  // RF-14 · Paso 4: valida monto, referencia y el archivo o el enlace del comprobante.
  private validarComprobante(seguimiento: SeguimientoPedido): boolean {
    const errores: Record<string, string> = {};
    const errorMonto = validarMontoComprobante(Number(this.monto()), this.porCubrir(seguimiento));
    if (errorMonto !== '') {
      errores['monto'] = errorMonto;
    }
    const errorReferencia = validarTextoCampo(this.referencia(), 'la referencia o folio bancario', 50, true);
    if (errorReferencia !== '') {
      errores['referencia'] = errorReferencia;
    }
    if (this.modoComprobante() === 'archivo') {
      // RF-14 · Excepción paso 4: «Elige la foto o el PDF de tu comprobante».
      if (this.archivo() === null) {
        errores['archivo'] = 'Elige la foto o el PDF de tu comprobante';
      }
    } else {
      const errorEnlace = validarEnlace(this.enlace());
      if (errorEnlace !== '') {
        errores['enlace'] = errorEnlace;
      }
    }
    this.erroresComprobante.set(errores);
    return Object.keys(errores).length === 0;
  }

  alEnviarComprobante(evento: Event): void {
    evento.preventDefault();
    this.enviarComprobante();
  }

  // RF-14 · Paso 3: el cliente eligió «Enviar comprobante»; se valida (Paso 4) y se envía a la API (Pasos 5 a 7).
  async enviarComprobante(): Promise<void> {
    const seguimiento = this.seguimiento();
    if (seguimiento === null || this.enviandoComprobante()) {
      return;
    }
    this.errorComprobante.set('');
    // RF-14 · Excepción paso 4: resalta el campo con error y conserva lo capturado.
    if (!this.validarComprobante(seguimiento)) {
      return;
    }
    this.enviandoComprobante.set(true);
    try {
      const archivo = await this.leerArchivo();
      let enlace = '';
      if (this.modoComprobante() === 'enlace') {
        enlace = this.enlace().trim();
      }
      // El comprobante se manda con el mismo dato con el que se encontró el pedido
      const contacto = contactoConsulta(this.contacto());
      const respuesta = await this.api.enviarComprobante({
        folio: seguimiento.folio,
        telefono: contacto.telefono,
        correo: contacto.correo,
        monto: redondear(Number(this.monto())),
        metodo: 'Transferencia',
        referencia: this.referencia().trim(),
        linkComprobante: enlace,
        archivo: archivo,
      });
      // RF-14 · Paso 8: «Recibimos tu comprobante…» y el detalle se actualiza con el pago «En revisión».
      this.usarSeguimiento(respuesta.seguimiento);
      this.referencia.set('');
      this.enlace.set('');
      this.archivo.set(null);
      this.liberarVistaPrevia();
      this.mensajeComprobante.set(respuesta.mensaje);
    } catch (error) {
      // RF-14 · Excepciones pasos 5 y 6: pedido no encontrado o que ya no admite pagos, 3 comprobantes en revisión,
      // monto mayor a lo que falta, referencia repetida o sin conexión.
      // Errores de un campo (monto mayor al saldo, referencia repetida...) se marcan en rojo
      const campos = erroresDeCampo(error);
      if (campos.length > 0) {
        const errores: Record<string, string> = {};
        for (const campo of campos) {
          let nombre = campo.campo;
          if (nombre === 'linkComprobante') {
            nombre = 'enlace';
          }
          if (CAMPOS_COMPROBANTE.indexOf(nombre) >= 0) {
            errores[nombre] = campo.mensaje;
          } else {
            // Folio, teléfono o correo: no son campos de este formulario, se muestra arriba del botón
            this.errorComprobante.set(campo.mensaje);
          }
        }
        this.erroresComprobante.set(errores);
      } else {
        this.errorComprobante.set(mensajeDeError(error, MENSAJE_COMPROBANTE_FALLO));
      }
    } finally {
      this.enviandoComprobante.set(false);
    }
  }
}
