/*
 * Servicio del Portal de Pedidos (Módulo 2): reglas de negocio de RF-6 a RF-13 y RF-14.
 *
 *  - validarCarrito:       RF-6 / RF-8 / RF-10 paso 2: revisa disponibilidad y precio de cada postre.
 *  - leerReglasEntrega:    RF-10 / RF-11: fechas, horarios, días llenos, anticipo y métodos de pago.
 *  - revisarCobertura:     RF-10 paso 7: si hay envío al código postal y cuánto cuesta.
 *  - registrarPedido:      RF-12: guarda el pedido en UNA transacción (o no guarda nada).
 *  - consultarPedido:      RF-13: seguimiento con folio + teléfono o correo.
 *  - registrarComprobante: RF-14: el cliente envía su comprobante; queda "Pendiente" para el Panel.
 *
 * Reglas de seguridad:
 *  - Los precios SIEMPRE se recalculan aquí (reglas del catálogo y vista vw_CostoPrecioPostre).
 *    Lo que manda el navegador solo sirve para avisar «El precio se actualizó».
 *  - Los datos de un cliente que ya existe no se reemplazan desde la web (solo se completa el correo
 *    si no tenía): así nadie puede cambiar a dónde le llegan los avisos a otra persona.
 */
const crypto = require('crypto');
const baseDatos = require('../../config/baseDatos');
const ErrorApi = require('../../utilidades/ErrorApi');
const enlaceImagen = require('../../utilidades/enlaceImagen');
const catalogoServicio = require('../catalogo/catalogo.servicio');
const repositorio = require('./pedidos.repositorio');
const reglasEntrega = require('./reglasEntrega');
const mensajes = require('./mensajesPedido');
const validacionPedido = require('./pedidos.validacion');

// Mensajes exactos de los casos de uso
const MSG_NO_DISPONIBLE = 'No disponible';
const MSG_REQUIERE_ACTUALIZAR = 'Requiere actualizar personalización';
const MSG_SIN_COBERTURA = 'Por el momento no tenemos envío a tu zona';
const MSG_SOLO_SUCURSAL = 'Por el momento solo está disponible el pago en sucursal';
const MSG_NO_ENCONTRADO = 'No encontramos un pedido con esos datos';
const MSG_REPETIDO = 'Este postre es igual a otro de tu carrito pero con distinto mensaje o indicaciones. ' +
  'Junta las piezas en uno solo o cambia alguna opción.';

// Candados con nombre (GET_LOCK): uno para pedidos y otro para pagos
const CANDADO_PEDIDOS = 'carmir_registro_pedidos';
const CANDADO_PAGOS = 'carmir_registro_pagos';

// Doble envío: minutos en que un pedido idéntico del mismo cliente se considera el mismo
const MINUTOS_DOBLE_ENVIO = 15;
// Cuánto se recuerda una llave de idempotencia en memoria
const HORAS_LLAVE = 24;
// Comprobantes en revisión que puede tener un pedido al mismo tiempo
const COMPROBANTES_PENDIENTES_MAXIMOS = 3;

// Llaves de idempotencia ya usadas: llave → { respuesta, expira } y las que se están procesando
const llavesUsadas = new Map();
const llavesEnProceso = new Map();

// ===================== Apoyo =====================

