import { Component, input, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';

// Imagen que se usa cuando el postre no tiene foto o la foto no carga (RF-1)
const IMAGEN_GENERICA = 'img/postre-generico.svg';

// Fotos elegidas como archivo en el Panel: la API las entrega en /api/imagenes/...
const PREFIJO_API = '/api/';

/**
 * Foto de un postre. Si no tiene enlace o el enlace falla, muestra la imagen genérica.
 * Si el enlace es de la API (/api/imagenes/...), se completa con la dirección de la API.
 */
@Component({
  selector: 'app-imagen-postre',
  template: `
    <img
      class="imagen"
      [src]="rutaImagen()"
      [alt]="'Foto de ' + nombre()"
      loading="lazy"
      (error)="alFallar()"
    />
  `,
  styles: `
    :host {
      display: block;
    }

    .imagen {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: inherit;
      background: var(--color-crema-claro);
    }
  `,
})
export class ImagenPostre {
  readonly link = input<string | null>(null);
  readonly nombre = input('postre');
  protected readonly fallo = signal(false);

  rutaImagen(): string {
    const link = this.link();
    if (this.fallo() || link === null || link.trim() === '') {
      return IMAGEN_GENERICA;
    }
    // RF-1 · Paso 4: la foto que se guardó como archivo de la computadora la sirve la API
    if (link.startsWith(PREFIJO_API)) {
      return environment.apiUrl + '/' + link.substring(PREFIJO_API.length);
    }
    return link;
  }

  alFallar(): void {
    this.fallo.set(true);
  }
}
