/*
 * Validaciones de los datos que llegan del navegador.
 * Nunca se confía en lo que manda el cliente: todo se revisa aquí antes de usarlo.
 * Si algo está mal se lanza un ErrorApi 400 con un mensaje claro.
 */
const ErrorApi = require('./ErrorApi');

// Letras (con acentos y ñ), números, espacios y signos de puntuación comunes.
// Deja fuera emojis y símbolos raros (RF-3: "longitud y caracteres permitidos").
const CARACTERES_PERMITIDOS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,;:¡!¿?'"()\-&#@/%$+*\n]*$/;

function datoInvalido(mensaje) {
  return new ErrorApi(400, 'DATOS_INVALIDOS', mensaje);
}

// Entero positivo obligatorio (por ejemplo el id de un producto en la URL)
function enteroPositivo(valor, nombreCampo) {
  const texto = String(valor);
  if (!/^[0-9]{1,9}$/.test(texto)) {
    throw datoInvalido('El dato «' + nombreCampo + '» no es válido');
  }
  const numero = Number(texto);
  if (numero <= 0) {
    throw datoInvalido('El dato «' + nombreCampo + '» no es válido');
  }
  return numero;
}

// Entero positivo opcional: si no viene regresa 0
function enteroOpcional(valor, nombreCampo) {
  if (valor === undefined || valor === null || valor === '') {
    return 0;
  }
  return enteroPositivo(valor, nombreCampo);
}

// Texto opcional con largo máximo y caracteres permitidos
function textoOpcional(valor, nombreCampo, largoMaximo) {
  if (valor === undefined || valor === null) {
    return '';
  }
  if (typeof valor !== 'string') {
    throw datoInvalido('El dato «' + nombreCampo + '» no es válido');
  }
  const texto = valor.trim();
  if (texto.length > largoMaximo) {
    throw datoInvalido('«' + nombreCampo + '» admite máximo ' + largoMaximo + ' caracteres');
  }
  if (!CARACTERES_PERMITIDOS.test(texto)) {
    throw datoInvalido('«' + nombreCampo + '» tiene caracteres no permitidos (no se permiten emojis ni símbolos especiales)');
  }
  return texto;
}

// Valor que debe estar en una lista (por ejemplo el orden del catálogo)
function valorDeLista(valor, permitidos, valorPorDefecto, nombreCampo) {
  if (valor === undefined || valor === null || valor === '') {
    return valorPorDefecto;
  }
  if (permitidos.indexOf(valor) < 0) {
    throw datoInvalido('El dato «' + nombreCampo + '» no es válido');
  }
  return valor;
}

// Un solo valor de la URL: si alguien manda ?buscar=a&buscar=b, se rechaza
function valorUnico(valor, nombreCampo) {
  if (Array.isArray(valor) || (valor !== null && typeof valor === 'object')) {
    throw datoInvalido('El dato «' + nombreCampo + '» no es válido');
  }
  return valor;
}

module.exports = {
  CARACTERES_PERMITIDOS: CARACTERES_PERMITIDOS,
  datoInvalido: datoInvalido,
  enteroPositivo: enteroPositivo,
  enteroOpcional: enteroOpcional,
  textoOpcional: textoOpcional,
  valorDeLista: valorDeLista,
  valorUnico: valorUnico
};
