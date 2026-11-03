package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.pedidos.controller.BarraEstados;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.service.ExclusionService;
import com.pac.modulos.catalogo.service.OpcionService;
import com.pac.modulos.catalogo.service.PermisosCatalogo;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña Exclusiones (exclusiones.fxml).
 * Lista las exclusiones de una categoría, registra nuevas, las activa / inactiva y las elimina.
 */
public class ExclusionesController {


    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private BarraEstados barraEstados;
    @FXML private TableView<FilaExclusion> tblExclusiones;
    @FXML private Label lblConteo;
    @FXML private Label lblVacio;
    @FXML private HBox cajaAcciones;
    @FXML private Button btnCambiarEstado;

    @FXML private VBox cajaAlta;
    @FXML private ComboBox<Opcion> cmbOpcionA;
    @FXML private ComboBox<Opcion> cmbOpcionB;
    @FXML private TextArea txtMotivo;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblMensaje;

    private ExclusionService servicio = new ExclusionService();
    private OpcionService opcionService = new OpcionService();
    private CatalogoController catalogo;
    private boolean puedeEditar;
    private boolean llenandoSelectores = false;

    // Todas las opciones de la categoría elegida
    private List<Opcion> opcionesCategoria = new ArrayList<Opcion>();

    public void iniciar(CatalogoController catalogo) {
        this.catalogo = catalogo;
        // Botones de estado: cada uno filtra las exclusiones
        barraEstados.agregar(EstadoRegistro.ACTIVA.getValorBD(), "Activa", "estado-terminado", "bi-check-circle-fill");
        barraEstados.agregar(EstadoRegistro.INACTIVA.getValorBD(), "Inactiva", "estado-baja", "bi-slash-circle-fill");
        barraEstados.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onFiltrar();
            }
        });
        for (EstadoRegistro estado : EstadoRegistro.values()) {
            cmbEstado.getItems().add(estado.getValorBD());
        }
        cmbEstado.setValue(EstadoRegistro.ACTIVA.getValorBD());

        puedeEditar = PermisosCatalogo.puedeEditarCatalogo();
        ApoyoVista.mostrarNodo(cajaAlta, puedeEditar);
        ApoyoVista.mostrarNodo(cajaAcciones, puedeEditar);
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    // Se llama cada vez que la pestaña queda visible
    // RF «Exclusiones entre opciones de características»: inicia aquí. Pasos 1 a 3: la pestaña muestra las exclusiones y las opciones de la categoría.
    public void alMostrar() {
        llenandoSelectores = true;
        boolean conectado = catalogo.llenarCategorias(cmbCategoria);
        llenandoSelectores = false;
        // RF «Exclusiones entre opciones de características» · Excepción paso 2: sin conexión, «Error de conexión».
        if (!conectado) {
            mostrarErrorConexion();
            return;
        }
        cargarCategoria();

        // Si se llegó desde la pestaña Opciones, esa opción queda elegida en el alta
        int idOpcion = catalogo.tomarIdOpcionActual();
        if (idOpcion > 0) {
            for (Opcion opcion : cmbOpcionA.getItems()) {
                if (opcion.getIdOpcion() == idOpcion) {
                    cmbOpcionA.setValue(opcion);
                }
            }
            onCambioOpcionA();
        }
    }

    @FXML
    private void onCambioCategoria() {
        if (llenandoSelectores) {
            return;
        }
        if (cmbCategoria.getValue() != null) {
            catalogo.setIdCategoriaActual(cmbCategoria.getValue().getIdCategoria());
        }
        cargarCategoria();
    }

    @FXML
    private void onFiltrar() {
        cargarCategoria();
    }

    @FXML
    private void onReintentar() {
        alMostrar();
    }

    // ===================== Lista =====================

    // Lee las exclusiones y las opciones de la categoría elegida
    // RF «Exclusiones entre opciones de características» · Pasos 2 y 3: lee las exclusiones registradas y las opciones de la categoría.
    private void cargarCategoria() {
        tblExclusiones.getItems().clear();
        lblConteo.setText("");
        Categoria categoria = cmbCategoria.getValue();
        if (categoria == null) {
            lblVacio.setText("No hay categorías registradas");
            opcionesCategoria = new ArrayList<Opcion>();
            llenarOpcionesA();
            return;
        }

        List<Exclusion> exclusiones;
        try {
            exclusiones = servicio.listarPorCategoria(categoria.getIdCategoria());
            opcionesCategoria = opcionService.listarPorCategoria(categoria.getIdCategoria());
        } catch (DAOException e) {
            mostrarErrorConexion();
            return;
        }

        String filtro = barraEstados.getClave();
        for (Exclusion exclusion : exclusiones) {
            if (filtro == null || filtro.equals(exclusion.getEstado().getValorBD())) {
                tblExclusiones.getItems().add(new FilaExclusion(exclusion));
            }
        }
        lblConteo.setText(tblExclusiones.getItems().size() + " exclusión(es)");
        if (tblExclusiones.getItems().isEmpty()) {
            lblVacio.setText("Esta categoría no tiene exclusiones con ese estado");
        }
        onSeleccion();
        llenarOpcionesA();
    }

    private void mostrarErrorConexion() {
        tblExclusiones.getItems().clear();
        lblVacio.setText(ExclusionService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
    }

    // Cambia el texto del botón según el estado de la exclusión elegida
    @FXML
    private void onSeleccion() {
        FilaExclusion fila = tblExclusiones.getSelectionModel().getSelectedItem();
        if (fila == null || fila.getExclusion().estaActiva()) {
            btnCambiarEstado.setText("Desactivar");
        } else {
            btnCambiarEstado.setText("Activar");
        }
    }

    // ===================== Alta =====================

    private void llenarOpcionesA() {
        cmbOpcionA.getItems().setAll(opcionesCategoria);
        cmbOpcionA.setValue(null);
        cmbOpcionB.getItems().clear();
        cmbOpcionB.setValue(null);
    }

    // La segunda opción debe ser de otra característica de la misma categoría
    @FXML
    // RF «Exclusiones entre opciones de características» · Paso 3: la opción incompatible solo puede ser de otra característica de la misma categoría.
    private void onCambioOpcionA() {
        Opcion elegida = cmbOpcionA.getValue();
        cmbOpcionB.getItems().clear();
        cmbOpcionB.setValue(null);
        if (elegida == null) {
            return;
        }
        for (Opcion opcion : opcionesCategoria) {
            if (opcion.getIdCaracteristica() != elegida.getIdCaracteristica()) {
                cmbOpcionB.getItems().add(opcion);
            }
        }
    }

    @FXML
    // RF «Exclusiones entre opciones de características» · Paso 4: la Administradora eligió «Guardar».
    private void onGuardar() {
        ApoyoVista.desmarcar(cmbOpcionA);
        ApoyoVista.desmarcar(cmbOpcionB);
        ApoyoVista.desmarcar(txtMotivo);
        ApoyoVista.mostrarNodo(lblMensaje, false);

        Exclusion datos = new Exclusion();
        if (cmbOpcionA.getValue() != null) {
            datos.setIdOpcion(cmbOpcionA.getValue().getIdOpcion());
        }
        if (cmbOpcionB.getValue() != null) {
            datos.setIdOpcionExcluida(cmbOpcionB.getValue().getIdOpcion());
        }
        datos.setMotivo(txtMotivo.getText());
        datos.setEstado(EstadoRegistro.desdeBD(cmbEstado.getValue()));

        ResultadoCatalogo resultado = servicio.registrar(datos);
        // RF «Exclusiones entre opciones de características» · Pasos 7 y 8: informa el éxito y actualiza el listado.
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
            limpiarAlta();
            cargarCategoria();
            return;
        }
        if (resultado.isRecargarLista()) {
            Alertas.aviso(resultado.getMensaje());
            cargarCategoria();
            return;
        }
        // RF «Exclusiones entre opciones de características» · Excepciones pasos 5 y 6: resalta el campo con error y conserva los datos.
        // Se resalta el campo con error y se conservan los datos
        lblMensaje.setText(resultado.getMensaje());
        ApoyoVista.mostrarNodo(lblMensaje, true);
        String campo = resultado.getCampo();
        if (ResultadoCatalogo.CAMPO_OPCION_A.equals(campo)) {
            ApoyoVista.marcar(cmbOpcionA);
        } else if (ResultadoCatalogo.CAMPO_OPCION_B.equals(campo)) {
            ApoyoVista.marcar(cmbOpcionB);
        } else if (ResultadoCatalogo.CAMPO_MOTIVO.equals(campo)) {
            ApoyoVista.marcar(txtMotivo);
        }
    }

    // Cancelar: se descartan los datos; las exclusiones no cambian
    @FXML
    // RF «Exclusiones entre opciones de características» · Excepción paso 4: «Cancelar» descarta los datos.
    private void onCancelar() {
        limpiarAlta();
    }

    private void limpiarAlta() {
        cmbOpcionA.setValue(null);
        cmbOpcionB.getItems().clear();
        txtMotivo.clear();
        cmbEstado.setValue(EstadoRegistro.ACTIVA.getValorBD());
        ApoyoVista.mostrarNodo(lblMensaje, false);
        ApoyoVista.desmarcar(cmbOpcionA);
        ApoyoVista.desmarcar(cmbOpcionB);
        ApoyoVista.desmarcar(txtMotivo);
    }

    // ===================== Cambio de estado y eliminación =====================

    @FXML
    // RF «Exclusiones entre opciones de características» · Paso 4: cambiar el estado de una exclusión (con confirmación).
    private void onCambiarEstado() {
        FilaExclusion fila = tblExclusiones.getSelectionModel().getSelectedItem();
        if (fila == null) {
            Alertas.info("Selecciona una exclusión de la lista");
            return;
        }
        Exclusion exclusion = fila.getExclusion();
        EstadoRegistro nuevo = EstadoRegistro.ACTIVA;
        String pregunta = "¿Activar esta exclusión? Se aplicará de inmediato en la personalización.";
        if (exclusion.estaActiva()) {
            nuevo = EstadoRegistro.INACTIVA;
            pregunta = "¿Inactivar esta exclusión? El cliente podrá volver a combinar las dos opciones.";
        }
        if (!Alertas.confirmar(exclusion.getDescripcionPar(), pregunta)) {
            return;
        }
        mostrarResultado(servicio.cambiarEstado(exclusion.getIdExclusion(), nuevo));
    }

    @FXML
    private void onEliminar() {
        FilaExclusion fila = tblExclusiones.getSelectionModel().getSelectedItem();
        if (fila == null) {
            Alertas.info("Selecciona una exclusión de la lista");
            return;
        }
        Exclusion exclusion = fila.getExclusion();
        if (!Alertas.confirmarDoble("¿Eliminar esta exclusión?", exclusion.getDescripcionPar()
                + "\nMotivo: " + ApoyoVista.texto(exclusion.getMotivo()))) {
            return;
        }
        mostrarResultado(servicio.eliminar(exclusion.getIdExclusion()));
    }

    private void mostrarResultado(ResultadoCatalogo resultado) {
        if (resultado.isExitoso()) {
            Alertas.info(resultado.getMensaje());
        } else {
            Alertas.error(resultado.getMensaje());
        }
        cargarCategoria();
    }
}
