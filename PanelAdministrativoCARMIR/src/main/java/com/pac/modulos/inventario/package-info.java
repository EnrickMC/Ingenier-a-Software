/**
 * Módulo 5 · Inventario y Recetas.
 * Apartados "Inventario de Insumos" (insumos, lotes y movimientos, alertas)
 * y "Recetas" del panel. También tiene el descuento automático de insumos
 * que usará el Gestor de Pedidos (RF-63).
 *
 *   model/       clases que representan las tablas (Insumo, Lote, Movimiento, Alerta, Receta, ...)
 *   dao/         interfaces de acceso a datos (y dao/mysql/ con su implementación JDBC)
 *   service/     reglas del negocio: InsumoService, InventarioService (entradas, mermas, PEPS),
 *                AlertaService y MonitorAlertas, RecetaService, DescuentoInsumosService
 *   controller/  controladores de las pestañas y formularios FXML
 * Las vistas están en resources/com/pac/view/inventario/
 */
package com.pac.modulos.inventario;
