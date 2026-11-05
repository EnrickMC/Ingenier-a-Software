package com.pac.modulos.inventario.service;

/**
 * Respuesta de los servicios del inventario y las recetas al guardar, registrar o dar de baja.
 * Dice si salió bien, el mensaje para mostrar y, si hubo error, qué campo marcar.
 */
public class ResultadoInventario {

    // Nombres de los campos que los formularios pueden resaltar en rojo
    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_CATEGORIA = "categoria";
    public static final String CAMPO_UNIDAD = "unidad";
    public static final String CAMPO_COSTO = "costo";
    public static final String CAMPO_STOCK = "stock";
    public static final String CAMPO_CANTIDAD = "cantidad";
    public static final String CAMPO_FECHA_COMPRA = "fechaCompra";
    public static final String CAMPO_CADUCIDAD = "caducidad";
    public static final String CAMPO_PROVEEDOR = "proveedor";
    public static final String CAMPO_MOTIVO = "motivo";
    public static final String CAMPO_DETALLE = "detalle";
    public static final String CAMPO_LOTE = "lote";
    public static final String CAMPO_OPCION = "opcion";
    public static final String CAMPO_TIEMPO = "tiempo";
    public static final String CAMPO_PASOS = "pasos";
    public static final String CAMPO_INGREDIENTES = "ingredientes";
    public static final String CAMPO_CONEXION = "conexion";

    private boolean exitoso;
    private String mensaje;
    private String campo;                 // campo con error (puede ser null)
    private boolean recargarLista;        // el registro ya no existe: hay que recargar la tabla
    private boolean ofrecerReactivar;     // existe un registro dado de baja con el mismo nombre
    private boolean ofrecerAbrir;         // ya existe otro registro y se puede consultar
    private int idRelacionado;            // id del registro que se puede reactivar o abrir
    private boolean seEliminara;          // en una baja: true = se borra, false = se inhabilita
    private String aviso;                 // mensaje extra para mostrar después del éxito (o null)
    private boolean ofrecerActualizarCosto;  // en una entrada: el costo del lote es distinto al del insumo
    private double valor;                 // dato numérico de apoyo (nueva existencia, costo del lote...)

    private ResultadoInventario(boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    // La operación salió bien
    public static ResultadoInventario correcto(String mensaje) {
        return new ResultadoInventario(true, mensaje);
    }

    // Hubo un error; campo indica qué casilla del formulario marcar (o null)
    public static ResultadoInventario error(String mensaje, String campo) {
        ResultadoInventario resultado = new ResultadoInventario(false, mensaje);
        resultado.campo = campo;
        return resultado;
    }

    // El registro ya no existe o ya no está disponible
    public static ResultadoInventario noEncontrado(String mensaje) {
        ResultadoInventario resultado = new ResultadoInventario(false, mensaje);
        resultado.recargarLista = true;
        return resultado;
    }

    // Existe un registro dado de baja con ese nombre: se ofrece reactivarlo
    public static ResultadoInventario reactivable(String mensaje, int idRelacionado) {
        ResultadoInventario resultado = new ResultadoInventario(false, mensaje);
        resultado.ofrecerReactivar = true;
        resultado.idRelacionado = idRelacionado;
        return resultado;
    }

    // Ya existe otro registro igual; se ofrece consultarlo
    public static ResultadoInventario existente(String mensaje, int idRelacionado, String campo) {
        ResultadoInventario resultado = new ResultadoInventario(false, mensaje);
        resultado.ofrecerAbrir = true;
        resultado.idRelacionado = idRelacionado;
        resultado.campo = campo;
        return resultado;
    }

    // Revisión previa a una baja: se puede continuar; el mensaje explica las consecuencias
    public static ResultadoInventario bajaPosible(String mensaje, boolean seEliminara) {
        ResultadoInventario resultado = new ResultadoInventario(true, mensaje);
        resultado.seEliminara = seEliminara;
        return resultado;
    }

    // ----- Datos extra -----

    public void setAviso(String aviso) {
        this.aviso = aviso;
    }

    public void setOfrecerActualizarCosto(boolean ofrecerActualizarCosto) {
        this.ofrecerActualizarCosto = ofrecerActualizarCosto;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public void setIdRelacionado(int idRelacionado) {
        this.idRelacionado = idRelacionado;
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

    public boolean isOfrecerReactivar() {
        return ofrecerReactivar;
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

    public String getAviso() {
        return aviso;
    }

    public boolean isOfrecerActualizarCosto() {
        return ofrecerActualizarCosto;
    }

    public double getValor() {
        return valor;
    }
}
