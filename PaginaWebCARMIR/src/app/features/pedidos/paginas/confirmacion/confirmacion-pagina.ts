import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { UbicacionTienda } from '../../../../shared/componentes/ubicacion-tienda/ubicacion-tienda';
import { PasosPedido } from '../../componentes/pasos-pedido/pasos-pedido';
import { ComprobantePedido, DireccionEntrega } from '../../modelos/pedidos.models';
import { PedidoEnCursoService } from '../../servicios/pedido-en-curso.service';
import { fechaLarga } from '../../utilidades/fechas';

/**
 * RF-11 paso 8 · Comprobante del pedido: folio, estado, productos, total, entrega
 * e instrucciones de pago. Se guarda en el navegador para que no se pierda al recargar.
 */
@Component({
  selector: 'app-confirmacion-pagina',
  imports: [RouterLink, CurrencyPipe, MensajeEstado, PasosPedido, UbicacionTienda],
  templateUrl: './confirmacion-pagina.html',
  styleUrls: ['../../pedidos-comun.css', './confirmacion-pagina.css'],
})
export default class ConfirmacionPagina {
  private readonly pedido = inject(PedidoEnCursoService);

  // RF-11 · Paso 8: comprobante guardado al registrar el pedido.
  comprobante(): ComprobantePedido | null {
    return this.pedido.datos().ultimoComprobante;
  }

  textoFecha(comprobante: ComprobantePedido): string {
    return fechaLarga(comprobante.entrega.fecha) + ' a las ' + comprobante.entrega.horario + ' h';
  }

  textoDireccion(direccion: DireccionEntrega): string {
    let texto = direccion.calle + ' ' + direccion.numeroExterior;
    if (direccion.numeroInterior !== '') {
      texto = texto + ' int. ' + direccion.numeroInterior;
    }
    return texto + ', ' + direccion.colonia + ', C.P. ' + direccion.codigoPostal;
  }

  textoMetodo(comprobante: ComprobantePedido): string {
    if (comprobante.metodoPago === 'Transferencia') {
      return 'Transferencia bancaria';
    }
    // Pedidos guardados antes de quitar la opción «Depósito»
    if ((comprobante.metodoPago as string) === 'Deposito') {
      return 'Depósito bancario';
    }
    return 'Efectivo en la tienda';
  }

  esEfectivo(comprobante: ComprobantePedido): boolean {
    return comprobante.metodoPago === 'Efectivo';
  }

  imprimir(): void {
    window.print();
  }
}
