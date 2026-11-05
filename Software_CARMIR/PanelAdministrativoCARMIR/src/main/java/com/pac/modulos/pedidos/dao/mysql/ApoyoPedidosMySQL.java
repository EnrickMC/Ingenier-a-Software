package com.pac.modulos.pedidos.dao.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Métodos de apoyo que comparten los DAO de pedidos.
 */
public class ApoyoPedidosMySQL {

    // Pedido sin cancelar ni dar de baja (alias "p" para la tabla Pedido)
    public static final String SIN_CANCELAR_NI_BAJA =
            "NOT EXISTS (SELECT 1 FROM Cancelacion cx WHERE cx.Folio = p.Folio) "
            + "AND NOT EXISTS (SELECT 1 FROM BajaPedido bx WHERE bx.Folio = p.Folio)";

    // Clase de apoyo: no se crean objetos de ella
    private ApoyoPedidosMySQL() {
    }

    // Guarda un id de usuario; 0 (Administradora o sistema) se guarda como NULL
    public static void ponerUsuario(PreparedStatement consulta, int posicion, int idUsuario) throws SQLException {
        if (idUsuario <= 0) {
            consulta.setNull(posicion, Types.INTEGER);
        } else {
            consulta.setInt(posicion, idUsuario);
        }
    }

    // Guarda un texto; vacío o null se guarda como NULL
    public static void ponerTexto(PreparedStatement consulta, int posicion, String texto) throws SQLException {
        if (texto == null || texto.isBlank()) {
            consulta.setNull(posicion, Types.VARCHAR);
        } else {
            consulta.setString(posicion, texto.trim());
        }
    }

    // Lee una columna DATETIME; null si está vacía
    public static LocalDateTime leerFechaHora(ResultSet resultado, String columna) throws SQLException {
        Timestamp valor = resultado.getTimestamp(columna);
        if (valor == null) {
            return null;
        }
        return valor.toLocalDateTime();
    }

    // Nombre que se muestra para quien hizo algo: el usuario, o "Administradora" si la columna está vacía
    public static String nombreQuien(String nombreUsuario) {
        if (nombreUsuario == null) {
            return "Administradora";
        }
        return nombreUsuario;
    }

    // true si el error se debe a que falta la migración V4 (tabla, columna o NULL no permitido)
    public static boolean faltaMigracion(SQLException error) {
        String detalle = String.valueOf(error.getMessage());
        return detalle.contains("BajaPedido") || detalle.contains("ModificacionPedido")
                || detalle.contains("MotivoFallo") || detalle.contains("Unknown column")
                || detalle.contains("cannot be null") || detalle.contains("Data truncated for column 'ResultadoEnvio'");
    }
}
