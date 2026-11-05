package com.pac.modulos.inventario.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.InsumoDAO;
import com.pac.modulos.inventario.model.Insumo;
import com.pac.modulos.inventario.model.UnidadMedida;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de InsumoDAO para MySQL (tabla Insumo).
 * La existencia se calcula igual que la vista vw_ExistenciaInsumo: suma de ExistenciaLote.
 */
public class InsumoDAOMySQL implements InsumoDAO {

    // Insumo + existencia total + caducidad más próxima de los lotes que aún tienen existencia
    private static final String SELECT_INSUMO =
            "SELECT i.IdInsumo, i.NombreInsumo, i.CategoriaInsumo, i.UnidadMedida, i.CostoUnitario, i.StockMinimo, "
            + "i.Perecedero, i.EstadoInsumo, COALESCE(SUM(l.ExistenciaLote), 0) AS Existencia, "
            + "MIN(CASE WHEN l.ExistenciaLote > 0 THEN l.FechaCaducidad END) AS CaducidadProxima "
            + "FROM Insumo i LEFT JOIN Lote l ON l.IdInsumo = i.IdInsumo ";

    private static final String GROUP_BY_INSUMO =
            " GROUP BY i.IdInsumo, i.NombreInsumo, i.CategoriaInsumo, i.UnidadMedida, i.CostoUnitario, "
            + "i.StockMinimo, i.Perecedero, i.EstadoInsumo";

    // ===================== Consultas =====================

    @Override
    public List<Insumo> listar(boolean incluirInactivos) throws DAOException {
        String sql = SELECT_INSUMO;
        if (!incluirInactivos) {
            sql = sql + "WHERE i.EstadoInsumo = 'Activo'";
        }
        sql = sql + GROUP_BY_INSUMO + " ORDER BY i.NombreInsumo";

        List<Insumo> lista = new ArrayList<Insumo>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(leerInsumo(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los insumos", e);
        }
        return lista;
    }

    @Override
    public Insumo buscarPorId(int idInsumo) throws DAOException {
        String sql = SELECT_INSUMO + "WHERE i.IdInsumo = ?" + GROUP_BY_INSUMO;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idInsumo);
            return leerUno(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al buscar el insumo " + idInsumo, e);
        }
    }

