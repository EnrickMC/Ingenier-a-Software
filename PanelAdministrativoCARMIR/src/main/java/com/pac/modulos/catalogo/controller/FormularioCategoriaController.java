package com.pac.modulos.catalogo.controller;

import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.catalogo.service.CategoriaService;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Ventana "Nueva categoría" / "Modificar categoría" (formulario-categoria.fxml).
 * RF-14 (registro) y RF-15 (modificación).
 */
public class FormularioCategoriaController {

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private ComboBox<String> cmbNombre;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextArea txtDescripcion;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private CategoriaService servicio = new CategoriaService();

    // Categoría que se modifica (null = registro nuevo)
    private Categoria editada;

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";

    @FXML
    private void initialize() {
        for (EstadoRegistro estado : EstadoRegistro.values()) {
            cmbEstado.getItems().add(estado.getValorBD());
        }
        cmbEstado.setValue(EstadoRegistro.ACTIVA.getValorBD());
        // Nombres sugeridos (se puede escribir otro)
        PresetsCatalogo.llenar(cmbNombre, PresetsCatalogo.categorias());
        ApoyoVista.mostrarNodo(lblMensaje, false);
    }

    // Formulario en blanco (paso 2 de RF-14)
    // RF-14 · Paso 2: despliega el formulario en blanco.
    public void prepararNueva() {
        editada = null;
        lblTitulo.setText("Nueva categoría");
    }

    // Formulario con los datos actuales (paso 3 de RF-15)
    // RF-15 · Paso 3: muestra el formulario con los datos actuales.
    public void prepararEdicion(Categoria categoria) {
        editada = categoria;
        lblTitulo.setText("Modificar categoría");
        lblSubtitulo.setText("Si la inactivas, sus productos dejan de verse en la tienda web.");
        PresetsCatalogo.poner(cmbNombre, categoria.getNombre());
        txtDescripcion.setText(ApoyoVista.texto(categoria.getDescripcion()));
        cmbEstado.setValue(categoria.getEstado().getValorBD());
    }

    @FXML
    // RF-14 / RF-15 · Paso 3 / Paso 4: la Administradora eligió «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        Categoria datos = leerFormulario();
        ResultadoCatalogo resultado;
        // RF-14 · Pasos 4 a 6 (registro) o RF-15 · Pasos 5 a 8 (modificación) en CategoriaService.
        if (editada == null) {
            resultado = servicio.registrar(datos);
        } else {
            resultado = servicio.modificar(datos, false);
            // Paso 7: se pide confirmar la inactivación
            if (resultado.isPideConfirmacion()) {
                if (Alertas.confirmar("¿Inactivar la categoría?", resultado.getMensaje())) {
                    resultado = servicio.modificar(datos, true);
                } else {
                    // RF-15 · Excepción paso 7: no confirmó la inactivación.
                    // Se restablece el estado a Activa sin guardar
                    cmbEstado.setValue(EstadoRegistro.ACTIVA.getValorBD());
                    return;
                }
            }
        }

        if (resultado.isExitoso()) {
            guardado = true;
            mensajeExito = resultado.getMensaje();
            cerrar();
            return;
        }
        // RF-15 · Excepción paso 2: la categoría ya no existe.
        if (resultado.isRecargarLista()) {
            Alertas.aviso(resultado.getMensaje());
            recargarLista = true;
            cerrar();
            return;
        }
        mostrarError(resultado);
    }

    // Cancelar: se descartan los datos y la lista no cambia
    @FXML
    // RF-14 · Excepción paso 3 / RF-15 · Excepción paso 4: «Cancelar» descarta los datos.
    private void onCancelar() {
        cerrar();
    }

    private Categoria leerFormulario() {
        Categoria datos = new Categoria();
        if (editada != null) {
            datos.setIdCategoria(editada.getIdCategoria());
        }
        datos.setNombre(PresetsCatalogo.texto(cmbNombre));
        datos.setDescripcion(txtDescripcion.getText());
        datos.setEstado(EstadoRegistro.desdeBD(cmbEstado.getValue()));
        return datos;
    }

    // Muestra el mensaje y resalta el campo con error; conserva lo capturado
    // RF-14 · Excepción paso 4 / RF-15 · Excepción paso 5: resalta el campo con error y conserva lo capturado.
    private void mostrarError(ResultadoCatalogo resultado) {
        lblMensaje.setText(resultado.getMensaje());
        ApoyoVista.mostrarNodo(lblMensaje, true);
        String campo = resultado.getCampo();
        if (ResultadoCatalogo.CAMPO_NOMBRE.equals(campo)) {
            ApoyoVista.marcar(cmbNombre);
        } else if (ResultadoCatalogo.CAMPO_DESCRIPCION.equals(campo)) {
            ApoyoVista.marcar(txtDescripcion);
        } else if (ResultadoCatalogo.CAMPO_ESTADO.equals(campo)) {
            ApoyoVista.marcar(cmbEstado);
        }
        Stage ventana = (Stage) btnGuardar.getScene().getWindow();
        ventana.sizeToScene();
    }

    private void quitarMarcas() {
        ApoyoVista.desmarcar(cmbNombre);
        ApoyoVista.desmarcar(txtDescripcion);
        ApoyoVista.desmarcar(cmbEstado);
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
