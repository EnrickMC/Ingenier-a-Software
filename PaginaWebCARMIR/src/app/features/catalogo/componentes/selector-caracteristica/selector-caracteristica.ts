import { Component, input, output } from '@angular/core';
import {
  CaracteristicaPersonalizable,
  Exclusion,
  OpcionElegida,
  OpcionPersonalizable,
} from '../../modelos/catalogo.models';
import { cantidadElegida, motivoExclusion, puntosUsados, redondear } from '../../utilidades/reglas-configuracion';
import { AparienciaOpcion, aparienciaDeOpcion, tipoDeCaracteristica } from '../../pastel3d/diseno-pastel';
import { rutaSvgForma, rutaSvgMolde } from '../../pastel3d/formas-pastel';

// Rectángulo de un piso en el icono de tamaño
interface PisoIcono {
  x: number;
  y: number;
  ancho: number;
  alto: number;
}

/**
 * Un paso del personalizador: una característica (Tamaño, Sabor, Decoración...) con sus opciones.
 *  - Selección única: botones de opción (radio). Si no es obligatoria se puede elegir "Sin ...".
 *  - Selección múltiple: contador por opción, limitado por los puntos del presupuesto.
 * Las opciones que chocan con algo ya elegido se deshabilitan y muestran el motivo.
 *
 * Si la característica es de tamaño, forma, sabor, relleno o cobertura, las opciones se muestran
 * como tarjetas con dibujo (pisos, silueta o color) y el pastel 3D cambia al elegirlas.
 */
@Component({
  selector: 'app-selector-caracteristica',
  templateUrl: './selector-caracteristica.html',
  styleUrl: './selector-caracteristica.css',
})
export class SelectorCaracteristica {
  readonly caracteristica = input.required<CaracteristicaPersonalizable>();
  readonly seleccion = input.required<OpcionElegida[]>();
  readonly exclusiones = input.required<Exclusion[]>();
  // Puntos totales de esta característica (ya multiplicados por el tamaño elegido)
  readonly puntosTotales = input(0);
  // Mensaje de error de esta característica (vacío = sin error)
  readonly error = input('');
  // Las gelatinas usan otros colores de sabor
  readonly esGelatina = input(false);
  // ¿Está dentro del editor de capas? (se ve más compacto, sin marco propio)
  readonly dentroDeCapa = input(false);

  // Selección única: id de la opción elegida (0 = ninguna)
  readonly opcionUnicaElegida = output<number>();
  // Selección múltiple: nueva cantidad de una opción
  readonly cantidadCambiada = output<OpcionElegida>();

  cantidad(opcion: OpcionPersonalizable): number {
    return cantidadElegida(this.seleccion(), opcion.idOpcion);
  }

  estaElegida(opcion: OpcionPersonalizable): boolean {
    return this.cantidad(opcion) > 0;
  }

  // ¿No hay ninguna opción elegida en esta característica?
  ningunaElegida(): boolean {
    for (const opcion of this.caracteristica().opciones) {
      if (this.estaElegida(opcion)) {
        return false;
      }
    }
    return true;
  }

  motivo(opcion: OpcionPersonalizable): string {
    return motivoExclusion(opcion.idOpcion, this.seleccion(), this.exclusiones());
  }

  // Una opción ya elegida nunca se bloquea (así el cliente la puede quitar)
  bloqueada(opcion: OpcionPersonalizable): boolean {
    return this.motivo(opcion) !== '' && !this.estaElegida(opcion);
  }

  puntosRestantes(): number {
    return redondear(this.puntosTotales() - puntosUsados(this.caracteristica(), this.seleccion()));
  }

  puedeSumar(opcion: OpcionPersonalizable): boolean {
    if (this.bloqueada(opcion)) {
      return false;
    }
    // CantidadElegida es TINYINT en la base de datos
    if (this.cantidad(opcion) >= 127) {
      return false;
    }
    return this.puntosRestantes() + 0.0001 >= opcion.puntosSeleccion;
  }

  textoPuntos(puntos: number): string {
    if (puntos === 1) {
      return '1 punto';
    }
    return puntos + ' puntos';
  }

  elegirUnica(idOpcion: number): void {
    this.opcionUnicaElegida.emit(idOpcion);
  }

  sumar(opcion: OpcionPersonalizable): void {
    if (this.puedeSumar(opcion)) {
      this.cantidadCambiada.emit({ idOpcion: opcion.idOpcion, cantidad: this.cantidad(opcion) + 1 });
    }
  }

  restar(opcion: OpcionPersonalizable): void {
    if (this.cantidad(opcion) > 0) {
      this.cantidadCambiada.emit({ idOpcion: opcion.idOpcion, cantidad: this.cantidad(opcion) - 1 });
    }
  }

  // ===================== Tarjetas con dibujo =====================

  // ¿Esta característica se ve en el pastel 3D?
  seVeEn3d(): boolean {
    return tipoDeCaracteristica(this.caracteristica().nombre) !== 'otra';
  }

  apariencia(opcion: OpcionPersonalizable): AparienciaOpcion {
    return aparienciaDeOpcion(this.caracteristica(), opcion, this.esGelatina());
  }

  // Las opciones se muestran como tarjetas si al menos una tiene dibujo
  conTarjetas(): boolean {
    if (this.caracteristica().seleccionMultiple) {
      return false;
    }
    for (const opcion of this.caracteristica().opciones) {
      if (this.apariencia(opcion).tipo !== 'ninguna') {
        return true;
      }
    }
    return false;
  }

  rutaForma(opcion: OpcionPersonalizable): string {
    const apariencia = this.apariencia(opcion);
    // Gelatinas: rosca, domo y gajos tienen su propio dibujo
    if (this.esGelatina() && apariencia.molde !== 'figura') {
      return rutaSvgMolde(apariencia.molde, 48);
    }
    return rutaSvgForma(apariencia.forma, 48);
  }

  // Pisos del icono de tamaño (caja de 64 x 56): más pisos y más anchos según el tamaño
  // Pisos del icono de «Pisos»: uno encima de otro
  pisosIcono(opcion: OpcionPersonalizable): PisoIcono[] {
    const nivel = this.apariencia(opcion).nivel;
    const pisos: PisoIcono[] = [];
    const altoPiso = Math.min(12, 46 / nivel);
    let ancho = 52;
    let y = 51;
    for (let i = 0; i < nivel; i++) {
      y = y - altoPiso;
      pisos.push({ x: 32 - ancho / 2, y: y, ancho: ancho, alto: altoPiso });
      ancho = ancho * 0.76;
    }
    return pisos;
  }

  // Tamaño: círculo visto desde arriba, más grande entre más grande es el postre
  radioTamano(opcion: OpcionPersonalizable): number {
    return 9 + this.apariencia(opcion).nivel * 4.5;
  }

  // Id único para los campos del formulario
  idCampo(opcion: OpcionPersonalizable): string {
    return 'opcion-' + opcion.idOpcion;
  }
}
