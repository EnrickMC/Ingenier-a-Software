import { CaracteristicaPersonalizable, Exclusion, OpcionPersonalizable } from '../modelos/catalogo.models';
import {
  calcularPrecio,
  motivoExclusion,
  puntosTotales,
  validarCantidad,
  validarConfiguracion,
  validarTexto,
} from './reglas-configuracion';

// Datos parecidos a la semilla del Módulo 3
function opcion(idOpcion: number, idCaracteristica: number, nombre: string, precio: number, seEscala: boolean,
  multIngredientes: number, puntos: number, multPresupuesto: number): OpcionPersonalizable {
  return {
    idOpcion: idOpcion, idCaracteristica: idCaracteristica, nombre: nombre, descripcion: null, linkImagen: null,
    precioReferencia: precio, seEscala: seEscala, multiplicadorIngredientes: multIngredientes,
    puntosSeleccion: puntos, multiplicadorPresupuesto: multPresupuesto,
  };
}

const tamano: CaracteristicaPersonalizable = {
  idCaracteristica: 1, nombre: 'Tamaño', descripcion: null, obligatoria: true, seleccionMultiple: false,
  presupuestoSeleccion: 1, ordenMostrar: 1,
  opciones: [opcion(1, 1, 'Chico', 138, false, 1, 1, 1), opcion(3, 1, 'Grande', 276, false, 2, 1, 2)],
};
const sabor: CaracteristicaPersonalizable = {
  idCaracteristica: 2, nombre: 'Sabor', descripcion: null, obligatoria: true, seleccionMultiple: false,
  presupuestoSeleccion: 1, ordenMostrar: 2,
  opciones: [opcion(4, 2, 'Vainilla', 15.3, true, 1, 1, 1), opcion(6, 2, 'Tres leches', 207.375, true, 1, 1, 1)],
};
const relleno: CaracteristicaPersonalizable = {
  idCaracteristica: 3, nombre: 'Relleno', descripcion: null, obligatoria: false, seleccionMultiple: false,
  presupuestoSeleccion: 1, ordenMostrar: 3,
  opciones: [opcion(8, 3, 'Frambuesa', 135.9, true, 1, 1, 1), opcion(9, 3, 'Cajeta', 90, true, 1, 1, 1)],
};
const decoracion: CaracteristicaPersonalizable = {
  idCaracteristica: 4, nombre: 'Decoración', descripcion: null, obligatoria: false, seleccionMultiple: true,
  presupuestoSeleccion: 3, ordenMostrar: 4,
  opciones: [opcion(11, 4, 'Listón', 24, false, 1, 2, 1), opcion(12, 4, 'Flores de azúcar', 52, true, 1, 1, 1)],
};
const caracteristicas = [tamano, sabor, relleno, decoracion];
const exclusiones: Exclusion[] = [
  { idOpcion: 6, idOpcionExcluida: 8, nombreOpcion: 'Tres leches', nombreOpcionExcluida: 'Frambuesa', motivo: 'Corta la leche' },
];

describe('reglas de configuración', function () {
  it('calcula el mismo precio que vw_CostoPrecioPostre (Pastel tres leches de fiesta = 1102.75)', function () {
    const seleccion = [
      { idOpcion: 3, cantidad: 1 }, { idOpcion: 6, cantidad: 1 }, { idOpcion: 9, cantidad: 1 },
      { idOpcion: 11, cantidad: 1 }, { idOpcion: 12, cantidad: 2 },
    ];
    expect(calcularPrecio(caracteristicas, seleccion)).toBe(1102.75);
  });

  it('marca la característica obligatoria que falta', function () {
    const errores = validarConfiguracion(caracteristicas, exclusiones, [{ idOpcion: 1, cantidad: 1 }]);
    expect(errores.length).toBe(1);
    expect(errores[0].idCaracteristica).toBe(2);
  });

  it('el tamaño grande amplía el presupuesto de la decoración', function () {
    const conChico = [{ idOpcion: 1, cantidad: 1 }, { idOpcion: 4, cantidad: 1 }, { idOpcion: 11, cantidad: 2 }];
    expect(validarConfiguracion(caracteristicas, [], conChico)[0].tipo).toBe('presupuesto');
    const conGrande = [{ idOpcion: 3, cantidad: 1 }, { idOpcion: 4, cantidad: 1 }, { idOpcion: 11, cantidad: 2 }];
    expect(validarConfiguracion(caracteristicas, [], conGrande).length).toBe(0);
    expect(puntosTotales(caracteristicas, decoracion, conGrande)).toBe(6);
  });

  it('explica por qué una opción está excluida', function () {
    const seleccion = [{ idOpcion: 6, cantidad: 1 }];
    expect(motivoExclusion(8, seleccion, exclusiones)).toContain('Tres leches');
    expect(motivoExclusion(9, seleccion, exclusiones)).toBe('');
  });

  it('valida dedicatoria y cantidad', function () {
    expect(validarTexto('¡Feliz cumpleaños, Mamá!', 'La dedicatoria', 150)).toBe('');
    expect(validarTexto('Feliz 🎂', 'La dedicatoria', 150)).not.toBe('');
    expect(validarTexto('a'.repeat(151), 'La dedicatoria', 150)).toContain('150');
    expect(validarCantidad(0)).not.toBe('');
    expect(validarCantidad(21)).not.toBe('');
    expect(validarCantidad(3)).toBe('');
  });
});
