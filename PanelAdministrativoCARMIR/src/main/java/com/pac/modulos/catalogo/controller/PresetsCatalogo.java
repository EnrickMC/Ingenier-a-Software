package com.pac.modulos.catalogo.controller;

import javafx.scene.control.ComboBox;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-32 (Sugerencia de nombres compatibles con la vista 3D): inicia aquí.
 * Nombres sugeridos (presets) para los formularios del Catálogo: categorías, características y opciones.
 *
 * Son los nombres que la página web reconoce para dibujar el postre en 3D
 * (PaginaWebCARMIR/src/app/features/catalogo/pastel3d/diseno-pastel.ts). Si se agrega un preset aquí,
 * también hay que agregar su palabra allá, y al revés.
 *
 * En los cupcakes el pan es el «Sabor» y lo de arriba es el «Frosting» (si el producto no tiene
 * «Frosting», el cupcake se ve sin nada arriba).
 *
 * No hay presets «Sin ...»: si la característica no es obligatoria, la página web ya ofrece
 * «Sin cobertura», «Sin frosting», etc., y en el 3D ese elemento no se dibuja.
 *
 * Solo son sugerencias: en el selector se puede escribir otro nombre. Debajo del nombre, el formulario
 * dice cómo se verá en el 3D de la página (o si la página no lo reconoce).
 */
public class PresetsCatalogo {

    // ===================== Categorías (tipo de postre) =====================

    private static final String[] CATEGORIAS = {
        "Pasteles", "Pasteles de cumpleaños", "Pasteles de boda", "Gelatinas", "Pays", "Cheesecakes",
        "Cupcakes", "Galletas", "Brownies", "Donas", "Roscas", "Macarons"
    };

    // Palabras con las que la web reconoce cada tipo de postre (mismo orden que en la web)
    private static final String[][] PALABRAS_POSTRE = {
        {"gelatina", "gelatina", "flan", "jalea"},
        {"cheesecake", "cheesecake", "cheese cake", "chescake", "tarta de queso"},
        {"macaron", "macaron", "macarron", "macaroon"},
        {"cupcake", "cupcake", "cup cake", "panque", "magdalena", "muffin", "mantecada"},
        {"galleta", "galleta", "cookie", "polvoron"},
        {"brownie", "brownie", "blondie"},
        {"dona", "dona", "donut", "rosquilla"},
        {"rosca", "rosca", "bundt", "savarin", "roscon"},
        {"pay", "pay", "pie", "tarta", "tartaleta", "kuchen", "strudel"},
        {"pastel", "pastel", "torta", "cake"}
    };

    // Cómo se ve cada tipo en el 3D
    private static final String[][] DIBUJO_POSTRE = {
        {"pastel", "pastel de 1 a 5 pisos"},
        {"gelatina", "gelatina en molde"},
        {"pay", "pay con costra y relleno"},
        {"cheesecake", "cheesecake con base de galleta"},
        {"cupcake", "un cupcake (el tamaño lo hace más grande)"},
        {"galleta", "una galleta (el tamaño la hace más grande)"},
        {"brownie", "un brownie (el tamaño lo hace más grande)"},
        {"dona", "una dona (el tamaño la hace más grande)"},
        {"rosca", "rosca con glaseado"},
        {"macaron", "un macaron (el tamaño lo hace más grande)"}
    };

    // ===================== Características =====================

    private static final String[] CARACTERISTICAS = {
        "Tamaño", "Pisos", "Forma", "Sabor", "Relleno", "Cobertura", "Frosting", "Color de cobertura", "Decoración", "Base"
    };

    // ===================== Opciones de cada característica =====================

