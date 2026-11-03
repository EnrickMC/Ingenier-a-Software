/*
 * Servicio del Catálogo Público: reglas de negocio de RF-1, RF-2, RF-3 (cotizar) y RF-4.
 * Recibe datos ya validados por el controlador, consulta el repositorio
 * y arma las respuestas que necesita la página web.
 *
 * Concepto clave: cada PRODUCTO del catálogo es una PLANTILLA BASE.
 * Sus opciones (tabla PostreOpcion) son el punto de partida que el cliente personaliza.
 */
const repositorio = require('./catalogo.repositorio');
const reglas = require('./reglasConfiguracion');
const ErrorApi = require('../../utilidades/ErrorApi');
const enlaceImagen = require('../../utilidades/enlaceImagen');

// Mensajes exactos de los casos de uso
const MENSAJE_PRODUCTO_NO_DISPONIBLE = 'Este producto ya no está disponible';
const MENSAJE_CATEGORIA_NO_DISPONIBLE = 'La categoría seleccionada ya no está disponible';
const MENSAJE_NO_PERSONALIZABLE = 'Por ahora este producto no se puede personalizar: una característica obligatoria no tiene opciones disponibles';

// ===================== Conversión de filas a objetos de la API =====================

function aBooleano(valor) {
  return valor === 1 || valor === true || valor === '1';
}

function aNumero(valor) {
  if (valor === null || valor === undefined) {
    return 0;
  }
  return Number(valor);
}

function convertirProducto(fila) {
  return {
    idPostre: fila.IdPostre,
    nombre: fila.NombrePostre,
    descripcion: fila.DescripcionPostre,
    linkFotografia: enlaceImagen.enlaceParaWeb(fila.LinkFotografia, 'postre', fila.IdPostre),
    precio: aNumero(fila.PrecioVenta),
    idCategoria: fila.IdCategoria,
    nombreCategoria: fila.NombreCategoria
  };
}

function convertirCaracteristica(fila) {
  return {
    idCaracteristica: fila.IdCaracteristica,
    nombre: fila.NombreCaracteristica,
    descripcion: fila.DescripcionCaracteristica,
    obligatoria: aBooleano(fila.Obligatoria),
    seleccionMultiple: aBooleano(fila.SeleccionMultiple),
    presupuestoSeleccion: aNumero(fila.PresupuestoSeleccion),
    ordenMostrar: fila.OrdenMostrar,
    disponible: aBooleano(fila.Disponible),
    opciones: []
  };
}

// La opción lleva su precio de referencia (costo × multiplicador), nunca el costo
function convertirOpcion(fila) {
  const precioReferencia = aNumero(fila.CostoReceta) * aNumero(fila.MultiplicadorCosto);
  return {
    idOpcion: fila.IdOpcion,
    idCaracteristica: fila.IdCaracteristica,
    nombre: fila.NombreOpcion,
    descripcion: fila.DescripcionOpcion,
    linkImagen: enlaceImagen.enlaceParaWeb(fila.LinkImagen, 'opcion', fila.IdOpcion),
    // Se redondea a 6 decimales solo para quitar el "ruido" de los decimales de JavaScript
    precioReferencia: Math.round(precioReferencia * 1000000) / 1000000,
    seEscala: aBooleano(fila.SeEscala),
    multiplicadorIngredientes: aNumero(fila.MultiplicadorIngredientes),
    puntosSeleccion: aNumero(fila.PuntosSeleccion),
    multiplicadorPresupuesto: aNumero(fila.MultiplicadorPresupuesto),
    disponible: aBooleano(fila.Disponible)
  };
}

function convertirExclusion(fila) {
  return {
    idOpcion: fila.IdOpcion,
    idOpcionExcluida: fila.IdOpcionExcluida,
    nombreOpcion: fila.NombreOpcion,
    nombreOpcionExcluida: fila.NombreOpcionExcluida,
    motivo: fila.MotivoExclusion
  };
}

// ===================== Apoyo =====================

