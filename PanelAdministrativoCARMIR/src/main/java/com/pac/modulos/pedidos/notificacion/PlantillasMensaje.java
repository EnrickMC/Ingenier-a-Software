package com.pac.modulos.pedidos.notificacion;

import com.pac.modulos.pedidos.model.Pedido;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;

/**
 * Textos de los avisos al cliente, uno por tipo de evento (RF-32).
 * Cada mensaje lleva el folio, el estado y, según el caso, montos o motivo.
 */
public class PlantillasMensaje {

    // Tipos de evento (se guardan en Notificacion.TipoEvento)
    public static final String APROBACION = "Pedido aprobado";
    public static final String MODIFICACION = "Pedido modificado";
    public static final String CANCELACION = "Pedido cancelado";
    public static final String BAJA = "Solicitud dada de baja";
    public static final String PAGO_VALIDADO = "Pago validado";
    public static final String PAGO_RECHAZADO = "Pago rechazado";
    public static final String CAMBIO_ESTADO = "Cambio de estado";

    // Clase de apoyo: no se crean objetos de ella
    private PlantillasMensaje() {
    }

    // Marca de las líneas de datos del mensaje: "• Etiqueta: valor"
    private static final String VINETA = "• ";

    // Asunto del correo (y primera línea del WhatsApp): negocio, folio y tipo de evento
    public static String asunto(String tipoEvento, Pedido pedido) {
        return ConfiguracionApp.getInstancia().getNombreNegocio() + " · Folio #" + pedido.getFolio() + " · " + tipoEvento;
    }

    // Cuerpo del mensaje en texto sencillo. Las líneas que empiezan con "• " son datos (Folio, Total...):
    // el correo las dibuja como tabla y WhatsApp las pone con negritas. detalle es el motivo o dato extra (puede ser null)
    public static String mensaje(String tipoEvento, Pedido pedido, String detalle) {
        String saludo = "Hola " + pedido.getCliente().getNombre() + ",\n";
        String datoFolio = dato("Folio", "#" + pedido.getFolio());
        String texto;

        if (APROBACION.equals(tipoEvento)) {
            texto = "¡Tu pedido fue aprobado!\n\n" + datoFolio
                    + dato("Entrega", Formatos.fecha(pedido.getFechaEntrega()) + " a las " + horario(pedido))
                    + dato("Total", Formatos.moneda(pedido.getMontoTotal()))
                    + dato("Para iniciar la preparación", Formatos.moneda(pedido.getMontoParaIniciar()) + " ("
                            + pedido.getModalidadPago().toString().toLowerCase() + ")")
                    + "\nPuedes pagar en sucursal o por transferencia/depósito y subir tu comprobante en el portal.";
        } else if (MODIFICACION.equals(tipoEvento)) {
            texto = "Tu pedido fue modificado.\n" + textoDetalle(detalle) + "\n" + datoFolio
                    + dato("Nuevo total", Formatos.moneda(pedido.getMontoTotal()))
                    + dato("Entrega", Formatos.fecha(pedido.getFechaEntrega()) + " a las " + horario(pedido));
        } else if (CANCELACION.equals(tipoEvento)) {
            texto = "Tu pedido fue cancelado.\n\n" + datoFolio + dato("Motivo", detalle);
        } else if (BAJA.equals(tipoEvento)) {
            texto = "Tu solicitud fue dada de baja y no se procesará.\n\n" + datoFolio + dato("Motivo", detalle);
        } else if (PAGO_VALIDADO.equals(tipoEvento)) {
            texto = "Confirmamos tu pago.\n\n" + datoFolio
                    + dato("Pago confirmado", detalle)
                    + dato("Total pagado", Formatos.moneda(pedido.getTotalPagado()))
                    + dato("Saldo pendiente", Formatos.moneda(pedido.getSaldo()));
        } else if (PAGO_RECHAZADO.equals(tipoEvento)) {
            texto = "No pudimos validar tu pago.\n\n" + datoFolio + dato("Motivo", detalle)
                    + "\nPor favor envía un comprobante válido o comunícate con nosotros.";
        } else {
            texto = "Tu pedido cambió de estado.\n\n" + datoFolio + dato("Estado", String.valueOf(pedido.getEstado()));
            if (detalle != null && !detalle.isBlank()) {
                texto = texto + "\n" + detalle;
            }
        }
        return saludo + texto + "\n\nGracias por tu preferencia.";
    }

    // Una línea de datos: "• Etiqueta: valor"
    private static String dato(String etiqueta, String valor) {
        return VINETA + etiqueta + ": " + valor + "\n";
    }

