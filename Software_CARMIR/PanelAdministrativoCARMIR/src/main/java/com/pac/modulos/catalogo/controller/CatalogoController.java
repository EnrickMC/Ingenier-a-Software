package com.pac.modulos.catalogo.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.service.CategoriaService;
import com.pac.modulos.catalogo.service.PermisosCatalogo;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.SeccionPanel;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.List;

/**
 * Controlador del apartado "Catálogo de Postres" (catalogo.fxml). Módulo 3.
 *
 * La pantalla tiene cinco pestañas y cada una tiene su propio controlador:
 * Categorías, Características, Opciones, Exclusiones y Productos.
 * Esta clase las coordina: recuerda la categoría elegida y permite saltar
 * de una pestaña a otra (por ejemplo, de una categoría a sus características).
 */
public class CatalogoController implements ControladorSeccion {

    @FXML private TabPane pestanas;
    @FXML private Tab tabCategorias;
    @FXML private Tab tabCaracteristicas;
    @FXML private Tab tabOpciones;
    @FXML private Tab tabExclusiones;
    @FXML private Tab tabProductos;
    @FXML private Label lblModoConsulta;

    // JavaFX inyecta el controlador de cada <fx:include> como "<fx:id>Controller"
    @FXML private CategoriasController categoriasController;
    @FXML private CaracteristicasController caracteristicasController;
    @FXML private OpcionesController opcionesController;
    @FXML private ExclusionesController exclusionesController;
    @FXML private ProductosController productosController;

    // Categoría, característica y opción elegidas por última vez (0 = ninguna)
    private int idCategoriaActual;
    private int idCaracteristicaActual;
    private int idOpcionActual;

    private boolean iniciado = false;
    private CategoriaService categoriaService = new CategoriaService();

    // El Navegador llama a este método al abrir el apartado
    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        categoriasController.iniciar(this);
        caracteristicasController.iniciar(this);
        opcionesController.iniciar(this);
        exclusionesController.iniciar(this);
        productosController.iniciar(this);

        // Aviso para el personal que solo puede consultar
        boolean soloConsulta = !PermisosCatalogo.puedeEditarCatalogo();
        ApoyoVista.mostrarNodo(lblModoConsulta, soloConsulta);
        if (soloConsulta && PermisosCatalogo.puedeEditarProductos()) {
            lblModoConsulta.setText("Modo consulta: puedes administrar productos, pero las categorías,"
                    + " características, opciones y exclusiones solo las cambia la administradora.");
        }

        iniciado = true;
        categoriasController.alMostrar();
    }

    // Al cambiar de pestaña se recarga la que quedó visible (los datos pudieron cambiar en otra)
    @FXML
    private void onCambioPestana(Event evento) {
        if (!iniciado) {
            return;
        }
        Tab pestana = (Tab) evento.getSource();
        if (!pestana.isSelected()) {
            return;
        }
        if (pestana == tabCategorias) {
            categoriasController.alMostrar();
        } else if (pestana == tabCaracteristicas) {
            caracteristicasController.alMostrar();
        } else if (pestana == tabOpciones) {
            opcionesController.alMostrar();
        } else if (pestana == tabExclusiones) {
            exclusionesController.alMostrar();
        } else if (pestana == tabProductos) {
            productosController.alMostrar();
        }
    }

    // ===================== Saltos entre pestañas =====================

    // Desde Categorías: ver las características de una categoría
    public void irACaracteristicas(Categoria categoria) {
        idCategoriaActual = categoria.getIdCategoria();
        pestanas.getSelectionModel().select(tabCaracteristicas);
    }

    // Desde Características: ver las opciones de una característica
    public void irAOpciones(Caracteristica caracteristica) {
        idCategoriaActual = caracteristica.getIdCategoria();
        idCaracteristicaActual = caracteristica.getIdCaracteristica();
        pestanas.getSelectionModel().select(tabOpciones);
    }

    // Desde Opciones: administrar las exclusiones de una opción
    public void irAExclusiones(Opcion opcion, int idCategoria) {
        idCategoriaActual = idCategoria;
        idOpcionActual = opcion.getIdOpcion();
        pestanas.getSelectionModel().select(tabExclusiones);
    }

    // Desde Categorías (lista vacía de productos): abrir la pestaña de categorías
    public void irACategorias() {
        pestanas.getSelectionModel().select(tabCategorias);
    }

    // ===================== Selección compartida =====================

    // Llena un selector con todas las categorías y deja elegida la última que se usó.
    // Devuelve false si no hubo conexión.
    public boolean llenarCategorias(ComboBox<Categoria> combo) {
        List<Categoria> categorias;
        try {
            categorias = categoriaService.listarTodas();
        } catch (DAOException e) {
            return false;
        }
        combo.getItems().setAll(categorias);
        combo.setValue(null);
        for (Categoria categoria : categorias) {
            if (categoria.getIdCategoria() == idCategoriaActual) {
                combo.setValue(categoria);
            }
        }
        // Si no había ninguna elegida, se toma la primera
        if (combo.getValue() == null && !categorias.isEmpty()) {
            combo.setValue(categorias.get(0));
            idCategoriaActual = categorias.get(0).getIdCategoria();
        }
        return true;
    }

    public int getIdCategoriaActual() {
        return idCategoriaActual;
    }

    public void setIdCategoriaActual(int idCategoriaActual) {
        this.idCategoriaActual = idCategoriaActual;
    }

    public int getIdCaracteristicaActual() {
        return idCaracteristicaActual;
    }

    public void setIdCaracteristicaActual(int idCaracteristicaActual) {
        this.idCaracteristicaActual = idCaracteristicaActual;
    }

    // Devuelve la opción pedida desde la pestaña Opciones y la olvida (se usa una sola vez)
    public int tomarIdOpcionActual() {
        int id = idOpcionActual;
        idOpcionActual = 0;
        return id;
    }
}
