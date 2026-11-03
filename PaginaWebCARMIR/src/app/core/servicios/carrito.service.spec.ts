import { TestBed } from '@angular/core/testing';
import { ArticuloNuevo } from '../modelos/articulo-carrito';
import { CarritoService, claveDeOpciones } from './carrito.service';

// Pastel de ejemplo (las opciones pueden venir en cualquier orden)
function pastel(cantidad: number, dedicatoria: string): ArticuloNuevo {
  return {
    idPostre: 1,
    nombre: 'Pastel tres leches',
    linkFotografia: null,
    opciones: [
      { idOpcion: 5, nombre: 'Fresa', cantidad: 1 },
      { idOpcion: 2, nombre: 'Grande', cantidad: 1 },
    ],
    cantidad: cantidad,
    dedicatoria: dedicatoria,
    comentario: '',
    precioUnitario: 450,
  };
}

describe('carrito de compras', function () {
  let carrito: CarritoService;

  beforeEach(function () {
    localStorage.clear();
    TestBed.configureTestingModule({});
    carrito = TestBed.inject(CarritoService);
  });

  it('la clave de opciones es la misma que usa la base de datos (ordenada por id)', function () {
    expect(claveDeOpciones(pastel(1, '').opciones)).toBe('2:1|5:1');
  });

  it('junta el mismo postre y respeta el máximo de 20 piezas', function () {
    carrito.agregar(pastel(15, 'Feliz día'));
    const resultado = carrito.agregar(pastel(10, 'Feliz día'));
    expect(resultado.juntado).toBe(true);
    expect(resultado.cantidadAjustada).toBe(true);
    expect(carrito.articulos().length).toBe(1);
    expect(carrito.articulos()[0].cantidad).toBe(20);
  });

  it('un mensaje distinto es otro renglón; editar para que quede igual los junta', function () {
    carrito.agregar(pastel(1, 'Feliz día'));
    carrito.agregar(pastel(2, 'Felicidades'));
    expect(carrito.articulos().length).toBe(2);
    const segundo = carrito.articulos()[1].id;
    expect(carrito.reemplazar(segundo, pastel(2, 'Feliz día'))).toBe(true);
    expect(carrito.articulos().length).toBe(1);
    expect(carrito.articulos()[0].cantidad).toBe(3);
  });

  it('quitar, vaciar y total', function () {
    carrito.agregar(pastel(2, ''));
    expect(carrito.totalImporte()).toBe(900);
    expect(carrito.quitar('no-existe')).toBe(false);
    carrito.vaciar();
    expect(carrito.articulos().length).toBe(0);
  });
});
