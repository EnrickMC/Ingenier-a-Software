package com.pac.modulos.catalogo.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.catalogo.model.Exclusion;

import java.util.List;

/**
 * Operaciones sobre la tabla ExclusionOpciones.
 * La clase ExclusionDAOMySQL es la que las implementa.
 */
public interface ExclusionDAO {

    // Exclusiones (activas e inactivas) entre opciones de una categoría
    List<Exclusion> listarPorCategoria(int idCategoria) throws DAOException;

    // Exclusiones en las que participa una opción (de cualquier lado)
    List<Exclusion> listarPorOpcion(int idOpcion) throws DAOException;

    // Busca una exclusión por su id; devuelve null si no existe
    Exclusion buscarPorId(int idExclusion) throws DAOException;

    // Busca la regla entre dos opciones en cualquiera de los dos sentidos; null si no existe
    Exclusion buscarPar(int idOpcion, int idOpcionExcluida) throws DAOException;

    // Exclusiones activas en las que las dos opciones están en la lista
    List<Exclusion> listarActivasEntre(List<Integer> idsOpcion) throws DAOException;

    // Guarda una exclusión nueva y devuelve su id
    int insertar(Exclusion exclusion) throws DAOException;

    // Activa o inactiva una exclusión
    void cambiarEstado(int idExclusion, EstadoRegistro estado) throws DAOException;

    // Borra la exclusión
    void eliminar(int idExclusion) throws DAOException;
}
