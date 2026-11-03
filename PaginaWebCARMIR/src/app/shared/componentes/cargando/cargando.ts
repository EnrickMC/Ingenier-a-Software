import { Component, input } from '@angular/core';

// Indicador de "cargando" con un círculo que gira
@Component({
  selector: 'app-cargando',
  template: `
    <div class="cargando" role="status">
      <span class="circulo" aria-hidden="true"></span>
      <span>{{ texto() }}</span>
    </div>
  `,
  styles: `
    .cargando {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 32px 0;
      color: var(--color-texto-suave);
    }

    .circulo {
      width: 28px;
      height: 28px;
      border: 4px solid var(--color-crema);
      border-top-color: var(--color-acento);
      border-radius: 50%;
      animation: girar 0.8s linear infinite;
    }

    @keyframes girar {
      to {
        transform: rotate(360deg);
      }
    }
  `,
})
export class Cargando {
  readonly texto = input('Cargando...');
}
