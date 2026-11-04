package com.pac.modulos.configuracion.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.configuracion.dao.ConfiguracionDAO;
import com.pac.modulos.configuracion.dao.IntegracionesDAO;
import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.pedidos.notificacion.CanalCorreo;
import com.pac.modulos.pedidos.notificacion.CanalWhatsApp;
import com.pac.modulos.pedidos.notificacion.ConfiguracionNotificaciones;
import com.pac.modulos.pedidos.notificacion.DatosNotificaciones;
import com.pac.modulos.pedidos.notificacion.ErrorEnvio;
import com.pac.util.CifradoSecretos;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RF-71 · Pasos 3 a 6: integraciones de notificaciones, datos de WhatsApp Business Cloud API y del correo SMTP
 * con los que se envían los avisos de RF-47.
 *
 * - Los lee de la tabla ConfiguracionNotificacion y los pasa a ConfiguracionNotificaciones (lo que usan
 *   los canales de envío).
 * - Guarda lo que se escribe en Configuración → Respaldos y sistema, con su bitácora en CambioConfiguracion.
 * - El token y la contraseña se guardan cifrados, nunca se muestran en pantalla ni en la consola,
 *   y en la bitácora solo se anota que cambiaron.
 * - Envía mensajes de prueba con los datos del formulario.
 */
public class IntegracionesService {

    public static final String MSG_GUARDADO = "Datos de notificaciones guardados";
    public static final String MSG_SIN_CAMBIOS = "No hay cambios por guardar";
    public static final String MSG_ERROR_GUARDAR = "No fue posible guardar los datos de notificaciones";
    public static final String MSG_ERROR_CIFRAR = "No se pudo cifrar el dato secreto: ";
    public static final String MSG_REAL_SIN_CANAL = "Para el modo real completa los datos de WhatsApp o del correo";

    // Estado de un secreto guardado (token o contraseña)
    public static final int SECRETO_VACIO = 0;
    public static final int SECRETO_GUARDADO = 1;
    public static final int SECRETO_ILEGIBLE = 2;   // se guardó con otra llave (otra computadora)

    // Prefijo de las claves en la bitácora CambioConfiguracion (para distinguirlas de los parámetros)
    public static final String PREFIJO_BITACORA = "Notif:";

    // Lo que se anota en la bitácora en lugar del secreto
    private static final String SECRETO_OCULTO = "•••••• (oculto)";

    // Asunto y texto del correo de prueba
    private static final String ASUNTO_PRUEBA = "Prueba de notificaciones CARMIR";
    private static final String TEXTO_PRUEBA = "Si recibiste este correo, el envío de notificaciones del "
            + "Panel Administrativo CARMIR ya funciona.";

    private IntegracionesDAO integracionesDAO;
    private ConfiguracionDAO configuracionDAO;

    // Últimos valores leídos de la tabla, tal como están guardados (secretos cifrados)
    private Properties guardados = new Properties();
    private String motivoError = null;

    public IntegracionesService() {
        integracionesDAO = DAOFactory.crearIntegracionesDAO();
        configuracionDAO = DAOFactory.crearConfiguracionDAO();
    }

    // ===================== Lectura =====================

    /**
     * Lee la tabla ConfiguracionNotificacion, descifra los secretos y los pasa a ConfiguracionNotificaciones.
     * Devuelve false si no se pudo (sin conexión o sin la migración): el panel sigue con notificaciones.properties.
     */
    public boolean cargarEnAplicacion() {
        try {
            guardados = integracionesDAO.listar();
            motivoError = null;
        } catch (DAOException e) {
            guardados = new Properties();
            motivoError = e.getMessage();
            System.out.println("Notificaciones: se usan los valores de notificaciones.properties (" + e.getMessage() + ")");
            return false;
        }
        Properties valores = new Properties();
        for (String clave : guardados.stringPropertyNames()) {
            String valor = guardados.getProperty(clave);
            if (esSecreta(clave)) {
                valor = CifradoSecretos.descifrar(valor);
                if (valor == null) {
                    // No se escribe el valor, solo el aviso
                    System.out.println("Notificaciones: no se pudo descifrar " + clave
                            + " (se guardó en otra computadora); hay que escribirlo de nuevo");
                    valor = "";
                }
            }
            valores.setProperty(clave, valor);
        }
        ConfiguracionNotificaciones.getInstancia().aplicarValoresBD(valores);
        return true;
    }

