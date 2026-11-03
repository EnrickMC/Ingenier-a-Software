package com.pac.modulos.pedidos.notificacion;

import com.pac.util.ConfiguracionApp;
import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.pedidos.dao.NotificacionDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.Notificacion;
import com.pac.modulos.pedidos.model.Pedido;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PATRÓN SINGLETON (fachada de las notificaciones, RF-32).
 *
 * Después de cada evento del pedido (aprobación, modificación, cancelación, baja, pago y cambio de estado)
 * los servicios llaman a notificar(). Este método arma el mensaje, revisa el teléfono y el correo del
 * cliente y manda el envío a un hilo en segundo plano: si el envío falla, el pedido NO se revierte.
 */
public class NotificacionService {

    public static final String MSG_SIN_CANALES =
            "El cliente no tiene un teléfono ni un correo válidos: contáctalo por otro medio";

    // Tipo de evento que guarda la página web al registrar un pedido (Módulo 2)
    public static final String TIPO_PEDIDO_WEB = "Pedido registrado";

    // Avisos pendientes que ya se mandaron a enviar en esta sesión (para no enviarlos dos veces)
    private Set<Integer> idsEnEnvio = new HashSet<Integer>();

    private static final String PATRON_CORREO = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PATRON_TELEFONO = "^[0-9]{10}$";

    private static NotificacionService instancia;

    private PedidoDAO pedidoDAO;
    private NotificacionDAO notificacionDAO;

    private NotificacionService() {
        pedidoDAO = DAOFactory.crearPedidoDAO();
        notificacionDAO = DAOFactory.crearNotificacionDAO();
    }

    public static synchronized NotificacionService getInstancia() {
        if (instancia == null) {
            instancia = new NotificacionService();
        }
        return instancia;
    }

    /**
     * Avisa al cliente por WhatsApp y correo.
     * Devuelve un aviso para mostrar a la Administradora (por ejemplo si no hay canales válidos) o null.
     // RF-32 (Notificaciones por WhatsApp y correo electrónico): inicia aquí. Paso 1: recibe el evento del pedido.
     */
    public String notificar(int folio, String tipoEvento, String detalle) {
        // Paso 2: el pedido existe y se leen los datos de contacto
        Pedido pedido;
        try {
            pedido = pedidoDAO.buscarPorFolio(folio);
        } catch (DAOException e) {
            System.out.println("Notificaciones: no se pudo leer el pedido " + folio + ": " + e.getMessage());
            return null;
        }
        // RF-32 · Excepción paso 2: el pedido no existe; se descarta el aviso y se registra el error.
        if (pedido == null) {
            System.out.println("Notificaciones: el pedido " + folio + " no existe; se descarta el aviso");
            return null;
        }

        // Paso 4: mensaje con la plantilla del evento
        String asunto = PlantillasMensaje.asunto(tipoEvento, pedido);
        String mensaje = PlantillasMensaje.mensaje(tipoEvento, pedido, detalle);

        // Paso 3: se revisa cada canal; el que no tiene datos válidos se omite y queda en la bitácora
        List<Notificacion> avisos = new ArrayList<Notificacion>();
        int canalesValidos = 0;

        // Un canal apagado en Configuración (o sin configurar en modo real) no se usa y no genera avisos fallidos
        DatosNotificaciones datos = ConfiguracionNotificaciones.getInstancia().getDatos();
        boolean usaWhatsApp = datos.isWhatsAppActivo();
        boolean usaCorreo = datos.isCorreoActivo();

        String telefono = pedido.getCliente().getTelefono();
        if (usaWhatsApp) {
            Notificacion whatsapp = crear(folio, tipoEvento, Notificacion.CANAL_WHATSAPP, telefono, mensaje);
            // RF-32 · Paso 3: teléfono válido, se usa WhatsApp.
            if (telefono != null && telefono.matches(PATRON_TELEFONO)) {
                canalesValidos++;
            } else {
                marcarOmitido(whatsapp, "Teléfono no válido: se omitió el canal");
            }
            avisos.add(whatsapp);
        }

        String correo = pedido.getCliente().getCorreo();
        if (usaCorreo && correo != null && !correo.isBlank()) {
            Notificacion aviso = crear(folio, tipoEvento, Notificacion.CANAL_CORREO, correo.trim(), mensaje);
            // RF-32 · Paso 3: correo válido, se usa correo electrónico.
            if (correo.trim().matches(PATRON_CORREO)) {
                canalesValidos++;
            } else {
                marcarOmitido(aviso, "Correo no válido: se omitió el canal");
            }
            avisos.add(aviso);
        }

        // Paso 5: el envío se hace en segundo plano
        if (!avisos.isEmpty()) {
            iniciarEnvio(avisos, asunto);
        }

        // Si los dos canales están apagados no hay nada que avisar a la administradora
        if (!usaWhatsApp && !usaCorreo) {
            return null;
        }
        // RF-32 · Excepción paso 3: no queda ningún canal válido; se avisa a la administradora.
        if (canalesValidos == 0) {
            return MSG_SIN_CANALES + " (pedido #" + folio + ").";
        }
        return null;
    }

