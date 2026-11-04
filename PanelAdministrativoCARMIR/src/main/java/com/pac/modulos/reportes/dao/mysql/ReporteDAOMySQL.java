package com.pac.modulos.reportes.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.reportes.dao.ReporteDAO;
import com.pac.modulos.reportes.model.MovimientoRevisar;
import com.pac.modulos.reportes.model.RenglonDia;
import com.pac.modulos.reportes.model.RenglonVenta;
import com.pac.modulos.reportes.model.ReporteIngresosGastos;
import com.pac.modulos.reportes.model.ReporteVentas;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ReporteDAO para MySQL.
 *
 * Fuentes (según el docx del Módulo 6):
 *  - Ingresos: Pago (solo EstadoValidacion = 'Validado'), por FechaPago.
 *  - Reembolsos: Cancelacion con EstadoReembolso = 'Realizado', por FechaHoraCancelacion.
 *  - Gastos: MovimientoInventario tipo 'Compra' y 'Merma' (CostoMovimiento); 'Produccion' es informativo.
 *  - Ventas: DetallePedido, Postre, PostreOpcion, OpcionCaracteristica, Caracteristica, CategoriaPostre.
 * Los pedidos dados de baja (BajaPedido) nunca cuentan.
 */
public class ReporteDAOMySQL implements ReporteDAO {

    // Máximo de movimientos "por revisar" que se listan
    private static final int MAXIMO_POR_REVISAR = 200;

    // ===================== RF-65 · Ingresos y gastos =====================

    // Pagos validados del periodo, separados en anticipo, liquidación y pago total.
    // Un pago es "Anticipo" si, antes de él, lo pagado del pedido aún no cubría el anticipo requerido.
    // (La suma "OVER" acumula los pagos anteriores del mismo pedido, en orden de fecha.)
    private static final String SQL_COBROS =
            "SELECT DATE(x.FechaPago) AS Dia, x.Tipo, SUM(x.MontoPago) AS Total, COUNT(*) AS Numero "
            + "FROM (SELECT pg.FechaPago, pg.MontoPago, "
            + "        CASE WHEN p.ModalidadPago = 'Pago total' THEN 'Pago total' "
            + "             WHEN COALESCE(SUM(pg.MontoPago) OVER (PARTITION BY pg.Folio ORDER BY pg.FechaPago, pg.IdPago "
            + "                  ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) < p.AnticipoRequerido THEN 'Anticipo' "
            + "             ELSE 'Liquidacion' END AS Tipo "
            + "      FROM Pago pg "
            + "      JOIN Pedido p ON p.Folio = pg.Folio "
            + "      WHERE pg.EstadoValidacion = 'Validado' "
            + "        AND pg.Folio IN (SELECT Folio FROM Pago WHERE EstadoValidacion = 'Validado' "
            + "                         AND FechaPago >= ? AND FechaPago < ?) "
            + "        AND NOT EXISTS (SELECT 1 FROM BajaPedido b WHERE b.Folio = pg.Folio) "
            + "        AND NOT EXISTS (SELECT 1 FROM Cancelacion c WHERE c.Folio = pg.Folio "
            + "                        AND c.EstadoReembolso = 'Pendiente')) x "
            + "WHERE x.FechaPago >= ? AND x.FechaPago < ? "
            + "GROUP BY DATE(x.FechaPago), x.Tipo";

    // Pagos validados de pedidos cancelados cuyo reembolso sigue pendiente (se muestran aparte)
    private static final String SQL_POR_REEMBOLSAR =
            "SELECT COALESCE(SUM(pg.MontoPago), 0) "
            + "FROM Pago pg "
            + "JOIN Cancelacion c ON c.Folio = pg.Folio AND c.EstadoReembolso = 'Pendiente' "
            + "WHERE pg.EstadoValidacion = 'Validado' AND pg.FechaPago >= ? AND pg.FechaPago < ? "
            + "  AND NOT EXISTS (SELECT 1 FROM BajaPedido b WHERE b.Folio = pg.Folio)";