    // Motivo por el que no se pudo leer la tabla (null si se leyó bien)
    public String getMotivoError() {
        return motivoError;
    }

    // Si hay token o contraseña guardados (llamar después de cargarEnAplicacion)
    public int estadoSecreto(String clave) {
        String valor = guardados.getProperty(clave, "");
        if (valor.isEmpty()) {
            return SECRETO_VACIO;
        }
        if (CifradoSecretos.descifrar(valor) == null) {
            return SECRETO_ILEGIBLE;
        }
        return SECRETO_GUARDADO;
    }

    // ===================== Guardar =====================

    /**
     * Guarda lo escrito en el formulario.
     * En "formulario" el token y la contraseña vienen vacíos si no se escribió uno nuevo (se conserva el guardado).
     * quitarToken / quitarContrasena = la persona pidió borrar el guardado.
     */
    // RF-71 · Pasos 3, 5 y 6: datos de las cuentas de WhatsApp y correo que usan los avisos al cliente (RF-47).
    public ResultadoConfiguracion guardar(DatosNotificaciones formulario, boolean quitarToken,
                                          boolean quitarContrasena) {
        // 1. Se leen los datos actuales para saber qué cambió
        // RF-71 · Excepción paso 1: si no se puede leer la tabla no se guarda.
        if (!cargarEnAplicacion()) {
            return ResultadoConfiguracion.error(motivoError, ResultadoConfiguracion.CAMPO_CONEXION);
        }
        DatosNotificaciones completos = completarSecretos(formulario, quitarToken, quitarContrasena);

        // 2. Cada valor debe ser válido (RF-71 · Paso 3)
        // RF-71 · Excepción paso 3: dato no válido o modo real sin ningún canal listo.
        ResultadoConfiguracion error = validar(completos);
        if (error != null) {
            return error;
        }

        // 3. Se arma la lista de lo que cambió (los secretos se cifran) (RF-71 · Paso 5)
        List<String> claves = new ArrayList<String>();
        List<String> valores = new ArrayList<String>();
        List<CambioConfiguracion> bitacora = new ArrayList<CambioConfiguracion>();
        int idUsuario = PermisosConfiguracion.idUsuarioSesion();
        Properties nuevos = aPropiedades(completos);
        for (String clave : ordenClaves()) {
            String valorNuevo = nuevos.getProperty(clave, "");
            String anterior = valorActual(clave);
            if (anterior != null && anterior.equals(valorNuevo)) {
                continue;
            }
            // Un secreto que nunca se ha escrito no se anota
            if (anterior == null && valorNuevo.isEmpty() && esSecreta(clave)) {
                continue;
            }
            String paraGuardar = valorNuevo;
            String textoAnterior = anterior;
            String textoNuevo = valorNuevo;
            if (esSecreta(clave)) {
                try {
                    paraGuardar = CifradoSecretos.cifrar(valorNuevo);
                } catch (GeneralSecurityException e) {
                    return ResultadoConfiguracion.error(MSG_ERROR_CIFRAR + e.getMessage(), clave);
                }
                // En la bitácora no se escribe el secreto, solo si había uno y si quedó uno
                textoAnterior = ocultar(anterior);
                textoNuevo = ocultar(valorNuevo);
            }
            claves.add(clave);
            valores.add(paraGuardar);
            bitacora.add(new CambioConfiguracion(PREFIJO_BITACORA + clave, recortar(textoAnterior),
                    recortar(textoNuevo), idUsuario));
        }
        // RF-71 · Excepción paso 5: «No hay cambios por guardar».
        if (claves.isEmpty()) {
            return ResultadoConfiguracion.correcto(MSG_SIN_CAMBIOS);
        }

        // 4. Se guardan los cambios y su bitácora en una sola transacción (RF-71 · Paso 5)
        // RF-71 · Excepción paso 5: si algo falla se deshace todo, «No fue posible guardar los datos de notificaciones».
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);
            for (int i = 0; i < claves.size(); i++) {
                integracionesDAO.guardar(conexion, claves.get(i), valores.get(i), esSecreta(claves.get(i)));
                configuracionDAO.registrarCambio(conexion, bitacora.get(i));
            }
            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoConfiguracion.error(MSG_ERROR_GUARDAR, ResultadoConfiguracion.CAMPO_CONEXION);
        } catch (DAOException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }

        // 5. Los avisos usan los datos nuevos desde ahora (RF-71 · Paso 6)
        cargarEnAplicacion();
        return ResultadoConfiguracion.correcto(MSG_GUARDADO + " (" + claves.size() + " cambio(s))");
    }

    // ===================== Pruebas de envío =====================

    // RF-71 · Paso 4: envía un correo de prueba con los datos del formulario (se puede probar antes de guardar).
    // RF-71 · Excepción paso 4: «No se pudo enviar el correo de prueba: …».
    // Corre en segundo plano: no toca la pantalla.
    public ResultadoConfiguracion probarCorreo(DatosNotificaciones formulario, boolean quitarContrasena,
                                               String destino) {
        DatosNotificaciones datos = completarSecretos(formulario, false, quitarContrasena);
        String correo = texto(destino);
        if (correo.isEmpty() || !correo.contains("@") || correo.contains(" ")) {
            return ResultadoConfiguracion.error("Escribe el correo al que se enviará la prueba", CAMPO_PRUEBA_CORREO);
        }
        ResultadoConfiguracion error = validarCorreo(datos, true);
        if (error != null) {
            return error;
        }
        try {
            new CanalCorreo(datos).enviar(correo, ASUNTO_PRUEBA, TEXTO_PRUEBA);
            return ResultadoConfiguracion.correcto("Correo de prueba enviado a " + correo
                    + ". Revisa la bandeja de entrada (y la de spam).");
        } catch (ErrorEnvio e) {
            return ResultadoConfiguracion.error("No se pudo enviar el correo de prueba: " + e.getMessage(),
                    CAMPO_PRUEBA_CORREO);
        }
    }

    // RF-71 · Paso 4: envía la plantilla de prueba hello_world por WhatsApp con los datos del formulario.
    // RF-71 · Excepción paso 4: «No se pudo enviar el WhatsApp de prueba: …».
    public ResultadoConfiguracion probarWhatsApp(DatosNotificaciones formulario, boolean quitarToken,
                                                 String destino) {
        DatosNotificaciones datos = completarSecretos(formulario, quitarToken, false);
        String numero = texto(destino);
        if (!soloDigitos(numero) || numero.length() < 10 || numero.length() > 15) {
            return ResultadoConfiguracion.error("Escribe el teléfono de prueba: 10 dígitos, sin espacios",
                    CAMPO_PRUEBA_WHATSAPP);
        }
        ResultadoConfiguracion error = validarWhatsApp(datos, true);
        if (error != null) {
            return error;
        }
        try {
            new CanalWhatsApp(datos).enviarPrueba(numero);
            return ResultadoConfiguracion.correcto("Meta aceptó el mensaje de prueba para " + numero
                    + ". Debe llegar la plantilla «" + CanalWhatsApp.PLANTILLA_PRUEBA + "» en unos segundos.");
        } catch (ErrorEnvio e) {
            return ResultadoConfiguracion.error("No se pudo enviar el WhatsApp de prueba: " + e.getMessage(),
                    CAMPO_PRUEBA_WHATSAPP);
        }
    }

    // Campos de prueba que el formulario puede marcar en rojo
    public static final String CAMPO_PRUEBA_CORREO = "pruebaCorreo";
    public static final String CAMPO_PRUEBA_WHATSAPP = "pruebaWhatsApp";

    // ===================== Validación =====================

    // Devuelve el error del primer dato no válido, o null si todo está bien
    private ResultadoConfiguracion validar(DatosNotificaciones datos) {
        if (!DatosNotificaciones.MODO_SIMULADO.equals(datos.getModo())
                && !DatosNotificaciones.MODO_REAL.equals(datos.getModo())) {
            return ResultadoConfiguracion.error("Elige el modo de las notificaciones", DatosNotificaciones.CLAVE_MODO);
        }
        if (datos.getReintentos() < 0 || datos.getReintentos() > 10) {
            return ResultadoConfiguracion.error("Los reintentos deben estar entre 0 y 10",
                    DatosNotificaciones.CLAVE_REINTENTOS);
        }
        if (datos.getSegundosEntreReintentos() < 1 || datos.getSegundosEntreReintentos() > 600) {
            return ResultadoConfiguracion.error("Los segundos entre reintentos deben estar entre 1 y 600",
                    DatosNotificaciones.CLAVE_SEGUNDOS_REINTENTO);
        }
        ResultadoConfiguracion error = validarWhatsApp(datos, false);
        if (error != null) {
            return error;
        }
        error = validarCorreo(datos, false);
        if (error != null) {
            return error;
        }
        // RF-71 · Excepción paso 3: en modo real debe quedar listo al menos un canal
        boolean whatsAppListo = datos.isWhatsAppHabilitado() && whatsAppCompleto(datos);
        boolean correoListo = datos.isCorreoHabilitado() && correoCompleto(datos);
        if (!datos.isSimulado() && !whatsAppListo && !correoListo) {
            return ResultadoConfiguracion.error(MSG_REAL_SIN_CANAL, DatosNotificaciones.CLAVE_MODO);
        }
        return null;
    }

    // completo = true pide todos los datos (para enviar una prueba)
    private ResultadoConfiguracion validarWhatsApp(DatosNotificaciones datos, boolean completo) {
        String idNumero = datos.getWhatsAppIdNumero();
        if (completo && idNumero.isEmpty()) {
            return ResultadoConfiguracion.error("Escribe el ID del número de teléfono de WhatsApp",
                    DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO);
        }
        if (!soloDigitos(idNumero) || idNumero.length() > 30) {
            return ResultadoConfiguracion.error("El ID del número de teléfono solo lleva números (lo da Meta, "
                    + "no es tu teléfono)", DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO);
        }
        if (completo && datos.getWhatsAppToken().isEmpty()) {
            return ResultadoConfiguracion.error("Escribe el token de acceso de WhatsApp",
                    DatosNotificaciones.CLAVE_WHATSAPP_TOKEN);
        }
        if (datos.getWhatsAppToken().length() > 1000 || datos.getWhatsAppToken().contains(" ")) {
            return ResultadoConfiguracion.error("El token de WhatsApp no es válido (no lleva espacios)",
                    DatosNotificaciones.CLAVE_WHATSAPP_TOKEN);
        }
        String lada = datos.getWhatsAppLada();
        if (lada.isEmpty() || !soloDigitos(lada) || lada.length() > 4) {
            return ResultadoConfiguracion.error("La lada del país lleva de 1 a 4 números (México = 52)",
                    DatosNotificaciones.CLAVE_WHATSAPP_LADA);
        }
        if (!versionValida(datos.getWhatsAppVersionApi())) {
            return ResultadoConfiguracion.error("La versión de la API se escribe como v20.0",
                    DatosNotificaciones.CLAVE_WHATSAPP_VERSION);
        }
        return null;
    }

    private ResultadoConfiguracion validarCorreo(DatosNotificaciones datos, boolean completo) {
        String servidor = datos.getSmtpServidor();
        if (completo && servidor.isEmpty()) {
            return ResultadoConfiguracion.error("Escribe el servidor de correo (por ejemplo smtp.gmail.com)",
                    DatosNotificaciones.CLAVE_SMTP_SERVIDOR);
        }
        if (servidor.contains(" ") || servidor.length() > 100) {
            return ResultadoConfiguracion.error("El servidor de correo no es válido",
                    DatosNotificaciones.CLAVE_SMTP_SERVIDOR);
        }
        if (datos.getSmtpPuerto() < 1 || datos.getSmtpPuerto() > 65535) {
            return ResultadoConfiguracion.error("El puerto debe estar entre 1 y 65535 (normalmente 587 o 465)",
                    DatosNotificaciones.CLAVE_SMTP_PUERTO);
        }
        if (!DatosNotificaciones.SEGURIDAD_STARTTLS.equals(datos.getSmtpSeguridad())
                && !DatosNotificaciones.SEGURIDAD_SSL.equals(datos.getSmtpSeguridad())) {
            return ResultadoConfiguracion.error("Elige la seguridad del correo", DatosNotificaciones.CLAVE_SMTP_SEGURIDAD);
        }
        String usuario = datos.getSmtpUsuario();
        if (completo && usuario.isEmpty()) {
            return ResultadoConfiguracion.error("Escribe el usuario (correo) de la cuenta",
                    DatosNotificaciones.CLAVE_SMTP_USUARIO);
        }
        if (usuario.contains(" ") || usuario.length() > 150) {
            return ResultadoConfiguracion.error("El usuario del correo no es válido", DatosNotificaciones.CLAVE_SMTP_USUARIO);
        }
        if (completo && datos.getSmtpContrasena().isEmpty()) {
            return ResultadoConfiguracion.error("Escribe la contraseña del correo",
                    DatosNotificaciones.CLAVE_SMTP_CONTRASENA);
        }
        if (datos.getSmtpContrasena().length() > 200) {
            return ResultadoConfiguracion.error("La contraseña del correo es demasiado larga",
                    DatosNotificaciones.CLAVE_SMTP_CONTRASENA);
        }
        String remitente = datos.getSmtpRemitente();
        if (!remitente.isEmpty() && (!remitente.contains("@") || remitente.contains(" ") || remitente.length() > 150)) {
            return ResultadoConfiguracion.error("El correo del remitente no es válido",
                    DatosNotificaciones.CLAVE_SMTP_REMITENTE);
        }
        return null;
    }

    // Texto que se guarda en la tabla para un sí/no
    private String textoBandera(boolean valor) {
        if (valor) {
            return "1";
        }
        return "0";
    }

    private boolean whatsAppCompleto(DatosNotificaciones datos) {
        return !datos.getWhatsAppIdNumero().isEmpty() && !datos.getWhatsAppToken().isEmpty();
    }

    private boolean correoCompleto(DatosNotificaciones datos) {
        return !datos.getSmtpServidor().isEmpty() && !datos.getSmtpUsuario().isEmpty()
                && !datos.getSmtpContrasena().isEmpty();
    }

    // "v20.0": una v, números, un punto y números
    private boolean versionValida(String version) {
        if (version.length() < 4 || version.charAt(0) != 'v') {
            return false;
        }
        String[] partes = version.substring(1).split("\\.");
        if (partes.length != 2) {
            return false;
        }
        return !partes[0].isEmpty() && !partes[1].isEmpty() && soloDigitos(partes[0]) && soloDigitos(partes[1]);
    }

    // ===================== Apoyo =====================

    // Si el token o la contraseña vienen vacíos se usa el guardado (salvo que se haya pedido quitarlo)
    private DatosNotificaciones completarSecretos(DatosNotificaciones formulario, boolean quitarToken,
                                                  boolean quitarContrasena) {
        DatosNotificaciones actuales = ConfiguracionNotificaciones.getInstancia().getDatos();
        DatosNotificaciones datos = formulario.copiar();
        if (quitarToken) {
            datos.setWhatsAppToken("");
        } else if (datos.getWhatsAppToken().isEmpty()) {
            datos.setWhatsAppToken(actuales.getWhatsAppToken());
        }
        if (quitarContrasena) {
            datos.setSmtpContrasena("");
        } else if (datos.getSmtpContrasena().isEmpty()) {
            datos.setSmtpContrasena(actuales.getSmtpContrasena());
        }
        return datos;
    }

    // Pasa los datos a Clave → Valor (como se guardan en la tabla, sin cifrar todavía)
    private Properties aPropiedades(DatosNotificaciones datos) {
        Properties valores = new Properties();
        valores.setProperty(DatosNotificaciones.CLAVE_MODO, datos.getModo());
        valores.setProperty(DatosNotificaciones.CLAVE_REINTENTOS, String.valueOf(datos.getReintentos()));
        valores.setProperty(DatosNotificaciones.CLAVE_SEGUNDOS_REINTENTO, String.valueOf(datos.getSegundosEntreReintentos()));
        valores.setProperty(DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO, datos.getWhatsAppIdNumero());
        valores.setProperty(DatosNotificaciones.CLAVE_WHATSAPP_TOKEN, datos.getWhatsAppToken());
        valores.setProperty(DatosNotificaciones.CLAVE_WHATSAPP_VERSION, datos.getWhatsAppVersionApi());
        valores.setProperty(DatosNotificaciones.CLAVE_WHATSAPP_LADA, datos.getWhatsAppLada());
        valores.setProperty(DatosNotificaciones.CLAVE_SMTP_SERVIDOR, datos.getSmtpServidor());
        valores.setProperty(DatosNotificaciones.CLAVE_SMTP_PUERTO, String.valueOf(datos.getSmtpPuerto()));
        valores.setProperty(DatosNotificaciones.CLAVE_SMTP_SEGURIDAD, datos.getSmtpSeguridad());
        valores.setProperty(DatosNotificaciones.CLAVE_SMTP_USUARIO, datos.getSmtpUsuario());
        valores.setProperty(DatosNotificaciones.CLAVE_SMTP_CONTRASENA, datos.getSmtpContrasena());
        valores.setProperty(DatosNotificaciones.CLAVE_SMTP_REMITENTE, datos.getSmtpRemitente());
        valores.setProperty(DatosNotificaciones.CLAVE_WHATSAPP_HABILITADO, textoBandera(datos.isWhatsAppHabilitado()));
        valores.setProperty(DatosNotificaciones.CLAVE_CORREO_HABILITADO, textoBandera(datos.isCorreoHabilitado()));
        return valores;
    }

    // Orden en que se guardan las claves (y aparecen en la bitácora)
    private String[] ordenClaves() {
        String[] claves = {
                DatosNotificaciones.CLAVE_MODO, DatosNotificaciones.CLAVE_REINTENTOS,
                DatosNotificaciones.CLAVE_SEGUNDOS_REINTENTO, DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO,
                DatosNotificaciones.CLAVE_WHATSAPP_TOKEN, DatosNotificaciones.CLAVE_WHATSAPP_VERSION,
                DatosNotificaciones.CLAVE_WHATSAPP_LADA, DatosNotificaciones.CLAVE_SMTP_SERVIDOR,
                DatosNotificaciones.CLAVE_SMTP_PUERTO, DatosNotificaciones.CLAVE_SMTP_SEGURIDAD,
                DatosNotificaciones.CLAVE_SMTP_USUARIO, DatosNotificaciones.CLAVE_SMTP_CONTRASENA,
                DatosNotificaciones.CLAVE_SMTP_REMITENTE, DatosNotificaciones.CLAVE_WHATSAPP_HABILITADO,
                DatosNotificaciones.CLAVE_CORREO_HABILITADO};
        return claves;
    }

    // Valor guardado de una clave (los secretos ya descifrados); null si no está en la tabla
    private String valorActual(String clave) {
        String valor = guardados.getProperty(clave);
        if (valor == null) {
            return null;
        }
        if (esSecreta(clave)) {
            String descifrado = CifradoSecretos.descifrar(valor);
            if (descifrado == null) {
                // No se puede leer: se considera distinto para que se guarde el nuevo
                return null;
            }
            return descifrado;
        }
        return valor;
    }

    public static boolean esSecreta(String clave) {
        for (String secreta : DatosNotificaciones.CLAVES_SECRETAS) {
            if (secreta.equals(clave)) {
                return true;
            }
        }
        return false;
    }

    // Lo que se anota en la bitácora en lugar de un secreto
    private String ocultar(String valor) {
        if (valor == null) {
            return null;
        }
        if (valor.isEmpty()) {
            return "";
        }
        return SECRETO_OCULTO;
    }

    // La bitácora guarda hasta 255 caracteres
    private String recortar(String valor) {
        if (valor == null || valor.length() <= 255) {
            return valor;
        }
        return valor.substring(0, 255);
    }

    private String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }

    private static boolean soloDigitos(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            if (letra < '0' || letra > '9') {
                return false;
            }
        }
        return true;
    }

    // Nombre en español de cada clave para el historial de cambios ("Notif:SmtpServidor" → ...)
    public static String nombreVisible(String claveBitacora) {
        String clave = claveBitacora.substring(PREFIJO_BITACORA.length());
        String nombre = clave;
        if (DatosNotificaciones.CLAVE_MODO.equals(clave)) {
            nombre = "Modo (simulado o real)";
        } else if (DatosNotificaciones.CLAVE_REINTENTOS.equals(clave)) {
            nombre = "Reintentos";
        } else if (DatosNotificaciones.CLAVE_SEGUNDOS_REINTENTO.equals(clave)) {
            nombre = "Segundos entre reintentos";
        } else if (DatosNotificaciones.CLAVE_WHATSAPP_ID_NUMERO.equals(clave)) {
            nombre = "WhatsApp: ID del número";
        } else if (DatosNotificaciones.CLAVE_WHATSAPP_TOKEN.equals(clave)) {
            nombre = "WhatsApp: token de acceso";
        } else if (DatosNotificaciones.CLAVE_WHATSAPP_VERSION.equals(clave)) {
            nombre = "WhatsApp: versión de la API";
        } else if (DatosNotificaciones.CLAVE_WHATSAPP_LADA.equals(clave)) {
            nombre = "WhatsApp: lada del país";
        } else if (DatosNotificaciones.CLAVE_SMTP_SERVIDOR.equals(clave)) {
            nombre = "Correo: servidor SMTP";
        } else if (DatosNotificaciones.CLAVE_SMTP_PUERTO.equals(clave)) {
            nombre = "Correo: puerto";
        } else if (DatosNotificaciones.CLAVE_SMTP_SEGURIDAD.equals(clave)) {
            nombre = "Correo: seguridad";
        } else if (DatosNotificaciones.CLAVE_SMTP_USUARIO.equals(clave)) {
            nombre = "Correo: usuario";
        } else if (DatosNotificaciones.CLAVE_SMTP_CONTRASENA.equals(clave)) {
            nombre = "Correo: contraseña";
        } else if (DatosNotificaciones.CLAVE_SMTP_REMITENTE.equals(clave)) {
            nombre = "Correo: remitente";
        } else if (DatosNotificaciones.CLAVE_WHATSAPP_HABILITADO.equals(clave)) {
            nombre = "WhatsApp: habilitado";
        } else if (DatosNotificaciones.CLAVE_CORREO_HABILITADO.equals(clave)) {
            nombre = "Correo: habilitado";
        }
        return "Notificaciones · " + nombre;
    }
}
