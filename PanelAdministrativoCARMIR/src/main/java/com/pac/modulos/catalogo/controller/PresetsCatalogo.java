package com.pac.modulos.catalogo.controller;

import javafx.scene.control.ComboBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Nombres sugeridos (presets) para los formularios del Catálogo.
 * Salen del diseñador de pasteles 3D (pastel3d): formas, sabores, coberturas, bases y decoraciones.
 * Solo son nombres: en el selector se puede escribir uno propio.
 */
public class PresetsCatalogo {

    // Nombres de categoría
    private static final String[] CATEGORIAS = {
        "Pasteles", "Gelatinas", "Cupcakes", "Pays", "Galletas", "Pasteles de boda", "Pasteles de cumpleaños"
    };

    // Nombres de característica
    private static final String[] CARACTERISTICAS = {
        "Tamaño", "Forma", "Pisos", "Sabor", "Cobertura", "Color de cobertura", "Decoración", "Base"
    };

    private static final String[] TAMANOS = {"Chico", "Mediano", "Grande", "Extra grande"};
    private static final String[] FORMAS = {"Redondo", "Cuadrado", "Corazón", "Hexágono", "Estrella", "Flor", "Ovalado"};
    private static final String[] PISOS = {"1 piso", "2 pisos", "3 pisos", "4 pisos", "5 pisos"};
    private static final String[] SABORES = {
        "Vainilla", "Chocolate", "Fresa", "Red velvet", "Limón", "Matcha", "Frutos rojos", "Caramelo", "Coco", "Mango"
    };
    private static final String[] COBERTURAS = {
        "Fondant liso", "Buttercream", "Rústica", "Naked cake", "Con chispas", "Marmoleado", "Degradado"
    };
    private static final String[] COLORES = {
        "Chocolate", "Marfil", "Cobre", "Rosa", "Azul cielo", "Frambuesa", "Dorado", "Verde menta"
    };
    private static final String[] DECORACIONES = {
        "Perlas", "Listón", "Rosetas de crema", "Goteo de ganache", "Flores", "Chispas", "Fresas", "Velas"
    };
    private static final String[] BASES = {
        "Base dorada", "Base plateada", "Plato blanco", "Tabla de madera", "Pedestal de cristal"
    };

    // Clase de apoyo: no se crean objetos de ella
    private PresetsCatalogo() {
    }

    public static List<String> categorias() {
        return lista(CATEGORIAS);
    }

    public static List<String> caracteristicas() {
        return lista(CARACTERISTICAS);
    }

    // Opciones sugeridas según la característica; si no se reconoce, se ofrecen todas sin repetir
    public static List<String> opciones(String nombreCaracteristica) {
        String nombre = "";
        if (nombreCaracteristica != null) {
            nombre = nombreCaracteristica.trim().toLowerCase();
        }
        if (nombre.startsWith("tamaño") || nombre.startsWith("tamano")) {
            return lista(TAMANOS);
        }
        if (nombre.startsWith("forma")) {
            return lista(FORMAS);
        }
        if (nombre.startsWith("piso") || nombre.startsWith("capa")) {
            return lista(PISOS);
        }
        if (nombre.startsWith("sabor")) {
            return lista(SABORES);
        }
        if (nombre.startsWith("color")) {
            return lista(COLORES);
        }
        if (nombre.startsWith("cobertura")) {
            return lista(COBERTURAS);
        }
        if (nombre.startsWith("decoraci") || nombre.startsWith("adorno")) {
            return lista(DECORACIONES);
        }
        if (nombre.startsWith("base")) {
            return lista(BASES);
        }
        String[][] grupos = {TAMANOS, FORMAS, PISOS, SABORES, COBERTURAS, COLORES, DECORACIONES, BASES};
        List<String> todas = new ArrayList<String>();
        for (String[] grupo : grupos) {
            for (String texto : grupo) {
                if (!todas.contains(texto)) {
                    todas.add(texto);
                }
            }
        }
        return todas;
    }

    // Pone los presets en el selector (editable: también se puede escribir otro nombre)
    public static void llenar(ComboBox<String> selector, List<String> nombres) {
        selector.getItems().setAll(nombres);
    }

    // Lo escrito en un selector editable está en su editor
    public static String texto(ComboBox<String> selector) {
        return selector.getEditor().getText();
    }

    // Pone un texto en un selector editable
    public static void poner(ComboBox<String> selector, String texto) {
        selector.setValue(texto);
        selector.getEditor().setText(texto);
    }

    private static List<String> lista(String[] nombres) {
        List<String> resultado = new ArrayList<String>();
        for (String nombre : nombres) {
            resultado.add(nombre);
        }
        return resultado;
    }
}
