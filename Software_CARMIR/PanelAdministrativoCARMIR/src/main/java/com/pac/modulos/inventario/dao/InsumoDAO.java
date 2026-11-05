package com.pac.modulos.inventario.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.inventario.model.Insumo;

import java.util.List;

/**
 * Operaciones sobre la tabla Insumo.
 * Cada insumo se lee con su existencia (suma de sus lotes) y la caducidad más próxima.
 * La clase InsumoDAOMySQL es la que las implementa.
 */
public interface InsumoDAO {

    // Insumos ordenados por nombre; si incluirInactivos es false, solo los activos
    List<Insumo> listar(boolean incluirInactivos) throws DAOException;

    // Busca un insumo por su id; devuelve null si no existe
    Insumo buscarPorId(int idInsumo) throws DAOException;

    // Busca un insumo por su nombre (activo o no); devuelve null si no existe
    Insumo buscarPorNombre(String nombre) throws DAOException;

    // Categorías de insumo que ya se usaron (para el filtro y el formulario)
    List<String> listarCategorias() throws DAOException;

    // Guarda un insumo nuevo (activo, sin existencia) y devuelve su id
    int insertar(Insumo insumo) throws DAOException;

    // Actualiza los datos del insumo (no toca existencias)
    void actualizar(Insumo insumo) throws DAOException;

    // Cambia solo el costo por unidad del insumo
    void actualizarCosto(int idInsumo, double costoUnitario) throws DAOException;

    // Vuelve a poner el insumo como activo
    void reactivar(int idInsumo) throws DAOException;

    // Baja lógica: EstadoInsumo = 'Inactivo' y sus alertas pendientes quedan atendidas
    void inhabilitar(int idInsumo) throws DAOException;

    // Borra el insumo (solo si no tiene lotes, movimientos, alertas ni recetas)
    void eliminar(int idInsumo) throws DAOException;

    // true si tiene lotes, movimientos, alertas o está en alguna receta
    boolean tieneHistorial(int idInsumo) throws DAOException;

    // true si tiene lotes registrados o está en alguna receta (no se le puede cambiar la unidad)
    boolean tieneLotesORecetas(int idInsumo) throws DAOException;

    // Recetas activas que usan el insumo, como "Bizcocho de chocolate (Sabor: Chocolate)"
    List<String> listarRecetasActivas(int idInsumo) throws DAOException;
}
