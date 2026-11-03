package com.pac.modulos.pedidos.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.FiltroPedidos;
import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.modulos.pedidos.model.Pedido;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * Operaciones sobre Pedido, DetallePedido y la vista vw_SaldoPedido.
 * Los métodos que reciben Connection corren dentro de una transacción que abre el servicio.
 * La clase PedidoDAOMySQL es la que las implementa.
 */
public interface PedidoDAO {

    // Lista de pedidos según el filtro, del más reciente al más antiguo (RF-21, RF-22)
    List<Pedido> buscar(FiltroPedidos filtro) throws DAOException;

    // Pedido con cliente, dirección, saldo, cancelación y baja; null si no existe
    Pedido buscarPorFolio(int folio) throws DAOException;

    // Postres del pedido con su personalización
    List<LineaPedido> listarLineas(int folio) throws DAOException;

    // Pedidos de la agenda (Aprobado a Pedido terminado, sin cancelar ni dar de baja) entre dos fechas de entrega
    List<Pedido> listarAgenda(LocalDate desde, LocalDate hasta) throws DAOException;

    // Pedidos de la agenda con la fecha de entrega ya vencida
    List<Pedido> listarAtrasados() throws DAOException;

    // Pedidos vigentes (sin cancelar ni dar de baja, no entregados) que se entregan ese día
    int contarEntregasDelDia(LocalDate fecha, int folioExcluido) throws DAOException;

    // Solicitudes nuevas "En proceso de aceptacion" sin cancelar ni dar de baja
    int contarPorRevisar() throws DAOException;

    // Folio más alto registrado (para saber si llegó un pedido nuevo de la web)
    int ultimoFolio() throws DAOException;

    // Opciones, características o categorías del pedido que ya no están activas (textos para mostrar)
    List<String> listarProductosInactivos(int folio) throws DAOException;

    // ----- Dentro de una transacción -----

    // Bloquea el pedido (FOR UPDATE) y devuelve su marca de versión; null si no existe
    String bloquear(Connection conexion, int folio) throws DAOException;

    void cambiarEstado(Connection conexion, int folio, EstadoPedido nuevo) throws DAOException;

    void actualizarAnticipo(Connection conexion, int folio, double anticipo) throws DAOException;

    // Actualiza fecha, horario, modalidad de entrega, total y anticipo
    void actualizarDatos(Connection conexion, Pedido pedido) throws DAOException;

    // Borra los postres del pedido y guarda los nuevos
    void reemplazarLineas(Connection conexion, int folio, List<LineaPedido> lineas) throws DAOException;

    // Suma de los pagos validados del pedido
    double sumarPagado(Connection conexion, int folio) throws DAOException;
}
