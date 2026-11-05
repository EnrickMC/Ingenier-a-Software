import { Injectable, signal } from '@angular/core';

/**
 * Avisos que una pantalla deja para la siguiente.
 * Ejemplo: la ficha de un producto que ya no existe regresa al catálogo
 * y el catálogo muestra «Este producto ya no está disponible».
 */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  readonly aviso = signal('');

  dejarAviso(mensaje: string): void {
    this.aviso.set(mensaje);
  }

  // Regresa el aviso pendiente y lo borra (solo se muestra una vez)
  tomarAviso(): string {
    const mensaje = this.aviso();
    this.aviso.set('');
    return mensaje;
  }
}
