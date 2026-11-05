package com.pac.modulos.usuarios.service;

import com.pac.modulos.usuarios.model.Usuario;
import com.pac.session.TipoAcceso;

/**
 * Respuesta del servicio de inicio de sesión.
 * Indica si se pudo entrar, el mensaje de error (si lo hay) y el usuario que entró.
 */
public class ResultadoAutenticacion {

    private boolean exitoso;
    private String mensaje;
    private Usuario usuario;
    private TipoAcceso tipoAcceso;   // solo cuando entró (Administradora o Personal)

    public ResultadoAutenticacion(boolean exitoso, String mensaje, Usuario usuario) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
        this.usuario = usuario;
    }

    // Copia del resultado con la forma de acceso (la usa el inicio de sesión unificado)
    public ResultadoAutenticacion conTipoAcceso(TipoAcceso tipo) {
        ResultadoAutenticacion copia = new ResultadoAutenticacion(exitoso, mensaje, usuario);
        copia.tipoAcceso = tipo;
        return copia;
    }

    public TipoAcceso getTipoAcceso() {
        return tipoAcceso;
    }

    // Crea un resultado cuando el usuario sí pudo entrar
    public static ResultadoAutenticacion correcto(Usuario usuario) {
        return new ResultadoAutenticacion(true, "", usuario);
    }

    // Crea un resultado con un mensaje de error
    public static ResultadoAutenticacion error(String mensaje) {
        return new ResultadoAutenticacion(false, mensaje, null);
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
