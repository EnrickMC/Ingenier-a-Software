package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.service.CaracteristicaService;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Ventana "Nueva característica" / "Modificar característica" (formulario-caracteristica.fxml).
 */
public class FormularioCaracteristicaController {

    @FXML private Label lblTitulo;
    @FXML private Label lblCategoria;
    @FXML private ComboBox<String> cmbNombre;
    @FXML private Label lblVista3d;
    @FXML private TextField txtOrden;
    @FXML private TextArea txtDescripcion;
    @FXML private RadioButton rbUnica;
    @FXML private RadioButton rbMultiple;
    @FXML private HBox cajaPresupuesto;
    @FXML private TextField txtPresupuesto;
    @FXML private Label lblAyudaPresupuesto;
    @FXML private CheckBox chkObligatoria;
    @FXML private CheckBox chkDisponible;
    @FXML private CheckBox chkPermiteReceta;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private CaracteristicaService servicio = new CaracteristicaService();
    private Caracteristica editada;
    private int idCategoria;
    // Nombre de la categoría (para avisar si la característica no sirve en ese postre)
    private String nombreCategoria = "";

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";

    @FXML
    private void initialize() {
        // RF-32 · Paso 2: nombres sugeridos (se puede escribir otro)
        PresetsCatalogo.llenar(cmbNombre, PresetsCatalogo.caracteristicas());
        ApoyoVista.mostrarNodo(lblMensaje, false);
        // Cada vez que cambia el nombre se explica qué cambia en el 3D de la página web
        // (listener de JavaFX: la flecha es la forma corta de escribirlo)
        cmbNombre.getEditor().textProperty().addListener((observable, antes, ahora) -> actualizarVista3d());
        actualizarVista3d();
    }

    // RF-32 · Paso 3: dice qué cambia esta característica en el 3D de la página web
    private void actualizarVista3d() {
        lblVista3d.setText(PresetsCatalogo.vista3dCaracteristica(PresetsCatalogo.texto(cmbNombre), nombreCategoria));
    }

    // RF-32 · Pasos 1 y 2: solo se sugieren las características que sirven para el tipo de postre de la categoría
    private void sugerirPara(Categoria categoria) {
        if (categoria != null) {
            nombreCategoria = categoria.getNombre();
            PresetsCatalogo.llenar(cmbNombre, PresetsCatalogo.caracteristicas(categoria.getNombre()));
        }
    }

    // Formulario en blanco con el siguiente orden libre
    // RF-19 · Paso 3: despliega el formulario en blanco con el siguiente orden libre.
    public void prepararNueva(Categoria categoria, int ordenSugerido) {
        editada = null;
        idCategoria = categoria.getIdCategoria();
        lblTitulo.setText("Nueva característica");
        lblCategoria.setText("Categoría: " + categoria.getNombre());
        sugerirPara(categoria);
        txtOrden.setText(String.valueOf(ordenSugerido));
        actualizarVista3d();
        txtPresupuesto.setText("1");
        rbUnica.setSelected(true);
        onCambioSeleccion();
    }

    // Formulario con los datos actuales
    // RF-20 · Paso 3: formulario con los datos actuales.
    public void prepararEdicion(Caracteristica caracteristica, Categoria categoria) {
        editada = caracteristica;
        idCategoria = caracteristica.getIdCategoria();
        lblTitulo.setText("Modificar característica");
        if (categoria != null) {
            lblCategoria.setText("Categoría: " + categoria.getNombre());
        }
        sugerirPara(categoria);
        PresetsCatalogo.poner(cmbNombre, caracteristica.getNombre());
        actualizarVista3d();
        txtDescripcion.setText(ApoyoVista.texto(caracteristica.getDescripcion()));
        txtOrden.setText(String.valueOf(caracteristica.getOrdenMostrar()));
        txtPresupuesto.setText(ValidacionCatalogo.numero(caracteristica.getPresupuestoSeleccion()));
        if (caracteristica.isSeleccionMultiple()) {
            rbMultiple.setSelected(true);
        } else {
            rbUnica.setSelected(true);
        }
        chkObligatoria.setSelected(caracteristica.isObligatoria());
        chkDisponible.setSelected(caracteristica.isDisponible());
        chkPermiteReceta.setSelected(caracteristica.isPermiteReceta());
        onCambioSeleccion();
    }

    // El presupuesto solo se captura en la selección múltiple
    @FXML
    private void onCambioSeleccion() {
        boolean multiple = rbMultiple.isSelected();
        cajaPresupuesto.setDisable(!multiple);
        lblAyudaPresupuesto.setDisable(!multiple);
    }

