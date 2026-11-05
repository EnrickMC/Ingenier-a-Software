package com.pac.modulos.pedidos.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.PagoDAO;
import com.pac.modulos.pedidos.model.ArchivoComprobante;
import com.pac.modulos.pedidos.model.EstadoPago;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.MetodoPago;
import com.pac.modulos.pedidos.model.Pago;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de PagoDAO para MySQL (tabla Pago).
 */
public class PagoDAOMySQL implements PagoDAO {

    private static final String SELECT_PAGO =
            "SELECT pg.IdPago, pg.Folio, pg.IdUsuario, u.NombreUsuario, pg.MontoPago, pg.FechaPago, pg.MetodoPago, "
            + "pg.Referencia, pg.LinkComprobante, pg.EstadoValidacion, pg.MotivoRechazo, "
            + "c.NombreCliente, p.EstadoPedido, "
            + "(SELECT COUNT(*) FROM Cancelacion cx WHERE cx.Folio = p.Folio) AS Cancelado, "
            // Solo el tipo del archivo del comprobante (el archivo se lee aparte, al verlo)
            + "ca.TipoContenido AS TipoArchivo "
            + "FROM Pago pg "
            + "JOIN Pedido p ON p.Folio = pg.Folio "
            + "JOIN Cliente c ON c.IdCliente = p.IdCliente "
            + "LEFT JOIN Usuario u ON u.IdUsuario = pg.IdUsuario "
            + "LEFT JOIN ComprobanteArchivo ca ON ca.IdPago = pg.IdPago ";

