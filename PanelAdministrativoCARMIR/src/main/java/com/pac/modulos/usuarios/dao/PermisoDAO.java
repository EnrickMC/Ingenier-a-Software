package com.pac.modulos.usuarios.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.usuarios.model.Permiso;

import java.util.List;

/**
 * Operaciones disponibles sobre las tablas Permiso y UsuarioPermiso.
 * La clase PermisoDAOMySQL es la que las implementa.
 */
public interface PermisoDAO {

    // Devuelve los permisos asignados a un usuario
    List<Permiso> listarPorUsuario(int idUsuario) throws DAOException;

    // Devuelve todos los permisos que existen
    List<Permiso> listarTodos() throws DAOException;

    // Da de alta los permisos de la lista que todavía no existan en la tabla Permiso
    void asegurarPermisos(String[] nombresPermisos) throws DAOException;
}
