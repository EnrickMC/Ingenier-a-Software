/*
 * Error "controlado" de la API.
 * Lleva el código HTTP, un código corto para Angular y el mensaje que verá el cliente.
 * Ejemplo: new ErrorApi(404, 'PRODUCTO_NO_DISPONIBLE', 'Este producto ya no está disponible')
 */
class ErrorApi extends Error {
  constructor(estadoHttp, codigo, mensaje, detalles) {
    super(mensaje);
    this.name = 'ErrorApi';
    this.estadoHttp = estadoHttp;
    this.codigo = codigo;
    // Lista opcional de errores de validación (por ejemplo, de la personalización)
    if (detalles === undefined) {
      this.detalles = null;
    } else {
      this.detalles = detalles;
    }
  }
}

module.exports = ErrorApi;
