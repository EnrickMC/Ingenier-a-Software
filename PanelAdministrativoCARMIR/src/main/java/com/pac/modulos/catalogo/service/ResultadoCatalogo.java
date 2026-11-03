package com.pac.modulos.catalogo.service;

/**
 * Respuesta de los servicios del catálogo al guardar, modificar o dar de baja.
 * Dice si salió bien, el mensaje para mostrar y, si hubo error, qué campo marcar.
 * También puede pedir una confirmación antes de guardar.
 */
public class ResultadoCatalogo {

    // Nombres de los campos que los formularios pueden resaltar en rojo
    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_DESCRIPCION = "descripcion";
    public static final String CAMPO_ESTADO = "estado";
    public static final String CAMPO_CATEGORIA = "categoria";
    public static final String CAMPO_PRESUPUESTO = "presupuesto";
    public static final String CAMPO_ORDEN = "orden";
    public static final String CAMPO_IMAGEN = "imagen";
    public static final String CAMPO_MULT_INGREDIENTES = "multIngredientes";
    public static final String CAMPO_MULT_COSTO = "multCosto";
    public static final String CAMPO_MULT_PRESUPUESTO = "multPresupuesto";
    public static final String CAMPO_PUNTOS = "puntos";
    public static final String CAMPO_OPCION_A = "opcionA";
    public static final String CAMPO_OPCION_B = "opcionB";
    public static final String CAMPO_MOTIVO = "motivo";
    public static final String CAMPO_FOTO = "foto";
    public static final String CAMPO_OPCIONES = "opciones";
    public static final String CAMPO_CONEXION = "conexion";

    private boolean exitoso;
    private String mensaje;
    private String campo;                 // campo con error (puede ser null)
    private boolean recargarLista;        // el registro ya no existe: hay que recargar la tabla
    private boolean pideConfirmacion;     // hay que preguntar antes de guardar
    private boolean ofrecerAbrir;         // ya existe otro registro igual y se puede abrir
    private int idRelacionado;            // id del registro que se puede abrir
    private boolean seEliminara;          // en una baja: true = se borra, false = se inhabilita

    private ResultadoCatalogo(boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    // La operación salió bien
    public static ResultadoCatalogo correcto(String mensaje) {
        return new ResultadoCatalogo(true, mensaje);
    }

    // Hubo un error; campo indica qué casilla del formulario marcar (o null)
    public static ResultadoCatalogo error(String mensaje, String campo) {
        ResultadoCatalogo resultado = new ResultadoCatalogo(false, mensaje);
        resultado.campo = campo;
        return resultado;
    }

    // El registro ya no existe en la base de datos
    public static ResultadoCatalogo noEncontrado(String mensaje) {
        ResultadoCatalogo resultado = new ResultadoCatalogo(false, mensaje);
        resultado.recargarLista = true;
        return resultado;
    }

    // Antes de guardar hay que preguntar a la administradora
    public static ResultadoCatalogo confirmacion(String mensaje, String campo) {
        ResultadoCatalogo resultado = new ResultadoCatalogo(false, mensaje);
        resultado.pideConfirmacion = true;
        resultado.campo = campo;
        return resultado;
    }

    // Ya existe otro registro igual; se ofrece abrirlo
    public static ResultadoCatalogo existente(String mensaje, int idRelacionado, String campo) {
        ResultadoCatalogo resultado = new ResultadoCatalogo(false, mensaje);
        resultado.ofrecerAbrir = true;
        resultado.idRelacionado = idRelacionado;
        resultado.campo = campo;
        return resultado;
    }

    // Revisión previa a una baja: se puede continuar; el mensaje explica las consecuencias
    public static ResultadoCatalogo bajaPosible(String mensaje, boolean seEliminara) {
        ResultadoCatalogo resultado = new ResultadoCatalogo(true, mensaje);
        resultado.seEliminara = seEliminara;
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

    public boolean isRecargarLista() {
        return recargarLista;
    }

    public boolean isPideConfirmacion() {
        return pideConfirmacion;
    }

    public boolean isOfrecerAbrir() {
        return ofrecerAbrir;
    }

    public int getIdRelacionado() {
        return idRelacionado;
    }

    public boolean isSeEliminara() {
        return seEliminara;
    }
}
