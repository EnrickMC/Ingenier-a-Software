package com.pac.modulos.usuarios.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.usuarios.dao.PermisoDAO;
import com.pac.modulos.usuarios.model.Permiso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de PermisoDAO para MySQL.
 */
public class PermisoDAOMySQL implements PermisoDAO {

    @Override
    public List<Permiso> listarPorUsuario(int idUsuario) throws DAOException {
        String sql = "SELECT p.IdPermiso, p.NombrePermiso FROM Permiso p "
                + "JOIN UsuarioPermiso up ON up.IdPermiso = p.IdPermiso "
                + "WHERE up.IdUsuario = ? ORDER BY p.IdPermiso";
        List<Permiso> permisos = new ArrayList<Permiso>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idUsuario);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                permisos.add(leerPermiso(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al leer los permisos del usuario " + idUsuario, e);
        }
        return permisos;
    }

    @Override
    public List<Permiso> listarTodos() throws DAOException {
        String sql = "SELECT IdPermiso, NombrePermiso FROM Permiso ORDER BY IdPermiso";
        List<Permiso> permisos = new ArrayList<Permiso>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                permisos.add(leerPermiso(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al leer la lista de permisos", e);
        }
        return permisos;
    }

    @Override
    public void asegurarPermisos(String[] nombresPermisos) throws DAOException {
        // INSERT IGNORE no marca error si el permiso ya existe (NombrePermiso es UNIQUE)
        String sql = "INSERT IGNORE INTO Permiso (NombrePermiso) VALUES (?)";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            for (String nombre : nombresPermisos) {
                consulta.setString(1, nombre);
                consulta.executeUpdate();
            }
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al dar de alta los permisos", e);
        }
    }

    // Convierte la fila actual del ResultSet en un objeto Permiso
    private Permiso leerPermiso(ResultSet resultado) throws SQLException {
        return new Permiso(resultado.getInt("IdPermiso"), resultado.getString("NombrePermiso"));
    }
}
