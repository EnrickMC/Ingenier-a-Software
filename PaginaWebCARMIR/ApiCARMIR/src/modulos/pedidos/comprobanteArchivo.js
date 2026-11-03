/*
 * Revisión del archivo de comprobante que el cliente sube desde «Mis pedidos» (RF-12).
 * La página manda el archivo en JSON: { nombre, tipo, contenidoBase64 }.
 *
 * Seguridad (nunca se confía en el navegador):
 *  - Solo se aceptan imágenes JPG, PNG o WEBP y documentos PDF.
 *  - El tipo se decide por los primeros bytes del archivo ("firma"), no por el nombre ni por lo que diga el navegador.
 *    Así un .exe renombrado a .jpg se rechaza.
 *  - Tamaño máximo 5 MB.
 *  - El nombre se limpia (sin rutas ni símbolos) y solo se guarda como referencia.
 *  - El archivo se guarda en la base de datos (tabla ComprobanteArchivo), no en una carpeta pública:
 *    nadie puede abrirlo desde internet; solo el Panel lo lee para validarlo.
 */
const ErrorApi = require('../../utilidades/ErrorApi');

// 5 MB
const TAMANO_MAXIMO = 5 * 1024 * 1024;
const TEXTO_TAMANO_MAXIMO = '5 MB';
const LARGO_NOMBRE = 120;
// Solo caracteres de Base64 (letras, números, + / y el relleno =)
const PATRON_BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

// Tipos que se aceptan (se guardan en ComprobanteArchivo.TipoContenido)
const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];

function errorArchivo(mensaje) {
  return new ErrorApi(400, 'DATOS_INVALIDOS', mensaje, [{ campo: 'archivo', mensaje: mensaje }]);
}

// Revisa si los bytes del inicio del archivo coinciden con la lista dada
function empiezaCon(datos, bytes, desde) {
  if (datos.length < desde + bytes.length) {
    return false;
  }
  for (let i = 0; i < bytes.length; i++) {
    if (datos[desde + i] !== bytes[i]) {
      return false;
    }
  }
  return true;
}

// Tipo real del archivo según su firma. '' = no es un tipo permitido.
function detectarTipo(datos) {
  // JPG: FF D8 FF
  if (empiezaCon(datos, [0xff, 0xd8, 0xff], 0)) {
    return 'image/jpeg';
  }
  // PNG: 89 50 4E 47 0D 0A 1A 0A
  if (empiezaCon(datos, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a], 0)) {
    return 'image/png';
  }
  // WEBP: "RIFF" ···· "WEBP"
  if (empiezaCon(datos, [0x52, 0x49, 0x46, 0x46], 0) && empiezaCon(datos, [0x57, 0x45, 0x42, 0x50], 8)) {
    return 'image/webp';
  }
  // PDF: "%PDF-"
  if (empiezaCon(datos, [0x25, 0x50, 0x44, 0x46, 0x2d], 0)) {
    return 'application/pdf';
  }
  return '';
}

// Deja solo letras, números, punto, guion y guion bajo; sin rutas (C:\..., ../)
function limpiarNombre(nombre, tipo) {
  let texto = '';
  if (typeof nombre === 'string') {
    texto = nombre;
  }
  // Solo la última parte de la ruta
  const partes = texto.split(/[\\/]/);
  texto = partes[partes.length - 1];
  texto = texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  texto = texto.replace(/[^A-Za-z0-9._-]+/g, '_');
  texto = texto.replace(/^[._]+/, '');
  if (texto.length > LARGO_NOMBRE) {
    texto = texto.substring(texto.length - LARGO_NOMBRE);
  }
  if (texto === '') {
    texto = 'comprobante' + extensionDe(tipo);
  }
  return texto;
}

function extensionDe(tipo) {
  if (tipo === 'image/jpeg') {
    return '.jpg';
  }
  if (tipo === 'image/png') {
    return '.png';
  }
  if (tipo === 'image/webp') {
    return '.webp';
  }
  return '.pdf';
}

/*
 * Lee y revisa el archivo. Regresa null si no se mandó archivo,
 * o { nombre, tipo, tamano, contenido (Buffer) } si es válido.
 */
// RF-12 (API): revisa la foto o PDF del comprobante subido desde los archivos del cliente.
function leerArchivo(valor) {
  if (valor === undefined || valor === null) {
    return null;
  }
  if (typeof valor !== 'object' || Array.isArray(valor) || typeof valor.contenidoBase64 !== 'string') {
    throw errorArchivo('El archivo del comprobante no es válido');
  }
  const base64 = valor.contenidoBase64.trim();
  // RF-12 · Excepción: el archivo pesa más de 5 MB (se revisa antes de convertirlo).
  if (base64.length > Math.ceil((TAMANO_MAXIMO * 4) / 3) + 4) {
    throw errorArchivo('El archivo pesa más de ' + TEXTO_TAMANO_MAXIMO + '. Toma una foto más ligera o recórtala.');
  }
  if (base64 === '' || !PATRON_BASE64.test(base64)) {
    throw errorArchivo('El archivo del comprobante no es válido');
  }
  const contenido = Buffer.from(base64, 'base64');
  if (contenido.length === 0) {
    throw errorArchivo('El archivo del comprobante está vacío');
  }
  if (contenido.length > TAMANO_MAXIMO) {
    throw errorArchivo('El archivo pesa más de ' + TEXTO_TAMANO_MAXIMO + '. Toma una foto más ligera o recórtala.');
  }
  // RF-12 · Excepción: el archivo no es imagen JPG/PNG/WEBP ni PDF (se revisa su contenido real).
  const tipo = detectarTipo(contenido);
  if (tipo === '') {
    throw errorArchivo('Solo se aceptan fotos JPG, PNG o WEBP, o un PDF');
  }
  return {
    nombre: limpiarNombre(valor.nombre, tipo),
    tipo: tipo,
    tamano: contenido.length,
    contenido: contenido
  };
}

module.exports = {
  TAMANO_MAXIMO: TAMANO_MAXIMO,
  TIPOS_PERMITIDOS: TIPOS_PERMITIDOS,
  detectarTipo: detectarTipo,
  limpiarNombre: limpiarNombre,
  leerArchivo: leerArchivo
};
