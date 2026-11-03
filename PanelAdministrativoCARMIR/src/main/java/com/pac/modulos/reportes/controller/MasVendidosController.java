package com.pac.modulos.reportes.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.service.CategoriaService;
import com.pac.modulos.reportes.exportacion.ExportadorReportes;
import com.pac.modulos.reportes.exportacion.LibroExcel;
import com.pac.modulos.reportes.model.RenglonVenta;
import com.pac.modulos.reportes.model.ReporteVentas;
import com.pac.modulos.reportes.service.ReporteService;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña "Más vendidos" (RF-47).
 * Muestra el top de productos, las ventas por categoría y por opción de personalización,
 * con filtro por categoría y orden por unidades o por utilidad.
 */
public class MasVendidosController implements PestanaReporte {

    private static final String TODAS = "Todas las categorías";
    private static final int TOP_GRAFICA = 10;

    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbOrden;
    @FXML private HBox cajaError;
    @FXML private Label lblError;
    @FXML private Label lblUnidades;
    @FXML private Label lblPedidos;
    @FXML private Label lblIngresosVentas;
    @FXML private Label lblCostoVentas;
    @FXML private Label lblUtilidadVentas;
    @FXML private Label lblMargen;
    @FXML private VBox cajaAvisos;
    @FXML private BarChart<Number, String> graficaTop;
    @FXML private Label lblConteoProductos;
    @FXML private TableView<FilaVenta> tblProductos;
    @FXML private Label lblVacioProductos;
    @FXML private TableView<FilaVenta> tblCategorias;
    @FXML private TableView<FilaVenta> tblOpciones;

