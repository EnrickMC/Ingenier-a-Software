/**
 * Apartado Configuración del panel (cierre del proyecto, Prompts 8.1 y 8.2 del docx).
 * Parámetros del negocio en la tabla Configuracion (antes fijos en config/app.properties),
 * zona de cobertura de envío (CodigoPostalCobertura), historial de cambios (CambioConfiguracion)
 * y respaldos de la base de datos (RNF-8).
 *
 *   model/       Parametro, CambioConfiguracion, CodigoPostalCobertura, ClavesConfiguracion
 *   dao/         ConfiguracionDAO, CoberturaDAO (y dao/mysql/ con su implementación)
 *   service/     ConfiguracionService, CoberturaService, RespaldoService, PermisosConfiguracion
 *   controller/  controladores de las pantallas FXML (resources/com/pac/view/configuracion/)
 */
package com.pac.modulos.configuracion;
