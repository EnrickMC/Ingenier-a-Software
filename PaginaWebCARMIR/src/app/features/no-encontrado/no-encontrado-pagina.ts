import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MensajeEstado } from '../../shared/componentes/mensaje-estado/mensaje-estado';

// Página para direcciones que no existen
@Component({
  selector: 'app-no-encontrado-pagina',
  imports: [RouterLink, MensajeEstado],
  template: `
    <div class="contenedor" style="padding: 40px var(--espacio-lateral)">
      <app-mensaje-estado tipo="info" titulo="No encontramos esta página">
        <a routerLink="/" class="btn btn-principal">Ir al catálogo</a>
      </app-mensaje-estado>
    </div>
  `,
})
export default class NoEncontradoPagina {}
