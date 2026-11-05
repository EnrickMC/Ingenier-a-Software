/**
 * Formas de los datos del Módulo 2 · Portal de Pedidos.
 * Son las mismas que regresa ApiCARMIR en /api/carrito, /api/entrega y /api/pedidos.
 * Los valores de modalidad y método son los de la base de datos (sin acentos).
 */

// ===================== Carrito (RF-6) =====================

// disponible | no_disponible | requiere_actualizar | repetido
export type EstadoArticulo = 'disponible' | 'no_disponible' | 'requiere_actualizar' | 'repetido';

export interface ArticuloRevisado {
  posicion: number;
  estado: EstadoArticulo;
  mensaje: string;
  precioUnitario: number;
  precioCambio: boolean;
  opcionesNoDisponibles: number[];
}

export interface RevisionCarrito {
  articulos: ArticuloRevisado[];
  total: number;
  puedeContinuar: boolean;
}

// ===================== Entrega y pago (RF-10 y RF-11) =====================

export interface ReglasEntrega {
  nombreNegocio: string;
  hoy: string;
  fechaMinima: string;
  fechaMaxima: string;
  diasAnticipacionMinima: number;
  // Letras L M X J V S D
  diasLaborables: string[];
  textoDiasLaborables: string;
  // Días que ya llegaron a la capacidad diaria
  fechasLlenas: string[];
  horaApertura: string;
  horaCierre: string;
  horarios: string[];
  porcentajeAnticipo: number;
  aceptaTransferencia: boolean;
  mensajeSoloSucursal: string;
}

export interface Cobertura {
  codigoPostal: string;
  costoEnvio: number;
}

export type ModalidadEntrega = 'Recoger en tienda' | 'Envio a domicilio';
// Solo efectivo en la tienda o transferencia
export type MetodoPago = 'Efectivo' | 'Transferencia';
export type ModalidadPago = 'Anticipo' | 'Pago total';

export interface DatosCliente {
  nombre: string;
  telefono: string;
  correo: string;
}

export interface DireccionEntrega {
  calle: string;
  numeroExterior: string;
  numeroInterior: string;
  colonia: string;
  codigoPostal: string;
  referencias: string;
}

export interface DatosEntrega {
  modalidad: ModalidadEntrega;
  fecha: string;
  horario: string;
  direccion: DireccionEntrega;
  // Costo de envío confirmado por la API para el código postal (0 al recoger en tienda)
  costoEnvio: number;
}

export interface DatosPago {
  metodo: MetodoPago;
  modalidad: ModalidadPago;
}

// ===================== Registro del pedido (RF-12) =====================

export interface ArticuloSolicitud {
  idPostre: number;
  // capa = capa del pastel (1 = Base)
  opciones: { idOpcion: number; cantidad: number; capa?: number }[];
  cantidad: number;
  dedicatoria: string;
  comentario: string;
  precioUnitario: number;
}

export interface SolicitudPedido {
  llaveIdempotencia: string;
  articulos: ArticuloSolicitud[];
  cliente: DatosCliente;
  entrega: {
    modalidad: ModalidadEntrega;
    fecha: string;
    horario: string;
    direccion: DireccionEntrega | null;
  };
  pago: DatosPago;
  totalMostrado: number;
}

export interface InstruccionesPago {
  titulo: string;
  pasos: string[];
  datosBancarios: string;
}

export interface ProductoComprobante {
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  dedicatoria: string;
  comentario: string;
}

export interface ComprobantePedido {
  folio: number;
  // true si el pedido ya se había registrado (doble envío): es el mismo folio
  repetido: boolean;
  estado: string;
  cliente: { nombre: string; telefono: string };
  productos: ProductoComprobante[];
  subtotalProductos: number;
  costoEnvio: number;
  montoTotal: number;
  anticipoRequerido: number;
  modalidadPago: ModalidadPago;
  metodoPago: MetodoPago;
  montoParaIniciar: number;
  entrega: {
    modalidad: string;
    fecha: string;
    horario: string;
    direccion: DireccionEntrega | null;
  };
  instrucciones: InstruccionesPago;
}

// Detalles del error PRECIO_CAMBIO: precios nuevos de cada renglón y total nuevo
export interface PreciosActualizados {
  articulos: { posicion: number; precioUnitario: number; precioCambio: boolean }[];
  costoEnvio: number;
  total: number;
}

// ===================== Seguimiento (RF-13) =====================

export interface PasoSeguimiento {
  estado: string;
  texto: string;
  completado: boolean;
  actual: boolean;
  fecha: string | null;
}

export interface ProductoSeguimiento {
  nombre: string;
  linkFotografia: string | null;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  dedicatoria: string | null;
  comentario: string | null;
  personalizacion: string[];
}

export interface PagoSeguimiento {
  monto: number;
  fecha: string;
  metodo: string;
  referencia: string | null;
  // Pendiente | Validado | Rechazado
  estado: string;
  motivoRechazo: string | null;
}

export interface SeguimientoPedido {
  folio: number;
  nombreCliente: string;
  fechaSolicitud: string;
  estado: string;
  estadoTexto: string;
  pasos: PasoSeguimiento[];
  cancelacion: { motivo: string; fecha: string; montoReembolso: number; estadoReembolso: string } | null;
  baja: { motivo: string; fecha: string } | null;
  entrega: {
    modalidad: string;
    fecha: string;
    horario: string;
    direccion: DireccionEntrega | null;
  };
  productos: ProductoSeguimiento[];
  costoEnvio: number;
  montoTotal: number;
  anticipoRequerido: number;
  modalidadPago: string;
  montoParaIniciar: number;
  totalPagado: number;
  porValidar: number;
  saldo: number;
  pagos: PagoSeguimiento[];
  puedeEnviarComprobante: boolean;
  motivoSinComprobante: string;
  datosBancarios: string;
}

// Foto o PDF del comprobante leído desde los archivos del cliente (en Base64)
export interface ArchivoComprobante {
  nombre: string;
  tipo: string;
  contenidoBase64: string;
}

// RF-14 · Paso 3: el cliente manda el enlace, el archivo o ambos (al menos uno)
export interface SolicitudComprobante {
  folio: number;
  // Uno de los dos: el teléfono o el correo con el que se encontró el pedido
  telefono: string;
  correo: string;
  monto: number;
  metodo: 'Transferencia';
  referencia: string;
  linkComprobante: string;
  archivo: ArchivoComprobante | null;
}

export interface RespuestaComprobante {
  mensaje: string;
  seguimiento: SeguimientoPedido;
}
