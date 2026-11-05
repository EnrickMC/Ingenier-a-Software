package com.pac.modulos.inventario.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.AlertaDAO;
import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.TipoAlerta;
import com.pac.modulos.inventario.model.UnidadMedida;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de AlertaDAO para MySQL (tablas AlertaInventario y AlertaCaducidad).
 */
public class AlertaDAOMySQL implements AlertaDAO {

    // Límite de alertas atendidas que se muestran (las pendientes siempre salen todas)
    private static final int MAXIMO_LISTADO = 300;

    private static final String SELECT_ALERTA =
            "SELECT a.IdAlerta, a.IdInsumo, a.TipoAlerta, a.FechaAlerta, a.ExistenciaEnAlerta, a.EstadoAlerta, "
            + "i.NombreInsumo, i.UnidadMedida, ac.IdLote, l.FechaCaducidad, l.ExistenciaLote "
            + "FROM AlertaInventario a "
            + "JOIN Insumo i ON i.IdInsumo = a.IdInsumo "
            + "LEFT JOIN AlertaCaducidad ac ON ac.IdAlerta = a.IdAlerta "
            + "LEFT JOIN Lote l ON l.IdLote = ac.IdLote ";

    // ===================== Consultas =====================

    @Override
    public List<AlertaInventario> listar(boolean soloPendientes) throws DAOException {
        String sql = SELECT_ALERTA;
        if (soloPendientes) {
            sql = sql + "WHERE a.EstadoAlerta = 'Pendiente' ";
        }
        // Primero las pendientes y luego las más recientes
        sql = sql + "ORDER BY (a.EstadoAlerta = 'Pendiente') DESC, a.FechaAlerta DESC, a.IdAlerta DESC";
        if (!soloPendientes) {
            sql = sql + " LIMIT " + MAXIMO_LISTADO;
        }
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las alertas", e);
        }
    }

    @Override
    public List<AlertaInventario> listarPendientesDeCaducidad() throws DAOException {
        String sql = SELECT_ALERTA + "WHERE a.EstadoAlerta = 'Pendiente' AND ac.IdLote IS NOT NULL";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las alertas de caducidad", e);
        }
    }

    @Override
    public int contarPendientes() throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM AlertaInventario WHERE EstadoAlerta = 'Pendiente'");
            ResultSet resultado = consulta.executeQuery();
            int total = 0;
            if (resultado.next()) {
                total = resultado.getInt(1);
            }
            resultado.close();
            consulta.close();
            return total;
        } catch (SQLException e) {
            throw new DAOException("Error al contar las alertas pendientes", e);
        }
    }

    @Override
    public AlertaInventario buscarPorId(int idAlerta) throws DAOException {
        return buscarUna(SELECT_ALERTA + "WHERE a.IdAlerta = ?", idAlerta);
    }

    @Override
    public AlertaInventario buscarPendienteDeExistencia(int idInsumo) throws DAOException {
        String sql = SELECT_ALERTA + "WHERE a.IdInsumo = ? AND a.EstadoAlerta = 'Pendiente' "
                + "AND a.TipoAlerta = 'Existencia baja' ORDER BY a.IdAlerta LIMIT 1";
        return buscarUna(sql, idInsumo);
    }

    @Override
    public AlertaInventario buscarPendienteDeLote(int idLote) throws DAOException {
        String sql = SELECT_ALERTA + "WHERE ac.IdLote = ? AND a.EstadoAlerta = 'Pendiente' ORDER BY a.IdAlerta LIMIT 1";
        return buscarUna(sql, idLote);
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(AlertaInventario alerta) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(
                    "INSERT INTO AlertaInventario (IdInsumo, TipoAlerta, ExistenciaEnAlerta, EstadoAlerta) "
                    + "VALUES (?, ?, ?, 'Pendiente')", Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, alerta.getIdInsumo());
            consulta.setString(2, alerta.getTipo().getValorBD());
            consulta.setDouble(3, alerta.getExistenciaEnAlerta());
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();

            // Las alertas de caducidad guardan también el lote
            if (alerta.getIdLote() > 0) {
                PreparedStatement detalle = conexion.prepareStatement(
                        "INSERT INTO AlertaCaducidad (IdAlerta, IdLote) VALUES (?, ?)");
                detalle.setInt(1, idNuevo);
                detalle.setInt(2, alerta.getIdLote());
                detalle.executeUpdate();
                detalle.close();
            }

            conexion.commit();
            alerta.setIdAlerta(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al registrar la alerta", e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void actualizar(int idAlerta, TipoAlerta tipo, double existenciaEnAlerta) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE AlertaInventario SET TipoAlerta = ?, ExistenciaEnAlerta = ? WHERE IdAlerta = ?");
            consulta.setString(1, tipo.getValorBD());
            consulta.setDouble(2, existenciaEnAlerta);
            consulta.setInt(3, idAlerta);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar la alerta " + idAlerta, e);
        }
    }

    @Override
    public void marcarAtendida(int idAlerta) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "UPDATE AlertaInventario SET EstadoAlerta = 'Atendida' WHERE IdAlerta = ?",
                    idAlerta);
        } catch (SQLException e) {
            throw new DAOException("Error al atender la alerta " + idAlerta, e);
        }
    }

    @Override
    public void atenderPendientesDeInsumo(int idInsumo) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "UPDATE AlertaInventario SET EstadoAlerta = 'Atendida' "
                    + "WHERE IdInsumo = ? AND EstadoAlerta = 'Pendiente'", idInsumo);
        } catch (SQLException e) {
            throw new DAOException("Error al atender las alertas del insumo " + idInsumo, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    private AlertaInventario buscarUna(String sql, int id) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, id);
            List<AlertaInventario> lista = leerLista(consulta);
            if (lista.isEmpty()) {
                return null;
            }
            return lista.get(0);
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la alerta", e);
        }
    }

    private List<AlertaInventario> leerLista(PreparedStatement consulta) throws SQLException {
        List<AlertaInventario> lista = new ArrayList<AlertaInventario>();
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            lista.add(leerAlerta(resultado));
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    // Convierte la fila actual del ResultSet en un objeto AlertaInventario
    private AlertaInventario leerAlerta(ResultSet resultado) throws SQLException {
        AlertaInventario a = new AlertaInventario();
        a.setIdAlerta(resultado.getInt("IdAlerta"));
        a.setIdInsumo(resultado.getInt("IdInsumo"));
        a.setTipo(TipoAlerta.desdeBD(resultado.getString("TipoAlerta")));
        Timestamp fecha = resultado.getTimestamp("FechaAlerta");
        if (fecha != null) {
            a.setFecha(fecha.toLocalDateTime());
        }
        a.setExistenciaEnAlerta(resultado.getDouble("ExistenciaEnAlerta"));
        a.setPendiente("Pendiente".equalsIgnoreCase(resultado.getString("EstadoAlerta")));
        a.setNombreInsumo(resultado.getString("NombreInsumo"));
        a.setUnidad(UnidadMedida.desdeBD(resultado.getString("UnidadMedida")));
        a.setIdLote(resultado.getInt("IdLote"));          // NULL se lee como 0
        Date caducidad = resultado.getDate("FechaCaducidad");
        if (caducidad != null) {
            a.setFechaCaducidad(caducidad.toLocalDate());
        }
        a.setExistenciaLote(resultado.getDouble("ExistenciaLote"));
        return a;
    }
}
