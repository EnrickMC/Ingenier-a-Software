import { Injectable, inject, signal } from '@angular/core';
import { Params, Router } from '@angular/router';
import { FiltrosCatalogo } from '../modelos/catalogo.models';

/**
 * Comparte los filtros del catálogo (RF-1) entre la página del catálogo y el encabezado.
 * El buscador se dibuja en el menú de arriba (encabezado); las categorías, en la página.
 *  - La página avisa cuándo está abierta y qué filtros están activos.
 *  - El encabezado muestra los filtros solo mientras la página del catálogo está abierta.
 *  - Al cambiar un filtro se cambia la dirección (?categoria=1&buscar=...&orden=...) y la página vuelve a consultar.
 */
@Injectable({ providedIn: 'root' })
export class FiltrosCatalogoService {
  private readonly router = inject(Router);

  // true mientras la página del catálogo está en pantalla
  readonly visibles = signal(false);
  readonly filtros = signal<FiltrosCatalogo>({ idCategoria: 0, buscar: '', orden: 'nombre' });

  // RF-1 · Paso 5: el cliente cambió la categoría, la búsqueda o el orden; se pasa a la dirección.
  aplicar(nuevos: FiltrosCatalogo): void {
    const parametros: Params = { categoria: null, buscar: null, orden: null };
    if (nuevos.idCategoria > 0) {
      parametros['categoria'] = nuevos.idCategoria;
    }
    if (nuevos.buscar !== '') {
      parametros['buscar'] = nuevos.buscar;
    }
    if (nuevos.orden !== 'nombre') {
      parametros['orden'] = nuevos.orden;
    }
    this.router.navigate(['/'], { queryParams: parametros });
  }
}
