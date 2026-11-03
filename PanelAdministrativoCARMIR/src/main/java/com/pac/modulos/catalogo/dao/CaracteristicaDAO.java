package com.pac.modulos.catalogo.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Caracteristica;

import java.util.List;

/**
 * Operaciones sobre la tabla Caracteristica.
 * La clase CaracteristicaDAOMySQL es la que las implementa.
 */
public interface CaracteristicaDAO {

    // Características de una categoría ordenadas por OrdenMostrar (con su número de opciones)
    List<Caracteristica> listarPorCategoria(int idCategoria) throws DAOException;

    // Busca una característica por su id; devuelve null si no existe
    Caracteristica buscarPorId(int idCaracteristica) throws DAOException;

    // Busca por nombre dentro de una categoría; devuelve null si no existe
    Caracteristica buscarPorNombre(int idCategoria, String nombre) throws DAOException;

    // true si otra característica de la categoría ya usa ese orden (idExcluir = la que se edita)
    boolean ordenOcupado(int idCategoria, int orden, int idExcluir) throws DAOException;

    // Guarda una característica nueva. Si recorrerOrden es true, primero mueve
    // una posición hacia abajo las que tengan ese orden o uno mayor.
    int insertar(Caracteristica caracteristica, boolean recorrerOrden) throws DAOException;

    // Actualiza una característica (con la misma regla de recorrerOrden)
    void actualizar(Caracteristica caracteristica, boolean recorrerOrden) throws DAOException;

    // Intercambia el OrdenMostrar de dos características (botones subir / bajar)
    void intercambiarOrden(Caracteristica primera, Caracteristica segunda) throws DAOException;

    // true si alguna de sus opciones está en un postre o tiene receta
    boolean tieneHistorial(int idCaracteristica) throws DAOException;

    // Folios de pedidos vigentes (no entregados ni cancelados) que usan alguna de sus opciones
    List<Integer> listarFoliosVigentes(int idCaracteristica) throws DAOException;

    // Baja lógica: la característica, sus opciones, sus recetas y sus exclusiones quedan inactivas
    void inhabilitar(int idCaracteristica) throws DAOException;

    // Borra la característica junto con sus opciones y exclusiones
    void eliminar(int idCaracteristica) throws DAOException;
}
