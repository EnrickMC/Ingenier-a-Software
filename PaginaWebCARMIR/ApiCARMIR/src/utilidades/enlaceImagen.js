/*
 * Enlaces de las fotos de postres y opciones (RF-1, RF-2 y RF-3).
 *
 * En el Panel Administrativo la foto puede ser:
 *  - un enlace de internet (http:// o https://): se manda tal cual a la página web.
 *  - un archivo de la computadora (C:\...\pastel.jpg o file:/C:/...): el navegador no puede
 *    abrir archivos de la computadora desde una página web, así que la API lo sirve en
 *    /api/imagenes/postre/:id (o /opcion/:id) y la página usa ese enlace.
 */
const path = require('path');
const url = require('url');

// Formatos que acepta el Panel (SelectorImagen.EXTENSIONES)
const EXTENSIONES_IMAGEN = ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'];

function esEnlaceInternet(texto) {
  const minusculas = texto.toLowerCase();
  return minusculas.startsWith('http://') || minusculas.startsWith('https://');
}

// Enlace que se manda a la página web (null si no hay foto)
function enlaceParaWeb(link, tipo, id) {
  if (link === null || link === undefined) {
    return null;
  }
  const limpio = String(link).trim();
  if (limpio === '') {
    return null;
  }
  if (esEnlaceInternet(limpio)) {
    return limpio;
  }
  // Archivo de la computadora: lo sirve la API
  return '/api/imagenes/' + tipo + '/' + id;
}

// Ruta del archivo en la computadora, o null si el texto no es un archivo de imagen válido
function rutaArchivo(link) {
  if (link === null || link === undefined) {
    return null;
  }
  let ruta = String(link).trim();
  if (ruta === '' || esEnlaceInternet(ruta)) {
    return null;
  }
  // El Panel puede guardar la ruta como file:/C:/... (File.toURI)
  if (ruta.toLowerCase().startsWith('file:')) {
    try {
      ruta = url.fileURLToPath(ruta);
    } catch (error) {
      return null;
    }
  }
  if (!path.isAbsolute(ruta)) {
    return null;
  }
  const extension = path.extname(ruta).toLowerCase();
  if (EXTENSIONES_IMAGEN.indexOf(extension) < 0) {
    return null;
  }
  return ruta;
}

module.exports = {
  enlaceParaWeb: enlaceParaWeb,
  rutaArchivo: rutaArchivo
};
