package com.pac.modulos.configuracion.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.configuracion.model.Parametro;

import java.sql.Connection;
import java.util.List;

/**
 * Acceso a las tablas Configuracion y CambioConfiguracion.
 * Los métodos que reciben una conexión se usan dentro de una transacción.
 */
public interface ConfiguracionDAO {

    String MSG_FALTA_MIGRACION = "La tabla Configuracion no existe: ejecuta DBCarmir/migraciones/MIGRACION_COMPLETA.sql";

    // Todos los parámetros guardados
    List<Parametro> listar() throws DAOException;

    // Cambia el valor de un parámetro (lo crea si no existe)
    void guardar(Connection conexion, Parametro parametro) throws DAOException;

    // Anota un cambio en la bitácora
    void registrarCambio(Connection conexion, CambioConfiguracion cambio) throws DAOException;

    // Últimos cambios, del más reciente al más antiguo
    List<CambioConfiguracion> listarCambios(int cantidad) throws DAOException;
}
