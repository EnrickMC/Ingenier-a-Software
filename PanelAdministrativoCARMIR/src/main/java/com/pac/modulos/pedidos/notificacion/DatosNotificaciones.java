package com.pac.modulos.pedidos.notificacion;

/**
 * Datos de las integraciones de notificaciones (RF-32): modo, WhatsApp Business Cloud API y correo SMTP.
 *
 * Los valores se guardan en la tabla ConfiguracionNotificacion (Clave → Valor) y se editan en
 * Configuración → Respaldos y sistema. Las constantes CLAVE_* son los nombres de la columna Clave.
 * El token y la contraseña se guardan cifrados (ver util/CifradoSecretos).
 */
public class DatosNotificaciones {

    // ----- Nombres de las claves en la tabla ConfiguracionNotificacion -----
    public static final String CLAVE_MODO = "Modo";
    public static final String CLAVE_REINTENTOS = "Reintentos";
    public static final String CLAVE_SEGUNDOS_REINTENTO = "SegundosEntreReintentos";
    public static final String CLAVE_WHATSAPP_ID_NUMERO = "WhatsAppIdNumero";
    public static final String CLAVE_WHATSAPP_TOKEN = "WhatsAppToken";
    public static final String CLAVE_WHATSAPP_VERSION = "WhatsAppVersionApi";
    public static final String CLAVE_WHATSAPP_LADA = "WhatsAppLada";
    public static final String CLAVE_SMTP_SERVIDOR = "SmtpServidor";
    public static final String CLAVE_SMTP_PUERTO = "SmtpPuerto";
    public static final String CLAVE_SMTP_SEGURIDAD = "SmtpSeguridad";
    public static final String CLAVE_SMTP_USUARIO = "SmtpUsuario";
    public static final String CLAVE_SMTP_CONTRASENA = "SmtpContrasena";
    public static final String CLAVE_SMTP_REMITENTE = "SmtpRemitente";
    // "1" = el canal está habilitado; "0" = apagado (no se intenta enviar nada por ese canal)
    public static final String CLAVE_WHATSAPP_HABILITADO = "WhatsAppHabilitado";
    public static final String CLAVE_CORREO_HABILITADO = "CorreoHabilitado";

    // Claves que son secretas (se cifran y nunca se muestran)
    public static final String[] CLAVES_SECRETAS = {CLAVE_WHATSAPP_TOKEN, CLAVE_SMTP_CONTRASENA};

    // ----- Valores posibles -----
    public static final String MODO_SIMULADO = "simulado";
    public static final String MODO_REAL = "real";
    public static final String SEGURIDAD_STARTTLS = "STARTTLS";
    public static final String SEGURIDAD_SSL = "SSL";

    private String modo = MODO_SIMULADO;
    private int reintentos = 3;
    private int segundosEntreReintentos = 20;

    private String whatsAppIdNumero = "";
    private String whatsAppToken = "";
    private String whatsAppVersionApi = "v20.0";
    private String whatsAppLada = "52";

    private String smtpServidor = "smtp.gmail.com";
    private int smtpPuerto = 587;
    private String smtpSeguridad = SEGURIDAD_STARTTLS;
    private String smtpUsuario = "";
    private String smtpContrasena = "";
    private String smtpRemitente = "";

    private boolean whatsAppHabilitado = true;
    private boolean correoHabilitado = true;

    // Copia con los mismos valores (para no modificar la configuración que está usando el panel)
    public DatosNotificaciones copiar() {
        DatosNotificaciones copia = new DatosNotificaciones();
        copia.modo = modo;
        copia.reintentos = reintentos;
        copia.segundosEntreReintentos = segundosEntreReintentos;
        copia.whatsAppIdNumero = whatsAppIdNumero;
        copia.whatsAppToken = whatsAppToken;
        copia.whatsAppVersionApi = whatsAppVersionApi;
        copia.whatsAppLada = whatsAppLada;
        copia.smtpServidor = smtpServidor;
        copia.smtpPuerto = smtpPuerto;
        copia.smtpSeguridad = smtpSeguridad;
        copia.smtpUsuario = smtpUsuario;
        copia.smtpContrasena = smtpContrasena;
        copia.smtpRemitente = smtpRemitente;
        copia.whatsAppHabilitado = whatsAppHabilitado;
        copia.correoHabilitado = correoHabilitado;
        return copia;
    }