    // Reenvía un aviso que quedó "Fallido" (botón «Reenviar» del detalle del pedido)
    // RF-32 · Excepción paso 6: «Reenviar» un aviso que quedó «Fallido».
    public boolean reenviar(int idNotificacion) {
        Notificacion aviso;
        Pedido pedido;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            aviso = notificacionDAO.buscarPorId(conexion, idNotificacion);
            if (aviso == null) {
                return false;
            }
            pedido = pedidoDAO.buscarPorFolio(aviso.getFolio());
        } catch (SQLException e) {
            return false;
        } catch (DAOException e) {
            return false;
        }
        if (pedido == null) {
            return false;
        }
        List<Notificacion> avisos = new ArrayList<Notificacion>();
        avisos.add(aviso);
        iniciarEnvio(avisos, PlantillasMensaje.asunto(aviso.getTipoEvento(), pedido));
        return true;
    }

    // Al abrir el panel se vuelven a intentar los avisos que quedaron "Pendiente"
    // RF-32 · Excepción paso 5: se reintentan los avisos que quedaron «Pendiente de reenvío».
    public void reintentarPendientes() {
        enviarPendientes(false);
    }

    /**
     * Módulo 2 · Portal de Pedidos: la página web deja el aviso "Pedido registrado" como "Pendiente"
     * (la página no tiene las credenciales de WhatsApp ni de correo). El panel lo envía en su
     * revisión de cada 60 segundos, así el cliente recibe el aviso aunque el panel ya esté abierto.
     */
    public void enviarAvisosDeLaWeb() {
        enviarPendientes(true);
    }

    // soloWeb = true: solo los avisos que dejó la página web
    private void enviarPendientes(boolean soloWeb) {
        List<Notificacion> pendientes;
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().abrirConexionAparte();
            pendientes = notificacionDAO.listarPendientes(conexion);
        } catch (SQLException e) {
            return;
        } catch (DAOException e) {
            return;
        } finally {
            if (conexion != null) {
                try {
                    conexion.close();
                } catch (SQLException e) {
                    System.out.println("Notificaciones: no se pudo cerrar la conexión");
                }
            }
        }
        DatosNotificaciones datos = ConfiguracionNotificaciones.getInstancia().getDatos();
        for (Notificacion aviso : pendientes) {
            if (soloWeb && !TIPO_PEDIDO_WEB.equals(aviso.getTipoEvento())) {
                continue;
            }
            // Si el canal está apagado o sin configurar, el aviso se queda como está (sin intentar enviar)
            if (Notificacion.CANAL_WHATSAPP.equals(aviso.getCanal()) && !datos.isWhatsAppActivo()) {
                continue;
            }
            if (Notificacion.CANAL_CORREO.equals(aviso.getCanal()) && !datos.isCorreoActivo()) {
                continue;
            }
            // Un aviso que ya se mandó a enviar en esta sesión no se vuelve a mandar
            if (!marcarEnEnvio(aviso.getIdNotificacion())) {
                continue;
            }
            List<Notificacion> uno = new ArrayList<Notificacion>();
            uno.add(aviso);
            iniciarEnvio(uno, ConfiguracionApp.getInstancia().getNombreNegocio() + " · Folio #" + aviso.getFolio() + " · " + aviso.getTipoEvento());
        }
    }

    // true si el aviso no se había mandado a enviar (y lo marca); false si ya estaba
    private synchronized boolean marcarEnEnvio(int idNotificacion) {
        if (idsEnEnvio.contains(idNotificacion)) {
            return false;
        }
        idsEnEnvio.add(idNotificacion);
        return true;
    }

    // Bitácora de avisos de un pedido (para el detalle)
    public List<Notificacion> listarPorFolio(int folio) throws DAOException {
        try {
            return notificacionDAO.listarPorFolio(ConexionBD.getInstancia().getConexion(), folio);
        } catch (SQLException e) {
            throw new DAOException("Error de conexión", e);
        }
    }

    // ===================== Apoyo =====================

    // Arranca un hilo en segundo plano; es "daemon" para que no impida cerrar el programa
    private void iniciarEnvio(List<Notificacion> avisos, String asunto) {
        Thread hilo = new Thread(new EnvioNotificaciones(avisos, asunto), "envio-notificaciones");
        hilo.setDaemon(true);
        hilo.start();
    }

    private Notificacion crear(int folio, String tipoEvento, String canal, String destino, String mensaje) {
        Notificacion aviso = new Notificacion();
        aviso.setFolio(folio);
        aviso.setTipoEvento(tipoEvento);
        aviso.setCanal(canal);
        if (destino == null || destino.isBlank()) {
            aviso.setDestino("(sin dato)");
        } else {
            aviso.setDestino(destino);
        }
        aviso.setMensaje(mensaje);
        return aviso;
    }

    private void marcarOmitido(Notificacion aviso, String motivo) {
        aviso.setResultado(Notificacion.FALLIDO);
        aviso.setMotivoFallo(motivo);
    }
}
