import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AvisosService } from '../../../../core/servicios/avisos.service';
import { CarritoService } from '../../../../core/servicios/carrito.service';
import { tieneCodigo } from '../../../../core/utilidades/errores';
import { Cargando } from '../../../../shared/componentes/cargando/cargando';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { UbicacionTienda } from '../../../../shared/componentes/ubicacion-tienda/ubicacion-tienda';
import { CalendarioEntrega } from '../../componentes/calendario-entrega/calendario-entrega';
import { PasosPedido } from '../../componentes/pasos-pedido/pasos-pedido';
import { ResumenPedido } from '../../componentes/resumen-pedido/resumen-pedido';
import { DatosEntrega, DireccionEntrega, ModalidadEntrega, ReglasEntrega } from '../../modelos/pedidos.models';
import { PedidoEnCursoService } from '../../servicios/pedido-en-curso.service';
import { PedidosApiService } from '../../servicios/pedidos-api.service';
import { estadoDelDia, fechaLarga } from '../../utilidades/fechas';
import {
  validarCodigoPostal,
  validarCorreo,
  validarNombre,
  validarTelefono,
  validarTextoCampo,
} from '../../utilidades/validacion-pedido';

type EstadoPantalla = 'cargando' | 'listo' | 'error';
// Revisión del código postal: sin revisar, revisando, con envío o sin envío
type EstadoCobertura = 'sin-revisar' | 'revisando' | 'con-envio' | 'sin-envio' | 'error';

const MENSAJE_SIN_CONEXION = 'No fue posible cargar los días de entrega. Revisa tu conexión e inténtalo de nuevo.';

/**
 * RF-10 · Datos de contacto y entrega.
 *  1. Se cargan las reglas de la repostería (días, horarios y cupo de cada día).
 *  2. El cliente escribe su nombre y WhatsApp, elige recoger en tienda o envío a domicilio.
 *  3. Con envío se revisa el código postal (cobertura y costo de envío).
 *  4. Elige la fecha en el calendario (solo días con cupo) y el horario.
 *  5. Todo se guarda en el navegador y pasa al pago.
 */
@Component({
  selector: 'app-entrega-pagina',
  imports: [
    RouterLink,
    CurrencyPipe,
    Cargando,
    MensajeEstado,
    PasosPedido,
    ResumenPedido,
    CalendarioEntrega,
    UbicacionTienda,
  ],
  templateUrl: './entrega-pagina.html',
  styleUrls: ['../../pedidos-comun.css', './entrega-pagina.css'],
})
export default class EntregaPagina implements OnInit {
  protected readonly carrito = inject(CarritoService);
  private readonly api = inject(PedidosApiService);
  private readonly pedido = inject(PedidoEnCursoService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);

  protected readonly estado = signal<EstadoPantalla>('cargando');
  protected readonly reglas = signal<ReglasEntrega | null>(null);
  protected readonly aviso = signal('');

  // Campos del formulario
  protected readonly nombre = signal('');
  protected readonly telefono = signal('');
  protected readonly correo = signal('');
  protected readonly modalidad = signal<ModalidadEntrega>('Recoger en tienda');
  protected readonly direccion = signal<DireccionEntrega>({
    calle: '',
    numeroExterior: '',
    numeroInterior: '',
    colonia: '',
    codigoPostal: '',
    referencias: '',
  });
  protected readonly fecha = signal('');
  protected readonly horario = signal('');

  // Cobertura del código postal
  protected readonly cobertura = signal<EstadoCobertura>('sin-revisar');
  protected readonly costoEnvio = signal(-1);
  private codigoRevisado = '';

  // Errores por campo: { telefono: 'El teléfono...' }
  protected readonly errores = signal<Record<string, string>>({});
  private intentoContinuar = false;

