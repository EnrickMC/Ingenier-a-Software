package com.pac.modulos.reportes.controller;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Hace que los selectores de fecha usen siempre el formato dd/MM/aaaa.
 *
 * Si se escribe un texto que no es fecha, JavaFX lo borra al salir del campo.
 * Por eso esta clase recuerda ese texto (getTextoInvalido) y el controlador
 * puede mostrar «Fecha inválida» en lugar de «campo vacío».
 */
public class ConvertidorFecha extends StringConverter<LocalDate> {

    // "uuuu" + STRICT rechaza fechas que no existen, como 31/02/2026
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    @Override
    public String toString(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return FORMATO.format(fecha);
    }

    private String textoInvalido;     // último texto escrito que no era una fecha (o null)

    @Override
    public LocalDate fromString(String texto) {
        if (texto == null || texto.isBlank()) {
            textoInvalido = null;
            return null;
        }
        try {
            LocalDate fecha = leer(texto);
            textoInvalido = null;
            return fecha;
        } catch (DateTimeParseException e) {
            textoInvalido = texto.trim();
            return null;
        }
    }

    public String getTextoInvalido() {
        return textoInvalido;
    }

    public void olvidarTextoInvalido() {
        textoInvalido = null;
    }

    // Convierte "dd/MM/aaaa" en fecha; lanza DateTimeParseException si no es válida
    public static LocalDate leer(String texto) {
        return LocalDate.parse(texto.trim(), FORMATO);
    }
}