// Regresa el producto visible o lanza "Este producto ya no está disponible"
// RF-2 / RF-3 / RF-4 · Paso 2: el producto debe existir, estar activo y visible.
async function obtenerProductoOFallar(idPostre) {
  const fila = await repositorio.buscarProductoVisible(idPostre);
  // Excepción paso 2 (RF-2, RF-3, RF-4): el producto o su categoría ya no están disponibles.
  if (fila === null) {
    throw new ErrorApi(404, 'PRODUCTO_NO_DISPONIBLE', MENSAJE_PRODUCTO_NO_DISPONIBLE);
  }
  return convertirProducto(fila);
}

// Carga las reglas de personalización de una categoría:
// características disponibles con sus opciones (todas, con su bandera disponible) y exclusiones
async function cargarReglasDeCategoria(idCategoria) {
  const filasCaracteristicas = await repositorio.listarCaracteristicas(idCategoria);
  const filasOpciones = await repositorio.listarOpciones(idCategoria);
  const filasExclusiones = await repositorio.listarExclusiones(idCategoria);

  const caracteristicas = [];
  for (let i = 0; i < filasCaracteristicas.length; i++) {
    caracteristicas.push(convertirCaracteristica(filasCaracteristicas[i]));
  }

  const opciones = [];
  for (let i = 0; i < filasOpciones.length; i++) {
    const opcion = convertirOpcion(filasOpciones[i]);
    opciones.push(opcion);
    for (let j = 0; j < caracteristicas.length; j++) {
      if (caracteristicas[j].idCaracteristica === opcion.idCaracteristica) {
        caracteristicas[j].opciones.push(opcion);
      }
    }
  }

  const exclusiones = [];
  for (let i = 0; i < filasExclusiones.length; i++) {
    exclusiones.push(convertirExclusion(filasExclusiones[i]));
  }

  return { caracteristicas: caracteristicas, opciones: opciones, exclusiones: exclusiones };
}

// Una opción se puede elegir si ella y su característica están disponibles
function opcionElegible(opcion, caracteristicas) {
  if (!opcion.disponible) {
    return false;
  }
  for (let i = 0; i < caracteristicas.length; i++) {
    if (caracteristicas[i].idCaracteristica === opcion.idCaracteristica) {
      return caracteristicas[i].disponible;
    }
  }
  return false;
}

function buscarOpcion(opciones, idOpcion) {
  for (let i = 0; i < opciones.length; i++) {
    if (opciones[i].idOpcion === idOpcion) {
      return opciones[i];
    }
  }
  return null;
}

// RF-2 paso 4: cada característica obligatoria debe tener al menos una opción disponible
function revisarSiSePuedePersonalizar(caracteristicas) {
  for (let i = 0; i < caracteristicas.length; i++) {
    const caracteristica = caracteristicas[i];
    if (caracteristica.disponible && caracteristica.obligatoria) {
      let hayOpcion = false;
      for (let j = 0; j < caracteristica.opciones.length; j++) {
        if (caracteristica.opciones[j].disponible) {
          hayOpcion = true;
        }
      }
      if (!hayOpcion) {
        return false;
      }
    }
  }
  return true;
}

// Copia de la característica solo con sus opciones disponibles (lo que ve el cliente)
function caracteristicaPublica(caracteristica) {
  const copia = {
    idCaracteristica: caracteristica.idCaracteristica,
    nombre: caracteristica.nombre,
    descripcion: caracteristica.descripcion,
    obligatoria: caracteristica.obligatoria,
    seleccionMultiple: caracteristica.seleccionMultiple,
    presupuestoSeleccion: caracteristica.presupuestoSeleccion,
    ordenMostrar: caracteristica.ordenMostrar,
    opciones: []
  };
  for (let i = 0; i < caracteristica.opciones.length; i++) {
    const opcion = caracteristica.opciones[i];
    if (opcion.disponible) {
      copia.opciones.push({
        idOpcion: opcion.idOpcion,
        idCaracteristica: opcion.idCaracteristica,
        nombre: opcion.nombre,
        descripcion: opcion.descripcion,
        linkImagen: opcion.linkImagen,
        precioReferencia: opcion.precioReferencia,
        seEscala: opcion.seEscala,
        multiplicadorIngredientes: opcion.multiplicadorIngredientes,
        puntosSeleccion: opcion.puntosSeleccion,
        multiplicadorPresupuesto: opcion.multiplicadorPresupuesto
      });
    }
  }
  return copia;
}

// ===================== RF-1: catálogo =====================

