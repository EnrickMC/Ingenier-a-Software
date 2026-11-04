package com.pac.modulos.reportes.controller;

import com.pac.modulos.reportes.exportacion.LibroExcel;

import java.time.LocalDate;

/**
 * Lo que ReportesController necesita de cada pestaña de reporte
 * (Ingresos y gastos, Más vendidos) para generarla y exportarla.
 */
public interface PestanaReporte {

    // Consulta y muestra el reporte del periodo; false si no se pudo generar
    boolean generar(LocalDate desde, LocalDate hasta);

    // true si ya hay un reporte en pantalla
    boolean tieneReporte();

    // true si el reporte en pantalla tiene datos para exportar
    boolean tieneDatos();

    // Periodo del reporte en pantalla (null si no hay)
    LocalDate getDesde();

    LocalDate getHasta();

    // Arma el archivo con la misma información de la pantalla (RF-67)
    LibroExcel armarLibro();

    // Tipo para el nombre del archivo: "IngresosGastos" o "MasVendidos"
    String getTipoArchivo();
}
