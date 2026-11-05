package com.pac.modulos.usuarios.service;

import com.pac.modulos.usuarios.model.Usuario;

/**
 * Respuesta de UsuarioService al registrar, modificar o dar de baja un usuario.
 * Dice si salió bien, el mensaje para mostrar y, si hubo error, qué campo marcar.
 */
public class ResultadoOperacion {

    // Nombres de los campos que el formulario puede resaltar en rojo
    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_USUARIO = "usuario";
    public static final String CAMPO_CONTRASENA = "contrasena";
    public static final String CAMPO_PERMISOS = "permisos";
    public static final String CAMPO_PERMISO_PROPIO = "permisoPropio";
    public static final String CAMPO_ESTADO = "estado";
    public static final String CAMPO_CONEXION = "conexion";

    private boolean exitoso;
    private String mensaje;
    private String campo;                 // campo con error (puede ser null)
    private boolean ofrecerReactivar;     // el usuario existe pero está dado de baja
    private int idUsuarioExistente;       // id de la cuenta que se puede reactivar
    private boolean recargarLista;        // el usuario ya no existe: hay que recargar la tabla
    private Usuario usuario;              // usuario leído de la base (por ejemplo, para confirmar la baja)

    private ResultadoOperacion(boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    // La operación salió bien
    public static ResultadoOperacion correcto(String mensaje) {
        return new ResultadoOperacion(true, mensaje);
    }

    // La operación salió bien y se devuelve el usuario leído
    public static ResultadoOperacion correcto(Usuario usuario) {
        ResultadoOperacion resultado = new ResultadoOperacion(true, "");
        resultado.usuario = usuario;
        return resultado;
    }

    // Hubo un error; campo indica qué casilla del formulario marcar (o null)
    public static ResultadoOperacion error(String mensaje, String campo) {
        ResultadoOperacion resultado = new ResultadoOperacion(false, mensaje);
        resultado.campo = campo;
        return resultado;
    }

    // El usuario ya no existe en la base de datos
    public static ResultadoOperacion noEncontrado(String mensaje) {
        ResultadoOperacion resultado = new ResultadoOperacion(false, mensaje);
        resultado.recargarLista = true;
        return resultado;
    }

    // El nombre de usuario pertenece a una cuenta dada de baja que se puede reactivar
    public static ResultadoOperacion reactivacion(int idUsuarioExistente, String mensaje) {
        ResultadoOperacion resultado = new ResultadoOperacion(false, mensaje);
        resultado.ofrecerReactivar = true;
        resultado.idUsuarioExistente = idUsuarioExistente;
        resultado.campo = CAMPO_USUARIO;
        return resultado;
    }

    // ----- Getters -----

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCampo() {
        return campo;
    }

    public boolean isOfrecerReactivar() {
        return ofrecerReactivar;
    }

    public int getIdUsuarioExistente() {
        return idUsuarioExistente;
    }

    public boolean isRecargarLista() {
        return recargarLista;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
