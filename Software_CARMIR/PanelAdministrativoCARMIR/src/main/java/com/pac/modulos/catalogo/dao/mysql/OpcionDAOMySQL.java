package com.pac.modulos.catalogo.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.model.Opcion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de OpcionDAO para MySQL (tabla OpcionCaracteristica).
 * El costo de receta viene de la vista vw_CostoReceta (solo la receta Activa de la opción);
 * si la opción no tiene receta activa, queda en NULL.
 */
public class OpcionDAOMySQL implements OpcionDAO {

    private static final String SELECT_OPCION =
            "SELECT o.IdOpcion, o.IdCaracteristica, o.NombreOpcion, o.DescripcionOpcion, o.LinkImagen, "
            + "o.MultiplicadorIngredientes, o.MultiplicadorCosto, o.SeEscala, o.Disponible, o.PuntosSeleccion, "
            + "o.MultiplicadorPresupuesto, ca.NombreCaracteristica, cr.CostoReceta "
            + "FROM OpcionCaracteristica o "
            + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
            + "LEFT JOIN Receta re ON re.IdOpcion = o.IdOpcion AND re.EstadoReceta = 'Activa' "
            + "LEFT JOIN vw_CostoReceta cr ON cr.IdReceta = re.IdReceta";

    // ===================== Consultas =====================

    @Override
    public List<Opcion> listarPorCaracteristica(int idCaracteristica) throws DAOException {
        String sql = SELECT_OPCION + " WHERE o.IdCaracteristica = ? ORDER BY o.NombreOpcion";
        return listar(sql, idCaracteristica);
    }

    @Override
    public List<Opcion> listarPorCategoria(int idCategoria) throws DAOException {
        String sql = SELECT_OPCION + " WHERE ca.IdCategoria = ? ORDER BY ca.OrdenMostrar, ca.NombreCaracteristica, o.NombreOpcion";
        return listar(sql, idCategoria);
    }

    @Override
    public Opcion buscarPorId(int idOpcion) throws DAOException {
        List<Opcion> lista = listar(SELECT_OPCION + " WHERE o.IdOpcion = ?", idOpcion);
        if (lista.isEmpty()) {
            return null;
        }
        return lista.get(0);
    }

