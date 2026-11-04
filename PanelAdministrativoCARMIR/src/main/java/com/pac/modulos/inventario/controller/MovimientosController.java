package com.pac.modulos.inventario.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.controller.ApoyoVista;
import com.pac.modulos.pedidos.controller.BarraEstados;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.model.Movimiento;
import com.pac.modulos.inventario.model.TipoMovimiento;
import com.pac.modulos.inventario.service.InsumoService;
import com.pac.modulos.inventario.service.InventarioService;
import com.pac.modulos.inventario.service.PermisosInventario;
import com.pac.util.Alertas;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-64 (Consulta de lotes y movimientos de insumos): pestaña Lotes y movimientos (movimientos.fxml).
 * Historial de movimientos filtrable por insumo, tipo y fechas; lotes con existencia;
 * y acceso a registrar compras (RF-59) y mermas (RF-60).
 */
public class MovimientosController {

    private static final String TODAS_CATEGORIAS = "Todas las categorías";

    @FXML private BarraEstados barraTipo;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<Insumo> cmbInsumo;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnEntrada;
    @FXML private Button btnMerma;

    @FXML private TableView<FilaMovimiento> tblMovimientos;
    @FXML private Label lblConteo;
    @FXML private Label lblAviso;
    @FXML private Label lblVacio;
    @FXML private Label lblTotales;

    @FXML private Label lblTituloLotes;
    @FXML private TableView<FilaLote> tblLotes;
    @FXML private TableColumn<FilaLote, String> colLoteInsumo;
    @FXML private Label lblLotesVacio;
    @FXML private Button btnMermaLote;

    private List<Insumo> insumosCargados = new ArrayList<Insumo>();   // todos los insumos (para filtrar por categoría)
    private InsumoService insumoService = new InsumoService();
    private InventarioService servicio = new InventarioService();
    private InventarioController inventario;
    private Insumo todos;                 // elemento "Todos los insumos" del selector
    private int idInsumoPedido = 0;       // insumo que pidió la pestaña Insumos (0 = ninguno)
    private boolean llenandoFiltros = false;
    private boolean puedeMover;

    @FXML
    private void initialize() {
        todos = new Insumo();
        todos.setNombre("Todos los insumos");

        // Botones de tipo de movimiento: cada uno filtra la lista
        barraTipo.agregar(TipoMovimiento.COMPRA.toString(), "Compra", "estado-aprobado", "bi-cart-fill");
        barraTipo.agregar(TipoMovimiento.PRODUCCION.toString(), "Producción", "estado-pagado", "bi-gear-fill");
        barraTipo.agregar(TipoMovimiento.MERMA.toString(), "Merma", "estado-cancelado", "bi-trash-fill");
        barraTipo.agregar(TipoMovimiento.AJUSTE.toString(), "Ajuste", "estado-preparacion", "bi-sliders");
        barraTipo.setAlCambiar(new Runnable() {
            @Override
            public void run() {
                onBuscar();
            }
        });
    }

    public void iniciar(InventarioController inventario) {
        this.inventario = inventario;
        puedeMover = PermisosInventario.puedeRegistrarMovimientos();
        ApoyoVista.mostrarNodo(btnEntrada, puedeMover);
        ApoyoVista.mostrarNodo(btnMerma, puedeMover);
        ApoyoVista.mostrarNodo(btnMermaLote, puedeMover);
    }

    // RF-64 · Paso 1: la pestaña Insumos pide ver los movimientos de un insumo
    public void filtrarPorInsumo(int idInsumo) {
        idInsumoPedido = idInsumo;
    }

    // RF-64: inicia aquí. Paso 1: se llama cada vez que la pestaña queda visible
    public void alMostrar() {
        if (!llenarInsumos()) {
            // RF-64 · Excepción paso 2: «Error de conexión» con «Reintentar».
            mostrarErrorConexion();
            return;
        }
        cargar();
    }

    // RF-64 · Paso 2: llena los selectores de categoría e insumo (insumos también dados de baja, por su historial);
    // false si no hubo conexión
    private boolean llenarInsumos() {
        int idElegido = idInsumoPedido;
        if (idElegido == 0 && cmbInsumo.getValue() != null) {
            idElegido = cmbInsumo.getValue().getIdInsumo();
        }
        idInsumoPedido = 0;

        List<Insumo> insumos;
        List<String> categorias;
        try {
            insumos = insumoService.listar(true);
            categorias = insumoService.listarCategorias();
        } catch (DAOException e) {
            return false;
        }
        insumosCargados = insumos;

        llenandoFiltros = true;
        // Categorías: se conserva la elegida; si piden ver un insumo de otra categoría, se quita el filtro
        String categoriaAntes = cmbCategoria.getValue();
        cmbCategoria.getItems().clear();
        cmbCategoria.getItems().add(TODAS_CATEGORIAS);
        cmbCategoria.getItems().addAll(categorias);
        if (categoriaAntes != null && cmbCategoria.getItems().contains(categoriaAntes)) {
            cmbCategoria.setValue(categoriaAntes);
        } else {
            cmbCategoria.setValue(TODAS_CATEGORIAS);
        }
        if (idElegido > 0) {
            Insumo pedido = buscarEnCargados(idElegido);
            if (pedido != null && !coincideCategoria(pedido.getCategoria())) {
                cmbCategoria.setValue(TODAS_CATEGORIAS);
            }
        }
        llenarSelectorInsumos(idElegido);
        llenandoFiltros = false;
        return true;
    }

