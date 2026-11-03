package com.pac.modulos.inventario.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.Movimiento;
import com.pac.modulos.inventario.model.TipoMovimiento;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Operaciones sobre la tabla MovimientoInventario (historial de entradas y salidas).
 */
public interface MovimientoDAO {

    // Mensaje cuando la Administradora registra un movimiento y falta la migración
    String MSG_FALTA_MIGRACION = "La Administradora no está en la tabla Usuario y MovimientoInventario.IdUsuario"
            + " no acepta vacío. Ejecuta DBCarmir/migraciones/MIGRACION_COMPLETA.sql"
            + " o registra el movimiento con una cuenta del Personal.";

    // Historial filtrado. idInsumo 0, tipo null o fechas null = sin ese filtro. Lo más reciente primero
    List<Movimiento> listar(int idInsumo, TipoMovimiento tipo, LocalDate desde, LocalDate hasta) throws DAOException;

    // Últimos movimientos de un insumo
    List<Movimiento> listarUltimos(int idInsumo, int cantidad) throws DAOException;

    // ----- Dentro de una transacción -----

    // Guarda un movimiento y devuelve su id
    int insertar(Connection conexion, Movimiento movimiento) throws DAOException;

    // true si al pedido ya se le descontaron insumos (movimientos tipo Produccion con su folio)
    boolean existeProduccionDeFolio(Connection conexion, int folio) throws DAOException;
}
