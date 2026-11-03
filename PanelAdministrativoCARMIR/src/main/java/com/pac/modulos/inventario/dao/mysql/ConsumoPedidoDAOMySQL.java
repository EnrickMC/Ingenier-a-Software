package com.pac.modulos.inventario.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.inventario.dao.ConsumoPedidoDAO;
import com.pac.modulos.inventario.model.OpcionPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ConsumoPedidoDAO para MySQL.
 * Lee el pedido, sus postres (DetallePedido) y las opciones de cada postre (PostreOpcion).
 */
public class ConsumoPedidoDAOMySQL implements ConsumoPedidoDAO {

    @Override
    public String buscarEstado(Connection conexion, int folio) throws DAOException {
        try {
            // FOR UPDATE: nadie más cambia el pedido mientras se descuentan sus insumos
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT EstadoPedido FROM Pedido WHERE Folio = ? FOR UPDATE");
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            String estado = null;
            if (resultado.next()) {
                estado = resultado.getString(1);
            }
            resultado.close();
            consulta.close();
            return estado;
        } catch (SQLException e) {
            throw new DAOException("Error al consultar el pedido " + folio, e);
        }
    }

    @Override
    public boolean estaCancelado(Connection conexion, int folio) throws DAOException {
        try {
            return ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM Cancelacion WHERE Folio = ?", folio) > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar la cancelación del pedido " + folio, e);
        }
    }

    @Override
    public List<OpcionPedido> listarOpciones(Connection conexion, int folio) throws DAOException {
        String sql = "SELECT d.IdPostre, d.Cantidad, po.IdOpcion, po.CantidadElegida, o.NombreOpcion, o.SeEscala, "
                + "o.MultiplicadorIngredientes, r.IdReceta "
                + "FROM DetallePedido d "
                + "JOIN PostreOpcion po ON po.IdPostre = d.IdPostre "
                + "JOIN OpcionCaracteristica o ON o.IdOpcion = po.IdOpcion "
                + "LEFT JOIN Receta r ON r.IdOpcion = po.IdOpcion AND r.EstadoReceta = 'Activa' "
                + "WHERE d.Folio = ? ORDER BY d.IdPostre, po.IdOpcion";
        List<OpcionPedido> lista = new ArrayList<OpcionPedido>();
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                OpcionPedido opcion = new OpcionPedido();
                opcion.setIdPostre(resultado.getInt("IdPostre"));
                opcion.setCantidadPostres(resultado.getInt("Cantidad"));
                opcion.setIdOpcion(resultado.getInt("IdOpcion"));
                opcion.setCantidadElegida(resultado.getInt("CantidadElegida"));
                opcion.setNombreOpcion(resultado.getString("NombreOpcion"));
                opcion.setSeEscala(resultado.getBoolean("SeEscala"));
                opcion.setMultiplicadorIngredientes(resultado.getDouble("MultiplicadorIngredientes"));
                opcion.setIdRecetaActiva(resultado.getInt("IdReceta"));    // NULL se lee como 0
                lista.add(opcion);
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las opciones del pedido " + folio, e);
        }
        return lista;
    }
}
