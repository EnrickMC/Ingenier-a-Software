package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.Pedido;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pasos que repiten todas las acciones sobre un pedido:
 * abrir la transacción, bloquear el pedido, revisar que nadie lo haya cambiado y cerrar.
 */
public class ApoyoTransaccion {

    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_ENCONTRADO = "Pedido no encontrado";
    public static final String MSG_ACTUALIZADO_POR_OTRO = "El pedido fue actualizado por otro usuario";

    // Clase de apoyo: no se crean objetos de ella
    private ApoyoTransaccion() {
    }

    // Abre la transacción sobre la conexión compartida
    public static Connection abrir() throws SQLException {
        Connection conexion = ConexionBD.getInstancia().getConexion();
        conexion.setAutoCommit(false);
        return conexion;
    }

    // Deshace lo hecho (si algo falla) y regresa la conexión a modo normal
    public static void deshacer(Connection conexion) {
        ApoyoMySQL.deshacer(conexion);
    }

    public static void cerrar(Connection conexion) {
        ApoyoMySQL.regresarAutoCommit(conexion);
    }

    /**
     * Bloquea el pedido y revisa que siga igual que cuando se abrió en pantalla.
     * Devuelve null si todo está bien, o el resultado de error para mostrar.
     */
    public static ResultadoPedido bloquearYRevisar(PedidoDAO pedidoDAO, Connection conexion, Pedido abierto)
            throws DAOException {
        String version = pedidoDAO.bloquear(conexion, abierto.getFolio());
        if (version == null) {
            return ResultadoPedido.recargar(MSG_NO_ENCONTRADO);
        }
        if (abierto.getVersion() != null && !abierto.getVersion().equals(version)) {
            return ResultadoPedido.recargar(MSG_ACTUALIZADO_POR_OTRO);
        }
        return null;
    }

    // Mensaje para un error dentro de la transacción: el general y, si aplica, que falta la migración V4
    public static String mensajeFallo(String general, DAOException error) {
        if (BitacoraPedidoDAO.MSG_FALTA_MIGRACION.equals(error.getMessage())) {
            return general + ".\n" + BitacoraPedidoDAO.MSG_FALTA_MIGRACION;
        }
        System.out.println(general + ": " + error.getMessage());
        return general;
    }
}
