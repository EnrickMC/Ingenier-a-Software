package com.pac.modulos.inventario.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.TipoAlerta;

import java.util.List;

/**
 * Operaciones sobre las tablas AlertaInventario y AlertaCaducidad.
 */
public interface AlertaDAO {

    // Alertas ordenadas de la más reciente a la más antigua
    List<AlertaInventario> listar(boolean soloPendientes) throws DAOException;

    // Alertas pendientes de caducidad (de un lote)
    List<AlertaInventario> listarPendientesDeCaducidad() throws DAOException;

    // Número de alertas pendientes (para el contador del menú)
    int contarPendientes() throws DAOException;

    // Busca una alerta por su id; devuelve null si no existe
    AlertaInventario buscarPorId(int idAlerta) throws DAOException;

    // Alerta pendiente de existencia baja del insumo; null si no tiene
    AlertaInventario buscarPendienteDeExistencia(int idInsumo) throws DAOException;

    // Alerta pendiente de caducidad del lote; null si no tiene
    AlertaInventario buscarPendienteDeLote(int idLote) throws DAOException;

    // Guarda la alerta (y su AlertaCaducidad si tiene lote) y devuelve su id
    int insertar(AlertaInventario alerta) throws DAOException;

    // Cambia el tipo y la existencia mostrada de una alerta pendiente
    void actualizar(int idAlerta, TipoAlerta tipo, double existenciaEnAlerta) throws DAOException;

    // EstadoAlerta = 'Atendida'
    void marcarAtendida(int idAlerta) throws DAOException;

    // Atiende todas las alertas pendientes de un insumo (por ejemplo al darlo de baja)
    void atenderPendientesDeInsumo(int idInsumo) throws DAOException;
}
