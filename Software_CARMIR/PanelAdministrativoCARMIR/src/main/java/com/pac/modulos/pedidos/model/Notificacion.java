package com.pac.modulos.pedidos.model;

import java.time.LocalDateTime;

/**
 * Un aviso enviado (o por enviar) al cliente (tabla Notificacion).
 */
public class Notificacion {

    // Canales (ENUM Notificacion.Canal)
    public static final String CANAL_WHATSAPP = "WhatsApp";
    public static final String CANAL_CORREO = "Correo";

    // Resultados (ENUM Notificacion.ResultadoEnvio; "Pendiente" lo agrega la migración V4)
    public static final String ENVIADO = "Enviado";
    public static final String FALLIDO = "Fallido";
    public static final String PENDIENTE = "Pendiente";

    private int idNotificacion;
    private int folio;
    private String tipoEvento;
    private String canal;
    private String destino;
    private String mensaje;
    private LocalDateTime fecha;
    private String resultado;
    private String motivoFallo;

    public boolean isFallida() {
        return FALLIDO.equals(resultado);
    }

    public int getIdNotificacion() {
        return idNotificacion;
    }

    public void setIdNotificacion(int idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public int getFolio() {
        return folio;
    }

    public void setFolio(int folio) {
        this.folio = folio;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void setMotivoFallo(String motivoFallo) {
        this.motivoFallo = motivoFallo;
    }
}
