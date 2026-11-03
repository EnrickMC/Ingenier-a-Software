/*
 * Rutas del Catálogo Público (todas públicas, sin inicio de sesión, solo lectura).
 * Se montan en /api/catalogo desde app.js.
 */
const express = require('express');
const controlador = require('./catalogo.controlador');
const seguridad = require('../../middlewares/seguridad');

const rutas = express.Router();

rutas.get('/categorias', controlador.listarCategorias);
rutas.get('/productos', controlador.listarProductos);
rutas.get('/productos/:id', controlador.obtenerFicha);
rutas.get('/productos/:id/personalizacion', controlador.obtenerPersonalizacion);
rutas.get('/productos/:id/plantillas', controlador.listarPlantillas);
rutas.post('/cotizar', seguridad.limiteCotizar, controlador.cotizar);

module.exports = rutas;