  // RF-10 (Registro de datos para la entrega): inicia aquí. Paso 1: el cliente eligió «Continuar con mi pedido» en el carrito.
  ngOnInit(): void {
    // RF-10 · Excepción paso 2: el carrito está vacío; regresa al carrito («Tu carrito está vacío» e «Ir al catálogo»).
    if (this.carrito.articulos().length === 0) {
      this.router.navigate(['/carrito']);
      return;
    }
    const aviso = this.avisos.tomarAviso();
    if (aviso !== '') {
      this.aviso.set(aviso);
    }
    this.precargar();
    this.cargarReglas();
  }

  // Lo que el cliente ya había llenado antes (RNF-7)
  // RF-10 · Paso 3: precarga lo que el cliente ya había capturado (RNF-7).
  private precargar(): void {
    const guardado = this.pedido.datos();
    this.nombre.set(guardado.cliente.nombre);
    this.telefono.set(guardado.cliente.telefono);
    this.correo.set(guardado.cliente.correo);
    this.modalidad.set(guardado.entrega.modalidad);
    this.direccion.set({ ...guardado.entrega.direccion });
    this.fecha.set(guardado.entrega.fecha);
    this.horario.set(guardado.entrega.horario);
    if (guardado.entrega.modalidad === 'Envio a domicilio' && guardado.entrega.direccion.codigoPostal !== '') {
      // El costo guardado se vuelve a confirmar con la API
      this.revisarCobertura();
    }
  }

  // RF-10 · Paso 3: carga las reglas (días laborables, horario, anticipación y cupo) para mostrar el formulario.
  async cargarReglas(): Promise<void> {
    this.estado.set('cargando');
    try {
      const reglas = await this.api.leerReglasEntrega();
      this.reglas.set(reglas);
      this.revisarFechaGuardada(reglas);
      this.estado.set('listo');
    } catch {
      // RF-10 · Excepción paso 3: sin conexión, no se pueden cargar los días de entrega (con «Reintentar»).
      this.estado.set('error');
    }
  }

  // Si la fecha que se tenía ya no se puede (se llenó o ya pasó), se quita y se avisa
  // RF-10 · Excepción paso 6: la fecha guardada ya no cumple la anticipación o no tiene cupo; se quita y se avisa.
  private revisarFechaGuardada(reglas: ReglasEntrega): void {
    if (this.fecha() !== '' && estadoDelDia(reglas, this.fecha()) !== 'disponible') {
      this.aviso.set('La fecha que habías elegido (' + fechaLarga(this.fecha()) + ') ya no está disponible. Elige otra.');
      this.fecha.set('');
    }
    if (this.horario() !== '' && reglas.horarios.indexOf(this.horario()) < 0) {
      this.horario.set('');
    }
  }

  // ===================== Cambios del cliente =====================

  // RF-10 · Paso 4: el cliente captura sus datos.
  cambiarCampo(campo: string, valor: string): void {
    if (campo === 'nombre') {
      this.nombre.set(valor);
    } else if (campo === 'telefono') {
      // Solo números (el teclado del celular a veces pone espacios o guiones)
      this.telefono.set(valor.replace(/[^0-9]/g, ''));
    } else if (campo === 'correo') {
      this.correo.set(valor);
    }
    this.revalidar();
  }

  cambiarDireccion(campo: string, valor: string): void {
    const direccion: DireccionEntrega = { ...this.direccion() };
    if (campo === 'calle') {
      direccion.calle = valor;
    } else if (campo === 'numeroExterior') {
      direccion.numeroExterior = valor;
    } else if (campo === 'numeroInterior') {
      direccion.numeroInterior = valor;
    } else if (campo === 'colonia') {
      direccion.colonia = valor;
    } else if (campo === 'referencias') {
      direccion.referencias = valor;
    } else if (campo === 'codigoPostal') {
      direccion.codigoPostal = valor.replace(/[^0-9]/g, '');
      // Si cambia el código postal hay que volver a revisar la cobertura
      if (direccion.codigoPostal !== this.codigoRevisado) {
        this.cobertura.set('sin-revisar');
        this.costoEnvio.set(-1);
      }
    }
    this.direccion.set(direccion);
    this.revalidar();
    // Con 5 dígitos se revisa la cobertura sin que el cliente tenga que salir del campo
    if (campo === 'codigoPostal' && direccion.codigoPostal.length === 5) {
      this.revisarCobertura();
    }
  }

