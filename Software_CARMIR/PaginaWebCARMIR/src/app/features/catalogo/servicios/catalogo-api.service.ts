import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Categoria,
  Cotizacion,
  DatosPersonalizacion,
  FichaProducto,
  FiltrosCatalogo,
  Plantilla,
  ProductoCatalogo,
  SolicitudCotizacion,
} from '../modelos/catalogo.models';

// Formas de las respuestas que vienen envueltas en un objeto
interface RespuestaCategorias {
  categorias: Categoria[];
}

interface RespuestaProductos {
  productos: ProductoCatalogo[];
}

interface RespuestaPlantillas {
  plantillas: Plantilla[];
}

/**
 * Servicio que habla con la API del Catálogo Público (ApiCARMIR).
 * Cada método regresa una promesa, así las pantallas usan async/await con try/catch.
 * Los errores ya llegan convertidos en ErrorApi por el interceptor.
 */
@Injectable({ providedIn: 'root' })
export class CatalogoApiService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl + '/catalogo';

  // RF-1: categorías activas con productos
  async listarCategorias(): Promise<Categoria[]> {
    const respuesta = await firstValueFrom(this.http.get<RespuestaCategorias>(this.url + '/categorias'));
    return respuesta.categorias;
  }

  // RF-1: productos con filtros (categoría, búsqueda y orden)
  async listarProductos(filtros: FiltrosCatalogo): Promise<ProductoCatalogo[]> {
    let parametros = new HttpParams().set('orden', filtros.orden);
    if (filtros.idCategoria > 0) {
      parametros = parametros.set('categoria', String(filtros.idCategoria));
    }
    if (filtros.buscar !== '') {
      parametros = parametros.set('buscar', filtros.buscar);
    }
    const respuesta = await firstValueFrom(
      this.http.get<RespuestaProductos>(this.url + '/productos', { params: parametros }),
    );
    return respuesta.productos;
  }

  // RF-2: ficha del producto
  obtenerFicha(idPostre: number): Promise<FichaProducto> {
    return firstValueFrom(this.http.get<FichaProducto>(this.url + '/productos/' + idPostre));
  }

  // RF-3: características, opciones y exclusiones para personalizar a partir de la plantilla
  obtenerPersonalizacion(idPostre: number): Promise<DatosPersonalizacion> {
    return firstValueFrom(
      this.http.get<DatosPersonalizacion>(this.url + '/productos/' + idPostre + '/personalizacion'),
    );
  }

  // RF-4: plantillas (productos base) de la misma categoría
  async listarPlantillas(idPostre: number): Promise<Plantilla[]> {
    const respuesta = await firstValueFrom(
      this.http.get<RespuestaPlantillas>(this.url + '/productos/' + idPostre + '/plantillas'),
    );
    return respuesta.plantillas;
  }

  // RF-3 paso 7: la API valida otra vez y da el precio vigente (no guarda nada)
  cotizar(solicitud: SolicitudCotizacion): Promise<Cotizacion> {
    return firstValueFrom(this.http.post<Cotizacion>(this.url + '/cotizar', solicitud));
  }
}
