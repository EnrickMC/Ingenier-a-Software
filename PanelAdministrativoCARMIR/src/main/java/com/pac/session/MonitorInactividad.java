package com.pac.session;

import com.pac.util.ConfiguracionApp;
import javafx.event.Event;
import javafx.event.EventHandler;

/**
 * PATRÓN SINGLETON.
 * Recuerda cuándo fue la última vez que se movió el ratón o se presionó una tecla.
 * El panel principal lo consulta para cerrar la sesión después de los minutos de inactividad
 * configurados (parámetro MinutosInactividadSesion; 0 = no se cierra sola).
 */
public class MonitorInactividad implements EventHandler<Event> {

    public static final String MSG_SESION_INACTIVA = "La sesión se cerró por inactividad. Vuelve a iniciar sesión";

    private static MonitorInactividad instancia;

    private long ultimaActividad;   // System.currentTimeMillis() del último movimiento

    private MonitorInactividad() {
        ultimaActividad = System.currentTimeMillis();
    }

    public static synchronized MonitorInactividad getInstancia() {
        if (instancia == null) {
            instancia = new MonitorInactividad();
        }
        return instancia;
    }

    // La escena llama a este método con cada movimiento del ratón o tecla
    @Override
    public void handle(Event evento) {
        registrarActividad();
    }

    public void registrarActividad() {
        ultimaActividad = System.currentTimeMillis();
    }

    // true si pasó más tiempo del permitido sin actividad
    // RNF-15 · Excepción paso 1 (también RNF-13 · Excepción paso 7): la sesión estuvo inactiva el tiempo máximo;
    // se cierra y se pide volver a iniciar sesión. Con 0 minutos no se cierra sola.
    public boolean seVencio() {
        int minutos = ConfiguracionApp.getInstancia().getMinutosInactividadSesion();
        if (minutos <= 0) {
            return false;
        }
        long transcurrido = System.currentTimeMillis() - ultimaActividad;
        return transcurrido >= minutos * 60L * 1000L;
    }
}
