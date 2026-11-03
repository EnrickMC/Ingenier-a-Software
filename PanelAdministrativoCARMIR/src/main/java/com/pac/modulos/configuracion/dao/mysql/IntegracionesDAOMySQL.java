package com.pac.modulos.configuracion.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.configuracion.dao.IntegracionesDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Implementación de IntegracionesDAO para MySQL (tabla ConfiguracionNotificacion).
 */
public class IntegracionesDAOMySQL implements IntegracionesDAO {

    @Override
    public Properties listar() throws DAOException {
        Properties valores = new Properties();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT Clave, Valor FROM ConfiguracionNotificacion");
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                String valor = resultado.getString("Valor");
                if (valor == null) {
                    valor = "";
                }
                valores.setProperty(resultado.getString("Clave"), valor);
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw convertir("Error al leer la configuración de notificaciones", e);
        }
        return valores;
    }

    @Override
    public void guardar(Connection conexion, String clave, String valor, boolean esSecreto) throws DAOException {
        // Si la clave ya existe solo se cambia el valor; si no, se crea
        String sql = "INSERT INTO ConfiguracionNotificacion (Clave, Valor, EsSecreto) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE Valor = VALUES(Valor), EsSecreto = VALUES(EsSecreto)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, clave);
            consulta.setString(2, valor);
            consulta.setBoolean(3, esSecreto);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            // El mensaje no incluye el valor para no mostrar secretos
            throw convertir("Error al guardar el dato " + clave, e);
        }
    }

    // Si la tabla no existe se explica qué script falta ejecutar
    private DAOException convertir(String mensaje, SQLException causa) {
        String detalle = String.valueOf(causa.getMessage());
        if (detalle.contains("doesn't exist")) {
            return new DAOException(MSG_FALTA_MIGRACION, causa);
        }
        return new DAOException(mensaje, causa);
    }
}
