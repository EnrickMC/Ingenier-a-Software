package com.pac.modulos.configuracion.controller;

import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.configuracion.model.ClavesConfiguracion;
import com.pac.modulos.configuracion.service.IntegracionesService;
import com.pac.util.Formatos;

/**
 * Renglón de la tabla del historial de cambios (textos listos para mostrar).
 */
public class FilaCambio {

    private final CambioConfiguracion cambio;

    public FilaCambio(CambioConfiguracion cambio) {
        this.cambio = cambio;
    }

    public String getFecha() {
        if (cambio.getFecha() == null) {
            return "";
        }
        return Formatos.fechaHoraCorta(cambio.getFecha());
    }

    public String getUsuario() {
        // IdUsuario NULL en la base de datos = la Administradora (no está en la tabla Usuario)
        if (cambio.getIdUsuario() <= 0) {
            return "Administradora";
        }
        if (cambio.getNombreUsuario() == null) {
            return "Usuario " + cambio.getIdUsuario();
        }
        return cambio.getNombreUsuario();
    }

    public String getParametro() {
        return nombreVisible(cambio.getClave());
    }

    public String getAnterior() {
        return mostrarValor(cambio.getValorAnterior(), "(sin valor)");
    }

    public String getNuevo() {
        return mostrarValor(cambio.getValorNuevo(), "(eliminado)");
    }

    private String mostrarValor(String valor, String siEsNulo) {
        if (valor == null) {
            return siEsNulo;
        }
        if (valor.isEmpty()) {
            return "(vacío)";
        }
        return valor;
    }

    // Nombre en español de cada clave de la tabla Configuracion
    public static String nombreVisible(String clave) {
        if (clave == null) {
            return "";
        }
        // Datos de WhatsApp y correo (Respaldos y sistema)
        if (clave.startsWith(IntegracionesService.PREFIJO_BITACORA)) {
            return IntegracionesService.nombreVisible(clave);
        }
        if (clave.startsWith("CP ")) {
            return "Cobertura " + clave;
        }
        if (ClavesConfiguracion.NOMBRE_NEGOCIO.equals(clave)) {
            return "Nombre del negocio";
        }
        if (ClavesConfiguracion.CAPACIDAD_DIARIA.equals(clave)) {
            return "Capacidad diaria";
        }
        if (ClavesConfiguracion.DIAS_PARA_PREPARAR.equals(clave)) {
            return "Días para preparar";
        }
        if (ClavesConfiguracion.DIAS_MAXIMOS_AGENDA.equals(clave)) {
            return "Días máximos de la agenda";
        }
        if (ClavesConfiguracion.DIAS_ANTICIPACION_MINIMA.equals(clave)) {
            return "Días de anticipación mínima";
        }
        if (ClavesConfiguracion.HORA_APERTURA.equals(clave)) {
            return "Hora de apertura";
        }
        if (ClavesConfiguracion.HORA_CIERRE.equals(clave)) {
            return "Hora de cierre";
        }
        if (ClavesConfiguracion.DIAS_LABORABLES.equals(clave)) {
            return "Días laborables";
        }
        if (ClavesConfiguracion.PORCENTAJE_ANTICIPO.equals(clave)) {
            return "Porcentaje de anticipo";
        }
        if (ClavesConfiguracion.DATOS_BANCARIOS.equals(clave)) {
            return "Datos bancarios";
        }
        if (ClavesConfiguracion.DIAS_AVISO_CADUCIDAD.equals(clave)) {
            return "Días de aviso de caducidad";
        }
        if (ClavesConfiguracion.INTENTOS_MAXIMOS_LOGIN.equals(clave)) {
            return "Intentos de inicio de sesión";
        }
        if (ClavesConfiguracion.MINUTOS_INACTIVIDAD.equals(clave)) {
            return "Minutos de inactividad";
        }
        return clave;
    }
}
