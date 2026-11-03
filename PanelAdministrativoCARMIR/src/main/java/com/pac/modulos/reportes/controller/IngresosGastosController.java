package com.pac.modulos.reportes.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.reportes.exportacion.ExportadorReportes;
import com.pac.modulos.reportes.exportacion.LibroExcel;
import com.pac.modulos.reportes.model.MovimientoRevisar;
import com.pac.modulos.reportes.model.RenglonDia;
import com.pac.modulos.reportes.model.ReporteIngresosGastos;
import com.pac.modulos.reportes.service.ReporteService;
import com.pac.util.Formatos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.List;

/**
 * Pestaña "Ingresos y gastos" (RF-46).
 * Muestra los totales, los avisos, la gráfica por semana o mes, el detalle por día
 * y los movimientos que no se sumaron por no ser consistentes.
 */
public class IngresosGastosController implements PestanaReporte {

    @FXML private HBox cajaError;
    @FXML private Label lblError;
    @FXML private Label lblIngresos;
    @FXML private Label lblDetalleIngresos;
    @FXML private Label lblGastos;
    @FXML private Label lblDetalleGastos;
    @FXML private VBox cajaUtilidad;
    @FXML private Label lblTituloUtilidad;
    @FXML private Label lblUtilidad;
    @FXML private Label lblDetalleUtilidad;
    @FXML private VBox cajaAvisos;
    @FXML private Label lblTituloGrafica;
    @FXML private BarChart<String, Number> graficaBarras;
    @FXML private Label lblConteoDias;
    @FXML private TableView<FilaDia> tblDias;
    @FXML private Label lblVacioDias;
    @FXML private VBox tarjetaRevisar;
    @FXML private TableView<FilaRevisar> tblRevisar;

