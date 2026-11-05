/*
 * GET /api/salud: dice si la API está viva, si la base de datos responde
 * y cuántos productos se ven en el catálogo.
 * Sirve para revisar rápido la conexión desde el navegador: http://localhost:3000/api/salud
 * Para un diagnóstico completo con instrucciones: npm run diagnostico
 */
const express = require('express');
const baseDatos = require('../../config/baseDatos');
const catalogoRepositorio = require('../catalogo/catalogo.repositorio');

const rutas = express.Router();

async function revisarSalud(peticion, respuesta) {
  try {
    await baseDatos.probarConexion();
  } catch (error) {
    // No se manda el detalle del error: solo se escribe en la consola del servidor
    console.error('[salud] La base de datos no responde: ' + error.code);
    respuesta.status(503).json({
      estado: 'error',
      baseDatos: 'sin conexión',
      sugerencia: 'Ejecuta npm run diagnostico en la carpeta ApiCARMIR'
    });
    return;
  }

  try {
    const productosVisibles = await catalogoRepositorio.contarProductosVisibles();
    respuesta.json({ estado: 'ok', baseDatos: 'conectada', productosVisibles: productosVisibles });
  } catch (error) {
    // Conecta, pero faltan tablas/vistas o permisos
    console.error('[salud] No se pudo consultar el catálogo: ' + error.code);
    respuesta.status(500).json({
      estado: 'error',
      baseDatos: 'conectada',
      catalogo: 'no se pudo consultar',
      sugerencia: 'Ejecuta npm run diagnostico en la carpeta ApiCARMIR'
    });
  }
}

rutas.get('/', revisarSalud);

module.exports = rutas;
