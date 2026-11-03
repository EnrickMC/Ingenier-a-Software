package com.pac.modulos.catalogo.dao;

import com.pac.dao.DAOException;
import com.pac.modulos.catalogo.model.Producto;

import java.util.List;

/**
 * Operaciones sobre las tablas Postre y PostreOpcion.
 * Aquí solo se manejan los postres del catálogo (los que tienen NombrePostre);
 * los postres que arma el cliente desde la web no tienen nombre y no se listan.
 * La clase ProductoDAOMySQL es la que las implementa.
 */
public interface ProductoDAO {

    int VISIBILIDAD_TODOS = 0;
    int VISIBILIDAD_EN_TIENDA = 1;
    int VISIBILIDAD_OCULTOS = 2;

    // Busca productos por nombre o SKU. idCategoria 0 = todas. visibilidad: constantes de arriba
    List<Producto> buscar(String texto, int idCategoria, int visibilidad) throws DAOException;

    // Lee un producto con sus opciones; devuelve null si no existe
    Producto buscarPorId(int idPostre) throws DAOException;

    // Busca un producto del catálogo por su nombre; null si no existe
    Producto buscarPorNombre(String nombre) throws DAOException;

    // Busca cualquier postre (con o sin nombre) con esa ClaveConfiguracion; null si no existe
    Producto buscarPorClave(String claveConfiguracion) throws DAOException;

    // Guarda Postre y PostreOpcion en una transacción y devuelve el id nuevo
    int insertar(Producto producto) throws DAOException;

    // Actualiza el postre; si cambiarOpciones es true también reemplaza sus PostreOpcion
    void actualizar(Producto producto, boolean cambiarOpciones) throws DAOException;

    // true si el postre aparece en algún pedido (DetallePedido)
    boolean tienePedidos(int idPostre) throws DAOException;

    // Folios de pedidos vigentes (no entregados ni cancelados) que incluyen el postre
    List<Integer> listarFoliosVigentes(int idPostre) throws DAOException;

    // Baja lógica: deja de mostrarse en la tienda (MostrarEnTienda = 0)
    void ocultar(int idPostre) throws DAOException;

    // Borra el postre (sus PostreOpcion se borran en cascada)
    void eliminar(int idPostre) throws DAOException;
}
