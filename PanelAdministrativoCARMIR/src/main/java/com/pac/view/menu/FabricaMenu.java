package com.pac.view.menu;

import com.pac.modulos.inventario.service.MonitorAlertas;
import com.pac.modulos.pedidos.service.MonitorPedidos;
import com.pac.navigation.SeccionPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma la lista de botones del menú lateral y les aplica los decoradores.
 */
public class FabricaMenu {

    public static List<ElementoMenu> crearMenu() {
        List<ElementoMenu> elementos = new ArrayList<ElementoMenu>();

        for (SeccionPanel seccion : SeccionPanel.values()) {
            ElementoMenu elemento = new ElementoMenuBase(seccion);

            // Contadores del menú
            if (seccion == SeccionPanel.PEDIDOS) {
                // Solicitudes «En proceso de aceptación» por revisar (Módulo 4, las cuenta MonitorPedidos)
                int pedidosNuevos = MonitorPedidos.getInstancia().getPorRevisar();
                elemento = new DecoradorInsignia(elemento, pedidosNuevos, "insignia-info");
            } else if (seccion == SeccionPanel.PAGOS) {
                // Pagos que subieron los clientes y nadie ha validado
                int pagosPendientes = MonitorPedidos.getInstancia().getPagosPendientes();
                elemento = new DecoradorInsignia(elemento, pagosPendientes, "insignia-info");
            } else if (seccion == SeccionPanel.INVENTARIO) {
                // Alertas de inventario pendientes (Módulo 5, las cuenta MonitorAlertas)
                int alertasInventario = MonitorAlertas.getInstancia().getPendientes();
                elemento = new DecoradorInsignia(elemento, alertasInventario, "insignia-alerta");
            }

            // Todos los botones revisan si el usuario tiene permiso
            elemento = new DecoradorPermiso(elemento);
            elementos.add(elemento);
        }
        return elementos;
    }
}
