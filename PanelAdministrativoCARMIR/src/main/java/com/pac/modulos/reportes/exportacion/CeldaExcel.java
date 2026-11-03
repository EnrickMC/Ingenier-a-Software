package com.pac.modulos.reportes.exportacion;

import java.time.LocalDate;

/**
 * Una celda de una hoja de Excel: texto, número, moneda, porcentaje o fecha,
 * con un estilo (normal, encabezado, título o total).
 */
public class CeldaExcel {

    // Tipos de dato
    public static final int TEXTO = 0;
    public static final int NUMERO = 1;
    public static final int MONEDA = 2;
    public static final int PORCENTAJE = 3;
    public static final int FECHA = 4;

    // Estilos de letra
    public static final int NORMAL = 0;
    public static final int ENCABEZADO = 1;
    public static final int TITULO = 2;
    public static final int TOTAL = 3;

    private int tipo;
    private int estilo;
    private String texto;
    private double numero;
    private LocalDate fecha;

    private CeldaExcel(int tipo, int estilo) {
        this.tipo = tipo;
        this.estilo = estilo;
    }

    // ----- Formas de crear una celda -----

    public static CeldaExcel texto(String texto) {
        CeldaExcel celda = new CeldaExcel(TEXTO, NORMAL);
        celda.texto = texto;
        return celda;
    }

    public static CeldaExcel encabezado(String texto) {
        CeldaExcel celda = new CeldaExcel(TEXTO, ENCABEZADO);
        celda.texto = texto;
        return celda;
    }

    public static CeldaExcel titulo(String texto) {
        CeldaExcel celda = new CeldaExcel(TEXTO, TITULO);
        celda.texto = texto;
        return celda;
    }

    public static CeldaExcel textoTotal(String texto) {
        CeldaExcel celda = new CeldaExcel(TEXTO, TOTAL);
        celda.texto = texto;
        return celda;
    }

    public static CeldaExcel numero(double numero) {
        CeldaExcel celda = new CeldaExcel(NUMERO, NORMAL);
        celda.numero = numero;
        return celda;
    }

    public static CeldaExcel moneda(double cantidad) {
        CeldaExcel celda = new CeldaExcel(MONEDA, NORMAL);
        celda.numero = cantidad;
        return celda;
    }

    public static CeldaExcel monedaTotal(double cantidad) {
        CeldaExcel celda = new CeldaExcel(MONEDA, TOTAL);
        celda.numero = cantidad;
        return celda;
    }

    // porcentaje de 0 a 100 (por ejemplo 35.5 = 35.5 %)
    public static CeldaExcel porcentaje(double porcentaje) {
        CeldaExcel celda = new CeldaExcel(PORCENTAJE, NORMAL);
        celda.numero = porcentaje / 100;
        return celda;
    }

    public static CeldaExcel fecha(LocalDate fecha) {
        CeldaExcel celda = new CeldaExcel(FECHA, NORMAL);
        celda.fecha = fecha;
        return celda;
    }

    // ----- Getters -----

    public int getTipo() {
        return tipo;
    }

    public int getEstilo() {
        return estilo;
    }

    public String getTexto() {
        if (texto == null) {
            return "";
        }
        return texto;
    }

    public double getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