    // Reembolsos ya realizados, por día de la cancelación
    private static final String SQL_REEMBOLSOS =
            "SELECT DATE(c.FechaHoraCancelacion) AS Dia, SUM(c.MontoReembolso) AS Total "
            + "FROM Cancelacion c "
            + "WHERE c.EstadoReembolso = 'Realizado' AND c.MontoReembolso > 0 "
            + "  AND c.FechaHoraCancelacion >= ? AND c.FechaHoraCancelacion < ? "
            + "  AND NOT EXISTS (SELECT 1 FROM BajaPedido b WHERE b.Folio = c.Folio) "
            + "GROUP BY DATE(c.FechaHoraCancelacion)";

    // Compras, mermas y producción del periodo. Solo los movimientos consistentes:
    // compra con cantidad y costo positivos; merma con cantidad negativa y costo no negativo.
    private static final String SQL_MOVIMIENTOS =
            "SELECT DATE(m.FechaMovimiento) AS Dia, m.TipoMovimiento, SUM(m.CostoMovimiento) AS Total, COUNT(*) AS Numero "
            + "FROM MovimientoInventario m "
            + "WHERE m.FechaMovimiento >= ? AND m.FechaMovimiento < ? "
            + "  AND ((m.TipoMovimiento = 'Compra' AND m.CantidadMovimiento > 0 AND m.CostoMovimiento > 0) "
            + "    OR (m.TipoMovimiento = 'Merma' AND m.CantidadMovimiento < 0 AND m.CostoMovimiento >= 0) "
            + "    OR m.TipoMovimiento = 'Produccion') "
            + "GROUP BY DATE(m.FechaMovimiento), m.TipoMovimiento";

    // Movimientos que no son consistentes: se excluyen de los totales y se listan para revisarlos
    private static final String SQL_POR_REVISAR =
            "SELECT pg.FechaPago AS Fecha, 'Pago' AS Origen, CONCAT('Pedido #', pg.Folio) AS Referencia, "
            + "       pg.MontoPago AS Monto, 'Pago validado de un pedido dado de baja' AS Motivo "
            + "FROM Pago pg JOIN BajaPedido b ON b.Folio = pg.Folio "
            + "WHERE pg.EstadoValidacion = 'Validado' AND pg.FechaPago >= ? AND pg.FechaPago < ? "
            + "UNION ALL "
            + "SELECT m.FechaMovimiento, 'Compra', i.NombreInsumo, m.CostoMovimiento, "
            + "       'Compra con cantidad o costo en cero o negativo' "
            + "FROM MovimientoInventario m JOIN Insumo i ON i.IdInsumo = m.IdInsumo "
            + "WHERE m.TipoMovimiento = 'Compra' AND (m.CantidadMovimiento <= 0 OR m.CostoMovimiento <= 0) "
            + "  AND m.FechaMovimiento >= ? AND m.FechaMovimiento < ? "
            + "UNION ALL "
            + "SELECT m.FechaMovimiento, 'Merma', i.NombreInsumo, m.CostoMovimiento, "
            + "       'Merma con cantidad positiva o costo negativo' "
            + "FROM MovimientoInventario m JOIN Insumo i ON i.IdInsumo = m.IdInsumo "
            + "WHERE m.TipoMovimiento = 'Merma' AND (m.CantidadMovimiento >= 0 OR m.CostoMovimiento < 0) "
            + "  AND m.FechaMovimiento >= ? AND m.FechaMovimiento < ? "
            + "ORDER BY Fecha "
            + "LIMIT " + MAXIMO_POR_REVISAR;

    @Override
    public void cargarIngresosGastos(ReporteIngresosGastos reporte) throws DAOException {
        LocalDate desde = reporte.getDesde();
        LocalDate hasta = reporte.getHasta();
        Date inicio = Date.valueOf(desde);
        // "hasta" incluye todo ese día: se compara contra el día siguiente
        Date fin = Date.valueOf(hasta.plusDays(1));

        // Un renglón por cada día del periodo; al final se quitan los días sin movimientos
        List<RenglonDia> dias = new ArrayList<RenglonDia>();
        LocalDate dia = desde;
        while (!dia.isAfter(hasta)) {
            dias.add(new RenglonDia(dia));
            dia = dia.plusDays(1);
        }

        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            leerCobros(conexion, reporte, dias, inicio, fin);
            leerReembolsos(conexion, reporte, dias, inicio, fin);
            leerMovimientos(conexion, reporte, dias, inicio, fin);
            reporte.setPorReembolsar(leerPorReembolsar(conexion, inicio, fin));
            reporte.setPorRevisar(leerPorRevisar(conexion, inicio, fin));
        } catch (SQLException e) {
            throw convertir("Error al consultar los ingresos y gastos", e);
        }

