import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ArticuloCarrito } from '../../../../core/modelos/articulo-carrito';
import { AvisosService } from '../../../../core/servicios/avisos.service';
import { CarritoService, PIEZAS_MAXIMAS } from '../../../../core/servicios/carrito.service';
import { mensajeDeError } from '../../../../core/utilidades/errores';
import { DialogoConfirmacion } from '../../../../shared/componentes/dialogo-confirmacion/dialogo-confirmacion';
import { ImagenPostre } from '../../../../shared/componentes/imagen-postre/imagen-postre';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { PasosPedido } from '../../../pedidos/componentes/pasos-pedido/pasos-pedido';
import { EstadoArticulo, RevisionCarrito } from '../../../pedidos/modelos/pedidos.models';
import { PedidosApiService } from '../../../pedidos/servicios/pedidos-api.service';

const MENSAJE_SIN_CONEXION = 'No fue posible revisar tu carrito. Revisa tu conexión e inténtalo de nuevo.';

// Resultado de la revisión de un renglón del carrito
interface EstadoRenglon {
  estado: EstadoArticulo;
  mensaje: string;
}

/**
 * Carrito de compras · Módulo 2 (RF-6 a RF-9).
 *  - RF-6: al abrir, la API revisa que cada postre siga disponible y con el mismo precio.
 *  - RF-8: cambiar la cantidad o volver a personalizar un postre.
 *  - RF-9: quitar un postre o vaciar el carrito (con confirmación, RNF-5).
 *  - «Continuar» revisa otra vez y pasa a los datos de entrega.
 */
@Component({
  selector: 'app-carrito-pagina',
  imports: [RouterLink, CurrencyPipe, ImagenPostre, MensajeEstado, DialogoConfirmacion, PasosPedido],
  templateUrl: './carrito-pagina.html',
  styleUrl: './carrito-pagina.css',
})
export default class CarritoPagina implements OnInit {
  protected readonly carrito = inject(CarritoService);
  private readonly api = inject(PedidosApiService);
  private readonly avisos = inject(AvisosService);
  private readonly router = inject(Router);

  protected readonly piezasMaximas = PIEZAS_MAXIMAS;

  // Revisión de la API por id de renglón
  protected readonly estados = signal<Record<string, EstadoRenglon>>({});
  protected readonly revisando = signal(false);
  protected readonly revisado = signal(false);
  protected readonly puedeContinuar = signal(false);
  protected readonly errorRevision = signal('');
  protected readonly avisoPrecios = signal('');
  protected readonly avisoGeneral = signal('');
  protected readonly mensajeExito = signal('');

  // Confirmaciones (RNF-5)
  protected readonly porQuitar = signal<ArticuloCarrito | null>(null);
  protected readonly confirmandoVaciar = signal(false);

  // RF-6 (Visualización de producto en el carrito): inicia aquí. Paso 1: el cliente abrió «Carrito».
  ngOnInit(): void {
    // Aviso que dejó otra pantalla (por ejemplo: «Actualizamos tu postre» o productos no disponibles)
    const aviso = this.avisos.tomarAviso();
    if (aviso !== '') {
      this.avisoGeneral.set(aviso);
    }
    this.revisar();
  }

  // ===================== RF-6: revisión con la API =====================

  // RF-6 · Pasos 2 a 5: revisa el carrito con la API (productos activos, precios y opciones vigentes).
  async revisar(): Promise<boolean> {
    const articulos = this.carrito.articulos();
    // RF-6 · Excepciones pasos 2 y 3: no hay carrito o no tiene productos, se muestra «Tu carrito está vacío».
    if (articulos.length === 0) {
      this.revisado.set(true);
      this.puedeContinuar.set(false);
      return false;
    }
    this.revisando.set(true);
    this.errorRevision.set('');
    try {
      // RF-6 · Paso 4: la API valida en la BD cada producto, su precio y sus opciones.
      const revision = await this.api.validarCarrito(articulos);
      this.usarRevision(articulos, revision);
      this.revisado.set(true);
      return revision.puedeContinuar;
    } catch (error) {
      // RF-6 · Excepción paso 2: sin conexión, se muestra el error con «Reintentar» y no se puede continuar.
      this.errorRevision.set(mensajeDeError(error, MENSAJE_SIN_CONEXION));
      this.puedeContinuar.set(false);
      return false;
    } finally {
      this.revisando.set(false);
    }
  }

  private usarRevision(articulos: ArticuloCarrito[], revision: RevisionCarrito): void {
    const estados: Record<string, EstadoRenglon> = {};
    const cambiaron: string[] = [];
    for (const revisado of revision.articulos) {
      const articulo = articulos[revisado.posicion];
      if (articulo === undefined) {
        continue;
      }
      // RF-6 · Excepción paso 4: producto «No disponible» o «Requiere actualizar personalización»; deshabilita «Continuar».
      estados[articulo.id] = { estado: revisado.estado, mensaje: revisado.mensaje };
      // RF-6 · Excepción paso 4: el precio cambió, se actualiza y se avisa «El precio de [producto] se actualizó».
      // Paso 4: el precio cambió en la base de datos, se actualiza y se avisa
      if (revisado.estado === 'disponible' && revisado.precioUnitario !== articulo.precioUnitario) {
        this.carrito.actualizarPrecio(revisado.posicion, revisado.precioUnitario);
        cambiaron.push(articulo.nombre);
      }
    }
    this.estados.set(estados);
    // RF-6 · Paso 6: muestra el resumen con las opciones de modificar, eliminar y continuar.
    this.puedeContinuar.set(revision.puedeContinuar);
    if (cambiaron.length > 0) {
      this.avisoPrecios.set('Se actualizó el precio de: ' + cambiaron.join(', ') + '. Revisa el nuevo total.');
    }
  }

