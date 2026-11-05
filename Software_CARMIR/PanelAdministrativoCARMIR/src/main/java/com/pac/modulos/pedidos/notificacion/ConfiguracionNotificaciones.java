package com.pac.modulos.pedidos.notificacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PATRÓN SINGLETON.
 * Guarda los datos de las notificaciones (modo simulado o real, WhatsApp y correo) que usa el panel.
 *
 * De dónde sale cada valor (en este orden):
 *   1. La tabla ConfiguracionNotificacion de la base de datos. Se edita en
 *      Configuración → Respaldos y sistema, e IntegracionesService la pasa aquí con aplicarValoresBD().
 *   2. Si la tabla no existe o no tiene el dato, config/notificaciones.properties (valores de fábrica).
 * El token de WhatsApp y la contraseña del correo solo se guardan en la base de datos (cifrados).
 */
public class ConfiguracionNotificaciones {

    private static final String ARCHIVO_CONFIG = "/com/pac/config/notificaciones.properties";

    private static ConfiguracionNotificaciones instancia;

    private Properties propiedades;       // valores de notificaciones.properties
    private DatosNotificaciones datos;    // valores que se están usando
    private boolean usaValoresBD = false;

    private ConfiguracionNotificaciones() {
        propiedades = new Properties();
        try {
            InputStream archivo = ConfiguracionNotificaciones.class.getResourceAsStream(ARCHIVO_CONFIG);
            if (archivo != null) {
                propiedades.load(archivo);
                archivo.close();
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ARCHIVO_CONFIG);
        }
        datos = leerArchivo();
    }

    public static synchronized ConfiguracionNotificaciones getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionNotificaciones();
        }
        return instancia;
    }

    /**
     * Reemplaza los datos con los de la tabla ConfiguracionNotificacion (Clave → Valor, ya descifrados).
     * Lo que no venga en la tabla se toma del archivo .properties.
     */
    public synchronized void aplicarValoresBD(Properties valores) {
        DatosNotificaciones nuevos = leerArchivo();
        nuevos.setModo(valores.getProperty(DatosNotificaciones.CLAVE_MODO, nuevos.getModo()));
        nuevos.setReintentos(convertirEntero(valores.getProperty(DatosNotificaciones.CLAVE_REINTENTOS),
                nuevos.getReintentos()));
        nuevos.setSegundosEntreReintentos(convertirEntero(
                valores.getProperty(DatosNotificaciones.CLAVE_SEGUNDOS_REINTENTO), nuevos.getSegundosEntreReintentos()));
        nuevos.setWhatsAppIdNumero(valores.getProperty(DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO,
                nuevos.getWhatsAppIdNumero()));
        nuevos.setWhatsAppToken(valores.getProperty(DatosNotificaciones.CLAVE_WHATSAPP_TOKEN, ""));
        nuevos.setWhatsAppVersionApi(valores.getProperty(DatosNotificaciones.CLAVE_WHATSAPP_VERSION,
                nuevos.getWhatsAppVersionApi()));
        nuevos.setWhatsAppLada(valores.getProperty(DatosNotificaciones.CLAVE_WHATSAPP_LADA, nuevos.getWhatsAppLada()));
        nuevos.setSmtpServidor(valores.getProperty(DatosNotificaciones.CLAVE_SMTP_SERVIDOR, nuevos.getSmtpServidor()));
        nuevos.setSmtpPuerto(convertirEntero(valores.getProperty(DatosNotificaciones.CLAVE_SMTP_PUERTO),
                nuevos.getSmtpPuerto()));
        nuevos.setSmtpSeguridad(valores.getProperty(DatosNotificaciones.CLAVE_SMTP_SEGURIDAD,
                nuevos.getSmtpSeguridad()));
        nuevos.setSmtpUsuario(valores.getProperty(DatosNotificaciones.CLAVE_SMTP_USUARIO, nuevos.getSmtpUsuario()));
        nuevos.setSmtpContrasena(valores.getProperty(DatosNotificaciones.CLAVE_SMTP_CONTRASENA, ""));
        nuevos.setSmtpRemitente(valores.getProperty(DatosNotificaciones.CLAVE_SMTP_REMITENTE,
                nuevos.getSmtpRemitente()));
        // Si la clave no existe todavía (base sin actualizar) el canal queda habilitado
        nuevos.setWhatsAppHabilitado(!"0".equals(valores.getProperty(DatosNotificaciones.CLAVE_WHATSAPP_HABILITADO, "1")));
        nuevos.setCorreoHabilitado(!"0".equals(valores.getProperty(DatosNotificaciones.CLAVE_CORREO_HABILITADO, "1")));
        datos = nuevos;
        usaValoresBD = true;
    }

    // Copia de los datos actuales (los canales la usan para enviar)
    public synchronized DatosNotificaciones getDatos() {
        return datos.copiar();
    }

    // true si los datos vienen de la base de datos y no del archivo
    public synchronized boolean usaValoresBD() {
        return usaValoresBD;
    }

    // true = no se envía nada de verdad (se escribe en la consola)
    public synchronized boolean isSimulado() {
        return datos.isSimulado();
    }

    public synchronized int getReintentos() {
        return datos.getReintentos();
    }

    public synchronized int getSegundosEntreReintentos() {
        return datos.getSegundosEntreReintentos();
    }

    // ===================== Apoyo =====================

    // Valores de fábrica escritos en notificaciones.properties (sin secretos)
    private DatosNotificaciones leerArchivo() {
        DatosNotificaciones valores = new DatosNotificaciones();
        valores.setModo(leer("notificaciones.modo", valores.getModo()));
        valores.setReintentos(convertirEntero(leer("notificaciones.reintentos", ""), valores.getReintentos()));
        valores.setSegundosEntreReintentos(convertirEntero(leer("notificaciones.segundosEntreReintentos", ""),
                valores.getSegundosEntreReintentos()));
        valores.setWhatsAppIdNumero(leer("whatsapp.idNumeroTelefono", valores.getWhatsAppIdNumero()));
        valores.setWhatsAppVersionApi(leer("whatsapp.versionApi", valores.getWhatsAppVersionApi()));
        valores.setWhatsAppLada(leer("whatsapp.lada", valores.getWhatsAppLada()));
        valores.setSmtpServidor(leer("smtp.servidor", valores.getSmtpServidor()));
        valores.setSmtpPuerto(convertirEntero(leer("smtp.puerto", ""), valores.getSmtpPuerto()));
        valores.setSmtpSeguridad(leer("smtp.seguridad", valores.getSmtpSeguridad()));
        valores.setSmtpUsuario(leer("smtp.usuario", valores.getSmtpUsuario()));
        valores.setSmtpRemitente(leer("smtp.remitente", valores.getSmtpRemitente()));
        return valores;
    }

    private String leer(String clave, String porDefecto) {
        return propiedades.getProperty(clave, porDefecto).trim();
    }

    // Convierte el texto a número entero no negativo; si no se puede, devuelve el valor por defecto
    private int convertirEntero(String texto, int porDefecto) {
        if (texto == null) {
            return porDefecto;
        }
        try {
            int numero = Integer.parseInt(texto.trim());
            if (numero < 0) {
                return porDefecto;
            }
            return numero;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
