package com.pac.modulos.inventario.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.LoteDAO;
import com.pac.modulos.inventario.model.Lote;
import com.pac.modulos.inventario.model.UnidadMedida;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de LoteDAO para MySQL (tabla Lote).
 *
 * Orden PEPS (primeras entradas, primeras salidas): primero el lote que caduca antes;
 * los lotes sin caducidad van al final; si empatan, el que se compró antes.
 */
public class LoteDAOMySQL implements LoteDAO {

    private static final String SELECT_LOTE =
            "SELECT l.IdLote, l.IdInsumo, l.CantidadComprada, l.ExistenciaLote, l.CostoCompra, l.FechaCompra, "
            + "l.Proveedor, l.FechaCaducidad, i.NombreInsumo, i.UnidadMedida "
            + "FROM Lote l JOIN Insumo i ON i.IdInsumo = l.IdInsumo ";

    private static final String ORDEN_PEPS =
            " ORDER BY (l.FechaCaducidad IS NULL), l.FechaCaducidad, l.FechaCompra, l.IdLote";

    // ===================== Consultas =====================

    @Override
    public List<Lote> listarPorInsumo(int idInsumo, boolean soloConExistencia) throws DAOException {
        String sql = SELECT_LOTE + "WHERE l.IdInsumo = ?";
        if (soloConExistencia) {
            sql = sql + " AND l.ExistenciaLote > 0";
        }
        sql = sql + ORDEN_PEPS;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idInsumo);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los lotes del insumo " + idInsumo, e);
        }
    }

    @Override
    public List<Lote> listarConExistencia() throws DAOException {
        String sql = SELECT_LOTE + "WHERE l.ExistenciaLote > 0 AND i.EstadoInsumo = 'Activo'" + ORDEN_PEPS;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los lotes con existencia", e);
        }
    }

    @Override
    public Lote buscarPorId(int idLote) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(SELECT_LOTE + "WHERE l.IdLote = ?");
            consulta.setInt(1, idLote);
            List<Lote> lista = leerLista(consulta);
            if (lista.isEmpty()) {
                return null;
            }
            return lista.get(0);
        } catch (SQLException e) {
            throw new DAOException("Error al buscar el lote " + idLote, e);
        }
    }

    // ===================== Dentro de una transacción =====================

    @Override
    public List<Lote> bloquearParaDescuento(Connection conexion, int idInsumo) throws DAOException {
        String sql = SELECT_LOTE + "WHERE l.IdInsumo = ? AND l.ExistenciaLote > 0" + ORDEN_PEPS + " FOR UPDATE";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idInsumo);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al leer los lotes del insumo " + idInsumo, e);
        }
    }

    @Override
    public double sumarExistencia(Connection conexion, int idInsumo) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT COALESCE(SUM(ExistenciaLote), 0) FROM Lote WHERE IdInsumo = ?");
            consulta.setInt(1, idInsumo);
            ResultSet resultado = consulta.executeQuery();
            double total = 0;
            if (resultado.next()) {
                total = resultado.getDouble(1);
            }
            resultado.close();
            consulta.close();
            return total;
        } catch (SQLException e) {
            throw new DAOException("Error al calcular la existencia del insumo " + idInsumo, e);
        }
    }

    @Override
    public int insertar(Connection conexion, Lote lote) throws DAOException {
        String sql = "INSERT INTO Lote (IdInsumo, CantidadComprada, ExistenciaLote, CostoCompra, FechaCompra, "
                + "Proveedor, FechaCaducidad) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, lote.getIdInsumo());
            consulta.setDouble(2, lote.getCantidadComprada());
            consulta.setDouble(3, lote.getExistencia());
            consulta.setDouble(4, lote.getCostoCompra());
            consulta.setDate(5, Date.valueOf(lote.getFechaCompra()));
            consulta.setString(6, ApoyoMySQL.textoONulo(lote.getProveedor()));
            if (lote.getFechaCaducidad() == null) {
                consulta.setNull(7, Types.DATE);
            } else {
                consulta.setDate(7, Date.valueOf(lote.getFechaCaducidad()));
            }
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            lote.setIdLote(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            throw new DAOException("Error al registrar el lote", e);
        }
    }

    @Override
    public void actualizarExistencia(Connection conexion, int idLote, double existencia) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Lote SET ExistenciaLote = ? WHERE IdLote = ?");
            consulta.setDouble(1, existencia);
            consulta.setInt(2, idLote);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar el lote " + idLote, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Ejecuta la consulta y convierte cada fila en un Lote
    private List<Lote> leerLista(PreparedStatement consulta) throws SQLException {
        List<Lote> lista = new ArrayList<Lote>();
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            lista.add(leerLote(resultado));
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    // Convierte la fila actual del ResultSet en un objeto Lote
    private Lote leerLote(ResultSet resultado) throws SQLException {
        Lote lote = new Lote();
        lote.setIdLote(resultado.getInt("IdLote"));
        lote.setIdInsumo(resultado.getInt("IdInsumo"));
        lote.setCantidadComprada(resultado.getDouble("CantidadComprada"));
        lote.setExistencia(resultado.getDouble("ExistenciaLote"));
        lote.setCostoCompra(resultado.getDouble("CostoCompra"));
        lote.setFechaCompra(resultado.getDate("FechaCompra").toLocalDate());
        lote.setProveedor(resultado.getString("Proveedor"));
        Date caducidad = resultado.getDate("FechaCaducidad");
        if (caducidad != null) {
            lote.setFechaCaducidad(caducidad.toLocalDate());
        }
        lote.setNombreInsumo(resultado.getString("NombreInsumo"));
        lote.setUnidad(UnidadMedida.desdeBD(resultado.getString("UnidadMedida")));
        return lote;
    }
}