// RF-1 · Paso 2: consulta las categorías activas con su total de productos.
async function listarCategorias() {
  const filas = await repositorio.listarCategorias();
  const categorias = [];
  for (let i = 0; i < filas.length; i++) {
    categorias.push({
      idCategoria: filas[i].IdCategoria,
      nombre: filas[i].NombreCategoria,
      descripcion: filas[i].DescripcionCategoria,
      totalProductos: aNumero(filas[i].TotalProductos)
    });
  }
  return categorias;
}

// RF-1 (API): inicia aquí. Pasos 2 a 4 y 6: lista los productos visibles con sus filtros.
async function listarProductos(idCategoria, buscar, orden) {
  // RF-1 paso 6: la categoría elegida debe existir y seguir activa
  if (idCategoria > 0) {
    const categoria = await repositorio.buscarCategoria(idCategoria);
    // RF-1 · Excepción paso 6: la categoría no existe o ya no está activa.
    if (categoria === null || categoria.EstadoCategoria !== 'Activa') {
      throw new ErrorApi(404, 'CATEGORIA_NO_DISPONIBLE', MENSAJE_CATEGORIA_NO_DISPONIBLE);
    }
  }
  // RF-1 · Paso 3: el repositorio solo regresa productos con sus datos mínimos completos (los incompletos se excluyen).
  const filas = await repositorio.listarProductos(idCategoria, buscar, orden);
  const productos = [];
  for (let i = 0; i < filas.length; i++) {
    productos.push(convertirProducto(filas[i]));
  }
  return productos;
}

// ===================== RF-2: ficha del producto =====================

// RF-2 (API): inicia aquí.
async function obtenerFicha(idPostre) {
  // RF-2 · Paso 2: valida que el producto exista, esté activo y visible.
  const producto = await obtenerProductoOFallar(idPostre);

  // RF-2 · Paso 3: recupera lo que incluye la plantilla.
  // Opciones que trae la plantilla, agrupadas por característica
  const filasOpciones = await repositorio.listarOpcionesDeProducto(idPostre);
  const incluye = [];
  const opcionesRetiradas = [];
  for (let i = 0; i < filasOpciones.length; i++) {
    const fila = filasOpciones[i];
    // RF-2 · Excepción paso 3: las opciones retiradas no se muestran como incluidas; se avisan aparte.
    if (!aBooleano(fila.OpcionDisponible) || !aBooleano(fila.CaracteristicaDisponible)) {
      opcionesRetiradas.push(fila.NombreOpcion);
      continue;
    }
    let grupo = null;
    for (let j = 0; j < incluye.length; j++) {
      if (incluye[j].idCaracteristica === fila.IdCaracteristica) {
        grupo = incluye[j];
      }
    }
    if (grupo === null) {
      grupo = { idCaracteristica: fila.IdCaracteristica, caracteristica: fila.NombreCaracteristica, opciones: [] };
      incluye.push(grupo);
    }
    grupo.opciones.push({ nombre: fila.NombreOpcion, cantidad: fila.CantidadElegida });
  }

  // Ingredientes según las recetas de sus opciones (sin cantidades)
  const filasIngredientes = await repositorio.listarIngredientesDeProducto(idPostre);
  // RF-2 · Excepción paso 3: si la lista queda vacía, la página muestra «Información de ingredientes no disponible, consulte con la pastelería».
  const ingredientes = [];
  for (let i = 0; i < filasIngredientes.length; i++) {
    ingredientes.push(filasIngredientes[i].NombreInsumo);
  }

  // Características que se pueden personalizar en su categoría
  const reglasCategoria = await cargarReglasDeCategoria(producto.idCategoria);
  // RF-2 · Paso 4: valida que cada característica obligatoria tenga al menos una opción disponible.
  const sePuedePersonalizar = revisarSiSePuedePersonalizar(reglasCategoria.caracteristicas);
  const personalizables = [];
  for (let i = 0; i < reglasCategoria.caracteristicas.length; i++) {
    const caracteristica = reglasCategoria.caracteristicas[i];
    if (caracteristica.disponible) {
      const publica = caracteristicaPublica(caracteristica);
      const nombresOpciones = [];
      for (let j = 0; j < publica.opciones.length; j++) {
        nombresOpciones.push(publica.opciones[j].nombre);
      }
      personalizables.push({
        nombre: publica.nombre,
        obligatoria: publica.obligatoria,
        opciones: nombresOpciones
      });
    }
  }

  let motivoNoPersonalizable = null;
  // RF-2 · Excepción paso 4: la ficha queda en modo informativo y se deshabilita «Personalizar pedido».
  if (!sePuedePersonalizar) {
    motivoNoPersonalizable = MENSAJE_NO_PERSONALIZABLE;
  }

  return {
    producto: producto,
    incluye: incluye,
    opcionesRetiradas: opcionesRetiradas,
    ingredientes: ingredientes,
    caracteristicas: personalizables,
    sePuedePersonalizar: sePuedePersonalizar,
    motivoNoPersonalizable: motivoNoPersonalizable
  };
}

