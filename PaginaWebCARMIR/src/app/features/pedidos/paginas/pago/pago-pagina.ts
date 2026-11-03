import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DIRECCION_TIENDA } from '../../../../core/datos-tienda';
import { AvisosService } from '../../../../core/servicios/avisos.service';
import { CarritoService } from '../../../../core/servicios/carrito.service';
import { erroresDeCampo, esErrorApi, esSinConexion, tieneCodigo } from '../../../../core/utilidades/errores';
import { Cargando } from '../../../../shared/componentes/cargando/cargando';
import { DialogoConfirmacion } from '../../../../shared/componentes/dialogo-confirmacion/dialogo-confirmacion';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { UbicacionTienda } from '../../../../shared/componentes/ubicacion-tienda/ubicacion-tienda';
import { PasosPedido } from '../../componentes/pasos-pedido/pasos-pedido';
import { ResumenPedido } from '../../componentes/resumen-pedido/resumen-pedido';
import { MetodoPago, ModalidadPago, PreciosActualizados, ReglasEntrega, SolicitudPedido } from '../../modelos/pedidos.models';
import { PedidoEnCursoService } from '../../servicios/pedido-en-curso.service';
import { PedidosApiService, articulosParaApi } from '../../servicios/pedidos-api.service';
import { fechaLarga } from '../../utilidades/fechas';
import { calcularAnticipo, redondear } from '../../utilidades/validacion-pedido';

type EstadoPantalla = 'cargando' | 'listo' | 'error';

const MENSAJE_SIN_CONEXION = 'No fue posible cargar las formas de pago. Revisa tu conexión e inténtalo de nuevo.';
const MENSAJE_ENVIO_FALLO =
  'No fue posible enviar tu pedido. Revisa tu conexión e inténtalo de nuevo; si ya se había enviado, no se duplicará.';

// Errores de la API que se corrigen en la pantalla de entrega
const CAMPOS_DE_ENTREGA = [
  'nombre',
  'telefono',
  'correo',
  'modalidad',
  'fecha',
  'horario',
  'calle',
  'numeroExterior',
  'numeroInterior',
  'colonia',
  'codigoPostal',
  'referencias',
];
// Errores de la API que se corrigen en el carrito
const CODIGOS_DE_CARRITO = ['PRODUCTOS_NO_DISPONIBLES', 'POSTRE_REPETIDO', 'CARRITO_VACIO'];

/**
 * RF-10 · Método y modalidad de pago, y RF-11 · Enviar el pedido.
 *  1. Se cargan el porcentaje de anticipo y si hay datos bancarios (si no, solo pago en sucursal).
 *  2. El cliente elige método (efectivo en la tienda o transferencia) y modalidad (anticipo o total).
 *     La opción «Depósito» se quitó a pedido de la repostería.
 *  3. Confirma en una ventana (RNF-5) y se envía el pedido con una llave contra doble envío.
 *  4. La API recalcula precios, revisa el cupo del día y guarda todo en una transacción.
 */
@Component({
  selector: 'app-pago-pagina',
  imports: [
    RouterLink,
    CurrencyPipe,
    Cargando,
    MensajeEstado,
    PasosPedido,
    ResumenPedido,
    DialogoConfirmacion,
    UbicacionTienda,
  ],
  templateUrl: './pago-pagina.html',
  styleUrls: ['../../pedidos-comun.css', './pago-pagina.css'],
})
export default class PagoPagina implements OnInit {
  protected readonly carrito = inject(CarritoService);
  protected readonly pedido = inject(PedidoEnCursoService);
  private readonly api = inject(PedidosApiService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);

  protected readonly estado = signal<EstadoPantalla>('cargando');
  protected readonly reglas = signal<ReglasEntrega | null>(null);
  protected readonly metodo = signal<MetodoPago>('Efectivo');
  protected readonly modalidadPago = signal<ModalidadPago>('Anticipo');

  protected readonly confirmando = signal(false);
  protected readonly enviando = signal(false);
  protected readonly mensajeError = signal('');
  protected readonly avisoPrecio = signal('');

  protected readonly mensajeSinConexion = MENSAJE_SIN_CONEXION;
  protected readonly direccionTienda = DIRECCION_TIENDA;

  // RF-10 (Selección de modalidad de pago): inicia aquí. Paso 1: el cliente continuó desde los datos de entrega.
  ngOnInit(): void {
    // RF-10 · Excepción paso 2: el carrito está vacío; regresa al carrito («Tu carrito está vacío»).
    if (this.carrito.articulos().length === 0) {
      this.router.navigate(['/carrito']);
      return;
    }
    // RF-10 · Excepción paso 2: faltan los datos de entrega; redirige al formulario de entrega.
    if (!this.pedido.entregaCompleta()) {
      this.router.navigate(['/pedido/entrega']);
      return;
    }
    const guardado = this.pedido.datos().pago;
    this.metodo.set(guardado.metodo);
    this.modalidadPago.set(guardado.modalidad);
    this.cargarReglas();
  }

