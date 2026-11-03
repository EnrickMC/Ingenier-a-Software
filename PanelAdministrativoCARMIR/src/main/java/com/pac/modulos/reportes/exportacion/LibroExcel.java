package com.pac.modulos.reportes.exportacion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Archivo de Excel (.xlsx) o CSV con una o varias hojas (RF-48, RNF-9).
 *
 * Un archivo .xlsx es un ZIP con varios archivos XML adentro (formato Office Open XML).
 * Esta clase escribe esos XML directamente con java.util.zip, así no hace falta
 * agregar librerías al proyecto. El archivo abre en Excel, LibreOffice y Google Sheets.
 *
 * Estilos que se usan (índices de "cellXfs" en styles.xml):
 *   0 normal · 1 encabezado (negrita con fondo pastel) · 2 moneda · 3 fecha · 4 porcentaje
 *   5 título · 6 número · 7 moneda en negrita (totales) · 8 texto en negrita (totales)
 */
public class LibroExcel {

    // Ancho mínimo y máximo de una columna (en caracteres)
    private static final int ANCHO_MINIMO = 8;
    private static final int ANCHO_MAXIMO = 60;

    // Excel cuenta las fechas como días desde el 30/12/1899
    private static final LocalDate INICIO_FECHAS_EXCEL = LocalDate.of(1899, 12, 30);
    private static final DateTimeFormatter FECHA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<HojaExcel> hojas = new ArrayList<HojaExcel>();

    // Crea una hoja nueva al final del libro
    public HojaExcel agregarHoja(String nombre) {
        HojaExcel hoja = new HojaExcel(nombre);
        hojas.add(hoja);
        return hoja;
    }

    public List<HojaExcel> getHojas() {
        return hojas;
    }

    // ===================== Excel (.xlsx) =====================

    // Escribe el libro como archivo .xlsx
    public void guardarXlsx(File archivo) throws IOException {
        FileOutputStream salida = new FileOutputStream(archivo);
        ZipOutputStream zip = new ZipOutputStream(salida);
        try {
            agregarArchivo(zip, "[Content_Types].xml", tiposDeContenido());
            agregarArchivo(zip, "_rels/.rels", relacionesPrincipales());
            agregarArchivo(zip, "xl/workbook.xml", libro());
            agregarArchivo(zip, "xl/_rels/workbook.xml.rels", relacionesDelLibro());
            agregarArchivo(zip, "xl/styles.xml", estilos());
            for (int i = 0; i < hojas.size(); i++) {
                agregarArchivo(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", hoja(hojas.get(i)));
            }
        } finally {
            zip.close();
        }
    }

    private void agregarArchivo(ZipOutputStream zip, String ruta, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String tiposDeContenido() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        xml.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        xml.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        xml.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        xml.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 0; i < hojas.size(); i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1)
               .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        xml.append("</Types>");
        return xml.toString();
    }

