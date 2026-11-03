package com.pac.modulos.usuarios.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.usuarios.dao.PermisoDAO;
import com.pac.modulos.usuarios.dao.UsuarioDAO;
import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de UsuarioDAO para MySQL.
 * Nota: la columna `Contraseña` lleva ñ, por eso va entre comillas invertidas.
 *
 * Las operaciones que tocan dos tablas (Usuario y UsuarioPermiso) se hacen en una
 * transacción: si algo falla, se deshace todo y la base queda como estaba.
 */
public class UsuarioDAOMySQL implements UsuarioDAO {

    private static final String SELECT_USUARIO =
            "SELECT IdUsuario, NombreCompleto, NombreUsuario, `Contraseña`, EstadoUsuario, "
            + "UltimoAcceso, IntentosFallidos FROM Usuario";

    private PermisoDAO permisoDAO;

    public UsuarioDAOMySQL(PermisoDAO permisoDAO) {
        this.permisoDAO = permisoDAO;
    }

    // ===================== Consultas =====================

    @Override
    public Usuario buscarPorNombreUsuario(String nombreUsuario) throws DAOException {
        String sql = SELECT_USUARIO + " WHERE NombreUsuario = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, nombreUsuario);
            Usuario usuario = leerUnUsuario(consulta);
            consulta.close();
            return usuario;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar el usuario " + nombreUsuario, e);
        }
    }

    @Override
    public Usuario buscarPorId(int idUsuario) throws DAOException {
        String sql = SELECT_USUARIO + " WHERE IdUsuario = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idUsuario);
            Usuario usuario = leerUnUsuario(consulta);
            consulta.close();
            return usuario;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar el usuario con id " + idUsuario, e);
        }
    }

    @Override
    public List<Usuario> buscar(String texto, EstadoUsuario estado) throws DAOException {
        String sql = SELECT_USUARIO + " WHERE (NombreCompleto LIKE ? OR NombreUsuario LIKE ?)";
        if (estado != null) {
            sql = sql + " AND EstadoUsuario = ?";
        }
        sql = sql + " ORDER BY NombreCompleto";

        // El % antes y después busca el texto en cualquier parte del nombre
        String patron = "%";
        if (texto != null) {
            patron = "%" + texto.trim() + "%";
        }

        List<Usuario> usuarios = new ArrayList<Usuario>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, patron);
            consulta.setString(2, patron);
            if (estado != null) {
                consulta.setString(3, estado.getValorBD());
            }
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                usuarios.add(leerUsuario(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar la lista de usuarios", e);
        }

        // A cada usuario se le cargan sus permisos
        for (Usuario usuario : usuarios) {
            cargarPermisos(usuario);
        }
        return usuarios;
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Usuario usuario) throws DAOException {
        String sql = "INSERT INTO Usuario (NombreCompleto, NombreUsuario, `Contraseña`, EstadoUsuario, IntentosFallidos) "
                + "VALUES (?, ?, ?, ?, ?)";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. Se guarda el usuario y se lee el id que generó MySQL
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setString(1, usuario.getNombreCompleto());
            consulta.setString(2, usuario.getNombreUsuario());
            consulta.setString(3, usuario.getContrasenaHash());
            consulta.setString(4, usuario.getEstado().getValorBD());
            consulta.setInt(5, usuario.getIntentosFallidos());
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();

            // 2. Se guardan sus permisos
            guardarPermisos(conexion, idNuevo, usuario.getPermisos());

            conexion.commit();
            usuario.setIdUsuario(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            deshacer(conexion);
            throw new DAOException("Error al registrar el usuario " + usuario.getNombreUsuario(), e);
        } finally {
            regresarAutoCommit(conexion);
        }
    }

    @Override
    public void actualizar(Usuario usuario, boolean cambiarContrasena) throws DAOException {
        String sql = "UPDATE Usuario SET NombreCompleto = ?, NombreUsuario = ?, EstadoUsuario = ?, IntentosFallidos = ?";
        if (cambiarContrasena) {
            sql = sql + ", `Contraseña` = ?";
        }
        sql = sql + " WHERE IdUsuario = ?";

        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. Se actualizan los datos del usuario
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, usuario.getNombreCompleto());
            consulta.setString(2, usuario.getNombreUsuario());
            consulta.setString(3, usuario.getEstado().getValorBD());
            consulta.setInt(4, usuario.getIntentosFallidos());
            if (cambiarContrasena) {
                consulta.setString(5, usuario.getContrasenaHash());
                consulta.setInt(6, usuario.getIdUsuario());
            } else {
                consulta.setInt(5, usuario.getIdUsuario());
            }
            consulta.executeUpdate();
            consulta.close();

            // 2. Se reemplazan sus permisos
            guardarPermisos(conexion, usuario.getIdUsuario(), usuario.getPermisos());

            conexion.commit();
        } catch (SQLException e) {
            deshacer(conexion);
            throw new DAOException("Error al actualizar el usuario " + usuario.getNombreUsuario(), e);
        } finally {
            regresarAutoCommit(conexion);
        }
    }

    @Override
    public void darDeBaja(int idUsuario) throws DAOException {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. El usuario pasa a Inactivo (su historial de pedidos y movimientos se conserva)
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Usuario SET EstadoUsuario = 'Inactivo' WHERE IdUsuario = ?");
            consulta.setInt(1, idUsuario);
            consulta.executeUpdate();
            consulta.close();

            // 2. Se le retiran todos sus permisos
            borrarPermisos(conexion, idUsuario);

            conexion.commit();
        } catch (SQLException e) {
            deshacer(conexion);
            throw new DAOException("Error al dar de baja el usuario " + idUsuario, e);
        } finally {
            regresarAutoCommit(conexion);
        }
    }

    @Override
    public void cambiarEstado(int idUsuario, EstadoUsuario estado) throws DAOException {
        String sql = "UPDATE Usuario SET EstadoUsuario = ? WHERE IdUsuario = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, estado.getValorBD());
            consulta.setInt(2, idUsuario);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al cambiar el estado del usuario " + idUsuario, e);
        }
    }

    @Override
    public void registrarAccesoExitoso(int idUsuario) throws DAOException {
        String sql = "UPDATE Usuario SET UltimoAcceso = NOW(), IntentosFallidos = 0 WHERE IdUsuario = ?";
        ejecutarActualizacion(sql, idUsuario);
    }

    @Override
    public void registrarIntentoFallido(int idUsuario) throws DAOException {
        // LEAST evita pasar de 127, que es el máximo de la columna TINYINT
        String sql = "UPDATE Usuario SET IntentosFallidos = LEAST(IntentosFallidos + 1, 127) WHERE IdUsuario = ?";
        ejecutarActualizacion(sql, idUsuario);
    }

    // ===================== Métodos de apoyo =====================

    // Borra los permisos actuales del usuario y guarda los de la lista
    private void guardarPermisos(Connection conexion, int idUsuario, List<Permiso> permisos) throws SQLException {
        borrarPermisos(conexion, idUsuario);

        PreparedStatement consulta = conexion.prepareStatement(
                "INSERT INTO UsuarioPermiso (IdUsuario, IdPermiso) VALUES (?, ?)");
        for (Permiso permiso : permisos) {
            consulta.setInt(1, idUsuario);
            consulta.setInt(2, permiso.getIdPermiso());
            consulta.executeUpdate();
        }
        consulta.close();
    }

    // Quita todos los permisos de un usuario
    private void borrarPermisos(Connection conexion, int idUsuario) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement("DELETE FROM UsuarioPermiso WHERE IdUsuario = ?");
        consulta.setInt(1, idUsuario);
        consulta.executeUpdate();
        consulta.close();
    }

    // Deshace la transacción si algo falló
    private void deshacer(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.rollback();
        } catch (SQLException e) {
            System.out.println("No se pudo deshacer la transacción: " + e.getMessage());
        }
    }

    // Deja la conexión como estaba (cada consulta se guarda sola)
    private void regresarAutoCommit(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println("No se pudo restaurar el autocommit: " + e.getMessage());
        }
    }

    // Ejecuta un UPDATE que solo recibe el id del usuario
    private void ejecutarActualizacion(String sql, int idUsuario) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idUsuario);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar el usuario " + idUsuario, e);
        }
    }

    // Ejecuta una consulta que devuelve a lo más un usuario y le carga sus permisos
    private Usuario leerUnUsuario(PreparedStatement consulta) throws SQLException, DAOException {
        ResultSet resultado = consulta.executeQuery();
        Usuario usuario = null;
        if (resultado.next()) {
            usuario = leerUsuario(resultado);
        }
        resultado.close();

        if (usuario != null) {
            cargarPermisos(usuario);
        }
        return usuario;
    }

    // Pide a PermisoDAO los permisos del usuario y se los asigna
    private void cargarPermisos(Usuario usuario) throws DAOException {
        List<Permiso> permisos = permisoDAO.listarPorUsuario(usuario.getIdUsuario());
        usuario.reemplazarPermisos(permisos);
    }

    // Convierte la fila actual del ResultSet en un objeto Usuario
    private Usuario leerUsuario(ResultSet resultado) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(resultado.getInt("IdUsuario"));
        usuario.setNombreCompleto(resultado.getString("NombreCompleto"));
        usuario.setNombreUsuario(resultado.getString("NombreUsuario"));
        usuario.setContrasenaHash(resultado.getString("Contraseña"));
        usuario.setEstado(EstadoUsuario.desdeBD(resultado.getString("EstadoUsuario")));
        usuario.setIntentosFallidos(resultado.getInt("IntentosFallidos"));

        Timestamp ultimoAcceso = resultado.getTimestamp("UltimoAcceso");
        if (ultimoAcceso != null) {
            usuario.setUltimoAcceso(ultimoAcceso.toLocalDateTime());
        }
        return usuario;
    }
}
