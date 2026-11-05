package com.pac.modulos.reportes.service;

import java.time.LocalDate;

/**
 * Resultado de revisar el periodo de un reporte (paso 3 de RF-65 y RF-66).
 * Si es correcto trae las fechas que se van a usar (la de fin puede venir ajustada a hoy)
 * y, si se ajustó, el aviso para mostrar. Si no, trae el mensaje y el campo que se marca.
 */
public class RevisionPeriodo {

    // Campos que el controlador marca en rojo
    public static final String CAMPO_DESDE = "desde";
    public static final String CAMPO_HASTA = "hasta";
    public static final String CAMPO_AMBOS = "ambos";

    private boolean correcto;
    private String mensaje;
    private String campo;
    private LocalDate desde;
    private LocalDate hasta;
    private String aviso;      // por ejemplo, que la fecha de fin se ajustó a hoy

    private RevisionPeriodo() {
    }

    // El periodo se puede usar
    public static RevisionPeriodo correcto(LocalDate desde, LocalDate hasta, String aviso) {
        RevisionPeriodo revision = new RevisionPeriodo();
        revision.correcto = true;
        revision.desde = desde;
        revision.hasta = hasta;
        revision.aviso = aviso;
        return revision;
    }

    // El periodo tiene un error; campo dice qué fecha marcar
    public static RevisionPeriodo error(String mensaje, String campo) {
        RevisionPeriodo revision = new RevisionPeriodo();
        revision.correcto = false;
        revision.mensaje = mensaje;
        revision.campo = campo;
        return revision;
    }

    public boolean isCorrecto() {
        return correcto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCampo() {
        return campo;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public String getAviso() {
        return aviso;
    }
}
