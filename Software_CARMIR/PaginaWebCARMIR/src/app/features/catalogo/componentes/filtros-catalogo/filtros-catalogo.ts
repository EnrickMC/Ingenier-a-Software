import { Component, input, output } from '@angular/core';
import { FiltrosCatalogo } from '../../modelos/catalogo.models';

/**
 * Filtros del catálogo (RF-1): buscador y botón «Todos».
 * El orden se queda como esté en la dirección (por nombre si no se indica).
 * No consulta nada: solo avisa a la página cuando el cliente cambia un filtro.
 * Se dibuja en el menú de arriba (encabezado), en la misma fila que el logo y el menú.
 * Las categorías se eligen en la página del catálogo, junto al total de postres.
 */
@Component({
  selector: 'app-filtros-catalogo',
  templateUrl: './filtros-catalogo.html',
  styleUrl: './filtros-catalogo.css',
})
export class FiltrosCatalogoComponente {
  readonly filtros = input.required<FiltrosCatalogo>();
  readonly filtrosCambiados = output<FiltrosCatalogo>();

  elegirCategoria(idCategoria: number): void {
    const actuales = this.filtros();
    this.filtrosCambiados.emit({ idCategoria: idCategoria, buscar: actuales.buscar, orden: actuales.orden });
  }

  buscar(texto: string): void {
    const actuales = this.filtros();
    this.filtrosCambiados.emit({ idCategoria: actuales.idCategoria, buscar: texto.trim(), orden: actuales.orden });
  }

  // El formulario de búsqueda no recarga la página; solo manda el texto
  alEnviarBusqueda(evento: Event, texto: string): void {
    evento.preventDefault();
    this.buscar(texto);
  }
}
