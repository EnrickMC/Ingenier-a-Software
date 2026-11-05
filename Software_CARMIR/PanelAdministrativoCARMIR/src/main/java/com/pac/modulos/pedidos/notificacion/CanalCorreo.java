package com.pac.modulos.pedidos.notificacion;

import com.pac.modulos.pedidos.model.Notificacion;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.util.Properties;

/**
 * Envía el aviso por correo electrónico (SMTP) con Jakarta Mail.
 * Los datos del servidor se escriben en Configuración → Respaldos y sistema.
 */
public class CanalCorreo implements CanalEnvio {

    private DatosNotificaciones config;

    // Usa los datos guardados en la configuración del panel
    public CanalCorreo() {
        this(ConfiguracionNotificaciones.getInstancia().getDatos());
    }

    // Usa los datos indicados (por ejemplo, los del formulario al enviar una prueba)
    public CanalCorreo(DatosNotificaciones config) {
        this.config = config;
    }

    @Override
    public String getNombre() {
        return Notificacion.CANAL_CORREO;
    }

    @Override
    public void enviar(String destino, String asunto, String mensaje) throws ErrorEnvio {
        if (config.getSmtpServidor().isEmpty() || config.getSmtpUsuario().isEmpty()
                || config.getSmtpContrasena().isEmpty()) {
            throw new ErrorEnvio("El correo no está configurado (Configuración → Respaldos y sistema)", false);
        }

        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.host", config.getSmtpServidor());
        propiedades.put("mail.smtp.port", String.valueOf(config.getSmtpPuerto()));
        propiedades.put("mail.smtp.auth", "true");
        // SSL: conexión cifrada desde el inicio (puerto 465). STARTTLS: se cifra después de conectar (puerto 587).
        if (DatosNotificaciones.SEGURIDAD_SSL.equalsIgnoreCase(config.getSmtpSeguridad())) {
            propiedades.put("mail.smtp.ssl.enable", "true");
        } else {
            propiedades.put("mail.smtp.starttls.enable", "true");
            propiedades.put("mail.smtp.starttls.required", "true");
        }
        propiedades.put("mail.smtp.connectiontimeout", "10000");
        propiedades.put("mail.smtp.timeout", "20000");

        Session sesion = Session.getInstance(propiedades, new AutenticadorSmtp(config.getSmtpUsuario(),
                config.getSmtpContrasena()));
        try {
            MimeMessage correo = new MimeMessage(sesion);
            correo.setFrom(new InternetAddress(config.getRemitenteEfectivo()));
            correo.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
            correo.setSubject(asunto, "UTF-8");
            // Texto sencillo + versión HTML con diseño; el lector de correo elige la que pueda mostrar
            MimeMultipart partes = new MimeMultipart("alternative");
            MimeBodyPart parteTexto = new MimeBodyPart();
            parteTexto.setText(mensaje, "UTF-8");
            MimeBodyPart parteHtml = new MimeBodyPart();
            parteHtml.setContent(PlantillasMensaje.html(asunto, mensaje), "text/html; charset=UTF-8");
            partes.addBodyPart(parteTexto);
            partes.addBodyPart(parteHtml);
            correo.setContent(partes);
            Transport.send(correo);
        } catch (AuthenticationFailedException e) {
            // La contraseña no se escribe en el mensaje, solo el motivo que da el servidor
            throw new ErrorEnvio("El servidor de correo rechazó el usuario o la contraseña: " + e.getMessage(), false);
        } catch (SendFailedException e) {
            throw new ErrorEnvio("El servidor de correo no aceptó el mensaje: " + e.getMessage(), false);
        } catch (AddressException e) {
            throw new ErrorEnvio("La dirección de correo no es válida: " + e.getMessage(), false);
        } catch (MessagingException e) {
            throw new ErrorEnvio("Sin conexión con el servidor de correo: " + e.getMessage(), true);
        }
    }

    // Usuario y contraseña que pide el servidor SMTP
    private static class AutenticadorSmtp extends Authenticator {

        private String usuario;
        private String contrasena;

        AutenticadorSmtp(String usuario, String contrasena) {
            this.usuario = usuario;
            this.contrasena = contrasena;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(usuario, contrasena);
        }
    }
}
