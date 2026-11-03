package com.pac.modulos.catalogo.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.CategoriaDAO;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de CategoriaDAO para MySQL (tabla CategoriaPostre).
 * El número de productos cuenta solo los postres del catálogo (con nombre).
 */
public class CategoriaDAOMySQL implements CategoriaDAO {

    private static final String SELECT_CATEGORIA =
            "SELECT c.IdCategoria, c.NombreCategoria, c.DescripcionCategoria, c.EstadoCategoria, "
            + "(SELECT COUNT(*) FROM Postre p WHERE p.IdCategoria = c.IdCategoria AND p.NombrePostre IS NOT NULL) AS NumeroProductos "
            + "FROM CategoriaPostre c";

    // ===================== Consultas =====================

    @Override
    public List<Categoria> buscar(String texto, EstadoRegistro estado) throws DAOException {
        String sql = SELECT_CATEGORIA + " WHERE (c.NombreCategoria LIKE ? OR c.DescripcionCategoria LIKE ?)";
        if (estado != null) {
            sql = sql + " AND c.EstadoCategoria = ?";
        }
        sql = sql + " ORDER BY c.NombreCategoria";

        List<Categoria> categorias = new ArrayList<Categoria>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, ApoyoMySQL.patron(texto));
            consulta.setString(2, ApoyoMySQL.patron(texto));
            if (estado != null) {
                consulta.setString(3, estado.getValorBD());
            }
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                categorias.add(leerCategoria(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las categorías", e);
        }
        return categorias;
    }

    @Override
    public Categoria buscarPorId(int idCategoria) throws DAOException {
        String sql = SELECT_CATEGORIA + " WHERE c.IdCategoria = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCategoria);
            Categoria categoria = leerUna(consulta);
            consulta.close();
            return categoria;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la categoría " + idCategoria, e);
        }
    }

    @Override
    public Categoria buscarPorNombre(String nombre) throws DAOException {
        // La comparación no distingue mayúsculas ni acentos (collation utf8mb4_unicode_ci)
        String sql = SELECT_CATEGORIA + " WHERE c.NombreCategoria = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, nombre.trim());
            Categoria categoria = leerUna(consulta);
            consulta.close();
            return categoria;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la categoría " + nombre, e);
        }
    }

    @Override
    public List<String> listarProductosActivos(int idCategoria) throws DAOException {
        String sql = "SELECT COALESCE(NombrePostre, CONCAT('Postre #', IdPostre)) FROM Postre "
                + "WHERE IdCategoria = ? AND MostrarEnTienda = 1 ORDER BY NombrePostre";
        List<String> nombres = new ArrayList<String>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCategoria);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                nombres.add(resultado.getString(1));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los productos de la categoría " + idCategoria, e);
        }
        return nombres;
    }

    @Override
    public boolean tieneHistorial(int idCategoria) throws DAOException {
        String sqlPostres = "SELECT COUNT(*) FROM Postre WHERE IdCategoria = ?";
        String sqlRecetas = "SELECT COUNT(*) FROM Receta r "
                + "JOIN OpcionCaracteristica o ON o.IdOpcion = r.IdOpcion "
                + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
                + "WHERE ca.IdCategoria = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            int postres = ApoyoMySQL.contar(conexion, sqlPostres, idCategoria);
            int recetas = ApoyoMySQL.contar(conexion, sqlRecetas, idCategoria);
            return postres > 0 || recetas > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar el historial de la categoría " + idCategoria, e);
        }
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Categoria categoria) throws DAOException {
        String sql = "INSERT INTO CategoriaPostre (NombreCategoria, DescripcionCategoria, EstadoCategoria) VALUES (?, ?, ?)";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setString(1, categoria.getNombre());
            consulta.setString(2, ApoyoMySQL.textoONulo(categoria.getDescripcion()));
            consulta.setString(3, categoria.getEstado().getValorBD());
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            categoria.setIdCategoria(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            throw new DAOException("Error al registrar la categoría " + categoria.getNombre(), e);
        }
    }

    @Override
    public void actualizar(Categoria categoria) throws DAOException {
        String sql = "UPDATE CategoriaPostre SET NombreCategoria = ?, DescripcionCategoria = ?, EstadoCategoria = ? "
                + "WHERE IdCategoria = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, categoria.getNombre());
            consulta.setString(2, ApoyoMySQL.textoONulo(categoria.getDescripcion()));
            consulta.setString(3, categoria.getEstado().getValorBD());
            consulta.setInt(4, categoria.getIdCategoria());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar la categoría " + categoria.getNombre(), e);
        }
    }

    @Override
    public void inhabilitar(int idCategoria) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. La categoría pasa a Inactiva
            ApoyoMySQL.ejecutar(conexion,
                    "UPDATE CategoriaPostre SET EstadoCategoria = 'Inactiva' WHERE IdCategoria = ?", idCategoria);
            // 2. Sus opciones dejan de estar disponibles
            ApoyoMySQL.ejecutar(conexion,
                    "UPDATE OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
                    + "SET o.Disponible = 0 WHERE ca.IdCategoria = ?", idCategoria);
            // 3. Sus características dejan de estar disponibles
            ApoyoMySQL.ejecutar(conexion,
                    "UPDATE Caracteristica SET Disponible = 0 WHERE IdCategoria = ?", idCategoria);

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al inhabilitar la categoría " + idCategoria, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void eliminar(int idCategoria) throws DAOException {
        // Subconsulta con las opciones de la categoría
        String opcionesDeCategoria = "SELECT o.IdOpcion FROM OpcionCaracteristica o "
                + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica WHERE ca.IdCategoria = ?";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // Se borra en orden: primero lo que depende de otras tablas
            PreparedStatement consulta = conexion.prepareStatement(
                    "DELETE FROM ExclusionOpciones WHERE IdOpcion IN (" + opcionesDeCategoria + ") "
                    + "OR IdOpcionExcluida IN (" + opcionesDeCategoria + ")");
            consulta.setInt(1, idCategoria);
            consulta.setInt(2, idCategoria);
            consulta.executeUpdate();
            consulta.close();

            ApoyoMySQL.ejecutar(conexion,
                    "DELETE o FROM OpcionCaracteristica o JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
                    + "WHERE ca.IdCategoria = ?", idCategoria);
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM Caracteristica WHERE IdCategoria = ?", idCategoria);
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM CategoriaPostre WHERE IdCategoria = ?", idCategoria);

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al eliminar la categoría " + idCategoria, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Ejecuta una consulta que devuelve a lo más una categoría
    private Categoria leerUna(PreparedStatement consulta) throws SQLException {
        ResultSet resultado = consulta.executeQuery();
        Categoria categoria = null;
        if (resultado.next()) {
            categoria = leerCategoria(resultado);
        }
        resultado.close();
        return categoria;
    }

    // Convierte la fila actual del ResultSet en un objeto Categoria
    private Categoria leerCategoria(ResultSet resultado) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(resultado.getInt("IdCategoria"));
        categoria.setNombre(resultado.getString("NombreCategoria"));
        categoria.setDescripcion(resultado.getString("DescripcionCategoria"));
        categoria.setEstado(EstadoRegistro.desdeBD(resultado.getString("EstadoCategoria")));
        categoria.setNumeroProductos(resultado.getInt("NumeroProductos"));
        return categoria;
    }
}