// ===================== RF-3: datos para personalizar =====================

// RF-3 (API): inicia aquí. Paso 2: valida el producto y consulta lo que se puede personalizar.
async function obtenerPersonalizacion(idPostre) {
  const producto = await obtenerProductoOFallar(idPostre);
  const reglasCategoria = await cargarReglasDeCategoria(producto.idCategoria);

  const caracteristicas = [];
  for (let i = 0; i < reglasCategoria.caracteristicas.length; i++) {
    if (reglasCategoria.caracteristicas[i].disponible) {
      caracteristicas.push(caracteristicaPublica(reglasCategoria.caracteristicas[i]));
    }
  }

  // Solo las exclusiones entre opciones que el cliente puede elegir
  const exclusiones = [];
  for (let i = 0; i < reglasCategoria.exclusiones.length; i++) {
    const exclusion = reglasCategoria.exclusiones[i];
    const opcionA = buscarOpcion(reglasCategoria.opciones, exclusion.idOpcion);
    const opcionB = buscarOpcion(reglasCategoria.opciones, exclusion.idOpcionExcluida);
    if (opcionA !== null && opcionB !== null) {
      exclusiones.push(exclusion);
    }
  }

  // Selección inicial = las opciones de la plantilla que siguen disponibles (RF-4: precarga)
  const filasOpciones = await repositorio.listarOpcionesDeProducto(idPostre);
  const seleccionInicial = [];
  const opcionesRetiradas = [];
  for (let i = 0; i < filasOpciones.length; i++) {
    const fila = filasOpciones[i];
    const opcion = buscarOpcion(reglasCategoria.opciones, fila.IdOpcion);
    if (opcion !== null && opcionElegible(opcion, reglasCategoria.caracteristicas)) {
      seleccionInicial.push({ idOpcion: fila.IdOpcion, cantidad: fila.CantidadElegida });
    } else {
      opcionesRetiradas.push(fila.NombreOpcion);
    }
  }

  // RF-3 · Excepción: si una obligatoria no tiene opciones, la página no deja personalizar.
  let motivoNoPersonalizable = null;
  const sePuedePersonalizar = revisarSiSePuedePersonalizar(reglasCategoria.caracteristicas);
  if (!sePuedePersonalizar) {
    motivoNoPersonalizable = MENSAJE_NO_PERSONALIZABLE;
  }

  return {
    producto: producto,
    caracteristicas: caracteristicas,
    exclusiones: exclusiones,
    seleccionInicial: seleccionInicial,
    opcionesRetiradas: opcionesRetiradas,
    sePuedePersonalizar: sePuedePersonalizar,
    motivoNoPersonalizable: motivoNoPersonalizable
  };
}

// ===================== RF-4: galería de plantillas =====================

