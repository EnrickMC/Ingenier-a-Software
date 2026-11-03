package com.pac.modulos.catalogo.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Opcion;

import java.util.List;

/**
 * Operaciones sobre la tabla OpcionCaracteristica.
 * Cada opción se lee con su costo de receta (vista vw_CostoReceta), si tiene.
 * La clase OpcionDAOMySQL es la que las implementa.
 */
public interface OpcionDAO {

    // Opciones de una característica, ordenadas por nombre
    List<Opcion> listarPorCaracteristica(int idCaracteristica) throws DAOException;

    // Opciones de todas las características de una categoría
    List<Opcion> listarPorCategoria(int idCategoria) throws DAOException;

    // Busca una opción por su id; devuelve null si no existe
    Opcion buscarPorId(int idOpcion) throws DAOException;

    // Busca por nombre dentro de una característica; devuelve null si no existe
    Opcion buscarPorNombre(int idCaracteristica, String nombre) throws DAOException;

    // Guarda una opción nueva y devuelve su id
    int insertar(Opcion opcion) throws DAOException;

    // Actualiza todos los datos de la opción
    void actualizar(Opcion opcion) throws DAOException;

    // Cuántas opciones disponibles tiene una característica
    int contarDisponibles(int idCaracteristica) throws DAOException;

    // true si la opción está en algún postre o tiene receta
    boolean tieneHistorial(int idOpcion) throws DAOException;

    // Folios de pedidos vigentes (no entregados ni cancelados) que usan la opción
    List<Integer> listarFoliosVigentes(int idOpcion) throws DAOException;

    // Baja lógica: opción no disponible, receta inactiva y exclusiones inactivas
    void inhabilitar(int idOpcion) throws DAOException;

    // Borra la opción y sus exclusiones
    void eliminar(int idOpcion) throws DAOException;
}