  estadoDe(articulo: ArticuloCarrito): EstadoRenglon | null {
    const estado = this.estados()[articulo.id];
    if (estado === undefined) {
      return null;
    }
    return estado;
  }

  tieneProblema(articulo: ArticuloCarrito): boolean {
    const estado = this.estadoDe(articulo);
    return estado !== null && estado.estado !== 'disponible';
  }

  // ===================== RF-8: cantidad =====================

  // RF-8 (Modificación de productos del carrito): inicia aquí para la cantidad; la personalización se edita en personalizar-pagina (?editar=).
  sumarPieza(articulo: ArticuloCarrito): void {
    if (articulo.cantidad < PIEZAS_MAXIMAS) {
      this.cambiarCantidad(articulo, articulo.cantidad + 1);
    }
  }

  restarPieza(articulo: ArticuloCarrito): void {
    if (articulo.cantidad > 1) {
      this.cambiarCantidad(articulo, articulo.cantidad - 1);
    }
  }

  // Cantidad escrita a mano: si no es válida se regresa al rango de 1 a 20
  // RF-8 · Excepción paso 5: cantidad fuera de rango, se regresa al valor permitido.
  escribirCantidad(articulo: ArticuloCarrito, texto: string): void {
    let cantidad = Math.floor(Number(texto));
    if (!Number.isFinite(cantidad) || cantidad < 1) {
      cantidad = 1;
    }
    if (cantidad > PIEZAS_MAXIMAS) {
      cantidad = PIEZAS_MAXIMAS;
    }
    this.cambiarCantidad(articulo, cantidad);
  }

  private cambiarCantidad(articulo: ArticuloCarrito, cantidad: number): void {
    this.mensajeExito.set('');
    // Si en otra pestaña ya se quitó, se avisa y se actualiza la vista
    // RF-8 · Excepción paso 2: «El producto ya no está en tu carrito».
    if (!this.carrito.cambiarCantidad(articulo.id, cantidad)) {
      this.avisoGeneral.set('Este producto ya no está en tu carrito');
    }
  }

  // ===================== RF-9: quitar y vaciar =====================

  // RF-9 (Eliminación de productos del carrito): inicia aquí. Pasos 1 a 3: pide quitar un producto y se muestra la confirmación.
  pedirQuitar(articulo: ArticuloCarrito): void {
    this.porQuitar.set(articulo);
  }

  // RF-9 · Excepción paso 4: el cliente canceló; el carrito queda igual.
  cancelarQuitar(): void {
    this.porQuitar.set(null);
  }

  // RF-9 · Paso 4: el cliente confirmó la eliminación.
  confirmarQuitar(): void {
    const articulo = this.porQuitar();
    this.porQuitar.set(null);
    if (articulo === null) {
      return;
    }
    // RF-9 · Paso 5: elimina el producto. Paso 7: informa el éxito.
    if (this.carrito.quitar(articulo.id)) {
      this.mensajeExito.set('Quitamos «' + articulo.nombre + '» de tu carrito.');
    } else {
      // RF-9 · Excepción paso 2: «El producto ya no está en tu carrito»; no se elimina nada.
      this.avisoGeneral.set('Este producto ya no está en tu carrito');
    }
    this.avisoPrecios.set('');
    // Al quitar un postre puede cambiar si los demás se pueden pedir (repetidos)
    // RF-9 · Paso 6: recalcula el total y el contador.
    this.revisar();
  }

  // RF-9 · Paso 1 y 3: «Vaciar carrito» muestra la confirmación con el número de productos.
  pedirVaciar(): void {
    this.confirmandoVaciar.set(true);
  }

  // RF-9 · Excepción paso 4: el cliente canceló el vaciado.
  cancelarVaciar(): void {
    this.confirmandoVaciar.set(false);
  }

  // RF-9 · Pasos 5 a 7: vacía el carrito e informa el éxito.
  confirmarVaciar(): void {
    this.confirmandoVaciar.set(false);
    this.carrito.vaciar();
    this.estados.set({});
    this.avisoPrecios.set('');
    this.avisoGeneral.set('');
    this.mensajeExito.set('Vaciamos tu carrito.');
  }

  // ===================== Continuar =====================

  // RF-6 · Paso 6: «Continuar» revisa otra vez el carrito y pasa a RF-10 (datos de entrega).
  async continuar(): Promise<void> {
    if (this.revisando()) {
      return;
    }
    const totalAntes = this.carrito.totalImporte();
    this.avisoPrecios.set('');
    const listo = await this.revisar();
    // RF-6 · Excepción paso 4: hay productos con problema, no se continúa.
    if (!listo) {
      return;
    }
    // Si un precio cambió justo ahora, el cliente revisa el total antes de seguir
    // RF-6 · Excepción paso 4: un precio cambió justo ahora; el cliente revisa el total antes de seguir.
    if (this.carrito.totalImporte() !== totalAntes) {
      return;
    }
    // RF-10: inicia en entrega-pagina.
    this.router.navigate(['/pedido/entrega']);
  }

  // ===================== Datos para la vista =====================

  subtotal(articulo: ArticuloCarrito): number {
    return Math.round(articulo.precioUnitario * articulo.cantidad * 100) / 100;
  }

  // "Grande, Tres leches, Flores de azúcar ×2"
  resumen(articulo: ArticuloCarrito): string {
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

  // Dirección para volver a personalizar el postre
  enlaceEditar(articulo: ArticuloCarrito): string[] {
    return ['/producto', String(articulo.idPostre), 'personalizar'];
  }
}
