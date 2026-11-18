/*
 * Enlaces de las fotos de postres y opciones (RF-1, RF-2 y RF-3).
 *
 * En el Panel Administrativo la foto puede ser:
 *  - un enlace de internet (http:// o https://): se manda tal cual a la página web.
 *  - un archivo de la computadora (C:\...\pastel.jpg o file:/C:/...): el navegador no puede
 *    abrir archivos de la computadora desde una página web, así que la API lo sirve en
 *    /api/imagenes/postre/:id (o /opcion/:id) y la página usa ese enlace.
 *  - una ruta relativa /imagenes/... (en AWS): la foto está en el bucket de S3 y la sirve
 *    CloudFront en el mismo dominio de la página, así que se manda tal cual.
 */
const path = require('path');
const url = require('url');

// Formatos que acepta el Panel (SelectorImagen.EXTENSIONES)
const EXTENSIONES_IMAGEN = ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'];

function esEnlaceInternet(texto) {
  const minusculas = texto.toLowerCase();
  return minusculas.startsWith('http://') || minusculas.startsWith('https://');
}

// Carpeta de las fotos en S3 (la misma ruta que tiene el comportamiento /imagenes/* de CloudFront)
const PREFIJO_S3 = '/imagenes/';

// true si es una foto subida a S3 (ruta relativa /imagenes/...)
function esRutaS3(texto) {
  return texto.startsWith(PREFIJO_S3);
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
  if (esRutaS3(limpio)) {
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
  // Las fotos de S3 no son archivos de esta computadora (en Linux "/imagenes/..." parecería una ruta absoluta)
  if (ruta === '' || esEnlaceInternet(ruta) || esRutaS3(ruta)) {
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
