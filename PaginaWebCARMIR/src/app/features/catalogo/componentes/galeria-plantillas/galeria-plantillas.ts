import { CurrencyPipe } from '@angular/common';
import { AfterViewInit, Component, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { ImagenPostre } from '../../../../shared/componentes/imagen-postre/imagen-postre';
import { Plantilla } from '../../modelos/catalogo.models';

/**
 * RF-4 · Galería de plantillas de diseño.
 * Las plantillas son los productos base de la misma categoría. El cliente elige una
 * y la confirma con «Usar esta plantilla»; la página carga sus opciones como punto de partida.
 */
@Component({
  selector: 'app-galeria-plantillas',
  imports: [CurrencyPipe, ImagenPostre],
  templateUrl: './galeria-plantillas.html',
  styleUrl: './galeria-plantillas.css',
})
export class GaleriaPlantillas implements AfterViewInit {
  readonly plantillas = input.required<Plantilla[]>();
  // Mensaje de error dentro de la galería (por ejemplo «La plantilla seleccionada ya no está disponible»)
  readonly mensaje = input('');
  readonly aplicando = input(false);

  readonly confirmar = output<Plantilla>();
  readonly cerrar = output<void>();

  protected readonly elegida = signal<Plantilla | null>(null);
  private readonly ventana = viewChild.required<ElementRef<HTMLElement>>('ventana');

  // Al abrir, el foco pasa a la ventana (para teclado y lectores de pantalla)
  ngAfterViewInit(): void {
    this.ventana().nativeElement.focus();
  }

  // RF-4 · Paso 5: el cliente selecciona una plantilla.
  elegir(plantilla: Plantilla): void {
    this.elegida.set(plantilla);
  }

  estaElegida(plantilla: Plantilla): boolean {
    const elegida = this.elegida();
    return elegida !== null && elegida.idPostre === plantilla.idPostre;
  }

  // RF-4 · Paso 5: el cliente confirma la plantilla («Usar esta plantilla»); sigue el paso 6 en la página.
  usarPlantilla(): void {
    const elegida = this.elegida();
    if (elegida !== null) {
      this.confirmar.emit(elegida);
    }
  }

  // RF-4 paso 5: cerrar sin confirmar descarta la elección
  cerrarGaleria(): void {
    this.elegida.set(null);
    this.cerrar.emit();
  }

  // Resumen de lo que trae la plantilla: "Grande, Tres leches, Flores de azúcar ×2"
  resumen(plantilla: Plantilla): string {
    const partes: string[] = [];
    for (const opcion of plantilla.opciones) {
      if (opcion.cantidad > 1) {
        partes.push(opcion.nombre + ' ×' + opcion.cantidad);
      } else {
        partes.push(opcion.nombre);
      }
    }
    return partes.join(', ');
  }

  // Cerrar con la tecla Escape
  alPresionarTecla(evento: KeyboardEvent): void {
    if (evento.key === 'Escape') {
      this.cerrarGaleria();
    }
  }
}
