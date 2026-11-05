import { Component, input } from '@angular/core';

/**
 * Mensaje grande para estados de una pantalla: vacío, error o información.
 * Los botones (Reintentar, Limpiar búsqueda...) se ponen dentro de la etiqueta:
 *   <app-mensaje-estado tipo="error" titulo="No fue posible cargar el catálogo">
 *     <button ...>Reintentar</button>
 *   </app-mensaje-estado>
 */
@Component({
  selector: 'app-mensaje-estado',
  templateUrl: './mensaje-estado.html',
  styleUrl: './mensaje-estado.css',
})
export class MensajeEstado {
  readonly titulo = input.required<string>();
  readonly texto = input('');
  // 'vacio' | 'error' | 'info'
  readonly tipo = input('info');

  // Los errores se anuncian de inmediato a los lectores de pantalla
  rol(): string {
    if (this.tipo() === 'error') {
      return 'alert';
    }
    return 'status';
  }
}