    // Texto para WhatsApp: encabezado con el negocio y el folio, y los datos en negritas (*texto*)
    public static String textoWhatsApp(String asunto, String mensaje) {
        String negocio = ConfiguracionApp.getInstancia().getNombreNegocio();
        String subtitulo = asunto;
        if (asunto.startsWith(negocio + " · ")) {
            subtitulo = asunto.substring(negocio.length() + 3);
        }
        String resultado = "*" + negocio + "*\n_" + subtitulo + "_\n\n";
        String[] lineas = mensaje.split("\n");
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            if (linea.startsWith(VINETA)) {
                int corte = linea.indexOf(": ");
                if (corte > 0) {
                    linea = VINETA + "*" + linea.substring(VINETA.length(), corte) + ":*" + linea.substring(corte + 1);
                }
            }
            resultado = resultado + linea + "\n";
        }
        return resultado.trim();
    }

    // Correo en HTML sencillo (tablas y estilos en línea, que es lo que aceptan los lectores de correo)
    public static String html(String asunto, String mensaje) {
        String negocio = ConfiguracionApp.getInstancia().getNombreNegocio();
        String subtitulo = asunto;
        if (asunto.startsWith(negocio + " · ")) {
            subtitulo = asunto.substring(negocio.length() + 3);
        }

        String cuerpo = "";
        String[] lineas = mensaje.split("\n");
        boolean enDatos = false;
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            boolean esDato = linea.startsWith(VINETA);
            // Se abre o se cierra la tabla de datos cuando cambia el tipo de línea
            if (esDato && !enDatos) {
                cuerpo = cuerpo + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                        + "style=\"background:#fdf6fa;border:1px solid #f1d9e6;border-radius:8px;margin:14px 0;"
                        + "border-collapse:separate;padding:6px 14px;\">";
                enDatos = true;
            }
            if (!esDato && enDatos) {
                cuerpo = cuerpo + "</table>";
                enDatos = false;
            }

            if (esDato) {
                String texto = linea.substring(VINETA.length());
                String etiqueta = texto;
                String valor = "";
                int corte = texto.indexOf(": ");
                if (corte > 0) {
                    etiqueta = texto.substring(0, corte);
                    valor = texto.substring(corte + 2);
                }
                cuerpo = cuerpo + "<tr><td style=\"padding:7px 14px;color:#7a6b88;font-size:13px;width:42%;\">"
                        + escaparHtml(etiqueta) + "</td><td style=\"padding:7px 14px;color:#2f2a3d;font-size:14px;"
                        + "font-weight:bold;\">" + escaparHtml(valor) + "</td></tr>";
            } else if (!linea.isBlank()) {
                cuerpo = cuerpo + "<p style=\"margin:0 0 10px 0;font-size:15px;line-height:1.5;color:#2f2a3d;\">"
                        + escaparHtml(linea) + "</p>";
            }
        }
        if (enDatos) {
            cuerpo = cuerpo + "</table>";
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f5eff7;font-family:Segoe UI,Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f5eff7;padding:24px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:560px;width:100%;background:#ffffff;border-radius:12px;overflow:hidden;"
                + "border:1px solid #e6dcef;\">"
                + "<tr><td style=\"background:#7d6bb8;padding:22px 28px;\">"
                + "<div style=\"color:#ffffff;font-size:22px;font-weight:bold;letter-spacing:1px;\">" + escaparHtml(negocio) + "</div>"
                + "<div style=\"color:#f7d9e8;font-size:14px;margin-top:4px;\">" + escaparHtml(subtitulo) + "</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:24px 28px 8px 28px;\">" + cuerpo + "</td></tr>"
                + "<tr><td style=\"padding:14px 28px 22px 28px;border-top:1px solid #f0e8f5;color:#9a8fad;font-size:12px;\">"
                + "Este mensaje se envió automáticamente. Si tienes dudas, responde a este correo o comunícate con "
                + escaparHtml(negocio) + "."
                + "</td></tr></table></td></tr></table></body></html>";
    }

    // Cambia los caracteres especiales del HTML por su código
    private static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        String resultado = "";
        for (int i = 0; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            if (letra == '&') {
                resultado = resultado + "&amp;";
            } else if (letra == '<') {
                resultado = resultado + "&lt;";
            } else if (letra == '>') {
                resultado = resultado + "&gt;";
            } else if (letra == '"') {
                resultado = resultado + "&quot;";
            } else {
                resultado = resultado + letra;
            }
        }
        return resultado;
    }

    private static String horario(Pedido pedido) {
        if (pedido.getHorarioEntrega() == null) {
            return "—";
        }
        String texto = pedido.getHorarioEntrega().toString();
        if (texto.length() > 5) {
            texto = texto.substring(0, 5);
        }
        return texto;
    }

    private static String textoDetalle(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return "";
        }
        return detalle + "\n";
    }
}