  elegirModalidad(modalidad: ModalidadEntrega): void {
    this.modalidad.set(modalidad);
    this.revalidar();
  }

  elegirFecha(fecha: string): void {
    this.fecha.set(fecha);
    this.revalidar();
  }

  elegirHorario(horario: string): void {
    this.horario.set(horario);
    this.revalidar();
  }

  // Después del primer intento, los errores se actualizan con cada cambio
  private revalidar(): void {
    if (this.intentoContinuar) {
      this.validarTodo();
    }
  }

  // ===================== RF-10 paso 7: cobertura =====================

  // RF-10 · Paso 7: con envío a domicilio, valida la cobertura del código postal y obtiene el costo de envío.
  async revisarCobertura(): Promise<void> {
    const codigo = this.direccion().codigoPostal;
    if (validarCodigoPostal(codigo) !== '' || this.cobertura() === 'revisando') {
      return;
    }
    this.cobertura.set('revisando');
    try {
      const respuesta = await this.api.revisarCobertura(codigo);
      // Si el cliente cambió el código mientras se revisaba, se ignora esta respuesta
      if (this.direccion().codigoPostal !== codigo) {
        this.cobertura.set('sin-revisar');
        return;
      }
      this.codigoRevisado = codigo;
      // RF-10 · Paso 7: el código tiene cobertura, se agrega el costo de envío.
      this.costoEnvio.set(respuesta.costoEnvio);
      this.cobertura.set('con-envio');
    } catch (error) {
      this.codigoRevisado = codigo;
      this.costoEnvio.set(-1);
      // RF-10 · Excepción paso 7: «Por el momento no tenemos envío a tu zona»; se ofrece recoger en tienda.
      if (tieneCodigo(error, 'SIN_COBERTURA')) {
        this.cobertura.set('sin-envio');
      } else {
        this.cobertura.set('error');
      }
    }
    this.revalidar();
  }

  // ===================== Validación y continuar =====================

  // RF-10 · Paso 5: valida campos obligatorios y formato de teléfono, correo y código postal.
  private validarTodo(): boolean {
    const errores: Record<string, string> = {};
    agregarError(errores, 'nombre', validarNombre(this.nombre()));
    agregarError(errores, 'telefono', validarTelefono(this.telefono()));
    agregarError(errores, 'correo', validarCorreo(this.correo()));

    if (this.modalidad() === 'Envio a domicilio') {
      const direccion = this.direccion();
      agregarError(errores, 'calle', validarTextoCampo(direccion.calle, 'la calle', 100, true));
      agregarError(errores, 'numeroExterior', validarTextoCampo(direccion.numeroExterior, 'el número exterior', 10, true));
      agregarError(errores, 'numeroInterior', validarTextoCampo(direccion.numeroInterior, 'número interior', 10, false));
      agregarError(errores, 'colonia', validarTextoCampo(direccion.colonia, 'la colonia', 80, true));
      agregarError(errores, 'referencias', validarTextoCampo(direccion.referencias, 'referencias', 255, false));
      let errorCodigo = validarCodigoPostal(direccion.codigoPostal);
      // RF-10 · Excepción paso 7: código postal fuera de la zona de cobertura.
      if (errorCodigo === '' && this.cobertura() === 'sin-envio') {
        errorCodigo = 'Por el momento no tenemos envío a tu zona. Puedes elegir recoger en tienda.';
      }
      agregarError(errores, 'codigoPostal', errorCodigo);
    }

    // RF-10 · Excepción paso 6: falta elegir la fecha (el calendario solo deja elegir fechas válidas).
    if (this.fecha() === '') {
      errores['fecha'] = 'Elige la fecha de entrega en el calendario';
    }
    // RF-10 · Excepción paso 6: falta elegir un horario dentro del horario de atención.
    if (this.horario() === '') {
      errores['horario'] = 'Elige el horario de entrega';
    }
    this.errores.set(errores);
    return Object.keys(errores).length === 0;
  }

