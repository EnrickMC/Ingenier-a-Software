package com.pac.modulos.configuracion.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.configuracion.dao.ConfiguracionDAO;
import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.configuracion.model.ClavesConfiguracion;
import com.pac.modulos.configuracion.model.Parametro;
import com.pac.util.ConfiguracionApp;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RF-68 · Pasos 4 a 7: parámetros del negocio (Prompt 8.2). Los lee de la tabla Configuracion, revisa cada valor,
 * guarda los que cambiaron con su bitácora (quién y cuándo) y los aplica de inmediato al panel
 * a través de ConfiguracionApp.
 */
public class ConfiguracionService {

    public static final String MSG_SIN_CAMBIOS = "No hay cambios por guardar";
    public static final String MSG_GUARDADO = "Configuración guardada";
    public static final String MSG_ERROR_GUARDAR = "No fue posible guardar la configuración";
    public static final String MSG_ERROR_CONEXION = "Error de conexión: no fue posible leer la configuración";

    // Cuántos cambios muestra el historial
    public static final int CAMBIOS_EN_HISTORIAL = 300;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private ConfiguracionDAO configuracionDAO;

    public ConfiguracionService() {
        configuracionDAO = DAOFactory.crearConfiguracionDAO();
    }

    // ===================== Lectura =====================

    // Parámetros guardados en la base de datos
    public List<Parametro> listar() throws DAOException {
        return configuracionDAO.listar();
    }

    // Lee la tabla Configuracion y la pasa a ConfiguracionApp. Devuelve false si no se pudo
    // (sin conexión o sin la migración): en ese caso el panel sigue con app.properties.
    public boolean cargarEnAplicacion() {
        // Datos de WhatsApp y correo (tabla ConfiguracionNotificacion); si falla, se usa notificaciones.properties
        new IntegracionesService().cargarEnAplicacion();
        try {
            aplicar(configuracionDAO.listar());
            return true;
        } catch (DAOException e) {
            System.out.println("Configuración: se usan los valores de app.properties (" + e.getMessage() + ")");
            return false;
        }
    }

    // RF-70 · Paso 3: los últimos 300 cambios de configuración.
    public List<CambioConfiguracion> historial() throws DAOException {
        return configuracionDAO.listarCambios(CAMBIOS_EN_HISTORIAL);
    }

    // ===================== Guardar =====================

    /**
     * Revisa todos los valores del formulario y guarda los que cambiaron.
     * Si un valor no es válido no se guarda nada y se indica qué campo corregir.
     */
    public ResultadoConfiguracion guardar(List<Parametro> nuevos) {
        // RF-68 · Paso 4: cada valor debe ser válido (obligatorio, rango, hora HH:mm, días laborables y largo)
        // RF-68 · Excepción paso 4: el primer valor no válido se regresa con su campo y no se guarda nada.
        for (Parametro parametro : nuevos) {
            String error = validar(parametro.getClave(), parametro.getValor());
            if (error != null) {
                return ResultadoConfiguracion.error(error, parametro.getClave());
            }
        }
        // RF-68 · Paso 4: reglas entre dos valores, la hora de cierre después de la de apertura
        String apertura = valorDe(nuevos, ClavesConfiguracion.HORA_APERTURA);
        String cierre = valorDe(nuevos, ClavesConfiguracion.HORA_CIERRE);
        if (apertura != null && cierre != null) {
            LocalTime horaApertura = LocalTime.parse(apertura, FORMATO_HORA);
            LocalTime horaCierre = LocalTime.parse(cierre, FORMATO_HORA);
            if (!horaCierre.isAfter(horaApertura)) {
                return ResultadoConfiguracion.error("La hora de cierre debe ser posterior a la de apertura",
                        ClavesConfiguracion.HORA_CIERRE);
            }
        }

        // RF-68 · Paso 5: se comparan con lo guardado para saber qué cambió
        List<Parametro> actuales;
        try {
            actuales = configuracionDAO.listar();
        } catch (DAOException e) {
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        }
        int idUsuario = PermisosConfiguracion.idUsuarioSesion();
        List<Parametro> cambiados = new ArrayList<Parametro>();
        List<CambioConfiguracion> bitacora = new ArrayList<CambioConfiguracion>();
        for (Parametro nuevo : nuevos) {
            String anterior = valorDe(actuales, nuevo.getClave());
            String valorNuevo = nuevo.getValor().trim();
            if (anterior == null || !anterior.equals(valorNuevo)) {
                nuevo.setValor(valorNuevo);
                cambiados.add(nuevo);
                bitacora.add(new CambioConfiguracion(nuevo.getClave(), anterior, valorNuevo, idUsuario));
            }
        }
        // RF-68 · Excepción paso 5: «No hay cambios por guardar».
        if (cambiados.isEmpty()) {
            return ResultadoConfiguracion.correcto(MSG_SIN_CAMBIOS);
        }

        // RF-68 · Paso 6: se guardan los cambios y su bitácora en una sola transacción
        // RF-68 · Excepción paso 6: si algo falla se deshace todo, «No fue posible guardar la configuración».
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);
            for (int i = 0; i < cambiados.size(); i++) {
                configuracionDAO.guardar(conexion, cambiados.get(i));
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

        // RF-68 · Paso 7: el panel usa los valores nuevos desde ahora
        cargarEnAplicacion();
        return ResultadoConfiguracion.correcto(MSG_GUARDADO + " (" + cambiados.size() + " cambio(s))");
    }

