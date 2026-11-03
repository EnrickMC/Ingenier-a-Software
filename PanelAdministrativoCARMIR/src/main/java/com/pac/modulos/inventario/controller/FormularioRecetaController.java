package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.OpcionReceta;
import com.pac.modulos.inventario.model.Receta;
import com.pac.modulos.inventario.service.RecetaService;
import com.pac.modulos.inventario.service.ResultadoInventario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana "Registrar receta" (RF-42) / "Modificar receta" (RF-43) (formulario-receta.fxml).
 * El costo y el precio base se recalculan en vivo al agregar o quitar ingredientes.
 */
public class FormularioRecetaController {

    @FXML private Label lblTitulo;
    @FXML private ComboBox<OpcionReceta> cmbOpcion;
    @FXML private Label lblOpcionFija;
    @FXML private TextField txtNombre;
    @FXML private TextField txtTiempo;
    @FXML private ComboBox<Insumo> cmbInsumo;
    @FXML private TextField txtCantidad;
    @FXML private Label lblUnidad;
    @FXML private TableView<FilaIngrediente> tblIngredientes;
    @FXML private Button btnQuitar;
    @FXML private Label lblCosto;
    @FXML private Label lblPrecio;
    @FXML private TextArea txtPasos;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private RecetaService servicio = new RecetaService();
    private List<IngredienteReceta> ingredientes = new ArrayList<IngredienteReceta>();
    private Receta editada;                 // null = receta nueva
    private double multiplicador = 1;       // multiplicador de costo de la opción elegida

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";
    private int idGuardado = 0;
    private int idExistente = 0;

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblMensaje, false);
        lblUnidad.setText("");
        lblPrecio.setText("");
    }

    // ===================== Preparar el formulario =====================

    /**
     * Receta nueva. Si idOpcion es mayor a cero, esa opción queda elegida.
     * Devuelve false si no se puede abrir (sin conexión, sin opciones o sin insumos).
     */
    // RF-42 · Pasos 2 y 3: carga las opciones sin receta y muestra el formulario.
    public boolean prepararNueva(int idOpcion) {
        editada = null;
        lblTitulo.setText("Registrar receta");
        ApoyoVista.mostrarNodo(lblOpcionFija, false);

        List<OpcionReceta> opciones;
        try {
            opciones = servicio.listarOpcionesSinReceta();
        } catch (DAOException e) {
            // RF-42 · Excepción paso 2: sin conexión, «Error de conexión».
            Alertas.error(RecetaService.MSG_ERROR_CONEXION);
            return false;
        }
        if (opciones.isEmpty()) {
            // RF-42 · Excepción paso 2: «No hay opciones pendientes de receta».
            Alertas.aviso(RecetaService.MSG_SIN_OPCIONES);
            return false;
        }
        if (!llenarInsumos()) {
            return false;
        }
        cmbOpcion.getItems().setAll(opciones);

        if (idOpcion > 0) {
            OpcionReceta elegida = null;
            for (OpcionReceta opcion : opciones) {
                if (opcion.getIdOpcion() == idOpcion) {
                    elegida = opcion;
                }
            }
            if (elegida == null) {
                Alertas.aviso(RecetaService.MSG_OPCION_NO_DISPONIBLE
                        + ". Revisa en el catálogo que la opción, su característica y su categoría estén disponibles.");
                return false;
            }
            cmbOpcion.setValue(elegida);
            onCambioOpcion();
        }
        actualizarCosto();
        return true;
    }

    // Receta existente: la opción no se puede cambiar
    // RF-43 · Paso 3: muestra el formulario con los datos actuales de la receta.
    public boolean prepararEdicion(Receta receta) {
        Receta actual;
        try {
            actual = servicio.buscarPorId(receta.getIdReceta());
        } catch (DAOException e) {
            Alertas.error(RecetaService.MSG_ERROR_CONEXION);
            return false;
        }
        if (actual == null || !actual.isActiva()) {
            // RF-43 · Excepción paso 2: «La receta ya no está disponible».
            Alertas.aviso(RecetaService.MSG_NO_DISPONIBLE);
            recargarLista = true;
            return false;
        }
        if (!llenarInsumos()) {
            return false;
        }
        editada = actual;
        lblTitulo.setText("Modificar receta");
        ApoyoVista.mostrarNodo(cmbOpcion, false);
        lblOpcionFija.setText(actual.getDescripcionOpcion());
        multiplicador = actual.getMultiplicadorCosto();
        llenarCampos(actual);

        // RF-43: si tiene insumos dados de baja se pide cambiarlos
        if (actual.requiereRevision()) {
            mostrarError(RecetaService.MSG_REQUIERE_REVISION + ": quita los insumos dados de baja"
                    + " y agrega los que los reemplazan.", tblIngredientes);
        }
        return true;
    }

    // Insumos activos para el selector; false si no hay conexión o no hay insumos
    private boolean llenarInsumos() {
        List<Insumo> insumos;
        try {
            insumos = servicio.listarInsumosActivos();
        } catch (DAOException e) {
            Alertas.error(RecetaService.MSG_ERROR_CONEXION);
            return false;
        }
        if (insumos.isEmpty()) {
            Alertas.aviso("Primero registra los insumos en Inventario de Insumos");
            return false;
        }
        cmbInsumo.getItems().setAll(insumos);
        return true;
    }

    private void llenarCampos(Receta receta) {
        txtNombre.setText(receta.getNombre());
        txtTiempo.setText(String.valueOf(receta.getTiempoMinutos()));
        txtPasos.setText(receta.getPasos());
        ingredientes.clear();
        ingredientes.addAll(receta.getIngredientes());
        mostrarIngredientes();
    }

    // Al elegir la opción se toma su multiplicador; si tenía una receta dada de baja, se ofrece como base
    @FXML
    private void onCambioOpcion() {
        OpcionReceta opcion = cmbOpcion.getValue();
        if (opcion == null) {
            multiplicador = 1;
            actualizarCosto();
            return;
        }
        multiplicador = opcion.getMultiplicadorCosto();
        boolean formularioVacio = ingredientes.isEmpty() && ValidacionCatalogo.estaVacio(txtNombre.getText());
        if (opcion.getIdRecetaInactiva() > 0 && formularioVacio) {
            usarRecetaAnterior(opcion.getIdRecetaInactiva());
        }
        actualizarCosto();
    }

    // Llena el formulario con la receta dada de baja de la opción (sin los insumos que ya no existen)
    private void usarRecetaAnterior(int idReceta) {
        Receta anterior;
        try {
            anterior = servicio.buscarPorId(idReceta);
        } catch (DAOException e) {
            return;
        }
        if (anterior == null) {
            return;
        }
        List<IngredienteReceta> vigentes = new ArrayList<IngredienteReceta>();
        for (IngredienteReceta ingrediente : anterior.getIngredientes()) {
            if (ingrediente.isInsumoActivo()) {
                vigentes.add(ingrediente);
            }
        }
        anterior.setIngredientes(vigentes);
        llenarCampos(anterior);
    }

    // ===================== Ingredientes =====================

    @FXML
    private void onCambioInsumo() {
        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            lblUnidad.setText("");
            return;
        }
        lblUnidad.setText(insumo.getUnidad().getValorBD());
        // Si ya está en la receta, se muestra su cantidad para cambiarla
        for (IngredienteReceta ingrediente : ingredientes) {
            if (ingrediente.getIdInsumo() == insumo.getIdInsumo()) {
                txtCantidad.setText(Formatos.cantidad(ingrediente.getCantidad()).replace(",", ""));
            }
        }
        txtCantidad.requestFocus();
    }

    @FXML
    private void onAgregar() {
        ApoyoVista.desmarcar(cmbInsumo);
        ApoyoVista.desmarcar(txtCantidad);
        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            mostrarError("Elige el insumo que vas a agregar", cmbInsumo);
            return;
        }
        if (!ValidacionCatalogo.esDecimal(txtCantidad.getText())
                || ValidacionCatalogo.aDecimal(txtCantidad.getText()) <= 0) {
            mostrarError("La cantidad debe ser mayor a cero", txtCantidad);
            return;
        }
        double cantidad = ValidacionCatalogo.aDecimal(txtCantidad.getText());

        // Un insumo no se repite: si ya está, solo se cambia la cantidad
        boolean encontrado = false;
        for (IngredienteReceta ingrediente : ingredientes) {
            if (ingrediente.getIdInsumo() == insumo.getIdInsumo()) {
                ingrediente.setCantidad(cantidad);
                encontrado = true;
            }
        }
        if (!encontrado) {
            ingredientes.add(new IngredienteReceta(insumo, cantidad));
        }
        ApoyoVista.mostrarNodo(lblMensaje, false);
        mostrarIngredientes();
        cmbInsumo.setValue(null);
        txtCantidad.clear();
        lblUnidad.setText("");
        cmbInsumo.requestFocus();
    }

    // Al hacer clic en un ingrediente se carga en el selector para cambiar su cantidad
    @FXML
    private void onClicIngrediente() {
        FilaIngrediente fila = tblIngredientes.getSelectionModel().getSelectedItem();
        if (fila == null) {
            return;
        }
        for (Insumo insumo : cmbInsumo.getItems()) {
            if (insumo.getIdInsumo() == fila.getIngrediente().getIdInsumo()) {
                cmbInsumo.setValue(insumo);
            }
        }
    }

    @FXML
    private void onQuitar() {
        FilaIngrediente fila = tblIngredientes.getSelectionModel().getSelectedItem();
        if (fila == null) {
            Alertas.info("Selecciona en la tabla el ingrediente que quieres quitar");
            return;
        }
        ingredientes.remove(fila.getIngrediente());
        mostrarIngredientes();
    }

    // Quita de la lista el insumo indicado (se usa cuando el servicio avisa que se dio de baja)
    private void quitarInsumo(int idInsumo) {
        IngredienteReceta quitar = null;
        for (IngredienteReceta ingrediente : ingredientes) {
            if (ingrediente.getIdInsumo() == idInsumo) {
                quitar = ingrediente;
            }
        }
        if (quitar != null) {
            ingredientes.remove(quitar);
        }
        // También se quita del selector
        Insumo enSelector = null;
        for (Insumo insumo : cmbInsumo.getItems()) {
            if (insumo.getIdInsumo() == idInsumo) {
                enSelector = insumo;
            }
        }
        if (enSelector != null) {
            cmbInsumo.getItems().remove(enSelector);
        }
        mostrarIngredientes();
    }

    private void mostrarIngredientes() {
        tblIngredientes.getItems().clear();
        for (IngredienteReceta ingrediente : ingredientes) {
            tblIngredientes.getItems().add(new FilaIngrediente(ingrediente));
        }
        actualizarCosto();
    }

    // Costo en vivo (igual que vw_CostoReceta) y precio base que tendrá la opción en el catálogo
    private void actualizarCosto() {
        double costo = RecetaService.calcularCosto(ingredientes);
        lblCosto.setText("Costo: " + Formatos.moneda(costo));
        boolean hayOpcion = editada != null || cmbOpcion.getValue() != null;
        if (!hayOpcion) {
            lblPrecio.setText("Elige la opción para ver su precio base");
            return;
        }
        double precio = Math.round(costo * multiplicador * 100) / 100.0;
        lblPrecio.setText("Precio base de la opción: " + Formatos.moneda(precio)
                + "  (×" + Formatos.cantidad(multiplicador) + ")");
    }

    // ===================== Guardar =====================

    @FXML
    // RF-42 · Paso 4 / RF-43 · Paso 4: la administradora elige «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        if (editada == null && cmbOpcion.getValue() == null) {
            // RF-42 · Excepción paso 5: falta elegir la opción.
            mostrarError(RecetaService.MSG_ELIGE_OPCION, cmbOpcion);
            return;
        }
        if (!ValidacionCatalogo.esEntero(txtTiempo.getText())) {
            mostrarError(RecetaService.MSG_TIEMPO_INVALIDO, txtTiempo);
            return;
        }

        Receta datos = new Receta();
        if (editada != null) {
            datos.setIdReceta(editada.getIdReceta());
            datos.setIdOpcion(editada.getIdOpcion());
        } else {
            datos.setIdOpcion(cmbOpcion.getValue().getIdOpcion());
        }
        datos.setNombre(txtNombre.getText());
        datos.setTiempoMinutos(ValidacionCatalogo.aEntero(txtTiempo.getText()));
        datos.setPasos(txtPasos.getText());
        datos.setIngredientes(new ArrayList<IngredienteReceta>(ingredientes));

        ResultadoInventario resultado;
        if (editada == null) {
            resultado = servicio.registrar(datos);
        } else {
            resultado = servicio.modificar(datos);
        }

        if (resultado.isExitoso()) {
            guardado = true;
            mensajeExito = resultado.getMensaje();
            idGuardado = datos.getIdReceta();
            cerrar();
            return;
        }
        // RF-42: la opción ya tiene receta → se cierra y se muestra la existente
        if (resultado.isOfrecerAbrir()) {
            Alertas.aviso(resultado.getMensaje() + ". Se mostrará la receta registrada.");
            idExistente = resultado.getIdRelacionado();
            cerrar();
            return;
        }
        if (resultado.isRecargarLista()) {
            // RF-42 / RF-43 · Excepciones pasos 6 y 7: se muestra el error y no se guarda.
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }
        // RF-42 / RF-43: un insumo se dio de baja mientras se capturaba → se quita de la lista
        if (resultado.getIdRelacionado() > 0 && ResultadoInventario.CAMPO_INGREDIENTES.equals(resultado.getCampo())) {
            quitarInsumo(resultado.getIdRelacionado());
        }
        mostrarError(resultado.getMensaje(), campoDe(resultado.getCampo()));
    }

    private Node campoDe(String campo) {
        if (ResultadoInventario.CAMPO_OPCION.equals(campo)) {
            return cmbOpcion;
        }
        if (ResultadoInventario.CAMPO_NOMBRE.equals(campo)) {
            return txtNombre;
        }
        if (ResultadoInventario.CAMPO_TIEMPO.equals(campo)) {
            return txtTiempo;
        }
        if (ResultadoInventario.CAMPO_PASOS.equals(campo)) {
            return txtPasos;
        }
        if (ResultadoInventario.CAMPO_INGREDIENTES.equals(campo)) {
            return tblIngredientes;
        }
        return null;
    }

    @FXML
    // RF-42 / RF-43 · Excepción paso 4: «Cancelar» descarta los datos y cierra el formulario.
    private void onCancelar() {
        cerrar();
    }

    private void mostrarError(String mensaje, Node campo) {
        lblMensaje.setText(mensaje);
        ApoyoVista.mostrarNodo(lblMensaje, true);
        if (campo != null) {
            ApoyoVista.marcar(campo);
        }
        if (btnGuardar.getScene() != null && btnGuardar.getScene().getWindow() != null) {
            Stage ventana = (Stage) btnGuardar.getScene().getWindow();
            ventana.sizeToScene();
        }
    }

    private void quitarMarcas() {
        Node[] campos = {cmbOpcion, txtNombre, txtTiempo, cmbInsumo, txtCantidad, tblIngredientes, txtPasos};
        for (Node campo : campos) {
            ApoyoVista.desmarcar(campo);
        }
    }

    private void cerrar() {
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.close();
    }

    public boolean seGuardo() {
        return guardado;
    }

    public boolean debeRecargarLista() {
        return recargarLista;
    }

    public String getMensajeExito() {
        return mensajeExito;
    }

    public int getIdGuardado() {
        return idGuardado;
    }

    public int getIdExistente() {
        return idExistente;
    }
}
