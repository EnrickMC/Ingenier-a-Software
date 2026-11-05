/*
 * Controlador de Imágenes: entrega la foto de un postre u opción que en el Panel
 * se eligió como archivo de la computadora (ver utilidades/enlaceImagen.js).
 */
const fs = require('fs');
const repositorio = require('./imagenes.repositorio');
const enlaceImagen = require('../../utilidades/enlaceImagen');
const validacion = require('../../utilidades/validacion');
const ErrorApi = require('../../utilidades/ErrorApi');

const MENSAJE_SIN_IMAGEN = 'La imagen no está disponible';

// Manda el archivo de imagen, o 404 si no existe (la página muestra la imagen genérica)
function enviarArchivo(link, respuesta, siguiente) {
  const ruta = enlaceImagen.rutaArchivo(link);
  if (ruta === null || !fs.existsSync(ruta)) {
    siguiente(new ErrorApi(404, 'IMAGEN_NO_DISPONIBLE', MENSAJE_SIN_IMAGEN));
    return;
  }
  // La página (otro puerto) debe poder mostrar la imagen
  respuesta.set('Cross-Origin-Resource-Policy', 'cross-origin');
  respuesta.sendFile(ruta, { maxAge: 5 * 60 * 1000 }, function alTerminar(error) {
    if (error && !respuesta.headersSent) {
      siguiente(new ErrorApi(404, 'IMAGEN_NO_DISPONIBLE', MENSAJE_SIN_IMAGEN));
    }
  });
}

// RF-1 · Paso 4 (API): foto del postre.
// GET /api/imagenes/postre/:id
async function fotoPostre(peticion, respuesta, siguiente) {
  try {
    const idPostre = validacion.enteroPositivo(peticion.params.id, 'producto');
    const link = await repositorio.linkDePostre(idPostre);
    enviarArchivo(link, respuesta, siguiente);
  } catch (error) {
    siguiente(error);
  }
}

// RF-3 · Paso 3 (API): imagen de una opción.
// GET /api/imagenes/opcion/:id
async function imagenOpcion(peticion, respuesta, siguiente) {
  try {
    const idOpcion = validacion.enteroPositivo(peticion.params.id, 'opción');
    const link = await repositorio.linkDeOpcion(idOpcion);
    enviarArchivo(link, respuesta, siguiente);
  } catch (error) {
    siguiente(error);
  }
}

module.exports = {
  fotoPostre: fotoPostre,
  imagenOpcion: imagenOpcion
};
