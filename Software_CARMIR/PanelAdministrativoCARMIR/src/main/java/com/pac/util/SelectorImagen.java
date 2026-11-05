package com.pac.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * Apoyo para las imágenes del catálogo.
 * Una imagen puede ser:
 *  - un enlace de internet (http:// o https://), o
 *  - una ruta de un archivo de esta computadora (por ejemplo C:\Imagenes\pastel.png).
 *
 * elegir() abre el explorador de archivos para escoger la imagen y devuelve su ruta.
 * comoUrl() convierte la ruta en algo que JavaFX y el navegador pueden abrir (file:/...).
 */
public class SelectorImagen {

    // Formatos de imagen que se pueden elegir
    public static final String[] EXTENSIONES = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"};

    // Texto para mostrar en las ayudas y los mensajes
    public static final String FORMATOS_TEXTO = "PNG, JPG, JPEG, GIF, BMP o WEBP";

    // Última carpeta que se usó, para abrir ahí la siguiente vez
    private static File ultimaCarpeta;

    // Clase de apoyo: no se crean objetos de ella
    private SelectorImagen() {
    }

    // Abre el explorador de archivos; devuelve la ruta completa de la imagen o null si se canceló
    public static String elegir(Window duena, String rutaActual) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir imagen");

        // Primer filtro: todos los formatos juntos; después uno por formato
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imágenes (" + FORMATOS_TEXTO + ")",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp",
                "*.PNG", "*.JPG", "*.JPEG", "*.GIF", "*.BMP", "*.WEBP"));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png", "*.PNG"));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG / JPEG",
                "*.jpg", "*.jpeg", "*.JPG", "*.JPEG"));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF", "*.gif", "*.GIF"));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("BMP", "*.bmp", "*.BMP"));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("WEBP", "*.webp", "*.WEBP"));

        File carpeta = carpetaInicial(rutaActual);
        if (carpeta != null) {
            selector.setInitialDirectory(carpeta);
        }

        File archivo = selector.showOpenDialog(duena);
        if (archivo == null) {
            return null;
        }
        ultimaCarpeta = archivo.getParentFile();
        return archivo.getAbsolutePath();
    }

    // Carpeta donde se abre el explorador: la de la imagen actual, la última usada o "Imágenes"
    private static File carpetaInicial(String rutaActual) {
        if (rutaActual != null && esRutaLocal(rutaActual)) {
            File padre = archivoDe(rutaActual).getParentFile();
            if (padre != null && padre.isDirectory()) {
                return padre;
            }
        }
        if (ultimaCarpeta != null && ultimaCarpeta.isDirectory()) {
            return ultimaCarpeta;
        }
        File imagenes = new File(System.getProperty("user.home"), "Pictures");
        if (imagenes.isDirectory()) {
            return imagenes;
        }
        File imagenesEs = new File(System.getProperty("user.home"), "Imágenes");
        if (imagenesEs.isDirectory()) {
            return imagenesEs;
        }
        return null;
    }

    // true si el texto es un enlace de internet (http o https)
    public static boolean esEnlace(String texto) {
        if (texto == null) {
            return false;
        }
        String limpio = texto.trim().toLowerCase();
        return limpio.startsWith("http://") || limpio.startsWith("https://");
    }

    // true si el texto es la ruta de un archivo (no es un enlace de internet)
    public static boolean esRutaLocal(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        return !esEnlace(texto);
    }

    // true si la ruta termina en uno de los formatos permitidos
    public static boolean tieneFormatoPermitido(String texto) {
        if (texto == null) {
            return false;
        }
        String limpio = texto.trim().toLowerCase();
        // En los enlaces se ignora lo que va después de "?" (parámetros)
        int signo = limpio.indexOf('?');
        if (signo > 0 && esEnlace(limpio)) {
            limpio = limpio.substring(0, signo);
        }
        for (String extension : EXTENSIONES) {
            if (limpio.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    // true si la ruta local existe, es un archivo y tiene un formato permitido
    public static boolean archivoValido(String ruta) {
        if (!esRutaLocal(ruta) || !tieneFormatoPermitido(ruta)) {
            return false;
        }
        File archivo = archivoDe(ruta);
        return archivo.isFile();
    }

    // Convierte el texto guardado en una dirección que JavaFX puede cargar
    public static String comoUrl(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (esEnlace(limpio)) {
            return limpio;
        }
        return archivoDe(limpio).toURI().toString();
    }

    // Archivo a partir de la ruta; también acepta rutas que ya empiezan con "file:"
    private static File archivoDe(String ruta) {
        String limpio = ruta.trim();
        if (limpio.toLowerCase().startsWith("file:")) {
            try {
                return new File(new java.net.URI(limpio));
            } catch (Exception e) {
                limpio = limpio.substring(5);
            }
        }
        return new File(limpio);
    }
}
