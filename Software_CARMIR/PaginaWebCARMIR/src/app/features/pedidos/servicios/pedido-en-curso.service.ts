import { Injectable, signal } from '@angular/core';
import { ComprobantePedido, DatosCliente, DatosEntrega, DatosPago } from '../modelos/pedidos.models';

// Nombre con el que se guardan los datos en el navegador
const LLAVE_ALMACEN = 'carmir.pedidoEnCurso';

// Todo lo que el cliente va llenando entre pantallas
export interface PedidoEnCurso {
  cliente: DatosCliente;
  entrega: DatosEntrega;
  pago: DatosPago;
  // Llave para que un doble clic o un reintento no registre el pedido dos veces
  llaveIdempotencia: string;
  // Último comprobante recibido (pantalla de confirmación)
  ultimoComprobante: ComprobantePedido | null;
  // Folio y teléfono (o correo) para abrir «Mis pedidos» ya llenos
  ultimaConsulta: { folio: string; contacto: string };
}

/**
 * Datos del pedido que se está armando (RNF-7: no se pierden al recargar o perder el internet).
 * Se guardan en el navegador del cliente hasta que el pedido se registra.
 * La API no confía en nada de esto: vuelve a revisar todo al recibir el pedido.
 */
@Injectable({ providedIn: 'root' })
export class PedidoEnCursoService {
  readonly datos = signal<PedidoEnCurso>(this.leerGuardado());

  // ===================== Cambios =====================

  guardarCliente(cliente: DatosCliente): void {
    this.cambiar({ ...this.datos(), cliente: cliente });
  }

  guardarEntrega(entrega: DatosEntrega): void {
    this.cambiar({ ...this.datos(), entrega: entrega });
  }

  guardarPago(pago: DatosPago): void {
    this.cambiar({ ...this.datos(), pago: pago });
  }

  // Regresa la llave del intento actual (la crea si no hay)
  llaveActual(): string {
    let llave = this.datos().llaveIdempotencia;
    if (llave === '') {
      llave = crearLlave();
      this.cambiar({ ...this.datos(), llaveIdempotencia: llave });
    }
    return llave;
  }

  /**
   * RF-12 paso 7: el pedido quedó registrado.
   * Se guarda el comprobante, se prepara «Mis pedidos» y se olvida la fecha y el pago
   * (los datos de contacto y la dirección se conservan para el siguiente pedido).
   */
  terminar(comprobante: ComprobantePedido): void {
    const actual = this.datos();
    const entrega: DatosEntrega = { ...actual.entrega, fecha: '', horario: '' };
    this.cambiar({
      ...actual,
      entrega: entrega,
      llaveIdempotencia: '',
      ultimoComprobante: comprobante,
      ultimaConsulta: { folio: String(comprobante.folio), contacto: comprobante.cliente.telefono },
    });
  }

  guardarConsulta(folio: string, contacto: string): void {
    this.cambiar({ ...this.datos(), ultimaConsulta: { folio: folio, contacto: contacto } });
  }

  // ===================== Consultas =====================

  // true si ya se llenaron los datos de la pantalla de entrega
  entregaCompleta(): boolean {
    const datos = this.datos();
    return datos.cliente.nombre !== '' && datos.cliente.telefono !== '' && datos.entrega.fecha !== '' && datos.entrega.horario !== '';
  }

  // ===================== Guardado en el navegador =====================

  private cambiar(nuevos: PedidoEnCurso): void {
    this.datos.set(nuevos);
    try {
      localStorage.setItem(LLAVE_ALMACEN, JSON.stringify(nuevos));
    } catch {
      // Si el navegador no deja guardar, los datos solo viven mientras la página esté abierta
    }
  }

  private leerGuardado(): PedidoEnCurso {
    const vacio = pedidoVacio();
    try {
      const texto = localStorage.getItem(LLAVE_ALMACEN);
      if (texto === null) {
        return vacio;
      }
      const guardado = JSON.parse(texto);
      if (guardado === null || typeof guardado !== 'object') {
        return vacio;
      }
      // Se toma cada parte solo si tiene la forma correcta; lo demás se queda vacío
      return {
        cliente: { ...vacio.cliente, ...soloTextos(guardado.cliente) },
        entrega: leerEntrega(guardado.entrega, vacio.entrega),
        pago: leerPago(guardado.pago, vacio.pago),
        llaveIdempotencia: textoOVacio(guardado.llaveIdempotencia),
        ultimoComprobante: leerComprobante(guardado.ultimoComprobante),
        ultimaConsulta: leerUltimaConsulta(guardado.ultimaConsulta),
      };
    } catch {
      // Datos dañados: se empieza de cero
    }
    return vacio;
  }
}

