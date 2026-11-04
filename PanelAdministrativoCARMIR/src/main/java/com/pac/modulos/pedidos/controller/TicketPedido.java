package com.pac.modulos.pedidos.controller;

import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.modulos.pedidos.model.ModalidadEntrega;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

/**
 * RF-48 · Paso 2: arma el ticket de cocina de un pedido para imprimirlo (botón «Imprimir ticket»).
 */
public class TicketPedido {

    private static final double ANCHO = 280;

    // Clase de apoyo: no se crean objetos de ella
    private TicketPedido() {
    }

    public static VBox crear(Pedido pedido) {
        VBox ticket = new VBox(4);
        ticket.setPrefWidth(ANCHO);
        ticket.setMaxWidth(ANCHO);
        ticket.getStylesheets().add(TicketPedido.class.getResource("/com/pac/css/pedidos.css").toExternalForm());
        ticket.getStyleClass().add("ticket");

        agregar(ticket, ConfiguracionApp.getInstancia().getNombreNegocio(), "ticket-titulo");
        agregar(ticket, "Ticket de cocina · Pedido #" + pedido.getFolio(), "ticket-titulo");
        agregar(ticket, "Estado: " + pedido.getTextoEstado(), null);
        String hora = "";
        if (pedido.getHorarioEntrega() != null) {
            hora = " " + pedido.getHorarioEntrega().toString();
        }
        agregar(ticket, "Entrega: " + Formatos.fecha(pedido.getFechaEntrega()) + hora, null);
        agregar(ticket, "Cliente: " + pedido.getCliente().getNombre() + " · " + pedido.getCliente().getTelefono(), null);
        if (pedido.getModalidadEntrega() == ModalidadEntrega.DOMICILIO && pedido.getDireccion() != null) {
            agregar(ticket, "Envío a: " + pedido.getDireccion().getTexto(), null);
        } else {
            agregar(ticket, "Recoger en tienda", null);
        }
        ticket.getChildren().add(new Separator());

        for (LineaPedido linea : pedido.getLineas()) {
            agregar(ticket, linea.getCantidad() + " × " + linea.getNombreMostrar(), "ticket-titulo");
            for (String especificacion : linea.getEspecificaciones()) {
                agregar(ticket, "   - " + especificacion, null);
            }
            if (linea.getDedicatoria() != null && !linea.getDedicatoria().isBlank()) {
                agregar(ticket, "   Dedicatoria: \"" + linea.getDedicatoria() + "\"", null);
            }
            if (linea.getComentario() != null && !linea.getComentario().isBlank()) {
                agregar(ticket, "   Nota: " + linea.getComentario(), null);
            }
        }
        ticket.getChildren().add(new Separator());
        // Pedidos de la página web: el envío viene incluido en el total
        if (pedido.getCostoEnvio() > 0) {
            agregar(ticket, "Envío a domicilio: " + Formatos.moneda(pedido.getCostoEnvio()), null);
        }
        agregar(ticket, "Total: " + Formatos.moneda(pedido.getMontoTotal()) + " · Pagado: "
                + Formatos.moneda(pedido.getTotalPagado()) + " · Saldo: " + Formatos.moneda(pedido.getSaldo()), null);
        agregar(ticket, "Impreso: " + Formatos.fechaHoraCorta(LocalDateTime.now()), null);
        return ticket;
    }

    private static void agregar(VBox ticket, String texto, String clase) {
        Label linea = new Label(texto);
        linea.setWrapText(true);
        linea.setMaxWidth(ANCHO);
        if (clase != null) {
            linea.getStyleClass().add(clase);
        }
        ticket.getChildren().add(linea);
    }
}
