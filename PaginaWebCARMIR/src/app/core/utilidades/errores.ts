import { environment } from '../../../environments/environment';
import { CODIGO_SIN_CONEXION, CODIGO_SIN_CONEXION_BD, ErrorApi, ErrorDeCampo } from '../modelos/error-api';

/**
 * Revisa si un error atrapado en un try/catch es un ErrorApi (lo crea el interceptor).
 * "error is ErrorApi" le avisa a TypeScript que, si regresa true, el error es un ErrorApi.
 */
export function esErrorApi(error: unknown): error is ErrorApi {
  if (error === null || typeof error !== 'object') {
    return false;
  }
  return 'codigo' in error && 'mensaje' in error && 'estado' in error;
}

// true si el error es porque no hay conexión con el servidor o la base de datos
export function esSinConexion(error: unknown): boolean {
  if (!esErrorApi(error)) {
    return true;
  }
  return error.codigo === CODIGO_SIN_CONEXION || error.estado >= 500;
}

// true si la API respondió 404 con ese código (por ejemplo PRODUCTO_NO_DISPONIBLE)
export function tieneCodigo(error: unknown, codigo: string): boolean {
  if (!esErrorApi(error)) {
    return false;
  }
  return error.codigo === codigo;
}

// Mensaje para mostrar: el de la API si es un error de datos; si no, el mensaje del caso de uso
export function mensajeDeError(error: unknown, mensajeSinConexion: string): string {
  if (esSinConexion(error)) {
    return mensajeSinConexion;
  }
  if (esErrorApi(error)) {
    return error.mensaje;
  }
  return mensajeSinConexion;
}

/**
 * Explica la causa de un error de conexión para que el cliente (y el equipo, en desarrollo)
 * sepa si falló la API o la base de datos. Se muestra debajo del mensaje del caso de uso.
 */
export function explicarFalla(error: unknown): string {
  if (!esErrorApi(error) || error.codigo === CODIGO_SIN_CONEXION) {
    if (environment.produccion) {
      return 'No hay comunicación con el servidor. Revisa tu conexión a internet e inténtalo de nuevo.';
    }
    return (
      'La API no responde en ' + environment.apiUrl + '. Revisa que ApiCARMIR esté encendida ' +
      '(en otra terminal: cd ApiCARMIR y npm start) y que abriste la página como http://localhost:4200.'
    );
  }
  if (error.codigo === CODIGO_SIN_CONEXION_BD) {
    if (environment.produccion) {
      return 'El servidor no pudo consultar la información. Inténtalo de nuevo en unos minutos.';
    }
    return (
      'La API está encendida pero no pudo conectarse a MySQL. Revisa el archivo ApiCARMIR/.env ' +
      '(usuario, contraseña y base) y que MySQL esté encendido. Diagnóstico: cd ApiCARMIR y npm run diagnostico.'
    );
  }
  let texto = 'Ocurrió un error en el servidor.';
  if (error.referencia !== '') {
    texto = texto + ' Referencia: ' + error.referencia + '.';
  }
  if (!environment.produccion) {
    texto = texto + ' Revisa la terminal de ApiCARMIR o ejecuta npm run diagnostico (por ejemplo, falta una vista de la base).';
  }
  return texto;
}

/**
 * Errores de campos que manda la API en "detalles": [{ campo, mensaje }].
 * Sirve para marcar en rojo el campo exacto (por ejemplo la fecha cuando el día ya se llenó).
 */
export function erroresDeCampo(error: unknown): ErrorDeCampo[] {
  const lista: ErrorDeCampo[] = [];
  if (!esErrorApi(error) || !Array.isArray(error.detalles)) {
    return lista;
  }
  for (const detalle of error.detalles) {
    if (detalle !== null && typeof detalle === 'object' && typeof detalle.campo === 'string' && typeof detalle.mensaje === 'string') {
      lista.push({ campo: detalle.campo, mensaje: detalle.mensaje });
    }
  }
  return lista;
}
