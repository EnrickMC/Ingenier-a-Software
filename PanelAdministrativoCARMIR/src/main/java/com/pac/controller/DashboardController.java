package com.pac.controller;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.TipoAlerta;
import com.pac.modulos.inventario.service.AlertaService;
import com.pac.modulos.pedidos.controller.FilaPedido;
import com.pac.modulos.pedidos.controller.PedidosController;
import com.pac.modulos.pedidos.model.FiltroPedidos;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.service.AgendaService;
import com.pac.modulos.pedidos.service.MonitorPedidos;
import com.pac.modulos.pedidos.service.PedidoService;
import com.pac.modulos.pedidos.service.PermisosPedidos;
import com.pac.modulos.reportes.service.PermisosReportes;
import com.pac.modulos.reportes.service.ReporteService;
import com.pac.navigation.ControladorSeccion;
import com.pac.navigation.Navegador;
import com.pac.navigation.SeccionPanel;
import com.pac.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del dashboard principal (dashboard.fxml).
 * Las alertas de inventario vienen del Módulo 5, los pedidos del Módulo 4
 * y los ingresos del mes del Módulo 6 (Reportes Financieros).
 */
public class DashboardController implements ControladorSeccion {

    private static final int ULTIMOS_PEDIDOS = 10;

    @FXML private Label lblPedidosHoy;
    @FXML private Label lblDetallePedidos;
    @FXML private TableView<FilaPedido> tblPedidos;
    @FXML private Label lblSinPedidos;
    @FXML private Label lblAyudaPedidos;
    @FXML private Label lblIngresos;
    @FXML private Label lblDetalleIngresos;
    @FXML private Label lblAlertas;
    @FXML private Label lblDetalleAlertas;

    @Override
    public void inicializarSeccion(SeccionPanel seccion) {
        mostrarIngresos();
        mostrarPedidos();
        mostrarAlertas();
    }

    // Ingresos del mes en curso (Módulo 6); solo con el permiso «Ver datos financieros»
    private void mostrarIngresos() {
        if (!PermisosReportes.puedeVerReportes()) {
            lblIngresos.setText("—");
            lblDetalleIngresos.setText("Sin permiso para ver datos financieros");
            return;
        }
        try {
            lblIngresos.setText(Formatos.moneda(new ReporteService().ingresosDelMes()));
            lblDetalleIngresos.setText("Pagos validados menos reembolsos, del 1 al "
                    + LocalDate.now().getDayOfMonth() + " de este mes");
        } catch (DAOException e) {
            lblIngresos.setText("—");
            lblDetalleIngresos.setText("Sin conexión a la base de datos");
        }
    }

    // Pedidos que se entregan hoy y los últimos que llegaron (Módulo 4)
    private void mostrarPedidos() {
        tblPedidos.getItems().clear();
        if (!PermisosPedidos.puedeGestionar() && !PermisosPedidos.puedeVerAgenda()) {
            lblPedidosHoy.setText("—");
            lblDetallePedidos.setText("Sin permiso para ver pedidos");
            lblSinPedidos.setText("No tienes permiso para ver los pedidos");
            lblAyudaPedidos.setVisible(false);
            return;
        }
        try {
            List<Pedido> hoy = new AgendaService().cargar(LocalDate.now(), LocalDate.now()).getPedidos();
            lblPedidosHoy.setText(String.valueOf(hoy.size()));
            lblDetallePedidos.setText(MonitorPedidos.getInstancia().getPorRevisar() + " solicitud(es) por revisar · "
                    + MonitorPedidos.getInstancia().getPagosPendientes() + " pago(s) por validar");

            if (PermisosPedidos.puedeGestionar()) {
                FiltroPedidos filtro = new FiltroPedidos();
                filtro.setGrupo(FiltroPedidos.ACTIVOS);
                List<Pedido> pedidos = new PedidoService().buscar(filtro);
                for (int i = 0; i < pedidos.size() && i < ULTIMOS_PEDIDOS; i++) {
                    tblPedidos.getItems().add(new FilaPedido(pedidos.get(i)));
                }
            } else {
                lblSinPedidos.setText("La lista de pedidos requiere el permiso «Gestionar pedidos»");
                lblAyudaPedidos.setVisible(false);
            }
        } catch (DAOException e) {
            lblPedidosHoy.setText("—");
            lblDetallePedidos.setText("Sin conexión a la base de datos");
            lblSinPedidos.setText("Error de conexión");
        }
    }

    // Doble clic: abre el pedido en Gestión de Pedidos
    @FXML
    private void onClicPedidos(MouseEvent evento) {
        FilaPedido fila = tblPedidos.getSelectionModel().getSelectedItem();
        if (fila == null || evento.getClickCount() != 2) {
            return;
        }
        PedidosController.abrirAlEntrar(fila.getPedidoCompleto().getFolio());
        Navegador.getInstancia().irASeccion(SeccionPanel.PEDIDOS);
    }

    // Alertas de inventario pendientes, separadas por tipo
    private void mostrarAlertas() {
        List<AlertaInventario> alertas;
        try {
            alertas = new AlertaService().listar(true);
        } catch (DAOException e) {
            lblAlertas.setText("—");
            lblDetalleAlertas.setText("Sin conexión a la base de datos");
            return;
        }
        int existencia = 0;
        int caducidad = 0;
        for (AlertaInventario alerta : alertas) {
            if (alerta.getTipo() == TipoAlerta.EXISTENCIA_BAJA) {
                existencia++;
            } else {
                caducidad++;
            }
        }
        lblAlertas.setText(String.valueOf(alertas.size()));
        if (alertas.isEmpty()) {
            lblDetalleAlertas.setText("Inventario en orden");
        } else {
            lblDetalleAlertas.setText(existencia + " de existencia baja · " + caducidad + " de caducidad");
        }
    }
}
