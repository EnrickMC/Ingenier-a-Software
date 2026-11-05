package com.pac.modulos.usuarios.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Usuario;

import java.util.List;

/**
 * Operaciones disponibles sobre la tabla Usuario (Módulo 7: Gestión de Usuarios).
 * La clase UsuarioDAOMySQL es la que las implementa.
 */
public interface UsuarioDAO {

    // Busca un usuario por su nombre de inicio de sesión; devuelve null si no existe
    Usuario buscarPorNombreUsuario(String nombreUsuario) throws DAOException;

    // Busca un usuario por su id; devuelve null si no existe
    Usuario buscarPorId(int idUsuario) throws DAOException;

    // Busca usuarios cuyo nombre o usuario contenga el texto.
    // Texto vacío = todos. Estado null = cualquier estado.
    List<Usuario> buscar(String texto, EstadoUsuario estado) throws DAOException;

    // Guarda un usuario nuevo con sus permisos y devuelve el id que le asignó la base de datos
    int insertar(Usuario usuario) throws DAOException;

    // Actualiza los datos y permisos de un usuario.
    // Si cambiarContrasena es false, la contraseña guardada no se toca.
    void actualizar(Usuario usuario, boolean cambiarContrasena) throws DAOException;

    // Baja lógica: pone el usuario como Inactivo y le quita sus permisos
    void darDeBaja(int idUsuario) throws DAOException;

    // Cambia solo el estado del usuario (por ejemplo, a Bloqueado)
    void cambiarEstado(int idUsuario, EstadoUsuario estado) throws DAOException;

    // Guarda la fecha del último acceso y reinicia los intentos fallidos
    void registrarAccesoExitoso(int idUsuario) throws DAOException;

    // Suma un intento fallido de inicio de sesión
    void registrarIntentoFallido(int idUsuario) throws DAOException;
}
