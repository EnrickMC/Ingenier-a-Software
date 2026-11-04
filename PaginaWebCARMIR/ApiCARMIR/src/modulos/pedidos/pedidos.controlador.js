/*
 * Controlador del Portal de Pedidos (Módulo 2).
 * Lee y valida los datos de la petición, llama al servicio y responde en JSON.
 * Los errores se pasan a "siguiente(error)" para que los atienda manejoErrores.js.
 */
const servicio = require('./pedidos.servicio');
const validacionPedido = require('./pedidos.validacion');
const validacion = require('../../utilidades/validacion');

function cuerpoValido(cuerpo) {
  if (cuerpo === undefined || cuerpo === null || typeof cuerpo !== 'object' || Array.isArray(cuerpo)) {
    throw validacion.datoInvalido('Los datos enviados no son válidos');
  }
  return cuerpo;
}

// POST /api/carrito/validar  { articulos: [...] }   (RF-6: disponibilidad y precio vigente)
// RF-6 (API): inicia aquí. Paso 4: valida en la BD cada producto, su precio y sus opciones.
async function validarCarrito(peticion, respuesta, siguiente) {
  try {
    const cuerpo = cuerpoValido(peticion.body);
    const articulos = validacionPedido.leerArticulos(cuerpo.articulos);
    const resultado = await servicio.validarCarrito(articulos);
    respuesta.json(resultado);
  } catch (error) {
    siguiente(error);
  }
}

// GET /api/entrega/reglas   (RF-10 y RF-11: fechas, horarios, anticipo y métodos de pago)
// RF-10 · Paso 3 / RF-11 · Paso 3 (API): reglas de entrega y de pago.
async function leerReglas(peticion, respuesta, siguiente) {
  try {
    const reglas = await servicio.leerReglasEntrega();
    respuesta.json(reglas);
  } catch (error) {
    siguiente(error);
  }
}

// GET /api/entrega/cobertura/:cp   (RF-10 paso 7)
// RF-10 · Paso 7 (API): cobertura del código postal y costo de envío.
async function revisarCobertura(peticion, respuesta, siguiente) {
  try {
    const codigoPostal = validacionPedido.leerCodigoPostal(peticion.params.cp);
    const cobertura = await servicio.revisarCobertura(codigoPostal);
    respuesta.json(cobertura);
  } catch (error) {
    siguiente(error);
  }
}

// POST /api/pedidos   (RF-12: registrar el pedido)
// RF-12 (API): inicia aquí. Paso 2: revisa que vengan carrito, datos de entrega y método de pago.
async function registrarPedido(peticion, respuesta, siguiente) {
  try {
    const solicitud = validacionPedido.leerSolicitudPedido(peticion.body);
    const comprobante = await servicio.registrarPedido(solicitud);
    // RF-12 · Excepción paso 6: doble envío; se regresa el comprobante con el folio ya generado.
    // 201 = se creó el pedido; 200 = ya existía (doble envío) y se regresa el mismo folio
    if (comprobante.repetido) {
      respuesta.status(200).json(comprobante);
    } else {
      respuesta.status(201).json(comprobante);
    }
  } catch (error) {
    siguiente(error);
  }
}

// POST /api/pedidos/consulta  { folio, telefono } o { folio, correo }   (RF-13: seguimiento)
// RF-13 (API): inicia aquí. Paso 3: valida folio y teléfono (o correo).
async function consultarPedido(peticion, respuesta, siguiente) {
  try {
    const datos = validacionPedido.leerConsulta(peticion.body);
    const seguimiento = await servicio.consultarPedido(datos.folio, { telefono: datos.telefono, correo: datos.correo });
    respuesta.json(seguimiento);
  } catch (error) {
    siguiente(error);
  }
}

// POST /api/pedidos/comprobante  { folio, telefono o correo, monto, metodo, referencia, linkComprobante }
// RF-14 (Envío de comprobante de pago) (API): inicia aquí. Paso 4: valida los datos enviados desde «Mis pedidos».
async function registrarComprobante(peticion, respuesta, siguiente) {
  try {
    const datos = validacionPedido.leerComprobante(peticion.body);
    const resultado = await servicio.registrarComprobante(datos);
    respuesta.status(201).json(resultado);
  } catch (error) {
    siguiente(error);
  }
}

module.exports = {
  validarCarrito: validarCarrito,
  leerReglas: leerReglas,
  revisarCobertura: revisarCobertura,
  registrarPedido: registrarPedido,
  consultarPedido: consultarPedido,
  registrarComprobante: registrarComprobante
};