    private ReporteService servicio = new ReporteService();
    private CategoriaService categoriaService = new CategoriaService();
    private ReportesController principal;
    private ReporteVentas reporte;                                    // reporte en pantalla (null si no hay)
    private List<Categoria> categorias = new ArrayList<Categoria>();  // mismo orden que cmbCategoria (después de "Todas")
    private boolean cambiandoFiltros = false;                         // evita generar mientras se llenan los combos

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(cajaError, false);
        ApoyoVista.mostrarNodo(lblVacioProductos, false);
        cambiandoFiltros = true;
        cmbOrden.getItems().add(ReporteService.ORDEN_UNIDADES);
        cmbOrden.getItems().add(ReporteService.ORDEN_UTILIDAD);
        cmbOrden.setValue(ReporteService.ORDEN_UNIDADES);
        cmbCategoria.getItems().add(TODAS);
        cmbCategoria.setValue(TODAS);
        cambiandoFiltros = false;
    }

    public void iniciar(ReportesController principal) {
        this.principal = principal;
        cargarCategorias();
    }

    // Llena el filtro con todas las categorías (también las inactivas, porque pudieron tener ventas)
    private void cargarCategorias() {
        String elegida = cmbCategoria.getValue();
        cambiandoFiltros = true;
        cmbCategoria.getItems().clear();
        cmbCategoria.getItems().add(TODAS);
        categorias.clear();
        try {
            categorias.addAll(categoriaService.listarTodas());
        } catch (DAOException e) {
            // Sin categorías el reporte se puede generar con "Todas"
        }
        for (Categoria categoria : categorias) {
            cmbCategoria.getItems().add(categoria.getNombre());
        }
        if (elegida != null && cmbCategoria.getItems().contains(elegida)) {
            cmbCategoria.setValue(elegida);
        } else {
            cmbCategoria.setValue(TODAS);
        }
        cambiandoFiltros = false;
    }

    @FXML
    private void onFiltro() {
        if (cambiandoFiltros || principal == null) {
            return;
        }
        principal.generarDeNuevo();
    }

    @FXML
    private void onReintentar() {
        principal.generarDeNuevo();
    }

    // ===================== Generar =====================

    @Override
    // RF-47 (Generación de reporte de productos más vendidos): inicia aquí la pestaña. Pasos 3 a 7.
    public boolean generar(LocalDate desde, LocalDate hasta) {
        ApoyoVista.mostrarNodo(cajaError, false);
        int idCategoria = idCategoriaElegida();
        String nombreCategoria = cmbCategoria.getValue();
        try {
            // Paso 3: la categoría elegida pudo borrarse mientras la pantalla estaba abierta
            if (!servicio.existeCategoria(idCategoria)) {
                // RF-47 · Excepción paso 3: «La categoría seleccionada ya no existe» y se quita el filtro.
                Alertas.aviso(ReporteService.MSG_CATEGORIA_NO_EXISTE);
                cargarCategorias();
                cambiandoFiltros = true;
                cmbCategoria.setValue(TODAS);
                cambiandoFiltros = false;
                idCategoria = 0;
                nombreCategoria = TODAS;
            }
            reporte = servicio.generarVentas(desde, hasta, idCategoria, nombreCategoria, cmbOrden.getValue());
        } catch (DAOException e) {
            reporte = null;
            limpiar();
            // RF-47 · Excepción paso 4: «Error de conexión: no fue posible generar el reporte» con «Reintentar».
            lblError.setText(ReporteService.MSG_ERROR_CONEXION + "\n" + e.getMessage());
            ApoyoVista.mostrarNodo(cajaError, true);
            return false;
        }
        mostrarTotales();
        mostrarAvisos();
        mostrarGrafica();
        mostrarTablas();
        return true;
    }

    // 0 = todas las categorías
    private int idCategoriaElegida() {
        int indice = cmbCategoria.getSelectionModel().getSelectedIndex();
        if (indice <= 0 || indice > categorias.size()) {
            return 0;
        }
        return categorias.get(indice - 1).getIdCategoria();
    }

    private void mostrarTotales() {
        lblUnidades.setText(String.valueOf(reporte.getUnidadesTotales()));
        lblPedidos.setText(reporte.getPedidos() + " pedido(s) en el periodo");
        lblIngresosVentas.setText(Formatos.moneda(reporte.getIngresosTotales()));
        lblCostoVentas.setText("Costo estimado de recetas: " + Formatos.moneda(reporte.getCostoTotal()));
        lblUtilidadVentas.setText(Formatos.moneda(reporte.getUtilidadTotal()));
        lblMargen.setText("Margen: " + Formatos.cantidad(reporte.getMargenTotal()) + " %");
    }

    private void mostrarAvisos() {
        cajaAvisos.getChildren().clear();
        List<String> avisos = servicio.avisosDe(reporte);
        for (String aviso : avisos) {
            Label etiqueta = new Label(aviso);
            etiqueta.setWrapText(true);
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            etiqueta.getStyleClass().add("aviso-amarillo");
            cajaAvisos.getChildren().add(etiqueta);
        }
        ApoyoVista.mostrarNodo(cajaAvisos, !avisos.isEmpty());
    }

    // Top 10 en barras horizontales; el primero queda arriba
    private void mostrarGrafica() {
        XYChart.Series<Number, String> serie = new XYChart.Series<Number, String>();
        List<RenglonVenta> productos = reporte.getPorProducto();
        int cuantos = Math.min(TOP_GRAFICA, productos.size());
        boolean porUtilidad = ReporteService.ORDEN_UTILIDAD.equals(reporte.getOrden());
        // El eje de categorías dibuja de abajo hacia arriba: se agregan del último al primero
        for (int i = cuantos - 1; i >= 0; i--) {
            RenglonVenta renglon = productos.get(i);
            String etiqueta = (i + 1) + ". " + renglon.getNombre();
            double valor = renglon.getUnidades();
            if (porUtilidad) {
                valor = renglon.getUtilidad();
            }
            serie.getData().add(new XYChart.Data<Number, String>(valor, etiqueta));
        }
        graficaTop.getData().clear();
        graficaTop.getData().add(serie);
    }

    // RF-47 · Paso 7: muestra el ranking y el resumen por categoría y personalización.
    private void mostrarTablas() {
        ObservableList<FilaVenta> productos = FXCollections.observableArrayList();
        int posicion = 1;
        for (RenglonVenta renglon : reporte.getPorProducto()) {
            productos.add(new FilaVenta(renglon, posicion, true));
            posicion++;
        }
        tblProductos.setItems(productos);
        lblConteoProductos.setText(productos.size() + " producto(s)");
        ApoyoVista.mostrarNodo(lblVacioProductos, productos.isEmpty());
        if (productos.isEmpty()) {
            // RF-47 · Excepción paso 5: «Sin registros de ventas en el periodo».
            lblVacioProductos.setText(ReporteService.MSG_SIN_VENTAS);
        }

        ObservableList<FilaVenta> porCategoria = FXCollections.observableArrayList();
        posicion = 1;
        for (RenglonVenta renglon : reporte.getPorCategoria()) {
            porCategoria.add(new FilaVenta(renglon, posicion, false));
            posicion++;
        }
        tblCategorias.setItems(porCategoria);

        ObservableList<FilaVenta> porOpcion = FXCollections.observableArrayList();
        posicion = 1;
        for (RenglonVenta renglon : reporte.getPorOpcion()) {
            porOpcion.add(new FilaVenta(renglon, posicion, false));
            posicion++;
        }
        tblOpciones.setItems(porOpcion);
    }

    private void limpiar() {
        lblUnidades.setText("0");
        lblPedidos.setText("");
        lblIngresosVentas.setText(Formatos.moneda(0));
        lblCostoVentas.setText("");
        lblUtilidadVentas.setText(Formatos.moneda(0));
        lblMargen.setText("");
        cajaAvisos.getChildren().clear();
        ApoyoVista.mostrarNodo(cajaAvisos, false);
        graficaTop.getData().clear();
        tblProductos.getItems().clear();
        tblCategorias.getItems().clear();
        tblOpciones.getItems().clear();
        lblConteoProductos.setText("");
    }

    // ===================== Para ReportesController =====================

    @Override
    public boolean tieneReporte() {
        return reporte != null;
    }

    @Override
    public boolean tieneDatos() {
        return reporte != null && reporte.tieneDatos();
    }

    @Override
    public LocalDate getDesde() {
        if (reporte == null) {
            return null;
        }
        return reporte.getDesde();
    }

    @Override
    public LocalDate getHasta() {
        if (reporte == null) {
            return null;
        }
        return reporte.getHasta();
    }

    @Override
    public LibroExcel armarLibro() {
        return ExportadorReportes.armar(reporte);
    }

    @Override
    public String getTipoArchivo() {
        return ExportadorReportes.TIPO_VENTAS;
    }
}
