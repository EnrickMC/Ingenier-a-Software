import { Routes } from '@angular/router';

/**
 * Rutas del Módulo 1 · Catálogo Público.
 * Cada página se descarga solo cuando se visita (carga diferida).
 * Las páginas usan "export default", por eso basta con regresar el import().
 */
export const RUTAS_CATALOGO: Routes = [
  { path: '', title: 'Catálogo - CARMIR', loadComponent: cargarCatalogo },
  { path: 'producto/:id', title: 'Producto - CARMIR', loadComponent: cargarFicha },
  { path: 'producto/:id/personalizar', title: 'Personalizar pedido - CARMIR', loadComponent: cargarPersonalizar },
];

function cargarCatalogo() {
  return import('./paginas/catalogo/catalogo-pagina');
}

function cargarFicha() {
  return import('./paginas/ficha-producto/ficha-producto-pagina');
}

function cargarPersonalizar() {
  return import('./paginas/personalizar/personalizar-pagina');
}
