package com.pac.modulos.catalogo.service;

import java.util.List;

import com.pac.util.SelectorImagen;

/**
 * Revisiones de formato que usan los formularios y los servicios del catálogo.
 */
public class ValidacionCatalogo {

    // Letras (con acentos), números, espacios y signos sencillos . , - ( ) ' & /
    private static final String PATRON_NOMBRE = "[\\p{L}0-9 .,\\-()'&/]+";

    
    // Clase de apoyo: no se crean objetos de ella
    private ValidacionCatalogo() {
    }

    // true si el texto está vacío o solo tiene espacios
    public static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // Nombre obligatorio, con caracteres permitidos y sin pasar del máximo de la columna
    public static boolean nombreValido(String nombre, int maximo) {
        if (estaVacio(nombre)) {
            return false;
        }
        String limpio = nombre.trim();
        if (limpio.length() > maximo) {
            return false;
        }
        return limpio.matches(PATRON_NOMBRE);
    }

    // Texto opcional que no pasa del máximo
    public static boolean largoValido(String texto, int maximo) {
        if (texto == null) {
            return true;
        }
        return texto.trim().length() <= maximo;
    }

    // true si el texto es un número decimal (acepta coma o punto)
    public static boolean esDecimal(String texto) {
        if (estaVacio(texto)) {
            return false;
        }
        try {
            Double.parseDouble(texto.trim().replace(',', '.'));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Convierte el texto en número; usar después de esDecimal
    public static double aDecimal(String texto) {
        return Double.parseDouble(texto.trim().replace(',', '.'));
    }

    // true si el texto es un número entero
    public static boolean esEntero(String texto) {
        if (estaVacio(texto)) {
            return false;
        }
        try {
            Integer.parseInt(texto.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Convierte el texto en entero; usar después de esEntero
    public static int aEntero(String texto) {
        return Integer.parseInt(texto.trim());
    }

    // true si el número no tiene parte decimal (por ejemplo 3.0)
    public static boolean sinDecimales(double numero) {
        return numero == Math.floor(numero);
    }

    // Enlace opcional: si se escribió, debe empezar con http:// o https://
    public static boolean enlaceValido(String enlace) {
        if (estaVacio(enlace)) {
            return true;
        }
        String limpio = enlace.trim().toLowerCase();
        if (limpio.contains(" ")) {
            return false;
        }
        return limpio.startsWith("http://") || limpio.startsWith("https://");
    }

    // Imagen de un producto u opción: un enlace http/https o la ruta de un archivo de esta computadora.
    // Debe terminar en .png, .jpg, .jpeg, .gif, .bmp o .webp. Una ruta local debe existir.
    public static boolean imagenValida(String texto) {
        if (estaVacio(texto)) {
            return false;
        }
        if (SelectorImagen.esEnlace(texto)) {
            return enlaceValido(texto) && SelectorImagen.tieneFormatoPermitido(texto);
        }
        return SelectorImagen.archivoValido(texto);
    }

    // Imagen opcional: vacía es válida; si se escribió, debe ser una imagen válida
    public static boolean imagenOpcionalValida(String texto) {
        if (estaVacio(texto)) {
            return true;
        }
        return imagenValida(texto);
    }

    // Une una lista de textos con comas: "a, b, c"
    public static String unir(List<String> textos) {
        String resultado = "";
        for (int i = 0; i < textos.size(); i++) {
            if (i > 0) {
                resultado = resultado + ", ";
            }
            resultado = resultado + textos.get(i);
        }
        return resultado;
    }

    // Une una lista de textos, uno por renglón
    public static String unirLineas(List<String> textos) {
        String resultado = "";
        for (int i = 0; i < textos.size(); i++) {
            if (i > 0) {
                resultado = resultado + "\n";
            }
            resultado = resultado + "• " + textos.get(i);
        }
        return resultado;
    }

    // Une una lista de folios: "#12, #15"
    public static String unirFolios(List<Integer> folios) {
        String resultado = "";
        for (int i = 0; i < folios.size(); i++) {
            if (i > 0) {
                resultado = resultado + ", ";
            }
            resultado = resultado + "#" + folios.get(i);
        }
        return resultado;
    }

    // Muestra un número sin ".0" cuando es entero (3 en lugar de 3.0)
    public static String numero(double valor) {
        if (sinDecimales(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(Math.round(valor * 100) / 100.0);
    }
}
