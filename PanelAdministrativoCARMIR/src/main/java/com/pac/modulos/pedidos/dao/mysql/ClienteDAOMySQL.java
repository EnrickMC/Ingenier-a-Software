package com.pac.modulos.pedidos.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.pedidos.dao.ClienteDAO;
import com.pac.modulos.pedidos.model.Cliente;
import com.pac.modulos.pedidos.model.Direccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementación de ClienteDAO para MySQL (tablas Cliente, Direccion y EntregaDomicilio).
 */
public class ClienteDAOMySQL implements ClienteDAO {

    @Override
    public boolean telefonoEnUso(String telefono, int idClienteExcluido) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM Cliente WHERE TelefonoWhatsApp = ? AND IdCliente <> ?");
            consulta.setString(1, telefono);
            consulta.setInt(2, idClienteExcluido);
            ResultSet resultado = consulta.executeQuery();
            int numero = 0;
            if (resultado.next()) {
                numero = resultado.getInt(1);
            }
            resultado.close();
            consulta.close();
            return numero > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar el teléfono del cliente", e);
        }
    }

    @Override
    public void actualizar(Connection conexion, Cliente cliente) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Cliente SET NombreCliente = ?, TelefonoWhatsApp = ?, CorreoCliente = ? WHERE IdCliente = ?");
            consulta.setString(1, cliente.getNombre().trim());
            consulta.setString(2, cliente.getTelefono().trim());
            ApoyoPedidosMySQL.ponerTexto(consulta, 3, cliente.getCorreo());
            consulta.setInt(4, cliente.getIdCliente());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al actualizar los datos del cliente", e);
        }
    }

    @Override
    public int insertarDireccion(Connection conexion, Direccion direccion) throws DAOException {
        String sql = "INSERT INTO Direccion (IdCliente, Calle, NumeroExterior, NumeroInterior, Colonia, CodigoPostal, "
                + "Referencias) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, direccion.getIdCliente());
            consulta.setString(2, direccion.getCalle().trim());
            consulta.setString(3, direccion.getNumeroExterior().trim());
            ApoyoPedidosMySQL.ponerTexto(consulta, 4, direccion.getNumeroInterior());
            consulta.setString(5, direccion.getColonia().trim());
            consulta.setString(6, direccion.getCodigoPostal().trim());
            ApoyoPedidosMySQL.ponerTexto(consulta, 7, direccion.getReferencias());
            consulta.executeUpdate();
            ResultSet llaves = consulta.getGeneratedKeys();
            int idNuevo = 0;
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();
            return idNuevo;
        } catch (SQLException e) {
            throw new DAOException("Error al guardar la dirección", e);
        }
    }

    @Override
    public void asignarDireccion(Connection conexion, int folio, int idDireccion) throws DAOException {
        try {
            // Si ya tenía dirección se cambia; si no, se agrega
            PreparedStatement consulta = conexion.prepareStatement(
                    "INSERT INTO EntregaDomicilio (Folio, IdDireccion) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE IdDireccion = VALUES(IdDireccion)");
            consulta.setInt(1, folio);
            consulta.setInt(2, idDireccion);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al asignar la dirección de entrega", e);
        }
    }

    @Override
    public void quitarDireccion(Connection conexion, int folio) throws DAOException {
        try {
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM EntregaDomicilio WHERE Folio = ?", folio);
        } catch (SQLException e) {
            throw new DAOException("Error al quitar la dirección de entrega", e);
        }
    }
}