    private static final String[] TAMANOS = {"Chico", "Mediano", "Grande", "Extra grande"};
    // Postres chicos de una sola pieza (cupcake, galleta, brownie, dona, macaron)
    private static final String[] TAMANOS_CHICOS = {"Mini", "Mediano", "Grande", "Jumbo"};
    private static final String[] PISOS = {"1 piso", "2 pisos", "3 pisos", "4 pisos", "5 pisos"};
    private static final String[] FORMAS = {"Redondo", "Cuadrado", "Corazón", "Hexágono", "Estrella", "Flor", "Ovalado"};
    private static final String[] SABORES = {
        "Vainilla", "Chocolate", "Chocolate blanco", "Fresa", "Red velvet", "Tres leches", "Limón", "Naranja",
        "Mango", "Piña", "Coco", "Café", "Cajeta", "Zanahoria", "Nuez", "Matcha", "Pistache", "Oreo",
        "Nutella", "Frutos rojos", "Plátano", "Manzana con canela", "Guayaba", "Queso"
    };
    private static final String[] SABORES_GELATINA = {
        "Fresa", "Limón", "Uva", "Naranja", "Piña", "Mango", "Mosaico", "Rompope", "Leche", "Coco", "Chocolate"
    };
    private static final String[] RELLENOS = {
        "Mermelada de fresa", "Frutos rojos", "Cajeta", "Nutella", "Crema pastelera", "Queso crema",
        "Durazno", "Mango", "Chocolate"
    };
    private static final String[] COBERTURAS = {
        "Betún de la casa", "Buttercream", "Fondant liso", "Glaseado", "Chantilly", "Ganache de chocolate",
        "Naked cake", "Chispas de colores", "Mármol", "Degradado"
    };
    // Parte de arriba del cupcake
    private static final String[] FROSTINGS = {
        "Remolino de betún", "Remolino de chantilly", "Remolino de queso crema", "Remolino con chispas",
        "Remolino degradado", "Domo de fondant", "Ganache de chocolate", "Mármol"
    };
    private static final String[] COLORES = {
        "Blanco", "Marfil", "Rosa", "Rosa pastel", "Rosa mexicano", "Rojo", "Lila", "Morado", "Azul cielo",
        "Azul", "Azul marino", "Turquesa", "Verde menta", "Verde", "Amarillo", "Naranja", "Durazno", "Coral",
        "Dorado", "Plateado", "Negro", "Chocolate"
    };
    private static final String[] DECORACIONES = {
        "Velas", "Moño", "Listón", "Flores de azúcar", "Perlas", "Chispas", "Fresas", "Frutos rojos",
        "Fruta picada", "Rosetas de crema", "Goteo de chocolate", "Hoja de oro"
    };
    private static final String[] BASES = {
        "Base dorada", "Base plateada", "Plato blanco", "Tabla de madera", "Pizarra", "Pedestal de cristal"
    };

    // ===================== Compatibilidad con cada postre =====================

    // Elementos que la web solo dibuja en algunos postres: {elemento, postres donde sí se ve...}
    // Lo que no está aquí se dibuja en todos los postres.
    private static final String[][] SOLO_EN = {
        {"goteo de chocolate (solo pasteles)", "pastel"},
        {"moño", "pastel", "cupcake"},
        {"listón", "pastel", "cupcake"},
        {"hoja de oro", "pastel", "pay", "cheesecake", "rosca", "cupcake", "galleta", "brownie", "dona", "macaron"},
        {"naked (se ven las capas)", "pastel"}
    };

    // Nombre de cada tipo de postre para los mensajes
    private static final String[][] NOMBRE_POSTRE = {
        {"pastel", "pasteles"}, {"gelatina", "gelatinas"}, {"pay", "pays"}, {"cheesecake", "cheesecakes"},
        {"cupcake", "cupcakes"}, {"galleta", "galletas"}, {"brownie", "brownies"}, {"dona", "donas"},
        {"rosca", "roscas"}, {"macaron", "macarons"}
    };

    // ===================== Palabras que reconoce la web =====================

    // Características (en el orden en que la web las revisa; «Color de cobertura» se revisa en medio)
    private static final String[][] PALABRAS_ANTES_DE_COLOR = {
        {"pisos", "piso", "nivel"},
        {"tamano", "tamano", "porcion", "personas", "medida", "pieza", "cantidad"},
        {"forma", "forma", "figura", "molde"},
        {"relleno", "relleno"}
    };
    private static final String[][] PALABRAS_CARACTERISTICA = {
        {"base", "base", "plato", "presentacion", "charola", "soporte", "pedestal", "stand"},
        {"frosting", "frosting", "copete", "remolino"},
        {"sabor", "sabor", "bizcocho", "pan"},
        {"cobertura", "cobertura", "betun", "acabado", "cubierta", "glaseado", "fondant", "decorado exterior"},
        {"decoracion", "decoracion", "adorno", "topping", "extra"}
    };

