package com.pac.modulos.catalogo.dao.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Métodos de apoyo que comparten los DAO del catálogo.
 */
public class ApoyoMySQL {

    // Un pedido está vigente si no se ha entregado y no tiene registro de cancelación.
    // Se usa con el alias "p" para la tabla Pedido.
    public static final String CONDICION_PEDIDO_VIGENTE =
            "p.EstadoPedido <> 'Entregado' AND NOT EXISTS (SELECT 1 FROM Cancelacion c WHERE c.Folio = p.Folio)";

    // Clase de apoyo: no se crean objetos de ella
    private ApoyoMySQL() {
    }

    // Ejecuta un INSERT, UPDATE o DELETE que solo recibe un id
    public static void ejecutar(Connection conexion, String sql, int id) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(sql);
        consulta.setInt(1, id);
        consulta.executeUpdate();
        consulta.close();
    }

    // Ejecuta una consulta que recibe un id y devuelve un número (por ejemplo COUNT)
    public static int contar(Connection conexion, String sql, int id) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(sql);
        consulta.setInt(1, id);
        ResultSet resultado = consulta.executeQuery();
        int numero = 0;
        if (resultado.next()) {
            numero = resultado.getInt(1);
        }
        resultado.close();
        consulta.close();
        return numero;
    }

    // Ejecuta una consulta que recibe un id y devuelve una lista de números (por ejemplo folios)
    public static List<Integer> leerEnteros(Connection conexion, String sql, int id) throws SQLException {
        List<Integer> lista = new ArrayList<Integer>();
        PreparedStatement consulta = conexion.prepareStatement(sql);
        consulta.setInt(1, id);
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            lista.add(resultado.getInt(1));
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    // Convierte un texto vacío en null para guardarlo como NULL en la base de datos
    public static String textoONulo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    // Arma el patrón de búsqueda "%texto%"
    public static String patron(String texto) {
        if (texto == null) {
            return "%";
        }
        return "%" + texto.trim() + "%";
    }

    // Deshace la transacción si algo falló
    public static void deshacer(Connection conexion) {
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
    public static void regresarAutoCommit(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.setAutoCommit(true);
        } catch (SQLException e) {
            System.out.println("No se pudo restaurar el autocommit: " + e.getMessage());
        }
    }
}
