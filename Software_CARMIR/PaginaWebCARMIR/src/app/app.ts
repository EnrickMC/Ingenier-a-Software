import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Encabezado } from './core/layout/encabezado/encabezado';
import { PiePagina } from './core/layout/pie-pagina/pie-pagina';

/**
 * Componente raíz: encabezado, la página actual (router-outlet) y pie de página.
 */
@Component({
  imports: [RouterOutlet, Encabezado, PiePagina],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
