/*
 * Middlewares de seguridad que se aplican a todas las peticiones:
 *  - helmet: cabeceras HTTP seguras.
 *  - cors: solo la página web de CARMIR (los orígenes del .env) puede llamar a la API desde el navegador.
 *  - límite de peticiones: evita que alguien sature la API.
 */
const helmet = require('helmet');
const cors = require('cors');
const limiteDePeticiones = require('express-rate-limit').rateLimit;
const configuracion = require('../config/configuracion');
const ErrorApi = require('../utilidades/ErrorApi');

// Decide si un origen (la página que hace la petición) está permitido
function revisarOrigen(origen, responder) {
  // Sin origen = no viene de un navegador (curl, pruebas, el mismo servidor): se permite
  if (!origen) {
    responder(null, true);
    return;
  }
  if (configuracion.servidor.origenesPermitidos.indexOf(origen) >= 0) {
    responder(null, true);
    return;
  }
  responder(new ErrorApi(403, 'ORIGEN_NO_PERMITIDO', 'Origen no permitido'));
}

const opcionesCors = {
  origin: revisarOrigen,
  // La API solo usa GET (leer) y POST (cotizar, registrar pedidos y comprobantes)
  methods: ['GET', 'POST'],
  allowedHeaders: ['Content-Type'],
  maxAge: 600
};

// Respuesta cuando se pasa el límite de peticiones
function demasiadasPeticiones(peticion, respuesta) {
  respuesta.status(429).json({
    codigo: 'DEMASIADAS_PETICIONES',
    mensaje: 'Hiciste demasiadas solicitudes. Espera un momento e intenta de nuevo.'
  });
}

function crearLimite(peticionesPorMinuto) {
  return limiteDePeticiones({
    windowMs: 60 * 1000,
    limit: peticionesPorMinuto,
    standardHeaders: 'draft-7',
    legacyHeaders: false,
    handler: demasiadasPeticiones
  });
}

// ===================== Límites del Módulo 2 (pedidos) =====================

// Límite por ventana de tiempo (minutos) con un mensaje propio
function crearLimitePorMinutos(peticiones, minutos, codigo, mensaje) {
  function responder(peticion, respuesta) {
    respuesta.status(429).json({ codigo: codigo, mensaje: mensaje });
  }
  return limiteDePeticiones({
    windowMs: minutos * 60 * 1000,
    limit: peticiones,
    standardHeaders: 'draft-7',
    legacyHeaders: false,
    handler: responder
  });
}

// Minutos que faltan para que se libere el bloqueo (al menos 1)
function minutosDeEspera(peticion) {
  let minutos = 1;
  if (peticion.rateLimit && peticion.rateLimit.resetTime) {
    const milisegundos = peticion.rateLimit.resetTime.getTime() - Date.now();
    minutos = Math.ceil(milisegundos / 60000);
  }
  if (minutos < 1) {
    minutos = 1;
  }
  return minutos;
}

// RF-12: búsqueda de pedidos. Solo cuentan los intentos FALLIDOS (folio y teléfono que no coinciden);
// a los 5 fallos se bloquea 15 minutos y se dice cuánto falta (sin decir qué dato falló).
// RF-12 · Excepción paso 5: búsqueda bloqueada por intentos fallidos; se indica el tiempo de espera.
function bloqueoPorIntentos(peticion, respuesta) {
  const minutos = minutosDeEspera(peticion);
  respuesta.status(429).json({
    codigo: 'BUSQUEDA_BLOQUEADA',
    mensaje: 'Hiciste demasiados intentos. Espera ' + minutos + ' minuto(s) para volver a buscar tu pedido.',
    detalles: { minutosEspera: minutos }
  });
}

// Solo es "intento fallido" cuando el folio y el teléfono no coinciden (404).
// Un error de formato o un monto mal escrito en el comprobante no gasta intentos.
function noFueIntentoFallido(peticion, respuesta) {
  return respuesta.statusCode !== 404;
}

const limiteConsultaPedido = limiteDePeticiones({
  windowMs: 15 * 60 * 1000,
  limit: 5,
  skipSuccessfulRequests: true,
  requestWasSuccessful: noFueIntentoFallido,
  standardHeaders: 'draft-7',
  legacyHeaders: false,
  handler: bloqueoPorIntentos
});

module.exports = {
  cabecerasSeguras: helmet(),
  cors: cors(opcionesCors),
  limiteGeneral: crearLimite(configuracion.servidor.peticionesPorMinuto),
  // Cotizar hace más trabajo, así que tiene un límite menor
  limiteCotizar: crearLimite(60),
  // Revisar el carrito y la cobertura (se llaman al abrir pantallas)
  limiteValidarCarrito: crearLimite(60),
  limiteCobertura: crearLimite(30),
  // Registrar pedidos: 10 por hora desde la misma conexión (evita pedidos falsos en masa)
  limiteRegistrarPedido: crearLimitePorMinutos(configuracion.servidor.pedidosPorHora, 60, 'DEMASIADOS_PEDIDOS',
    'Hiciste demasiados pedidos seguidos. Espera un momento o comunícate con la repostería.'),
  // Subir comprobantes: 10 por hora
  limiteComprobante: crearLimitePorMinutos(10, 60, 'DEMASIADAS_PETICIONES',
    'Hiciste demasiadas solicitudes. Espera un momento e intenta de nuevo.'),
  limiteConsultaPedido: limiteConsultaPedido
};
