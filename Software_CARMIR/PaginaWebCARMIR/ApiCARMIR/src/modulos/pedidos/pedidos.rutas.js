/*
 * Rutas del Portal de Pedidos (Módulo 2). Todas son públicas (el cliente no inicia sesión).
 * app.js las monta así:
 *   /api/carrito  → validar el carrito
 *   /api/entrega  → reglas de entrega y cobertura de envío
 *   /api/pedidos  → registrar, consultar y enviar comprobante
 * Cada ruta que escribe o que busca con datos personales tiene su propio límite de peticiones.
 */
const express = require('express');
const controlador = require('./pedidos.controlador');
const seguridad = require('../../middlewares/seguridad');

const carrito = express.Router();
carrito.post('/validar', seguridad.limiteValidarCarrito, controlador.validarCarrito);

const entrega = express.Router();
entrega.get('/reglas', controlador.leerReglas);
entrega.get('/cobertura/:cp', seguridad.limiteCobertura, controlador.revisarCobertura);

const pedidos = express.Router();
pedidos.post('/', seguridad.limiteRegistrarPedido, controlador.registrarPedido);
pedidos.post('/consulta', seguridad.limiteConsultaPedido, controlador.consultarPedido);
pedidos.post('/comprobante', seguridad.limiteConsultaPedido, seguridad.limiteComprobante, controlador.registrarComprobante);

module.exports = {
  carrito: carrito,
  entrega: entrega,
  pedidos: pedidos
};
