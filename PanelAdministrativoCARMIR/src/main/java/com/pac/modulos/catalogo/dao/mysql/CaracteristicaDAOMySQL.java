package com.pac.modulos.catalogo.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.CaracteristicaDAO;
import com.pac.modulos.catalogo.model.Caracteristica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de CaracteristicaDAO para MySQL (tabla Caracteristica).
 */
public class CaracteristicaDAOMySQL implements CaracteristicaDAO {

    private static final String SELECT_CARACTERISTICA =
            "SELECT ca.IdCaracteristica, ca.IdCategoria, ca.NombreCaracteristica, ca.DescripcionCaracteristica, "
            + "ca.Obligatoria, ca.SeleccionMultiple, ca.PresupuestoSeleccion, ca.OrdenMostrar, ca.Disponible, ca.PermiteReceta, "
            + "(SELECT COUNT(*) FROM OpcionCaracteristica o WHERE o.IdCaracteristica = ca.IdCaracteristica) AS NumeroOpciones "
            + "FROM Caracteristica ca";

    // Subconsulta con las opciones de una característica
    private static final String OPCIONES_DE_CARACTERISTICA =
            "SELECT IdOpcion FROM OpcionCaracteristica WHERE IdCaracteristica = ?";

    // ===================== Consultas =====================

    @Override
    public List<Caracteristica> listarPorCategoria(int idCategoria) throws DAOException {
        String sql = SELECT_CARACTERISTICA + " WHERE ca.IdCategoria = ? ORDER BY ca.OrdenMostrar, ca.NombreCaracteristica";
        List<Caracteristica> lista = new ArrayList<Caracteristica>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCategoria);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(leerCaracteristica(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las características de la categoría " + idCategoria, e);
        }
        return lista;
    }

