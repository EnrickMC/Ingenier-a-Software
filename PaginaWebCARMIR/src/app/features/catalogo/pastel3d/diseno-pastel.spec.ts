import { CaracteristicaPersonalizable, OpcionPersonalizable } from '../modelos/catalogo.models';
import { aparienciaDeOpcion, disenoDesdeSeleccion, tipoDeCaracteristica } from './diseno-pastel';
import { puntosForma, rutaSvgForma } from './formas-pastel';

// Opción con los datos mínimos (el precio no importa para el 3D)
function opcion(idOpcion: number, idCaracteristica: number, nombre: string): OpcionPersonalizable {
  return {
    idOpcion: idOpcion, idCaracteristica: idCaracteristica, nombre: nombre, descripcion: null, linkImagen: null,
    precioReferencia: 10, seEscala: false, multiplicadorIngredientes: 1, puntosSeleccion: 1, multiplicadorPresupuesto: 1,
  };
}

function caracteristica(id: number, nombre: string, multiple: boolean, opciones: OpcionPersonalizable[]): CaracteristicaPersonalizable {
  return {
    idCaracteristica: id, nombre: nombre, descripcion: null, obligatoria: false, seleccionMultiple: multiple,
    presupuestoSeleccion: 3, ordenMostrar: id, opciones: opciones,
  };
}

const tamano = caracteristica(1, 'Tamaño', false, [opcion(1, 1, 'Chico'), opcion(2, 1, 'Mediano'), opcion(3, 1, 'Grande')]);
const sabor = caracteristica(2, 'Sabor', false, [opcion(4, 2, 'Vainilla'), opcion(5, 2, 'Chocolate')]);
const forma = caracteristica(3, 'Forma del pastel', false, [opcion(6, 3, 'Redondo'), opcion(7, 3, 'Corazón')]);
const decoracion = caracteristica(4, 'Decoración', true, [opcion(8, 4, 'Velas'), opcion(9, 4, 'Moño'), opcion(10, 4, 'Flores de azúcar')]);
const pisos = caracteristica(5, 'Pisos', false, [opcion(11, 5, 'Un piso'), opcion(12, 5, 'Dos pisos'), opcion(13, 5, '3 pisos')]);
const cobertura = caracteristica(6, 'Cobertura', false, [opcion(14, 6, 'Fondant'), opcion(15, 6, 'Ganache'), opcion(16, 6, 'Especial de la casa')]);
const todas = [tamano, sabor, forma, decoracion, pisos, cobertura];