        // Solo quedan los días que tuvieron algún importe
        List<RenglonDia> conDatos = new ArrayList<RenglonDia>();
        for (RenglonDia renglon : dias) {
            boolean vacio = renglon.getCobros() == 0 && renglon.getReembolsos() == 0
                    && renglon.getCompras() == 0 && renglon.getMermas() == 0;
            if (!vacio) {
                conDatos.add(renglon);
            }
        }
        reporte.setDias(conDatos);
    }

    // Pagos validados: suma por tipo y por día
    private void leerCobros(Connection conexion, ReporteIngresosGastos reporte, List<RenglonDia> dias,
                            Date inicio, Date fin) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(SQL_COBROS);
        consulta.setDate(1, inicio);
        consulta.setDate(2, fin);
        consulta.setDate(3, inicio);
        consulta.setDate(4, fin);
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            String tipo = resultado.getString("Tipo");
            double total = resultado.getDouble("Total");
            int numero = resultado.getInt("Numero");
            reporte.setNumeroPagos(reporte.getNumeroPagos() + numero);
            if ("Anticipo".equals(tipo)) {
                reporte.setAnticipos(reporte.getAnticipos() + total);
            } else if ("Pago total".equals(tipo)) {
                reporte.setPagosTotales(reporte.getPagosTotales() + total);
            } else {
                reporte.setLiquidaciones(reporte.getLiquidaciones() + total);
            }
            RenglonDia renglon = renglonDe(dias, reporte.getDesde(), resultado.getDate("Dia"));
            if (renglon != null) {
                renglon.setCobros(renglon.getCobros() + total);
            }
        }
        resultado.close();
        consulta.close();
    }

    // Reembolsos realizados por día
    private void leerReembolsos(Connection conexion, ReporteIngresosGastos reporte, List<RenglonDia> dias,
                                Date inicio, Date fin) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(SQL_REEMBOLSOS);
        consulta.setDate(1, inicio);
        consulta.setDate(2, fin);
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            double total = resultado.getDouble("Total");
            reporte.setReembolsos(reporte.getReembolsos() + total);
            RenglonDia renglon = renglonDe(dias, reporte.getDesde(), resultado.getDate("Dia"));
            if (renglon != null) {
                renglon.setReembolsos(renglon.getReembolsos() + total);
            }
        }
        resultado.close();
        consulta.close();
    }

    // Compras, mermas y producción por día
    private void leerMovimientos(Connection conexion, ReporteIngresosGastos reporte, List<RenglonDia> dias,
                                 Date inicio, Date fin) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(SQL_MOVIMIENTOS);
        consulta.setDate(1, inicio);
        consulta.setDate(2, fin);
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            String tipo = resultado.getString("TipoMovimiento");
            double total = resultado.getDouble("Total");
            int numero = resultado.getInt("Numero");
            RenglonDia renglon = renglonDe(dias, reporte.getDesde(), resultado.getDate("Dia"));
            if ("Compra".equals(tipo)) {
                reporte.setCompras(reporte.getCompras() + total);
                reporte.setNumeroCompras(reporte.getNumeroCompras() + numero);
                if (renglon != null) {
                    renglon.setCompras(renglon.getCompras() + total);
                }
            } else if ("Merma".equals(tipo)) {
                reporte.setMermas(reporte.getMermas() + total);
                reporte.setNumeroMermas(reporte.getNumeroMermas() + numero);
                if (renglon != null) {
                    renglon.setMermas(renglon.getMermas() + total);
                }
            } else {
                // Producción: solo informativo
                reporte.setProduccion(reporte.getProduccion() + total);
            }
        }
        resultado.close();
        consulta.close();
    }

    private double leerPorReembolsar(Connection conexion, Date inicio, Date fin) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(SQL_POR_REEMBOLSAR);
        consulta.setDate(1, inicio);
        consulta.setDate(2, fin);
        ResultSet resultado = consulta.executeQuery();
        double total = 0;
        if (resultado.next()) {
            total = resultado.getDouble(1);
        }
        resultado.close();
        consulta.close();
        return total;
    }

    private List<MovimientoRevisar> leerPorRevisar(Connection conexion, Date inicio, Date fin) throws SQLException {
        List<MovimientoRevisar> lista = new ArrayList<MovimientoRevisar>();
        PreparedStatement consulta = conexion.prepareStatement(SQL_POR_REVISAR);
        // Las tres partes del UNION usan el mismo periodo
        for (int parte = 0; parte < 3; parte++) {
            consulta.setDate(parte * 2 + 1, inicio);
            consulta.setDate(parte * 2 + 2, fin);
        }
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            Timestamp fecha = resultado.getTimestamp("Fecha");
            lista.add(new MovimientoRevisar(fecha.toLocalDateTime(), resultado.getString("Origen"),
                    resultado.getString("Referencia"), resultado.getDouble("Monto"), resultado.getString("Motivo")));
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    // Busca el renglón de un día dentro de la lista (la lista empieza en "desde", un renglón por día)
    private RenglonDia renglonDe(List<RenglonDia> dias, LocalDate desde, Date fecha) {
        if (fecha == null) {
            return null;
        }
        long posicion = ChronoUnit.DAYS.between(desde, fecha.toLocalDate());
        if (posicion < 0 || posicion >= dias.size()) {
            return null;
        }
        return dias.get((int) posicion);
    }

    // ===================== RF-66 · Productos más vendidos =====================

    // Ventas que cuentan: pedidos pagados (o en un estado posterior), sin cancelación ni baja,
    // solicitados dentro del periodo. El filtro de categoría se agrega solo si se eligió una.
    private static final String FROM_VENTAS =
            "FROM DetallePedido d "
            + "JOIN Pedido p ON p.Folio = d.Folio "
            + "JOIN Postre po ON po.IdPostre = d.IdPostre "
            + "JOIN CategoriaPostre c ON c.IdCategoria = po.IdCategoria ";

    private static final String WHERE_VENTAS =
            "WHERE p.FechaSolicitud >= ? AND p.FechaSolicitud < ? "
            + "  AND p.EstadoPedido IN ('Pagado', 'En preparacion', 'Pedido terminado', 'Entregado') "
            + "  AND NOT EXISTS (SELECT 1 FROM Cancelacion cn WHERE cn.Folio = p.Folio) "
            + "  AND NOT EXISTS (SELECT 1 FROM BajaPedido b WHERE b.Folio = p.Folio) ";

    // Una venta está completa si tiene precio (el precio y el costo se congelan al hacer el pedido)
    private static final String VENTA_COMPLETA = "  AND d.PrecioUnitario > 0 ";
    private static final String VENTA_INCOMPLETA = "  AND d.PrecioUnitario <= 0 ";
    private static final String FILTRO_CATEGORIA = "  AND po.IdCategoria = ? ";

    private static final String ORDEN_UNIDADES = "ORDER BY Unidades DESC, Ingresos DESC, Nombre";
    // (MariaDB no acepta operaciones con alias de SUM en el ORDER BY: por eso se usa la columna Utilidad)
    private static final String ORDEN_UTILIDAD = "ORDER BY Utilidad DESC, Unidades DESC, Nombre";

    // Por producto. Los postres personalizados (sin nombre) se juntan en uno por categoría.
    // "Producto inactivo" = ya no se puede vender: su categoría está inactiva o alguna de sus opciones
    // ya no está disponible. "Oculto" = producto del catálogo que no se muestra en la tienda.
    private static final String SQL_POR_PRODUCTO =
            "SELECT CASE WHEN po.NombrePostre IS NULL THEN CONCAT('Personalizado · ', c.NombreCategoria) "
            + "            ELSE po.NombrePostre END AS Nombre, "
            + "       c.NombreCategoria AS Grupo, "
            + "       MAX(CASE WHEN c.EstadoCategoria = 'Inactiva' THEN 1 "
            + "                WHEN po.NombrePostre IS NOT NULL AND EXISTS (SELECT 1 FROM PostreOpcion px "
            + "                     JOIN OpcionCaracteristica ox ON ox.IdOpcion = px.IdOpcion "
            + "                     JOIN Caracteristica cx ON cx.IdCaracteristica = ox.IdCaracteristica "
            + "                     WHERE px.IdPostre = po.IdPostre AND (ox.Disponible = 0 OR cx.Disponible = 0)) THEN 1 "
            + "                ELSE 0 END) AS Inactivo, "
            + "       MAX(CASE WHEN po.NombrePostre IS NOT NULL AND po.MostrarEnTienda = 0 THEN 1 ELSE 0 END) AS Oculto, "
            + "       SUM(d.Cantidad) AS Unidades, SUM(d.Cantidad * d.PrecioUnitario) AS Ingresos, "
            + "       SUM(d.Cantidad * d.CostoUnitario) AS Costo, "
            + "       SUM(d.Cantidad * d.PrecioUnitario) - SUM(d.Cantidad * d.CostoUnitario) AS Utilidad "
            + FROM_VENTAS + WHERE_VENTAS + VENTA_COMPLETA + "%s"
            + "GROUP BY CASE WHEN po.NombrePostre IS NULL THEN -po.IdCategoria ELSE po.IdPostre END, "
            + "         Nombre, c.NombreCategoria ";

    private static final String SQL_POR_CATEGORIA =
            "SELECT c.NombreCategoria AS Nombre, 'Categoría' AS Grupo, "
            + "       MAX(CASE WHEN c.EstadoCategoria = 'Inactiva' THEN 1 ELSE 0 END) AS Inactivo, 0 AS Oculto, "
            + "       SUM(d.Cantidad) AS Unidades, SUM(d.Cantidad * d.PrecioUnitario) AS Ingresos, "
            + "       SUM(d.Cantidad * d.CostoUnitario) AS Costo, "
            + "       SUM(d.Cantidad * d.PrecioUnitario) - SUM(d.Cantidad * d.CostoUnitario) AS Utilidad "
            + FROM_VENTAS + WHERE_VENTAS + VENTA_COMPLETA + "%s"
            + "GROUP BY c.IdCategoria, c.NombreCategoria ";

    // Por opción de personalización: cuántos postres vendidos la llevaban y el importe de esos postres.
    // DISTINCT: si la opción va en varias capas del mismo pastel (NumeroCapa) el postre se cuenta una vez.
    private static final String SQL_POR_OPCION =
            "SELECT o.NombreOpcion AS Nombre, CONCAT(ca.NombreCaracteristica, ' (', c.NombreCategoria, ')') AS Grupo, "
            + "       MAX(CASE WHEN o.Disponible = 0 OR ca.Disponible = 0 THEN 1 ELSE 0 END) AS Inactivo, 0 AS Oculto, "
            + "       SUM(d.Cantidad) AS Unidades, SUM(d.Cantidad * d.PrecioUnitario) AS Ingresos, "
            + "       SUM(d.Cantidad * d.CostoUnitario) AS Costo, "
            + "       SUM(d.Cantidad * d.PrecioUnitario) - SUM(d.Cantidad * d.CostoUnitario) AS Utilidad "
            + FROM_VENTAS
            + "JOIN (SELECT DISTINCT IdPostre, IdOpcion FROM PostreOpcion) pop ON pop.IdPostre = po.IdPostre "
            + "JOIN OpcionCaracteristica o ON o.IdOpcion = pop.IdOpcion "
            + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
            + WHERE_VENTAS + VENTA_COMPLETA + "%s"
            + "GROUP BY o.IdOpcion, o.NombreOpcion, ca.NombreCaracteristica, c.NombreCategoria ";

    private static final String SQL_INCOMPLETAS =
            "SELECT COUNT(*) " + FROM_VENTAS + WHERE_VENTAS + VENTA_INCOMPLETA + "%s";

    private static final String SQL_PEDIDOS =
            "SELECT COUNT(DISTINCT d.Folio) " + FROM_VENTAS + WHERE_VENTAS + VENTA_COMPLETA + "%s";

    @Override
    public void cargarVentas(ReporteVentas reporte, boolean ordenPorUtilidad) throws DAOException {
        String filtro = "";
        if (reporte.getIdCategoria() > 0) {
            filtro = FILTRO_CATEGORIA;
        }
        String orden = ORDEN_UNIDADES;
        if (ordenPorUtilidad) {
            orden = ORDEN_UTILIDAD;
        }
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            reporte.setPorProducto(leerVentas(conexion, String.format(SQL_POR_PRODUCTO, filtro) + orden, reporte));
            reporte.setPorCategoria(leerVentas(conexion, String.format(SQL_POR_CATEGORIA, filtro) + orden, reporte));
            reporte.setPorOpcion(leerVentas(conexion, String.format(SQL_POR_OPCION, filtro) + orden, reporte));
            reporte.setLineasIncompletas(contarVentas(conexion, String.format(SQL_INCOMPLETAS, filtro), reporte));
            reporte.setPedidos(contarVentas(conexion, String.format(SQL_PEDIDOS, filtro), reporte));
        } catch (SQLException e) {
            throw convertir("Error al consultar las ventas", e);
        }
    }

    private List<RenglonVenta> leerVentas(Connection conexion, String sql, ReporteVentas reporte) throws SQLException {
        List<RenglonVenta> lista = new ArrayList<RenglonVenta>();
        PreparedStatement consulta = prepararVentas(conexion, sql, reporte);
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            RenglonVenta renglon = new RenglonVenta(resultado.getString("Nombre"), resultado.getString("Grupo"),
                    resultado.getInt("Unidades"), resultado.getDouble("Ingresos"), resultado.getDouble("Costo"));
            renglon.setInactivo(resultado.getInt("Inactivo") == 1);
            renglon.setOculto(resultado.getInt("Oculto") == 1);
            lista.add(renglon);
        }
        resultado.close();
        consulta.close();
        return lista;
    }

    private int contarVentas(Connection conexion, String sql, ReporteVentas reporte) throws SQLException {
        PreparedStatement consulta = prepararVentas(conexion, sql, reporte);
        ResultSet resultado = consulta.executeQuery();
        int numero = 0;
        if (resultado.next()) {
            numero = resultado.getInt(1);
        }
        resultado.close();
        consulta.close();
        return numero;
    }

    // Pone los parámetros comunes de las consultas de ventas: periodo y, si aplica, categoría
    private PreparedStatement prepararVentas(Connection conexion, String sql, ReporteVentas reporte) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(sql);
        consulta.setDate(1, Date.valueOf(reporte.getDesde()));
        consulta.setDate(2, Date.valueOf(reporte.getHasta().plusDays(1)));
        if (reporte.getIdCategoria() > 0) {
            consulta.setInt(3, reporte.getIdCategoria());
        }
        return consulta;
    }

    @Override
    public boolean existeCategoria(int idCategoria) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM CategoriaPostre WHERE IdCategoria = ?");
            consulta.setInt(1, idCategoria);
            ResultSet resultado = consulta.executeQuery();
            boolean existe = false;
            if (resultado.next()) {
                existe = resultado.getInt(1) > 0;
            }
            resultado.close();
            consulta.close();
            return existe;
        } catch (SQLException e) {
            throw convertir("Error al revisar la categoría", e);
        }
    }

    // Si falta una tabla de la migración (por ejemplo BajaPedido) se explica qué script ejecutar
    private DAOException convertir(String mensaje, SQLException causa) {
        String detalle = String.valueOf(causa.getMessage());
        if (detalle.contains("BajaPedido") || detalle.contains("doesn't exist")) {
            return new DAOException(MSG_FALTA_MIGRACION, causa);
        }
        return new DAOException(mensaje, causa);
    }
}
