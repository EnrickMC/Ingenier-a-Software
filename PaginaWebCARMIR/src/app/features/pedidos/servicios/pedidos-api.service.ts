import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ArticuloCarrito } from '../../../core/modelos/articulo-carrito';
import {
  ArticuloSolicitud,
  Cobertura,
  ComprobantePedido,
  ReglasEntrega,
  RespuestaComprobante,
  RevisionCarrito,
  SeguimientoPedido,
  SolicitudComprobante,
  SolicitudPedido,
} from '../modelos/pedidos.models';

/**
 * Servicio que habla con la API del Portal de Pedidos (ApiCARMIR).
 * Igual que en el catálogo: cada método regresa una promesa y los errores llegan como ErrorApi.
 * La API vuelve a revisar y a calcular todo; lo que manda la página solo sirve para avisar cambios.
 */
@Injectable({ providedIn: 'root' })
export class PedidosApiService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  // RF-5: disponibilidad y precio vigente de cada postre del carrito
  validarCarrito(articulos: ArticuloCarrito[]): Promise<RevisionCarrito> {
    const cuerpo = { articulos: articulosParaApi(articulos) };
    return firstValueFrom(this.http.post<RevisionCarrito>(this.url + '/carrito/validar', cuerpo));
  }

  // RF-9 y RF-10: días, horarios, cupo, anticipo y métodos de pago
  leerReglasEntrega(): Promise<ReglasEntrega> {
    return firstValueFrom(this.http.get<ReglasEntrega>(this.url + '/entrega/reglas'));
  }

  // RF-9 paso 7: si hay envío al código postal y cuánto cuesta
  revisarCobertura(codigoPostal: string): Promise<Cobertura> {
    return firstValueFrom(this.http.get<Cobertura>(this.url + '/entrega/cobertura/' + encodeURIComponent(codigoPostal)));
  }

  // RF-11: registra el pedido y regresa el comprobante
  registrarPedido(solicitud: SolicitudPedido): Promise<ComprobantePedido> {
    return firstValueFrom(this.http.post<ComprobantePedido>(this.url + '/pedidos', solicitud));
  }

  // RF-12: seguimiento con folio y teléfono
  consultarPedido(folio: string, telefono: string): Promise<SeguimientoPedido> {
    return firstValueFrom(
      this.http.post<SeguimientoPedido>(this.url + '/pedidos/consulta', { folio: folio, telefono: telefono }),
    );
  }

  // RF-12: comprobante de transferencia (foto/PDF subido desde el equipo y/o enlace)
  enviarComprobante(solicitud: SolicitudComprobante): Promise<RespuestaComprobante> {
    return firstValueFrom(this.http.post<RespuestaComprobante>(this.url + '/pedidos/comprobante', solicitud));
  }
}

// Solo se mandan los datos que la API necesita (sin nombres ni fotos)
export function articulosParaApi(articulos: ArticuloCarrito[]): ArticuloSolicitud[] {
  const lista: ArticuloSolicitud[] = [];
  for (const articulo of articulos) {
    const opciones: { idOpcion: number; cantidad: number }[] = [];
    for (const opcion of articulo.opciones) {
      opciones.push({ idOpcion: opcion.idOpcion, cantidad: opcion.cantidad });
    }
    lista.push({
      idPostre: articulo.idPostre,
      opciones: opciones,
      cantidad: articulo.cantidad,
      dedicatoria: articulo.dedicatoria,
      comentario: articulo.comentario,
      precioUnitario: articulo.precioUnitario,
    });
  }
  return lista;
}