  // RF-10 · Paso 3: carga el porcentaje de anticipo y si hay datos bancarios para mostrar las modalidades.
  async cargarReglas(): Promise<void> {
    this.estado.set('cargando');
    try {
      const reglas = await this.api.leerReglasEntrega();
      this.reglas.set(reglas);
      // Sin datos bancarios solo se puede pagar en sucursal (RF-10 paso 3)
      // RF-10 · Excepción paso 6: sin datos bancarios configurados, solo queda el pago en sucursal.
      if (!reglas.aceptaTransferencia && this.metodo() !== 'Efectivo') {
        this.metodo.set('Efectivo');
      }
      this.estado.set('listo');
    } catch {
      // RF-10 · Excepción paso 3: sin conexión, no se pueden cargar las formas de pago.
      this.estado.set('error');
    }
  }

  // ===================== Elecciones del cliente =====================

  // RF-10 · Paso 4: el cliente selecciona el método de pago.
  elegirMetodo(metodo: MetodoPago): void {
    const reglas = this.reglas();
    // RF-10 · Excepción paso 6: la transferencia está deshabilitada sin datos bancarios.
    if (metodo !== 'Efectivo' && (reglas === null || !reglas.aceptaTransferencia)) {
      return;
    }
    this.metodo.set(metodo);
    this.guardarPago();
  }

  // RF-10 · Paso 4: el cliente selecciona la modalidad (anticipo o pago total).
  elegirModalidad(modalidad: ModalidadPago): void {
    this.modalidadPago.set(modalidad);
    this.guardarPago();
  }

  // RF-10 · Paso 6: asocia la modalidad al pedido en curso (las instrucciones se generan al registrarlo).
  private guardarPago(): void {
    this.mensajeError.set('');
    this.pedido.guardarPago({ metodo: this.metodo(), modalidad: this.modalidadPago() });
  }

  // ===================== Importes =====================

  costoEnvio(): number {
    return this.pedido.datos().entrega.costoEnvio;
  }

  conEnvio(): boolean {
    return this.pedido.datos().entrega.modalidad === 'Envio a domicilio';
  }

  // RF-10 · Paso 3: desglose del total (productos más envío) y anticipo mínimo.
  total(): number {
    let envio = 0;
    if (this.conEnvio()) {
      envio = this.costoEnvio();
    }
    return redondear(this.carrito.totalImporte() + envio);
  }

  porcentaje(): number {
    const reglas = this.reglas();
    if (reglas === null) {
      return 50;
    }
    return reglas.porcentajeAnticipo;
  }

  anticipo(): number {
    return calcularAnticipo(this.total(), this.porcentaje(), 'Anticipo');
  }

  montoParaIniciar(): number {
    return calcularAnticipo(this.total(), this.porcentaje(), this.modalidadPago());
  }

  // ===================== Datos de entrega para mostrar =====================

  textoFecha(): string {
    const entrega = this.pedido.datos().entrega;
    return fechaLarga(entrega.fecha) + ' a las ' + entrega.horario + ' h';
  }

  textoDireccion(): string {
    const direccion = this.pedido.datos().entrega.direccion;
    let texto = direccion.calle + ' ' + direccion.numeroExterior;
    if (direccion.numeroInterior !== '') {
      texto = texto + ' int. ' + direccion.numeroInterior;
    }
    return texto + ', ' + direccion.colonia + ', C.P. ' + direccion.codigoPostal;
  }

  // ===================== RF-11: enviar el pedido =====================

  // RF-11 (Confirmación y envío de solicitud del pedido): inicia aquí. Paso 1: el cliente pidió «Confirmar pedido».
  pedirConfirmacion(): void {
    if (this.enviando()) {
      return;
    }
    this.mensajeError.set('');
    // RF-11 · Paso 4: muestra la confirmación final con el total, la fecha de entrega y el anticipo (RNF-5).
    this.confirmando.set(true);
  }

  // RF-11 · Excepción paso 5: el cliente canceló; se queda en el resumen sin registrar el pedido.
  cancelarConfirmacion(): void {
    if (!this.enviando()) {
      this.confirmando.set(false);
    }
  }

