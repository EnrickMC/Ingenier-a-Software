/**
 * Fechas del Portal de Pedidos. Se manejan como texto "AAAA-MM-DD" (igual que la API),
 * así no se recorren un día por la zona horaria del navegador.
 */
import { ReglasEntrega } from '../modelos/pedidos.models';

const LETRAS_DIAS = ['D', 'L', 'M', 'X', 'J', 'V', 'S'];
const NOMBRES_DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
export const NOMBRES_MESES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];

// Estado de un día en el calendario de entrega
export type EstadoDia = 'disponible' | 'lleno' | 'no-laborable' | 'fuera-de-rango';

export interface DiaCalendario {
  // "" en los huecos antes del día 1
  fecha: string;
  numero: number;
  estado: EstadoDia;
}

function dosDigitos(numero: number): string {
  if (numero < 10) {
    return '0' + numero;
  }
  return String(numero);
}

export function armarFecha(anio: number, mes: number, dia: number): string {
  return anio + '-' + dosDigitos(mes) + '-' + dosDigitos(dia);
}

// "AAAA-MM-DD" → Date a mediodía; null si la fecha no existe
export function textoAFecha(texto: string): Date | null {
  const partes = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/.exec(texto);
  if (partes === null) {
    return null;
  }
  const anio = Number(partes[1]);
  const mes = Number(partes[2]);
  const dia = Number(partes[3]);
  const fecha = new Date(anio, mes - 1, dia, 12, 0, 0);
  if (fecha.getFullYear() !== anio || fecha.getMonth() !== mes - 1 || fecha.getDate() !== dia) {
    return null;
  }
  return fecha;
}

// "2026-11-10" → "10/11/2026"
export function fechaCorta(texto: string | null): string {
  if (texto === null || texto === '') {
    return '';
  }
  const partes = texto.substring(0, 10).split('-');
  if (partes.length !== 3) {
    return texto;
  }
  return partes[2] + '/' + partes[1] + '/' + partes[0];
}

// "2026-11-10" → "martes 10 de noviembre de 2026"
export function fechaLarga(texto: string): string {
  const fecha = textoAFecha(texto.substring(0, 10));
  if (fecha === null) {
    return texto;
  }
  return (
    NOMBRES_DIAS[fecha.getDay()] +
    ' ' +
    fecha.getDate() +
    ' de ' +
    NOMBRES_MESES[fecha.getMonth()] +
    ' de ' +
    fecha.getFullYear()
  );
}

// "2026-11-03 14:05:00" → "03/11/2026 14:05"
export function fechaHora(texto: string | null): string {
  if (texto === null || texto === '') {
    return '';
  }
  const fecha = fechaCorta(texto.substring(0, 10));
  if (texto.length >= 16) {
    return fecha + ' ' + texto.substring(11, 16);
  }
  return fecha;
}

// Qué tan disponible está un día según las reglas de la repostería
export function estadoDelDia(reglas: ReglasEntrega, fecha: string): EstadoDia {
  const dia = textoAFecha(fecha);
  if (dia === null || fecha < reglas.fechaMinima || fecha > reglas.fechaMaxima) {
    return 'fuera-de-rango';
  }
  if (reglas.diasLaborables.indexOf(LETRAS_DIAS[dia.getDay()]) < 0) {
    return 'no-laborable';
  }
  if (reglas.fechasLlenas.indexOf(fecha) >= 0) {
    return 'lleno';
  }
  return 'disponible';
}

/**
 * Días de un mes para dibujar el calendario (la semana empieza en lunes).
 * Antes del día 1 se ponen huecos vacíos para que cada día caiga en su columna.
 */
export function diasDelMes(reglas: ReglasEntrega, anio: number, mes: number): DiaCalendario[] {
  const dias: DiaCalendario[] = [];
  const primero = new Date(anio, mes - 1, 1, 12, 0, 0);
  // getDay(): 0 = domingo. Con la semana en lunes, el domingo es la columna 7
  let huecos = primero.getDay() - 1;
  if (huecos < 0) {
    huecos = 6;
  }
  for (let i = 0; i < huecos; i++) {
    dias.push({ fecha: '', numero: 0, estado: 'fuera-de-rango' });
  }
  const totalDias = new Date(anio, mes, 0).getDate();
  for (let numero = 1; numero <= totalDias; numero++) {
    const fecha = armarFecha(anio, mes, numero);
    dias.push({ fecha: fecha, numero: numero, estado: estadoDelDia(reglas, fecha) });
  }
  return dias;
}
