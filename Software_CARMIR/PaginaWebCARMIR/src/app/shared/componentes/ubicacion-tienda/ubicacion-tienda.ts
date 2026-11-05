import { Component, input } from '@angular/core';
import { DIRECCION_TIENDA, ENLACE_MAPA_TIENDA } from '../../../core/datos-tienda';

/**
 * RF-10 · Paso 3 / RF-12 · Paso 8: tarjeta con la dirección de la tienda y «Ver en el mapa»
 * (donde se paga en efectivo y se recogen los pedidos).
 * Se usa en Entrega, Pago, Confirmación y Mis pedidos.
 * "titulo" cambia según la pantalla: «Recoge tu pedido en», «Paga en efectivo en», etc.
 */
@Component({
  selector: 'app-ubicacion-tienda',
  template: `
    <div class="ubicacion">
      <svg class="pin" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 2a7 7 0 0 0-7 7c0 5.2 7 13 7 13s7-7.8 7-13a7 7 0 0 0-7-7zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5z" />
      </svg>
      <div>
        <strong>{{ titulo() }}</strong>
        <span class="direccion">{{ direccion }}</span>
        <a [href]="enlaceMapa" target="_blank" rel="noopener noreferrer">Ver en el mapa</a>
      </div>
    </div>
  `,
  styles: `
    .ubicacion {
      display: flex;
      gap: 10px;
      align-items: flex-start;
      padding: 12px 14px;
      border-radius: var(--radio-chico);
      background: var(--color-crema-claro);
      border: 1px dashed var(--color-encabezado);
      margin: 0 0 16px;
      text-align: left;
    }

    .pin {
      width: 22px;
      height: 22px;
      flex-shrink: 0;
      fill: var(--color-acento-oscuro);
      margin-top: 2px;
    }

    strong,
    .direccion {
      display: block;
    }

    .direccion {
      color: var(--color-texto);
      overflow-wrap: anywhere;
    }

    a {
      font-size: 0.9rem;
      font-weight: 600;
    }
  `,
})
export class UbicacionTienda {
  readonly titulo = input('Nuestra tienda');
  protected readonly direccion = DIRECCION_TIENDA;
  protected readonly enlaceMapa = ENLACE_MAPA_TIENDA;
}