    @Override
    public List<Pago> listarPorFolio(int folio) throws DAOException {
        String sql = SELECT_PAGO + "WHERE pg.Folio = ? ORDER BY pg.FechaPago, pg.IdPago";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, folio);
            return leerLista(consulta);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los pagos del pedido " + folio, e);
        }
    }

    @Override
    public List<Pago> listar(EstadoPago estado, String texto) throws DAOException {
        // Los pagos de pedidos dados de baja no se muestran
        String sql = SELECT_PAGO + "WHERE NOT EXISTS (SELECT 1 FROM BajaPedido bx WHERE bx.Folio = p.Folio)";
        if (estado != null) {
            sql = sql + " AND pg.EstadoValidacion = ?";
        }
        String limpio = "";
        if (texto != null) {
            limpio = texto.trim();
            if (limpio.startsWith("#")) {
                limpio = limpio.substring(1);
            }
        }
        if (!limpio.isEmpty()) {
            sql = sql + " AND (CAST(pg.Folio AS CHAR) = ? OR c.NombreCliente LIKE ? OR pg.Referencia LIKE ?)";
        }
        sql = sql + " ORDER BY pg.FechaPago DESC, pg.IdPago DESC";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            int posicion = 1;
            if (estado != null) {
                consulta.setString(posicion, estado.getValorBD());
                posicion++;
            }
            if (!limpio.isEmpty()) {
                consulta.setString(posicion, limpio);
                consulta.setString(posicion + 1, ApoyoMySQL.patron(limpio));
                consulta.setString(posicion + 2, ApoyoMySQL.patron(limpio));
            }
            return leerLista(consulta);
        } catch (SQLException e) {
            if (String.valueOf(e.getMessage()).contains("BajaPedido")) {
                throw new DAOException(BitacoraPedidoDAO.MSG_FALTA_MIGRACION, e);
            }
            throw new DAOException("Error al consultar los pagos", e);
        }
    }

    @Override
    public Pago buscarPorId(int idPago) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(SELECT_PAGO + "WHERE pg.IdPago = ?");
            consulta.setInt(1, idPago);
            List<Pago> lista = leerLista(consulta);
            if (lista.isEmpty()) {
                return null;
            }
            return lista.get(0);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar el pago " + idPago, e);
        }
    }

    @Override
    public boolean referenciaEnUso(String referencia, int idPagoExcluido) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM Pago WHERE Referencia = ? AND IdPago <> ? AND EstadoValidacion <> 'Rechazado'");
            consulta.setString(1, referencia.trim());
            consulta.setInt(2, idPagoExcluido);
            ResultSet resultado = consulta.executeQuery();
            int numero = 0;
            if (resultado.next()) {
                numero = resultado.getInt(1);
            }
            resultado.close();
            consulta.close();
            return numero > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar la referencia del pago", e);
        }
    }

    // RF-50 · Paso 2: lee el archivo del comprobante que subió el cliente
    @Override
    public ArchivoComprobante buscarArchivo(int idPago) throws DAOException {
        String sql = "SELECT IdPago, NombreArchivo, TipoContenido, TamanoBytes, Contenido, FechaSubida "
                + "FROM ComprobanteArchivo WHERE IdPago = ?";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setInt(1, idPago);
            ResultSet resultado = consulta.executeQuery();
            ArchivoComprobante archivo = null;
            if (resultado.next()) {
                archivo = new ArchivoComprobante();
                archivo.setIdPago(resultado.getInt("IdPago"));
                archivo.setNombreArchivo(resultado.getString("NombreArchivo"));
                archivo.setTipoContenido(resultado.getString("TipoContenido"));
                archivo.setTamanoBytes(resultado.getInt("TamanoBytes"));
                archivo.setContenido(resultado.getBytes("Contenido"));
                archivo.setFechaSubida(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaSubida"));
            }
            resultado.close();
            consulta.close();
            return archivo;
        } catch (SQLException e) {
            throw new DAOException("Error al leer el comprobante del pago " + idPago, e);
        }
    }

    @Override
    public int contarPendientes() throws DAOException {
        String sql = "SELECT COUNT(*) FROM Pago pg JOIN Pedido p ON p.Folio = pg.Folio "
                + "WHERE pg.EstadoValidacion = 'Pendiente' AND " + ApoyoPedidosMySQL.SIN_CANCELAR_NI_BAJA;
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
            throw new DAOException("Error al contar los pagos pendientes", e);
        }
    }

    // ===================== Dentro de una transacción =====================

    @Override
    public int insertar(Connection conexion, Pago pago) throws DAOException {
        String sql = "INSERT INTO Pago (Folio, IdUsuario, MontoPago, FechaPago, MetodoPago, Referencia, LinkComprobante, "
                + "EstadoValidacion, MotivoRechazo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consulta.setInt(1, pago.getFolio());
            ApoyoPedidosMySQL.ponerUsuario(consulta, 2, pago.getIdUsuario());
            consulta.setDouble(3, pago.getMonto());
            consulta.setTimestamp(4, Timestamp.valueOf(pago.getFechaPago()));
            consulta.setString(5, pago.getMetodo().getValorBD());
            ApoyoPedidosMySQL.ponerTexto(consulta, 6, pago.getReferencia());
            ApoyoPedidosMySQL.ponerTexto(consulta, 7, pago.getLinkComprobante());
            consulta.setString(8, pago.getEstado().getValorBD());
            ApoyoPedidosMySQL.ponerTexto(consulta, 9, pago.getMotivoRechazo());
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
            throw new DAOException("Error al registrar el pago", e);
        }
    }

    @Override
    public void validar(Connection conexion, Pago pago) throws DAOException {
        String sql = "UPDATE Pago SET EstadoValidacion = 'Validado', IdUsuario = ?, MontoPago = ?, MetodoPago = ?, "
                + "Referencia = ?, MotivoRechazo = NULL WHERE IdPago = ?";
        try {
            PreparedStatement consulta = conexion.prepareStatement(sql);
            ApoyoPedidosMySQL.ponerUsuario(consulta, 1, pago.getIdUsuario());
            consulta.setDouble(2, pago.getMonto());
            consulta.setString(3, pago.getMetodo().getValorBD());
            ApoyoPedidosMySQL.ponerTexto(consulta, 4, pago.getReferencia());
            consulta.setInt(5, pago.getIdPago());
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al validar el pago " + pago.getIdPago(), e);
        }
    }

    @Override
    public void rechazar(Connection conexion, int idPago, int idUsuario, String motivo) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "UPDATE Pago SET EstadoValidacion = 'Rechazado', IdUsuario = ?, MotivoRechazo = ? WHERE IdPago = ?");
            ApoyoPedidosMySQL.ponerUsuario(consulta, 1, idUsuario);
            consulta.setString(2, motivo.trim());
            consulta.setInt(3, idPago);
            consulta.executeUpdate();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al rechazar el pago " + idPago, e);
        }
    }

    @Override
    public EstadoPago bloquear(Connection conexion, int idPago) throws DAOException {
        try {
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT EstadoValidacion FROM Pago WHERE IdPago = ? FOR UPDATE");
            consulta.setInt(1, idPago);
            ResultSet resultado = consulta.executeQuery();
            EstadoPago estado = null;
            if (resultado.next()) {
                estado = EstadoPago.desdeBD(resultado.getString(1));
            }
            resultado.close();
            consulta.close();
            return estado;
        } catch (SQLException e) {
            throw new DAOException("Error al bloquear el pago " + idPago, e);
        }
    }

    // ===================== Apoyo =====================

    private List<Pago> leerLista(PreparedStatement consulta) throws SQLException {
        List<Pago> pagos = new ArrayList<Pago>();
        ResultSet resultado = consulta.executeQuery();
        while (resultado.next()) {
            Pago pago = new Pago();
            pago.setIdPago(resultado.getInt("IdPago"));
            pago.setFolio(resultado.getInt("Folio"));
            pago.setIdUsuario(resultado.getInt("IdUsuario"));     // NULL se lee como 0
            pago.setMonto(resultado.getDouble("MontoPago"));
            pago.setFechaPago(ApoyoPedidosMySQL.leerFechaHora(resultado, "FechaPago"));
            pago.setMetodo(MetodoPago.desdeBD(resultado.getString("MetodoPago")));
            pago.setReferencia(resultado.getString("Referencia"));
            pago.setLinkComprobante(resultado.getString("LinkComprobante"));
            pago.setTipoArchivo(resultado.getString("TipoArchivo"));
            pago.setEstado(EstadoPago.desdeBD(resultado.getString("EstadoValidacion")));
            pago.setMotivoRechazo(resultado.getString("MotivoRechazo"));
            pago.setNombreCliente(resultado.getString("NombreCliente"));
            pago.setEstadoPedido(EstadoPedido.desdeBD(resultado.getString("EstadoPedido")));
            pago.setPedidoCancelado(resultado.getInt("Cancelado") > 0);

            // Quién lo revisó: un pago pendiente aún no tiene revisor
            String usuario = resultado.getString("NombreUsuario");
            if (pago.getEstado() == EstadoPago.PENDIENTE) {
                pago.setNombreUsuario("—");
            } else {
                pago.setNombreUsuario(ApoyoPedidosMySQL.nombreQuien(usuario));
            }
            pagos.add(pago);
        }
        resultado.close();
        consulta.close();
        return pagos;
    }
}