// ===================== Apoyo =====================

export function pedidoVacio(): PedidoEnCurso {
  return {
    cliente: { nombre: '', telefono: '', correo: '' },
    entrega: {
      modalidad: 'Recoger en tienda',
      fecha: '',
      horario: '',
      direccion: { calle: '', numeroExterior: '', numeroInterior: '', colonia: '', codigoPostal: '', referencias: '' },
      costoEnvio: 0,
    },
    pago: { metodo: 'Efectivo', modalidad: 'Anticipo' },
    llaveIdempotencia: '',
    ultimoComprobante: null,
    ultimaConsulta: { folio: '', contacto: '' },
  };
}

// Llave al azar de 32 caracteres (la API acepta letras, números y guiones)
function crearLlave(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  let llave = '';
  while (llave.length < 32) {
    llave = llave + Math.random().toString(36).substring(2);
  }
  return llave.substring(0, 32);
}

function textoOVacio(valor: unknown): string {
  if (typeof valor === 'string') {
    return valor;
  }
  return '';
}

// Copia solo las propiedades de texto de un objeto guardado
// Última búsqueda de «Mis pedidos» (antes se guardaba como "telefono")
function leerUltimaConsulta(valor: unknown): { folio: string; contacto: string } {
  const textos = soloTextos(valor);
  let contacto = '';
  if (textos['contacto'] !== undefined) {
    contacto = textos['contacto'];
  } else if (textos['telefono'] !== undefined) {
    contacto = textos['telefono'];
  }
  let folio = '';
  if (textos['folio'] !== undefined) {
    folio = textos['folio'];
  }
  return { folio: folio, contacto: contacto };
}

function soloTextos(valor: unknown): Record<string, string> {
  const resultado: Record<string, string> = {};
  if (valor === null || typeof valor !== 'object') {
    return resultado;
  }
  const objeto = valor as Record<string, unknown>;
  for (const llave of Object.keys(objeto)) {
    if (typeof objeto[llave] === 'string') {
      resultado[llave] = objeto[llave] as string;
    }
  }
  return resultado;
}

function leerEntrega(valor: unknown, vacia: DatosEntrega): DatosEntrega {
  if (valor === null || typeof valor !== 'object') {
    return vacia;
  }
  const guardada = valor as DatosEntrega;
  const entrega: DatosEntrega = {
    ...vacia,
    direccion: { ...vacia.direccion, ...soloTextos(guardada.direccion) },
  };
  if (guardada.modalidad === 'Recoger en tienda' || guardada.modalidad === 'Envio a domicilio') {
    entrega.modalidad = guardada.modalidad;
  }
  entrega.fecha = textoOVacio(guardada.fecha);
  entrega.horario = textoOVacio(guardada.horario);
  if (typeof guardada.costoEnvio === 'number' && guardada.costoEnvio >= 0) {
    entrega.costoEnvio = guardada.costoEnvio;
  }
  return entrega;
}

function leerPago(valor: unknown, vacio: DatosPago): DatosPago {
  if (valor === null || typeof valor !== 'object') {
    return vacio;
  }
  const guardado = valor as DatosPago;
  const pago: DatosPago = { ...vacio };
  if (guardado.metodo === 'Efectivo' || guardado.metodo === 'Transferencia') {
    pago.metodo = guardado.metodo;
  }
  if (guardado.modalidad === 'Anticipo' || guardado.modalidad === 'Pago total') {
    pago.modalidad = guardado.modalidad;
  }
  return pago;
}

function leerComprobante(valor: unknown): ComprobantePedido | null {
  if (valor === null || typeof valor !== 'object') {
    return null;
  }
  const comprobante = valor as ComprobantePedido;
  if (typeof comprobante.folio !== 'number' || !Array.isArray(comprobante.productos) || comprobante.instrucciones === undefined) {
    return null;
  }
  return comprobante;
}