    @FXML
    // RF-19 · Paso 4 / RF-20 · Paso 4: la Administradora eligió «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        // Los números se revisan aquí para poder marcar el campo exacto
        // RF-19 (Registro de característica) / RF-20 · Excepción paso 5: el orden de muestra no es un entero válido.
        if (!ValidacionCatalogo.esEntero(txtOrden.getText())) {
            mostrarError(CaracteristicaService.MSG_ORDEN_INVALIDO, txtOrden);
            return;
        }
        double presupuesto = 1;
        if (rbMultiple.isSelected()) {
            // RF-19 (Registro de característica) / RF-20 · Excepción paso 5: el presupuesto de selección no es válido.
            if (!ValidacionCatalogo.esDecimal(txtPresupuesto.getText())) {
                mostrarError(CaracteristicaService.MSG_PRESUPUESTO_INVALIDO, txtPresupuesto);
                return;
            }
            presupuesto = ValidacionCatalogo.aDecimal(txtPresupuesto.getText());
        }

        // RF-19 (Registro de característica) / RF-20 · Excepción paso 5: la característica no se puede mostrar en este postre (por ejemplo pisos en galletas); se marca el nombre (RF-32 · Paso 4).
        String incompatible = PresetsCatalogo.incompatibilidadCaracteristica(PresetsCatalogo.texto(cmbNombre), nombreCategoria);
        if (!incompatible.isEmpty()) {
            mostrarError(incompatible, cmbNombre);
            return;
        }

        Caracteristica datos = new Caracteristica();
        if (editada != null) {
            datos.setIdCaracteristica(editada.getIdCaracteristica());
        }
        datos.setIdCategoria(idCategoria);
        datos.setNombre(PresetsCatalogo.texto(cmbNombre));
        datos.setDescripcion(txtDescripcion.getText());
        datos.setOrdenMostrar(ValidacionCatalogo.aEntero(txtOrden.getText()));
        datos.setSeleccionMultiple(rbMultiple.isSelected());
        datos.setPresupuestoSeleccion(presupuesto);
        datos.setObligatoria(chkObligatoria.isSelected());
        datos.setDisponible(chkDisponible.isSelected());
        datos.setPermiteReceta(chkPermiteReceta.isSelected());

        ResultadoCatalogo resultado;
        if (editada == null) {
            resultado = servicio.registrar(datos);
        } else {
            resultado = servicio.modificar(datos, false);
            // Paso 7: cambio que afecta los carritos; si no confirma, se queda en el formulario
            // RF-20 · Excepción paso 7: no confirmó el cambio; regresa al formulario sin guardar.
            if (resultado.isPideConfirmacion()) {
                if (!Alertas.confirmar("Confirmar cambio", resultado.getMensaje())) {
                    return;
                }
                resultado = servicio.modificar(datos, true);
            }
        }

        if (resultado.isExitoso()) {
            guardado = true;
            mensajeExito = resultado.getMensaje();
            cerrar();
            return;
        }
        if (resultado.isRecargarLista()) {
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }

        String campo = resultado.getCampo();
        if (ResultadoCatalogo.CAMPO_NOMBRE.equals(campo)) {
            mostrarError(resultado.getMensaje(), cmbNombre);
        } else if (ResultadoCatalogo.CAMPO_DESCRIPCION.equals(campo)) {
            mostrarError(resultado.getMensaje(), txtDescripcion);
        } else if (ResultadoCatalogo.CAMPO_PRESUPUESTO.equals(campo)) {
            mostrarError(resultado.getMensaje(), txtPresupuesto);
        } else if (ResultadoCatalogo.CAMPO_ORDEN.equals(campo)) {
            mostrarError(resultado.getMensaje(), txtOrden);
        } else {
            mostrarError(resultado.getMensaje(), null);
        }
    }

    @FXML
    // RF-19 (Registro de característica) / RF-20 · Excepción paso 4: «Cancelar» descarta los datos.
    private void onCancelar() {
        cerrar();
    }

    // Muestra el mensaje y marca el campo (si se indica)
    private void mostrarError(String mensaje, Node campo) {
        lblMensaje.setText(mensaje);
        ApoyoVista.mostrarNodo(lblMensaje, true);
        if (campo != null) {
            ApoyoVista.marcar(campo);
        }
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.sizeToScene();
    }

    private void quitarMarcas() {
        ApoyoVista.desmarcar(cmbNombre);
        ApoyoVista.desmarcar(txtDescripcion);
        ApoyoVista.desmarcar(txtOrden);
        ApoyoVista.desmarcar(txtPresupuesto);
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
}
