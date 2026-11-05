package com.pac.modulos.reportes.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.reportes.model.ReporteIngresosGastos;
import com.pac.modulos.reportes.model.ReporteVentas;

/**
 * Consultas de los reportes financieros (Módulo 6).
 * Todas son consultas agregadas (SUM, COUNT, GROUP BY): no se cargan los pagos
 * ni los movimientos uno por uno en memoria.
 */
public interface ReporteDAO {

    String MSG_FALTA_MIGRACION = "Falta ejecutar DBCarmir/migraciones/MIGRACION_COMPLETA.sql en la base de datos";

    // RF-65: llena el reporte con los pagos, reembolsos, compras y mermas de su periodo
    void cargarIngresosGastos(ReporteIngresosGastos reporte) throws DAOException;

    // RF-66: llena el reporte con las ventas agrupadas por producto, categoría y opción.
    // ordenPorUtilidad = false ordena por unidades vendidas
    void cargarVentas(ReporteVentas reporte, boolean ordenPorUtilidad) throws DAOException;

    // true si la categoría todavía existe en la base de datos
    boolean existeCategoria(int idCategoria) throws DAOException;
}
