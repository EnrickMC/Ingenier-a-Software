package com.pac.modulos.pedidos.notificacion;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.pedidos.dao.NotificacionDAO;
import com.pac.modulos.pedidos.model.Notificacion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Trabajo que corre en un hilo aparte (segundo plano) para no congelar la pantalla (RF-47).
 * Guarda cada aviso como "Pendiente", lo intenta enviar y lo deja "Enviado" o "Fallido".
 * Si falla, reintenta varias veces esperando entre cada intento.
 * Usa su propia conexión a la base de datos para no mezclarse con las transacciones de la pantalla.
 */
public class EnvioNotificaciones implements Runnable {

    private List<Notificacion> avisos;
    private String asunto;

    public EnvioNotificaciones(List<Notificacion> avisos, String asunto) {
        this.avisos = avisos;
        this.asunto = asunto;
    }

    @Override
    // RF-47 · Paso 5: envío en segundo plano por WhatsApp y correo.
    public void run() {
        NotificacionDAO dao = DAOFactory.crearNotificacionDAO();
        ConfiguracionNotificaciones config = ConfiguracionNotificaciones.getInstancia();
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().abrirConexionAparte();
            for (Notificacion aviso : avisos) {
                enviarUno(conexion, dao, config, aviso);
            }
        } catch (SQLException e) {
            System.out.println("Notificaciones: no hay conexión con la base de datos: " + e.getMessage());
        } finally {
            cerrar(conexion);
        }
    }

    // Registra el aviso, lo intenta enviar y guarda el resultado
    private void enviarUno(Connection conexion, NotificacionDAO dao, ConfiguracionNotificaciones config,
                           Notificacion aviso) {
        try {
            // Los avisos que ya no se pueden enviar (destino no válido) solo se registran
            // RF-47 · Excepción paso 3: canal omitido por no tener datos válidos; solo se registra.
            if (Notificacion.FALLIDO.equals(aviso.getResultado())) {
                dao.insertar(conexion, aviso);
                return;
            }
            // 1. Queda "Pendiente" mientras se envía
            if (aviso.getIdNotificacion() <= 0) {
                aviso.setResultado(Notificacion.PENDIENTE);
                aviso.setIdNotificacion(dao.insertar(conexion, aviso));
            } else {
                dao.actualizarResultado(conexion, aviso.getIdNotificacion(), Notificacion.PENDIENTE, null);
            }

            // 2. Intentos de envío
            CanalEnvio canal = crearCanal(aviso.getCanal(), config);
            int intentos = config.getReintentos() + 1;
            String motivo = null;
            // RF-47 · Paso 6: intenta enviar y espera la confirmación del servicio externo.
            for (int intento = 1; intento <= intentos; intento++) {
                try {
                    canal.enviar(aviso.getDestino(), asunto, aviso.getMensaje());
                    dao.actualizarResultado(conexion, aviso.getIdNotificacion(), Notificacion.ENVIADO, null);
                    // RF-47 · Paso 7: se registró como «Enviada» en la bitácora.
                    return;
                } catch (ErrorEnvio error) {
                    motivo = error.getMessage();
                    System.out.println("Notificación #" + aviso.getIdNotificacion() + " intento " + intento
                            + " falló: " + motivo);
                    // Si no fue por la red, reintentar no ayuda (por ejemplo, falta configurar el canal)
                    // RF-47 · Excepción paso 6: el servicio rechazó el mensaje; se marca «Fallida» con su motivo.
                    if (!error.isSinRed()) {
                        break;
                    }
                    // RF-47 · Excepción paso 5: sin red, queda «Pendiente de reenvío» y se reintenta más tarde.
                    if (intento < intentos) {
                        dao.actualizarResultado(conexion, aviso.getIdNotificacion(), Notificacion.PENDIENTE,
                                "Pendiente de reenvío: " + motivo);
                        esperar(config.getSegundosEntreReintentos());
                    }
                }
            }
            // 3. Tras los reintentos sigue sin enviarse
            // RF-47 · Excepción paso 6: sigue sin enviarse tras los reintentos; queda «Fallida».
            dao.actualizarResultado(conexion, aviso.getIdNotificacion(), Notificacion.FALLIDO, motivo);
        } catch (DAOException e) {
            System.out.println("Notificaciones: no se pudo registrar el aviso: " + e.getMessage());
        }
    }

    // Elige el canal: simulado (pruebas) o el real
    private CanalEnvio crearCanal(String nombre, ConfiguracionNotificaciones config) {
        if (config.isSimulado()) {
            return new CanalSimulado(nombre);
        }
        if (Notificacion.CANAL_WHATSAPP.equals(nombre)) {
            return new CanalWhatsApp();
        }
        return new CanalCorreo();
    }

    private void esperar(int segundos) {
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void cerrar(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.close();
        } catch (SQLException e) {
            System.out.println("Notificaciones: no se pudo cerrar la conexión");
        }
    }
}