    private static final String[][] PALABRAS_COBERTURA = {
        {"sin cobertura", "sin cobertura", "sin betun", "sin glaseado", "sin frosting", "sin copete", "ninguna", "ninguno", "sin nada", "al natural", "solo pan", "desnudo"},
        {"chispas de colores", "chispa", "sprinkle", "grajea", "confeti", "granillo"},
        {"mármol (vetas)", "marmol", "marble"},
        {"degradado (color abajo, blanco arriba)", "degradado", "ombre", "difuminado"},
        {"naked (se ven las capas)", "naked", "semi desnudo", "semidesnudo"},
        {"fondant o glaseado liso", "fondant", "liso", "lisa", "glaseado", "glase", "royal"},
        {"chantilly", "chantilly", "crema batida", "nata"},
        {"ganache brillante", "ganache", "espejo"},
        {"betún de la casa", "betun", "buttercream", "merengue", "rustic", "queso crema", "mantequilla", "crema",
            "defecto", "predeterminad", "normal", "clasic", "tradicional", "sencill", "basic", "estandar", "de la casa", "original"}
    };

    private static final String[][] PALABRAS_DECORACION = {
        {"topper (letrero de pie)", "topper", "letrero"},
        {"velas", "vela"},
        {"moño", "mono"},
        {"listón", "liston", "cinta"},
        {"flores de azúcar", "flor"},
        {"perlas", "perla"},
        {"hoja de oro", "hoja de oro", "hojas de oro", "oro comestible", "laminilla", "pan de oro", "hojuela"},
        {"chispas", "chispa", "grajea", "confeti"},
        {"goteo de chocolate (solo pasteles)", "goteo", "ganache", "drip"},
        {"frutos rojos", "frutos rojos", "frutos del bosque", "mora", "arandano", "frambuesa", "blueberr"},
        {"fresas", "fresa"},
        {"fruta picada", "fruta", "durazno", "kiwi", "mango"},
        {"rosetas de crema", "crema", "chantilly", "roseta", "merengue"}
    };

    private static final String[][] PALABRAS_BASE = {
        {"pedestal de cristal", "cristal", "vidrio", "pedestal", "stand", "base alta"},
        {"tabla de madera", "madera", "tabla", "rustic"},
        {"pizarra", "pizarra", "negra", "piedra"},
        {"base plateada", "platead", "plata"},
        {"base dorada", "dorad", "oro"},
        {"plato blanco", "plato", "blanc", "ceramica", "porcelana", "charola"}
    };

    private static final String[][] PALABRAS_FORMA = {
        {"corazón", "corazon"},
        {"cuadrado", "cuadrad", "rectangul"},
        {"hexágono", "hexagon"},
        {"estrella", "estrella"},
        {"flor", "flor"},
        {"ovalado", "oval"},
        {"redondo", "redond", "circul"}
    };

    private static final String[][] PALABRAS_TAMANO = {
        {"extra grande", "extra", "gigante", "jumbo"},
        {"grande", "grande", "familiar"},
        {"mediano", "median"},
        {"chico", "chic", "individual", "mini", "pequen"}
    };

    // Sabores y colores: la palabra que la web busca (sin acentos)
    private static final String[] PALABRAS_SABOR = {
        "red velvet", "terciopelo", "tres leches", "3 leches", "oreo", "galleta", "cookies", "nutella", "avellana",
        "ferrero", "kinder", "chocolate", "choco", "cacao", "brownie", "ganache", "cereza", "fresa", "frutos rojos",
        "frambuesa", "mora", "zarzamora", "uva", "arandano", "limon", "lima", "naranja", "mandarina", "mango",
        "durazno", "maracuya", "pina", "cafe", "moka", "capuchino", "cajeta", "caramelo", "dulce de leche",
        "zanahoria", "nuez", "almendra", "coco", "matcha", "te verde", "pistache", "menta", "mosaico", "platano",
        "banana", "manzana", "canela", "horchata", "chai", "calabaza", "guayaba", "mazapan", "cacahuate", "elote",
        "crema pastelera", "pastelera", "natilla", "leche", "rompope", "vainilla", "queso"
    };

