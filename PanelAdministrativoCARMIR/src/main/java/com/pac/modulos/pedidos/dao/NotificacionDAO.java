package com.pac.modulos.pedidos.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.pedidos.model.Notificacion;

import java.sql.Connection;
import java.util.List;

/**
 * Bitácora de avisos al cliente (tabla Notificacion, RF-47).
 * Todas reciben la conexión: el envío corre en un hilo aparte con su propia conexión.
 */
public interface NotificacionDAO {

    List<Notificacion> listarPorFolio(Connection conexion, int folio) throws DAOException;

    // Avisos que quedaron "Pendiente" (por ejemplo, se cerró el panel antes de reintentar)
    List<Notificacion> listarPendientes(Connection conexion) throws DAOException;

    // Guarda el aviso y devuelve su id
    int insertar(Connection conexion, Notificacion notificacion) throws DAOException;

    // Cambia el resultado del envío (Enviado, Fallido o Pendiente) y su motivo
    void actualizarResultado(Connection conexion, int idNotificacion, String resultado, String motivo)
            throws DAOException;

    // Busca un aviso por id; null si no existe
    Notificacion buscarPorId(Connection conexion, int idNotificacion) throws DAOException;
}
