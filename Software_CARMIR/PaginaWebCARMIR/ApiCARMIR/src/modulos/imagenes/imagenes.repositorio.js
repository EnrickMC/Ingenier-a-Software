/*
 * Repositorio de Imágenes: lee de la base el enlace guardado de la foto.
 * Solo SELECT. El archivo que se sirve SIEMPRE sale de la base, nunca de la dirección que manda el navegador.
 */
const baseDatos = require('../../config/baseDatos');

async function linkDePostre(idPostre) {
  const filas = await baseDatos.consultar('SELECT LinkFotografia AS Link FROM Postre WHERE IdPostre = ?', [idPostre]);
  if (filas.length === 0) {
    return null;
  }
  return filas[0].Link;
}

async function linkDeOpcion(idOpcion) {
  const filas = await baseDatos.consultar('SELECT LinkImagen AS Link FROM OpcionCaracteristica WHERE IdOpcion = ?', [idOpcion]);
  if (filas.length === 0) {
    return null;
  }
  return filas[0].Link;
}

module.exports = {
  linkDePostre: linkDePostre,
  linkDeOpcion: linkDeOpcion
};
