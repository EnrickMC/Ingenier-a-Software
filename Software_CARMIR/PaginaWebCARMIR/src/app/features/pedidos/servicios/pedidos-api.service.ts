import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ArticuloCarrito } from '../../../core/modelos/articulo-carrito';
import { comentarioConTopper } from '../../catalogo/utilidades/reglas-configuracion';
import { ContactoConsulta } from '../utilidades/validacion-pedido';
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

  // RF-6: disponibilidad y precio vigente de cada postre del carrito
  validarCarrito(articulos: ArticuloCarrito[]): Promise<RevisionCarrito> {
    const cuerpo = { articulos: articulosParaApi(articulos) };
    return firstValueFrom(this.http.post<RevisionCarrito>(this.url + '/carrito/validar', cuerpo));
  }

  // RF-10 y RF-11: días, horarios, cupo, anticipo y métodos de pago
  leerReglasEntrega(): Promise<ReglasEntrega> {
    return firstValueFrom(this.http.get<ReglasEntrega>(this.url + '/entrega/reglas'));
  }

  // RF-10 paso 7: si hay envío al código postal y cuánto cuesta
  revisarCobertura(codigoPostal: string): Promise<Cobertura> {
    return firstValueFrom(this.http.get<Cobertura>(this.url + '/entrega/cobertura/' + encodeURIComponent(codigoPostal)));
  }

  // RF-12: registra el pedido y regresa el comprobante
  registrarPedido(solicitud: SolicitudPedido): Promise<ComprobantePedido> {
    return firstValueFrom(this.http.post<ComprobantePedido>(this.url + '/pedidos', solicitud));
  }

  // RF-13: seguimiento con folio y teléfono (o con folio y correo)
  consultarPedido(folio: string, contacto: ContactoConsulta): Promise<SeguimientoPedido> {
    let cuerpo: Record<string, string> = { folio: folio, telefono: contacto.telefono };
    if (contacto.telefono === '') {
      cuerpo = { folio: folio, correo: contacto.correo };
    }
    return firstValueFrom(this.http.post<SeguimientoPedido>(this.url + '/pedidos/consulta', cuerpo));
  }

  // RF-14 · Pasos 5 a 8: comprobante de transferencia (foto/PDF subido desde el equipo y/o enlace)
  enviarComprobante(solicitud: SolicitudComprobante): Promise<RespuestaComprobante> {
    return firstValueFrom(this.http.post<RespuestaComprobante>(this.url + '/pedidos/comprobante', solicitud));
  }
}

// Solo se mandan los datos que la API necesita (sin nombres ni fotos)
export function articulosParaApi(articulos: ArticuloCarrito[]): ArticuloSolicitud[] {
  const lista: ArticuloSolicitud[] = [];
  for (const articulo of articulos) {
    const opciones: { idOpcion: number; cantidad: number; capa: number }[] = [];
    for (const opcion of articulo.opciones) {
      // Capa del pastel (1 = Base); los artículos viejos del carrito no la traen
      let capa = 1;
      if (opcion.capa !== undefined && opcion.capa > 1) {
        capa = opcion.capa;
      }
      opciones.push({ idOpcion: opcion.idOpcion, cantidad: opcion.cantidad, capa: capa });
    }
    lista.push({
      idPostre: articulo.idPostre,
      opciones: opciones,
      cantidad: articulo.cantidad,
      dedicatoria: articulo.dedicatoria,
      // Si el mensaje va en un topper se avisa en las indicaciones
      comentario: comentarioConTopper(articulo.comentario, articulo.dedicatoria, articulo.mensajeEnTopper === true),
      precioUnitario: articulo.precioUnitario,
    });
  }
  return lista;
}
