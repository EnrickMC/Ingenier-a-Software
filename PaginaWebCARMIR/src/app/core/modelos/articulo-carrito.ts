/**
 * Un postre configurado que el cliente agregó al carrito (Módulo 2 · Portal de Pedidos).
 * El carrito se guarda en el navegador; la API vuelve a revisar todo antes de registrar el pedido.
 */
export interface OpcionDelArticulo {
  idOpcion: number;
  nombre: string;
  cantidad: number;
}

export interface ArticuloCarrito {
  // Identificador del renglón dentro del carrito (para editar o quitar ese renglón)
  id: string;
  // Producto base (plantilla) desde el que se personalizó
  idPostre: number;
  nombre: string;
  linkFotografia: string | null;
  opciones: OpcionDelArticulo[];
  cantidad: number;
  dedicatoria: string;
  // ¿El mensaje va en un topper (letrero de pie)? Los carritos guardados antes no lo tienen (= no)
  mensajeEnTopper?: boolean;
  comentario: string;
  // Precio de una pieza confirmado por la API (se actualiza si cambia en la base de datos)
  precioUnitario: number;
}

// Lo que se manda para agregar o reemplazar un artículo (el carrito le pone el id)
export type ArticuloNuevo = Omit<ArticuloCarrito, 'id'>;

// Resultado de agregar: si se juntó con uno igual y si se tuvo que ajustar la cantidad al máximo
export interface ResultadoAgregar {
  juntado: boolean;
  cantidadAjustada: boolean;
}