function redondear(valor) {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

// ClaveConfiguracion igual que ConfiguracionPostre.claveConfiguracion del Panel: "IdOpcion:Cantidad|..." por id
function claveConfiguracion(opciones) {
  const ordenadas = opciones.slice();
  // Ordenamiento por inserción (como en el Panel), de menor a mayor id de opción
  for (let i = 1; i < ordenadas.length; i++) {
    const actual = ordenadas[i];
    let j = i - 1;
    while (j >= 0 && ordenadas[j].idOpcion > actual.idOpcion) {
      ordenadas[j + 1] = ordenadas[j];
      j = j - 1;
    }
    ordenadas[j + 1] = actual;
  }
  const partes = [];
  for (let i = 0; i < ordenadas.length; i++) {
    partes.push(ordenadas[i].idOpcion + ':' + ordenadas[i].cantidad);
  }
  return partes.join('|');
}

async function cargarReglas() {
  const filas = await repositorio.leerConfiguracion();
  return reglasEntrega.armarReglas(filas);
}

// Cotiza un artículo con las reglas del catálogo; si el producto base ya no existe regresa null
async function cotizarArticulo(articulo) {
  try {
    return await catalogoServicio.cotizar(articulo);
  } catch (error) {
    if (error instanceof ErrorApi && error.codigo === 'PRODUCTO_NO_DISPONIBLE') {
      return null;
    }
    throw error;
  }
}

/*
 * Revisa todos los artículos (RF-6 paso 4). Regresa una fila por artículo con:
 *   estado: 'disponible' | 'no_disponible' | 'requiere_actualizar' | 'repetido'
 *   precioUnitario vigente y si cambió respecto al que vio el cliente.
 */
// RF-6 · Paso 4: valida en la BD que cada producto siga activo, con precio y opciones vigentes.
async function revisarArticulos(articulos) {
  const revisados = [];
  const claves = [];
  for (let i = 0; i < articulos.length; i++) {
    const articulo = articulos[i];
    // RF-6 · Paso 4: cotiza con las mismas reglas del catálogo (RF-3).
    const cotizacion = await cotizarArticulo(articulo);
    const fila = {
      posicion: articulo.posicion,
      estado: 'disponible',
      mensaje: '',
      precioUnitario: articulo.precioMostrado,
      precioCambio: false,
      opcionesNoDisponibles: [],
      clave: claveConfiguracion(articulo.opciones),
      idCategoria: 0,
      nombre: '',
      articulo: articulo
    };
    // RF-6 · Excepción paso 4: el producto ya no está activo, se marca «No disponible».
    if (cotizacion === null) {
      fila.estado = 'no_disponible';
      fila.mensaje = MSG_NO_DISPONIBLE;
    } else {
      fila.idCategoria = cotizacion.idCategoria;
      fila.nombre = cotizacion.nombreProducto;
      fila.precioUnitario = cotizacion.precioUnitario;
      fila.precioCambio = cotizacion.precioCambio;
      // RF-6 · Excepción paso 4: opciones no disponibles o incompatibles, «Requiere actualizar personalización».
      if (cotizacion.opcionesNoDisponibles.length > 0 || !cotizacion.valido) {
        fila.estado = 'requiere_actualizar';
        fila.mensaje = MSG_REQUIERE_ACTUALIZAR;
        fila.opcionesNoDisponibles = cotizacion.opcionesNoDisponibles;
      }
    }
    // La tabla DetallePedido no permite el mismo postre dos veces en un pedido
    // RF-6 · Excepción paso 4: el mismo postre dos veces con distinto mensaje (la tabla no lo permite).
    if (fila.estado === 'disponible') {
      if (claves.indexOf(fila.clave) >= 0) {
        fila.estado = 'repetido';
        fila.mensaje = MSG_REPETIDO;
      } else {
        claves.push(fila.clave);
      }
    }
    revisados.push(fila);
  }
  return revisados;
}

// Fila revisada → lo que ve el navegador (sin datos internos)
function filaPublica(fila) {
  return {
    posicion: fila.posicion,
    estado: fila.estado,
    mensaje: fila.mensaje,
    precioUnitario: fila.precioUnitario,
    precioCambio: fila.precioCambio,
    opcionesNoDisponibles: fila.opcionesNoDisponibles
  };
}

// ===================== RF-6: validar el carrito =====================

// RF-6 (API): inicia aquí.
async function validarCarrito(articulos) {
  const revisados = await revisarArticulos(articulos);
  const resultado = [];
  let total = 0;
  let puedeContinuar = true;
  for (let i = 0; i < revisados.length; i++) {
    resultado.push(filaPublica(revisados[i]));
    // RF-6 · Paso 5: calcula el subtotal de cada producto y el total.
    if (revisados[i].estado === 'disponible') {
      total = total + revisados[i].precioUnitario * revisados[i].articulo.cantidad;
    } else {
      // RF-6 · Excepción paso 4: el producto con problema se excluye del total y deshabilita «Continuar».
      // Un producto no disponible se excluye del total y no deja continuar
      puedeContinuar = false;
    }
  }
  return { articulos: resultado, total: redondear(total), puedeContinuar: puedeContinuar };
}

// ===================== RF-10 / RF-11: reglas de entrega y pago =====================

// RF-10 · Paso 3 / RF-11 · Paso 3 (API): inicia aquí. Reglas para el formulario de entrega y de pago.
async function leerReglasEntrega() {
  const reglas = await cargarReglas();
  const desde = reglasEntrega.sumarDias(reglasEntrega.hoy(), reglas.diasAnticipacionMinima);
  const hasta = reglasEntrega.sumarDias(reglasEntrega.hoy(), reglas.diasMaximosAdelante);

  // RF-10 · Excepción paso 6: días sin cupo que el calendario deshabilita.
  // Días que ya llegaron a la capacidad diaria: no se ofrecen en el calendario
  const cargas = await repositorio.contarEntregasPorDia(desde, hasta);
  const fechasLlenas = [];
  for (let i = 0; i < cargas.length; i++) {
    if (Number(cargas[i].Total) >= reglas.capacidadDiaria) {
      fechasLlenas.push(cargas[i].Fecha);
    }
  }

  return {
    nombreNegocio: reglas.nombreNegocio,
    hoy: reglasEntrega.hoy(),
    fechaMinima: reglasEntrega.primeraFechaValida(reglas),
    fechaMaxima: hasta,
    diasAnticipacionMinima: reglas.diasAnticipacionMinima,
    diasLaborables: reglas.diasLaborables,
    textoDiasLaborables: reglasEntrega.textoDiasLaborables(reglas),
    fechasLlenas: fechasLlenas,
    horaApertura: reglas.horaApertura,
    horaCierre: reglas.horaCierre,
    horarios: reglasEntrega.horariosDisponibles(reglas),
    porcentajeAnticipo: reglas.porcentajeAnticipo,
    // RF-11 · Excepción paso 6: sin datos bancarios solo se acepta pago en sucursal.
    aceptaTransferencia: reglasEntrega.aceptaTransferencia(reglas),
    mensajeSoloSucursal: MSG_SOLO_SUCURSAL
  };
}

// ===================== RF-10 paso 7: cobertura =====================

// RF-10 · Paso 7 (API): inicia aquí.
async function revisarCobertura(codigoPostal) {
  const fila = await repositorio.buscarCobertura(codigoPostal);
  // RF-10 · Excepción paso 7: «Por el momento no tenemos envío a tu zona».
  if (fila === null) {
    throw new ErrorApi(404, 'SIN_COBERTURA', MSG_SIN_COBERTURA, [{ campo: 'codigoPostal', mensaje: MSG_SIN_COBERTURA }]);
  }
  return { codigoPostal: fila.CodigoPostal, costoEnvio: Number(fila.CostoEnvio) };
}

// ===================== RF-12: registrar el pedido =====================

/*
 * Doble envío: si la misma llave ya se usó, se regresa el mismo comprobante sin guardar nada.
 * Si llega dos veces al mismo tiempo, la segunda espera a la primera.
 */
// RF-12 · Paso 6 (API): inicia aquí el registro del pedido.
async function registrarPedido(solicitud) {
  limpiarLlavesViejas();
  const usada = llavesUsadas.get(solicitud.llaveIdempotencia);
  // RF-12 · Excepción paso 6: doble envío con la misma llave; se regresa el mismo comprobante sin crear duplicado.
  if (usada !== undefined) {
    return copiaRepetida(usada.respuesta);
  }
  const enProceso = llavesEnProceso.get(solicitud.llaveIdempotencia);
  // RF-12 · Excepción paso 6: doble envío al mismo tiempo; la segunda petición espera a la primera.
  if (enProceso !== undefined) {
    const respuestaAnterior = await enProceso;
    return copiaRepetida(respuestaAnterior);
  }

  const promesa = registrarPedidoNuevo(solicitud);
  llavesEnProceso.set(solicitud.llaveIdempotencia, promesa);
  try {
    const respuesta = await promesa;
    llavesUsadas.set(solicitud.llaveIdempotencia, {
      respuesta: respuesta,
      expira: Date.now() + HORAS_LLAVE * 60 * 60 * 1000
    });
    return respuesta;
  } finally {
    llavesEnProceso.delete(solicitud.llaveIdempotencia);
  }
}

function copiaRepetida(respuesta) {
  const copia = Object.assign({}, respuesta);
  copia.repetido = true;
  return copia;
}

function limpiarLlavesViejas() {
  const ahora = Date.now();
  for (const entrada of llavesUsadas) {
    if (entrada[1].expira < ahora) {
      llavesUsadas.delete(entrada[0]);
    }
  }
}

// RF-12 · Pasos 2 a 9 del registro (ya validados los datos en pedidos.validacion.js).
async function registrarPedidoNuevo(solicitud) {
  const reglas = await cargarReglas();

  // RF-11 paso 6: transferencia solo si hay datos bancarios
  // RF-11 · Excepción paso 6: transferencia sin datos bancarios configurados.
  if (solicitud.pago.metodo !== 'Efectivo' && !reglasEntrega.aceptaTransferencia(reglas)) {
    throw new ErrorApi(400, 'SOLO_SUCURSAL', MSG_SOLO_SUCURSAL, [{ campo: 'metodo', mensaje: MSG_SOLO_SUCURSAL }]);
  }

  // RF-12 paso 3: productos activos y precios vigentes (reglas del catálogo)
  // RF-12 · Paso 3: valida en la BD que los productos sigan activos y con precio vigente.
  const revisados = await revisarArticulos(solicitud.articulos);
  revisarDisponibilidad(revisados);

  // RF-10 paso 7: costo de envío del código postal
  let costoEnvio = 0;
  // RF-10 · Paso 7: con envío a domicilio se agrega el costo de envío.
  if (solicitud.entrega.modalidad === 'Envio a domicilio') {
    const cobertura = await revisarCobertura(solicitud.entrega.direccion.codigoPostal);
    costoEnvio = cobertura.costoEnvio;
  }

  // Todo lo que se guarda va en una sola transacción, con el candado para respetar la capacidad diaria
  // RF-12 · Paso 6: registra el pedido en una sola transacción; si algo falla se deshace todo («No fue posible enviar tu pedido»).
  const guardado = await baseDatos.conTransaccion(guardarPedido(solicitud, reglas, revisados, costoEnvio), CANDADO_PEDIDOS);

  // RF-12 paso 9: aviso al cliente. Si falla, el pedido ya quedó registrado (no se deshace)
  // RF-12 · Paso 9: deja el aviso por WhatsApp y correo para que el Panel lo envíe.
  if (!guardado.repetido) {
    await dejarAvisoDeRegistro(guardado, solicitud, costoEnvio);
  }
  // RF-12 · Paso 8: comprobante con folio, resumen e instrucciones de pago.
  return armarComprobante(guardado, solicitud, reglas, revisados, costoEnvio);
}

// Lanza el error adecuado si algún artículo no se puede pedir
function revisarDisponibilidad(revisados) {
  const problemas = [];
  for (let i = 0; i < revisados.length; i++) {
    if (revisados[i].estado !== 'disponible') {
      problemas.push(filaPublica(revisados[i]));
    }
  }
  // RF-12 · Excepción paso 3: algún producto ya no está disponible; no se registra y regresa al carrito.
  if (problemas.length === 0) {
    return;
  }
  for (let i = 0; i < problemas.length; i++) {
    if (problemas[i].estado === 'repetido') {
      throw new ErrorApi(409, 'POSTRE_REPETIDO', MSG_REPETIDO, problemas);
    }
  }
  throw new ErrorApi(409, 'PRODUCTOS_NO_DISPONIBLES',
    'Algunos productos de tu carrito ya no están disponibles. Revisa tu carrito.', problemas);
}

// RF-12 paso 3: el total cambió. Se manda el precio vigente de cada postre para que el carrito se actualice
function errorPrecioCambio(lineas, costoEnvio, total) {
  const articulos = [];
  for (let i = 0; i < lineas.length; i++) {
    const articulo = lineas[i].fila.articulo;
    articulos.push({
      posicion: articulo.posicion,
      precioUnitario: lineas[i].precioUnitario,
      precioCambio: Math.abs(lineas[i].precioUnitario - articulo.precioMostrado) >= 0.01
    });
  }
  return new ErrorApi(409, 'PRECIO_CAMBIO', 'El total de tu pedido se actualizó. Revisa el nuevo total y confirma otra vez.', {
    articulos: articulos,
    costoEnvio: costoEnvio,
    total: total
  });
}

/*
 * Regresa la función que conTransaccion ejecuta con la conexión abierta.
 * (Se arma así para pasarle los datos sin usar funciones flecha.)
 */
function guardarPedido(solicitud, reglas, revisados, costoEnvio) {
  return async function (conexion) {
    const entrega = solicitud.entrega;

    // 1. Cliente: se busca por su teléfono de WhatsApp o se crea
    const cliente = await obtenerCliente(conexion, solicitud.cliente);

    // 2. Postres: se reutiliza el que tenga la misma configuración; si no existe se crea después
    const lineas = [];
    let todosExisten = true;
    for (let i = 0; i < revisados.length; i++) {
      const postre = await repositorio.buscarPostrePorClave(conexion, revisados[i].clave);
      let idPostre = 0;
      if (postre !== null) {
        idPostre = postre.IdPostre;
      } else {
        todosExisten = false;
      }
      lineas.push({ fila: revisados[i], idPostre: idPostre, precioUnitario: 0, costoUnitario: 0 });
    }

    // 3. Datos del pedido (el doble envío se revisa después de calcular el total)
    const pedido = {
      idCliente: cliente.idCliente,
      fechaEntrega: entrega.fecha,
      horarioEntrega: entrega.horario,
      modalidadEntrega: entrega.modalidad,
      modalidadPago: solicitud.pago.modalidad,
      montoTotal: 0,
      anticipoRequerido: 0,
      huellaLineas: ''
    };
    // (los postres nuevos se crean aquí; si después algo falla, la transacción los quita)
    if (!todosExisten) {
      for (let i = 0; i < lineas.length; i++) {
        if (lineas[i].idPostre === 0) {
          lineas[i].idPostre = await crearPostre(conexion, lineas[i].fila);
        }
      }
    }

    // 4. Precio congelado = precio vigente de la vista vw_CostoPrecioPostre (la misma que usa el Panel).
    //    Si el total no es el que vio el cliente, no se guarda nada y se le avisa (RF-12 paso 3)
    pedido.montoTotal = await calcularPrecios(conexion, lineas, costoEnvio);
    // RF-12 · Excepción paso 3: el total cambió; no se guarda nada y se avisa al cliente.
    if (Math.abs(pedido.montoTotal - solicitud.totalMostrado) >= 0.01) {
      throw errorPrecioCambio(lineas, costoEnvio, pedido.montoTotal);
    }
    // 5. Doble envío aunque la API se haya reiniciado: mismo cliente, misma entrega y mismos postres
    if (todosExisten) {
      pedido.huellaLineas = huellaDeLineas(lineas);
      const folioAnterior = await repositorio.buscarPedidoReciente(conexion, cliente.idCliente, pedido, MINUTOS_DOBLE_ENVIO);
      // RF-12 · Excepción paso 6: pedido idéntico reciente del mismo cliente; se regresa el folio ya generado.
      if (folioAnterior > 0) {
        return { folio: folioAnterior, repetido: true, cliente: cliente, lineas: lineas, montoTotal: pedido.montoTotal };
      }
    }
    // 6. Fecha, horario y capacidad del día (RF-10 paso 6), ya con el candado puesto
    const carga = await repositorio.contarEntregasDelDia(conexion, entrega.fecha);
    const problema = reglasEntrega.revisarFechaYHorario(reglas, entrega.fecha, entrega.horario, carga);
    // RF-10 · Excepción paso 6: la fecha ya no cumple anticipación, día laborable, horario o cupo.
    if (problema !== null) {
      throw validacionPedido.errorDeCampo(problema.campo, problema.mensaje);
    }
    pedido.anticipoRequerido = reglasEntrega.calcularAnticipo(reglas, pedido.montoTotal, pedido.modalidadPago);

    // 7. Pedido, entrega a domicilio, postres y primer registro del historial
    // RF-12 · Paso 6: registra el pedido con folio único.
    const folio = await repositorio.insertarPedido(conexion, pedido);
    if (entrega.modalidad === 'Envio a domicilio') {
      const idDireccion = await repositorio.insertarDireccion(conexion, cliente.idCliente, entrega.direccion);
      await repositorio.insertarEntregaDomicilio(conexion, folio, idDireccion);
    }
    for (let i = 0; i < lineas.length; i++) {
      const articulo = lineas[i].fila.articulo;
      await repositorio.insertarDetalle(conexion, folio, {
        idPostre: lineas[i].idPostre,
        cantidad: articulo.cantidad,
        dedicatoria: articulo.dedicatoria,
        comentario: articulo.comentario,
        precioUnitario: lineas[i].precioUnitario,
        costoUnitario: lineas[i].costoUnitario
      });
    }
    // RF-12 · Paso 6: estado inicial «En proceso de aceptación» en el historial.
    await repositorio.insertarHistorialInicial(conexion, folio);

    return {
      folio: folio,
      repetido: false,
      cliente: cliente,
      lineas: lineas,
      montoTotal: pedido.montoTotal,
      anticipoRequerido: pedido.anticipoRequerido
    };
  };
}

// Busca al cliente por teléfono o lo registra; regresa { idCliente, nombre, telefono, correo }
async function obtenerCliente(conexion, datos) {
  const existente = await repositorio.buscarClientePorTelefono(conexion, datos.telefono);
  if (existente === null) {
    const idCliente = await repositorio.insertarCliente(conexion, datos);
    return { idCliente: idCliente, nombre: datos.nombre, telefono: datos.telefono, correo: datos.correo };
  }
  let correo = existente.CorreoCliente;
  if ((correo === null || correo === '') && datos.correo !== '') {
    await repositorio.completarCorreo(conexion, existente.IdCliente, datos.correo);
    correo = datos.correo;
  }
  if (correo === null) {
    correo = '';
  }
  return {
    idCliente: existente.IdCliente,
    nombre: existente.NombreCliente,
    telefono: existente.TelefonoWhatsApp,
    correo: correo
  };
}

// Registra un postre personalizado (oculto en la tienda) con sus opciones
async function crearPostre(conexion, fila) {
  const idPostre = await repositorio.insertarPostrePersonalizado(conexion, fila.idCategoria, fila.clave);
  const opciones = fila.articulo.opciones;
  for (let i = 0; i < opciones.length; i++) {
    await repositorio.insertarPostreOpcion(conexion, idPostre, opciones[i].idOpcion, opciones[i].cantidad);
  }
  return idPostre;
}

// Lee precio y costo vigentes de cada postre (vw_CostoPrecioPostre) y regresa el total con envío
async function calcularPrecios(conexion, lineas, costoEnvio) {
  let total = 0;
  for (let i = 0; i < lineas.length; i++) {
    const precio = await repositorio.leerPrecioPostre(conexion, lineas[i].idPostre);
    // RF-12 · Excepción paso 3: el postre ya no tiene precio vigente.
    if (precio === null || Number(precio.PrecioVenta) <= 0) {
      throw new ErrorApi(409, 'PRODUCTOS_NO_DISPONIBLES',
        'Algunos productos de tu carrito ya no están disponibles. Revisa tu carrito.',
        [{ posicion: lineas[i].fila.posicion, estado: 'no_disponible', mensaje: MSG_NO_DISPONIBLE }]);
    }
    lineas[i].precioUnitario = Number(precio.PrecioVenta);
    lineas[i].costoUnitario = Number(precio.CostoIngredientes);
    total = total + lineas[i].precioUnitario * lineas[i].fila.articulo.cantidad;
  }
  return redondear(total + costoEnvio);
}

// "IdPostre:Cantidad|..." ordenado por IdPostre (igual que la consulta de buscarPedidoReciente)
function huellaDeLineas(lineas) {
  const pares = [];
  for (let i = 0; i < lineas.length; i++) {
    pares.push({ idOpcion: lineas[i].idPostre, cantidad: lineas[i].fila.articulo.cantidad });
  }
  return claveConfiguracion(pares);
}

// Deja los avisos "Pendiente" (WhatsApp y, si hay, correo); el Panel los envía
async function dejarAvisoDeRegistro(guardado, solicitud, costoEnvio) {
  const mensaje = mensajes.mensajeRegistro({
    folio: guardado.folio,
    nombreCliente: guardado.cliente.nombre,
    fechaEntrega: solicitud.entrega.fecha,
    horario: solicitud.entrega.horario,
    modalidadEntrega: solicitud.entrega.modalidad,
    modalidadPago: solicitud.pago.modalidad,
    metodo: solicitud.pago.metodo,
    montoTotal: guardado.montoTotal,
    anticipo: guardado.anticipoRequerido,
    costoEnvio: costoEnvio
  });
  try {
    await baseDatos.conTransaccion(guardarAvisos(guardado, mensaje));
  } catch (error) {
    // RF-12 · Excepción paso 9: si el aviso no se pudo dejar, el pedido NO se deshace.
    // El pedido NO se deshace: la Administradora lo verá sin aviso y puede contactar al cliente
    const referencia = crypto.randomUUID().substring(0, 8);
    console.error('[ERROR ' + referencia + '] No se pudo dejar el aviso del pedido ' + guardado.folio);
    console.error(error);
  }
}

function guardarAvisos(guardado, mensaje) {
  return async function (conexion) {
    await repositorio.insertarNotificacion(conexion, {
      folio: guardado.folio,
      tipoEvento: mensajes.TIPO_REGISTRO,
      canal: 'WhatsApp',
      destino: guardado.cliente.telefono,
      mensaje: mensaje
    });
    if (guardado.cliente.correo !== '') {
      await repositorio.insertarNotificacion(conexion, {
        folio: guardado.folio,
        tipoEvento: mensajes.TIPO_REGISTRO,
        canal: 'Correo',
        destino: guardado.cliente.correo,
        mensaje: mensaje
      });
    }
  };
}

// Comprobante que ve el cliente al terminar (RF-12 paso 8)
// RF-12 · Paso 8: arma el comprobante que ve el cliente.
function armarComprobante(guardado, solicitud, reglas, revisados, costoEnvio) {
  const productos = [];
  let subtotal = 0;
  for (let i = 0; i < guardado.lineas.length; i++) {
    const linea = guardado.lineas[i];
    const articulo = linea.fila.articulo;
    const importe = redondear(linea.precioUnitario * articulo.cantidad);
    subtotal = subtotal + importe;
    productos.push({
      nombre: linea.fila.nombre,
      cantidad: articulo.cantidad,
      precioUnitario: linea.precioUnitario,
      subtotal: importe,
      dedicatoria: articulo.dedicatoria,
      comentario: articulo.comentario
    });
  }
  let anticipo = guardado.anticipoRequerido;
  if (anticipo === undefined) {
    anticipo = reglasEntrega.calcularAnticipo(reglas, guardado.montoTotal, solicitud.pago.modalidad);
  }
  return {
    folio: guardado.folio,
    repetido: guardado.repetido,
    estado: mensajes.textoEstado('En proceso de aceptacion'),
    cliente: { nombre: guardado.cliente.nombre, telefono: guardado.cliente.telefono },
    productos: productos,
    subtotalProductos: redondear(subtotal),
    costoEnvio: costoEnvio,
    montoTotal: guardado.montoTotal,
    anticipoRequerido: anticipo,
    modalidadPago: solicitud.pago.modalidad,
    metodoPago: solicitud.pago.metodo,
    montoParaIniciar: mensajes.montoParaIniciar(solicitud.pago.modalidad, guardado.montoTotal, anticipo),
    entrega: {
      modalidad: mensajes.textoModalidadEntrega(solicitud.entrega.modalidad),
      fecha: solicitud.entrega.fecha,
      horario: solicitud.entrega.horario,
      direccion: solicitud.entrega.direccion
    },
    // RF-11 · Paso 6: instrucciones de pago según el método y la modalidad.
    instrucciones: mensajes.instruccionesPago(reglas, solicitud.pago.metodo, solicitud.pago.modalidad,
      guardado.montoTotal, anticipo)
  };
}

// ===================== RF-13: seguimiento =====================

// RF-13 (API): inicia aquí. Paso 4: busca el pedido con folio y teléfono (o correo).
// "contacto" = { telefono, correo }: uno de los dos trae texto.
async function consultarPedido(folio, contacto) {
  const fila = await repositorio.buscarPedidoDeCliente(folio, contacto);
  // RF-13 · Excepción: no hay pedido con esos datos (no se dice cuál de los dos falló).
  if (fila === null) {
    // No se dice cuál de los dos datos falló
    throw new ErrorApi(404, 'PEDIDO_NO_ENCONTRADO', MSG_NO_ENCONTRADO);
  }
  const reglas = await cargarReglas();
  const lineas = await repositorio.listarLineas(folio);
  const opciones = await repositorio.listarOpcionesDeLineas(folio);
  const historial = await repositorio.listarHistorial(folio);
  const pagos = await repositorio.listarPagos(folio);
  return armarSeguimiento(fila, lineas, opciones, historial, pagos, reglas);
}

// RF-13 · Pasos 5 y 6: arma el detalle (estado, productos, entrega, pagos y saldo).
function armarSeguimiento(fila, lineas, opciones, historial, pagos, reglas) {
  const montoTotal = Number(fila.MontoTotal);
  const totalPagado = Number(fila.TotalPagado);
  const porValidar = Number(fila.PorValidar);
  // El envío no tiene columna: es lo que el total tiene de más sobre los postres
  let costoEnvio = redondear(montoTotal - Number(fila.SumaProductos));
  if (costoEnvio < 0.005) {
    costoEnvio = 0;
  }

  const seguimiento = {
    folio: fila.Folio,
    nombreCliente: fila.NombreCliente,
    fechaSolicitud: fila.FechaSolicitud,
    estado: fila.EstadoPedido,
    estadoTexto: mensajes.textoEstado(fila.EstadoPedido),
    pasos: armarPasos(fila.EstadoPedido, historial),
    cancelacion: null,
    baja: null,
    entrega: {
      modalidad: mensajes.textoModalidadEntrega(fila.ModalidadEntrega),
      fecha: fila.FechaEntrega,
      horario: String(fila.HorarioEntrega).substring(0, 5),
      direccion: null
    },
    productos: armarProductos(lineas, opciones),
    costoEnvio: costoEnvio,
    montoTotal: montoTotal,
    anticipoRequerido: Number(fila.AnticipoRequerido),
    modalidadPago: fila.ModalidadPago,
    montoParaIniciar: mensajes.montoParaIniciar(fila.ModalidadPago, montoTotal, Number(fila.AnticipoRequerido)),
    totalPagado: totalPagado,
    porValidar: porValidar,
    saldo: redondear(montoTotal - totalPagado),
    pagos: armarPagos(pagos),
    puedeEnviarComprobante: false,
    motivoSinComprobante: '',
    datosBancarios: reglas.datosBancarios
  };

  if (fila.Calle !== null) {
    seguimiento.entrega.direccion = {
      calle: fila.Calle,
      numeroExterior: fila.NumeroExterior,
      numeroInterior: fila.NumeroInterior,
      colonia: fila.Colonia,
      codigoPostal: fila.CodigoPostal,
      referencias: fila.Referencias
    };
  }
  if (fila.FechaHoraBaja !== null) {
    seguimiento.baja = { motivo: fila.MotivoBaja, fecha: fila.FechaHoraBaja };
  }
  // RF-13: si el pedido fue cancelado se muestra el motivo y el reembolso.
  if (fila.FechaHoraCancelacion !== null) {
    seguimiento.cancelacion = {
      motivo: fila.MotivoCancelacion,
      fecha: fila.FechaHoraCancelacion,
      montoReembolso: Number(fila.MontoReembolso),
      estadoReembolso: fila.EstadoReembolso
    };
  }
  const motivo = motivoSinComprobante(fila, montoTotal - totalPagado - porValidar);
  seguimiento.puedeEnviarComprobante = motivo === '';
  seguimiento.motivoSinComprobante = motivo;
  return seguimiento;
}

// Barra de avance: los 6 estados, cuáles ya pasaron y la fecha en que llegó a cada uno
function armarPasos(estadoActual, historial) {
  let posicionActual = 0;
  for (let i = 0; i < mensajes.ESTADOS.length; i++) {
    if (mensajes.ESTADOS[i].valor === estadoActual) {
      posicionActual = i;
    }
  }
  const pasos = [];
  for (let i = 0; i < mensajes.ESTADOS.length; i++) {
    let fecha = null;
    // Se toma la última vez que el pedido llegó a ese estado
    for (let j = 0; j < historial.length; j++) {
      if (historial[j].EstadoNuevo === mensajes.ESTADOS[i].valor) {
        fecha = historial[j].FechaHoraCambio;
      }
    }
    pasos.push({
      estado: mensajes.ESTADOS[i].valor,
      texto: mensajes.ESTADOS[i].texto,
      completado: i <= posicionActual,
      actual: i === posicionActual,
      fecha: fecha
    });
  }
  return pasos;
}

function armarProductos(lineas, opciones) {
  const productos = [];
  for (let i = 0; i < lineas.length; i++) {
    const linea = lineas[i];
    let nombre = linea.NombrePostre;
    if (nombre === null || nombre === '') {
      nombre = linea.NombreCategoria + ' personalizado';
    }
    const personalizacion = [];
    for (let j = 0; j < opciones.length; j++) {
      if (opciones[j].IdPostre === linea.IdPostre) {
        let texto = opciones[j].NombreCaracteristica + ': ' + opciones[j].NombreOpcion;
        if (opciones[j].CantidadElegida > 1) {
          texto = texto + ' ×' + opciones[j].CantidadElegida;
        }
        personalizacion.push(texto);
      }
    }
    productos.push({
      nombre: nombre,
      linkFotografia: enlaceImagen.enlaceParaWeb(linea.LinkFotografia, 'postre', linea.IdPostre),
      cantidad: linea.Cantidad,
      precioUnitario: Number(linea.PrecioUnitario),
      subtotal: redondear(Number(linea.PrecioUnitario) * linea.Cantidad),
      dedicatoria: linea.Dedicatoria,
      comentario: linea.Comentario,
      personalizacion: personalizacion
    });
  }
  return productos;
}

function armarPagos(pagos) {
  const lista = [];
  for (let i = 0; i < pagos.length; i++) {
    lista.push({
      monto: Number(pagos[i].MontoPago),
      fecha: pagos[i].FechaPago,
      metodo: mensajes.textoMetodo(pagos[i].MetodoPago),
      referencia: pagos[i].Referencia,
      estado: pagos[i].EstadoValidacion,
      motivoRechazo: pagos[i].MotivoRechazo
    });
  }
  return lista;
}

// Por qué no se puede enviar un comprobante ('' = sí se puede). Mismas reglas que PagoService del Panel.
// RF-14 · Excepciones pasos 1 y 5: motivos por los que no se puede enviar un comprobante.
function motivoSinComprobante(fila, porCubrir) {
  if (fila.FechaHoraBaja !== null) {
    return 'Esta solicitud fue dada de baja';
  }
  if (fila.FechaHoraCancelacion !== null) {
    return 'No se pueden registrar pagos en un pedido cancelado';
  }
  if (fila.EstadoPedido === 'En proceso de aceptacion') {
    return 'Podrás enviar tu comprobante cuando la repostería apruebe tu pedido';
  }
  if (fila.EstadoPedido === 'Entregado' || porCubrir < 0.005) {
    return 'Tu pedido ya está liquidado o tiene comprobantes en revisión por el total';
  }
  return '';
}

// ===================== RF-14: enviar comprobante =====================

// RF-14 · Pasos 5 a 8 (API): registra el comprobante de pago; queda «Pendiente» para RF-42 en el Panel.
async function registrarComprobante(datos) {
  // RF-14 · Excepción paso 7: si algo falla, la transacción se deshace y no se registra nada.
  await baseDatos.conTransaccion(guardarComprobante(datos), CANDADO_PAGOS);
  // RF-14 · Paso 8: mensaje de éxito y detalle actualizado con el pago «En revisión».
  return {
    mensaje: 'Recibimos tu comprobante. La repostería lo revisará y te avisaremos por WhatsApp cuando lo valide.',
    seguimiento: await consultarPedido(datos.folio, { telefono: datos.telefono, correo: datos.correo })
  };
}

function guardarComprobante(datos) {
  return async function (conexion) {
    // RF-14 · Paso 5: busca el pedido con el folio y el mismo teléfono o correo, y revisa que admita pagos.
    const fila = await repositorio.buscarPedidoParaPago(conexion, datos.folio, { telefono: datos.telefono, correo: datos.correo });
    // RF-14 · Excepción paso 5: «No encontramos un pedido con esos datos».
    if (fila === null) {
      throw new ErrorApi(404, 'PEDIDO_NO_ENCONTRADO', MSG_NO_ENCONTRADO);
    }
    const porCubrir = redondear(Number(fila.MontoTotal) - Number(fila.TotalPagado) - Number(fila.PorValidar));
    const motivo = motivoSinComprobante(fila, porCubrir);
    // RF-14 · Excepción paso 5: el pedido no admite comprobante (baja, cancelado, sin aprobar o liquidado).
    if (motivo !== '') {
      throw new ErrorApi(409, 'COMPROBANTE_NO_PERMITIDO', motivo);
    }
    // RF-14 · Paso 6: revisa los comprobantes en revisión, el monto y la referencia.
    const pendientes = await repositorio.contarPagosPendientes(conexion, datos.folio);
    // RF-14 · Excepción paso 6: «Ya tienes N comprobantes en revisión…» (máximo 3).
    if (pendientes >= COMPROBANTES_PENDIENTES_MAXIMOS) {
      throw new ErrorApi(409, 'COMPROBANTE_NO_PERMITIDO',
        'Ya tienes ' + pendientes + ' comprobantes en revisión. Espera a que la repostería los revise.');
    }
    // RF-14 · Excepción paso 6: el monto es mayor a lo que falta por pagar.
    if (datos.monto > porCubrir + 0.004) {
      throw validacionPedido.errorDeCampo('monto',
        'El monto no puede ser mayor a lo que falta por pagar (' + mensajes.moneda(porCubrir) + ')');
    }
    // RF-14 · Excepción paso 6: la referencia ya está registrada en otro pago.
    if (await repositorio.referenciaEnUso(conexion, datos.referencia)) {
      throw validacionPedido.errorDeCampo('referencia', 'Esa referencia ya está registrada en otro pago');
    }
    // RF-14 · Paso 7: registra el pago por transferencia como «Pendiente».
    const idPago = await repositorio.insertarPago(conexion, datos);
    // RF-14 · Paso 7: si el cliente subió la foto o PDF, se guarda junto al pago (misma transacción)
    if (datos.archivo !== null) {
      await repositorio.insertarArchivoComprobante(conexion, idPago, datos.archivo);
    }
  };
}

module.exports = {
  validarCarrito: validarCarrito,
  leerReglasEntrega: leerReglasEntrega,
  revisarCobertura: revisarCobertura,
  registrarPedido: registrarPedido,
  consultarPedido: consultarPedido,
  registrarComprobante: registrarComprobante,
  // Para las pruebas
  claveConfiguracion: claveConfiguracion
};
