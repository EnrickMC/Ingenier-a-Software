package com.pac.modulos.pedidos.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.pedidos.model.BajaPedido;
import com.pac.modulos.pedidos.model.CambioEstado;
import com.pac.modulos.pedidos.model.Cancelacion;
import com.pac.modulos.pedidos.model.Modificacion;

import java.sql.Connection;
import java.util.List;

/**
 * Registros de lo que le pasa a un pedido: cambios de estado (HistorialEstado),
 * cancelación (Cancelacion), baja (BajaPedido) y modificaciones (ModificacionPedido).
 * Las dos últimas tablas las crea la migración V4.
 */
public interface BitacoraPedidoDAO {

    // Mensaje cuando falta la migración V4 (tablas o columnas nuevas)
    String MSG_FALTA_MIGRACION = "Falta ejecutar DBCarmir/migraciones/MIGRACION_COMPLETA.sql en la base de datos";

    List<CambioEstado> listarHistorial(int folio) throws DAOException;

    List<Modificacion> listarModificaciones(int folio) throws DAOException;

    // ----- Dentro de una transacción (idUsuario 0 = Administradora o sistema: se guarda NULL) -----

    void registrarCambio(Connection conexion, int folio, int idUsuario, String anterior, String nuevo) throws DAOException;

    void registrarCancelacion(Connection conexion, Cancelacion cancelacion, int idUsuario) throws DAOException;

    void registrarBaja(Connection conexion, BajaPedido baja, int idUsuario) throws DAOException;

    void registrarModificacion(Connection conexion, int folio, int idUsuario, String antes, String despues)
            throws DAOException;
}
