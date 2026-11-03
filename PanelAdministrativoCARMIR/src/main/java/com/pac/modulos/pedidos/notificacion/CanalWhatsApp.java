package com.pac.modulos.pedidos.notificacion;

import com.pac.modulos.pedidos.model.Notificacion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envía el aviso por WhatsApp Business Cloud API (Meta) con el HttpClient de Java.
 *
 * Nota: WhatsApp solo permite texto libre si el cliente escribió al negocio en las últimas 24 horas;
 * fuera de esa ventana Meta pide usar plantillas aprobadas. Si Meta rechaza el mensaje,
 * el aviso queda "Fallido" con el motivo y se puede reenviar desde el detalle del pedido.
 */
public class CanalWhatsApp implements CanalEnvio {

    private static final String URL_API = "https://graph.facebook.com/";

    // Plantilla que Meta crea en todas las cuentas nuevas; sirve para probar fuera de la ventana de 24 h
    public static final String PLANTILLA_PRUEBA = "hello_world";

    private HttpClient cliente;
    private DatosNotificaciones config;

    // Usa los datos guardados en la configuración del panel
    public CanalWhatsApp() {
        this(ConfiguracionNotificaciones.getInstancia().getDatos());
    }

    // Usa los datos indicados (por ejemplo, los del formulario al enviar una prueba)
    public CanalWhatsApp(DatosNotificaciones config) {
        this.config = config;
        cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String getNombre() {
        return Notificacion.CANAL_WHATSAPP;
    }

    // RF-32 · Paso 6: envía el aviso como texto libre por WhatsApp Business Cloud API.
    @Override
    public void enviar(String destino, String asunto, String mensaje) throws ErrorEnvio {
        String cuerpo = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + numeroCompleto(destino) + "\","
                + "\"type\":\"text\",\"text\":{\"body\":\"" + escaparJson(PlantillasMensaje.textoWhatsApp(asunto, mensaje)) + "\"}}";
        mandar(cuerpo);
    }

    // Prueba desde Configuración: manda la plantilla hello_world (en inglés), que Meta deja enviar
    // aunque el cliente no haya escrito antes al negocio.
    public void enviarPrueba(String destino) throws ErrorEnvio {
        String cuerpo = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + numeroCompleto(destino) + "\","
                + "\"type\":\"template\",\"template\":{\"name\":\"" + PLANTILLA_PRUEBA + "\","
                + "\"language\":{\"code\":\"en_US\"}}}";
        mandar(cuerpo);
    }

    // Hace la petición POST a la API de Meta con el token en el encabezado
    private void mandar(String cuerpo) throws ErrorEnvio {
        if (config.getWhatsAppIdNumero().isEmpty() || config.getWhatsAppToken().isEmpty()) {
            throw new ErrorEnvio("WhatsApp no está configurado (Configuración → Respaldos y sistema)", false);
        }
        HttpRequest peticion;
        try {
            peticion = HttpRequest.newBuilder()
                    .uri(URI.create(URL_API + config.getWhatsAppVersionApi() + "/" + config.getWhatsAppIdNumero()
                            + "/messages"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + config.getWhatsAppToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();
        } catch (IllegalArgumentException e) {
            // Pasa si el ID del número o la versión tienen espacios u otros caracteres no válidos
            throw new ErrorEnvio("El ID del número o la versión de la API de WhatsApp no son válidos", false);
        }
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                // La respuesta de Meta trae el motivo del error (no incluye el token)
                throw new ErrorEnvio("WhatsApp respondió " + respuesta.statusCode() + ": " + respuesta.body(), false);
            }
        } catch (IOException e) {
            throw new ErrorEnvio("Sin conexión con WhatsApp: " + e.getMessage(), true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorEnvio("Se interrumpió el envío por WhatsApp", true);
        }
    }

    // Número internacional: lada + 10 dígitos, sin signos ni espacios
    private String numeroCompleto(String destino) {
        String digitos = "";
        if (destino != null) {
            for (int i = 0; i < destino.length(); i++) {
                char letra = destino.charAt(i);
                if (letra >= '0' && letra <= '9') {
                    digitos = digitos + letra;
                }
            }
        }
        // Si ya trae la lada (más de 10 dígitos) se deja como está
        if (digitos.length() > 10) {
            return digitos;
        }
        return config.getWhatsAppLada() + digitos;
    }

    // Escapa comillas, diagonales y saltos de línea para meter el texto en el JSON
    private String escaparJson(String texto) {
        String resultado = "";
        for (int i = 0; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            if (letra == '"') {
                resultado = resultado + "\\\"";
            } else if (letra == '\\') {
                resultado = resultado + "\\\\";
            } else if (letra == '\n') {
                resultado = resultado + "\\n";
            } else if (letra == '\r') {
                resultado = resultado + "";
            } else {
                resultado = resultado + letra;
            }
        }
        return resultado;
    }
}
