package com.pac.modulos.catalogo.dao.mysql;

import com.pac.dao.DAOException;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.dao.ProductoDAO;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de ProductoDAO para MySQL (tablas Postre y PostreOpcion).
 * El costo y el precio salen de la vista vw_CostoPrecioPostre.
 */
public class ProductoDAOMySQL implements ProductoDAO {

    private static final String SELECT_PRODUCTO =
            "SELECT p.IdPostre, p.IdCategoria, p.ClaveConfiguracion, p.NombrePostre, p.DescripcionPostre, "
            + "p.LinkFotografia, p.MostrarEnTienda, cat.NombreCategoria, cat.EstadoCategoria, "
            + "v.CostoIngredientes, v.PrecioVenta "
            + "FROM Postre p "
            + "JOIN CategoriaPostre cat ON cat.IdCategoria = p.IdCategoria "
            + "LEFT JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre";

    private OpcionDAO opcionDAO;

    public ProductoDAOMySQL(OpcionDAO opcionDAO) {
        this.opcionDAO = opcionDAO;
    }

    // ===================== Consultas =====================

    @Override
    public List<Producto> buscar(String texto, int idCategoria, int visibilidad) throws DAOException {
        // El SKU no es columna: se arma igual que en Producto.getSku()
        String sql = SELECT_PRODUCTO + " WHERE p.NombrePostre IS NOT NULL "
                + "AND (p.NombrePostre LIKE ? OR CONCAT('PST-', LPAD(p.IdPostre, 4, '0')) LIKE ?)";
        if (idCategoria > 0) {
            sql = sql + " AND p.IdCategoria = ?";
        }
        if (visibilidad == VISIBILIDAD_EN_TIENDA) {
            sql = sql + " AND p.MostrarEnTienda = 1";
        } else if (visibilidad == VISIBILIDAD_OCULTOS) {
            sql = sql + " AND p.MostrarEnTienda = 0";
        }
        sql = sql + " ORDER BY p.NombrePostre";

        List<Producto> productos = new ArrayList<Producto>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            consulta.setString(1, ApoyoMySQL.patron(texto));
            consulta.setString(2, ApoyoMySQL.patron(texto));
            if (idCategoria > 0) {
                consulta.setInt(3, idCategoria);
            }
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                productos.add(leerProducto(resultado));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los productos", e);
        }
        return productos;
    }

    @Override
    public Producto buscarPorId(int idPostre) throws DAOException {
        Producto producto = buscarUno(SELECT_PRODUCTO + " WHERE p.IdPostre = ?", String.valueOf(idPostre), true);
        if (producto != null) {
            cargarOpciones(producto);
        }
        return producto;
    }

    @Override
    public Producto buscarPorNombre(String nombre) throws DAOException {
        return buscarUno(SELECT_PRODUCTO + " WHERE p.NombrePostre = ?", nombre.trim(), false);
    }

    @Override
    public Producto buscarPorClave(String claveConfiguracion) throws DAOException {
        return buscarUno(SELECT_PRODUCTO + " WHERE p.ClaveConfiguracion = ?", claveConfiguracion, false);
    }

    @Override
    public boolean tienePedidos(int idPostre) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            int pedidos = ApoyoMySQL.contar(conexion, "SELECT COUNT(*) FROM DetallePedido WHERE IdPostre = ?", idPostre);
            return pedidos > 0;
        } catch (SQLException e) {
            throw new DAOException("Error al revisar los pedidos del producto " + idPostre, e);
        }
    }

    @Override
    public List<Integer> listarFoliosVigentes(int idPostre) throws DAOException {
        String sql = "SELECT DISTINCT p.Folio FROM Pedido p JOIN DetallePedido d ON d.Folio = p.Folio "
                + "WHERE d.IdPostre = ? AND " + ApoyoMySQL.CONDICION_PEDIDO_VIGENTE + " ORDER BY p.Folio";
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            return ApoyoMySQL.leerEnteros(conexion, sql, idPostre);
        } catch (SQLException e) {
            throw new DAOException("Error al consultar los pedidos del producto " + idPostre, e);
        }
    }

    // ===================== Altas y cambios =====================

    @Override
    public int insertar(Producto producto) throws DAOException {
        String sql = "INSERT INTO Postre (IdCategoria, ClaveConfiguracion, NombrePostre, DescripcionPostre, "
                + "LinkFotografia, MostrarEnTienda) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            // 1. Se guarda el postre
            PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            llenarDatos(consulta, producto);
            consulta.executeUpdate();

            int idNuevo = 0;
            ResultSet llaves = consulta.getGeneratedKeys();
            if (llaves.next()) {
                idNuevo = llaves.getInt(1);
            }
            llaves.close();
            consulta.close();

            // 2. Se guardan las opciones que lo forman
            guardarOpciones(conexion, idNuevo, producto.getOpciones());

            conexion.commit();
            producto.setIdPostre(idNuevo);
            return idNuevo;
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al registrar el producto " + producto.getNombre(), e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void actualizar(Producto producto, boolean cambiarOpciones) throws DAOException {
        String sql = "UPDATE Postre SET IdCategoria = ?, ClaveConfiguracion = ?, NombrePostre = ?, "
                + "DescripcionPostre = ?, LinkFotografia = ?, MostrarEnTienda = ? WHERE IdPostre = ?";
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);

            PreparedStatement consulta = conexion.prepareStatement(sql);
            llenarDatos(consulta, producto);
            consulta.setInt(7, producto.getIdPostre());
            consulta.executeUpdate();
            consulta.close();

            if (cambiarOpciones) {
                ApoyoMySQL.ejecutar(conexion, "DELETE FROM PostreOpcion WHERE IdPostre = ?", producto.getIdPostre());
                guardarOpciones(conexion, producto.getIdPostre(), producto.getOpciones());
            }

            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            throw new DAOException("Error al actualizar el producto " + producto.getNombre(), e);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
    }

    @Override
    public void ocultar(int idPostre) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            ApoyoMySQL.ejecutar(conexion, "UPDATE Postre SET MostrarEnTienda = 0 WHERE IdPostre = ?", idPostre);
        } catch (SQLException e) {
            throw new DAOException("Error al ocultar el producto " + idPostre, e);
        }
    }

    @Override
    public void eliminar(int idPostre) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            // PostreOpcion tiene ON DELETE CASCADE: sus filas se borran solas
            ApoyoMySQL.ejecutar(conexion, "DELETE FROM Postre WHERE IdPostre = ?", idPostre);
        } catch (SQLException e) {
            throw new DAOException("Error al eliminar el producto " + idPostre, e);
        }
    }

    // ===================== Métodos de apoyo =====================

    // Llena los 6 datos del postre (posiciones 1 a 6)
    private void llenarDatos(PreparedStatement consulta, Producto producto) throws SQLException {
        consulta.setInt(1, producto.getIdCategoria());
        consulta.setString(2, producto.getClaveConfiguracion());
        consulta.setString(3, ApoyoMySQL.textoONulo(producto.getNombre()));
        consulta.setString(4, ApoyoMySQL.textoONulo(producto.getDescripcion()));
        consulta.setString(5, ApoyoMySQL.textoONulo(producto.getLinkFotografia()));
        consulta.setBoolean(6, producto.isMostrarEnTienda());
    }

    // Guarda una fila de PostreOpcion por cada opción elegida
    private void guardarOpciones(Connection conexion, int idPostre, List<OpcionElegida> opciones) throws SQLException {
        PreparedStatement consulta = conexion.prepareStatement(
                "INSERT INTO PostreOpcion (IdPostre, IdOpcion, CantidadElegida) VALUES (?, ?, ?)");
        for (OpcionElegida elegida : opciones) {
            consulta.setInt(1, idPostre);
            consulta.setInt(2, elegida.getOpcion().getIdOpcion());
            consulta.setInt(3, elegida.getCantidad());
            consulta.executeUpdate();
        }
        consulta.close();
    }

    // Lee las filas de PostreOpcion y les carga los datos de cada opción
    private void cargarOpciones(Producto producto) throws DAOException {
        List<Integer> ids = new ArrayList<Integer>();
        List<Integer> cantidades = new ArrayList<Integer>();
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT IdOpcion, CantidadElegida FROM PostreOpcion WHERE IdPostre = ?");
            consulta.setInt(1, producto.getIdPostre());
            ResultSet resultado = consulta.executeQuery();
            while (resultado.next()) {
                ids.add(resultado.getInt("IdOpcion"));
                cantidades.add(resultado.getInt("CantidadElegida"));
            }
            resultado.close();
            consulta.close();
        } catch (SQLException e) {
            throw new DAOException("Error al consultar las opciones del producto " + producto.getIdPostre(), e);
        }

        List<OpcionElegida> opciones = new ArrayList<OpcionElegida>();
        for (int i = 0; i < ids.size(); i++) {
            Opcion opcion = opcionDAO.buscarPorId(ids.get(i));
            if (opcion != null) {
                opciones.add(new OpcionElegida(opcion, cantidades.get(i)));
            }
        }
        producto.setOpciones(opciones);
    }

    // Ejecuta una consulta con un solo parámetro que devuelve a lo más un producto
    private Producto buscarUno(String sql, String valor, boolean esNumero) throws DAOException {
        try {
            Connection conexion = ConexionBD.getInstancia().getConexion();
            PreparedStatement consulta = conexion.prepareStatement(sql);
            if (esNumero) {
                consulta.setInt(1, Integer.parseInt(valor));
            } else {
                consulta.setString(1, valor);
            }
            ResultSet resultado = consulta.executeQuery();
            Producto producto = null;
            if (resultado.next()) {
                producto = leerProducto(resultado);
            }
            resultado.close();
            consulta.close();
            return producto;
        } catch (SQLException e) {
            throw new DAOException("Error al buscar el producto", e);
        }
    }

    // Convierte la fila actual del ResultSet en un objeto Producto (sin sus opciones)
    private Producto leerProducto(ResultSet resultado) throws SQLException {
        Producto p = new Producto();
        p.setIdPostre(resultado.getInt("IdPostre"));
        p.setIdCategoria(resultado.getInt("IdCategoria"));
        p.setClaveConfiguracion(resultado.getString("ClaveConfiguracion"));
        p.setNombre(resultado.getString("NombrePostre"));
        p.setDescripcion(resultado.getString("DescripcionPostre"));
        p.setLinkFotografia(resultado.getString("LinkFotografia"));
        p.setMostrarEnTienda(resultado.getBoolean("MostrarEnTienda"));
        p.setNombreCategoria(resultado.getString("NombreCategoria"));
        p.setCategoriaActiva("Activa".equalsIgnoreCase(resultado.getString("EstadoCategoria")));
        // Si el postre no tiene opciones con receta, la vista devuelve NULL y queda en 0
        p.setCostoIngredientes(resultado.getDouble("CostoIngredientes"));
        p.setPrecioVenta(resultado.getDouble("PrecioVenta"));
        return p;
    }
}
