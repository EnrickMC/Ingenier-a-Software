package com.pac.dao;

import com.pac.modulos.catalogo.dao.CaracteristicaDAO;
import com.pac.modulos.catalogo.dao.CategoriaDAO;
import com.pac.modulos.catalogo.dao.ExclusionDAO;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.dao.ProductoDAO;
import com.pac.modulos.catalogo.dao.mysql.CaracteristicaDAOMySQL;
import com.pac.modulos.catalogo.dao.mysql.CategoriaDAOMySQL;
import com.pac.modulos.catalogo.dao.mysql.ExclusionDAOMySQL;
import com.pac.modulos.catalogo.dao.mysql.OpcionDAOMySQL;
import com.pac.modulos.catalogo.dao.mysql.ProductoDAOMySQL;
import com.pac.modulos.configuracion.dao.CoberturaDAO;
import com.pac.modulos.configuracion.dao.ConfiguracionDAO;
import com.pac.modulos.configuracion.dao.IntegracionesDAO;
import com.pac.modulos.configuracion.dao.mysql.CoberturaDAOMySQL;
import com.pac.modulos.configuracion.dao.mysql.ConfiguracionDAOMySQL;
import com.pac.modulos.configuracion.dao.mysql.IntegracionesDAOMySQL;
import com.pac.modulos.inventario.dao.AlertaDAO;
import com.pac.modulos.inventario.dao.ConsumoPedidoDAO;
import com.pac.modulos.inventario.dao.InsumoDAO;
import com.pac.modulos.inventario.dao.LoteDAO;
import com.pac.modulos.inventario.dao.MovimientoDAO;
import com.pac.modulos.inventario.dao.RecetaDAO;
import com.pac.modulos.inventario.dao.mysql.AlertaDAOMySQL;
import com.pac.modulos.inventario.dao.mysql.ConsumoPedidoDAOMySQL;
import com.pac.modulos.inventario.dao.mysql.InsumoDAOMySQL;
import com.pac.modulos.inventario.dao.mysql.LoteDAOMySQL;
import com.pac.modulos.inventario.dao.mysql.MovimientoDAOMySQL;
import com.pac.modulos.inventario.dao.mysql.RecetaDAOMySQL;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.ClienteDAO;
import com.pac.modulos.pedidos.dao.NotificacionDAO;
import com.pac.modulos.pedidos.dao.PagoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.dao.mysql.BitacoraPedidoDAOMySQL;
import com.pac.modulos.pedidos.dao.mysql.ClienteDAOMySQL;
import com.pac.modulos.pedidos.dao.mysql.NotificacionDAOMySQL;
import com.pac.modulos.pedidos.dao.mysql.PagoDAOMySQL;
import com.pac.modulos.pedidos.dao.mysql.PedidoDAOMySQL;
import com.pac.modulos.reportes.dao.ReporteDAO;
import com.pac.modulos.reportes.dao.mysql.ReporteDAOMySQL;
import com.pac.modulos.usuarios.dao.PermisoDAO;
import com.pac.modulos.usuarios.dao.UsuarioDAO;
import com.pac.modulos.usuarios.dao.mysql.PermisoDAOMySQL;
import com.pac.modulos.usuarios.dao.mysql.UsuarioDAOMySQL;

/**
 * Lugar único donde se crean los DAO de todos los módulos.
 * Si algún día se cambia de MySQL a otro motor, solo se modifica esta clase.
 */
public class DAOFactory {

    // ----- Módulo 3: Catálogo de Personalización -----

    public static CategoriaDAO crearCategoriaDAO() {
        return new CategoriaDAOMySQL();
    }

    public static CaracteristicaDAO crearCaracteristicaDAO() {
        return new CaracteristicaDAOMySQL();
    }

    public static OpcionDAO crearOpcionDAO() {
        return new OpcionDAOMySQL();
    }

    public static ExclusionDAO crearExclusionDAO() {
        return new ExclusionDAOMySQL();
    }

    public static ProductoDAO crearProductoDAO() {
        return new ProductoDAOMySQL(crearOpcionDAO());
    }

    // ----- Módulo 4: Gestor de Pedidos y Producción -----

    public static PedidoDAO crearPedidoDAO() {
        return new PedidoDAOMySQL();
    }

    public static ClienteDAO crearClienteDAO() {
        return new ClienteDAOMySQL();
    }

    public static PagoDAO crearPagoDAO() {
        return new PagoDAOMySQL();
    }

    public static BitacoraPedidoDAO crearBitacoraPedidoDAO() {
        return new BitacoraPedidoDAOMySQL();
    }

    public static NotificacionDAO crearNotificacionDAO() {
        return new NotificacionDAOMySQL();
    }

    // ----- Módulo 5: Inventario y Recetas -----

    public static InsumoDAO crearInsumoDAO() {
        return new InsumoDAOMySQL();
    }

    public static LoteDAO crearLoteDAO() {
        return new LoteDAOMySQL();
    }

    public static MovimientoDAO crearMovimientoDAO() {
        return new MovimientoDAOMySQL();
    }

    public static AlertaDAO crearAlertaDAO() {
        return new AlertaDAOMySQL();
    }

    public static RecetaDAO crearRecetaDAO() {
        return new RecetaDAOMySQL();
    }

    public static ConsumoPedidoDAO crearConsumoPedidoDAO() {
        return new ConsumoPedidoDAOMySQL();
    }

    // ----- Módulo 6: Reportes Financieros -----

    public static ReporteDAO crearReporteDAO() {
        return new ReporteDAOMySQL();
    }

    // ----- Módulo 7: Gestión de Usuarios -----

    public static UsuarioDAO crearUsuarioDAO() {
        return new UsuarioDAOMySQL(crearPermisoDAO());
    }

    public static PermisoDAO crearPermisoDAO() {
        return new PermisoDAOMySQL();
    }

    // ----- Apartado Configuración -----

    public static ConfiguracionDAO crearConfiguracionDAO() {
        return new ConfiguracionDAOMySQL();
    }

    public static CoberturaDAO crearCoberturaDAO() {
        return new CoberturaDAOMySQL();
    }

    // Datos de WhatsApp y correo (tabla ConfiguracionNotificacion)
    public static IntegracionesDAO crearIntegracionesDAO() {
        return new IntegracionesDAOMySQL();
    }
}