    // ===================== Validación de cada parámetro =====================

    // RF-68 · Paso 4: devuelve el mensaje de error del valor, o null si es válido
    public String validar(String clave, String valor) {
        String texto = "";
        if (valor != null) {
            texto = valor.trim();
        }

        if (ClavesConfiguracion.NOMBRE_NEGOCIO.equals(clave)) {
            if (texto.isEmpty()) {
                return "Escribe el nombre del negocio";
            }
            return validarLargo(texto, 100, "El nombre del negocio");
        }
        if (ClavesConfiguracion.CAPACIDAD_DIARIA.equals(clave)) {
            return validarEntero(texto, 1, 100, "La capacidad diaria");
        }
        if (ClavesConfiguracion.DIAS_PARA_PREPARAR.equals(clave)) {
            return validarEntero(texto, 1, 30, "Los días para preparar");
        }
        if (ClavesConfiguracion.DIAS_MAXIMOS_AGENDA.equals(clave)) {
            return validarEntero(texto, 7, 366, "Los días máximos de la agenda");
        }
        if (ClavesConfiguracion.DIAS_ANTICIPACION_MINIMA.equals(clave)) {
            return validarEntero(texto, 0, 60, "Los días de anticipación");
        }
        if (ClavesConfiguracion.HORA_APERTURA.equals(clave) || ClavesConfiguracion.HORA_CIERRE.equals(clave)) {
            return validarHora(texto);
        }
        if (ClavesConfiguracion.DIAS_LABORABLES.equals(clave)) {
            return validarDiasLaborables(texto);
        }
        if (ClavesConfiguracion.PORCENTAJE_ANTICIPO.equals(clave)) {
            return validarEntero(texto, 1, 100, "El porcentaje de anticipo");
        }
        if (ClavesConfiguracion.DATOS_BANCARIOS.equals(clave)) {
            return validarLargo(texto, 255, "Los datos bancarios");
        }
        if (ClavesConfiguracion.DIAS_AVISO_CADUCIDAD.equals(clave)) {
            return validarEntero(texto, 0, 60, "Los días de aviso de caducidad");
        }
        if (ClavesConfiguracion.INTENTOS_MAXIMOS_LOGIN.equals(clave)) {
            return validarEntero(texto, 1, 10, "Los intentos de inicio de sesión");
        }
        if (ClavesConfiguracion.MINUTOS_INACTIVIDAD.equals(clave)) {
            return validarEntero(texto, 0, 480, "Los minutos de inactividad");
        }
        return "Parámetro desconocido: " + clave;
    }

    private String validarEntero(String texto, int minimo, int maximo, String nombre) {
        if (texto.isEmpty()) {
            return nombre + " es obligatorio";
        }
        int numero;
        try {
            numero = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return nombre + " debe ser un número entero";
        }
        if (numero < minimo || numero > maximo) {
            return nombre + " debe estar entre " + minimo + " y " + maximo;
        }
        return null;
    }

    private String validarLargo(String texto, int maximo, String nombre) {
        if (texto.length() > maximo) {
            return nombre + " no puede pasar de " + maximo + " caracteres";
        }
        return null;
    }

    private String validarHora(String texto) {
        try {
            LocalTime.parse(texto, FORMATO_HORA);
            return null;
        } catch (DateTimeParseException e) {
            return "Escribe la hora con el formato HH:mm (por ejemplo 09:00)";
        }
    }

    // "L,M,X,J,V,S": al menos un día y solo letras válidas, sin repetir
    private String validarDiasLaborables(String texto) {
        if (texto.isEmpty()) {
            return "Elige al menos un día laborable";
        }
        String[] dias = texto.split(",");
        List<String> vistos = new ArrayList<String>();
        for (String dia : dias) {
            String letra = dia.trim();
            boolean valida = false;
            for (String permitida : ClavesConfiguracion.LETRAS_DIAS) {
                if (permitida.equals(letra)) {
                    valida = true;
                }
            }
            if (!valida || vistos.contains(letra)) {
                return "Los días laborables no son válidos";
            }
            vistos.add(letra);
        }
        return null;
    }

    // ===================== Apoyo =====================

    // Pasa la lista de parámetros a ConfiguracionApp
    private void aplicar(List<Parametro> parametros) {
        Properties valores = new Properties();
        for (Parametro parametro : parametros) {
            valores.setProperty(parametro.getClave(), parametro.getValor());
        }
        ConfiguracionApp.getInstancia().aplicarValoresBD(valores);
    }

    // Valor de una clave dentro de una lista; null si no está
    public static String valorDe(List<Parametro> parametros, String clave) {
        for (Parametro parametro : parametros) {
            if (parametro.getClave().equals(clave)) {
                return parametro.getValor();
            }
        }
        return null;
    }
}
