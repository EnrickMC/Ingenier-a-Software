package com.pac.modulos.pedidos.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.BajaPedido;
import com.pac.modulos.pedidos.model.Cancelacion;
import com.pac.modulos.pedidos.model.Cliente;
import com.pac.modulos.pedidos.model.Direccion;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.FiltroPedidos;
import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.modulos.pedidos.model.ModalidadEntrega;
import com.pac.modulos.pedidos.model.ModalidadPago;
import com.pac.modulos.pedidos.model.Pedido;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de PedidoDAO para MySQL.
 * El saldo sale de la vista vw_SaldoPedido; la cancelación y la baja, de sus tablas.
 */
public class PedidoDAOMySQL implements PedidoDAO {

    // Marca de versión: cambia si cambia el estado, hay un cambio en el historial,
    // una modificación, una cancelación o una baja (control de concurrencia)
    private static final String VERSION =
            "CONCAT(p.EstadoPedido, '|', "
            + "COALESCE((SELECT MAX(h.IdHistorial) FROM HistorialEstado h WHERE h.Folio = p.Folio), 0), '|', "
            + "COALESCE((SELECT MAX(m.IdModificacion) FROM ModificacionPedido m WHERE m.Folio = p.Folio), 0), '|', "
            + "(SELECT COUNT(*) FROM Cancelacion c2 WHERE c2.Folio = p.Folio), '|', "
            + "(SELECT COUNT(*) FROM BajaPedido b2 WHERE b2.Folio = p.Folio))";

    // Consulta base de un pedido con todo lo que se muestra en la lista y en la ficha
    private static final String SELECT_PEDIDO =
            "SELECT p.Folio, p.IdCliente, c.NombreCliente, c.TelefonoWhatsApp, c.CorreoCliente, "
            + "p.FechaSolicitud, p.FechaEntrega, p.HorarioEntrega, p.ModalidadEntrega, p.ModalidadPago, "
            + "p.MontoTotal, p.AnticipoRequerido, p.EstadoPedido, COALESCE(s.TotalPagado, 0) AS TotalPagado, "
            + "(SELECT COUNT(*) FROM Pago pg WHERE pg.Folio = p.Folio AND pg.EstadoValidacion = 'Pendiente') AS PagosPendientes, "
            + "(SELECT COALESCE(SUM(d.Cantidad), 0) FROM DetallePedido d WHERE d.Folio = p.Folio) AS Articulos, "
            + "(SELECT GROUP_CONCAT(CONCAT(d.Cantidad, ' × ', COALESCE(po.NombrePostre, CONCAT(cat.NombreCategoria, ' personalizado'))) "
            + "        ORDER BY po.NombrePostre SEPARATOR ', ') "
            + "   FROM DetallePedido d JOIN Postre po ON po.IdPostre = d.IdPostre "
            + "   JOIN CategoriaPostre cat ON cat.IdCategoria = po.IdCategoria WHERE d.Folio = p.Folio) AS Resumen, "
            + "cn.Folio AS FolioCancelado, cn.MotivoCancelacion, cn.FechaHoraCancelacion, cn.MontoReembolso, "
            + "cn.EstadoReembolso, uc.NombreUsuario AS UsuarioCancelo, "
            + "b.Folio AS FolioBaja, b.MotivoBaja, b.FechaHoraBaja, ub.NombreUsuario AS UsuarioBaja, "
            + "dir.IdDireccion, dir.Calle, dir.NumeroExterior, dir.NumeroInterior, dir.Colonia, dir.CodigoPostal, "
            + "dir.Referencias, " + VERSION + " AS Version "
            + "FROM Pedido p "
            + "JOIN Cliente c ON c.IdCliente = p.IdCliente "
            + "LEFT JOIN vw_SaldoPedido s ON s.Folio = p.Folio "
            + "LEFT JOIN Cancelacion cn ON cn.Folio = p.Folio "
            + "LEFT JOIN Usuario uc ON uc.IdUsuario = cn.IdUsuario "
            + "LEFT JOIN BajaPedido b ON b.Folio = p.Folio "
            + "LEFT JOIN Usuario ub ON ub.IdUsuario = b.IdUsuario "
            + "LEFT JOIN EntregaDomicilio ed ON ed.Folio = p.Folio "
            + "LEFT JOIN Direccion dir ON dir.IdDireccion = ed.IdDireccion ";

