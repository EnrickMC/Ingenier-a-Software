/*
 * Validación de los datos que manda el navegador en el Portal de Pedidos.
 * Nunca se confía en el cliente: aquí se revisa forma, largo y caracteres de cada dato.
 * Los precios que manda el navegador NO se usan para cobrar: solo para avisar si cambiaron.
 *
 * Cada función regresa los datos limpios o lanza un ErrorApi 400 (DATOS_INVALIDOS).
 * Si el error es de un campo del formulario, "detalles" trae { campo, mensaje } para marcarlo en rojo.
 */
const ErrorApi = require('../../utilidades/ErrorApi');
const validacion = require('../../utilidades/validacion');
const catalogoControlador = require('../catalogo/catalogo.controlador');
const comprobanteArchivo = require('./comprobanteArchivo');

const ARTICULOS_MAXIMOS = 20;
const MODALIDADES_ENTREGA = ['Recoger en tienda', 'Envio a domicilio'];
const MODALIDADES_PAGO = ['Anticipo', 'Pago total'];
// Solo efectivo en la tienda o transferencia
const METODOS_PAGO = ['Efectivo', 'Transferencia'];
const PATRON_TELEFONO = /^[0-9]{10}$/;
const PATRON_CORREO = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
// Nombres: letras (con acentos), espacios, punto, apóstrofo y guion
const PATRON_NOMBRE = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ .'-]+$/;
const PATRON_FECHA = /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/;
const PATRON_HORA = /^[0-9]{2}:[0-9]{2}$/;
const PATRON_LLAVE = /^[A-Za-z0-9-]{16,64}$/;
const PATRON_FOLIO = /^[0-9]{1,9}$/;
const PATRON_ENLACE = /^https?:\/\/[^\s<>"']+$/;

// Error de un campo del formulario (la página lo marca en rojo)
function errorDeCampo(campo, mensaje) {
  return new ErrorApi(400, 'DATOS_INVALIDOS', mensaje, [{ campo: campo, mensaje: mensaje }]);
}

function esObjeto(valor) {
  return valor !== null && valor !== undefined && typeof valor === 'object' && !Array.isArray(valor);
}

// Texto obligatorio con largo máximo y caracteres permitidos
function textoObligatorio(valor, campo, etiqueta, largoMaximo) {
  if (typeof valor !== 'string' || valor.trim() === '') {
    throw errorDeCampo(campo, 'Indica ' + etiqueta);
  }
  return textoOpcional(valor, campo, etiqueta, largoMaximo);
}

function textoOpcional(valor, campo, etiqueta, largoMaximo) {
  try {
    return validacion.textoOpcional(valor, etiqueta, largoMaximo);
  } catch (error) {
    throw errorDeCampo(campo, error.message);
  }
}

// ===================== Datos del cliente =====================

// RF-10 · Paso 5 (API): valida campos obligatorios y formato de los datos de contacto.
function leerCliente(datos) {
  if (!esObjeto(datos)) {
    throw validacion.datoInvalido('Faltan los datos de contacto');
  }
  const nombre = textoObligatorio(datos.nombre, 'nombre', 'tu nombre completo', 100);
  if (nombre.length < 3 || !PATRON_NOMBRE.test(nombre)) {
    // RF-10 · Excepción paso 5: nombre inválido.
    throw errorDeCampo('nombre', 'Escribe tu nombre completo (solo letras y espacios)');
  }
  const telefono = leerTelefono(datos.telefono);
  let correo = '';
  if (datos.correo !== undefined && datos.correo !== null && datos.correo !== '') {
    if (typeof datos.correo !== 'string') {
      // RF-10 · Excepción paso 5: correo inválido.
      throw errorDeCampo('correo', 'Escribe un correo válido, por ejemplo nombre@correo.com');
    }
    correo = datos.correo.trim();
    if (correo.length > 100 || !PATRON_CORREO.test(correo)) {
      throw errorDeCampo('correo', 'Escribe un correo válido, por ejemplo nombre@correo.com');
    }
  }
  // Se guardan los espacios repetidos como uno solo
  return { nombre: nombre.replace(/\s+/g, ' '), telefono: telefono, correo: correo };
}

function leerTelefono(valor) {
  if (typeof valor !== 'string' || !PATRON_TELEFONO.test(valor.trim())) {
    // RF-10 · Excepción paso 5: teléfono inválido.
    throw errorDeCampo('telefono', 'El teléfono de WhatsApp debe tener 10 dígitos, sin espacios ni guiones');
  }
  return valor.trim();
}

// ===================== Entrega =====================

// RF-10 · Pasos 5 y 6 (API): método de entrega, fecha y horario.
function leerEntrega(datos) {
  if (!esObjeto(datos)) {
    throw validacion.datoInvalido('Faltan los datos de entrega');
  }
  if (MODALIDADES_ENTREGA.indexOf(datos.modalidad) < 0) {
    throw errorDeCampo('modalidad', 'Elige cómo quieres recibir tu pedido');
  }
  if (typeof datos.fecha !== 'string' || !PATRON_FECHA.test(datos.fecha)) {
    throw errorDeCampo('fecha', 'Indica la fecha de entrega');
  }
  if (typeof datos.horario !== 'string' || !PATRON_HORA.test(datos.horario)) {
    throw errorDeCampo('horario', 'Elige el horario de entrega');
  }
  const entrega = { modalidad: datos.modalidad, fecha: datos.fecha, horario: datos.horario, direccion: null };
  if (datos.modalidad === 'Envio a domicilio') {
    entrega.direccion = leerDireccion(datos.direccion);
  }
  return entrega;
}

// Mismos largos que la tabla Direccion
// RF-10 · Paso 5 (API): dirección de envío a domicilio.
function leerDireccion(datos) {
  if (!esObjeto(datos)) {
    throw errorDeCampo('calle', 'Indica la dirección de entrega');
  }
  const direccion = {
    calle: textoObligatorio(datos.calle, 'calle', 'la calle', 100),
    numeroExterior: textoObligatorio(datos.numeroExterior, 'numeroExterior', 'el número exterior', 10),
    numeroInterior: textoOpcional(datos.numeroInterior, 'numeroInterior', 'Número interior', 10),
    colonia: textoObligatorio(datos.colonia, 'colonia', 'la colonia', 80),
    codigoPostal: leerCodigoPostal(datos.codigoPostal),
    referencias: textoOpcional(datos.referencias, 'referencias', 'Referencias', 255)
  };
  return direccion;
}

function leerCodigoPostal(valor) {
  if (typeof valor !== 'string' || !/^[0-9]{5}$/.test(valor.trim())) {
    // RF-10 · Excepción paso 5: código postal inválido.
    throw errorDeCampo('codigoPostal', 'El código postal debe tener 5 dígitos');
  }
  return valor.trim();
}

// ===================== Pago =====================

// RF-11 · Paso 5 (API): valida que se haya seleccionado una modalidad y un método disponibles.
function leerPago(datos) {
  if (!esObjeto(datos)) {
    // RF-11 · Excepción paso 5: «Selecciona un método de pago».
    throw errorDeCampo('metodo', 'Selecciona un método de pago');
  }
  if (METODOS_PAGO.indexOf(datos.metodo) < 0) {
    throw errorDeCampo('metodo', 'Selecciona un método de pago');
  }
  if (MODALIDADES_PAGO.indexOf(datos.modalidad) < 0) {
    throw errorDeCampo('modalidad', 'Elige si pagas un anticipo o el total');
  }
  return { metodo: datos.metodo, modalidad: datos.modalidad };
}

// ===================== Artículos del carrito =====================

/*
 * Cada artículo se revisa con las mismas reglas de «Agregar al carrito» del catálogo
 * (leerDatosCotizacion): producto, opciones, cantidad (1 a 20), dedicatoria y comentario.
 * precioMostrado = precio por pieza que vio el cliente (solo para avisar si cambió).
 */
// RF-6 / RF-12 · Paso 2 (API): revisa los productos del carrito.
function leerArticulos(lista) {
  if (!Array.isArray(lista) || lista.length === 0) {
    // RF-6 / RF-10 / RF-11 / RF-12 · Excepción paso 2: «Tu carrito está vacío».
    throw new ErrorApi(400, 'CARRITO_VACIO', 'Tu carrito está vacío');
  }
  if (lista.length > ARTICULOS_MAXIMOS) {
    throw validacion.datoInvalido('Tu carrito tiene demasiados productos (máximo ' + ARTICULOS_MAXIMOS + ')');
  }
  const articulos = [];
  for (let i = 0; i < lista.length; i++) {
    const original = lista[i];
    if (!esObjeto(original)) {
      throw validacion.datoInvalido('Los productos del carrito no son válidos');
    }
    // El catálogo llama "precioMostrado" a lo que el carrito guarda como precioUnitario
    const copia = {
      idPostre: original.idPostre,
      opciones: original.opciones,
      cantidad: original.cantidad,
      dedicatoria: original.dedicatoria,
      comentario: original.comentario,
      precioMostrado: original.precioUnitario
    };
    const articulo = catalogoControlador.leerDatosCotizacion(copia);
    articulo.posicion = i;
    articulos.push(articulo);
  }
  return articulos;
}

// Monto en pesos que manda el navegador (total que vio el cliente); 0 si no viene
function leerMonto(valor) {
  if (typeof valor === 'number' && Number.isFinite(valor) && valor > 0 && valor < 10000000) {
    return Math.round(valor * 100) / 100;
  }
  return 0;
}

// ===================== Pedido completo =====================

// RF-12 · Paso 2 (API): la solicitud debe traer carrito, datos de entrega y método de pago.
function leerSolicitudPedido(cuerpo) {
  if (!esObjeto(cuerpo)) {
    throw validacion.datoInvalido('Los datos del pedido no son válidos');
  }
  if (typeof cuerpo.llaveIdempotencia !== 'string' || !PATRON_LLAVE.test(cuerpo.llaveIdempotencia)) {
    throw validacion.datoInvalido('Los datos del pedido no son válidos');
  }
  return {
    llaveIdempotencia: cuerpo.llaveIdempotencia,
    articulos: leerArticulos(cuerpo.articulos),
    cliente: leerCliente(cuerpo.cliente),
    entrega: leerEntrega(cuerpo.entrega),
    pago: leerPago(cuerpo.pago),
    totalMostrado: leerMonto(cuerpo.totalMostrado)
  };
}

// ===================== Seguimiento y comprobante =====================

// RF-13 · Paso 4 (API): formato del folio.
function leerFolio(valor) {
  let texto = '';
  if (typeof valor === 'string' || typeof valor === 'number') {
    texto = String(valor).trim().replace(/^#/, '');
  }
  if (!PATRON_FOLIO.test(texto) || Number(texto) <= 0) {
    // RF-13 · Excepción paso 4: el folio no tiene formato válido.
    throw errorDeCampo('folio', 'El folio es el número que recibiste al enviar tu pedido (solo números)');
  }
  return Number(texto);
}

// RF-13 · Paso 4 (API): folio y un dato de contacto: el teléfono de WhatsApp o el correo.
// Regresa { folio, telefono, correo }; de los dos datos de contacto solo uno trae texto.
function leerConsulta(cuerpo) {
  if (!esObjeto(cuerpo)) {
    throw validacion.datoInvalido('Escribe tu folio y tu teléfono o correo');
  }
  const folio = leerFolio(cuerpo.folio);
  // Si viene el teléfono se usa el teléfono; si no, el correo
  if (cuerpo.telefono !== undefined && cuerpo.telefono !== null && cuerpo.telefono !== '') {
    return { folio: folio, telefono: leerTelefono(cuerpo.telefono), correo: '' };
  }
  return { folio: folio, telefono: '', correo: leerCorreoConsulta(cuerpo.correo) };
}

// Correo para buscar un pedido (aquí sí es obligatorio)
function leerCorreoConsulta(valor) {
  if (typeof valor !== 'string' || valor.trim() === '') {
    // RF-13 · Excepción paso 4: no escribió teléfono ni correo.
    throw errorDeCampo('contacto', 'Escribe tu teléfono de WhatsApp o tu correo');
  }
  const correo = valor.trim();
  if (correo.length > 100 || !PATRON_CORREO.test(correo)) {
    // RF-13 · Excepción paso 4: formato de correo inválido.
    throw errorDeCampo('contacto', 'Escribe un correo válido, por ejemplo nombre@correo.com');
  }
  return correo;
}

// RF-14 · Paso 4 (API): valida los datos del comprobante de pago.
// El cliente puede pegar un enlace, subir una foto/PDF desde sus archivos, o ambas cosas (al menos una).
function leerComprobante(cuerpo) {
  const consulta = leerConsulta(cuerpo);
  // Solo transferencia: el efectivo se paga en la tienda.
  // Si la página no manda el método, se toma como transferencia.
  let metodo = cuerpo.metodo;
  if (metodo === undefined || metodo === null || metodo === '') {
    metodo = 'Transferencia';
  }
  if (metodo !== 'Transferencia') {
    throw errorDeCampo('metodo', 'Solo se reciben comprobantes de transferencia. El efectivo se paga en la tienda.');
  }
  const monto = leerMonto(cuerpo.monto);
  if (monto <= 0) {
    throw errorDeCampo('monto', 'El monto debe ser mayor a $0.00');
  }
  const referencia = textoObligatorio(cuerpo.referencia, 'referencia', 'la referencia o folio bancario', 50);

  // Enlace (opcional si viene archivo)
  let enlace = '';
  if (cuerpo.linkComprobante !== undefined && cuerpo.linkComprobante !== null) {
    if (typeof cuerpo.linkComprobante !== 'string') {
      throw errorDeCampo('linkComprobante', 'Pega el enlace de la imagen de tu comprobante');
    }
    enlace = cuerpo.linkComprobante.trim();
  }
  if (enlace !== '' && (enlace.length > 255 || !PATRON_ENLACE.test(enlace))) {
    throw errorDeCampo('linkComprobante', 'El enlace del comprobante debe empezar con http:// o https:// (máximo 255 caracteres)');
  }

  // Archivo subido desde el equipo del cliente (opcional si viene enlace)
  const archivo = comprobanteArchivo.leerArchivo(cuerpo.archivo);

  // RF-14 · Excepción paso 4: no se mandó ni enlace ni archivo.
  if (enlace === '' && archivo === null) {
    throw errorDeCampo('archivo', 'Sube la foto de tu comprobante o pega el enlace donde la guardaste');
  }

  let linkComprobante = null;
  if (enlace !== '') {
    linkComprobante = enlace;
  }
  return {
    folio: consulta.folio,
    telefono: consulta.telefono,
    correo: consulta.correo,
    metodo: metodo,
    monto: monto,
    referencia: referencia,
    linkComprobante: linkComprobante,
    archivo: archivo
  };
}

module.exports = {
  errorDeCampo: errorDeCampo,
  leerArticulos: leerArticulos,
  leerCodigoPostal: leerCodigoPostal,
  leerSolicitudPedido: leerSolicitudPedido,
  leerConsulta: leerConsulta,
  leerComprobante: leerComprobante,
  leerMonto: leerMonto
};
