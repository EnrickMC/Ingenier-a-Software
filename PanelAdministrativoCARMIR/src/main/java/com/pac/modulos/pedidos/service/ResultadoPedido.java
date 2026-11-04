package com.pac.modulos.pedidos.service;

import com.pac.modulos.pedidos.model.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de los servicios de pedidos: si salió bien, el mensaje para mostrar
 * y qué debe hacer la pantalla después (recargar, ofrecer registrar un pago, etc.).
 */
public class ResultadoPedido {

    // Campos que los formularios pueden resaltar en rojo
    public static final String CAMPO_MOTIVO = "motivo";
    public static final String CAMPO_DETALLE = "detalle";
    public static final String CAMPO_ANTICIPO = "anticipo";
    public static final String CAMPO_REEMBOLSO = "reembolso";
    public static final String CAMPO_MONTO = "monto";
    public static final String CAMPO_FECHA = "fecha";
    public static final String CAMPO_HORARIO = "horario";
    public static final String CAMPO_METODO = "metodo";
    public static final String CAMPO_REFERENCIA = "referencia";
    public static final String CAMPO_COMPROBANTE = "comprobante";
    public static final String CAMPO_NOMBRE = "nombre";
    public static final String CAMPO_TELEFONO = "telefono";
    public static final String CAMPO_CORREO = "correo";
    public static final String CAMPO_DIRECCION = "direccion";
    public static final String CAMPO_PRODUCTOS = "productos";
    public static final String CAMPO_CONEXION = "conexion";

    private boolean exitoso;
    private String mensaje;
    private String campo;                    // campo con error (o null)
    private boolean recargar;                // el pedido ya no existe o cambió: recargar la lista
    private boolean ofrecerPago;             // ofrecer "Registrar pago"
    private boolean ofrecerRevision;         // ofrecer abrir la revisión del pedido
    private boolean preguntarSinReceta;      // RF-63: hay opciones sin receta, preguntar si se continúa
    private Pedido pedido;                   // pedido leído (en las revisiones previas)
    private List<String> avisos = new ArrayList<String>();  // mensajes extra después del éxito

    private ResultadoPedido(boolean exitoso, String mensaje) {
        this.exitoso = exitoso;
        this.mensaje = mensaje;
    }

    public static ResultadoPedido correcto(String mensaje) {
        return new ResultadoPedido(true, mensaje);
    }

    public static ResultadoPedido error(String mensaje, String campo) {
        ResultadoPedido resultado = new ResultadoPedido(false, mensaje);
        resultado.campo = campo;
        return resultado;
    }

    // El pedido no existe o lo cambió otra persona: la pantalla debe recargar
    public static ResultadoPedido recargar(String mensaje) {
        ResultadoPedido resultado = new ResultadoPedido(false, mensaje);
        resultado.recargar = true;
        return resultado;
    }

    // true si el error es de conexión con la base de datos
    public boolean esErrorConexion() {
        return CAMPO_CONEXION.equals(campo);
    }

    public void agregarAviso(String aviso) {
        if (aviso != null && !aviso.isBlank()) {
            avisos.add(aviso);
        }
    }

    // Mensaje con los avisos debajo
    public String getMensajeCompleto() {
        String texto = mensaje;
        for (String aviso : avisos) {
            texto = texto + "\n\n" + aviso;
        }
        return texto;
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

    public boolean isRecargar() {
        return recargar;
    }

    public boolean isOfrecerPago() {
        return ofrecerPago;
    }

    public void setOfrecerPago(boolean ofrecerPago) {
        this.ofrecerPago = ofrecerPago;
    }

    public boolean isOfrecerRevision() {
        return ofrecerRevision;
    }

    public void setOfrecerRevision(boolean ofrecerRevision) {
        this.ofrecerRevision = ofrecerRevision;
    }

    public boolean isPreguntarSinReceta() {
        return preguntarSinReceta;
    }

    public void setPreguntarSinReceta(boolean preguntarSinReceta) {
        this.preguntarSinReceta = preguntarSinReceta;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public List<String> getAvisos() {
        return avisos;
    }
}