    // true = no se envía nada de verdad (se escribe en la consola)
    public boolean isSimulado() {
        return !MODO_REAL.equalsIgnoreCase(modo);
    }

    // true si WhatsApp está habilitado y listo para usarse (en modo real necesita su ID y su token)
    public boolean isWhatsAppActivo() {
        if (!whatsAppHabilitado) {
            return false;
        }
        return isSimulado() || (!whatsAppIdNumero.isEmpty() && !whatsAppToken.isEmpty());
    }

    // true si el correo está habilitado y listo para usarse (en modo real necesita servidor, usuario y contraseña)
    public boolean isCorreoActivo() {
        if (!correoHabilitado) {
            return false;
        }
        return isSimulado() || (!smtpServidor.isEmpty() && !smtpUsuario.isEmpty() && !smtpContrasena.isEmpty());
    }

    // Correo que aparece como remitente; si está vacío se usa el usuario
    public String getRemitenteEfectivo() {
        if (smtpRemitente.isEmpty()) {
            return smtpUsuario;
        }
        return smtpRemitente;
    }

    // ===================== Getters y setters =====================

    public boolean isWhatsAppHabilitado() {
        return whatsAppHabilitado;
    }

    public void setWhatsAppHabilitado(boolean whatsAppHabilitado) {
        this.whatsAppHabilitado = whatsAppHabilitado;
    }

    public boolean isCorreoHabilitado() {
        return correoHabilitado;
    }

    public void setCorreoHabilitado(boolean correoHabilitado) {
        this.correoHabilitado = correoHabilitado;
    }

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = limpiar(modo);
    }

    public int getReintentos() {
        return reintentos;
    }

    public void setReintentos(int reintentos) {
        this.reintentos = reintentos;
    }

    public int getSegundosEntreReintentos() {
        return segundosEntreReintentos;
    }

    public void setSegundosEntreReintentos(int segundosEntreReintentos) {
        this.segundosEntreReintentos = segundosEntreReintentos;
    }

    public String getWhatsAppIdNumero() {
        return whatsAppIdNumero;
    }

    public void setWhatsAppIdNumero(String whatsAppIdNumero) {
        this.whatsAppIdNumero = limpiar(whatsAppIdNumero);
    }

    public String getWhatsAppToken() {
        return whatsAppToken;
    }

    public void setWhatsAppToken(String whatsAppToken) {
        this.whatsAppToken = limpiar(whatsAppToken);
    }

    public String getWhatsAppVersionApi() {
        return whatsAppVersionApi;
    }

    public void setWhatsAppVersionApi(String whatsAppVersionApi) {
        this.whatsAppVersionApi = limpiar(whatsAppVersionApi);
    }

    public String getWhatsAppLada() {
        return whatsAppLada;
    }

    public void setWhatsAppLada(String whatsAppLada) {
        this.whatsAppLada = limpiar(whatsAppLada);
    }

    public String getSmtpServidor() {
        return smtpServidor;
    }

    public void setSmtpServidor(String smtpServidor) {
        this.smtpServidor = limpiar(smtpServidor);
    }

    public int getSmtpPuerto() {
        return smtpPuerto;
    }

    public void setSmtpPuerto(int smtpPuerto) {
        this.smtpPuerto = smtpPuerto;
    }

    public String getSmtpSeguridad() {
        return smtpSeguridad;
    }

    public void setSmtpSeguridad(String smtpSeguridad) {
        this.smtpSeguridad = limpiar(smtpSeguridad);
    }

    public String getSmtpUsuario() {
        return smtpUsuario;
    }

    public void setSmtpUsuario(String smtpUsuario) {
        this.smtpUsuario = limpiar(smtpUsuario);
    }

    public String getSmtpContrasena() {
        return smtpContrasena;
    }

    public void setSmtpContrasena(String smtpContrasena) {
        this.smtpContrasena = limpiar(smtpContrasena);
    }

    public String getSmtpRemitente() {
        return smtpRemitente;
    }

    public void setSmtpRemitente(String smtpRemitente) {
        this.smtpRemitente = limpiar(smtpRemitente);
    }

    // Quita espacios al inicio y al final; null se vuelve texto vacío
    private String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim();
    }
}
