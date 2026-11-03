import { Routes } from '@angular/router';

/**
 * Rutas del carrito (Módulo 2 · Portal de Pedidos, RF-5 a RF-8).
 * Los pasos siguientes (entrega, pago y confirmación) están en features/pedidos.
 */
export const RUTAS_CARRITO: Routes = [{ path: '', title: 'Carrito - CARMIR', loadComponent: cargarCarrito }];

function cargarCarrito() {
  return import('./paginas/carrito/carrito-pagina');
}
