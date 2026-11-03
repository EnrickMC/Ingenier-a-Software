-- =====================================================================
-- CARMIR · PROPUESTA de migración para el Módulo 3 (NO aplicada)
-- Este archivo NO se ejecuta solo y el panel funciona sin él.
-- Revísenlo en equipo; si lo aceptan, renómbrenlo a V2__catalogo.sql
-- y ejecútenlo después de CARMIR_BaseDeDatos.sql.
--
-- Huecos que se encontraron al construir el catálogo:
--  1. RF-4 pide "plantillas" de postres (configuraciones base que el
--     cliente puede ajustar). La tabla Postre no distingue una plantilla
--     de un producto normal  → columna EsPlantilla.
--  2. Caracteristica no tiene un estado propio; hoy el panel usa la
--     columna Disponible como "activa / inactiva" (no hace falta cambio).
--  3. No hay columna SKU ni precio base: el panel muestra el SKU como
--     PST-0001 (a partir de IdPostre) y el precio sale de la vista
--     vw_CostoPrecioPostre (no hace falta cambio).
-- =====================================================================
USE CARMIR_DB;

ALTER TABLE Postre
  ADD COLUMN EsPlantilla TINYINT(1) NOT NULL DEFAULT 0
    COMMENT 'Si el postre es una plantilla base que el cliente puede personalizar (RF-4)'
    AFTER MostrarEnTienda;
