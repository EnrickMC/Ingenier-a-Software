/*
 * Manejo de errores de toda la API.
 * Regla de seguridad: al cliente NUNCA se le manda el error interno (SQL, rutas, contraseñas...).
 * El detalle solo se escribe en la consola del servidor con un número de referencia.
 */
const crypto = require('crypto');
const ErrorApi = require('../utilidades/ErrorApi');

// Códigos de mysql2 que significan "no hay conexión con la base de datos"
const ERRORES_DE_CONEXION = [
  'ECONNREFUSED',
  'ENOTFOUND',
  'ETIMEDOUT',
  'ECONNRESET',
  'EHOSTUNREACH',
  'PROTOCOL_CONNECTION_LOST',
  'ER_ACCESS_DENIED_ERROR',
  'ER_ACCESS_DENIED_NO_PASSWORD_ERROR',
  'ER_DBACCESS_DENIED_ERROR',
  'ER_NOT_SUPPORTED_AUTH_MODE',
  'ER_BAD_DB_ERROR',
  'ER_CON_COUNT_ERROR',
  'POOL_ENQUEUELIMIT',
  'POOL_CLOSED'
];

function esErrorDeConexion(error) {
  if (error && error.code) {
    return ERRORES_DE_CONEXION.indexOf(error.code) >= 0;
  }
  return false;
}

// Para rutas que no existen
function rutaNoEncontrada(peticion, respuesta) {
  respuesta.status(404).json({
    codigo: 'RUTA_NO_ENCONTRADA',
    mensaje: 'La dirección solicitada no existe'
  });
}

// Express reconoce este middleware como manejador de errores porque recibe 4 parámetros
// eslint-disable-next-line no-unused-vars
// Excepciones comunes de todos los RF de la web: aquí se convierte cualquier error en la respuesta { codigo, mensaje }.
function manejarErrores(error, peticion, respuesta, siguiente) {
  // 1. Errores que nosotros mismos lanzamos (validación, producto no disponible...)
  if (error instanceof ErrorApi) {
    const cuerpo = { codigo: error.codigo, mensaje: error.message };
    if (error.detalles !== null) {
      cuerpo.detalles = error.detalles;
    }
    respuesta.status(error.estadoHttp).json(cuerpo);
    return;
  }

  // 2. JSON mal escrito o demasiado grande en el cuerpo de la petición
  if (error && error.type === 'entity.parse.failed') {
    respuesta.status(400).json({ codigo: 'JSON_INVALIDO', mensaje: 'Los datos enviados no tienen un formato válido' });
    return;
  }
  if (error && error.type === 'entity.too.large') {
    // RF-12 · Excepción: la foto o PDF del comprobante es demasiado grande
    if (peticion.originalUrl.indexOf('/comprobante') >= 0) {
      const mensaje = 'El archivo pesa más de 5 MB. Toma una foto más ligera o recórtala.';
      respuesta.status(413).json({
        codigo: 'DATOS_INVALIDOS',
        mensaje: mensaje,
        detalles: [{ campo: 'archivo', mensaje: mensaje }]
      });
      return;
    }
    respuesta.status(413).json({ codigo: 'DATOS_DEMASIADO_GRANDES', mensaje: 'Los datos enviados son demasiado grandes' });
    return;
  }

  // 3. Errores inesperados: se registran con una referencia y al cliente solo se le da la referencia
  const referencia = crypto.randomUUID().substring(0, 8);
  console.error('[ERROR ' + referencia + '] ' + peticion.method + ' ' + peticion.originalUrl);
  console.error(error);

  // Excepción «sin conexión con la base de datos» (RF-1, RF-2, RF-3, RF-4, RF-5, RF-11, RF-12 paso 2): responde 503.
  if (esErrorDeConexion(error)) {
    respuesta.status(503).json({
      codigo: 'SIN_CONEXION_BD',
      mensaje: 'No fue posible conectar con el servidor. Intenta de nuevo en unos momentos.',
      referencia: referencia
    });
    return;
  }

  respuesta.status(500).json({
    codigo: 'ERROR_INTERNO',
    mensaje: 'Ocurrió un error inesperado. Intenta de nuevo más tarde.',
    referencia: referencia
  });
}

module.exports = {
  rutaNoEncontrada: rutaNoEncontrada,
  manejarErrores: manejarErrores,
  esErrorDeConexion: esErrorDeConexion
};
