package com.pac.session;

import com.pac.modulos.usuarios.model.Usuario;

/**
 * PATRÓN OBSERVER (observador).
 * Las clases que quieran enterarse cuando alguien entra o sale del panel
 * implementan esta interfaz y se registran en SesionUsuario.
 */
public interface SesionObserver {

    // Se llama cuando un usuario inicia sesión
    void sesionIniciada(Usuario usuario, TipoAcceso tipoAcceso);

    // Se llama cuando se cierra la sesión
    void sesionCerrada();
}
