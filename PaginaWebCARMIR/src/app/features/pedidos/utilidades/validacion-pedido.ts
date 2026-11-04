/**
 * Validaciones de los formularios del Portal de Pedidos (las mismas reglas que la API).
 * Cada función regresa el mensaje de error o "" si el dato es correcto.
 * La API vuelve a validar todo: esto solo sirve para avisar rápido al cliente.
 */
import { validarTexto } from '../../catalogo/utilidades/reglas-configuracion';

const PATRON_NOMBRE = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ .'-]+$/;
const PATRON_TELEFONO = /^[0-9]{10}$/;
const PATRON_CORREO = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PATRON_CODIGO_POSTAL = /^[0-9]{5}$/;
const PATRON_FOLIO = /^#?[0-9]{1,9}$/;
const PATRON_ENLACE = /^https?:\/\/[^\s<>"']+$/;

// RF-10 · Paso 5: validaciones de formato de los datos de contacto y entrega.
export function validarNombre(texto: string): string {
  const limpio = texto.trim();
  if (limpio === '') {
    return 'Indica tu nombre completo';
  }
  if (limpio.length < 3 || limpio.length > 100 || !PATRON_NOMBRE.test(limpio)) {
    return 'Escribe tu nombre completo (solo letras y espacios)';
  }
  return '';
}

export function validarTelefono(texto: string): string {
  if (!PATRON_TELEFONO.test(texto.trim())) {
    return 'El teléfono de WhatsApp debe tener 10 dígitos, sin espacios ni guiones';
  }
  return '';
}

// El correo es opcional
export function validarCorreo(texto: string): string {
  const limpio = texto.trim();
  if (limpio === '') {
    return '';
  }
  if (limpio.length > 100 || !PATRON_CORREO.test(limpio)) {
    return 'Escribe un correo válido, por ejemplo nombre@correo.com';
  }
  return '';
}

// RF-13: el cliente busca su pedido con el teléfono de WhatsApp o con el correo que registró
export interface ContactoConsulta {
  telefono: string;
  correo: string;
}

// Si el texto trae @ es un correo; si no, es el teléfono (sin espacios, guiones ni paréntesis)
export function contactoConsulta(texto: string): ContactoConsulta {
  const limpio = texto.trim();
  if (limpio.indexOf('@') >= 0) {
    return { telefono: '', correo: limpio };
  }
  return { telefono: limpio.replace(/[\s()-]/g, ''), correo: '' };
}

// RF-13 · Paso 4: el dato de contacto es un teléfono de 10 dígitos o un correo válido
export function validarContactoConsulta(texto: string): string {
  if (texto.trim() === '') {
    return 'Escribe tu teléfono de WhatsApp o tu correo';
  }
  const contacto = contactoConsulta(texto);
  if (contacto.correo !== '') {
    return validarCorreo(contacto.correo);
  }
  if (validarTelefono(contacto.telefono) !== '') {
    return 'Escribe tu teléfono de WhatsApp (10 dígitos) o tu correo';
  }
  return '';
}

export function validarCodigoPostal(texto: string): string {
  if (!PATRON_CODIGO_POSTAL.test(texto.trim())) {
    return 'El código postal debe tener 5 dígitos';
  }
  return '';
}

// Texto obligatorio u opcional con largo máximo
export function validarTextoCampo(texto: string, etiqueta: string, largoMaximo: number, obligatorio: boolean): string {
  const limpio = texto.trim();
  if (limpio === '') {
    if (obligatorio) {
      return 'Indica ' + etiqueta;
    }
    return '';
  }
  // Mismo largo y caracteres permitidos que la dedicatoria del catálogo (y que la API)
  return validarTexto(limpio, etiquetaConMayuscula(etiqueta), largoMaximo);
}

// "la calle" → "La calle"
function etiquetaConMayuscula(etiqueta: string): string {
  return etiqueta.charAt(0).toUpperCase() + etiqueta.substring(1);
}

// RF-13 · Paso 3: validación del folio.
export function validarFolio(texto: string): string {
  if (!PATRON_FOLIO.test(texto.trim()) || Number(texto.trim().replace('#', '')) <= 0) {
    return 'El folio es el número que recibiste al enviar tu pedido (solo números)';
  }
  return '';
}

// RF-14 · Paso 4: validaciones del comprobante de pago.
export function validarMontoComprobante(monto: number, maximo: number): string {
  if (!Number.isFinite(monto) || monto <= 0) {
    return 'El monto debe ser mayor a $0.00';
  }
  if (monto > maximo + 0.004) {
    return 'El monto no puede ser mayor a lo que falta por pagar';
  }
  return '';
}

export function validarEnlace(texto: string): string {
  const limpio = texto.trim();
  if (limpio === '') {
    return 'Pega el enlace de la imagen de tu comprobante';
  }
  if (limpio.length > 255 || !PATRON_ENLACE.test(limpio)) {
    return 'El enlace del comprobante debe empezar con http:// o https:// (máximo 255 caracteres)';
  }
  return '';
}

// ===================== Archivo del comprobante (RF-14) =====================

// Mismas reglas que ApiCARMIR/src/modulos/pedidos/comprobanteArchivo.js
export const TAMANO_MAXIMO_COMPROBANTE = 5 * 1024 * 1024;
export const TIPOS_COMPROBANTE = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
// Para el selector de archivos del navegador
export const ACEPTA_COMPROBANTE = '.jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf';

// Algunos navegadores no dicen el tipo del archivo: se toma de la extensión
export function tipoDeArchivo(nombre: string, tipoNavegador: string): string {
  if (tipoNavegador !== '') {
    return tipoNavegador;
  }
  const minusculas = nombre.toLowerCase();
  if (minusculas.endsWith('.jpg') || minusculas.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (minusculas.endsWith('.png')) {
    return 'image/png';
  }
  if (minusculas.endsWith('.webp')) {
    return 'image/webp';
  }
  if (minusculas.endsWith('.pdf')) {
    return 'application/pdf';
  }
  return '';
}

// RF-14 · Paso 4: revisa tipo y tamaño de la foto o PDF antes de subirlo (la API revisa además su contenido real).
export function validarArchivoComprobante(tipo: string, tamano: number): string {
  if (TIPOS_COMPROBANTE.indexOf(tipo) < 0) {
    return 'Solo se aceptan fotos JPG, PNG o WEBP, o un PDF';
  }
  if (tamano <= 0) {
    return 'El archivo está vacío';
  }
  if (tamano > TAMANO_MAXIMO_COMPROBANTE) {
    return 'El archivo pesa más de 5 MB. Toma una foto más ligera o recórtala.';
  }
  return '';
}

// "1.2 MB" o "350 KB"
export function textoTamano(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return (Math.round((bytes / (1024 * 1024)) * 10) / 10).toString() + ' MB';
  }
  return Math.max(1, Math.round(bytes / 1024)).toString() + ' KB';
}

// Convierte los bytes del archivo a texto Base64 para mandarlo en JSON.
// Se hace por bloques para no saturar el navegador con archivos grandes.
export function bytesABase64(bytes: Uint8Array): string {
  let binario = '';
  const bloque = 32768;
  for (let inicio = 0; inicio < bytes.length; inicio = inicio + bloque) {
    const parte = bytes.subarray(inicio, inicio + bloque);
    binario = binario + String.fromCharCode.apply(null, Array.from(parte));
  }
  return btoa(binario);
}

// Redondeo a centavos
export function redondear(valor: number): number {
  return Math.round(valor * 100) / 100;
}

// Anticipo con el mismo cálculo que la API
export function calcularAnticipo(total: number, porcentaje: number, modalidad: string): number {
  if (modalidad === 'Pago total') {
    return total;
  }
  return Math.round(total * porcentaje) / 100;
}
