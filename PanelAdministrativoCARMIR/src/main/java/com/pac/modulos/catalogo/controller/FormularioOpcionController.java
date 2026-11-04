package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.service.CategoriaService;
import com.pac.modulos.catalogo.service.OpcionService;
import com.pac.modulos.catalogo.service.ResultadoCatalogo;
import com.pac.modulos.catalogo.service.ValidacionCatalogo;
import com.pac.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Ventana "Nueva opción" / "Modificar opción" (formulario-opcion.fxml).
 */
public class FormularioOpcionController {

    @FXML private Label lblTitulo;
    @FXML private Label lblCaracteristica;
    @FXML private ComboBox<String> cmbNombre;
    @FXML private Label lblVista3d;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtImagen;
    @FXML private TextField txtMultIngredientes;
    @FXML private TextField txtMultCosto;
    @FXML private TextField txtPuntos;
    @FXML private Label lblAyudaPuntos;
    @FXML private TextField txtMultPresupuesto;
    @FXML private CheckBox chkSeEscala;
    @FXML private CheckBox chkDisponible;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private OpcionService servicio = new OpcionService();
    private Opcion editada;
    private Caracteristica caracteristica;
    // Nombre de la categoría de la característica (se lee una vez al abrir el formulario)
    private String nombreCategoria = "";

    private boolean guardado = false;
    private boolean recargarLista = false;
    private String mensajeExito = "";

    @FXML
    private void initialize() {
        ApoyoVista.mostrarNodo(lblMensaje, false);
        // Cada vez que cambia el nombre se explica cómo se verá en el 3D de la página web
        // (listener de JavaFX: la flecha es la forma corta de escribirlo)
        cmbNombre.getEditor().textProperty().addListener((observable, antes, ahora) -> actualizarVista3d());
    }

    // RF-32 · Paso 3: dice cómo se verá esta opción en el 3D de la página web
    private void actualizarVista3d() {
        String nombreCaracteristica = "";
        if (caracteristica != null) {
            nombreCaracteristica = caracteristica.getNombre();
        }
        lblVista3d.setText(PresetsCatalogo.vista3dOpcion(nombreCaracteristica, PresetsCatalogo.texto(cmbNombre), nombreCategoria));
    }

    // Nombre de la categoría de la característica (para sugerir solo opciones compatibles con el postre)
    private String buscarNombreCategoria() {
        try {
            Categoria categoria = new CategoriaService().buscarPorId(caracteristica.getIdCategoria());
            if (categoria != null) {
                return categoria.getNombre();
            }
        } catch (DAOException e) {
            // Si no se puede leer la categoría, se sugieren los nombres generales
        }
        return "";
    }

    // Formulario en blanco con los valores por defecto de la tabla
    // RF-23 · Paso 3: formulario en blanco.
    public void prepararNueva(Caracteristica caracteristica) {
        editada = null;
        this.caracteristica = caracteristica;
        lblTitulo.setText("Nueva opción");
        mostrarCaracteristica();
        Opcion porDefecto = new Opcion();
        llenarNumeros(porDefecto);
        chkSeEscala.setSelected(porDefecto.isSeEscala());
    }

    // Formulario con los datos actuales
    // RF-25 · Paso 3: formulario con los datos actuales.
    public void prepararEdicion(Opcion opcion, Caracteristica caracteristica) {
        editada = opcion;
        this.caracteristica = caracteristica;
        lblTitulo.setText("Modificar opción");
        mostrarCaracteristica();
        PresetsCatalogo.poner(cmbNombre, opcion.getNombre());
        txtDescripcion.setText(ApoyoVista.texto(opcion.getDescripcion()));
        txtImagen.setText(ApoyoVista.texto(opcion.getLinkImagen()));
        llenarNumeros(opcion);
        chkSeEscala.setSelected(opcion.isSeEscala());
        chkDisponible.setSelected(opcion.isDisponible());
    }

    private void mostrarCaracteristica() {
        String texto = "Característica: " + caracteristica.getNombre();
        if (caracteristica.isSeleccionMultiple()) {
            texto = texto + " · selección múltiple con presupuesto de "
                    + ValidacionCatalogo.numero(caracteristica.getPresupuestoSeleccion()) + " puntos";
        } else {
            texto = texto + " · selección única (los puntos no se usan)";
        }
        lblCaracteristica.setText(texto);
        // RF-32 · Pasos 1 y 2: nombres sugeridos según la característica y la categoría (se puede escribir otro)
        nombreCategoria = buscarNombreCategoria();
        PresetsCatalogo.llenar(cmbNombre, PresetsCatalogo.opciones(caracteristica.getNombre(), nombreCategoria));
        actualizarVista3d();
    }