    private static final String[] PALABRAS_COLOR = {
        "azul marino", "azul cielo", "azul bebe", "celeste", "azul rey", "azul", "turquesa", "aqua", "verde menta",
        "menta", "verde", "rosa mexicano", "fiusha", "fucsia", "rosa palo", "rosa pastel", "rosa", "rojo", "vino",
        "guinda", "frambuesa", "lila", "lavanda", "morado", "purpura", "violeta", "amarillo", "naranja", "durazno",
        "melocoton", "coral", "dorado", "oro", "plateado", "plata", "cobre", "bronce", "negro", "gris", "chocolate",
        "cafe", "marfil", "hueso", "champan", "beige", "arena", "nude", "blanco"
    };

    // Clase de apoyo: no se crean objetos de ella
    private PresetsCatalogo() {
    }

    // ===================== Listas para los selectores (RF-32 · Paso 2) =====================

    public static List<String> categorias() {
        return lista(CATEGORIAS);
    }

    public static List<String> caracteristicas() {
        return lista(CARACTERISTICAS);
    }

    /**
     * RF-32 · Paso 2: características que tienen sentido para una categoría.
     * Por ejemplo, «Pisos» solo para pasteles y «Relleno» no para galletas.
     */
    public static List<String> caracteristicas(String nombreCategoria) {
        String tipo = tipoDePostre(nombreCategoria);
        List<String> resultado = new ArrayList<String>();
        for (String nombre : CARACTERISTICAS) {
            boolean sirve = caracteristicaSirve(tipoDeCaracteristica(nombre), tipo);
            // El cupcake usa «Frosting» en lugar de «Cobertura» (la web entiende las dos)
            if (nombre.equals("Cobertura") && tipo.equals("cupcake")) {
                sirve = false;
            }
            if (sirve) {
                resultado.add(nombre);
            }
        }
        return resultado;
    }

    // ¿La web dibuja este tipo de característica en este postre?
    private static boolean caracteristicaSirve(String tipoCaracteristica, String tipoPostre) {
        if (tipoCaracteristica.equals("pisos")) {
            return tipoPostre.equals("pastel");
        }
        if (tipoCaracteristica.equals("forma")) {
            return tipoPostre.equals("pastel") || tipoPostre.equals("gelatina");
        }
        if (tipoCaracteristica.equals("relleno")) {
            return !tipoPostre.equals("galleta") && !tipoPostre.equals("brownie") && !tipoPostre.equals("dona");
        }
        if (tipoCaracteristica.equals("cobertura")) {
            // La gelatina no lleva cobertura y en el macaron solo se verían chispas (eso ya está en Decoración)
            return !tipoPostre.equals("gelatina") && !tipoPostre.equals("macaron");
        }
        if (tipoCaracteristica.equals("color")) {
            return !tipoPostre.equals("gelatina");
        }
        if (tipoCaracteristica.equals("frosting")) {
            return tipoPostre.equals("cupcake");
        }
        return true;
    }

