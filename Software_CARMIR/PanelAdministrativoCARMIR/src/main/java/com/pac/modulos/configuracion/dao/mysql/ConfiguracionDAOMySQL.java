package com.pac.modulos.configuracion.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.configuracion.dao.ConfiguracionDAO;
import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.configuracion.model.Parametro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ConfiguracionDAO para MySQL.
 * Un IdUsuario vacío (NULL) en la bitácora significa que el cambio lo hizo la Administradora.
 */
public class ConfiguracionDAOMySQL implements ConfiguracionDAO {

    @Override
    public List<Parametro> listar() throws DAOException {
        List<Parametro> lista = new ArrayList<Parametro>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT Clave, Valor, Descripcion FROM Configuracion ORDER BY Clave");
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(new Parametro(resultado.getString("Clave"), resultado.getString("Valor"),
                        resultado.getString("Descripcion")));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw convertir("Error al leer la configuración", e);
        }
        return lista;
    }

    @Override
    public void guardar(Connection conexion, Parametro parametro) throws DAOException {
        // Si la clave ya existe solo se cambia el valor; si no, se crea
        String sql = "INSERT INTO Configuracion (Clave, Valor, Descripcion) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE Valor = VALUES(Valor)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, parametro.getClave());
            consulta.setString(2, parametro.getValor());
            consulta.setString(3, parametro.getDescripcion());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw convertir("Error al guardar el parámetro " + parametro.getClave(), e);
        }
    }

    @Override
    public void registrarCambio(Connection conexion, CambioConfiguracion cambio) throws DAOException {
        String sql = "INSERT INTO CambioConfiguracion (Clave, ValorAnterior, ValorNuevo, IdUsuario) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, cambio.getClave());
            consulta.setString(2, cambio.getValorAnterior());
            consulta.setString(3, cambio.getValorNuevo());
            if (cambio.getIdUsuario() > 0) {
                consulta.setInt(4, cambio.getIdUsuario());
            } else {
                consulta.setNull(4, Types.INTEGER);
            }
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw convertir("Error al registrar el cambio de configuración", e);
        }
    }

    @Override
    public List<CambioConfiguracion> listarCambios(int cantidad) throws DAOException {
        List<CambioConfiguracion> lista = new ArrayList<CambioConfiguracion>();
        String sql = "SELECT c.IdCambio, c.Clave, c.ValorAnterior, c.ValorNuevo, c.IdUsuario, c.FechaHoraCambio, "
                + "u.NombreUsuario FROM CambioConfiguracion c LEFT JOIN Usuario u ON u.IdUsuario = c.IdUsuario "
                + "ORDER BY c.FechaHoraCambio DESC, c.IdCambio DESC LIMIT ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, cantidad);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                CambioConfiguracion cambio = new CambioConfiguracion(resultado.getString("Clave"),
                        resultado.getString("ValorAnterior"), resultado.getString("ValorNuevo"),
                        resultado.getInt("IdUsuario"));
                cambio.setIdCambio(resultado.getInt("IdCambio"));
                Timestamp fecha = resultado.getTimestamp("FechaHoraCambio");
                if (fecha != null) {
                    cambio.setFecha(fecha.toLocalDateTime());
                }
                // Sin usuario: lo cambió la Administradora
                String usuario = resultado.getString("NombreUsuario");
                if (usuario == null) {
                    usuario = "Administración";
                }
                cambio.setNombreUsuario(usuario);
                lista.add(cambio);
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw convertir("Error al leer el historial de cambios", e);
        }
        return lista;
    }

    // Si la tabla no existe se explica qué script falta ejecutar
    static DAOException convertir(String mensaje, SQLException causa) {
        String detalle = String.valueOf(causa.getMessage());
        if (detalle.contains("doesn't exist")) {
            return new DAOException(MSG_FALTA_MIGRACION, causa);
        }
        return new DAOException(mensaje, causa);
    }
}
