package com.pac.modulos.inventario.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.Lote;

import java.sql.Connection;
import java.util.List;

/**
 * Operaciones sobre la tabla Lote.
 * Los métodos que reciben una Connection se usan dentro de una transacción
 * (el servicio se encarga del commit o del rollback).
 */
public interface LoteDAO {

    // Lotes de un insumo en orden PEPS (primero el que caduca antes)
    List<Lote> listarPorInsumo(int idInsumo, boolean soloConExistencia) throws DAOException;

    // Lotes con existencia de todos los insumos activos, en orden de caducidad
    List<Lote> listarConExistencia() throws DAOException;

    // Busca un lote por su id; devuelve null si no existe
    Lote buscarPorId(int idLote) throws DAOException;

    // ----- Dentro de una transacción -----

    // Lotes con existencia del insumo en orden PEPS, bloqueados hasta el commit (FOR UPDATE)
    List<Lote> bloquearParaDescuento(Connection conexion, int idInsumo) throws DAOException;

    // Suma de la existencia de los lotes del insumo
    double sumarExistencia(Connection conexion, int idInsumo) throws DAOException;

    // Guarda un lote nuevo y devuelve su id
    int insertar(Connection conexion, Lote lote) throws DAOException;

    // Cambia lo que queda del lote
    void actualizarExistencia(Connection conexion, int idLote, double existencia) throws DAOException;
}