    @Override
    public Insumo buscarPorNombre(String nombre) throws DAOException {
        String sql = SELECT_INSUMO + "WHERE i.NombreInsumo = ?" + GROUP_BY_INSUMO;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, nombre.trim());
            return leerUno(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al buscar el insumo " + nombre, e);
        }
    }

    @Override
    public List<String> listarCategorias() throws DAOException {
        List<String> lista = new ArrayList<String>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT DISTINCT CategoriaInsumo FROM Insumo ORDER BY CategoriaInsumo");
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(resultado.getString(1));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las categorías de insumo", e);
        }
        return lista;
    }

    @Override
    public boolean tieneHistorial(int idInsumo) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            int lotes = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM Lote WHERE IdInsumo = ?", idInsumo);
            int movimientos = ApoyoMySQL.contar(conexion,
                    "SELECT COUNT(*) FROM MovimientoInventario WHERE IdInsumo = ?", idInsumo);
            int alertas = ApoyoMySQL.contar(conexion,
                    "SELECT COUNT(*) FROM AlertaInventario WHERE IdInsumo = ?", idInsumo);
            int recetas = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM RecetaInsumo WHERE IdInsumo = ?", idInsumo);
            return lotes > 0 || movimientos > 0 || alertas > 0 || recetas > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar el historial del insumo " + idInsumo, e);
        }
    }

    @Override
    public boolean tieneLotesORecetas(int idInsumo) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            int lotes = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM Lote WHERE IdInsumo = ?", idInsumo);
            int recetas = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM RecetaInsumo WHERE IdInsumo = ?", idInsumo);
            return lotes > 0 || recetas > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar los lotes y recetas del insumo " + idInsumo, e);
        }
    }

    @Override
    public List<String> listarRecetasActivas(int idInsumo) throws DAOException {
        String sql = "SELECT r.NombreReceta, ca.NombreCaracteristica, o.NombreOpcion FROM RecetaInsumo ri "
                + "JOIN Receta r ON r.IdReceta = ri.IdReceta "
                + "JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion "
                + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
                + "WHERE ri.IdInsumo = ? AND r.EstadoReceta = 'Activa' ORDER BY r.NombreReceta";
        List<String> lista = new ArrayList<String>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idInsumo);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(resultado.getString(1) + " (" + resultado.getString(2) + ": " + resultado.getString(3) + ")");
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las recetas del insumo " + idInsumo, e);
        }
        return lista;
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Insumo insumo) throws DAOException {
        String sql = "INSERT INTO Insumo (NombreInsumo, CategoriaInsumo, UnidadMedida, CostoUnitario, StockMinimo, "
                + "Perecedero, EstadoInsumo) VALUES (?, ?, ?, ?, ?, ?, 'Activo')";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            llenarDatos(consulta, insumo);
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            insumo.setIdInsumo(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            throw new DAOException("Error al registrar el insumo " + insumo.getNombre(), e);
        }
    }

    @Override
    public void actualizar(Insumo insumo) throws DAOException {
        String sql = "UPDATE Insumo SET NombreInsumo = ?, CategoriaInsumo = ?, UnidadMedida = ?, CostoUnitario = ?, "
                + "StockMinimo = ?, Perecedero = ? WHERE IdInsumo = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            llenarDatos(consulta, insumo);
            consulta.setInt(7, insumo.getIdInsumo());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar el insumo " + insumo.getNombre(), e);
        }
    }

    @Override
    public void actualizarCosto(int idInsumo, double costoUnitario) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Insumo SET CostoUnitario = ? WHERE IdInsumo = ?");
            consulta.setDouble(1, costoUnitario);
            consulta.setInt(2, idInsumo);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar el costo del insumo " + idInsumo, e);
        }
    }

    @Override
    public void reactivar(int idInsumo) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "UPDATE Insumo SET EstadoInsumo = 'Activo' WHERE IdInsumo = ?", idInsumo);
        } catch (SQLException e) {
            throw new DAOException("Error al reactivar el insumo " + idInsumo, e);
        }
    }

    @Override
    public void inhabilitar(int idInsumo) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);
            ApoyoMySQL.ejecutar(conexion, "UPDATE Insumo SET EstadoInsumo = 'Inactivo' WHERE IdInsumo = ?", idInsumo);
            ApoyoMySQL.ejecutar(conexion, "UPDATE AlertaInventario SET EstadoAlerta = 'Atendida' "
                    + "WHERE IdInsumo = ? AND EstadoAlerta = 'Pendiente'", idInsumo);
            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al inhabilitar el insumo " + idInsumo, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void eliminar(int idInsumo) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM Insumo WHERE IdInsumo = ?", idInsumo);
        } catch (SQLException e) {
            throw new DAOException("Error al eliminar el insumo " + idInsumo, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Llena los 6 datos del insumo a partir de la posición 1
    private void llenarDatos(PreparedStatement consulta, Insumo insumo) throws SQLException {
        consulta.setString(1, insumo.getNombre());
        consulta.setString(2, insumo.getCategoria());
        consulta.setString(3, insumo.getUnidad().getValorBD());
        consulta.setDouble(4, insumo.getCostoUnitario());
        consulta.setDouble(5, insumo.getStockMinimo());
        consulta.setBoolean(6, insumo.isPerecedero());
    }

    // Ejecuta una consulta que devuelve un solo insumo (o null)
    private Insumo leerUno(PreparedStatement consulta) throws SQLException {
        ResultSet resultado = consulta.executeQuery();
        Insumo insumo = null;
        if (resultado.next()) {
            insumo = leerInsumo(resultado);
        }
        resultado.close();
        consulta.close();
        return insumo;
    }

    // Convierte la fila actual del ResultSet en un objeto Insumo
    private Insumo leerInsumo(ResultSet resultado) throws SQLException {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(resultado.getInt("IdInsumo"));
        insumo.setNombre(resultado.getString("NombreInsumo"));
        insumo.setCategoria(resultado.getString("CategoriaInsumo"));
        insumo.setUnidad(UnidadMedida.desdeBD(resultado.getString("UnidadMedida")));
        insumo.setCostoUnitario(resultado.getDouble("CostoUnitario"));
        insumo.setStockMinimo(resultado.getDouble("StockMinimo"));
        insumo.setPerecedero(resultado.getBoolean("Perecedero"));
        insumo.setActivo("Activo".equalsIgnoreCase(resultado.getString("EstadoInsumo")));
        insumo.setExistencia(resultado.getDouble("Existencia"));

        Date caducidad = resultado.getDate("CaducidadProxima");
        if (caducidad != null) {
            insumo.setCaducidadMasProxima(caducidad.toLocalDate());
        }
        return insumo;
    }
}
