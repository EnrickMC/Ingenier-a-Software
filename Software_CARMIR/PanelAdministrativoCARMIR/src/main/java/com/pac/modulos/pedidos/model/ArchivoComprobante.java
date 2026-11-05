package com.pac.modulos.pedidos.model;

import java.time.LocalDateTime;

/**
 * Foto o PDF del comprobante que el cliente subió desde la página web (tabla ComprobanteArchivo).
 * RF-14 lo guarda y RF-50 lo muestra en el panel.
 */
public class ArchivoComprobante {

    public static final String TIPO_JPG = "image/jpeg";
    public static final String TIPO_PNG = "image/png";
    public static final String TIPO_WEBP = "image/webp";
    public static final String TIPO_PDF = "application/pdf";

    private int idPago;
    private String nombreArchivo;
    private String tipoContenido;
    private int tamanoBytes;
    private byte[] contenido;
    private LocalDateTime fechaSubida;

    public boolean esPdf() {
        return TIPO_PDF.equals(tipoContenido);
    }

    // Extensión para guardar una copia en el equipo (.jpg, .png, .webp o .pdf)
    public String getExtension() {
        if (TIPO_PNG.equals(tipoContenido)) {
            return ".png";
        }
        if (TIPO_WEBP.equals(tipoContenido)) {
            return ".webp";
        }
        if (TIPO_PDF.equals(tipoContenido)) {
            return ".pdf";
        }
        return ".jpg";
    }

    // Tipo corto para mostrar: JPG, PNG, WEBP o PDF
    public String getTipoCorto() {
        return getExtension().substring(1).toUpperCase();
    }

    public int getIdPago() {
        return idPago;
    }

    public void setIdPago(int idPago) {
        this.idPago = idPago;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public void setTipoContenido(String tipoContenido) {
        this.tipoContenido = tipoContenido;
    }

    public int getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(int tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}