    // Estados que aparecen en la agenda de producción (RF-45)
    private static final String ESTADOS_AGENDA =
            "p.EstadoPedido IN ('Aprobado', 'Pagado', 'En preparacion', 'Pedido terminado')";

    // ===================== Consultas =====================

    @Override
    public List<Pedido> buscar(FiltroPedidos filtro) throws DAOException {
        String sql = SELECT_PEDIDO + "WHERE 1 = 1";
        List<Object> valores = new ArrayList<Object>();

        // Grupo de pedidos
        if (filtro.getGrupo() == FiltroPedidos.ACTIVOS) {
            sql = sql + " AND cn.Folio IS NULL AND b.Folio IS NULL";
            // Los entregados salen de la lista de trabajo, salvo que se pidan con el filtro de estado
            if (filtro.getEstado() == null) {
                sql = sql + " AND p.EstadoPedido <> 'Entregado'";
            }
        } else if (filtro.getGrupo() == FiltroPedidos.CANCELADOS) {
            sql = sql + " AND cn.Folio IS NOT NULL AND b.Folio IS NULL";
        } else if (filtro.getGrupo() == FiltroPedidos.DADOS_DE_BAJA) {
            sql = sql + " AND b.Folio IS NOT NULL";
        }

        // Estado
        if (filtro.getEstado() != null) {
            sql = sql + " AND p.EstadoPedido = ?";
            valores.add(filtro.getEstado().getValorBD());
        }

        // Búsqueda por folio, nombre o teléfono
        String texto = filtro.getTexto();
        if (texto != null && !texto.isBlank()) {
            String limpio = texto.trim();
            if (limpio.startsWith("#")) {
                // "#123" busca solo por folio
                sql = sql + " AND CAST(p.Folio AS CHAR) = ?";
                valores.add(limpio.substring(1).trim());
            } else {
                sql = sql + " AND (CAST(p.Folio AS CHAR) = ? OR c.NombreCliente LIKE ? OR c.TelefonoWhatsApp LIKE ?)";
                valores.add(limpio);
                valores.add(ApoyoMySQL.patron(limpio));
                valores.add(ApoyoMySQL.patron(limpio));
            }
        }

        // Rango de fechas de solicitud
        if (filtro.getDesde() != null) {
            sql = sql + " AND DATE(p.FechaSolicitud) >= ?";
            valores.add(Date.valueOf(filtro.getDesde()));
        }
        if (filtro.getHasta() != null) {
            sql = sql + " AND DATE(p.FechaSolicitud) <= ?";
            valores.add(Date.valueOf(filtro.getHasta()));
        }
        sql = sql + " ORDER BY p.FechaSolicitud DESC, p.Folio DESC";

        return consultarPedidos(sql, valores);
    }

    @Override
    public Pedido buscarPorFolio(int folio) throws DAOException {
        List<Object> valores = new ArrayList<Object>();
        valores.add(folio);
        List<Pedido> lista = consultarPedidos(SELECT_PEDIDO + "WHERE p.Folio = ?", valores);
        if (lista.isEmpty()) {
            return null;
        }
        return lista.get(0);
    }

    @Override
    public List<Pedido> listarAgenda(LocalDate desde, LocalDate hasta) throws DAOException {
        String sql = SELECT_PEDIDO + "WHERE " + ESTADOS_AGENDA + " AND cn.Folio IS NULL AND b.Folio IS NULL "
                + "AND p.FechaEntrega BETWEEN ? AND ? ORDER BY p.FechaEntrega, p.HorarioEntrega, p.Folio";
        List<Object> valores = new ArrayList<Object>();
        valores.add(Date.valueOf(desde));
        valores.add(Date.valueOf(hasta));
        return consultarPedidos(sql, valores);
    }

