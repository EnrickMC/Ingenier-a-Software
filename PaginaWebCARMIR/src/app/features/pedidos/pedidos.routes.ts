import { Routes } from '@angular/router';

/**
 * Rutas del Módulo 2 · Portal de Pedidos.
 *  - pedido/entrega:      datos de contacto, entrega, fecha y horario (RF-9)
 *  - pedido/pago:         método y modalidad de pago, envío del pedido (RF-10 y RF-11)
 *  - pedido/confirmacion: comprobante con folio e instrucciones (RF-11 paso 8)
 * «Mis pedidos» (RF-12) tiene su propia ruta en app.routes.ts.
 */
export const RUTAS_PEDIDO: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'entrega' },
  { path: 'entrega', title: 'Datos de entrega - CARMIR', loadComponent: cargarEntrega },
  { path: 'pago', title: 'Pago - CARMIR', loadComponent: cargarPago },
  { path: 'confirmacion', title: 'Pedido enviado - CARMIR', loadComponent: cargarConfirmacion },
];

function cargarEntrega() {
  return import('./paginas/entrega/entrega-pagina');
}

function cargarPago() {
  return import('./paginas/pago/pago-pagina');
}

function cargarConfirmacion() {
  return import('./paginas/confirmacion/confirmacion-pagina');
}
