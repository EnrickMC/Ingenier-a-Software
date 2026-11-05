import { Component, input } from '@angular/core';

// Los pasos para hacer un pedido, en orden
const PASOS = ['Carrito', 'Entrega', 'Pago', 'Confirmación'];

/**
 * Barra de avance del pedido: Carrito → Entrega → Pago → Confirmación.
 * "actual" va de 1 a 4.
 */
@Component({
  selector: 'app-pasos-pedido',
  template: `
    <ol class="pasos" aria-label="Pasos de tu pedido">
      @for (paso of pasos; track paso; let i = $index) {
        <li [class.hecho]="i + 1 < actual()" [class.actual]="i + 1 === actual()" [attr.aria-current]="marca(i)">
          <span class="numero" aria-hidden="true">{{ i + 1 }}</span>
          <span class="texto">{{ paso }}</span>
        </li>
      }
    </ol>
  `,
  styles: `
    .pasos {
      display: flex;
      list-style: none;
      margin: 0 0 20px;
      padding: 0;
      gap: 4px;
    }

    li {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      font-size: 0.8rem;
      color: var(--color-texto-suave);
      position: relative;
      text-align: center;
    }

    /* Línea entre un paso y el siguiente */
    li + li::before {
      content: '';
      position: absolute;
      top: 15px;
      right: 50%;
      width: 100%;
      height: 2px;
      background: var(--color-borde);
      z-index: 0;
    }

    li.hecho::before,
    li.actual::before {
      background: var(--color-acento);
    }

    .numero {
      position: relative;
      z-index: 1;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-blanco);
      border: 2px solid var(--color-borde);
      font-weight: 700;
    }

    li.hecho .numero {
      background: var(--color-acento);
      border-color: var(--color-acento);
      color: var(--color-blanco);
    }

    li.actual .numero {
      border-color: var(--color-acento);
      color: var(--color-acento-oscuro);
    }

    li.actual {
      color: var(--color-titulo);
      font-weight: 700;
    }

    @media (min-width: 600px) {
      li {
        font-size: 0.9rem;
      }
    }
  `,
})
export class PasosPedido {
  readonly actual = input(1);
  protected readonly pasos = PASOS;

  // Los lectores de pantalla anuncian cuál es el paso actual
  marca(indice: number): string | null {
    if (indice + 1 === this.actual()) {
      return 'step';
    }
    return null;
  }
}
