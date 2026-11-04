import { Injectable, signal } from '@angular/core';
import { ArticuloCarrito, ArticuloNuevo, ResultadoAgregar } from '../modelos/articulo-carrito';

// Nombre con el que se guarda el carrito en el navegador
const LLAVE_ALMACEN = 'carmir.carrito';
// Máximo de piezas de un mismo postre (igual que la API)
export const PIEZAS_MAXIMAS = 20;
// Máximo de renglones en el carrito (igual que la API)
export const RENGLONES_MAXIMOS = 20;

/**
 * Carrito de compras (RF-6 a RF-9 y RNF-7).
 * Guarda los postres en el navegador (localStorage) para que no se pierdan al recargar,
 * cerrar la pestaña o perder el internet, hasta que el pedido se envía.
 * El encabezado lee "articulos" para mostrar el contador.
 */
@Injectable({ providedIn: 'root' })
export class CarritoService {
  // RF-6 / RF-7 · Paso 3: recupera el carrito guardado en el navegador o empieza uno vacío (RNF-7).
  readonly articulos = signal<ArticuloCarrito[]>(this.leerGuardado());
  // false si el navegador no deja guardar (modo privado o almacenamiento bloqueado)
  readonly guardadoDisponible = signal(true);

  // ===================== Consultas =====================

  buscar(id: string): ArticuloCarrito | null {
    for (const articulo of this.articulos()) {
      if (articulo.id === id) {
        return articulo;
      }
    }
    return null;
  }

  // Total de piezas (para el contador del encabezado)
  totalPiezas(): number {
    let total = 0;
    for (const articulo of this.articulos()) {
      total = total + articulo.cantidad;
    }
    return total;
  }

  totalImporte(): number {
    let total = 0;
    for (const articulo of this.articulos()) {
      total = total + articulo.precioUnitario * articulo.cantidad;
    }
    return Math.round(total * 100) / 100;
  }

  // ===================== Cambios (RF-7, RF-8, RF-9) =====================

  /**
   * RF-7: agrega el postre. Si ya hay uno con la misma configuración, mensaje e indicaciones,
   * solo suma las piezas (paso 4). Si pasa del máximo, la cantidad se ajusta al máximo (paso 5).
   */
  // RF-7 (Registro de producto en el carrito): inicia aquí, después de que RF-3 validó el postre con la API (paso 2).
  agregar(nuevo: ArticuloNuevo): ResultadoAgregar {
    const lista = this.articulos().slice();
    // RF-7 · Paso 4: ¿el carrito ya tiene el mismo postre con la misma configuración?
    for (let i = 0; i < lista.length; i++) {
      if (sonIguales(lista[i], nuevo)) {
        // RF-7 · Paso 5: ya estaba, se incrementa su cantidad.
        const suma = lista[i].cantidad + nuevo.cantidad;
        // RF-7 · Excepción paso 5: si excede el máximo por producto, se ajusta al máximo y se informa.
        const cantidad = Math.min(suma, PIEZAS_MAXIMAS);
        lista[i] = { ...lista[i], cantidad: cantidad, precioUnitario: nuevo.precioUnitario };
        this.guardar(lista);
        return { juntado: true, cantidadAjustada: suma > PIEZAS_MAXIMAS };
      }
    }
    // RF-7 · Paso 5: se agrega como renglón nuevo con su precio.
    const cantidad = Math.min(nuevo.cantidad, PIEZAS_MAXIMAS);
    lista.push({ ...nuevo, cantidad: cantidad, id: crearId() });
    // RF-7 · Paso 6: al guardar se recalcula el total y el contador «Carrito (N)» (paso 7).
    this.guardar(lista);
    return { juntado: false, cantidadAjustada: nuevo.cantidad > PIEZAS_MAXIMAS };
  }

  /**
   * RF-8: cambia la personalización de un renglón (conserva su id). false si ya no está.
   * Si después del cambio queda igual a otro renglón, se juntan en uno solo.
   */
  // RF-8 · Paso 7: actualiza el postre editado en el carrito.
  reemplazar(id: string, nuevo: ArticuloNuevo): boolean {
    // RF-8 · Excepción paso 2: el producto ya no está en el carrito.
    if (this.buscar(id) === null) {
      return false;
    }
    const lista: ArticuloCarrito[] = [];
    let juntado = false;
    for (const articulo of this.articulos()) {
      if (articulo.id !== id && sonIguales(articulo, nuevo)) {
        const cantidad = Math.min(articulo.cantidad + nuevo.cantidad, PIEZAS_MAXIMAS);
        lista.push({ ...articulo, cantidad: cantidad, precioUnitario: nuevo.precioUnitario });
        juntado = true;
      } else {
        lista.push(articulo);
      }
    }
    for (let i = 0; i < lista.length; i++) {
      if (lista[i].id === id) {
        if (juntado) {
          lista.splice(i, 1);
        } else {
          lista[i] = { ...nuevo, cantidad: Math.min(nuevo.cantidad, PIEZAS_MAXIMAS), id: id };
        }
        break;
      }
    }
    this.guardar(lista);
    return true;
  }