    @Override
    public Caracteristica buscarPorId(int idCaracteristica) throws DAOException {
        String sql = SELECT_CARACTERISTICA + " WHERE ca.IdCaracteristica = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCaracteristica);
            Caracteristica caracteristica = leerUna(consulta);
            consulta.close();
            return caracteristica;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la característica " + idCaracteristica, e);
        }
    }

    @Override
    public Caracteristica buscarPorNombre(int idCategoria, String nombre) throws DAOException {
        String sql = SELECT_CARACTERISTICA + " WHERE ca.IdCategoria = ? AND ca.NombreCaracteristica = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCategoria);
            consulta.setString(2, nombre.trim());
            Caracteristica caracteristica = leerUna(consulta);
            consulta.close();
            return caracteristica;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la característica " + nombre, e);
        }
    }

    @Override
    public boolean ordenOcupado(int idCategoria, int orden, int idExcluir) throws DAOException {
        String sql = "SELECT COUNT(*) FROM Caracteristica WHERE IdCategoria = ? AND OrdenMostrar = ? AND IdCaracteristica <> ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCategoria);
            consulta.setInt(2, orden);
            consulta.setInt(3, idExcluir);
            ResultSet resultado = consulta.executeQuery();
            int numero = 0;
            if (resultado.next()) {
                numero = resultado.getInt(1);
            }
            resultado.close();
            consulta.close();
            return numero > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar el orden de muestra", e);
        }
    }

    @Override
    public boolean tieneHistorial(int idCaracteristica) throws DAOException {
        String sqlPostres = "SELECT COUNT(*) FROM PostreOpcion WHERE IdOpcion IN (" + OPCIONES_DE_CARACTERISTICA + ")";
        String sqlRecetas = "SELECT COUNT(*) FROM Receta WHERE IdOpcion IN (" + OPCIONES_DE_CARACTERISTICA + ")";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            int postres = ApoyoMySQL.contar(conexion, sqlPostres, idCaracteristica);
            int recetas = ApoyoMySQL.contar(conexion, sqlRecetas, idCaracteristica);
            return postres > 0 || recetas > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar el historial de la característica " + idCaracteristica, e);
        }
    }

    @Override
    public List<Integer> listarFoliosVigentes(int idCaracteristica) throws DAOException {
        String sql = "SELECT DISTINCT p.Folio FROM Pedido p "
                + "JOIN DetallePedido d ON d.Folio = p.Folio "
                + "JOIN PostreOpcion po ON po.IdPostre = d.IdPostre "
                + "JOIN OpcionCaracteristica o ON o.IdOpcion = po.IdOpcion "
                + "WHERE o.IdCaracteristica = ? AND " + ApoyoMySQL.CONDICION_PEDIDO_VIGENTE + " ORDER BY p.Folio";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            return ApoyoMySQL.leerEnteros(conexion, sql, idCaracteristica);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los pedidos de la característica " + idCaracteristica, e);
        }
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Caracteristica caracteristica, boolean recorrerOrden) throws DAOException {
        String sql = "INSERT INTO Caracteristica (IdCategoria, NombreCaracteristica, DescripcionCaracteristica, Obligatoria, "
                + "SeleccionMultiple, PresupuestoSeleccion, OrdenMostrar, Disponible, PermiteReceta) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. Si el orden está ocupado, las siguientes bajan una posición
            if (recorrerOrden) {
                recorrer(conexion, caracteristica.getIdCategoria(), caracteristica.getOrdenMostrar(), 0);
            }

            // 2. Se guarda la característica
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, caracteristica.getIdCategoria());
            llenarDatos(consulta, caracteristica, 2);
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();

            conexion.commit();
            caracteristica.setIdCaracteristica(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al registrar la característica " + caracteristica.getNombre(), e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void actualizar(Caracteristica caracteristica, boolean recorrerOrden) throws DAOException {
        String sql = "UPDATE Caracteristica SET NombreCaracteristica = ?, DescripcionCaracteristica = ?, Obligatoria = ?, "
                + "SeleccionMultiple = ?, PresupuestoSeleccion = ?, OrdenMostrar = ?, Disponible = ?, PermiteReceta = ? "
                + "WHERE IdCaracteristica = ?";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            if (recorrerOrden) {
                recorrer(conexion, caracteristica.getIdCategoria(), caracteristica.getOrdenMostrar(),
                        caracteristica.getIdCaracteristica());
            }

            PreparedStatement consulta = conexion.prepareStatement(sql);
            llenarDatos(consulta, caracteristica, 1);
            consulta.setInt(9, caracteristica.getIdCaracteristica());
            consulta.executeUpdate();
            consulta.close();

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al actualizar la característica " + caracteristica.getNombre(), e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void intercambiarOrden(Caracteristica primera, Caracteristica segunda) throws DAOException {
        String sql = "UPDATE Caracteristica SET OrdenMostrar = ? WHERE IdCaracteristica = ?";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            int ordenPrimera = primera.getOrdenMostrar();
            int ordenSegunda = segunda.getOrdenMostrar();
            // Si las dos tenían el mismo número, se separan para que el cambio se note
            if (ordenPrimera == ordenSegunda) {
                ordenSegunda = ordenPrimera + 1;
            }

            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, ordenSegunda);
            consulta.setInt(2, primera.getIdCaracteristica());
            consulta.executeUpdate();
            consulta.setInt(1, ordenPrimera);
            consulta.setInt(2, segunda.getIdCaracteristica());
            consulta.executeUpdate();
            consulta.close();

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al cambiar el orden de las características", e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void inhabilitar(int idCaracteristica) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. Exclusiones de sus opciones: Inactivas
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE ExclusionOpciones SET EstadoExclusion = 'Inactiva' "
                    + "WHERE IdOpcion IN (" + OPCIONES_DE_CARACTERISTICA + ") "
                    + "OR IdOpcionExcluida IN (" + OPCIONES_DE_CARACTERISTICA + ")");
            consulta.setInt(1, idCaracteristica);
            consulta.setInt(2, idCaracteristica);
            consulta.executeUpdate();
            consulta.close();

            // 2. Recetas de sus opciones: Inactivas
            ApoyoMySQL.ejecutar(conexion, "UPDATE Receta SET EstadoReceta = 'Inactiva' "
                    + "WHERE IdOpcion IN (" + OPCIONES_DE_CARACTERISTICA + ")", idCaracteristica);
            // 3. Sus opciones y ella misma: no disponibles
            ApoyoMySQL.ejecutar(conexion,
                    "UPDATE OpcionCaracteristica SET Disponible = 0 WHERE IdCaracteristica = ?", idCaracteristica);
            ApoyoMySQL.ejecutar(conexion,
                    "UPDATE Caracteristica SET Disponible = 0 WHERE IdCaracteristica = ?", idCaracteristica);

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al inhabilitar la característica " + idCaracteristica, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void eliminar(int idCaracteristica) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(
                    "DELETE FROM ExclusionOpciones WHERE IdOpcion IN (" + OPCIONES_DE_CARACTERISTICA + ") "
                    + "OR IdOpcionExcluida IN (" + OPCIONES_DE_CARACTERISTICA + ")");
            consulta.setInt(1, idCaracteristica);
            consulta.setInt(2, idCaracteristica);
            consulta.executeUpdate();
            consulta.close();

            ApoyoMySQL.ejecutar(conexion, "DELETE FROM OpcionCaracteristica WHERE IdCaracteristica = ?", idCaracteristica);
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM Caracteristica WHERE IdCaracteristica = ?", idCaracteristica);

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al eliminar la característica " + idCaracteristica, e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Mueve una posición hacia abajo las características con orden >= al indicado
    private void recorrer(Connection conexion, int idCategoria, int desdeOrden, int idExcluir) throws SQLException {
        // LEAST evita pasar de 127, el máximo de la columna TINYINT
        PreparedStatement consulta = conexion.prepareStatement(
                "UPDATE Caracteristica SET OrdenMostrar = LEAST(OrdenMostrar + 1, 127) "
                + "WHERE IdCategoria = ? AND OrdenMostrar >= ? AND IdCaracteristica <> ?");
        consulta.setInt(1, idCategoria);
        consulta.setInt(2, desdeOrden);
        consulta.setInt(3, idExcluir);
        consulta.executeUpdate();
        consulta.close();
    }

    // Llena los 8 datos de la característica a partir de la posición indicada
    private void llenarDatos(PreparedStatement consulta, Caracteristica c, int inicio) throws SQLException {
        consulta.setString(inicio, c.getNombre());
        consulta.setString(inicio + 1, ApoyoMySQL.textoONulo(c.getDescripcion()));
        consulta.setBoolean(inicio + 2, c.isObligatoria());
        consulta.setBoolean(inicio + 3, c.isSeleccionMultiple());
        consulta.setDouble(inicio + 4, c.getPresupuestoSeleccion());
        consulta.setInt(inicio + 5, c.getOrdenMostrar());
        consulta.setBoolean(inicio + 6, c.isDisponible());
        consulta.setBoolean(inicio + 7, c.isPermiteReceta());
    }

    // Ejecuta una consulta que devuelve a lo más una característica
    private Caracteristica leerUna(PreparedStatement consulta) throws SQLException {
        ResultSet resultado = consulta.executeQuery();
        Caracteristica caracteristica = null;
        if (resultado.next()) {
            caracteristica = leerCaracteristica(resultado);
        }
        resultado.close();
        return caracteristica;
    }

    // Convierte la fila actual del ResultSet en un objeto Caracteristica
    private Caracteristica leerCaracteristica(ResultSet resultado) throws SQLException {
        Caracteristica c = new Caracteristica();
        c.setIdCaracteristica(resultado.getInt("IdCaracteristica"));
        c.setIdCategoria(resultado.getInt("IdCategoria"));
        c.setNombre(resultado.getString("NombreCaracteristica"));
        c.setDescripcion(resultado.getString("DescripcionCaracteristica"));
        c.setObligatoria(resultado.getBoolean("Obligatoria"));
        c.setSeleccionMultiple(resultado.getBoolean("SeleccionMultiple"));
        c.setPresupuestoSeleccion(resultado.getDouble("PresupuestoSeleccion"));
        c.setOrdenMostrar(resultado.getInt("OrdenMostrar"));
        c.setDisponible(resultado.getBoolean("Disponible"));
        c.setPermiteReceta(resultado.getBoolean("PermiteReceta"));
        c.setNumeroOpciones(resultado.getInt("NumeroOpciones"));
        return c;
    }
}