    @Override
    public Opcion buscarPorNombre(int idCaracteristica, String nombre) throws DAOException {
        String sql = SELECT_OPCION + " WHERE o.IdCaracteristica = ? AND o.NombreOpcion = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCaracteristica);
            consulta.setString(2, nombre.trim());
            ResultSet resultado = consulta.executeQuery();
            Opcion opcion = null;
            if (resultado.next()) {
                opcion = leerOpcion(resultado);
            }
            resultado.close();
            consulta.close();
            return opcion;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la opción " + nombre, e);
        }
    }

    @Override
    public int contarDisponibles(int idCaracteristica) throws DAOException {
        String sql = "SELECT COUNT(*) FROM OpcionCaracteristica WHERE IdCaracteristica = ? AND Disponible = 1";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            return ApoyoMySQL.contar(conexion, sql, idCaracteristica);
        } catch (SQLException e) {
            throw new DAOException("Error al contar las opciones de la característica " + idCaracteristica, e);
        }
    }

    @Override
    public boolean tieneHistorial(int idOpcion) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            int postres = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM PostreOpcion WHERE IdOpcion = ?", idOpcion);
            int recetas = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM Receta WHERE IdOpcion = ?", idOpcion);
            return postres > 0 || recetas > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar el historial de la opción " + idOpcion, e);
        }
    }

    @Override
    public List<Integer> listarFoliosVigentes(int idOpcion) throws DAOException {
        String sql = "SELECT DISTINCT p.Folio FROM Pedido p "
                + "JOIN DetallePedido d ON d.Folio = p.Folio "
                + "JOIN PostreOpcion po ON po.IdPostre = d.IdPostre "
                + "WHERE po.IdOpcion = ? AND " + ApoyoMySQL.CONDICION_PEDIDO_VIGENTE + " ORDER BY p.Folio";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            return ApoyoMySQL.leerEnteros(conexion, sql, idOpcion);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los pedidos de la opción " + idOpcion, e);
        }
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Opcion opcion) throws DAOException {
        String sql = "INSERT INTO OpcionCaracteristica (IdCaracteristica, NombreOpcion, DescripcionOpcion, LinkImagen, "
                + "MultiplicadorIngredientes, MultiplicadorCosto, SeEscala, Disponible, PuntosSeleccion, "
                + "MultiplicadorPresupuesto) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, opcion.getIdCaracteristica());
            llenarDatos(consulta, opcion, 2);
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            opcion.setIdOpcion(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            throw new DAOException("Error al registrar la opción " + opcion.getNombre(), e);
        }
    }

    @Override
    public void actualizar(Opcion opcion) throws DAOException {
        String sql = "UPDATE OpcionCaracteristica SET NombreOpcion = ?, DescripcionOpcion = ?, LinkImagen = ?, "
                + "MultiplicadorIngredientes = ?, MultiplicadorCosto = ?, SeEscala = ?, Disponible = ?, "
                + "PuntosSeleccion = ?, MultiplicadorPresupuesto = ? WHERE IdOpcion = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            llenarDatos(consulta, opcion, 1);
            consulta.setInt(10, opcion.getIdOpcion());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar la opción " + opcion.getNombre(), e);
        }
    }

    @Override
    public void inhabilitar(int idOpcion) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE ExclusionOpciones SET EstadoExclusion = 'Inactiva' WHERE IdOpcion = ? OR IdOpcionExcluida = ?");
            consulta.setInt(1, idOpcion);
            consulta.setInt(2, idOpcion);
            consulta.executeUpdate();
            consulta.close();

            ApoyoMySQL.ejecutar(conexion, "UPDATE Receta SET EstadoReceta = 'Inactiva' WHERE IdOpcion = ?", idOpcion);
            ApoyoMySQL.ejecutar(conexion, "UPDATE OpcionCaracteristica SET Disponible = 0 WHERE IdOpcion = ?", idOpcion);

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al inhabilitar la opción " + idOpcion, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void eliminar(int idOpcion) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(
                    "DELETE FROM ExclusionOpciones WHERE IdOpcion = ? OR IdOpcionExcluida = ?");
            consulta.setInt(1, idOpcion);
            consulta.setInt(2, idOpcion);
            consulta.executeUpdate();
            consulta.close();

            ApoyoMySQL.ejecutar(conexion, "DELETE FROM OpcionCaracteristica WHERE IdOpcion = ?", idOpcion);

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al eliminar la opción " + idOpcion, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Ejecuta una consulta que recibe un id y devuelve varias opciones
    private List<Opcion> listar(String sql, int id) throws DAOException {
        List<Opcion> lista = new ArrayList<Opcion>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, id);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(leerOpcion(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las opciones", e);
        }
        return lista;
    }

    // Llena los 9 datos de la opción a partir de la posición indicada
    private void llenarDatos(PreparedStatement consulta, Opcion o, int inicio) throws SQLException {
        consulta.setString(inicio, o.getNombre());
        consulta.setString(inicio + 1, ApoyoMySQL.textoONulo(o.getDescripcion()));
        consulta.setString(inicio + 2, ApoyoMySQL.textoONulo(o.getLinkImagen()));
        consulta.setDouble(inicio + 3, o.getMultiplicadorIngredientes());
        consulta.setDouble(inicio + 4, o.getMultiplicadorCosto());
        consulta.setBoolean(inicio + 5, o.isSeEscala());
        consulta.setBoolean(inicio + 6, o.isDisponible());
        consulta.setDouble(inicio + 7, o.getPuntosSeleccion());
        consulta.setDouble(inicio + 8, o.getMultiplicadorPresupuesto());
    }

    // Convierte la fila actual del ResultSet en un objeto Opcion
    private Opcion leerOpcion(ResultSet resultado) throws SQLException {
        Opcion o = new Opcion();
        o.setIdOpcion(resultado.getInt("IdOpcion"));
        o.setIdCaracteristica(resultado.getInt("IdCaracteristica"));
        o.setNombre(resultado.getString("NombreOpcion"));
        o.setDescripcion(resultado.getString("DescripcionOpcion"));
        o.setLinkImagen(resultado.getString("LinkImagen"));
        o.setMultiplicadorIngredientes(resultado.getDouble("MultiplicadorIngredientes"));
        o.setMultiplicadorCosto(resultado.getDouble("MultiplicadorCosto"));
        o.setSeEscala(resultado.getBoolean("SeEscala"));
        o.setDisponible(resultado.getBoolean("Disponible"));
        o.setPuntosSeleccion(resultado.getDouble("PuntosSeleccion"));
        o.setMultiplicadorPresupuesto(resultado.getDouble("MultiplicadorPresupuesto"));
        o.setNombreCaracteristica(resultado.getString("NombreCaracteristica"));

        // Si no tiene receta, la vista no devuelve fila y la columna llega en NULL
        double costo = resultado.getDouble("CostoReceta");
        if (resultado.wasNull()) {
            o.setTieneReceta(false);
            o.setCostoReceta(0);
        } else {
            o.setTieneReceta(true);
            o.setCostoReceta(costo);
        }
        return o;
    }
}
