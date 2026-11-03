package com.pac.modulos.inventario.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.OpcionPedido;

import java.sql.Connection;
import java.util.List;

/**
 * Consultas del pedido que necesita el descuento automático de insumos (RF-45).
 * Todas reciben la conexión porque corren dentro de la transacción del cambio de estado.
 */
public interface ConsumoPedidoDAO {

    // Estado actual del pedido; null si el pedido no existe
    String buscarEstado(Connection conexion, int folio) throws DAOException;

    // true si el pedido tiene registro de cancelación
    boolean estaCancelado(Connection conexion, int folio) throws DAOException;

    // Opciones de cada postre del pedido, con su receta activa (si tiene)
    List<OpcionPedido> listarOpciones(Connection conexion, int folio) throws DAOException;
}
