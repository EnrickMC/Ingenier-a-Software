package com.pac.modulos.pedidos.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.NotificacionDAO;
import com.pac.modulos.pedidos.model.Notificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de NotificacionDAO para MySQL (tabla Notificacion).
 */
public class NotificacionDAOMySQL implements NotificacionDAO {

    private static final String SELECT_NOTIFICACION =
            "SELECT IdNotificacion, Folio, TipoEvento, Canal, Destino, Mensaje, FechaHoraEnvio, ResultadoEnvio, MotivoFallo "
            + "FROM Notificacion ";

    @Override
    public List<Notificacion> listarPorFolio(Connection conexion, int folio) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    SELECT_NOTIFICACION + "WHERE Folio = ? ORDER BY FechaHoraEnvio DESC, IdNotificacion DESC");
            consulta.setInt(1, folio);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw error("Error al consultar las notificaciones del pedido " + folio, e);
        }
    }

    @Override
    public List<Notificacion> listarPendientes(Connection conexion) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    SELECT_NOTIFICACION + "WHERE ResultadoEnvio = 'Pendiente' ORDER BY IdNotificacion");
            return leerLista(consulta);
        } catch (SQLException e) {
            throw error("Error al consultar las notificaciones pendientes", e);
        }
    }

    @Override
    public Notificacion buscarPorId(Connection conexion, int idNotificacion) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(SELECT_NOTIFICACION + "WHERE IdNotificacion = ?");
            consulta.setInt(1, idNotificacion);
            List<Notificacion> lista = leerLista(consulta);
            if (lista.isEmpty()) {
                return null;
            }
            return lista.get(0);
        } catch (SQLException e) {
            throw error("Error al consultar la notificación " + idNotificacion, e);
        }
    }

    @Override
    public int insertar(Connection conexion, Notificacion notificacion) throws DAOException {
        String sql = "INSERT INTO Notificacion (Folio, TipoEvento, Canal, Destino, Mensaje, ResultadoEnvio, MotivoFallo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, notificacion.getFolio());
            consulta.setString(2, notificacion.getTipoEvento());
            consulta.setString(3, notificacion.getCanal());
            consulta.setString(4, notificacion.getDestino());
            consulta.setString(5, notificacion.getMensaje());
            consulta.setString(6, notificacion.getResultado());
            ApoyoPedidosMySQL.ponerTexto(consulta, 7, recortar(notificacion.getMotivoFallo()));
            consulta.executeUpdate();
            ResultSet llaves = consulta.getGeneratedKeys();
            int idNuevo = 0;
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            return idNuevo;
        } catch (SQLException e) {
            throw error("Error al registrar la notificación", e);
        }
    }

    @Override
    public void actualizarResultado(Connection conexion, int idNotificacion, String resultado, String motivo)
            throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Notificacion SET ResultadoEnvio = ?, MotivoFallo = ?, FechaHoraEnvio = NOW() "
                    + "WHERE IdNotificacion = ?");
            consulta.setString(1, resultado);
            ApoyoPedidosMySQL.ponerTexto(consulta, 2, recortar(motivo));
            consulta.setInt(3, idNotificacion);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al actualizar la notificación " + idNotificacion, e);
        }
    }

    // ===================== Apoyo =====================

    private List<Notificacion> leerLista(PreparedStatement consulta) throws SQLException {
        List<Notificacion> lista = new ArrayList<Notificacion>();
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            Notificacion notificacion = new Notificacion();
            notificacion.setIdNotificacion(resultado.getInt("IdNotificacion"));
            notificacion.setFolio(resultado.getInt("Folio"));
            notificacion.setTipoEvento(resultado.getString("TipoEvento"));
            notificacion.setCanal(resultado.getString("Canal"));
            notificacion.setDestino(resultado.getString("Destino"));
            notificacion.setMensaje(resultado.getString("Mensaje"));
            notificacion.setFecha(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaHoraEnvio"));
            notificacion.setResultado(resultado.getString("ResultadoEnvio"));
            notificacion.setMotivoFallo(resultado.getString("MotivoFallo"));
            lista.add(notificacion);
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    // La columna MotivoFallo guarda hasta 255 caracteres
    private String recortar(String texto) {
        if (texto == null || texto.length() <= 255) {
            return texto;
        }
        return texto.substring(0, 252) + "...";
    }

    private DAOException error(String mensaje, SQLException causa) {
        if (ApoyoPedidosMySQL.faltaMigracion(causa)) {
            return new DAOException(BitacoraPedidoDAO.MSG_FALTA_MIGRACION, causa);
        }
        return new DAOException(mensaje, causa);
    }
}