describe('Diseño del pastel 3D', function () {
  it('reconoce el tipo de cada característica por su nombre', function () {
    expect(tipoDeCaracteristica('Tamaño')).toBe('tamano');
    expect(tipoDeCaracteristica('Forma del pastel')).toBe('forma');
    expect(tipoDeCaracteristica('Sabor del pan')).toBe('sabor');
    expect(tipoDeCaracteristica('Relleno')).toBe('relleno');
    expect(tipoDeCaracteristica('Color de vela')).toBe('otra');
  });

  it('el tamaño hace más grande el pastel sin agregar pisos', function () {
    const chico = disenoDesdeSeleccion(todas, [{ idOpcion: 1, cantidad: 1 }], 'Pastel', 'Pastel', '');
    const grande = disenoDesdeSeleccion(todas, [{ idOpcion: 3, cantidad: 1 }], 'Pastel', 'Pastel', '');
    expect(chico.pisos).toBe(1);
    expect(grande.pisos).toBe(1);
    expect(grande.diametro).toBeGreaterThan(chico.diametro);
  });

  it('los pisos solo cambian con la característica «Pisos»', function () {
    expect(tipoDeCaracteristica('Pisos')).toBe('pisos');
    const dos = disenoDesdeSeleccion(todas, [{ idOpcion: 3, cantidad: 1 }, { idOpcion: 12, cantidad: 1 }], 'Pastel', 'Pastel', '');
    const tres = disenoDesdeSeleccion(todas, [{ idOpcion: 13, cantidad: 1 }], 'Pastel', 'Pastel', '');
    expect(dos.pisos).toBe(2);
    expect(tres.pisos).toBe(3);
  });

  it('cada cobertura cambia el acabado del pastel', function () {
    const fondant = disenoDesdeSeleccion(todas, [{ idOpcion: 14, cantidad: 1 }], 'Pastel', 'Pastel', '');
    const ganache = disenoDesdeSeleccion(todas, [{ idOpcion: 15, cantidad: 1 }], 'Pastel', 'Pastel', '');
    const otra = disenoDesdeSeleccion(todas, [{ idOpcion: 16, cantidad: 1 }], 'Pastel', 'Pastel', '');
    expect(fondant.cobertura).toBe('lisa');
    expect(ganache.cobertura).toBe('ganache');
    expect(ganache.colorCobertura).not.toBe(fondant.colorCobertura);
    expect(otra.cobertura).toBe('chantilly');
  });

  it('la forma, el sabor y las decoraciones cambian el diseño', function () {
    const diseno = disenoDesdeSeleccion(
      todas,
      [{ idOpcion: 7, cantidad: 1 }, { idOpcion: 5, cantidad: 1 }, { idOpcion: 8, cantidad: 3 }, { idOpcion: 9, cantidad: 1 }],
      'Pastel',
      'Pastel',
      ' ¡Feliz día! ',
    );
    expect(diseno.forma).toBe('corazon');
    expect(diseno.colorBizcocho).toBe('#5a3221');
    expect(diseno.decoraciones.velas).toBe(3);
    expect(diseno.decoraciones.mono).toBe(1);
    expect(diseno.decoraciones.flores).toBe(0);
    expect(diseno.dedicatoria).toBe('¡Feliz día!');
  });

  it('cada unidad elegida de una decoración es una pieza en el 3D', function () {
    const muchas = disenoDesdeSeleccion(
      todas,
      [{ idOpcion: 8, cantidad: 7 }, { idOpcion: 9, cantidad: 2 }, { idOpcion: 10, cantidad: 5 }],
      'Pastel',
      'Pastel',
      '',
    );
    expect(muchas.decoraciones.velas).toBe(7);
    expect(muchas.decoraciones.mono).toBe(2);
    expect(muchas.decoraciones.flores).toBe(5);
  });

  it('si la característica no es múltiple solo se dibuja una pieza', function () {
    const unica = caracteristica(7, 'Adorno', false, [opcion(17, 7, 'Fresas')]);
    const diseno = disenoDesdeSeleccion([unica], [{ idOpcion: 17, cantidad: 4 }], 'Pastel', 'Pastel', '');
    expect(diseno.decoraciones.fresas).toBe(1);
  });

  it('una gelatina se dibuja de un solo molde', function () {
    const diseno = disenoDesdeSeleccion([tamano], [{ idOpcion: 3, cantidad: 1 }], 'Gelatina', 'Gelatina mosaico', '');
    expect(diseno.tipo).toBe('gelatina');
    expect(diseno.pisos).toBe(1);
  });

  it('las opciones se dibujan con pisos, silueta o color', function () {
    expect(aparienciaDeOpcion(tamano, tamano.opciones[1], false).nivel).toBe(2);
    expect(aparienciaDeOpcion(forma, forma.opciones[1], false).forma).toBe('corazon');
    expect(aparienciaDeOpcion(sabor, sabor.opciones[1], false).tipo).toBe('color');
    expect(aparienciaDeOpcion(decoracion, decoracion.opciones[0], false).tipo).toBe('ninguna');
  });

  it('cada forma tiene el contorno del tamaño pedido', function () {
    const puntos = puntosForma('estrella', 30, 90);
    let maximo = 0;
    for (const punto of puntos) {
      maximo = Math.max(maximo, Math.abs(punto.x), Math.abs(punto.y));
    }
    expect(puntos.length).toBe(90);
    expect(maximo).toBeLessThanOrEqual(15.01);
    expect(rutaSvgForma('corazon', 48).startsWith('M')).toBe(true);
  });
});
