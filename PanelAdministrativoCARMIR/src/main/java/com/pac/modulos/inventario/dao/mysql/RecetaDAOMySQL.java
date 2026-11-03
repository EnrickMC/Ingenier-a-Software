package com.pac.modulos.inventario.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.RecetaDAO;
import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.modulos.inventario.model.OpcionReceta;
import com.pac.modulos.inventario.model.Receta;
import com.pac.modulos.inventario.model.UnidadMedida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de RecetaDAO para MySQL (tablas Receta y RecetaInsumo).
 * El costo se calcula igual que la vista vw_CostoReceta: suma de CantidadInsumo × CostoUnitario.
 */
public class RecetaDAOMySQL implements RecetaDAO {

    private static final String SELECT_RECETA =
            "SELECT r.IdReceta, r.IdOpcion, r.NombreReceta, r.PasosPreparacion, r.TiempoElaboracion, r.EstadoReceta, "
            + "o.NombreOpcion, o.Disponible, o.MultiplicadorCosto, ca.NombreCaracteristica, cp.NombreCategoria, "
            + "(SELECT COALESCE(SUM(ri.CantidadInsumo * i.CostoUnitario), 0) FROM RecetaInsumo ri "
            + "   JOIN Insumo i ON i.IdInsumo = ri.IdInsumo WHERE ri.IdReceta = r.IdReceta) AS Costo, "
            + "(SELECT COUNT(*) FROM RecetaInsumo ri "
            + "   JOIN Insumo i ON i.IdInsumo = ri.IdInsumo "
            + "   WHERE ri.IdReceta = r.IdReceta AND i.EstadoInsumo = 'Inactivo') AS InsumosBaja "
            + "FROM Receta r "
            + "JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion "
            + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
            + "JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria ";

    private static final String SELECT_OPCION =
            "SELECT o.IdOpcion, o.NombreOpcion, o.Disponible, o.MultiplicadorCosto, ca.NombreCaracteristica, "
            + "ca.PermiteReceta, cp.NombreCategoria, r.IdReceta AS IdRecetaInactiva "
            + "FROM OpcionCaracteristica o "
            + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
            + "JOIN CategoriaPostre cp ON cp.IdCategoria = ca.IdCategoria "
            + "LEFT JOIN Receta r ON r.IdOpcion = o.IdOpcion AND r.EstadoReceta = 'Inactiva' ";

    // ===================== Consultas =====================

    @Override
    public List<Receta> listar(boolean incluirInactivas) throws DAOException {
        String sql = SELECT_RECETA;
        if (!incluirInactivas) {
            sql = sql + "WHERE r.EstadoReceta = 'Activa' ";
        }
        sql = sql + "ORDER BY cp.NombreCategoria, ca.OrdenMostrar, ca.NombreCaracteristica, o.NombreOpcion";

        List<Receta> lista = new ArrayList<Receta>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(leerReceta(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las recetas", e);
        }
        return lista;
    }

    @Override
    public Receta buscarPorId(int idReceta) throws DAOException {
        return buscarConIngredientes(SELECT_RECETA + "WHERE r.IdReceta = ?", idReceta);
    }

    @Override
    public Receta buscarPorOpcion(int idOpcion) throws DAOException {
        return buscarConIngredientes(SELECT_RECETA + "WHERE r.IdOpcion = ?", idOpcion);
    }

    @Override
    public List<OpcionReceta> listarOpcionesSinRecetaActiva() throws DAOException {
        String sql = SELECT_OPCION
                + "WHERE o.Disponible = 1 AND ca.Disponible = 1 AND ca.PermiteReceta = 1 AND cp.EstadoCategoria = 'Activa' "
                + "AND NOT EXISTS (SELECT 1 FROM Receta ra WHERE ra.IdOpcion = o.IdOpcion AND ra.EstadoReceta = 'Activa') "
                + "ORDER BY cp.NombreCategoria, ca.OrdenMostrar, ca.NombreCaracteristica, o.NombreOpcion";
        List<OpcionReceta> lista = new ArrayList<OpcionReceta>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(leerOpcion(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las opciones sin receta", e);
        }
        return lista;
    }

    @Override
    public OpcionReceta buscarOpcion(int idOpcion) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(SELECT_OPCION + "WHERE o.IdOpcion = ?");
            consulta.setInt(1, idOpcion);
            ResultSet resultado = consulta.executeQuery();
            OpcionReceta opcion = null;
            if (resultado.next()) {
                opcion = leerOpcion(resultado);
            }
            resultado.close();
            consulta.close();
            return opcion;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la opción " + idOpcion, e);
        }
    }

