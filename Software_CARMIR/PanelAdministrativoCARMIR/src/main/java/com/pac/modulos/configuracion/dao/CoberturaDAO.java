package com.pac.modulos.configuracion.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.configuracion.model.CodigoPostalCobertura;

import java.sql.Connection;
import java.util.List;

/**
 * Acceso a la tabla CodigoPostalCobertura (zona de envío a domicilio).
 */
public interface CoberturaDAO {

    // Códigos postales que empiezan con el texto (vacío = todos), ordenados
    List<CodigoPostalCobertura> buscar(String inicioCodigo) throws DAOException;

    // Busca un código postal; null si no está en la cobertura
    CodigoPostalCobertura buscarPorCodigo(String codigoPostal) throws DAOException;

    void insertar(Connection conexion, CodigoPostalCobertura cobertura) throws DAOException;

    void actualizar(Connection conexion, CodigoPostalCobertura cobertura) throws DAOException;

    void eliminar(Connection conexion, String codigoPostal) throws DAOException;
}