// Plantillas = los demás productos base visibles de la misma categoría.
// Solo se ofrecen las que tienen todas sus opciones disponibles (si no, no se pueden aplicar).
// RF-4 (API): inicia aquí. Paso 2: valida el producto y consulta las plantillas activas de su categoría.
async function listarPlantillas(idPostre) {
  const producto = await obtenerProductoOFallar(idPostre);
  const reglasCategoria = await cargarReglasDeCategoria(producto.idCategoria);
  const filasProductos = await repositorio.listarProductos(producto.idCategoria, '', 'nombre');
  const filasOpciones = await repositorio.listarOpcionesDePlantillas(producto.idCategoria);

  const plantillas = [];
  for (let i = 0; i < filasProductos.length; i++) {
    const plantilla = convertirProducto(filasProductos[i]);
    const opciones = [];
    let todasDisponibles = true;
    for (let j = 0; j < filasOpciones.length; j++) {
      if (filasOpciones[j].IdPostre === plantilla.idPostre) {
        const opcion = buscarOpcion(reglasCategoria.opciones, filasOpciones[j].IdOpcion);
        // RF-4 · Excepción paso 6: una plantilla con opciones retiradas no se ofrece.
        if (opcion === null || !opcionElegible(opcion, reglasCategoria.caracteristicas)) {
          todasDisponibles = false;
        } else {
          opciones.push({ idOpcion: opcion.idOpcion, nombre: opcion.nombre, cantidad: filasOpciones[j].CantidadElegida });
        }
      }
    }
    if (todasDisponibles && opciones.length > 0) {
      plantilla.opciones = opciones;
      plantillas.push(plantilla);
    }
  }
  // RF-4 · Paso 3: si la lista queda vacía, la página muestra «Este producto no cuenta con plantillas de diseño».
  return plantillas;
}

// ===================== RF-3: cotizar una configuración =====================

// datos = { idPostre, opciones: [{ idOpcion, cantidad }], cantidad, dedicatoria, comentario, precioMostrado } (ya validados)
// RF-3 · Paso 7 (API): inicia aquí la validación final antes de agregar al carrito.
async function cotizar(datos) {
  // RF-3 · Paso 7: el producto sigue disponible.
  const producto = await obtenerProductoOFallar(datos.idPostre);
  const reglasCategoria = await cargarReglasDeCategoria(producto.idCategoria);

  const elegidas = [];
  const opcionesNoDisponibles = [];
  const errores = [];

  for (let i = 0; i < datos.opciones.length; i++) {
    const pedida = datos.opciones[i];
    const opcion = buscarOpcion(reglasCategoria.opciones, pedida.idOpcion);
    if (opcion === null || !opcionElegible(opcion, reglasCategoria.caracteristicas)) {
      // RF-3 paso 7: la opción ya no existe o se desactivó; la página la retira
      opcionesNoDisponibles.push(pedida.idOpcion);
      let nombre = 'opción ' + pedida.idOpcion;
      if (opcion !== null) {
        nombre = opcion.nombre;
      }
      errores.push({
        tipo: 'disponibilidad',
        idCaracteristica: 0,
        mensaje: 'La opción seleccionada ya no está disponible: ' + nombre
      });
    } else {
      elegidas.push({ opcion: opcion, cantidad: pedida.cantidad });
    }
  }

  // RF-3 · Pasos 6 y 7: obligatorias, presupuesto de puntos y exclusiones entre opciones.
  const erroresReglas = reglas.validar(reglasCategoria.caracteristicas, reglasCategoria.exclusiones, elegidas);
  for (let i = 0; i < erroresReglas.length; i++) {
    errores.push(erroresReglas[i]);
  }

  // RF-3 · Paso 8: precio de la configuración.
  const precioUnitario = reglas.calcularPrecio(elegidas);
  const total = reglas.redondear(precioUnitario * datos.cantidad);

  // RF-3 paso 7: si el precio que vio el cliente ya no es el vigente, se le avisa
  let precioCambio = false;
  if (datos.precioMostrado > 0 && Math.abs(datos.precioMostrado - precioUnitario) >= 0.01) {
    precioCambio = true;
  }

  return {
    valido: errores.length === 0,
    // Datos del producto base (el Módulo 2 los usa para registrar el postre y armar el comprobante)
    idCategoria: producto.idCategoria,
    nombreProducto: producto.nombre,
    precioUnitario: precioUnitario,
    precioCambio: precioCambio,
    cantidad: datos.cantidad,
    total: total,
    errores: errores,
    opcionesNoDisponibles: opcionesNoDisponibles
  };
}

module.exports = {
  listarCategorias: listarCategorias,
  listarProductos: listarProductos,
  obtenerFicha: obtenerFicha,
  obtenerPersonalizacion: obtenerPersonalizacion,
  listarPlantillas: listarPlantillas,
  cotizar: cotizar
};
