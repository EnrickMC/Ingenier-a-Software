package com.pac.modulos.pedidos.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.pedidos.model.ArchivoComprobante;
import com.pac.modulos.pedidos.model.EstadoPago;
import com.pac.modulos.pedidos.model.Pago;

import java.sql.Connection;
import java.util.List;

/**
 * Operaciones sobre la tabla Pago (RF-42, RF-43).
 */
public interface PagoDAO {

    List<Pago> listarPorFolio(int folio) throws DAOException;

    // Pagos de todos los pedidos con ese estado (null = todos); texto busca folio, cliente o referencia
    List<Pago> listar(EstadoPago estado, String texto) throws DAOException;

    // Pago con los datos de su pedido; null si no existe
    Pago buscarPorId(int idPago) throws DAOException;

    // true si otro pago ya usa esa referencia
    boolean referenciaEnUso(String referencia, int idPagoExcluido) throws DAOException;

    // RF-50: foto o PDF del comprobante que subió el cliente; null si el pago no tiene archivo
    ArchivoComprobante buscarArchivo(int idPago) throws DAOException;

    // Número de pagos pendientes de validar en pedidos sin cancelar ni dar de baja
    int contarPendientes() throws DAOException;

    // ----- Dentro de una transacción -----

    // Guarda un pago nuevo y devuelve su id
    int insertar(Connection conexion, Pago pago) throws DAOException;

    // Marca un pago pendiente como Validado (puede corregir el monto, la referencia y el método)
    void validar(Connection conexion, Pago pago) throws DAOException;

    // Marca un pago pendiente como Rechazado con su motivo
    void rechazar(Connection conexion, int idPago, int idUsuario, String motivo) throws DAOException;

    // Estado actual del pago bloqueado (FOR UPDATE); null si no existe
    EstadoPago bloquear(Connection conexion, int idPago) throws DAOException;
}