  // RF-8: nueva cantidad (de 1 al máximo). false si el producto ya no está en el carrito.
  // RF-8 · Paso 7: actualiza la cantidad del renglón.
  cambiarCantidad(id: string, cantidad: number): boolean {
    const lista = this.articulos().slice();
    for (let i = 0; i < lista.length; i++) {
      if (lista[i].id === id) {
        lista[i] = { ...lista[i], cantidad: cantidad };
        this.guardar(lista);
        return true;
      }
    }
    // RF-8 · Excepción paso 2: el producto ya no está en el carrito.
    return false;
  }

  // RF-9: quita un renglón. false si ya no estaba.
  // RF-9 · Paso 5: elimina el producto seleccionado.
  quitar(id: string): boolean {
    const lista: ArticuloCarrito[] = [];
    let encontrado = false;
    for (const articulo of this.articulos()) {
      if (articulo.id === id) {
        encontrado = true;
      } else {
        lista.push(articulo);
      }
    }
    if (encontrado) {
      this.guardar(lista);
    }
    // RF-9 · Excepción paso 2: si ya no estaba, regresa false y la página lo avisa.
    return encontrado;
  }

  // RF-9: vaciar el carrito (también al terminar el pedido, RF-12 paso 7)
  // RF-9 · Paso 5: elimina todos los productos («Vaciar carrito»).
  vaciar(): void {
    this.guardar([]);
  }

  // RF-6 paso 4: la API dio un precio nuevo para el renglón de esa posición
  actualizarPrecio(posicion: number, precioUnitario: number): void {
    const lista = this.articulos().slice();
    if (posicion >= 0 && posicion < lista.length && lista[posicion].precioUnitario !== precioUnitario) {
      lista[posicion] = { ...lista[posicion], precioUnitario: precioUnitario };
      this.guardar(lista);
    }
  }

  // ===================== Guardado en el navegador =====================

  // RF-7 · Paso 6 / RF-8 · Paso 7 / RF-9 · Paso 6: guarda el carrito y recalcula total y contador.
  private guardar(lista: ArticuloCarrito[]): void {
    this.articulos.set(lista);
    try {
      localStorage.setItem(LLAVE_ALMACEN, JSON.stringify(lista));
      this.guardadoDisponible.set(true);
    } catch {
      // RF-7 · Excepción paso 3: el navegador no deja guardar; la página avisa revisar la configuración del navegador.
      // Si el navegador no deja guardar (modo privado), el carrito solo vive mientras la página esté abierta
      this.guardadoDisponible.set(false);
    }
  }

  private leerGuardado(): ArticuloCarrito[] {
    try {
      const texto = localStorage.getItem(LLAVE_ALMACEN);
      if (texto === null) {
        return [];
      }
      const datos = JSON.parse(texto);
      if (!Array.isArray(datos)) {
        return [];
      }
      const lista: ArticuloCarrito[] = [];
      for (const dato of datos) {
        if (esArticuloValido(dato)) {
          // Los carritos guardados antes del Módulo 2 no tenían id
          if (typeof dato.id !== 'string' || dato.id === '') {
            dato.id = crearId();
          }
          lista.push(dato);
        }
      }
      return lista;
    } catch {
      // RF-6 · Excepción paso 2: sin carrito guardado o dañado, se empieza vacío («Tu carrito está vacío»).
      // Datos dañados o almacenamiento bloqueado: se empieza con el carrito vacío
    }
    return [];
  }
}

// ===================== Apoyo =====================

// Identificador corto para un renglón del carrito
function crearId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substring(2, 8);
}

// Revisa que lo guardado tenga la forma de un artículo (por si alguien lo editó a mano)
function esArticuloValido(dato: unknown): dato is ArticuloCarrito {
  if (dato === null || typeof dato !== 'object') {
    return false;
  }
  const articulo = dato as ArticuloCarrito;
  return (
    typeof articulo.idPostre === 'number' &&
    typeof articulo.nombre === 'string' &&
    Array.isArray(articulo.opciones) &&
    typeof articulo.cantidad === 'number' &&
    articulo.cantidad >= 1 &&
    typeof articulo.precioUnitario === 'number' &&
    typeof articulo.dedicatoria === 'string' &&
    typeof articulo.comentario === 'string'
  );
}

/**
 * "IdOpcion:Cantidad|..." ordenado por id: la misma ClaveConfiguracion que usan la API y el Panel.
 * Dos postres con la misma clave son el mismo postre en la base de datos.
 */
export function claveDeOpciones(opciones: { idOpcion: number; cantidad: number }[]): string {
  const ordenadas = opciones.slice();
  ordenadas.sort(compararOpciones);
  const partes: string[] = [];
  for (const opcion of ordenadas) {
    partes.push(opcion.idOpcion + ':' + opcion.cantidad);
  }
  return partes.join('|');
}

function compararOpciones(a: { idOpcion: number }, b: { idOpcion: number }): number {
  return a.idOpcion - b.idOpcion;
}

// Mismo postre con el mismo mensaje e indicaciones
function sonIguales(articulo: ArticuloCarrito, nuevo: ArticuloNuevo): boolean {
  return (
    claveDeOpciones(articulo.opciones) === claveDeOpciones(nuevo.opciones) &&
    articulo.dedicatoria === nuevo.dedicatoria &&
    articulo.mensajeEnTopper === nuevo.mensajeEnTopper &&
    articulo.comentario === nuevo.comentario
  );
}