    // Pone en el selector los insumos de la categoría elegida y vuelve a elegir el indicado (si sigue ahí)
    private void llenarSelectorInsumos(int idElegido) {
        cmbInsumo.getItems().clear();
        cmbInsumo.getItems().add(todos);
        cmbInsumo.setValue(todos);
        for (Insumo insumo : insumosCargados) {
            if (coincideCategoria(insumo.getCategoria())) {
                cmbInsumo.getItems().add(insumo);
                if (insumo.getIdInsumo() == idElegido) {
                    cmbInsumo.setValue(insumo);
                }
            }
        }
    }

    // true si la categoría coincide con el filtro (o no hay filtro de categoría)
    private boolean coincideCategoria(String categoria) {
        String elegida = categoriaElegida();
        if (elegida == null) {
            return true;
        }
        return elegida.equalsIgnoreCase(categoria);
    }

    // Categoría elegida en el filtro; null si es "Todas las categorías"
    private String categoriaElegida() {
        String elegida = cmbCategoria.getValue();
        if (elegida == null || TODAS_CATEGORIAS.equals(elegida)) {
            return null;
        }
        return elegida;
    }

    private Insumo buscarEnCargados(int idInsumo) {
        for (Insumo insumo : insumosCargados) {
            if (insumo.getIdInsumo() == idInsumo) {
                return insumo;
            }
        }
        return null;
    }

    // Categoría de un insumo (por su id); null si no se encuentra
    private String categoriaDeInsumo(int idInsumo) {
        Insumo insumo = buscarEnCargados(idInsumo);
        if (insumo == null) {
            return null;
        }
        return insumo.getCategoria();
    }

    // Al cambiar la categoría, el selector de insumos solo muestra los de esa categoría
    @FXML
    private void onCambioCategoria() {
        if (llenandoFiltros) {
            return;
        }
        llenandoFiltros = true;
        llenarSelectorInsumos(0);
        llenandoFiltros = false;
        cargar();
    }

    // RF-64 · Paso 3: el usuario cambió un filtro (tipo de movimiento, insumo o fechas).
    @FXML
    private void onBuscar() {
        if (llenandoFiltros) {
            return;
        }
        cargar();
    }

    @FXML
    private void onReintentar() {
        alMostrar();
    }

    // ===================== Historial y lotes =====================

    // RF-64 · Pasos 4 a 7: valida las fechas, consulta y muestra movimientos, totales y lotes.
    private void cargar() {
        tblMovimientos.getItems().clear();
        tblLotes.getItems().clear();
        ApoyoVista.mostrarNodo(lblAviso, false);
        lblConteo.setText("");
        lblTotales.setText("");

        // Filtros
        int idInsumo = 0;
        if (cmbInsumo.getValue() != null) {
            idInsumo = cmbInsumo.getValue().getIdInsumo();
        }
        TipoMovimiento tipo = null;
        for (TipoMovimiento posible : TipoMovimiento.values()) {
            if (posible.toString().equals(barraTipo.getClave())) {
                tipo = posible;
            }
        }
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            // RF-64 · Excepción paso 4: «La fecha inicial no puede ser posterior a la final»; no se consulta.
            lblAviso.setText("La fecha inicial no puede ser posterior a la final");
            ApoyoVista.mostrarNodo(lblAviso, true);
            lblVacio.setText("Corrige las fechas del filtro");
            return;
        }

        // RF-64 · Paso 5: consulta los movimientos y los lotes que cumplen los filtros.
        List<Movimiento> movimientos;
        List<Lote> lotes;
        try {
            movimientos = servicio.listarMovimientos(idInsumo, tipo, desde, hasta);
            if (idInsumo > 0) {
                lotes = servicio.listarLotes(idInsumo, true);
            } else {
                lotes = servicio.listarLotesConExistencia();
            }
        } catch (DAOException e) {
            // RF-64 · Excepción paso 2: «Error de conexión» (también al consultar).
            mostrarErrorConexion();
            return;
        }

