/*
 * Controlador del Catálogo Público.
 * Lee y valida los datos de la petición, llama al servicio y responde en JSON.
 * Los errores se pasan a "siguiente(error)" para que los atienda manejoErrores.js.
 */
const servicio = require('./catalogo.servicio');
const validacion = require('../../utilidades/validacion');

const ORDENES_PERMITIDOS = ['nombre', 'precio_asc', 'precio_desc'];
const CANTIDAD_MAXIMA_PIEZAS = 20;
const LARGO_MAXIMO_DEDICATORIA = 150;
const LARGO_MAXIMO_COMENTARIO = 255;
const OPCIONES_MAXIMAS = 80;
// Capas máximas de un pastel (PostreOpcion.NumeroCapa: 1 = Base)
const CAPA_MAXIMA = 10;

// RF-1 · Paso 2 (API): categorías activas para el catálogo.
// GET /api/catalogo/categorias
async function listarCategorias(peticion, respuesta, siguiente) {
  try {
    const categorias = await servicio.listarCategorias();
    respuesta.json({ categorias: categorias });
  } catch (error) {
    siguiente(error);
  }
}

// RF-1 · Pasos 2, 3 y 6 (API): productos activos con filtros de categoría, búsqueda y orden.
// GET /api/catalogo/productos?categoria=1&buscar=texto&orden=precio_asc
async function listarProductos(peticion, respuesta, siguiente) {
  try {
    const consulta = peticion.query;
    // RF-1 · Paso 6: se validan los filtros que llegan en la dirección.
    const idCategoria = validacion.enteroOpcional(validacion.valorUnico(consulta.categoria, 'categoría'), 'categoría');
    const buscar = validacion.textoOpcional(validacion.valorUnico(consulta.buscar, 'búsqueda'), 'búsqueda', 60);
    const orden = validacion.valorDeLista(validacion.valorUnico(consulta.orden, 'orden'), ORDENES_PERMITIDOS, 'nombre', 'orden');

    const productos = await servicio.listarProductos(idCategoria, buscar, orden);
    respuesta.json({ productos: productos });
  } catch (error) {
    // RF-1 · Excepción paso 2: cualquier falla (sin base de datos, filtro inválido) la responde manejoErrores.js.
    siguiente(error);
  }
}

// RF-2 · Pasos 2 a 4 (API): ficha detallada del producto.
// GET /api/catalogo/productos/:id
async function obtenerFicha(peticion, respuesta, siguiente) {
  try {
    const idPostre = validacion.enteroPositivo(peticion.params.id, 'producto');
    const ficha = await servicio.obtenerFicha(idPostre);
    respuesta.json(ficha);
  } catch (error) {
    siguiente(error);
  }
}

// RF-3 · Paso 2 (API): características, opciones y exclusiones para personalizar.
// GET /api/catalogo/productos/:id/personalizacion
async function obtenerPersonalizacion(peticion, respuesta, siguiente) {
  try {
    const idPostre = validacion.enteroPositivo(peticion.params.id, 'producto');
    const datos = await servicio.obtenerPersonalizacion(idPostre);
    respuesta.json(datos);
  } catch (error) {
    siguiente(error);
  }
}

// RF-4 · Pasos 2 y 3 (API): plantillas de la galería.
// GET /api/catalogo/productos/:id/plantillas
async function listarPlantillas(peticion, respuesta, siguiente) {
  try {
    const idPostre = validacion.enteroPositivo(peticion.params.id, 'producto');
    const plantillas = await servicio.listarPlantillas(idPostre);
    respuesta.json({ plantillas: plantillas });
  } catch (error) {
    siguiente(error);
  }
}

