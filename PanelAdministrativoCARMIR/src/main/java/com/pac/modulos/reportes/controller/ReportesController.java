package com.pac.modulos.reportes.controller;

import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.reportes.exportacion.ExportadorReportes;
import com.pac.modulos.reportes.exportacion.LibroExcel;
import com.pac.modulos.reportes.service.PermisosReportes;
import com.pac.modulos.reportes.service.ReporteService;
import com.pac.modulos.reportes.service.RevisionPeriodo;
import com.pac.modulos.usuarios.service.ControlAcceso;
import com.pac.modulos.usuarios.service.ResultadoAcceso;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.Navegador;
import com.pac.navigation.SeccionPanel;
import com.pac.navigation.Vista;
import com.pac.session.SesionUsuario;
import com.pac.util.Alertas;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Controlador del apartado "Reportes Financieros" (reportes.fxml). Módulo 6.
 *
 * Arriba está el periodo (con accesos rápidos) y los botones Generar y Exportar;
 * abajo, dos pestañas con su propio controlador: Ingresos y gastos (RF-65) y Más vendidos (RF-66).
 * Esta clase revisa el periodo (paso 3), el permiso (paso 2) y hace la exportación (RF-67).
 */
public class ReportesController implements ControladorSeccion {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;
    @FXML private Label lblErrorPeriodo;
    @FXML private Label lblAvisoPeriodo;
    @FXML private TabPane pestanas;
    @FXML private Tab tabIngresos;
    @FXML private Tab tabVentas;

    // JavaFX inyecta el controlador de cada <fx:include> como "<fx:id>Controller"
    @FXML private IngresosGastosController ingresosGastosController;
    @FXML private MasVendidosController masVendidosController;

    private ReporteService servicio = new ReporteService();
    private ConvertidorFecha convertidorDesde = new ConvertidorFecha();
    private ConvertidorFecha convertidorHasta = new ConvertidorFecha();
    private boolean iniciado = false;

    // Último periodo válido que se usó (lo reutilizan los filtros y "Reintentar")
    private LocalDate desdeVigente;
    private LocalDate hastaVigente;

    @FXML
    private void initialize() {
        dpDesde.setConverter(convertidorDesde);
        dpHasta.setConverter(convertidorHasta);
        ApoyoVista.mostrarNodo(lblErrorPeriodo, false);
        ApoyoVista.mostrarNodo(lblAvisoPeriodo, false);
        btnExportar.setDisable(true);
    }

    // El Navegador llama a este método al abrir el apartado
    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        ingresosGastosController.iniciar(this);
        masVendidosController.iniciar(this);
        // Al entrar se muestra el mes en curso
        ponerPeriodo(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        iniciado = true;
        onGenerar();
    }

    // ===================== Periodo =====================

