/**
 * Datos fijos de la tienda física de CARMIR.
 * Aquí se paga en efectivo y se recogen los pedidos («Recoger en tienda»).
 * Si la tienda cambia de lugar, solo se cambia este archivo (y ApiCARMIR/src/config/tienda.js).
 */
export const NOMBRE_NEGOCIO = 'Delicias de Postres';
export const DIRECCION_TIENDA = 'José Ma. Rodríguez 231, Prado Hermoso, 37238 León de los Aldama';
// Enlace para abrir la dirección en Google Maps
export const ENLACE_MAPA_TIENDA =
  'https://www.google.com/maps/search/?api=1&query=' +
  encodeURIComponent('José María Rodríguez 231, Prado Hermoso, 37238 León de los Aldama, Gto.');
