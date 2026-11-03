package com.pac.modulos.catalogo.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.EstadoRegistro;

import java.util.List;

/**
 * Operaciones sobre la tabla CategoriaPostre (Módulo 3: Catálogo de Personalización).
 * La clase CategoriaDAOMySQL es la que las implementa.
 */
public interface CategoriaDAO {

    // Busca categorías cuyo nombre o descripción contenga el texto.
    // Texto vacío = todas. Estado null = cualquier estado.
    List<Categoria> buscar(String texto, EstadoRegistro estado) throws DAOException;

    // Busca una categoría por su id; devuelve null si no existe
    Categoria buscarPorId(int idCategoria) throws DAOException;

    // Busca una categoría por su nombre exacto; devuelve null si no existe
    Categoria buscarPorNombre(String nombre) throws DAOException;

    // Guarda una categoría nueva y devuelve el id que le asignó la base de datos
    int insertar(Categoria categoria) throws DAOException;

    // Actualiza nombre, descripción y estado
    void actualizar(Categoria categoria) throws DAOException;

    // Nombres de los productos de la categoría que se muestran en la tienda
    List<String> listarProductosActivos(int idCategoria) throws DAOException;

    // true si la categoría ya se usó: tiene postres o alguna de sus opciones tiene receta
    boolean tieneHistorial(int idCategoria) throws DAOException;

    // Baja lógica: categoría Inactiva y sus características y opciones no disponibles
    void inhabilitar(int idCategoria) throws DAOException;

    // Borra la categoría junto con sus características, opciones y exclusiones
    void eliminar(int idCategoria) throws DAOException;
}