        // Filtro por categoría de insumo (el servicio filtra por insumo, tipo y fechas)
        if (categoriaElegida() != null) {
            List<Movimiento> deLaCategoria = new ArrayList<Movimiento>();
            for (Movimiento movimiento : movimientos) {
                if (coincideCategoria(categoriaDeInsumo(movimiento.getIdInsumo()))) {
                    deLaCategoria.add(movimiento);
                }
            }
            movimientos = deLaCategoria;
            List<Lote> lotesDeLaCategoria = new ArrayList<Lote>();
            for (Lote lote : lotes) {
                if (coincideCategoria(categoriaDeInsumo(lote.getIdInsumo()))) {
                    lotesDeLaCategoria.add(lote);
                }
            }
            lotes = lotesDeLaCategoria;
        }

        // RF-64 · Paso 6: movimientos, cuántos son y totales en dinero por tipo
        double compras = 0;
        double mermas = 0;
        double produccion = 0;
        for (Movimiento movimiento : movimientos) {
            tblMovimientos.getItems().add(new FilaMovimiento(movimiento));
            if (movimiento.getTipo() == TipoMovimiento.COMPRA) {
                compras = compras + movimiento.getCosto();
            } else if (movimiento.getTipo() == TipoMovimiento.MERMA) {
                mermas = mermas + movimiento.getCosto();
            } else if (movimiento.getTipo() == TipoMovimiento.PRODUCCION) {
                produccion = produccion + movimiento.getCosto();
            }
        }
        lblConteo.setText(movimientos.size() + " movimiento(s)");
        if (movimientos.isEmpty()) {
            // RF-64 · Excepción paso 6: no hay movimientos (con o sin filtros).
            if (hayFiltro()) {
                lblVacio.setText("No hay movimientos con esos filtros");
            } else {
                lblVacio.setText("No hay movimientos registrados. Empieza registrando una compra.");
            }
        } else {
            lblTotales.setText("Compras: " + Formatos.moneda(compras) + "   ·   Mermas (pérdidas): " + Formatos.moneda(mermas)
                    + "   ·   Usado en producción: " + Formatos.moneda(produccion));
        }

        // RF-64 · Paso 7: lotes del insumo elegido o todos los que tienen existencia
        for (Lote lote : lotes) {
            tblLotes.getItems().add(new FilaLote(lote));
        }
        colLoteInsumo.setVisible(idInsumo == 0);
        if (idInsumo > 0) {
            lblTituloLotes.setText("Lotes de " + cmbInsumo.getValue().getNombre());
        } else if (categoriaElegida() != null) {
            lblTituloLotes.setText("Lotes de " + categoriaElegida());
        } else {
            lblTituloLotes.setText("Lotes con existencia");
        }
    }

    private boolean hayFiltro() {
        boolean conCategoria = categoriaElegida() != null;
        boolean conInsumo = cmbInsumo.getValue() != null && cmbInsumo.getValue() != todos;
        boolean conTipo = barraTipo.getClave() != null;
        return conCategoria || conInsumo || conTipo || dpDesde.getValue() != null || dpHasta.getValue() != null;
    }

    private void mostrarErrorConexion() {
        tblMovimientos.getItems().clear();
        tblLotes.getItems().clear();
        lblVacio.setText(InventarioService.MSG_ERROR_CONEXION);
        lblConteo.setText("");
        lblTotales.setText("");
    }

    // ===================== Compras y mermas (RF-59 y RF-60 desde RF-64) =====================

    @FXML
    private void onEntrada() {
        if (inventario.abrirEntrada(insumoElegido())) {
            alMostrar();
        }
    }

    @FXML
    private void onMerma() {
        if (inventario.abrirMerma(insumoElegido(), null, null)) {
            alMostrar();
        }
    }

    // Merma del lote seleccionado en la tabla de lotes
    @FXML
    private void onMermaLote() {
        FilaLote fila = tblLotes.getSelectionModel().getSelectedItem();
        if (fila == null) {
            Alertas.info("Selecciona un lote de la tabla");
            return;
        }
        Insumo insumo = buscarInsumo(fila.getLote().getIdInsumo());
        if (inventario.abrirMerma(insumo, fila.getLote(), null)) {
            alMostrar();
        }
    }

    // Insumo elegido en el filtro (null si es "Todos los insumos" o está dado de baja)
    private Insumo insumoElegido() {
        Insumo elegido = cmbInsumo.getValue();
        if (elegido == null || elegido == todos || !elegido.isActivo()) {
            return null;
        }
        return elegido;
    }

    private Insumo buscarInsumo(int idInsumo) {
        for (Insumo insumo : cmbInsumo.getItems()) {
            if (insumo.getIdInsumo() == idInsumo && insumo != todos) {
                return insumo;
            }
        }
        return null;
    }
}
