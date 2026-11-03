package com.pac.modulos.inventario.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.MovimientoDAO;
import com.pac.modulos.inventario.model.Movimiento;
import com.pac.modulos.inventario.model.TipoMovimiento;
import com.pac.modulos.inventario.model.UnidadMedida;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de MovimientoDAO para MySQL (tabla MovimientoInventario).
 * Un IdUsuario vacío (NULL) significa que lo registró la Administradora o el sistema.
 */
public class MovimientoDAOMySQL implements MovimientoDAO {

    // Límite de filas del historial para que la tabla no se vuelva lenta
    private static final int MAXIMO_HISTORIAL = 500;

    private static final String SELECT_MOVIMIENTO =
            "SELECT m.IdMovimiento, m.IdInsumo, m.IdUsuario, m.Folio, m.TipoMovimiento, m.CantidadMovimiento, "
            + "m.Motivo, m.FechaMovimiento, m.CostoMovimiento, i.NombreInsumo, i.UnidadMedida, u.NombreUsuario "
            + "FROM MovimientoInventario m "
            + "JOIN Insumo i ON i.IdInsumo = m.IdInsumo "
            + "LEFT JOIN Usuario u ON u.IdUsuario = m.IdUsuario ";

    // ===================== Consultas =====================

    @Override
    public List<Movimiento> listar(int idInsumo, TipoMovimiento tipo, LocalDate desde, LocalDate hasta)
            throws DAOException {
        // Se arma el WHERE solo con los filtros que se usaron
        String sql = SELECT_MOVIMIENTO + "WHERE 1 = 1";
        if (idInsumo > 0) {
            sql = sql + " AND m.IdInsumo = ?";
        }
        if (tipo != null) {
            sql = sql + " AND m.TipoMovimiento = ?";
        }
        if (desde != null) {
            sql = sql + " AND m.FechaMovimiento >= ?";
        }
        if (hasta != null) {
            sql = sql + " AND m.FechaMovimiento < ?";
        }
        sql = sql + " ORDER BY m.FechaMovimiento DESC, m.IdMovimiento DESC LIMIT " + MAXIMO_HISTORIAL;

        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            int posicion = 1;
            if (idInsumo > 0) {
                consulta.setInt(posicion, idInsumo);
                posicion++;
            }
            if (tipo != null) {
                consulta.setString(posicion, tipo.getValorBD());
                posicion++;
            }
            if (desde != null) {
                consulta.setDate(posicion, Date.valueOf(desde));
                posicion++;
            }
            if (hasta != null) {
                // "hasta" incluye todo ese día: se compara contra el día siguiente
                consulta.setDate(posicion, Date.valueOf(hasta.plusDays(1)));
            }
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los movimientos", e);
        }
    }

    @Override
    public List<Movimiento> listarUltimos(int idInsumo, int cantidad) throws DAOException {
        String sql = SELECT_MOVIMIENTO + "WHERE m.IdInsumo = ? ORDER BY m.FechaMovimiento DESC, m.IdMovimiento DESC LIMIT ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idInsumo);
            consulta.setInt(2, cantidad);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los movimientos del insumo " + idInsumo, e);
        }
    }

    // ===================== Dentro de una transacción =====================

    @Override
    public int insertar(Connection conexion, Movimiento movimiento) throws DAOException {
        String sql = "INSERT INTO MovimientoInventario (IdInsumo, IdUsuario, Folio, TipoMovimiento, "
                + "CantidadMovimiento, Motivo, CostoMovimiento) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, movimiento.getIdInsumo());
            if (movimiento.getIdUsuario() > 0) {
                consulta.setInt(2, movimiento.getIdUsuario());
            } else {
                consulta.setNull(2, Types.INTEGER);
            }
            if (movimiento.getFolio() > 0) {
                consulta.setInt(3, movimiento.getFolio());
            } else {
                consulta.setNull(3, Types.INTEGER);
            }
            consulta.setString(4, movimiento.getTipo().getValorBD());
            consulta.setDouble(5, movimiento.getCantidad());
            consulta.setString(6, ApoyoMySQL.textoONulo(movimiento.getMotivo()));
            consulta.setDouble(7, movimiento.getCosto());
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            movimiento.setIdMovimiento(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            // Sin la migración V3, la columna IdUsuario no acepta NULL (caso de la Administradora)
            String detalle = String.valueOf(e.getMessage());
            if (movimiento.getIdUsuario() <= 0 && detalle.contains("IdUsuario")) {
                throw new DAOException(MSG_FALTA_MIGRACION, e);
            }
            throw new DAOException("Error al registrar el movimiento", e);
        }
    }

    @Override
    public boolean existeProduccionDeFolio(Connection conexion, int folio) throws DAOException {
        String sql = "SELECT COUNT(*) FROM MovimientoInventario WHERE Folio = ? AND TipoMovimiento = 'Produccion'";
        try {
            return ApoyoMySQL.contar(conexion, sql, folio) > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar los descuentos del pedido " + folio, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    private List<Movimiento> leerLista(PreparedStatement consulta) throws SQLException {
        List<Movimiento> lista = new ArrayList<Movimiento>();
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            lista.add(leerMovimiento(resultado));
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    // Convierte la fila actual del ResultSet en un objeto Movimiento
    private Movimiento leerMovimiento(ResultSet resultado) throws SQLException {
        Movimiento m = new Movimiento();
        m.setIdMovimiento(resultado.getInt("IdMovimiento"));
        m.setIdInsumo(resultado.getInt("IdInsumo"));
        m.setIdUsuario(resultado.getInt("IdUsuario"));     // NULL se lee como 0
        m.setFolio(resultado.getInt("Folio"));             // NULL se lee como 0
        m.setTipo(TipoMovimiento.desdeBD(resultado.getString("TipoMovimiento")));
        m.setCantidad(resultado.getDouble("CantidadMovimiento"));
        m.setMotivo(resultado.getString("Motivo"));
        Timestamp fecha = resultado.getTimestamp("FechaMovimiento");
        if (fecha != null) {
            m.setFecha(fecha.toLocalDateTime());
        }
        m.setCosto(resultado.getDouble("CostoMovimiento"));
        m.setNombreInsumo(resultado.getString("NombreInsumo"));
        m.setUnidad(UnidadMedida.desdeBD(resultado.getString("UnidadMedida")));

        // Sin usuario: lo registró la Administradora o el sistema (descuento automático)
        String usuario = resultado.getString("NombreUsuario");
        if (usuario == null) {
            if (m.getTipo() == TipoMovimiento.PRODUCCION) {
                usuario = "Sistema";
            } else {
                usuario = "Administración";
            }
        }
        m.setNombreUsuario(usuario);
        return m;
    }
}
