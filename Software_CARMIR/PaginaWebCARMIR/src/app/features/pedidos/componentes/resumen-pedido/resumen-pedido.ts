import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { ArticuloCarrito } from '../../../../core/modelos/articulo-carrito';
import { calcularAnticipo, redondear } from '../../utilidades/validacion-pedido';

/**
 * Panel «Resumen de tu pedido» (izquierda en el diseño "Carrito y Checkout").
 * Muestra los postres, el envío, el total y, si ya se eligió, lo que se paga para iniciar.
 * Los importes son los confirmados por la API al revisar el carrito.
 */
@Component({
  selector: 'app-resumen-pedido',
  imports: [CurrencyPipe],
  templateUrl: './resumen-pedido.html',
  styleUrl: './resumen-pedido.css',
})
export class ResumenPedido {
  readonly articulos = input.required<ArticuloCarrito[]>();
  // true si es envío a domicilio (aunque todavía no se sepa el costo)
  readonly conEnvio = input(false);
  // -1 = todavía no se revisa el código postal
  readonly costoEnvio = input(0);
  // '' = todavía no se elige cómo pagar
  readonly modalidadPago = input('');
  readonly porcentajeAnticipo = input(50);

  subtotal(articulo: ArticuloCarrito): number {
    return redondear(articulo.precioUnitario * articulo.cantidad);
  }

  subtotalProductos(): number {
    let total = 0;
    for (const articulo of this.articulos()) {
      total = total + this.subtotal(articulo);
    }
    return redondear(total);
  }

  envio(): number {
    if (!this.conEnvio() || this.costoEnvio() < 0) {
      return 0;
    }
    return this.costoEnvio();
  }

  envioPendiente(): boolean {
    return this.conEnvio() && this.costoEnvio() < 0;
  }

  total(): number {
    return redondear(this.subtotalProductos() + this.envio());
  }

  montoParaIniciar(): number {
    return calcularAnticipo(this.total(), this.porcentajeAnticipo(), this.modalidadPago());
  }

  // "Grande, Tres leches, Flores ×2"
  opciones(articulo: ArticuloCarrito): string {
    const partes: string[] = [];
    for (const opcion of articulo.opciones) {
      if (opcion.cantidad > 1) {
        partes.push(opcion.nombre + ' ×' + opcion.cantidad);
      } else {
        partes.push(opcion.nombre);
      }
    }
    return partes.join(', ');
  }
}
