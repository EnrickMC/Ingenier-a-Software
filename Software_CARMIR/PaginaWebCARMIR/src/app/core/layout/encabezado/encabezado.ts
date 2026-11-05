import { Component, inject, signal } from '@angular/core';
import { IsActiveMatchOptions, RouterLink, RouterLinkActive } from '@angular/router';
import { FiltrosCatalogoComponente } from '../../../features/catalogo/componentes/filtros-catalogo/filtros-catalogo';
import { FiltrosCatalogoService } from '../../../features/catalogo/servicios/filtros-catalogo.service';
import { CarritoService } from '../../servicios/carrito.service';

/**
 * Encabezado de todas las páginas: logo de CARMIR, menú (Catálogo, Mis pedidos, Carrito) y contador del carrito.
 * En el catálogo también trae los filtros (buscador, orden y categorías), todo en el menú de arriba.
 * En celular el menú se abre con el botón de las tres rayas.
 */
@Component({
  selector: 'app-encabezado',
  imports: [RouterLink, RouterLinkActive, FiltrosCatalogoComponente],
  templateUrl: './encabezado.html',
  styleUrl: './encabezado.css',
})
export class Encabezado {
  protected readonly carrito = inject(CarritoService);
  protected readonly filtrosCatalogo = inject(FiltrosCatalogoService);
  protected readonly menuAbierto = signal(false);
  // «Catálogo» se marca activo aunque haya filtros en la dirección (?categoria=1...)
  protected readonly opcionesCatalogoActivo: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    matrixParams: 'ignored',
    fragment: 'ignored',
  };

  alternarMenu(): void {
    this.menuAbierto.set(!this.menuAbierto());
  }

  cerrarMenu(): void {
    this.menuAbierto.set(false);
  }
}
