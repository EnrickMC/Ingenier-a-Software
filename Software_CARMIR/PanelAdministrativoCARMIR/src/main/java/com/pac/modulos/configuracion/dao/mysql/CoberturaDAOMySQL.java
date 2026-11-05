package com.pac.modulos.configuracion.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.configuracion.dao.CoberturaDAO;
import com.pac.modulos.configuracion.model.CodigoPostalCobertura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de CoberturaDAO para MySQL (tabla CodigoPostalCobertura).
 */
public class CoberturaDAOMySQL implements CoberturaDAO {

    @Override
    public List<CodigoPostalCobertura> buscar(String inicioCodigo) throws DAOException {
        List<CodigoPostalCobertura> lista = new ArrayList<CodigoPostalCobertura>();
        String inicio = "";
        if (inicioCodigo != null) {
            inicio = inicioCodigo.trim();
        }
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT CodigoPostal, CostoEnvio FROM CodigoPostalCobertura WHERE CodigoPostal LIKE ? ORDER BY CodigoPostal");
            consulta.setString(1, inicio + "%");
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                lista.add(new CodigoPostalCobertura(resultado.getString("CodigoPostal"), resultado.getDouble("CostoEnvio")));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw ConfiguracionDAOMySQL.convertir("Error al leer la cobertura de envío", e);
        }
        return lista;
    }

    @Override
    public CodigoPostalCobertura buscarPorCodigo(String codigoPostal) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT CodigoPostal, CostoEnvio FROM CodigoPostalCobertura WHERE CodigoPostal = ?");
            consulta.setString(1, codigoPostal);
            ResultSet resultado = consulta.executeQuery();
            CodigoPostalCobertura encontrado = null;
            if (resultado.next()) {
                encontrado = new CodigoPostalCobertura(resultado.getString("CodigoPostal"), resultado.getDouble("CostoEnvio"));
            }
            resultado.close();
            consulta.close();
            return encontrado;
        } catch (SQLException e) {
            throw ConfiguracionDAOMySQL.convertir("Error al buscar el código postal", e);
        }
    }

    @Override
    public void insertar(Connection conexion, CodigoPostalCobertura cobertura) throws DAOException {
        ejecutar(conexion, "INSERT INTO CodigoPostalCobertura (CostoEnvio, CodigoPostal) VALUES (?, ?)", cobertura,
                "Error al agregar el código postal");
    }

    @Override
    public void actualizar(Connection conexion, CodigoPostalCobertura cobertura) throws DAOException {
        ejecutar(conexion, "UPDATE CodigoPostalCobertura SET CostoEnvio = ? WHERE CodigoPostal = ?", cobertura,
                "Error al cambiar el costo de envío");
    }

    @Override
    public void eliminar(Connection conexion, String codigoPostal) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement("DELETE FROM CodigoPostalCobertura WHERE CodigoPostal = ?");
            consulta.setString(1, codigoPostal);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw ConfiguracionDAOMySQL.convertir("Error al quitar el código postal", e);
        }
    }

    // INSERT y UPDATE usan los mismos dos parámetros: costo y código postal
    private void ejecutar(Connection conexion, String sql, CodigoPostalCobertura cobertura, String mensaje)
            throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setDouble(1, cobertura.getCostoEnvio());
            consulta.setString(2, cobertura.getCodigoPostal());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw ConfiguracionDAOMySQL.convertir(mensaje, e);
        }
    }
}
