import { Routes } from '@angular/router';
import { RUTAS_CATALOGO } from './features/catalogo/catalogo.routes';

/**
 * Rutas de toda la página. Cada módulo tiene su propio archivo de rutas en features/.
 *  - Módulo 1 · Catálogo Público: '' (inicio), 'producto/:id', 'producto/:id/personalizar'
 *  - Módulo 2 · Portal de Pedidos: 'carrito', 'pedido/entrega', 'pedido/pago', 'pedido/confirmacion', 'mis-pedidos'
 */
export const routes: Routes = [
  { path: '', children: RUTAS_CATALOGO },
  { path: 'carrito', loadChildren: cargarRutasCarrito },
  { path: 'pedido', loadChildren: cargarRutasPedido },
  { path: 'mis-pedidos', title: 'Mis pedidos - CARMIR', loadComponent: cargarMisPedidos },
  { path: '**', title: 'Página no encontrada - CARMIR', loadComponent: cargarNoEncontrado },
];

function cargarRutasCarrito() {
  return import('./features/carrito/carrito.routes').then(tomarRutasCarrito);
}

function tomarRutasCarrito(archivo: { RUTAS_CARRITO: Routes }): Routes {
  return archivo.RUTAS_CARRITO;
}

function cargarRutasPedido() {
  return import('./features/pedidos/pedidos.routes').then(tomarRutasPedido);
}

function tomarRutasPedido(archivo: { RUTAS_PEDIDO: Routes }): Routes {
  return archivo.RUTAS_PEDIDO;
}

function cargarMisPedidos() {
  return import('./features/pedidos/paginas/mis-pedidos/mis-pedidos-pagina');
}

function cargarNoEncontrado() {
  return import('./features/no-encontrado/no-encontrado-pagina');
}
