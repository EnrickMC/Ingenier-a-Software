import { ReglasEntrega } from '../modelos/pedidos.models';
import { diasDelMes, estadoDelDia, fechaCorta, fechaLarga, textoAFecha } from './fechas';

// Reglas de ejemplo: lunes a sábado, del 2 al 30 de noviembre de 2026, el día 10 lleno
function reglasDePrueba(): ReglasEntrega {
  return {
    nombreNegocio: 'CARMIR',
    hoy: '2026-10-30',
    fechaMinima: '2026-11-02',
    fechaMaxima: '2026-11-30',
    diasAnticipacionMinima: 3,
    diasLaborables: ['L', 'M', 'X', 'J', 'V', 'S'],
    textoDiasLaborables: 'lunes a sábado',
    fechasLlenas: ['2026-11-10'],
    horaApertura: '09:00',
    horaCierre: '19:00',
    horarios: ['09:00', '09:30'],
    porcentajeAnticipo: 50,
    aceptaTransferencia: false,
    mensajeSoloSucursal: '',
  };
}

describe('fechas del Portal de Pedidos', function () {
  it('convierte y muestra fechas sin recorrer el día', function () {
    expect(fechaCorta('2026-11-10')).toBe('10/11/2026');
    expect(fechaLarga('2026-11-10')).toBe('martes 10 de noviembre de 2026');
    expect(textoAFecha('2026-02-30')).toBeNull();
  });

  it('marca días disponibles, llenos, no laborables y fuera de rango', function () {
    const reglas = reglasDePrueba();
    expect(estadoDelDia(reglas, '2026-11-09')).toBe('disponible');
    expect(estadoDelDia(reglas, '2026-11-10')).toBe('lleno');
    expect(estadoDelDia(reglas, '2026-11-08')).toBe('no-laborable');
    expect(estadoDelDia(reglas, '2026-11-01')).toBe('fuera-de-rango');
    expect(estadoDelDia(reglas, '2026-12-01')).toBe('fuera-de-rango');
  });

  it('el calendario empieza en lunes (noviembre 2026 empieza en domingo)', function () {
    const dias = diasDelMes(reglasDePrueba(), 2026, 11);
    // 6 huecos (lunes a sábado) y luego el domingo 1
    expect(dias[6].numero).toBe(1);
    expect(dias[5].fecha).toBe('');
    expect(dias.length).toBe(36);
  });
});
