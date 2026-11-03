/*
 * Crea la aplicación Express con sus middlewares y rutas.
 * Está separada de servidor.js para poder probarla sin abrir un puerto.
 *
 * Orden de los middlewares (importa):
 *  1. Seguridad (cabeceras, CORS y límite de peticiones)
 *  2. Lectura del cuerpo JSON (máximo 32 KB: un carrito de 20 postres cabe de sobra).
 *     Excepción: el envío del comprobante admite hasta 8 MB porque puede traer la foto o PDF (máx. 5 MB)
 *     escrita en Base64 (ocupa un tercio más).
 *  3. Rutas de cada módulo
 *  4. Ruta no encontrada y manejo de errores (siempre al final)
 */
const express = require('express');
const seguridad = require('./middlewares/seguridad');
const manejoErrores = require('./middlewares/manejoErrores');
const rutasSalud = require('./modulos/salud/salud.rutas');
const rutasCatalogo = require('./modulos/catalogo/catalogo.rutas');
const rutasPedidos = require('./modulos/pedidos/pedidos.rutas');
const rutasImagenes = require('./modulos/imagenes/imagenes.rutas');

function crearApp() {
  const app = express();

  // No anunciar que el servidor usa Express
  app.disable('x-powered-by');

  app.use(seguridad.cabecerasSeguras);
  app.use(seguridad.cors);
  app.use(seguridad.limiteGeneral);
  // RF-12: solo esta ruta acepta cuerpos grandes (foto del comprobante). Va antes del lector general:
  // cuando un lector ya leyó el cuerpo, el siguiente lo salta.
  app.use('/api/pedidos/comprobante', express.json({ limit: '8mb' }));
  app.use(express.json({ limit: '32kb' }));

  // Módulos de la API
  app.use('/api/salud', rutasSalud);
  app.use('/api/catalogo', rutasCatalogo);
  // Fotos elegidas como archivo de la computadora en el Panel
  app.use('/api/imagenes', rutasImagenes);
  // Módulo 2 · Portal de Pedidos
  app.use('/api/carrito', rutasPedidos.carrito);
  app.use('/api/entrega', rutasPedidos.entrega);
  app.use('/api/pedidos', rutasPedidos.pedidos);

  app.use(manejoErrores.rutaNoEncontrada);
  app.use(manejoErrores.manejarErrores);

  return app;
}

module.exports = crearApp;
