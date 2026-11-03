package com.pac.modulos.inventario.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.IngredienteReceta;
import com.pac.modulos.inventario.model.OpcionReceta;
import com.pac.modulos.inventario.model.Receta;

import java.sql.Connection;
import java.util.List;

/**
 * Operaciones sobre las tablas Receta y RecetaInsumo.
 */
public interface RecetaDAO {

    // Recetas con su opción, costo y número de insumos dados de baja (sin la lista de ingredientes)
    List<Receta> listar(boolean incluirInactivas) throws DAOException;

    // Receta con sus ingredientes; null si no existe
    Receta buscarPorId(int idReceta) throws DAOException;

    // Receta (activa o no) de una opción, con sus ingredientes; null si la opción no tiene
    Receta buscarPorOpcion(int idOpcion) throws DAOException;

    // Opciones disponibles del catálogo que no tienen receta activa
    List<OpcionReceta> listarOpcionesSinRecetaActiva() throws DAOException;

    // Datos de una opción del catálogo; null si no existe
    OpcionReceta buscarOpcion(int idOpcion) throws DAOException;

    // Guarda la receta y sus ingredientes en una transacción; devuelve el id
    int insertar(Receta receta) throws DAOException;

    // Actualiza la receta, la deja activa y reemplaza sus ingredientes, en una transacción
    void actualizar(Receta receta) throws DAOException;

    // Baja lógica: EstadoReceta = 'Inactiva'
    void inhabilitar(int idReceta) throws DAOException;

    // Borra la receta (sus ingredientes se borran en cascada)
    void eliminar(int idReceta) throws DAOException;

    // true si la opción aparece en algún pedido
    boolean opcionEnPedidos(int idOpcion) throws DAOException;

    // Folios de pedidos que usan la opción y aún no entran a preparación (ni están cancelados)
    List<Integer> listarFoliosPorPreparar(int idOpcion) throws DAOException;

    // Ingredientes de una receta (se usa también dentro de la transacción del descuento)
    List<IngredienteReceta> listarIngredientes(Connection conexion, int idReceta) throws DAOException;
}
