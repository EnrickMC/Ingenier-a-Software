/*
 * Textos del Portal de Pedidos: instrucciones de pago (RF-10) y aviso de registro (RF-32).
 * El aviso se guarda en la tabla Notificacion como "Pendiente" y lo envía el Panel
 * por WhatsApp y correo (el Panel es quien tiene las credenciales de esos servicios).
 */
const reglasEntrega = require('./reglasEntrega');
const tienda = require('../../config/tienda');

// Tipo de evento que se guarda en Notificacion.TipoEvento (el Panel ya tiene los demás)
const TIPO_REGISTRO = 'Pedido registrado';

// Estados del pedido: valor en la base de datos → texto con acentos para el cliente
const ESTADOS = [
  { valor: 'En proceso de aceptacion', texto: 'En proceso de aceptación' },
  { valor: 'Aprobado', texto: 'Aprobado' },
  { valor: 'Pagado', texto: 'Pagado' },
  { valor: 'En preparacion', texto: 'En preparación' },
  { valor: 'Pedido terminado', texto: 'Pedido terminado' },
  { valor: 'Entregado', texto: 'Entregado' }
];

function textoEstado(valor) {
  for (let i = 0; i < ESTADOS.length; i++) {
    if (ESTADOS[i].valor === valor) {
      return ESTADOS[i].texto;
    }
  }
  return valor;
}

function textoModalidadEntrega(valor) {
  if (valor === 'Envio a domicilio') {
    return 'Envío a domicilio';
  }
  return 'Recoger en tienda';
}

function textoMetodo(valor) {
  // «Depósito» ya no se ofrece en la página; se conserva el texto por si un pedido viejo lo tiene
  if (valor === 'Deposito') {
    return 'Depósito bancario';
  }
  if (valor === 'Transferencia') {
    return 'Transferencia bancaria';
  }
  return 'Efectivo en la repostería';
}

// "$1,234.50"
function moneda(valor) {
  const numero = Number(valor);
  const partes = numero.toFixed(2).split('.');
  const enteros = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return '$' + enteros + '.' + partes[1];
}

// Monto que se debe pagar para que el pedido pueda empezar a prepararse
function montoParaIniciar(modalidadPago, total, anticipo) {
  if (modalidadPago === 'Pago total') {
    return total;
  }
  return anticipo;
}

/*
 * Instrucciones de pago (RF-10 paso 6). Regresa { titulo, pasos[], datosBancarios }.
 * datosBancarios solo se manda cuando el cliente eligió transferencia.
 */
function instruccionesPago(reglas, metodo, modalidadPago, total, anticipo) {
  const monto = moneda(montoParaIniciar(modalidadPago, total, anticipo));
  const pasos = [];
  pasos.push('Espera a que la repostería apruebe tu pedido; te avisaremos por WhatsApp.');

  if (metodo === 'Efectivo') {
    pasos.push('Paga ' + monto + ' en efectivo en nuestra tienda (' + tienda.direccion + ') para que empecemos a preparar tu pedido.');
    pasos.push('Menciona tu folio al pagar.');
    return { titulo: 'Pago en la tienda', pasos: pasos, datosBancarios: '' };
  }

  pasos.push('Haz tu ' + textoMetodo(metodo).toLowerCase() + ' por ' + monto + ' con los datos de abajo.');
  pasos.push('Escribe tu folio en el concepto o referencia del pago.');
  pasos.push('Entra a «Mis pedidos», busca tu pedido y sube la foto de tu comprobante (o pega el enlace) para que lo validemos.');
  return { titulo: textoMetodo(metodo), pasos: pasos, datosBancarios: reglas.datosBancarios };
}

// Mensaje del aviso "Pedido registrado" (mismo estilo que PlantillasMensaje del Panel)
function mensajeRegistro(datos) {
  let texto = 'Hola ' + datos.nombreCliente + '.\n';
  texto = texto + 'Recibimos tu pedido #' + datos.folio + '. Está «En proceso de aceptación»: ' +
    'te avisaremos cuando la repostería lo apruebe.\n';
  texto = texto + 'Entrega: ' + reglasEntrega.fechaParaMostrar(datos.fechaEntrega) + ' a las ' + datos.horario +
    ' (' + textoModalidadEntrega(datos.modalidadEntrega).toLowerCase() + ').\n';
  // Al recoger en tienda se indica dónde está
  if (datos.modalidadEntrega !== 'Envio a domicilio') {
    texto = texto + 'Recoge en: ' + tienda.direccion + '.\n';
  }
  texto = texto + 'Total: ' + moneda(datos.montoTotal);
  if (datos.costoEnvio > 0) {
    texto = texto + ' (incluye envío de ' + moneda(datos.costoEnvio) + ')';
  }
  texto = texto + '. Para iniciar la preparación se necesitan ' +
    moneda(montoParaIniciar(datos.modalidadPago, datos.montoTotal, datos.anticipo)) +
    ' (' + datos.modalidadPago.toLowerCase() + ', ' + textoMetodo(datos.metodo).toLowerCase() + ').\n';
  texto = texto + 'Consulta tu pedido en «Mis pedidos» con tu folio y este teléfono.';
  return texto + '\n\nGracias por tu preferencia.';
}

module.exports = {
  TIPO_REGISTRO: TIPO_REGISTRO,
  ESTADOS: ESTADOS,
  textoEstado: textoEstado,
  textoModalidadEntrega: textoModalidadEntrega,
  textoMetodo: textoMetodo,
  moneda: moneda,
  montoParaIniciar: montoParaIniciar,
  instruccionesPago: instruccionesPago,
  mensajeRegistro: mensajeRegistro
};
