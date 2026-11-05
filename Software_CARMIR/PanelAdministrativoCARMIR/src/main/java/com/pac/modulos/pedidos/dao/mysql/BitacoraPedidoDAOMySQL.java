package com.pac.modulos.pedidos.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.model.BajaPedido;
import com.pac.modulos.pedidos.model.CambioEstado;
import com.pac.modulos.pedidos.model.Cancelacion;
import com.pac.modulos.pedidos.model.Modificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de BitacoraPedidoDAO para MySQL
 * (tablas HistorialEstado, Cancelacion, BajaPedido y ModificacionPedido).
 */
public class BitacoraPedidoDAOMySQL implements BitacoraPedidoDAO {

    @Override
    public List<CambioEstado> listarHistorial(int folio) throws DAOException {
        String sql = "SELECT h.Folio, h.EstadoAnterior, h.EstadoNuevo, h.FechaHoraCambio, u.NombreUsuario "
                + "FROM HistorialEstado h LEFT JOIN Usuario u ON u.IdUsuario = h.IdUsuario "
                + "WHERE h.Folio = ? ORDER BY h.FechaHoraCambio, h.IdHistorial";
        List<CambioEstado> lista = new ArrayList<CambioEstado>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                CambioEstado cambio = new CambioEstado();
                cambio.setFolio(resultado.getInt("Folio"));
                cambio.setEstadoAnterior(resultado.getString("EstadoAnterior"));
                cambio.setEstadoNuevo(resultado.getString("EstadoNuevo"));
                cambio.setFecha(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaHoraCambio"));
                // El primer registro sin usuario lo hizo el cliente desde la web
                String usuario = resultado.getString("NombreUsuario");
                if (usuario == null && cambio.getEstadoAnterior() == null) {
                    cambio.setNombreUsuario("Cliente (web)");
                } else {
                    cambio.setNombreUsuario(ApoyoPedidosMySQL.nombreQuien(usuario));
                }
                lista.add(cambio);
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar el historial del pedido " + folio, e);
        }
        return lista;
    }

    @Override
    public List<Modificacion> listarModificaciones(int folio) throws DAOException {
        String sql = "SELECT m.Folio, m.DatosAnteriores, m.DatosNuevos, m.FechaHoraModificacion, u.NombreUsuario "
                + "FROM ModificacionPedido m LEFT JOIN Usuario u ON u.IdUsuario = m.IdUsuario "
                + "WHERE m.Folio = ? ORDER BY m.FechaHoraModificacion, m.IdModificacion";
        List<Modificacion> lista = new ArrayList<Modificacion>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                Modificacion modificacion = new Modificacion();
                modificacion.setFolio(resultado.getInt("Folio"));
                modificacion.setDatosAnteriores(resultado.getString("DatosAnteriores"));
                modificacion.setDatosNuevos(resultado.getString("DatosNuevos"));
                modificacion.setFecha(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaHoraModificacion"));
                modificacion.setNombreUsuario(ApoyoPedidosMySQL.nombreQuien(resultado.getString("NombreUsuario")));
                lista.add(modificacion);
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al consultar las modificaciones del pedido " + folio, e);
        }
        return lista;
    }

    // ===================== Dentro de una transacción =====================

    @Override
    public void registrarCambio(Connection conexion, int folio, int idUsuario, String anterior, String nuevo)
            throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "INSERT INTO HistorialEstado (Folio, IdUsuario, EstadoAnterior, EstadoNuevo) VALUES (?, ?, ?, ?)");
            consulta.setInt(1, folio);
            ApoyoPedidosMySQL.ponerUsuario(consulta, 2, idUsuario);
            ApoyoPedidosMySQL.ponerTexto(consulta, 3, anterior);
            consulta.setString(4, nuevo);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al registrar el cambio de estado del pedido " + folio, e);
        }
    }

    @Override
    public void registrarCancelacion(Connection conexion, Cancelacion cancelacion, int idUsuario) throws DAOException {
        String sql = "INSERT INTO Cancelacion (Folio, IdUsuario, MotivoCancelacion, MontoReembolso, EstadoReembolso) "
                + "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, cancelacion.getFolio());
            ApoyoPedidosMySQL.ponerUsuario(consulta, 2, idUsuario);
            consulta.setString(3, cancelacion.getMotivo().trim());
            consulta.setDouble(4, cancelacion.getMontoReembolso());
            consulta.setString(5, cancelacion.getEstadoReembolso());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al registrar la cancelación del pedido " + cancelacion.getFolio(), e);
        }
    }

    @Override
    public void registrarBaja(Connection conexion, BajaPedido baja, int idUsuario) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "INSERT INTO BajaPedido (Folio, MotivoBaja, IdUsuario) VALUES (?, ?, ?)");
            consulta.setInt(1, baja.getFolio());
            consulta.setString(2, baja.getMotivo().trim());
            ApoyoPedidosMySQL.ponerUsuario(consulta, 3, idUsuario);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al registrar la baja del pedido " + baja.getFolio(), e);
        }
    }

    @Override
    public void registrarModificacion(Connection conexion, int folio, int idUsuario, String antes, String despues)
            throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "INSERT INTO ModificacionPedido (Folio, IdUsuario, DatosAnteriores, DatosNuevos) VALUES (?, ?, ?, ?)");
            consulta.setInt(1, folio);
            ApoyoPedidosMySQL.ponerUsuario(consulta, 2, idUsuario);
            consulta.setString(3, antes);
            consulta.setString(4, despues);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al registrar la modificación del pedido " + folio, e);
        }
    }

    // Si la causa es que falta la migración V4, lo dice
    private DAOException error(String mensaje, SQLException causa) {
        if (ApoyoPedidosMySQL.faltaMigracion(causa) || String.valueOf(causa.getMessage()).contains("doesn't exist")) {
            return new DAOException(MSG_FALTA_MIGRACION, causa);
        }
        return new DAOException(mensaje, causa);
    }
}