// RF-3 · Paso 6 (API): revisa la forma de los datos (cantidad, dedicatoria, opciones repetidas).
// Revisa la forma del cuerpo de POST /cotizar y regresa los datos limpios
function leerDatosCotizacion(cuerpo) {
  if (cuerpo === undefined || cuerpo === null || typeof cuerpo !== 'object' || Array.isArray(cuerpo)) {
    throw validacion.datoInvalido('Los datos de la personalización no son válidos');
  }

  const idPostre = validacion.enteroPositivo(cuerpo.idPostre, 'producto');

  if (!Array.isArray(cuerpo.opciones)) {
    throw validacion.datoInvalido('Los datos de la personalización no son válidos');
  }
  if (cuerpo.opciones.length > OPCIONES_MAXIMAS) {
    throw validacion.datoInvalido('Se eligieron demasiadas opciones');
  }

  const opciones = [];
  const vistas = [];
  for (let i = 0; i < cuerpo.opciones.length; i++) {
    const elegida = cuerpo.opciones[i];
    if (elegida === null || typeof elegida !== 'object') {
      throw validacion.datoInvalido('Los datos de la personalización no son válidos');
    }
    const idOpcion = validacion.enteroPositivo(elegida.idOpcion, 'opción');
    const cantidad = validacion.enteroPositivo(elegida.cantidad, 'cantidad de la opción');
    // CantidadElegida es TINYINT en la base de datos
    if (cantidad > 127) {
      throw validacion.datoInvalido('La cantidad de una opción no es válida');
    }
    // Capa del pastel donde va la opción; si no viene es la Base (1)
    let capa = 1;
    if (elegida.capa !== undefined && elegida.capa !== null) {
      capa = validacion.enteroPositivo(elegida.capa, 'capa de la opción');
    }
    if (capa > CAPA_MAXIMA) {
      throw validacion.datoInvalido('La capa de una opción no es válida');
    }
    // Una opción puede repetirse solo en capas distintas (llave de PostreOpcion)
    const llave = idOpcion + '@' + capa;
    if (vistas.indexOf(llave) >= 0) {
      throw validacion.datoInvalido('Una opción viene repetida');
    }
    vistas.push(llave);
    opciones.push({ idOpcion: idOpcion, cantidad: cantidad, capa: capa });
  }

  let cantidad = 1;
  if (cuerpo.cantidad !== undefined) {
    cantidad = validacion.enteroPositivo(cuerpo.cantidad, 'cantidad');
  }
  // RF-3 · Excepción paso 6: la cantidad no es válida.
  if (cantidad > CANTIDAD_MAXIMA_PIEZAS) {
    throw validacion.datoInvalido('La cantidad debe estar entre 1 y ' + CANTIDAD_MAXIMA_PIEZAS);
  }

  // RF-3 · Excepción paso 6: la dedicatoria o el comentario no cumplen el largo o los caracteres.
  const dedicatoria = validacion.textoOpcional(cuerpo.dedicatoria, 'Dedicatoria', LARGO_MAXIMO_DEDICATORIA);
  const comentario = validacion.textoOpcional(cuerpo.comentario, 'Comentario', LARGO_MAXIMO_COMENTARIO);

  let precioMostrado = 0;
  if (typeof cuerpo.precioMostrado === 'number' && Number.isFinite(cuerpo.precioMostrado) && cuerpo.precioMostrado > 0) {
    precioMostrado = cuerpo.precioMostrado;
  }

  return {
    idPostre: idPostre,
    opciones: opciones,
    cantidad: cantidad,
    dedicatoria: dedicatoria,
    comentario: comentario,
    precioMostrado: precioMostrado
  };
}

// RF-3 · Paso 7 (API): valida contra la BD y confirma el precio vigente.
// POST /api/catalogo/cotizar  (no guarda nada: solo valida y calcula el precio)
async function cotizar(peticion, respuesta, siguiente) {
  try {
    const datos = leerDatosCotizacion(peticion.body);
    const cotizacion = await servicio.cotizar(datos);
    respuesta.json(cotizacion);
  } catch (error) {
    siguiente(error);
  }
}

module.exports = {
  listarCategorias: listarCategorias,
  listarProductos: listarProductos,
  obtenerFicha: obtenerFicha,
  obtenerPersonalizacion: obtenerPersonalizacion,
  listarPlantillas: listarPlantillas,
  cotizar: cotizar,
  // El Módulo 2 (pedidos) revisa cada postre del carrito con las mismas reglas
  leerDatosCotizacion: leerDatosCotizacion
};
