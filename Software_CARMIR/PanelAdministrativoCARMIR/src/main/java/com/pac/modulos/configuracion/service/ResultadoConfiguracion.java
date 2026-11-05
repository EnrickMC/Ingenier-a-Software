package com.pac.modulos.configuracion.service;

/**
 * Respuesta de los servicios de Configuración al guardar, agregar o eliminar.
 * Dice si salió bien, el mensaje para mostrar y, si hubo error, qué campo marcar.
 */
public class ResultadoConfiguracion {

    // Campos que el formulario puede resaltar en rojo (además de las claves de ClavesConfiguracion)
    public static final String CAMPO_CODIGO_POSTAL = "codigoPostal";
    public static final String CAMPO_COSTO = "costo";
    public static final String CAMPO_CONEXION = "conexion";

    private boolean exitoso;
    private String mensaje;
    private String campo;

    private ResultadoConfiguracion(boolean exitoso, String mensaje, String campo) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
        this.campo = campo;
    }

    public static ResultadoConfiguracion correcto(String mensaje) {
        return new ResultadoConfiguracion(true, mensaje, null);
    }

    public static ResultadoConfiguracion error(String mensaje, String campo) {
        return new ResultadoConfiguracion(false, mensaje, campo);
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCampo() {
        return campo;
    }
}