    private void llenarNumeros(Opcion opcion) {
        txtMultIngredientes.setText(ValidacionCatalogo.numero(opcion.getMultiplicadorIngredientes()));
        txtMultCosto.setText(ValidacionCatalogo.numero(opcion.getMultiplicadorCosto()));
        txtPuntos.setText(ValidacionCatalogo.numero(opcion.getPuntosSeleccion()));
        txtMultPresupuesto.setText(ValidacionCatalogo.numero(opcion.getMultiplicadorPresupuesto()));
    }

    @FXML
    // RF-23 (Registro de opción de característica) / RF-25 · Paso 4: la Administradora eligió «Guardar».
    private void onGuardar() {
        quitarMarcas();
        ApoyoVista.mostrarNodo(lblMensaje, false);

        // Los números se revisan aquí para marcar el campo exacto
        TextField[] numericos = {txtMultIngredientes, txtMultCosto, txtPuntos, txtMultPresupuesto};
        for (TextField campo : numericos) {
            // RF-23 (Registro de opción de característica) / RF-25 · Excepción paso 5: un número no es válido; se marca el campo.
            if (!ValidacionCatalogo.esDecimal(campo.getText())) {
                mostrarError("Escribe un número válido (por ejemplo 1 o 1.5)", campo);
                return;
            }
        }

        // RF-23 (Registro de opción de característica) / RF-25 · Excepción paso 5: la opción no se puede dibujar en este postre (por ejemplo goteo en cupcakes); se marca el nombre (RF-32 · Paso 4).
        String incompatible = PresetsCatalogo.incompatibilidadOpcion(caracteristica.getNombre(), PresetsCatalogo.texto(cmbNombre), nombreCategoria);
        if (!incompatible.isEmpty()) {
            mostrarError(incompatible, cmbNombre);
            return;
        }

        Opcion datos = new Opcion();
        if (editada != null) {
            datos.setIdOpcion(editada.getIdOpcion());
        }
        datos.setIdCaracteristica(caracteristica.getIdCaracteristica());
        datos.setNombre(PresetsCatalogo.texto(cmbNombre));
        datos.setDescripcion(txtDescripcion.getText());
        datos.setLinkImagen(txtImagen.getText());
        datos.setMultiplicadorIngredientes(ValidacionCatalogo.aDecimal(txtMultIngredientes.getText()));
        datos.setMultiplicadorCosto(ValidacionCatalogo.aDecimal(txtMultCosto.getText()));
        datos.setPuntosSeleccion(ValidacionCatalogo.aDecimal(txtPuntos.getText()));
        datos.setMultiplicadorPresupuesto(ValidacionCatalogo.aDecimal(txtMultPresupuesto.getText()));
        datos.setSeEscala(chkSeEscala.isSelected());
        datos.setDisponible(chkDisponible.isSelected());

        ResultadoCatalogo resultado;
        if (editada == null) {
            resultado = servicio.registrar(datos);
        } else {
            resultado = servicio.modificar(datos, false);
            // Paso 7: cambió el multiplicador de costo o la disponibilidad
            // RF-25 · Excepción paso 7: no confirmó el cambio; regresa al formulario.
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
        mostrarError(resultado.getMensaje(), campoDe(resultado.getCampo()));
    }

    // Convierte el nombre del campo del resultado en el control del formulario
    private Node campoDe(String campo) {
        if (ResultadoCatalogo.CAMPO_NOMBRE.equals(campo)) {
            return cmbNombre;
        }
        if (ResultadoCatalogo.CAMPO_DESCRIPCION.equals(campo)) {
            return txtDescripcion;
        }
        if (ResultadoCatalogo.CAMPO_IMAGEN.equals(campo)) {
            return txtImagen;
        }
        if (ResultadoCatalogo.CAMPO_MULT_INGREDIENTES.equals(campo)) {
            return txtMultIngredientes;
        }
        if (ResultadoCatalogo.CAMPO_MULT_COSTO.equals(campo)) {
            return txtMultCosto;
        }
        if (ResultadoCatalogo.CAMPO_PUNTOS.equals(campo)) {
            return txtPuntos;
        }
        if (ResultadoCatalogo.CAMPO_MULT_PRESUPUESTO.equals(campo)) {
            return txtMultPresupuesto;
        }
        return null;
    }

    @FXML
    // RF-23 (Registro de opción de característica) / RF-25 · Excepción paso 4: «Cancelar» descarta los datos.
    private void onCancelar() {
        cerrar();
    }

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
        Node[] campos = {cmbNombre, txtDescripcion, txtImagen, txtMultIngredientes, txtMultCosto, txtPuntos,
                txtMultPresupuesto};
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

    // Botón "Elegir imagen": abre el explorador de archivos y escribe la ruta en el campo
    @FXML
    private void onElegirImagen() {
        ApoyoVista.elegirImagen(txtImagen);
    }
}