    @Override
    public List<Pedido> listarAtrasados() throws DAOException {
        String sql = SELECT_PEDIDO + "WHERE " + ESTADOS_AGENDA + " AND cn.Folio IS NULL AND b.Folio IS NULL "
                + "AND TIMESTAMP(p.FechaEntrega, p.HorarioEntrega) < NOW() ORDER BY p.FechaEntrega, p.HorarioEntrega";
        return consultarPedidos(sql, new ArrayList<Object>());
    }

    @Override
    public int contarEntregasDelDia(LocalDate fecha, int folioExcluido) throws DAOException {
        String sql = "SELECT COUNT(*) FROM Pedido p WHERE p.FechaEntrega = ? AND p.Folio <> ? "
                + "AND p.EstadoPedido <> 'Entregado' AND " + ApoyoPedidosMySQL.SIN_CANCELAR_NI_BAJA;
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setDate(1, Date.valueOf(fecha));
            consulta.setInt(2, folioExcluido);
            ResultSet resultado = consulta.executeQuery();
            int total = 0;
            if (resultado.next()) {
                total = resultado.getInt(1);
            }
            resultado.close();
            consulta.close();
            return total;
        } catch (SQLException e) {
            throw error("Error al consultar la carga de la agenda", e);
        }
    }

    @Override
    public int contarPorRevisar() throws DAOException {
        String sql = "SELECT COUNT(*) FROM Pedido p WHERE p.EstadoPedido = 'En proceso de aceptacion' AND "
                + ApoyoPedidosMySQL.SIN_CANCELAR_NI_BAJA;
        return contarSinParametros(sql);
    }

    @Override
    public int ultimoFolio() throws DAOException {
        return contarSinParametros("SELECT COALESCE(MAX(Folio), 0) FROM Pedido");
    }

    @Override
    public List<String> listarProductosInactivos(int folio) throws DAOException {
        String sql = "SELECT DISTINCT cat.NombreCategoria, cat.EstadoCategoria, ca.NombreCaracteristica, ca.Disponible AS CaracDisponible, "
                + "o.NombreOpcion, o.Disponible AS OpcionDisponible "
                + "FROM DetallePedido d "
                + "JOIN Postre po ON po.IdPostre = d.IdPostre "
                + "JOIN CategoriaPostre cat ON cat.IdCategoria = po.IdCategoria "
                + "JOIN PostreOpcion pop ON pop.IdPostre = d.IdPostre "
                + "JOIN OpcionCaracteristica o ON o.IdOpcion = pop.IdOpcion "
                + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
                + "WHERE d.Folio = ? AND (cat.EstadoCategoria <> 'Activa' OR ca.Disponible = 0 OR o.Disponible = 0)";
        List<String> textos = new ArrayList<String>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                String texto;
                if (!"Activa".equals(resultado.getString("EstadoCategoria"))) {
                    texto = "La categoría «" + resultado.getString("NombreCategoria") + "» ya no está activa";
                } else if (!resultado.getBoolean("CaracDisponible")) {
                    texto = "La característica «" + resultado.getString("NombreCaracteristica") + "» ya no está disponible";
                } else {
                    texto = "La opción «" + resultado.getString("NombreOpcion") + "» ya no está disponible";
                }
                if (!textos.contains(texto)) {
                    textos.add(texto);
                }
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al revisar los productos del pedido " + folio, e);
        }
        return textos;
    }

    @Override
    public List<LineaPedido> listarLineas(int folio) throws DAOException {
        String sql = "SELECT d.Folio, d.IdPostre, d.Cantidad, d.Dedicatoria, d.Comentario, d.PrecioUnitario, d.CostoUnitario, "
                + "po.NombrePostre, po.LinkFotografia, po.IdCategoria, cat.NombreCategoria "
                + "FROM DetallePedido d "
                + "JOIN Postre po ON po.IdPostre = d.IdPostre "
                + "JOIN CategoriaPostre cat ON cat.IdCategoria = po.IdCategoria "
                + "WHERE d.Folio = ? ORDER BY d.IdPostre";
        List<LineaPedido> lineas = new ArrayList<LineaPedido>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                LineaPedido linea = new LineaPedido();
                linea.setFolio(resultado.getInt("Folio"));
                linea.setIdPostre(resultado.getInt("IdPostre"));
                linea.setCantidad(resultado.getInt("Cantidad"));
                linea.setDedicatoria(resultado.getString("Dedicatoria"));
                linea.setComentario(resultado.getString("Comentario"));
                linea.setPrecioUnitario(resultado.getDouble("PrecioUnitario"));
                linea.setCostoUnitario(resultado.getDouble("CostoUnitario"));
                linea.setNombrePostre(resultado.getString("NombrePostre"));
                linea.setLinkFotografia(resultado.getString("LinkFotografia"));
                linea.setIdCategoria(resultado.getInt("IdCategoria"));
                linea.setNombreCategoria(resultado.getString("NombreCategoria"));
                lineas.add(linea);
            }
            resultado.close();
            consulta.close();

            // Personalización de cada postre
            for (LineaPedido linea : lineas) {
                cargarEspecificaciones(conexion, linea);
            }
        } catch (SQLException e) {
            throw error("Error al consultar los postres del pedido " + folio, e);
        }
        return lineas;
    }

    // "Característica: Opción" de cada opción del postre, y las imágenes de referencia
    private void cargarEspecificaciones(Connection conexion, LineaPedido linea) throws SQLException {
        String sql = "SELECT ca.NombreCaracteristica, o.NombreOpcion, pop.CantidadElegida, o.LinkImagen "
                + "FROM PostreOpcion pop "
                + "JOIN OpcionCaracteristica o ON o.IdOpcion = pop.IdOpcion "
                + "JOIN Caracteristica ca ON ca.IdCaracteristica = o.IdCaracteristica "
                + "WHERE pop.IdPostre = ? ORDER BY ca.OrdenMostrar, ca.NombreCaracteristica, o.NombreOpcion";
        PreparedStatement consulta = conexion.prepareStatement(sql);
        consulta.setInt(1, linea.getIdPostre());
        ResultSet resultado = consulta.executeQuery();
        List<String> especificaciones = new ArrayList<String>();
        List<String> imagenes = new ArrayList<String>();
        while (resultado.next()) {
            String texto = resultado.getString("NombreCaracteristica") + ": " + resultado.getString("NombreOpcion");
            int veces = resultado.getInt("CantidadElegida");
            if (veces > 1) {
                texto = texto + " ×" + veces;
            }
            especificaciones.add(texto);
            String imagen = resultado.getString("LinkImagen");
            if (imagen != null && !imagen.isBlank()) {
                imagenes.add(imagen);
            }
        }
        resultado.close();
        consulta.close();
        linea.setEspecificaciones(especificaciones);
        linea.setImagenes(imagenes);
    }

    // ===================== Dentro de una transacción =====================

    @Override
    public String bloquear(Connection conexion, int folio) throws DAOException {
        try {
            // FOR UPDATE: nadie más cambia el pedido hasta que termine la transacción
            int existe = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM Pedido WHERE Folio = ? FOR UPDATE", folio);
            if (existe == 0) {
                return null;
            }
            PreparedStatement consulta = conexion.prepareStatement("SELECT " + VERSION + " FROM Pedido p WHERE p.Folio = ?");
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            String version = null;
            if (resultado.next()) {
                version = resultado.getString(1);
            }
            resultado.close();
            consulta.close();
            return version;
        } catch (SQLException e) {
            throw error("Error al bloquear el pedido " + folio, e);
        }
    }

    @Override
    public void cambiarEstado(Connection conexion, int folio, EstadoPedido nuevo) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement("UPDATE Pedido SET EstadoPedido = ? WHERE Folio = ?");
            consulta.setString(1, nuevo.getValorBD());
            consulta.setInt(2, folio);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al cambiar el estado del pedido " + folio, e);
        }
    }

    @Override
    public void actualizarAnticipo(Connection conexion, int folio, double anticipo) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement("UPDATE Pedido SET AnticipoRequerido = ? WHERE Folio = ?");
            consulta.setDouble(1, anticipo);
            consulta.setInt(2, folio);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al actualizar el anticipo del pedido " + folio, e);
        }
    }

    @Override
    public void actualizarDatos(Connection conexion, Pedido pedido) throws DAOException {
        String sql = "UPDATE Pedido SET FechaEntrega = ?, HorarioEntrega = ?, ModalidadEntrega = ?, MontoTotal = ?, "
                + "AnticipoRequerido = ? WHERE Folio = ?";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setDate(1, Date.valueOf(pedido.getFechaEntrega()));
            consulta.setTime(2, Time.valueOf(pedido.getHorarioEntrega()));
            consulta.setString(3, pedido.getModalidadEntrega().getValorBD());
            consulta.setDouble(4, pedido.getMontoTotal());
            consulta.setDouble(5, pedido.getAnticipoRequerido());
            consulta.setInt(6, pedido.getFolio());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al actualizar el pedido " + pedido.getFolio(), e);
        }
    }

    @Override
    public void reemplazarLineas(Connection conexion, int folio, List<LineaPedido> lineas) throws DAOException {
        String sql = "INSERT INTO DetallePedido (Folio, IdPostre, Cantidad, Dedicatoria, Comentario, PrecioUnitario, "
                + "CostoUnitario) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM DetallePedido WHERE Folio = ?", folio);
            PreparedStatement consulta = conexion.prepareStatement(sql);
            for (LineaPedido linea : lineas) {
                consulta.setInt(1, folio);
                consulta.setInt(2, linea.getIdPostre());
                consulta.setInt(3, linea.getCantidad());
                ApoyoPedidosMySQL.ponerTexto(consulta, 4, linea.getDedicatoria());
                ApoyoPedidosMySQL.ponerTexto(consulta, 5, linea.getComentario());
                consulta.setDouble(6, linea.getPrecioUnitario());
                consulta.setDouble(7, linea.getCostoUnitario());
                consulta.executeUpdate();
            }
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al guardar los postres del pedido " + folio, e);
        }
    }

    @Override
    public double sumarPagado(Connection conexion, int folio) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT COALESCE(SUM(MontoPago), 0) FROM Pago WHERE Folio = ? AND EstadoValidacion = 'Validado'");
            consulta.setInt(1, folio);
            ResultSet resultado = consulta.executeQuery();
            double total = 0;
            if (resultado.next()) {
                total = resultado.getDouble(1);
            }
            resultado.close();
            consulta.close();
            return total;
        } catch (SQLException e) {
            throw error("Error al sumar los pagos del pedido " + folio, e);
        }
    }

    // ===================== Apoyo =====================

    // Ejecuta una consulta de pedidos con sus parámetros (textos, números o fechas)
    private List<Pedido> consultarPedidos(String sql, List<Object> valores) throws DAOException {
        List<Pedido> pedidos = new ArrayList<Pedido>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            for (int i = 0; i < valores.size(); i++) {
                consulta.setObject(i + 1, valores.get(i));
            }
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                pedidos.add(leerPedido(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw error("Error al consultar los pedidos", e);
        }
        return pedidos;
    }

    // Convierte el renglón actual en un Pedido
    private Pedido leerPedido(ResultSet resultado) throws SQLException {
        Pedido pedido = new Pedido();
        pedido.setFolio(resultado.getInt("Folio"));

        Cliente cliente = new Cliente();
        cliente.setIdCliente(resultado.getInt("IdCliente"));
        cliente.setNombre(resultado.getString("NombreCliente"));
        cliente.setTelefono(resultado.getString("TelefonoWhatsApp"));
        cliente.setCorreo(resultado.getString("CorreoCliente"));
        pedido.setCliente(cliente);

        pedido.setFechaSolicitud(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaSolicitud"));
        Date entrega = resultado.getDate("FechaEntrega");
        if (entrega != null) {
            pedido.setFechaEntrega(entrega.toLocalDate());
        }
        Time horario = resultado.getTime("HorarioEntrega");
        if (horario != null) {
            pedido.setHorarioEntrega(horario.toLocalTime());
        }
        pedido.setModalidadEntrega(ModalidadEntrega.desdeBD(resultado.getString("ModalidadEntrega")));
        pedido.setModalidadPago(ModalidadPago.desdeBD(resultado.getString("ModalidadPago")));
        pedido.setMontoTotal(resultado.getDouble("MontoTotal"));
        pedido.setAnticipoRequerido(resultado.getDouble("AnticipoRequerido"));
        pedido.setEstado(EstadoPedido.desdeBD(resultado.getString("EstadoPedido")));
        pedido.setTotalPagado(resultado.getDouble("TotalPagado"));
        pedido.setPagosPendientes(resultado.getInt("PagosPendientes"));
        pedido.setNumeroArticulos(resultado.getInt("Articulos"));
        pedido.setResumenProductos(resultado.getString("Resumen"));
        pedido.setVersion(resultado.getString("Version"));

        // Cancelación
        resultado.getInt("FolioCancelado");
        if (!resultado.wasNull()) {
            Cancelacion cancelacion = new Cancelacion();
            cancelacion.setFolio(pedido.getFolio());
            cancelacion.setMotivo(resultado.getString("MotivoCancelacion"));
            cancelacion.setFecha(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaHoraCancelacion"));
            cancelacion.setMontoReembolso(resultado.getDouble("MontoReembolso"));
            cancelacion.setEstadoReembolso(resultado.getString("EstadoReembolso"));
            cancelacion.setNombreUsuario(ApoyoPedidosMySQL.nombreQuien(resultado.getString("UsuarioCancelo")));
            pedido.setCancelacion(cancelacion);
        }

        // Baja
        resultado.getInt("FolioBaja");
        if (!resultado.wasNull()) {
            BajaPedido baja = new BajaPedido();
            baja.setFolio(pedido.getFolio());
            baja.setMotivo(resultado.getString("MotivoBaja"));
            baja.setFecha(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaHoraBaja"));
            baja.setNombreUsuario(ApoyoPedidosMySQL.nombreQuien(resultado.getString("UsuarioBaja")));
            pedido.setBaja(baja);
        }

        // Dirección de entrega
        int idDireccion = resultado.getInt("IdDireccion");
        if (!resultado.wasNull()) {
            Direccion direccion = new Direccion();
            direccion.setIdDireccion(idDireccion);
            direccion.setIdCliente(cliente.getIdCliente());
            direccion.setCalle(resultado.getString("Calle"));
            direccion.setNumeroExterior(resultado.getString("NumeroExterior"));
            direccion.setNumeroInterior(resultado.getString("NumeroInterior"));
            direccion.setColonia(resultado.getString("Colonia"));
            direccion.setCodigoPostal(resultado.getString("CodigoPostal"));
            direccion.setReferencias(resultado.getString("Referencias"));
            pedido.setDireccion(direccion);
        }
        return pedido;
    }

    private int contarSinParametros(String sql) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            ResultSet resultado = consulta.executeQuery();
            int numero = 0;
            if (resultado.next()) {
                numero = resultado.getInt(1);
            }
            resultado.close();
            consulta.close();
            return numero;
        } catch (SQLException e) {
            throw error("Error al contar los pedidos", e);
        }
    }

    // Arma el error; si la causa es que falta la migración V4, lo dice
    private DAOException error(String mensaje, SQLException causa) {
        if (ApoyoPedidosMySQL.faltaMigracion(causa) || String.valueOf(causa.getMessage()).contains("doesn't exist")) {
            return new DAOException(BitacoraPedidoDAO.MSG_FALTA_MIGRACION, causa);
        }
        return new DAOException(mensaje, causa);
    }
}
