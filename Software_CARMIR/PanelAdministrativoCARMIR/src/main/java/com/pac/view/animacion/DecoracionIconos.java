package com.pac.view.animacion;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ContentDisplay;

/**
 * Pone iconos animados (Lottie) en lugares estratégicos de las pantallas ya cargadas:
 * - Los mensajes de "tabla vacía" (clase tabla-vacia) llevan una cajita animada arriba del texto.
 * - Los títulos de las tarjetas del Dashboard (clase kpi-titulo) llevan el icono de su apartado.
 * Recorre los nodos una sola vez y no repite un icono que ya se puso.
 */
public class DecoracionIconos {

    private static final String MARCA = "iconoCarmir";
    // Tamaño máximo de la ilustración de "tabla vacía" (como el pastel de Gestión de Pedidos)
    private static final double TAMANO_VACIO_MAXIMO = 456;
    private static final double TAMANO_VACIO_MINIMO = 220;
    private static final double TAMANO_KPI = 44;

    // Iconos de las tarjetas del Dashboard, en el orden en que aparecen
    // Tarjetas con icono: 0-2 Dashboard, 3-5 Reportes (ingresos y gastos), 6-8 Reportes (más vendidos)
    private static final String[] TITULOS_KPI = {"PEDIDOS DE HOY", "INGRESOS DEL MES", "ALERTAS DE INVENTARIO",
            "INGRESOS", "GASTOS", "UTILIDAD BRUTA", "UNIDADES VENDIDAS", "INGRESOS POR VENTAS", "UTILIDAD"};
    private static final String[] ARCHIVOS_KPI = {"kpi_pedidos.json", "kpi_ingresos.json", "kpi_alertas.json",
            "rep_ingresos.json", "rep_gastos.json", "rep_utilidad.json",
            "rep_unidades.json", "rep_ventas.json", "rep_ganancia.json"};
    private static final double[] CUADROS_KPI = {40, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final String[] RESPALDOS_KPI = {"bi-basket-fill", "bi-piggy-bank", "bi-bell-fill",
            "bi-arrow-down-circle-fill", "bi-arrow-up-circle-fill", "bi-bar-chart-fill",
            "bi-box-seam", "bi-cash-coin", "bi-trophy-fill"};

    // Tabla o lista cuyo mensaje de "sin datos" se está revisando (sirve para ajustar el tamaño)
    private Region contenedorActual = null;

    // Recorre toda la pantalla y decora lo que encuentre
    public static void aplicar(Node raiz) {
        DecoracionIconos decoracion = new DecoracionIconos();
        decoracion.recorrer(raiz);
    }

    private void recorrer(Node nodo) {
        if (nodo instanceof Label) {
            decorar((Label) nodo);
        }
        if (nodo instanceof ScrollPane) {
            // El contenido de un ScrollPane no siempre está en sus hijos si aún no se pinta
            Node contenido = ((ScrollPane) nodo).getContent();
            if (contenido != null) {
                recorrer(contenido);
            }
        }
        if (nodo instanceof TabPane) {
            // El contenido de las pestañas no seleccionadas no está en la escena: se revisa aparte
            TabPane pestanas = (TabPane) nodo;
            for (Tab pestana : pestanas.getTabs()) {
                if (pestana.getContent() != null) {
                    recorrer(pestana.getContent());
                }
            }
        }
        // El mensaje de "sin datos" de las tablas y listas vive en su "placeholder", que no está en la escena
        if (nodo instanceof TableView) {
            Node mensaje = ((TableView<?>) nodo).getPlaceholder();
            if (mensaje != null) {
                contenedorActual = (Region) nodo;
                recorrer(mensaje);
                contenedorActual = null;
            }
        }
        if (nodo instanceof ListView) {
            Node mensaje = ((ListView<?>) nodo).getPlaceholder();
            if (mensaje != null) {
                contenedorActual = (Region) nodo;
                recorrer(mensaje);
                contenedorActual = null;
            }
        }
        if (nodo instanceof Parent) {
            Parent padre = (Parent) nodo;
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                recorrer(hijo);
            }
        }
    }

    // Devuelve la posición de la tarjeta según su título (o -1 si no lleva icono)
    private static int buscarTarjeta(String texto) {
        if (texto == null) {
            return -1;
        }
        String titulo = texto.trim().toUpperCase();
        for (int i = 0; i < TITULOS_KPI.length; i++) {
            if (TITULOS_KPI[i].equals(titulo)) {
                return i;
            }
        }
        return -1;
    }

    // Calcula el tamaño que cabe en la tabla (deja lugar para el texto y el botón) y cambia la ilustración si hace falta
    private static void ajustarCaja(Label etiqueta, Region medida) {
        double alto = medida.getHeight();
        if (alto <= 0) {
            return;
        }
        double tamano = Math.max(130, Math.min(TAMANO_VACIO_MAXIMO, alto - 125));
        Object actual = etiqueta.getProperties().get("tamanoCaja");
        if (actual instanceof Double && Math.abs(((Double) actual).doubleValue() - tamano) < 12) {
            return;
        }
        ponerCajaVacia(etiqueta, medida, tamano);
    }

    private static void ponerCajaVacia(Label etiqueta, Region medida, double tamano) {
        IconoLottie caja = new IconoLottie("vacio_grande.json", tamano, 30, "bi-inbox");
        etiqueta.setGraphic(caja);
        etiqueta.getProperties().put("tamanoCaja", Double.valueOf(tamano));
    }

    private void decorar(Label etiqueta) {
        if (etiqueta.getProperties().containsKey(MARCA)) {
            return;
        }
        if (etiqueta.getStyleClass().contains("tabla-vacia")) {
            etiqueta.getProperties().put(MARCA, Boolean.TRUE);
            etiqueta.setContentDisplay(ContentDisplay.TOP);
            etiqueta.setGraphicTextGap(2);
            Region referencia = contenedorActual;
            if (referencia == null && etiqueta.getParent() instanceof Region) {
                referencia = (Region) etiqueta.getParent();
            }
            ponerCajaVacia(etiqueta, referencia, TAMANO_VACIO_MINIMO);
            if (referencia != null) {
                // Cuando la tabla ya tiene su alto real, la ilustración se ajusta para que quepa
                final Label destino = etiqueta;
                final Region medida = referencia;
                medida.heightProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> valor, Number antes, Number ahora) {
                        ajustarCaja(destino, medida);
                    }
                });
                ajustarCaja(destino, medida);
            }
        } else if (etiqueta.getStyleClass().contains("kpi-titulo")) {
            // Tarjetas del Dashboard y de Reportes: icono propio, sin animación (se queda en un cuadro fijo)
            int numero = buscarTarjeta(etiqueta.getText());
            // La tarjeta de utilidad de Ingresos y gastos cambia su texto a "UTILIDAD" o "PÉRDIDA": se reconoce por su id
            if ("lblTituloUtilidad".equals(etiqueta.getId())) {
                numero = 5;
            }
            if (numero < 0) {
                return;
            }
            etiqueta.getProperties().put(MARCA, Boolean.TRUE);
            etiqueta.setGraphic(new IconoLottie(ARCHIVOS_KPI[numero], TAMANO_KPI, CUADROS_KPI[numero], RESPALDOS_KPI[numero]));
        }
    }
}