  // RF-11 · Paso 5: el cliente confirmó el envío.
  async enviarPedido(): Promise<void> {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.avisoPrecio.set('');
    const datos = this.pedido.datos();
    let direccion = null;
    if (datos.entrega.modalidad === 'Envio a domicilio') {
      direccion = datos.entrega.direccion;
    }
    // RF-11 · Paso 2: arma la solicitud con el carrito, los datos de entrega y el método de pago.
    const solicitud: SolicitudPedido = {
      // RF-11 · Excepción paso 6: la llave evita registrar dos veces el mismo pedido (doble envío).
      llaveIdempotencia: this.pedido.llaveActual(),
      articulos: articulosParaApi(this.carrito.articulos()),
      cliente: datos.cliente,
      entrega: {
        modalidad: datos.entrega.modalidad,
        fecha: datos.entrega.fecha,
        horario: datos.entrega.horario,
        direccion: direccion,
      },
      pago: { metodo: this.metodo(), modalidad: this.modalidadPago() },
      totalMostrado: this.total(),
    };
    try {
      // RF-11 · Pasos 3 y 6: la API valida en la BD y registra el pedido con folio único.
      const comprobante = await this.api.registrarPedido(solicitud);
      // RF-11 paso 7: se vacía el carrito y se guarda el comprobante
      this.carrito.vaciar();
      // RF-11 · Paso 8: guarda el comprobante y muestra la confirmación.
      this.pedido.terminar(comprobante);
      this.confirmando.set(false);
      this.router.navigate(['/pedido/confirmacion'], { replaceUrl: true });
    } catch (error) {
      this.confirmando.set(false);
      this.atenderError(error);
    } finally {
      this.enviando.set(false);
    }
  }

  // Cada error manda al cliente al lugar donde lo puede corregir
  // RF-11: cada excepción manda al cliente al lugar donde la puede corregir.
  private atenderError(error: unknown): void {
    // RF-11 · Excepción paso 6: «No fue posible enviar tu pedido» con «Reintentar»; se conservan los datos.
    if (esSinConexion(error) || !esErrorApi(error)) {
      this.mensajeError.set(MENSAJE_ENVIO_FALLO);
      return;
    }

    // RF-11 · Excepción paso 3: los precios cambiaron; se actualiza el resumen y se avisa al cliente.
    // El precio cambió: se actualiza el carrito y el cliente confirma otra vez
    if (error.codigo === 'PRECIO_CAMBIO') {
      this.usarPreciosNuevos(error.detalles);
      this.avisoPrecio.set(error.mensaje);
      return;
    }

    // RF-11 · Excepciones pasos 2 y 3: carrito vacío o productos «No disponible»; regresa al carrito.
    if (CODIGOS_DE_CARRITO.indexOf(error.codigo) >= 0) {
      this.avisos.dejarAviso(error.mensaje);
      this.router.navigate(['/carrito']);
      return;
    }

    // RF-11 · Excepción paso 2: datos de entrega incompletos o inválidos; redirige a la entrega conservando lo capturado.
    // Sin cobertura, día lleno, datos de contacto...: se corrigen en la pantalla de entrega
    const campos = erroresDeCampo(error);
    if (tieneCodigo(error, 'SIN_COBERTURA') || (campos.length > 0 && CAMPOS_DE_ENTREGA.indexOf(campos[0].campo) >= 0)) {
      this.avisos.dejarAviso(error.mensaje);
      this.router.navigate(['/pedido/entrega']);
      return;
    }

    // RF-10 · Excepción paso 6: «Por el momento solo está disponible el pago en sucursal».
    // La repostería quitó sus datos bancarios mientras tanto
    if (tieneCodigo(error, 'SOLO_SUCURSAL')) {
      this.metodo.set('Efectivo');
      this.guardarPago();
      this.cargarReglas();
    }
    this.mensajeError.set(error.mensaje);
  }

  private usarPreciosNuevos(detalles: unknown): void {
    if (detalles === null || typeof detalles !== 'object') {
      return;
    }
    const precios = detalles as PreciosActualizados;
    if (Array.isArray(precios.articulos)) {
      for (const articulo of precios.articulos) {
        if (typeof articulo.posicion === 'number' && typeof articulo.precioUnitario === 'number') {
          this.carrito.actualizarPrecio(articulo.posicion, articulo.precioUnitario);
        }
      }
    }
    if (typeof precios.costoEnvio === 'number' && this.conEnvio()) {
      this.pedido.guardarEntrega({ ...this.pedido.datos().entrega, costoEnvio: precios.costoEnvio });
    }
  }

  textoBotonEnviar(): string {
    if (this.enviando()) {
      return 'Enviando...';
    }
    return 'Sí, enviar pedido';
  }

  textoConfirmacion(): string {
    return (
      'Total: ' +
      formatoMoneda(this.total()) +
      '. Para iniciar la preparación pagarás ' +
      formatoMoneda(this.montoParaIniciar()) +
      '. Entrega: ' +
      this.textoFecha() +
      '.'
    );
  }
}

// "$1,234.50"
function formatoMoneda(valor: number): string {
  return '$' + valor.toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
