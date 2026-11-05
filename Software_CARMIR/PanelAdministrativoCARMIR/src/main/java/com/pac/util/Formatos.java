package com.pac.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos de fecha y hora en español que se muestran en las pantallas.
 */
public class Formatos {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("dd MMMM yyyy", ESPANOL);
    private static final DateTimeFormatter FECHA_BLOQUEO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ESPANOL);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ESPANOL);
    private static final DateTimeFormatter FECHA_HORA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ESPANOL);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ESPANOL);

    // Ejemplo: "08 Septiembre 2026"
    public static String fechaLarga(LocalDateTime fecha) {
        String texto = FECHA_LARGA.format(fecha);
        String[] palabras = texto.split(" ");
        String resultado = "";
        for (int i = 0; i < palabras.length; i++) {
            if (i > 0) {
                resultado = resultado + " ";
            }
            resultado = resultado + primeraMayuscula(palabras[i]);
        }
        return resultado;
    }

    // Ejemplo: "Martes, 8 de septiembre"
    public static String fechaBloqueo(LocalDateTime fecha) {
        return primeraMayuscula(FECHA_BLOQUEO.format(fecha));
    }

    // Ejemplo: "14:30"
    public static String hora(LocalDateTime fecha) {
        return HORA.format(fecha);
    }

    // Ejemplo: "01/11/2026 14:30". Si la fecha es null devuelve "Nunca"
    public static String fechaHoraCorta(LocalDateTime fecha) {
        if (fecha == null) {
            return "Nunca";
        }
        return FECHA_HORA_CORTA.format(fecha);
    }

    // Ejemplo: "$1,250.50" (se usa en los precios del catálogo)
    public static String moneda(double cantidad) {
        return String.format(ESPANOL, "$%,.2f", cantidad);
    }

    // Ejemplo: "01/11/2026". Si la fecha es null devuelve "—"
    public static String fecha(LocalDate fecha) {
        if (fecha == null) {
            return "—";
        }
        return FECHA_CORTA.format(fecha);
    }

    // Cantidad de inventario con hasta 3 decimales y sin ceros de más. Ejemplo: 2.5, 0.125, 3
    public static String cantidad(double valor) {
        double redondeado = Math.round(valor * 1000) / 1000.0;
        if (redondeado == Math.floor(redondeado)) {
            return String.format(ESPANOL, "%,d", (long) redondeado);
        }
        String texto = String.format(ESPANOL, "%,.3f", redondeado);
        // Se quitan los ceros del final (2.500 → 2.5)
        while (texto.endsWith("0")) {
            texto = texto.substring(0, texto.length() - 1);
        }
        return texto;
    }

    // Pone en mayúscula la primera letra de un texto
    private static String primeraMayuscula(String texto) {
        if (texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
