package com.pac.modulos.pedidos.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.pedidos.model.Cliente;
import com.pac.modulos.pedidos.model.Direccion;

import java.sql.Connection;

/**
 * Operaciones sobre Cliente, Direccion y EntregaDomicilio que necesita la modificación de pedidos.
 */
public interface ClienteDAO {

    // true si otro cliente ya tiene ese teléfono (TelefonoWhatsApp es único)
    boolean telefonoEnUso(String telefono, int idClienteExcluido) throws DAOException;

    void actualizar(Connection conexion, Cliente cliente) throws DAOException;

    // Guarda una dirección nueva del cliente y devuelve su id
    int insertarDireccion(Connection conexion, Direccion direccion) throws DAOException;

    // Pone (o cambia) la dirección de entrega del pedido
    void asignarDireccion(Connection conexion, int folio, int idDireccion) throws DAOException;

    // Quita la dirección de entrega (el pedido pasa a recoger en tienda)
    void quitarDireccion(Connection conexion, int folio) throws DAOException;
}
