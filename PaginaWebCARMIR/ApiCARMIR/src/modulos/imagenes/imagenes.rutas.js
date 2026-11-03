/*
 * Rutas de Imágenes (públicas, solo lectura). Se montan en /api/imagenes desde app.js.
 */
const express = require('express');
const controlador = require('./imagenes.controlador');

const rutas = express.Router();

rutas.get('/postre/:id', controlador.fotoPostre);
rutas.get('/opcion/:id', controlador.imagenOpcion);

module.exports = rutas;
