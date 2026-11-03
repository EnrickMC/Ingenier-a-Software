package com.pac.modulos.catalogo.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.ExclusionDAO;
import com.pac.modulos.catalogo.model.EstadoRegistro;
import com.pac.modulos.catalogo.model.Exclusion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ExclusionDAO para MySQL (tabla ExclusionOpciones).
 */
public class ExclusionDAOMySQL implements ExclusionDAO {

    // Se une dos veces con OpcionCaracteristica y Caracteristica: una por cada lado del par
    private static final String SELECT_EXCLUSION =
            "SELECT e.IdExclusion, e.IdOpcion, e.IdOpcionExcluida, e.MotivoExclusion, e.EstadoExclusion, "
            + "oa.NombreOpcion AS NombreA, ca.NombreCaracteristica AS CaracteristicaA, "
            + "ob.NombreOpcion AS NombreB, cb.NombreCaracteristica AS CaracteristicaB "
            + "FROM ExclusionOpciones e "
            + "JOIN OpcionCaracteristica oa ON oa.IdOpcion = e.IdOpcion "
            + "JOIN Caracteristica ca ON ca.IdCaracteristica = oa.IdCaracteristica "
            + "JOIN OpcionCaracteristica ob ON ob.IdOpcion = e.IdOpcionExcluida "
            + "JOIN Caracteristica cb ON cb.IdCaracteristica = ob.IdCaracteristica";

    private static final String ORDEN = " ORDER BY e.EstadoExclusion, ca.OrdenMostrar, oa.NombreOpcion";

    // ===================== Consultas =====================

    @Override
    public List<Exclusion> listarPorCategoria(int idCategoria) throws DAOException {
        String sql = SELECT_EXCLUSION + " WHERE ca.IdCategoria = ?" + ORDEN;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idCategoria);
            List<Exclusion> lista = leerLista(consulta);
            consulta.close();
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las exclusiones de la categoría " + idCategoria, e);
        }
    }

    @Override
    public List<Exclusion> listarPorOpcion(int idOpcion) throws DAOException {
        String sql = SELECT_EXCLUSION + " WHERE e.IdOpcion = ? OR e.IdOpcionExcluida = ?" + ORDEN;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idOpcion);
            consulta.setInt(2, idOpcion);
            List<Exclusion> lista = leerLista(consulta);
            consulta.close();
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las exclusiones de la opción " + idOpcion, e);
        }
    }

    @Override
    public Exclusion buscarPorId(int idExclusion) throws DAOException {
        String sql = SELECT_EXCLUSION + " WHERE e.IdExclusion = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idExclusion);
            List<Exclusion> lista = leerLista(consulta);
            consulta.close();
            if (lista.isEmpty()) {
                return null;
            }
            return lista.get(0);
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la exclusión " + idExclusion, e);
        }
    }

    @Override
    public Exclusion buscarPar(int idOpcion, int idOpcionExcluida) throws DAOException {
        // A-B y B-A son la misma regla
        String sql = SELECT_EXCLUSION + " WHERE (e.IdOpcion = ? AND e.IdOpcionExcluida = ?) "
                + "OR (e.IdOpcion = ? AND e.IdOpcionExcluida = ?)";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idOpcion);
            consulta.setInt(2, idOpcionExcluida);
            consulta.setInt(3, idOpcionExcluida);
            consulta.setInt(4, idOpcion);
            List<Exclusion> lista = leerLista(consulta);
            consulta.close();
            if (lista.isEmpty()) {
                return null;
            }
            return lista.get(0);
        } catch (SQLException e) {
            throw new DAOException("Error al buscar la exclusión entre las opciones", e);
        }
    }

    @Override
    public List<Exclusion> listarActivasEntre(List<Integer> idsOpcion) throws DAOException {
        List<Exclusion> lista = new ArrayList<Exclusion>();
        if (idsOpcion.isEmpty()) {
            return lista;
        }

        // Se arma "?, ?, ?" con un signo por cada id
        String signos = "";
        for (int i = 0; i < idsOpcion.size(); i++) {
            if (i > 0) {
                signos = signos + ", ";
            }
            signos = signos + "?";
        }
        String sql = SELECT_EXCLUSION + " WHERE e.EstadoExclusion = 'Activa' "
                + "AND e.IdOpcion IN (" + signos + ") AND e.IdOpcionExcluida IN (" + signos + ")";

        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            int posicion = 1;
            for (int vuelta = 0; vuelta < 2; vuelta++) {
                for (Integer id : idsOpcion) {
                    consulta.setInt(posicion, id);
                    posicion++;
                }
            }
            lista = leerLista(consulta);
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al revisar las exclusiones de las opciones elegidas", e);
        }
        return lista;
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Exclusion exclusion) throws DAOException {
        String sql = "INSERT INTO ExclusionOpciones (IdOpcion, IdOpcionExcluida, MotivoExclusion, EstadoExclusion) "
                + "VALUES (?, ?, ?, ?)";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, exclusion.getIdOpcion());
            consulta.setInt(2, exclusion.getIdOpcionExcluida());
            consulta.setString(3, ApoyoMySQL.textoONulo(exclusion.getMotivo()));
            consulta.setString(4, exclusion.getEstado().getValorBD());
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            exclusion.setIdExclusion(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            throw new DAOException("Error al registrar la exclusión", e);
        }
    }

    @Override
    public void cambiarEstado(int idExclusion, EstadoRegistro estado) throws DAOException {
        String sql = "UPDATE ExclusionOpciones SET EstadoExclusion = ? WHERE IdExclusion = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, estado.getValorBD());
            consulta.setInt(2, idExclusion);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al cambiar el estado de la exclusión " + idExclusion, e);
        }
    }

    @Override
    public void eliminar(int idExclusion) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM ExclusionOpciones WHERE IdExclusion = ?", idExclusion);
        } catch (SQLException e) {
            throw new DAOException("Error al eliminar la exclusión " + idExclusion, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Ejecuta la consulta y convierte todas las filas en objetos Exclusion
    private List<Exclusion> leerLista(PreparedStatement consulta) throws SQLException {
        List<Exclusion> lista = new ArrayList<Exclusion>();
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            Exclusion e = new Exclusion();
            e.setIdExclusion(resultado.getInt("IdExclusion"));
            e.setIdOpcion(resultado.getInt("IdOpcion"));
            e.setIdOpcionExcluida(resultado.getInt("IdOpcionExcluida"));
            e.setMotivo(resultado.getString("MotivoExclusion"));
            e.setEstado(EstadoRegistro.desdeBD(resultado.getString("EstadoExclusion")));
            e.setNombreOpcion(resultado.getString("NombreA"));
            e.setNombreCaracteristica(resultado.getString("CaracteristicaA"));
            e.setNombreOpcionExcluida(resultado.getString("NombreB"));
            e.setNombreCaracteristicaExcluida(resultado.getString("CaracteristicaB"));
            lista.add(e);
        }
        resultado.close();
        return lista;
    }
}