  errorDe(campo: string): string {
    const mensaje = this.errores()[campo];
    if (mensaje === undefined) {
      return '';
    }
    return mensaje;
  }

  hayErrores(): boolean {
    return Object.keys(this.errores()).length > 0;
  }

  alEnviar(evento: Event): void {
    evento.preventDefault();
    this.continuar();
  }

  // RF-10 · Paso 4: el cliente eligió «Continuar».
  async continuar(): Promise<void> {
    this.intentoContinuar = true;
    this.aviso.set('');
    // Con envío, el costo tiene que estar confirmado para el código postal escrito
    if (this.modalidad() === 'Envio a domicilio' && this.cobertura() !== 'con-envio') {
      await this.revisarCobertura();
    }
    // RF-10 · Excepción paso 5: resalta cada campo con error y conserva los demás datos (regresa al paso 4).
    if (!this.validarTodo()) {
      return;
    }
    // RF-10 · Excepción paso 7: no se pudo confirmar la cobertura por falta de conexión.
    if (this.modalidad() === 'Envio a domicilio' && this.cobertura() !== 'con-envio') {
      this.aviso.set('No fue posible revisar tu código postal. Revisa tu conexión e inténtalo de nuevo.');
      return;
    }

    // RF-10 · Paso 8: guarda los datos de entrega y muestra la selección del método de pago (RF-11).
    this.pedido.guardarCliente({
      nombre: this.nombre().trim().replace(/\s+/g, ' '),
      telefono: this.telefono(),
      correo: this.correo().trim(),
    });
    let costo = 0;
    if (this.modalidad() === 'Envio a domicilio') {
      costo = this.costoEnvio();
    }
    const entrega: DatosEntrega = {
      modalidad: this.modalidad(),
      fecha: this.fecha(),
      horario: this.horario(),
      direccion: limpiarDireccion(this.direccion()),
      costoEnvio: costo,
    };
    this.pedido.guardarEntrega(entrega);
    this.router.navigate(['/pedido/pago']);
  }

  // ===================== Datos para la vista =====================

  fechaElegidaTexto(): string {
    if (this.fecha() === '') {
      return '';
    }
    return fechaLarga(this.fecha());
  }

  conEnvio(): boolean {
    return this.modalidad() === 'Envio a domicilio';
  }

  // Mensaje de las reglas de entrega para el cliente
  textoReglas(reglas: ReglasEntrega): string {
    return (
      'Entregamos ' +
      reglas.textoDiasLaborables +
      ' de ' +
      reglas.horaApertura +
      ' a ' +
      reglas.horaCierre +
      ' h. Pide con al menos ' +
      reglas.diasAnticipacionMinima +
      ' día(s) de anticipación.'
    );
  }

  protected readonly mensajeSinConexion = MENSAJE_SIN_CONEXION;
}

// ===================== Apoyo =====================

function agregarError(errores: Record<string, string>, campo: string, mensaje: string): void {
  if (mensaje !== '') {
    errores[campo] = mensaje;
  }
}

function limpiarDireccion(direccion: DireccionEntrega): DireccionEntrega {
  return {
    calle: direccion.calle.trim(),
    numeroExterior: direccion.numeroExterior.trim(),
    numeroInterior: direccion.numeroInterior.trim(),
    colonia: direccion.colonia.trim(),
    codigoPostal: direccion.codigoPostal.trim(),
    referencias: direccion.referencias.trim(),
  };
}