    private ReporteService servicio = new ReporteService();
    private ReportesController principal;
    private ReporteIngresosGastos reporte;     // reporte en pantalla (null si no hay)

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(cajaError, false);
        ApoyoVista.mostrarNodo(tarjetaRevisar, false);
        ApoyoVista.mostrarNodo(lblVacioDias, false);
        // Los días con pérdida se pintan con otro color
        tblDias.setRowFactory(new FabricaFilasDia());
    }

    // ReportesController se presenta para que "Reintentar" pueda volver a generar
    public void iniciar(ReportesController principal) {
        this.principal = principal;
    }

    @FXML
    private void onReintentar() {
        principal.generarDeNuevo();
    }

    // ===================== Generar =====================

    @Override
    // RF-46 (Generación de reporte de ingresos y gastos): inicia aquí la pestaña. Pasos 4 a 7.
    public boolean generar(LocalDate desde, LocalDate hasta) {
        ApoyoVista.mostrarNodo(cajaError, false);
        try {
            reporte = servicio.generarIngresosGastos(desde, hasta);
        } catch (DAOException e) {
            // Error de conexión: se avisa y se ofrece "Reintentar"
            reporte = null;
            limpiar();
            // RF-46 · Excepción paso 4: «Error de conexión: no fue posible generar el reporte» con «Reintentar».
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

    // RF-46 · Paso 7: muestra totales de ingresos, gastos y utilidad.
    private void mostrarTotales() {
        lblIngresos.setText(Formatos.moneda(reporte.getIngresos()));
        String detalle = "Anticipos " + Formatos.moneda(reporte.getAnticipos())
                + " · Liquidaciones " + Formatos.moneda(reporte.getLiquidaciones())
                + "\nPagos totales " + Formatos.moneda(reporte.getPagosTotales())
                + " · Reembolsos −" + Formatos.moneda(reporte.getReembolsos());
        if (reporte.getPorReembolsar() > 0) {
            detalle = detalle + "\nPor reembolsar (no sumado): " + Formatos.moneda(reporte.getPorReembolsar());
        }
        lblDetalleIngresos.setText(detalle);

        lblGastos.setText(Formatos.moneda(reporte.getGastos()));
        lblDetalleGastos.setText("Compras " + Formatos.moneda(reporte.getCompras())
                + " (" + reporte.getNumeroCompras() + ")"
                + " · Mermas " + Formatos.moneda(reporte.getMermas())
                + " (" + reporte.getNumeroMermas() + ")"
                + "\nConsumo en producción (informativo): " + Formatos.moneda(reporte.getProduccion()));

        lblUtilidad.setText(Formatos.moneda(reporte.getUtilidad()));
        cajaUtilidad.getStyleClass().remove("perdida");
        if (reporte.esPerdida()) {
            // RF-46 · Excepción paso 6: los gastos superan a los ingresos, «Pérdida en el periodo».
            lblTituloUtilidad.setText("PÉRDIDA");
            cajaUtilidad.getStyleClass().add("perdida");
        } else {
            lblTituloUtilidad.setText("UTILIDAD");
        }
        lblDetalleUtilidad.setText("Del " + Formatos.fecha(reporte.getDesde())
                + " al " + Formatos.fecha(reporte.getHasta())
                + "\nGenerado: " + Formatos.fechaHoraCorta(reporte.getFechaGeneracion()));
    }

    private void mostrarAvisos() {
        cajaAvisos.getChildren().clear();
        List<String> avisos = servicio.avisosDe(reporte);
        for (String aviso : avisos) {
            Label etiqueta = new Label(aviso);
            etiqueta.setWrapText(true);
            etiqueta.setMaxWidth(Double.MAX_VALUE);
            if (ReporteService.MSG_PERDIDA.equals(aviso)) {
                etiqueta.getStyleClass().add("aviso-rojo");
            } else {
                etiqueta.getStyleClass().add("aviso-amarillo");
            }
            cajaAvisos.getChildren().add(etiqueta);
        }
        ApoyoVista.mostrarNodo(cajaAvisos, !avisos.isEmpty());
    }

    private void mostrarGrafica() {
        if (servicio.graficaPorSemana(reporte)) {
            lblTituloGrafica.setText("Ingresos y gastos por semana");
        } else {
            lblTituloGrafica.setText("Ingresos y gastos por mes");
        }
        XYChart.Series<String, Number> serieIngresos = new XYChart.Series<String, Number>();
        serieIngresos.setName("Ingresos");
        XYChart.Series<String, Number> serieGastos = new XYChart.Series<String, Number>();
        serieGastos.setName("Gastos");
        for (RenglonDia barra : servicio.agruparParaGrafica(reporte)) {
            serieIngresos.getData().add(new XYChart.Data<String, Number>(barra.getEtiqueta(), barra.getIngresos()));
            serieGastos.getData().add(new XYChart.Data<String, Number>(barra.getEtiqueta(), barra.getGastos()));
        }
        graficaBarras.getData().clear();
        graficaBarras.getData().add(serieIngresos);
        graficaBarras.getData().add(serieGastos);
    }

    private void mostrarTablas() {
        ObservableList<FilaDia> filas = FXCollections.observableArrayList();
        for (RenglonDia dia : reporte.getDias()) {
            filas.add(new FilaDia(dia));
        }
        tblDias.setItems(filas);
        lblConteoDias.setText(filas.size() + " día(s) con movimientos");
        ApoyoVista.mostrarNodo(lblVacioDias, filas.isEmpty());
        if (filas.isEmpty()) {
            // RF-46 · Excepción paso 5: sin registros; totales en $0.00 y «Exportar» deshabilitado.
            lblVacioDias.setText(ReporteService.MSG_SIN_REGISTROS);
        }

        ObservableList<FilaRevisar> revisar = FXCollections.observableArrayList();
        for (MovimientoRevisar movimiento : reporte.getPorRevisar()) {
            revisar.add(new FilaRevisar(movimiento));
        }
        tblRevisar.setItems(revisar);
        ApoyoVista.mostrarNodo(tarjetaRevisar, !revisar.isEmpty());
    }

    // Deja la pestaña en ceros (cuando la consulta falla)
    private void limpiar() {
        lblIngresos.setText(Formatos.moneda(0));
        lblDetalleIngresos.setText("");
        lblGastos.setText(Formatos.moneda(0));
        lblDetalleGastos.setText("");
        lblUtilidad.setText(Formatos.moneda(0));
        lblDetalleUtilidad.setText("");
        cajaAvisos.getChildren().clear();
        ApoyoVista.mostrarNodo(cajaAvisos, false);
        graficaBarras.getData().clear();
        tblDias.getItems().clear();
        lblConteoDias.setText("");
        tblRevisar.getItems().clear();
        ApoyoVista.mostrarNodo(tarjetaRevisar, false);
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
        return ExportadorReportes.TIPO_INGRESOS;
    }

    // ===================== Filas con pérdida =====================

    // Crea las filas de la tabla por día; marca con "fila-perdida" los días con utilidad negativa
    private static class FabricaFilasDia implements Callback<TableView<FilaDia>, TableRow<FilaDia>> {
        @Override
        public TableRow<FilaDia> call(TableView<FilaDia> tabla) {
            return new FilaConPerdida();
        }
    }

    private static class FilaConPerdida extends TableRow<FilaDia> {
        @Override
        protected void updateItem(FilaDia fila, boolean vacia) {
            super.updateItem(fila, vacia);
            getStyleClass().remove("fila-perdida");
            if (!vacia && fila != null && fila.getDia().getUtilidad() < 0) {
                getStyleClass().add("fila-perdida");
            }
        }
    }
}
