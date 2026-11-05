/**
 * Módulo 6 · Reportes Financieros.
 * RF-65 Reporte de ingresos y gastos, RF-66 Productos más vendidos y RF-67 Exportación a Excel.
 *
 *   model/        datos de cada reporte (totales, renglones por día y por producto)
 *   dao/          interfaz de consultas (y dao/mysql/ con las consultas a MySQL)
 *   service/      reglas: revisión del periodo, permisos, avisos y agrupación de la gráfica
 *   exportacion/  arma y guarda el archivo .xlsx (o .csv) sin librerías externas
 *   controller/   controladores de las pantallas FXML
 * Las vistas van en resources/com/pac/view/reportes/
 */
package com.pac.modulos.reportes;
