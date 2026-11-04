import { CaracteristicaPersonalizable, OpcionPersonalizable } from '../modelos/catalogo.models';
import { aparienciaDeOpcion, baseDeOpcion, capasDelPostre, colorDeNombre, colorDeOpcion, disenoDesdeSeleccion, lugarDelMensaje, moldeDeOpcion, nombreCapa, textoTopper, tipoDePostre, tipoDeCaracteristica } from './diseno-pastel';
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
    // «Especial de la casa» es la cobertura de siempre: betún
    expect(otra.cobertura).toBe('betun');
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
    expect(diseno.colorBizcocho).toBe('#4e2a1a');
    expect(diseno.decoraciones.velas).toBe(3);
    expect(diseno.decoraciones.mono).toBe(1);
    expect(diseno.decoraciones.flores).toBe(0);
    // Por defecto el mensaje se escribe sobre el pastel
    expect(diseno.dedicatoria).toBe('¡Feliz día!');
    expect(diseno.topper).toBe('');
  });

  it('el mensaje va en un topper si el cliente lo pide; en gelatinas y postres chicos siempre', function () {
    const pastel = disenoDesdeSeleccion([], [], 'Pasteles', 'Pastel', 'Hola', true);
    expect(pastel.topper).toBe('Hola');
    expect(pastel.dedicatoria).toBe('');
    const gelatina = disenoDesdeSeleccion([], [], 'Gelatinas', 'Gelatina', 'Hola', false);
    expect(gelatina.topper).toBe('Hola');
    const galletas = disenoDesdeSeleccion([], [], 'Galletas', 'Galletas', 'Hola', false);
    expect(galletas.topper).toBe('Hola');
    expect(galletas.dedicatoria).toBe('');
    expect(lugarDelMensaje('cupcake')).toBe('topper');
    expect(lugarDelMensaje('rosca')).toBe('elegir');
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

  it('el relleno toma el color de la mermelada o crema, no el del pan', function () {
    const relleno = caracteristica(8, 'Relleno', false, [opcion(18, 8, 'Mermelada de fresa'), opcion(19, 8, 'Cajeta')]);
    const fresa = disenoDesdeSeleccion([relleno], [{ idOpcion: 18, cantidad: 1 }], 'Pastel', 'Pastel', '');
    const cajeta = disenoDesdeSeleccion([relleno], [{ idOpcion: 19, cantidad: 1 }], 'Pastel', 'Pastel', '');
    expect(fresa.colorRelleno).toBe('#c3203a');
    expect(cajeta.colorRelleno).toBe('#a35d24');
    expect(aparienciaDeOpcion(relleno, relleno.opciones[0], false).color).toBe('#c3203a');
  });

  it('reconoce el tipo de postre: primero por categoría y luego por nombre', function () {
    expect(tipoDePostre('Pays', 'Pay de queso')).toBe('pay');
    expect(tipoDePostre('Cupcakes', 'Caja surtida')).toBe('cupcake');
    expect(tipoDePostre('Galletas', 'Galletas de avena')).toBe('galleta');
    expect(tipoDePostre('Postres', 'Brownie con nuez')).toBe('brownie');
    expect(tipoDePostre('Donas', 'Dona glaseada')).toBe('dona');
    // «Pastel de galleta oreo» sigue siendo pastel por su categoría
    expect(tipoDePostre('Pastel', 'Pastel de galleta oreo')).toBe('pastel');
    // «pie» no se confunde con «pieza»
    expect(tipoDePostre('Especiales', 'Pieza de temporada')).toBe('pastel');
  });

  it('las coberturas de mármol y sin cobertura se reconocen por nombre; las chispas son decoración', function () {
    const cubierta = caracteristica(9, 'Cobertura', false, [opcion(20, 9, 'Chispas de colores'), opcion(21, 9, 'Mármol'), opcion(22, 9, 'Sin cobertura'), opcion(23, 9, 'Normal')]);
    // Una cobertura «de chispas» de antes se dibuja con betún y la cubierta de chispas (decoración)
    const conChispas = disenoDesdeSeleccion([cubierta], [{ idOpcion: 20, cantidad: 1 }], 'Pastel', 'Pastel', '');
    expect(conChispas.cobertura).toBe('betun');
    expect(conChispas.decoraciones.chispasTodo).toBe(1);
    expect(disenoDesdeSeleccion([cubierta], [{ idOpcion: 21, cantidad: 1 }], 'Pastel', 'Pastel', '').cobertura).toBe('marmol');
    expect(disenoDesdeSeleccion([cubierta], [{ idOpcion: 22, cantidad: 1 }], 'Pastel', 'Pastel', '').cobertura).toBe('ninguna');
    expect(disenoDesdeSeleccion([cubierta], [{ idOpcion: 23, cantidad: 1 }], 'Pastel', 'Pastel', '').cobertura).toBe('betun');
    // Si el producto deja elegir cobertura y no se eligió ninguna, va sin cobertura
    expect(disenoDesdeSeleccion([cubierta], [], 'Pastel', 'Pastel', '').cobertura).toBe('ninguna');
    // Si el producto no tiene cobertura para elegir, lleva el betún de la casa
    expect(disenoDesdeSeleccion([], [], 'Pastel', 'Pastel', '').cobertura).toBe('betun');
  });

  it('galletas, donas y macarons se ven de una sola pieza y el tamaño la hace más grande', function () {
    const medidas = caracteristica(10, 'Tamaño', false, [opcion(24, 10, 'Mini'), opcion(25, 10, 'Mediano'), opcion(26, 10, 'Jumbo')]);
    const mini = disenoDesdeSeleccion([medidas], [{ idOpcion: 24, cantidad: 1 }], 'Donas', 'Donas', '');
    const jumbo = disenoDesdeSeleccion([medidas], [{ idOpcion: 26, cantidad: 1 }], 'Donas', 'Donas', '');
    expect(jumbo.diametro).toBeGreaterThan(mini.diametro);
    const macaron = disenoDesdeSeleccion([medidas], [{ idOpcion: 25, cantidad: 1 }], 'Macarons', 'Macarons', '');
    const galleta = disenoDesdeSeleccion([medidas], [{ idOpcion: 25, cantidad: 1 }], 'Galletas', 'Galletas', '');
    expect(galleta.diametro).toBeGreaterThan(macaron.diametro);
  });

  it('el pan del cupcake es el sabor y lo de arriba es el «Frosting»', function () {
    const sabores = caracteristica(12, 'Sabor', false, [opcion(31, 12, 'Chocolate')]);
    const frosting = caracteristica(13, 'Frosting', false, [opcion(32, 13, 'Remolino de chantilly'), opcion(33, 13, 'Domo de fondant'), opcion(34, 13, 'Frosting de fresa')]);
    expect(tipoDeCaracteristica('Frosting')).toBe('frosting');
    expect(tipoDeCaracteristica('Color del frosting')).toBe('color');
    // Sin «Frosting» el cupcake va sin nada arriba
    const solo = disenoDesdeSeleccion([sabores], [{ idOpcion: 31, cantidad: 1 }], 'Cupcakes', 'Cupcake', '');
    expect(solo.cobertura).toBe('ninguna');
    expect(solo.colorBizcocho).toBe('#4e2a1a');
    // Con «Frosting» el pan sigue siendo de chocolate y arriba va lo elegido
    const conChantilly = disenoDesdeSeleccion([sabores, frosting], [{ idOpcion: 31, cantidad: 1 }, { idOpcion: 32, cantidad: 1 }], 'Cupcakes', 'Cupcake', '');
    expect(conChantilly.cobertura).toBe('chantilly');
    expect(conChantilly.colorBizcocho).toBe('#4e2a1a');
    expect(disenoDesdeSeleccion([frosting], [{ idOpcion: 33, cantidad: 1 }], 'Cupcakes', 'Cupcake', '').cobertura).toBe('lisa');
    const fresa = disenoDesdeSeleccion([frosting], [{ idOpcion: 34, cantidad: 1 }], 'Cupcakes', 'Cupcake', '');
    expect(fresa.cobertura).toBe('betun');
    expect(fresa.colorCobertura).toBe('#f7b2c1');
    // Si el producto tiene «Frosting» pero no se eligió, tampoco lleva nada arriba
    expect(disenoDesdeSeleccion([frosting], [], 'Cupcakes', 'Cupcake', '').cobertura).toBe('ninguna');
  });

  it('el cupcake siempre es uno solo y el tamaño lo hace más grande', function () {
    const medidas = caracteristica(11, 'Tamaño', false, [opcion(27, 11, 'Mini'), opcion(28, 11, 'Mediano'), opcion(29, 11, 'Jumbo'), opcion(30, 11, 'Docena')]);
    const mini = disenoDesdeSeleccion([medidas], [{ idOpcion: 27, cantidad: 1 }], 'Cupcakes', 'Cupcake', '');
    const jumbo = disenoDesdeSeleccion([medidas], [{ idOpcion: 29, cantidad: 1 }], 'Cupcakes', 'Cupcake', '');
    expect(jumbo.diametro).toBeGreaterThan(mini.diametro);
  });

  // Los mismos nombres que sugiere el Panel Administrativo (PresetsCatalogo.java): todos deben verse en el 3D
  it('las categorías sugeridas en el Panel se dibujan con su modelo', function () {
    const esperado: string[][] = [
      ['Pasteles', 'pastel'], ['Gelatinas', 'gelatina'],
      ['Pays', 'pay'], ['Cheesecakes', 'cheesecake'], ['Cupcakes', 'cupcake'], ['Galletas', 'galleta'], ['Brownies', 'brownie'],
      ['Donas', 'dona'], ['Roscas', 'rosca'], ['Macarons', 'macaron'],
    ];
    for (const par of esperado) {
      expect(tipoDePostre(par[0], ''), par[0]).toBe(par[1]);
    }
  });

  it('las características sugeridas en el Panel cambian el 3D', function () {
    const esperado: string[][] = [
      ['Tamaño', 'tamano'], ['Pisos', 'pisos'], ['Forma', 'forma'], ['Sabor', 'sabor'], ['Relleno', 'relleno'],
      ['Cobertura', 'cobertura'], ['Frosting', 'frosting'], ['Color de cobertura', 'color'], ['Decoración', 'decoracion'], ['Base', 'base'],
    ];
    for (const par of esperado) {
      expect(tipoDeCaracteristica(par[0]), par[0]).toBe(par[1]);
    }
    expect(tipoDeCaracteristica('Color de vela')).toBe('otra');
  });

  it('las coberturas sugeridas en el Panel se reconocen', function () {
    const esperado: string[][] = [
      ['Betún de la casa', 'betun'], ['Buttercream', 'buttercream'], ['Rústica', 'rustica'], ['Fondant liso', 'lisa'], ['Glaseado', 'lisa'],
      ['Chantilly', 'chantilly'], ['Ganache de chocolate', 'ganache'], ['Naked cake', 'naked'], ['Mármol', 'marmol'],
      ['Degradado', 'degradado'], ['Sin cobertura', 'ninguna'],
      // Opciones de «Frosting» (parte de arriba del cupcake)
      ['Remolino de betún', 'betun'], ['Remolino de chantilly', 'chantilly'], ['Remolino de queso crema', 'betun'],
      ['Remolino degradado', 'degradado'], ['Domo de fondant', 'lisa'], ['Sin frosting', 'ninguna'],
    ];
    const opciones: OpcionPersonalizable[] = [];
    for (let i = 0; i < esperado.length; i++) {
      opciones.push(opcion(100 + i, 20, esperado[i][0]));
    }
    const cubierta = caracteristica(20, 'Cobertura', false, opciones);
    for (let i = 0; i < esperado.length; i++) {
      const diseno = disenoDesdeSeleccion([cubierta], [{ idOpcion: 100 + i, cantidad: 1 }], 'Pasteles', 'Pastel', '');
      expect(diseno.cobertura, esperado[i][0]).toBe(esperado[i][1]);
    }
  });

  it('las decoraciones sugeridas en el Panel aparecen en el 3D', function () {
    const nombres = ['Velas', 'Moño', 'Listón', 'Flores de azúcar', 'Perlas', 'Chispas', 'Cubierta de chispas', 'Fresas', 'Frutos rojos', 'Fruta picada', 'Rosetas de crema', 'Goteo de chocolate', 'Hoja de oro'];
    const campos = ['velas', 'mono', 'liston', 'flores', 'perlas', 'chispas', 'chispasTodo', 'fresas', 'frutosRojos', 'frutaPicada', 'rosetas', 'goteo', 'hojaOro'];
    const opciones: OpcionPersonalizable[] = [];
    for (let i = 0; i < nombres.length; i++) {
      opciones.push(opcion(200 + i, 21, nombres[i]));
    }
    const adornos = caracteristica(21, 'Decoración', true, opciones);
    for (let i = 0; i < nombres.length; i++) {
      const diseno = disenoDesdeSeleccion([adornos], [{ idOpcion: 200 + i, cantidad: 2 }], 'Pasteles', 'Pastel', '');
      const cuenta = diseno.decoraciones as unknown as Record<string, number>;
      expect(cuenta[campos[i]], nombres[i]).toBe(2);
    }
    expect(textoTopper('Topper Feliz cumpleaños')).toBe('Feliz cumpleaños');
    expect(textoTopper('Topper')).toBe('¡Felicidades!');
  });

  it('las bases, colores y sabores sugeridos en el Panel se reconocen', function () {
    const bases: string[][] = [
      ['Base dorada', 'dorada'], ['Base plateada', 'plateada'], ['Plato blanco', 'blanca'], ['Tabla de madera', 'madera'],
      ['Pizarra', 'pizarra'], ['Pedestal de cristal', 'cristal'],
    ];
    for (const par of bases) {
      expect(baseDeOpcion(par[0]), par[0]).toBe(par[1]);
    }
    const colores = ['Blanco', 'Marfil', 'Rosa', 'Rosa pastel', 'Rosa mexicano', 'Rojo', 'Lila', 'Morado', 'Azul cielo', 'Azul', 'Azul marino',
      'Turquesa', 'Verde menta', 'Verde', 'Amarillo', 'Naranja', 'Durazno', 'Coral', 'Dorado', 'Plateado', 'Negro', 'Chocolate'];
    for (const nombre of colores) {
      expect(colorDeNombre(nombre), nombre).not.toBeNull();
    }
    const sabores = ['Vainilla', 'Chocolate', 'Chocolate blanco', 'Fresa', 'Red velvet', 'Tres leches', 'Limón', 'Naranja', 'Mango', 'Piña',
      'Coco', 'Café', 'Cajeta', 'Zanahoria', 'Nuez', 'Matcha', 'Pistache', 'Oreo', 'Nutella', 'Frutos rojos', 'Plátano', 'Manzana con canela',
      'Guayaba', 'Queso', 'Mermelada de fresa', 'Crema pastelera', 'Queso crema', 'Durazno'];
    for (const nombre of sabores) {
      expect(colorDeOpcion(nombre, false, false), nombre).not.toBeNull();
    }
    const gelatinas = ['Fresa', 'Limón', 'Uva', 'Naranja', 'Piña', 'Mango', 'Mosaico', 'Rompope', 'Leche', 'Coco', 'Chocolate'];
    for (const nombre of gelatinas) {
      expect(colorDeOpcion(nombre, true, false), nombre).not.toBeNull();
    }
  });

  it('la base y el color de cobertura cambian el diseño', function () {
    const base = caracteristica(22, 'Base', false, [opcion(300, 22, 'Pedestal de cristal')]);
    const color = caracteristica(23, 'Color de cobertura', false, [opcion(301, 23, 'Azul cielo')]);
    const diseno = disenoDesdeSeleccion([base, color], [{ idOpcion: 300, cantidad: 1 }, { idOpcion: 301, cantidad: 1 }], 'Pasteles', 'Pastel', '');
    expect(diseno.base).toBe('cristal');
    expect(diseno.colorCobertura).toBe('#a9d4f0');
    // Sin base elegida: el pastel va en base dorada y las galletas en tabla
    expect(disenoDesdeSeleccion([], [], 'Pasteles', 'Pastel', '').base).toBe('dorada');
    expect(disenoDesdeSeleccion([], [], 'Galletas', 'Galletas', '').base).toBe('madera');
    // Si el producto deja elegir base y el cliente deja «Sin base», no se dibuja base
    expect(disenoDesdeSeleccion([base], [], 'Pasteles', 'Pastel', '').base).toBe('ninguna');
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
  it('cada capa del pastel tiene su propio sabor, cobertura y decoración', function () {
    const seleccion = [
      { idOpcion: 13, cantidad: 1 },
      // Base: chocolate con ganache y velas
      { idOpcion: 5, cantidad: 1, capa: 1 }, { idOpcion: 15, cantidad: 1, capa: 1 }, { idOpcion: 8, cantidad: 2, capa: 1 },
      // Capa 2: vainilla con fondant y flores
      { idOpcion: 4, cantidad: 1, capa: 2 }, { idOpcion: 14, cantidad: 1, capa: 2 }, { idOpcion: 10, cantidad: 3, capa: 2 },
      // Superior: vainilla con betún de la casa
      { idOpcion: 4, cantidad: 1, capa: 3 }, { idOpcion: 16, cantidad: 1, capa: 3 },
    ];
    const diseno = disenoDesdeSeleccion(todas, seleccion, 'Pasteles', 'Pastel', '');
    expect(diseno.pisos).toBe(3);
    expect(diseno.porCapas).toBe(true);
    expect(diseno.capas.length).toBe(3);
    expect(diseno.capas[0].cobertura).toBe('ganache');
    expect(diseno.capas[1].cobertura).toBe('lisa');
    expect(diseno.capas[2].cobertura).toBe('betun');
    expect(diseno.capas[0].colorBizcocho).not.toBe(diseno.capas[1].colorBizcocho);
    expect(diseno.capas[1].decoraciones.flores).toBe(3);
    expect(diseno.capas[2].decoraciones.flores).toBe(0);
    // Las velas se suman (siempre van hasta arriba)
    expect(diseno.decoraciones.velas).toBe(2);
    expect(capasDelPostre(todas, seleccion, 'Pasteles', 'Pastel')).toBe(3);
    // Las gelatinas no tienen capas
    expect(capasDelPostre(todas, seleccion, 'Gelatinas', 'Gelatina')).toBe(1);
    expect(nombreCapa(1, 3)).toBe('Base');
    expect(nombreCapa(2, 3)).toBe('Capa 2');
    expect(nombreCapa(3, 3)).toBe('Superior');
  });

  it('las formas de gelatina sugeridas en el Panel tienen su molde', function () {
    const esperado: string[][] = [
      ['Molde de gajos', 'gajos'], ['Rosca', 'rosca'], ['Domo', 'domo'], ['Corazón', 'figura'], ['Estrella', 'figura'],
      ['Flor', 'figura'], ['Cuadrada', 'figura'], ['Redonda', 'gajos'],
    ];
    for (const par of esperado) {
      expect(moldeDeOpcion(par[0]), par[0]).toBe(par[1]);
    }
    const moldes = caracteristica(30, 'Forma', false, [opcion(300, 30, 'Rosca'), opcion(301, 30, 'Corazón')]);
    expect(disenoDesdeSeleccion([moldes], [{ idOpcion: 300, cantidad: 1 }], 'Gelatinas', 'Gelatina', '').molde).toBe('rosca');
    const corazon = disenoDesdeSeleccion([moldes], [{ idOpcion: 301, cantidad: 1 }], 'Gelatinas', 'Gelatina', '');
    expect(corazon.molde).toBe('figura');
    expect(corazon.forma).toBe('corazon');
    expect(aparienciaDeOpcion(moldes, moldes.opciones[0], true).molde).toBe('rosca');
  });
});
