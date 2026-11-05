/**
 * Error que regresa la API de CARMIR. Todas las respuestas de error tienen esta forma:
 * { codigo: 'PRODUCTO_NO_DISPONIBLE', mensaje: 'Este producto ya no está disponible' }
 */
export interface ErrorApi {
  // Código HTTP (0 = no hubo respuesta: el servidor está apagado o no hay internet)
  estado: number;
  codigo: string;
  mensaje: string;
  // Número que el servidor escribió en su consola junto con el error (vacío si no hay)
  referencia: string;
  // Información extra de algunos errores (campos con error, precios nuevos...). null si no trae.
  detalles: unknown;
}

// Error de un campo del formulario: { campo: 'telefono', mensaje: '...' }
export interface ErrorDeCampo {
  campo: string;
  mensaje: string;
}

// Códigos que usan varias pantallas
// La API no respondió (apagada, puerto equivocado, sin internet o bloqueada por CORS)
export const CODIGO_SIN_CONEXION = 'SIN_CONEXION';
// La API respondió, pero no pudo conectarse a MySQL
export const CODIGO_SIN_CONEXION_BD = 'SIN_CONEXION_BD';