    private String relacionesPrincipales() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String libro() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ");
        xml.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < hojas.size(); i++) {
            xml.append("<sheet name=\"").append(escapar(hojas.get(i).getNombre())).append("\" sheetId=\"")
               .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        xml.append("</sheets></workbook>");
        return xml.toString();
    }

    private String relacionesDelLibro() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < hojas.size(); i++) {
            xml.append("<Relationship Id=\"rId").append(i + 1)
               .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
               .append(i + 1).append(".xml\"/>");
        }
        // La hoja de estilos va después de las hojas
        xml.append("<Relationship Id=\"rId").append(hojas.size() + 1)
           .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        xml.append("</Relationships>");
        return xml.toString();
    }

    // Formatos de número, letras, fondos y la lista de estilos (ver comentario de la clase)
    private String estilos() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"2\">"
                + "<numFmt numFmtId=\"164\" formatCode=\"&quot;$&quot;#,##0.00\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"dd/mm/yyyy\"/>"
                + "</numFmts>"
                + "<fonts count=\"3\">"
                + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FF2C3E50\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"14\"/><color rgb=\"FF2C3E50\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"3\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                // Fondo rosa pastel de los encabezados (RNF-10)
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF8D7E3\"/><bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"2\">"
                + "<border><left/><right/><top/><bottom/><diagonal/></border>"
                + "<border><left/><right/><top/><bottom style=\"thin\"><color rgb=\"FFD4A373\"/></bottom><diagonal/></border>"
                + "</borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"9\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
                + "</cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "</styleSheet>";
    }

    // XML de una hoja: anchos de columna y filas con sus celdas
    private String hoja(HojaExcel hoja) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

        // Columnas "autoajustadas": el ancho sale del texto más largo de cada columna
        int columnas = hoja.getNumeroColumnas();
        if (columnas > 0) {
            xml.append("<cols>");
            for (int c = 0; c < columnas; c++) {
                xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                   .append("\" width=\"").append(anchoColumna(hoja, c)).append("\" customWidth=\"1\"/>");
            }
            xml.append("</cols>");
        }

        xml.append("<sheetData>");
        List<List<CeldaExcel>> filas = hoja.getFilas();
        for (int f = 0; f < filas.size(); f++) {
            List<CeldaExcel> fila = filas.get(f);
            xml.append("<row r=\"").append(f + 1).append("\">");
            for (int c = 0; c < fila.size(); c++) {
                xml.append(celda(fila.get(c), referencia(c, f + 1)));
            }
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    // XML de una celda según su tipo
    private String celda(CeldaExcel celda, String referencia) {
        int estilo = estiloDe(celda);
        String inicio = "<c r=\"" + referencia + "\" s=\"" + estilo + "\"";
        if (celda.getTipo() == CeldaExcel.TEXTO) {
            // Texto dentro de la celda ("inlineStr"); se conservan los espacios
            return inicio + " t=\"inlineStr\"><is><t xml:space=\"preserve\">" + escapar(celda.getTexto()) + "</t></is></c>";
        }
        if (celda.getTipo() == CeldaExcel.FECHA) {
            if (celda.getFecha() == null) {
                return inicio + "/>";
            }
            long dias = ChronoUnit.DAYS.between(INICIO_FECHAS_EXCEL, celda.getFecha());
            return inicio + "><v>" + dias + "</v></c>";
        }
        return inicio + "><v>" + numeroXml(celda.getNumero()) + "</v></c>";
    }

    // Elige el índice de estilo de styles.xml
    private int estiloDe(CeldaExcel celda) {
        int tipo = celda.getTipo();
        int estilo = celda.getEstilo();
        if (tipo == CeldaExcel.TEXTO) {
            if (estilo == CeldaExcel.ENCABEZADO) {
                return 1;
            }
            if (estilo == CeldaExcel.TITULO) {
                return 5;
            }
            if (estilo == CeldaExcel.TOTAL) {
                return 8;
            }
            return 0;
        }
        if (tipo == CeldaExcel.MONEDA) {
            if (estilo == CeldaExcel.TOTAL) {
                return 7;
            }
            return 2;
        }
        if (tipo == CeldaExcel.FECHA) {
            return 3;
        }
        if (tipo == CeldaExcel.PORCENTAJE) {
            return 4;
        }
        return 6;
    }

    // Ancho de una columna según el contenido más largo (los títulos no cuentan, ocupan varias columnas)
    private int anchoColumna(HojaExcel hoja, int columna) {
        int ancho = ANCHO_MINIMO;
        for (List<CeldaExcel> fila : hoja.getFilas()) {
            if (columna < fila.size()) {
                CeldaExcel celda = fila.get(columna);
                if (celda.getEstilo() != CeldaExcel.TITULO) {
                    int largo = textoCsv(celda).length() + 3;
                    if (largo > ancho) {
                        ancho = largo;
                    }
                }
            }
        }
        if (ancho > ANCHO_MAXIMO) {
            return ANCHO_MAXIMO;
        }
        return ancho;
    }

    // Referencia de Excel de una celda: columna 0, fila 1 → "A1"; columna 27 → "AB"
    private String referencia(int columna, int fila) {
        String letras = "";
        int numero = columna + 1;
        while (numero > 0) {
            int resto = (numero - 1) % 26;
            letras = (char) ('A' + resto) + letras;
            numero = (numero - 1) / 26;
        }
        return letras + fila;
    }

    // Número con punto decimal (Excel siempre lo guarda así, sin importar el idioma)
    private String numeroXml(double numero) {
        return String.format(Locale.US, "%.4f", numero);
    }

    // Cambia los caracteres especiales de XML
    private String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ===================== CSV =====================

    /**
     * Escribe el libro como CSV en UTF-8 con BOM (así Excel lee bien los acentos).
     * Las hojas van una debajo de otra, separadas por una fila vacía.
     */
    public void guardarCsv(File archivo) throws IOException {
        BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8));
        try {
            escritor.write('﻿');
            for (int h = 0; h < hojas.size(); h++) {
                HojaExcel hoja = hojas.get(h);
                if (h > 0) {
                    escritor.write("\r\n");
                }
                escritor.write(campoCsv("== " + hoja.getNombre() + " =="));
                escritor.write("\r\n");
                for (List<CeldaExcel> fila : hoja.getFilas()) {
                    for (int c = 0; c < fila.size(); c++) {
                        if (c > 0) {
                            escritor.write(",");
                        }
                        escritor.write(campoCsv(textoCsv(fila.get(c))));
                    }
                    escritor.write("\r\n");
                }
            }
        } finally {
            escritor.close();
        }
    }

    // Texto de una celda tal como va en el CSV (números con punto, fechas dd/MM/yyyy)
    private String textoCsv(CeldaExcel celda) {
        if (celda.getTipo() == CeldaExcel.TEXTO) {
            return celda.getTexto();
        }
        if (celda.getTipo() == CeldaExcel.FECHA) {
            if (celda.getFecha() == null) {
                return "";
            }
            return FECHA_CSV.format(celda.getFecha());
        }
        if (celda.getTipo() == CeldaExcel.PORCENTAJE) {
            return String.format(Locale.US, "%.2f%%", celda.getNumero() * 100);
        }
        if (celda.getTipo() == CeldaExcel.MONEDA) {
            return String.format(Locale.US, "%.2f", celda.getNumero());
        }
        // Número: sin decimales si es entero
        double numero = celda.getNumero();
        if (numero == Math.floor(numero)) {
            return String.valueOf((long) numero);
        }
        return String.format(Locale.US, "%.3f", numero);
    }

    // Pone comillas al campo si tiene comas, comillas o saltos de línea
    private String campoCsv(String texto) {
        boolean necesitaComillas = texto.contains(",") || texto.contains("\"") || texto.contains("\n");
        if (!necesitaComillas) {
            return texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
