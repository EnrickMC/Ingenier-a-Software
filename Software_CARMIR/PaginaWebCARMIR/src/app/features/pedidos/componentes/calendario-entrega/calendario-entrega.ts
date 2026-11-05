import { Component, OnInit, input, output, signal } from '@angular/core';
import { ReglasEntrega } from '../../modelos/pedidos.models';
import { DiaCalendario, NOMBRES_MESES, diasDelMes, fechaLarga, textoAFecha } from '../../utilidades/fechas';

/**
 * Calendario para elegir la fecha de entrega (RF-10 pasos 4 a 6).
 * Solo deja elegir días laborables, desde la anticipación mínima y que todavía tengan cupo
 * (la misma capacidad diaria que usa la agenda del Panel). La API lo vuelve a revisar al enviar.
 */
@Component({
  selector: 'app-calendario-entrega',
  templateUrl: './calendario-entrega.html',
  styleUrl: './calendario-entrega.css',
})
export class CalendarioEntrega implements OnInit {
  readonly reglas = input.required<ReglasEntrega>();
  readonly seleccionada = input('');
  readonly conError = input(false);
  readonly elegir = output<string>();

  protected readonly encabezados = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
  protected readonly anio = signal(0);
  protected readonly mes = signal(0);

  ngOnInit(): void {
    // Se abre en el mes de la fecha elegida o en el de la primera fecha disponible
    let inicio = textoAFecha(this.seleccionada());
    if (inicio === null) {
      inicio = textoAFecha(this.reglas().fechaMinima);
    }
    if (inicio === null) {
      inicio = new Date();
    }
    this.anio.set(inicio.getFullYear());
    this.mes.set(inicio.getMonth() + 1);
  }

  // RF-10 · Paso 6: cada día del mes con su estado (disponible, sin cupo, no laborable o antes de la anticipación mínima).
  dias(): DiaCalendario[] {
    return diasDelMes(this.reglas(), this.anio(), this.mes());
  }

  tituloMes(): string {
    const nombre = NOMBRES_MESES[this.mes() - 1];
    return nombre.charAt(0).toUpperCase() + nombre.substring(1) + ' ' + this.anio();
  }

  // "AAAA-MM" del mes visible, para comparar con la fecha mínima y máxima
  private mesVisible(): string {
    let mes = String(this.mes());
    if (this.mes() < 10) {
      mes = '0' + mes;
    }
    return this.anio() + '-' + mes;
  }

  puedeRetroceder(): boolean {
    return this.mesVisible() > this.reglas().fechaMinima.substring(0, 7);
  }

  puedeAvanzar(): boolean {
    return this.mesVisible() < this.reglas().fechaMaxima.substring(0, 7);
  }

  mesAnterior(): void {
    if (!this.puedeRetroceder()) {
      return;
    }
    if (this.mes() === 1) {
      this.mes.set(12);
      this.anio.set(this.anio() - 1);
    } else {
      this.mes.set(this.mes() - 1);
    }
  }

  mesSiguiente(): void {
    if (!this.puedeAvanzar()) {
      return;
    }
    if (this.mes() === 12) {
      this.mes.set(1);
      this.anio.set(this.anio() + 1);
    } else {
      this.mes.set(this.mes() + 1);
    }
  }

  elegirDia(dia: DiaCalendario): void {
    if (dia.estado === 'disponible') {
      this.elegir.emit(dia.fecha);
    }
  }

  // Lo que escucha un lector de pantalla en cada día
  etiqueta(dia: DiaCalendario): string {
    let texto = fechaLarga(dia.fecha);
    if (dia.estado === 'lleno') {
      texto = texto + ', sin cupo';
    } else if (dia.estado === 'no-laborable') {
      texto = texto + ', no hay entregas';
    } else if (dia.estado === 'fuera-de-rango') {
      texto = texto + ', no disponible';
    }
    return texto;
  }
}
