import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Observable, catchError, throwError, timeout } from 'rxjs';
import { CODIGO_SIN_CONEXION, ErrorApi } from '../modelos/error-api';

// Tiempo máximo de espera de una respuesta de la API (milisegundos)
const TIEMPO_MAXIMO = 15000;

/**
 * Interceptor de errores: toda respuesta con error de la API se convierte en un ErrorApi
 * sencillo { estado, codigo, mensaje, referencia, detalles }, así las pantallas no tienen que revisar HttpErrorResponse.
 */
export function erroresApiInterceptor(
  peticion: HttpRequest<unknown>,
  siguiente: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  return siguiente(peticion).pipe(timeout(TIEMPO_MAXIMO), catchError(convertirError));
}

// Convierte cualquier error en un ErrorApi
// Excepción «Hay conexión con el servidor? No» de todos los RF de la web: aquí se detecta la API apagada o sin respuesta.
function convertirError(error: unknown): Observable<never> {
  const errorApi: ErrorApi = {
    estado: 0,
    codigo: CODIGO_SIN_CONEXION,
    mensaje: 'No fue posible conectar con el servidor',
    referencia: '',
    detalles: null,
  };

  if (error instanceof HttpErrorResponse) {
    errorApi.estado = error.status;
    // La API responde { codigo, mensaje }; si no viene así, se deja el mensaje genérico
    const cuerpo = error.error;
    if (cuerpo !== null && typeof cuerpo === 'object' && typeof cuerpo.mensaje === 'string') {
      errorApi.codigo = cuerpo.codigo;
      errorApi.mensaje = cuerpo.mensaje;
      if (typeof cuerpo.referencia === 'string') {
        errorApi.referencia = cuerpo.referencia;
      }
      // Algunos errores traen detalles (campos con error, precios nuevos...)
      if (cuerpo.detalles !== undefined) {
        errorApi.detalles = cuerpo.detalles;
      }
    } else if (error.status !== 0) {
      errorApi.codigo = 'ERROR_' + error.status;
    }
  }

  return throwError(crearError(errorApi));
}

// throwError recibe una función que crea el error
function crearError(errorApi: ErrorApi) {
  return function (): ErrorApi {
    return errorApi;
  };
}