    // ¿La web dibuja esta opción en este postre? (por ejemplo, el goteo solo en pasteles)
    private static boolean opcionSirve(String tipoCaracteristica, String nombreOpcion, String tipoPostre) {
        String texto = limpiar(nombreOpcion);
        String elemento = null;
        if (tipoCaracteristica.equals("decoracion")) {
            elemento = buscarGrupo(texto, PALABRAS_DECORACION, false);
        }
        if (tipoCaracteristica.equals("cobertura") || tipoCaracteristica.equals("frosting")) {
            elemento = buscarGrupo(texto, PALABRAS_COBERTURA, false);
        }
        if (elemento == null) {
            return true;
        }
        for (String[] fila : SOLO_EN) {
            if (fila[0].equals(elemento)) {
                for (int i = 1; i < fila.length; i++) {
                    if (fila[i].equals(tipoPostre)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    // Quita de la lista las opciones que no se dibujan en el postre
    private static List<String> compatibles(String[] nombres, String tipoCaracteristica, String tipoPostre) {
        List<String> resultado = new ArrayList<String>();
        for (String nombre : nombres) {
            if (opcionSirve(tipoCaracteristica, nombre, tipoPostre)) {
                resultado.add(nombre);
            }
        }
        return resultado;
    }

    /**
     * RF-32 · Paso 4 y Excepción paso 4: mensaje si la característica no se puede usar en la categoría (por ejemplo «Pisos» en galletas).
     * Regresa "" si sí se puede o si la web no reconoce el nombre.
     */
    public static String incompatibilidadCaracteristica(String nombreCaracteristica, String nombreCategoria) {
        if (limpiar(nombreCategoria).isEmpty()) {
            return "";
        }
        String tipoPostre = tipoDePostre(nombreCategoria);
        if (caracteristicaSirve(tipoDeCaracteristica(nombreCaracteristica), tipoPostre)) {
            return "";
        }
        return "«" + nombreCaracteristica.trim() + "» no es compatible con " + plural(tipoPostre)
                + ": la página web no la puede mostrar en ese postre.";
    }

    /**
     * RF-32 · Paso 4 y Excepción paso 4: mensaje si la opción no se puede usar en la categoría (por ejemplo «Goteo de chocolate» en cupcakes).
     * Regresa "" si sí se puede o si la web no reconoce el nombre.
     */
    public static String incompatibilidadOpcion(String nombreCaracteristica, String nombreOpcion, String nombreCategoria) {
        if (limpiar(nombreCategoria).isEmpty()) {
            return "";
        }
        String tipoPostre = tipoDePostre(nombreCategoria);
        if (opcionSirve(tipoDeCaracteristica(nombreCaracteristica), nombreOpcion, tipoPostre)) {
            return "";
        }
        return "«" + nombreOpcion.trim() + "» no es compatible con " + plural(tipoPostre)
                + ": la página web no lo puede dibujar en ese postre.";
    }

    private static String plural(String tipoPostre) {
        for (String[] fila : NOMBRE_POSTRE) {
            if (fila[0].equals(tipoPostre)) {
                return fila[1];
            }
        }
        return tipoPostre;
    }

    // Opciones sugeridas según la característica; si no se reconoce, se ofrecen todas sin repetir
    public static List<String> opciones(String nombreCaracteristica) {
        return opciones(nombreCaracteristica, "");
    }

    // RF-32 · Paso 2: opciones sugeridas según la característica y la categoría (el tamaño cambia en los postres chicos)
    public static List<String> opciones(String nombreCaracteristica, String nombreCategoria) {
        String tipoPostre = tipoDePostre(nombreCategoria);
        String tipo = tipoDeCaracteristica(nombreCaracteristica);
        if (tipo.equals("tamano")) {
            if (tipoPostre.equals("cupcake") || esPiezaChica(tipoPostre)) {
                return lista(TAMANOS_CHICOS);
            }
            return lista(TAMANOS);
        }
        if (tipo.equals("pisos")) {
            return lista(PISOS);
        }
        if (tipo.equals("forma")) {
            return lista(FORMAS);
        }
        if (tipo.equals("sabor")) {
            if (tipoPostre.equals("gelatina")) {
                return lista(SABORES_GELATINA);
            }
            return lista(SABORES);
        }
        if (tipo.equals("relleno")) {
            return lista(RELLENOS);
        }
        if (tipo.equals("cobertura")) {
            return compatibles(COBERTURAS, tipo, tipoPostre);
        }
        if (tipo.equals("frosting")) {
            return compatibles(FROSTINGS, tipo, tipoPostre);
        }
        if (tipo.equals("color")) {
            return lista(COLORES);
        }
        if (tipo.equals("decoracion")) {
            return compatibles(DECORACIONES, tipo, tipoPostre);
        }
        if (tipo.equals("base")) {
            return lista(BASES);
        }
        String[][] grupos = {TAMANOS, TAMANOS_CHICOS, FORMAS, PISOS, SABORES, SABORES_GELATINA, RELLENOS,
            COBERTURAS, FROSTINGS, COLORES, DECORACIONES, BASES};
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

    // ===================== Cómo se ve en el 3D de la página web (RF-32 · Paso 3) =====================
    // RF-32 · Excepción paso 3: si la página no reconoce el nombre se avisa «La página web no reconoce este
    // nombre…», pero sí se puede guardar.

    // Texto de ayuda bajo el nombre de una categoría
    public static String vista3dCategoria(String nombre) {
        if (limpiar(nombre).isEmpty()) {
            return "Escribe o elige un nombre de la lista para que la página web lo dibuje en 3D.";
        }
        String tipo = buscarGrupo(limpiar(nombre), PALABRAS_POSTRE, true);
        if (tipo == null) {
            return "La página web no reconoce este nombre: sus productos se dibujarán como pastel "
                    + "(a menos que el nombre del producto diga el tipo, por ejemplo «Galletas de avena»).";
        }
        return "En la página web se verá en 3D como: " + dibujoDe(tipo) + ".";
    }

    // Texto de ayuda bajo el nombre de una característica, avisando si no sirve para la categoría
    public static String vista3dCaracteristica(String nombre, String nombreCategoria) {
        String incompatible = incompatibilidadCaracteristica(nombre, nombreCategoria);
        if (!incompatible.isEmpty()) {
            return incompatible;
        }
        return vista3dCaracteristica(nombre);
    }

    // Texto de ayuda bajo el nombre de una característica
    public static String vista3dCaracteristica(String nombre) {
        if (limpiar(nombre).isEmpty()) {
            return "Escribe o elige un nombre de la lista para que cambie el modelo 3D de la página web.";
        }
        String tipo = tipoDeCaracteristica(nombre);
        if (tipo.equals("otra")) {
            return "La página web no reconoce este nombre: no cambia el modelo 3D (sí cuenta para el precio y la receta).";
        }
        String[][] explicacion = {
            {"tamano", "el tamaño (en cupcakes, galletas, brownies, donas y macarons hace más grande la pieza)"},
            {"pisos", "el número de pisos (solo pasteles)"},
            {"forma", "la forma del pastel o de la gelatina"},
            {"sabor", "el color del pan (en cupcakes, solo el pan), del relleno y de la cobertura"},
            {"relleno", "el color del relleno (se ve con «Ver por dentro»)"},
            {"cobertura", "la cobertura (betún, fondant, chispas, mármol, degradado...)"},
            {"frosting", "la parte de arriba del cupcake (remolino o domo); sin esta característica el cupcake va sin nada arriba"},
            {"color", "el color de la cobertura (o del frosting)"},
            {"base", "dónde se presenta: plato, tabla, pizarra, base dorada o plateada, pedestal"},
            {"decoracion", "las decoraciones (cada unidad elegida es una pieza en el 3D)"}
        };
        for (String[] fila : explicacion) {
            if (fila[0].equals(tipo)) {
                return "En el 3D de la página web cambia " + fila[1] + ".";
            }
        }
        return "";
    }

    // Texto de ayuda bajo el nombre de una opción, avisando si no sirve para la categoría
    public static String vista3dOpcion(String nombreCaracteristica, String nombreOpcion, String nombreCategoria) {
        String incompatible = incompatibilidadOpcion(nombreCaracteristica, nombreOpcion, nombreCategoria);
        if (!incompatible.isEmpty()) {
            return incompatible;
        }
        return vista3dOpcion(nombreCaracteristica, nombreOpcion);
    }

    // Texto de ayuda bajo el nombre de una opción
    public static String vista3dOpcion(String nombreCaracteristica, String nombreOpcion) {
        String texto = limpiar(nombreOpcion);
        if (texto.isEmpty()) {
            return "Escribe o elige un nombre de la lista para que se vea en el 3D de la página web.";
        }
        String tipo = tipoDeCaracteristica(nombreCaracteristica);
        String encontrado = null;
        String siNo = "";
        if (tipo.equals("otra")) {
            return "Esta característica no cambia el modelo 3D (sí cuenta para el precio y la receta).";
        }
        if (tipo.equals("tamano")) {
            encontrado = buscarGrupo(texto, PALABRAS_TAMANO, false);
            siNo = "toma el tamaño según su lugar en la lista (la primera es la más chica)";
        } else if (tipo.equals("pisos")) {
            if (texto.matches(".*[1-9].*") || contieneAlguna(texto, new String[] {"un ", "uno", "dos", "tres", "cuatro", "cinco"})) {
                encontrado = "ese número de pisos";
            }
            siNo = "toma los pisos según su lugar en la lista";
        } else if (tipo.equals("forma")) {
            encontrado = buscarGrupo(texto, PALABRAS_FORMA, false);
            siNo = "se queda redondo";
        } else if (tipo.equals("sabor") || tipo.equals("relleno")) {
            if (contieneAlguna(texto, PALABRAS_SABOR)) {
                encontrado = "el color de ese sabor";
            }
            siNo = "usa el color de vainilla";
        } else if (tipo.equals("cobertura") || tipo.equals("frosting")) {
            encontrado = buscarGrupo(texto, PALABRAS_COBERTURA, false);
            siNo = "se dibuja con betún de la casa";
        } else if (tipo.equals("color")) {
            if (tienePalabraDe(texto, PALABRAS_COLOR)) {
                encontrado = "ese color";
            }
            siNo = "la cobertura conserva su color";
        } else if (tipo.equals("base")) {
            encontrado = buscarGrupo(texto, PALABRAS_BASE, false);
            siNo = "se usa la base normal del postre";
        } else if (tipo.equals("decoracion")) {
            encontrado = buscarGrupo(texto, PALABRAS_DECORACION, false);
            siNo = "no aparece en el 3D";
        }
        if (encontrado == null) {
            return "La página web no reconoce este nombre: " + siNo + ".";
        }
        return "Se verá en el 3D como: " + encontrado + ".";
    }

    // ===================== Selectores editables =====================

    // RF-32 · Paso 2: pone los presets en el selector (editable: también se puede escribir otro nombre)
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

    // ===================== Apoyo =====================

    // Tipo de característica como lo entiende la web (tamano, pisos, forma... u "otra")
    public static String tipoDeCaracteristica(String nombre) {
        String texto = limpiar(nombre);
        String antes = buscarGrupo(texto, PALABRAS_ANTES_DE_COLOR, false);
        if (antes != null) {
            return antes;
        }
        // «Color de cobertura» va antes que «Cobertura» (pero «Color de vela» no cambia la cobertura)
        boolean esColorSolo = texto.equals("color") || texto.equals("colores");
        String[] deCobertura = {"cobertura", "frosting", "copete", "betun", "fondant", "glaseado", "pastel", "crema"};
        if (texto.contains("color") && (esColorSolo || contieneAlguna(texto, deCobertura))) {
            return "color";
        }
        String tipo = buscarGrupo(texto, PALABRAS_CARACTERISTICA, false);
        if (tipo == null) {
            return "otra";
        }
        return tipo;
    }

    // RF-32 · Paso 1: tipo de postre de una categoría como lo entiende la web; "pastel" si no se reconoce
    public static String tipoDePostre(String nombreCategoria) {
        String tipo = buscarGrupo(limpiar(nombreCategoria), PALABRAS_POSTRE, true);
        if (tipo == null) {
            return "pastel";
        }
        return tipo;
    }

    // Postres chicos de una sola pieza (además del cupcake): sin pisos ni forma ni relleno (salvo el macaron)
    private static boolean esPiezaChica(String tipo) {
        return tipo.equals("galleta") || tipo.equals("brownie") || tipo.equals("dona") || tipo.equals("macaron");
    }

    private static String dibujoDe(String tipo) {
        for (String[] fila : DIBUJO_POSTRE) {
            if (fila[0].equals(tipo)) {
                return fila[1];
            }
        }
        return tipo;
    }

    /**
     * Busca en qué grupo cae el texto. Cada grupo es {resultado, palabra1, palabra2, ...}.
     * Con "palabraCompleta" la palabra debe estar completa («pie» no cuenta dentro de «pieza»).
     */
    private static String buscarGrupo(String texto, String[][] grupos, boolean palabraCompleta) {
        for (String[] grupo : grupos) {
            for (int i = 1; i < grupo.length; i++) {
                boolean esta;
                if (palabraCompleta) {
                    esta = tienePalabra(texto, grupo[i]);
                } else {
                    esta = texto.contains(grupo[i]);
                }
                if (esta) {
                    return grupo[0];
                }
            }
        }
        return null;
    }

    private static boolean contieneAlguna(String texto, String[] palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tienePalabraDe(String texto, String[] palabras) {
        for (String palabra : palabras) {
            if (tienePalabra(texto, palabra)) {
                return true;
            }
        }
        return false;
    }

    // ¿El texto trae la palabra completa (o en plural)?
    private static boolean tienePalabra(String texto, String palabra) {
        String partes = (" " + texto + " ").replaceAll("[^a-z0-9]+", " ");
        return partes.contains(" " + palabra + " ") || partes.contains(" " + palabra + "s ") || partes.contains(" " + palabra + "es ");
    }

    // Minúsculas y sin acentos (igual que la web)
    private static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD);
        return sinAcentos.replaceAll("\\p{M}", "").trim();
    }

    private static List<String> lista(String[] nombres) {
        List<String> resultado = new ArrayList<String>();
        for (String nombre : nombres) {
            resultado.add(nombre);
        }
        return resultado;
    }
}
