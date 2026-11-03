/**
 * Modelos del Catálogo Público: la forma de los datos que regresa la API.
 *
 * Concepto clave: cada PRODUCTO del catálogo es una PLANTILLA BASE
 * (un Postre con nombre y visible en la tienda). Sus opciones son el punto de partida
 * que el cliente puede personalizar.
 */

// ===================== RF-1: catálogo =====================

export interface Categoria {
  idCategoria: number;
  nombre: string;
  descripcion: string | null;
  totalProductos: number;
}

export interface ProductoCatalogo {
  idPostre: number;
  nombre: string;
  descripcion: string | null;
  linkFotografia: string | null;
  precio: number;
  idCategoria: number;
  nombreCategoria: string;
}

// Orden que puede elegir el cliente
export type OrdenCatalogo = 'nombre' | 'precio_asc' | 'precio_desc';

export interface FiltrosCatalogo {
  idCategoria: number;
  buscar: string;
  orden: OrdenCatalogo;
}

// ===================== RF-2: ficha del producto =====================

export interface OpcionIncluida {
  nombre: string;
  cantidad: number;
}

// Opciones que trae la plantilla, agrupadas por característica
export interface GrupoIncluido {
  idCaracteristica: number;
  caracteristica: string;
  opciones: OpcionIncluida[];
}

export interface CaracteristicaResumen {
  nombre: string;
  obligatoria: boolean;
  opciones: string[];
}

export interface FichaProducto {
  producto: ProductoCatalogo;
  incluye: GrupoIncluido[];
  opcionesRetiradas: string[];
  ingredientes: string[];
  caracteristicas: CaracteristicaResumen[];
  sePuedePersonalizar: boolean;
  motivoNoPersonalizable: string | null;
}

// ===================== RF-3: personalización =====================

export interface OpcionPersonalizable {
  idOpcion: number;
  idCaracteristica: number;
  nombre: string;
  descripcion: string | null;
  linkImagen: string | null;
  // Precio de la opción sin escalar (la API ya lo calculó; el costo nunca llega al navegador)
  precioReferencia: number;
  seEscala: boolean;
  multiplicadorIngredientes: number;
  puntosSeleccion: number;
  multiplicadorPresupuesto: number;
}

export interface CaracteristicaPersonalizable {
  idCaracteristica: number;
  nombre: string;
  descripcion: string | null;
  obligatoria: boolean;
  seleccionMultiple: boolean;
  presupuestoSeleccion: number;
  ordenMostrar: number;
  opciones: OpcionPersonalizable[];
}

export interface Exclusion {
  idOpcion: number;
  idOpcionExcluida: number;
  nombreOpcion: string;
  nombreOpcionExcluida: string;
  motivo: string | null;
}

// Una opción elegida y cuántas veces se eligió
export interface OpcionElegida {
  idOpcion: number;
  cantidad: number;
}

export interface DatosPersonalizacion {
  producto: ProductoCatalogo;
  caracteristicas: CaracteristicaPersonalizable[];
  exclusiones: Exclusion[];
  seleccionInicial: OpcionElegida[];
  opcionesRetiradas: string[];
  sePuedePersonalizar: boolean;
  motivoNoPersonalizable: string | null;
}

// Error de una regla al armar el postre
export interface ErrorConfiguracion {
  // obligatoria | seleccionUnica | presupuesto | exclusion | disponibilidad | opciones
  tipo: string;
  idCaracteristica: number;
  mensaje: string;
}

// Lo que se manda a POST /api/catalogo/cotizar
export interface SolicitudCotizacion {
  idPostre: number;
  opciones: OpcionElegida[];
  cantidad: number;
  dedicatoria: string;
  comentario: string;
  precioMostrado: number;
}

export interface Cotizacion {
  valido: boolean;
  precioUnitario: number;
  precioCambio: boolean;
  cantidad: number;
  total: number;
  errores: ErrorConfiguracion[];
  opcionesNoDisponibles: number[];
}

// ===================== RF-4: plantillas =====================

export interface OpcionDePlantilla {
  idOpcion: number;
  nombre: string;
  cantidad: number;
}

// Una plantilla es un producto base con las opciones que trae
export interface Plantilla extends ProductoCatalogo {
  opciones: OpcionDePlantilla[];
}