    @FXML
    private void onEsteMes() {
        ponerPeriodo(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        onGenerar();
    }

    @FXML
    private void onMesPasado() {
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        ponerPeriodo(inicioMes, inicioMes.plusMonths(1).minusDays(1));
        onGenerar();
    }

    @FXML
    private void onEsteAnio() {
        ponerPeriodo(LocalDate.now().withDayOfYear(1), LocalDate.now());
        onGenerar();
    }

    private void ponerPeriodo(LocalDate desde, LocalDate hasta) {
        convertidorDesde.olvidarTextoInvalido();
        convertidorHasta.olvidarTextoInvalido();
        dpDesde.setValue(desde);
        dpHasta.setValue(hasta);
        dpDesde.getEditor().setText(convertidorDesde.toString(desde));
        dpHasta.getEditor().setText(convertidorHasta.toString(hasta));
    }

    // "Generar reporte": revisa el periodo y el permiso, y genera la pestaña visible
    @FXML
    // RF-65 / RF-66: inicia aquí en la pantalla. Paso 1: la persona indica el periodo y pide generar el reporte.
    private void onGenerar() {
        limpiarMensajes();

        // Se leen las fechas tal como están escritas
        boolean desdeInvalida = false;
        boolean hastaInvalida = false;
        LocalDate desde = null;
        LocalDate hasta = null;
        try {
            desde = leerFecha(dpDesde, convertidorDesde);
        } catch (DateTimeParseException e) {
            desdeInvalida = true;
        }
        try {
            hasta = leerFecha(dpHasta, convertidorHasta);
        } catch (DateTimeParseException e) {
            hastaInvalida = true;
        }

        // Paso 3: formato y rango del periodo
        RevisionPeriodo revision = servicio.revisarPeriodo(desde, desdeInvalida, hasta, hastaInvalida);
        if (!revision.isCorrecto()) {
            mostrarErrorPeriodo(revision);
            return;
        }
        if (revision.getAviso() != null) {
            lblAvisoPeriodo.setText(revision.getAviso());
            ApoyoVista.mostrarNodo(lblAvisoPeriodo, true);
            dpHasta.setValue(revision.getHasta());
        }
        desdeVigente = revision.getDesde();
        hastaVigente = revision.getHasta();
        generarPestanaVisible();
    }

    // La usan los filtros de "Más vendidos" y los botones "Reintentar"
    public void generarDeNuevo() {
        if (desdeVigente == null) {
            onGenerar();
            return;
        }
        generarPestanaVisible();
    }

    // Paso 2 (sesión y permiso) y después la consulta de la pestaña visible
    private void generarPestanaVisible() {
        String mensajeSinPermiso = ReporteService.MSG_SIN_PERMISO_FINANCIERO;
        if (pestanaVisible() == masVendidosController) {
            mensajeSinPermiso = ReporteService.MSG_SIN_PERMISO_REPORTES;
        }
        // RF-65 / RF-66 · Excepción paso 2: sin permiso financiero, no se genera el reporte.
        if (!revisarAcceso(mensajeSinPermiso)) {
            return;
        }
        pestanaVisible().generar(desdeVigente, hastaVigente);
        actualizarExportar();
    }

    // Lee la fecha escrita; null si está vacía. Lanza DateTimeParseException si no es una fecha
    private LocalDate leerFecha(DatePicker selector, ConvertidorFecha convertidor) {
        String texto = selector.getEditor().getText();
        if (texto == null || texto.isBlank()) {
            // JavaFX borra el texto inválido al salir del campo: se recupera para mostrarlo
            if (convertidor.getTextoInvalido() != null) {
                selector.getEditor().setText(convertidor.getTextoInvalido());
                throw new DateTimeParseException("Fecha inválida", convertidor.getTextoInvalido(), 0);
            }
            return null;
        }
        LocalDate fecha = ConvertidorFecha.leer(texto);
        selector.setValue(fecha);
        return fecha;
    }

    // RF-65 / RF-66 · Excepciones paso 3: se resalta el campo con error y no se genera el reporte.
    private void mostrarErrorPeriodo(RevisionPeriodo revision) {
        String campo = revision.getCampo();
        if (RevisionPeriodo.CAMPO_DESDE.equals(campo) || RevisionPeriodo.CAMPO_AMBOS.equals(campo)) {
            ApoyoVista.marcar(dpDesde);
        }
        if (RevisionPeriodo.CAMPO_HASTA.equals(campo) || RevisionPeriodo.CAMPO_AMBOS.equals(campo)) {
            ApoyoVista.marcar(dpHasta);
        }
        lblErrorPeriodo.setText(revision.getMensaje());
        ApoyoVista.mostrarNodo(lblErrorPeriodo, true);
        // «Rango de fechas inválido» se muestra además como alerta y bloquea la consulta
        if (ReporteService.MSG_RANGO_INVALIDO.equals(revision.getMensaje())) {
            Alertas.aviso(ReporteService.MSG_RANGO_INVALIDO);
        }
    }

    private void limpiarMensajes() {
        ApoyoVista.desmarcar(dpDesde);
        ApoyoVista.desmarcar(dpHasta);
        ApoyoVista.mostrarNodo(lblErrorPeriodo, false);
        ApoyoVista.mostrarNodo(lblAvisoPeriodo, false);
    }

    // ===================== Pestañas =====================

    // Al cambiar de pestaña se genera su reporte si todavía no tiene el del periodo vigente
    @FXML
    private void onCambioPestana(Event evento) {
        if (!iniciado) {
            return;
        }
        Tab pestana = (Tab) evento.getSource();
        if (!pestana.isSelected()) {
            return;
        }
        PestanaReporte visible = pestanaVisible();
        boolean mismoPeriodo = visible.tieneReporte() && desdeVigente != null
                && desdeVigente.equals(visible.getDesde()) && hastaVigente.equals(visible.getHasta());
        if (!mismoPeriodo && desdeVigente != null) {
            generarPestanaVisible();
        }
        actualizarExportar();
    }

    private PestanaReporte pestanaVisible() {
        if (tabVentas.isSelected()) {
            return masVendidosController;
        }
        return ingresosGastosController;
    }

    // «Exportar» solo se habilita si la pestaña visible tiene un reporte con datos
    public void actualizarExportar() {
        PestanaReporte visible = pestanaVisible();
        btnExportar.setDisable(!visible.tieneReporte() || !visible.tieneDatos());
    }

    // ===================== Permiso (paso 2) =====================

    // Revisa que la sesión siga activa y que tenga el permiso; si no, avisa y devuelve false
    // RF-65 / RF-66 / RF-67 · Paso 2: revisa sesión y permiso contra la base de datos.
    private boolean revisarAcceso(String mensajeSinPermiso) {
        ResultadoAcceso acceso = PermisosReportes.revisarAcceso();
        if (acceso == ResultadoAcceso.PERMITIDO) {
            return true;
        }
        if (acceso == ResultadoAcceso.SIN_VERIFICAR) {
            // Excepción paso 2: sin conexión, «Error de conexión».
            Alertas.error(ReporteService.MSG_ERROR_CONEXION);
            return false;
        }
        if (acceso == ResultadoAcceso.SESION_VENCIDA || acceso == ResultadoAcceso.CUENTA_INACTIVA) {
            // Sin sesión: se regresa al inicio de sesión
            // RF-67 · Excepción paso 2: sesión vencida o cuenta inactiva; se redirige al inicio de sesión.
            String mensaje = ControlAcceso.MSG_SESION_VENCIDA;
            if (acceso == ResultadoAcceso.CUENTA_INACTIVA) {
                mensaje = ControlAcceso.MSG_CUENTA_INACTIVA;
            }
            SesionUsuario.getInstancia().cerrar();
            Alertas.aviso(mensaje);
            Navegador.getInstancia().mostrar(Vista.LOGIN_PERSONAL);
            return false;
        }
        // RF-65 / RF-66 · Excepción paso 2: «No tienes permiso para consultar reportes financieros».
        Alertas.error(mensajeSinPermiso);
        return false;
    }

    // ===================== Exportar a Excel (RF-67) =====================

    @FXML
    // RF-67 (Exportación de reportes a Excel): inicia aquí. Paso 1: la persona pide «Exportar a Excel».
    private void onExportar() {
        PestanaReporte visible = pestanaVisible();
        if (!visible.tieneReporte()) {
            // RF-67 · Excepción paso 2: «Genera el reporte antes de exportarlo».
            Alertas.info(ExportadorReportes.MSG_GENERA_ANTES);
            return;
        }
        if (!visible.tieneDatos()) {
            btnExportar.setDisable(true);
            // RF-67 · Excepción paso 2: «No hay datos para exportar».
            Alertas.info(ExportadorReportes.MSG_SIN_DATOS);
            return;
        }
        if (!revisarAcceso(ReporteService.MSG_SIN_PERMISO_REPORTES)) {
            return;
        }

        // RF-67 · Paso 5: nombre del archivo con el reporte y el periodo.
        // Nombre sugerido: CARMIR_Reporte_<tipo>_<inicio>_<fin>.xlsx
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar reporte");
        selector.setInitialFileName(ExportadorReportes.nombreSugerido(visible.getTipoArchivo(),
                visible.getDesde(), visible.getHasta()));
        FileChooser.ExtensionFilter filtroExcel = new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter filtroCsv = new FileChooser.ExtensionFilter("CSV UTF-8 (*.csv)", "*.csv");
        selector.getExtensionFilters().add(filtroExcel);
        selector.getExtensionFilters().add(filtroCsv);
        File archivo = selector.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) {
            return;   // canceló
        }
        archivo = conExtension(archivo, selector.getSelectedExtensionFilter() == filtroCsv);

        LibroExcel libro = visible.armarLibro();
        boolean otraVez = true;
        while (otraVez) {
            otraVez = false;
            try {
                ExportadorReportes.guardar(libro, archivo);
                // RF-67 · Paso 5: informa el éxito.
                Alertas.info(ExportadorReportes.MSG_EXITO + "\n" + archivo.getAbsolutePath());
            } catch (IOException e) {
                if (ExportadorReportes.MSG_DESCARGA_INCOMPLETA.equals(e.getMessage())) {
                    // Ofrece «Descargar de nuevo»
                    // RF-67 · Excepción paso 5: «La descarga no se completó»; se ofrece «Descargar de nuevo».
                    otraVez = Alertas.confirmar(ExportadorReportes.MSG_DESCARGA_INCOMPLETA, "¿Descargar de nuevo?");
                } else {
                    // RF-67 · Excepción paso 4: «No fue posible generar el archivo de Excel».
                    Alertas.error(ExportadorReportes.MSG_ERROR_ARCHIVO + "\n" + e.getMessage());
                }
            }
        }
    }

    // Agrega .xlsx o .csv al nombre si el usuario no lo escribió
    private File conExtension(File archivo, boolean esCsv) {
        String nombre = archivo.getName().toLowerCase();
        if (esCsv && !nombre.endsWith(".csv")) {
            return new File(archivo.getParentFile(), archivo.getName() + ".csv");
        }
        if (!esCsv && !nombre.endsWith(".xlsx") && !nombre.endsWith(".csv")) {
            return new File(archivo.getParentFile(), archivo.getName() + ".xlsx");
        }
        return archivo;
    }
}
