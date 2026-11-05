import { AfterViewInit, Component, ElementRef, input, output, viewChild } from '@angular/core';

/**
 * Ventana para confirmar una acción importante (RNF-5): quitar un postre, vaciar el carrito,
 * enviar el pedido. Se muestra con @if desde la página:
 *   @if (confirmandoVaciar()) {
 *     <app-dialogo-confirmacion titulo="¿Vaciar tu carrito?" (confirmar)="vaciar()" (cancelar)="..." />
 *   }
 * Al abrirse, el foco queda en «Cancelar» para que Enter no confirme por accidente.
 */
@Component({
  selector: 'app-dialogo-confirmacion',
  templateUrl: './dialogo-confirmacion.html',
  styleUrl: './dialogo-confirmacion.css',
})
export class DialogoConfirmacion implements AfterViewInit {
  readonly titulo = input.required<string>();
  readonly texto = input('');
  readonly textoConfirmar = input('Sí, continuar');
  readonly textoCancelar = input('Cancelar');
  // true mientras la acción se está haciendo (desactiva los botones)
  readonly ocupado = input(false);

  readonly confirmar = output<void>();
  readonly cancelar = output<void>();

  private readonly botonCancelar = viewChild<ElementRef<HTMLButtonElement>>('botonCancelar');

  ngAfterViewInit(): void {
    const boton = this.botonCancelar();
    if (boton !== undefined) {
      boton.nativeElement.focus();
    }
  }

  alConfirmar(): void {
    if (!this.ocupado()) {
      this.confirmar.emit();
    }
  }

  alCancelar(): void {
    if (!this.ocupado()) {
      this.cancelar.emit();
    }
  }

  // Clic en el fondo oscuro (fuera de la ventana) = cancelar
  alTocarFondo(evento: MouseEvent): void {
    if (evento.target === evento.currentTarget) {
      this.alCancelar();
    }
  }
}
