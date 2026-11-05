import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DIRECCION_TIENDA, ENLACE_MAPA_TIENDA, NOMBRE_NEGOCIO } from '../../datos-tienda';

/**
 * Pie de página de todas las páginas, a todo el ancho y delgado:
 *  - Izquierda: logo, nombre del negocio y dirección de la tienda (pago en efectivo y recolección).
 *  - Derecha: enlaces del menú y «Ver en el mapa».
 *  - Línea final: derechos a la izquierda y aviso de la tienda a la derecha.
 */
@Component({
  selector: 'app-pie-pagina',
  imports: [RouterLink],
  template: `
    <footer class="pie">
      <div class="franja">
        <div class="izquierda">
          <img src="img/logo-circulo.png" alt="Logo de CARMIR" width="96" height="96" loading="lazy" />
          <div>
            <p class="negocio">CARMIR · {{ negocio }}</p>
            <p class="direccion">{{ direccion }}</p>
          </div>
        </div>

        <nav class="derecha" aria-label="Enlaces del pie de página">
          <a routerLink="/">Catálogo</a>
          <a routerLink="/carrito">Carrito</a>
          <a routerLink="/mis-pedidos">Mis pedidos</a>
          <a class="mapa" [href]="enlaceMapa" target="_blank" rel="noopener noreferrer">Ver en el mapa</a>
        </nav>
      </div>

      <div class="franja final">
        <p>&copy; {{ anio }} Sistema CARMIR - Desarrollado para {{ negocio }}.</p>
        <p>Paga en efectivo y recoge tu pedido en nuestra tienda.</p>
      </div>
    </footer>
  `,
  styles: `
    .pie {
      background-color: var(--color-texto);
      color: var(--color-crema);
      font-size: 0.9rem;
    }

    /* Celular: todo apilado y centrado */
    .franja {
      width: 100%;
      padding: 16px var(--espacio-lateral);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      text-align: center;
    }

    p {
      margin: 0;
    }

    .izquierda {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 10px;
    }

    .izquierda img {
      width: 52px;
      height: 52px;
      border-radius: 50%;
    }

    .negocio {
      font-weight: 700;
      font-size: 1rem;
    }

    .direccion {
      opacity: 0.85;
    }

    .derecha {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 6px 20px;
    }

    a {
      color: var(--color-crema);
      font-weight: 600;
      text-decoration: none;
    }

    a:hover {
      text-decoration: underline;
    }

    .mapa {
      padding: 4px 14px;
      border: 1px solid rgba(250, 237, 205, 0.6);
      border-radius: 999px;
    }

    .final {
      padding-top: 10px;
      padding-bottom: 10px;
      border-top: 1px solid rgba(250, 237, 205, 0.18);
      font-size: 0.8rem;
      opacity: 0.85;
      gap: 4px;
    }

    /* Tableta y escritorio: cosas a la izquierda y a la derecha, a todo el ancho */
    @media (min-width: 800px) {
      .franja {
        flex-direction: row;
        justify-content: space-between;
        text-align: left;
      }

      .izquierda {
        flex-direction: row;
        gap: 14px;
      }

      .derecha {
        justify-content: flex-end;
        align-items: center;
      }
    }

    @media print {
      .pie {
        display: none;
      }
    }
  `,
})
export class PiePagina {
  protected readonly anio = new Date().getFullYear();
  protected readonly negocio = NOMBRE_NEGOCIO;
  protected readonly direccion = DIRECCION_TIENDA;
  protected readonly enlaceMapa = ENLACE_MAPA_TIENDA;
}