    @Override
    public boolean opcionEnPedidos(int idOpcion) throws DAOException {
        String sql = "SELECT COUNT(*) FROM DetallePedido d "
                + "JOIN PostreOpcion po ON po.IdPostre = d.IdPostre WHERE po.IdOpcion = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            return ApoyoMySQL.contar(conexion, sql, idOpcion) > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar los pedidos de la opción " + idOpcion, e);
        }
    }

    @Override
    public List<Integer> listarFoliosPorPreparar(int idOpcion) throws DAOException {
        // Pedidos que todavía no entran a preparación y que no están cancelados
        String sql = "SELECT DISTINCT p.Folio FROM Pedido p "
                + "JOIN DetallePedido d ON d.Folio = p.Folio "
                + "JOIN PostreOpcion po ON po.IdPostre = d.IdPostre "
                + "WHERE po.IdOpcion = ? "
                + "AND p.EstadoPedido IN ('En proceso de aceptacion', 'Aprobado', 'Pagado') "
                + "AND NOT EXISTS (SELECT 1 FROM Cancelacion c WHERE c.Folio = p.Folio) "
                + "ORDER BY p.Folio";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            return ApoyoMySQL.leerEnteros(conexion, sql, idOpcion);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los pedidos de la opción " + idOpcion, e);
        }
    }

    @Override
    public List<IngredienteReceta> listarIngredientes(Connection conexion, int idReceta) throws DAOException {
        String sql = "SELECT ri.IdInsumo, ri.CantidadInsumo, i.NombreInsumo, i.UnidadMedida, i.CostoUnitario, "
                + "i.EstadoInsumo FROM RecetaInsumo ri JOIN Insumo i ON i.IdInsumo = ri.IdInsumo "
                + "WHERE ri.IdReceta = ? ORDER BY i.NombreInsumo";
        List<IngredienteReceta> lista = new ArrayList<IngredienteReceta>();
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idReceta);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                IngredienteReceta ingrediente = new IngredienteReceta();
                ingrediente.setIdInsumo(resultado.getInt("IdInsumo"));
                ingrediente.setCantidad(resultado.getDouble("CantidadInsumo"));
                ingrediente.setNombreInsumo(resultado.getString("NombreInsumo"));
                ingrediente.setUnidad(UnidadMedida.desdeBD(resultado.getString("UnidadMedida")));
                ingrediente.setCostoUnitario(resultado.getDouble("CostoUnitario"));
                ingrediente.setInsumoActivo("Activo".equalsIgnoreCase(resultado.getString("EstadoInsumo")));
                lista.add(ingrediente);
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los ingredientes de la receta " + idReceta, e);
        }
        return lista;
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Receta receta) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(
                    "INSERT INTO Receta (IdOpcion, NombreReceta, PasosPreparacion, TiempoElaboracion, EstadoReceta) "
                    + "VALUES (?, ?, ?, ?, 'Activa')", Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, receta.getIdOpcion());
            consulta.setString(2, receta.getNombre());
            consulta.setString(3, receta.getPasos());
            consulta.setInt(4, receta.getTiempoMinutos());
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();

            insertarIngredientes(conexion, idNuevo, receta.getIngredientes());
            conexion.commit();
            receta.setIdReceta(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al registrar la receta " + receta.getNombre(), e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void actualizar(Receta receta) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Receta SET NombreReceta = ?, PasosPreparacion = ?, TiempoElaboracion = ?, "
                    + "EstadoReceta = 'Activa' WHERE IdReceta = ?");
            consulta.setString(1, receta.getNombre());
            consulta.setString(2, receta.getPasos());
            consulta.setInt(3, receta.getTiempoMinutos());
            consulta.setInt(4, receta.getIdReceta());
            consulta.executeUpdate();
            consulta.close();

            // Los ingredientes se reemplazan completos
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM RecetaInsumo WHERE IdReceta = ?", receta.getIdReceta());
            insertarIngredientes(conexion, receta.getIdReceta(), receta.getIngredientes());

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al actualizar la receta " + receta.getNombre(), e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void inhabilitar(int idReceta) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "UPDATE Receta SET EstadoReceta = 'Inactiva' WHERE IdReceta = ?", idReceta);
        } catch (SQLException e) {
            throw new DAOException("Error al inhabilitar la receta " + idReceta, e);
        }
    }

    @Override
    public void eliminar(int idReceta) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            // Sus filas de RecetaInsumo se borran solas (ON DELETE CASCADE)
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM Receta WHERE IdReceta = ?", idReceta);
        } catch (SQLException e) {
            throw new DAOException("Error al eliminar la receta " + idReceta, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    private void insertarIngredientes(Connection conexion, int idReceta, List<IngredienteReceta> ingredientes)
            throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(
                "INSERT INTO RecetaInsumo (IdReceta, IdInsumo, CantidadInsumo) VALUES (?, ?, ?)");
        for (IngredienteReceta ingrediente : ingredientes) {
            consulta.setInt(1, idReceta);
            consulta.setInt(2, ingrediente.getIdInsumo());
            consulta.setDouble(3, ingrediente.getCantidad());
            consulta.executeUpdate();
        }
        consulta.close();
    }

    // Lee una receta con su lista de ingredientes; null si no existe
    private Receta buscarConIngredientes(String sql, int id) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, id);
            ResultSet resultado = consulta.executeQuery();
            Receta receta = null;
            if (resultado.next()) {
                receta = leerReceta(resultado);
            }
            resultado.close();
            consulta.close();
            if (receta != null) {
                receta.setIngredientes(listarIngredientes(conexion, receta.getIdReceta()));
            }
            return receta;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la receta", e);
        }
    }

    // Convierte la fila actual del ResultSet en un objeto Receta (sin ingredientes)
    private Receta leerReceta(ResultSet resultado) throws SQLException {
        Receta r = new Receta();
        r.setIdReceta(resultado.getInt("IdReceta"));
        r.setIdOpcion(resultado.getInt("IdOpcion"));
        r.setNombre(resultado.getString("NombreReceta"));
        r.setPasos(resultado.getString("PasosPreparacion"));
        r.setTiempoMinutos(resultado.getInt("TiempoElaboracion"));
        r.setActiva("Activa".equalsIgnoreCase(resultado.getString("EstadoReceta")));
        r.setNombreOpcion(resultado.getString("NombreOpcion"));
        r.setOpcionDisponible(resultado.getBoolean("Disponible"));
        r.setMultiplicadorCosto(resultado.getDouble("MultiplicadorCosto"));
        r.setNombreCaracteristica(resultado.getString("NombreCaracteristica"));
        r.setNombreCategoria(resultado.getString("NombreCategoria"));
        r.setCostoListado(Math.round(resultado.getDouble("Costo") * 100) / 100.0);
        r.setInsumosDadosDeBaja(resultado.getInt("InsumosBaja"));
        return r;
    }

    // Convierte la fila actual del ResultSet en un objeto OpcionReceta
    private OpcionReceta leerOpcion(ResultSet resultado) throws SQLException {
        OpcionReceta o = new OpcionReceta();
        o.setIdOpcion(resultado.getInt("IdOpcion"));
        o.setNombreOpcion(resultado.getString("NombreOpcion"));
        o.setDisponible(resultado.getBoolean("Disponible"));
        o.setMultiplicadorCosto(resultado.getDouble("MultiplicadorCosto"));
        o.setPermiteReceta(resultado.getBoolean("PermiteReceta"));
        o.setNombreCaracteristica(resultado.getString("NombreCaracteristica"));
        o.setNombreCategoria(resultado.getString("NombreCategoria"));
        o.setIdRecetaInactiva(resultado.getInt("IdRecetaInactiva"));     // NULL se lee como 0
        return o;
    }
}
