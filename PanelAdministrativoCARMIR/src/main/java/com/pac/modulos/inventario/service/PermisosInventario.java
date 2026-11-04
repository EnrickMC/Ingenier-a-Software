package com.pac.modulos.inventario.service;

import com.pac.modulos.usuarios.model.Permisos;
import com.pac.session.SesionUsuario;

/**
 * Quién puede cambiar qué dentro de Inventario y Recetas.
 *
 * - Para entrar a "Inventario de Insumos" se necesita "Gestionar insumos" y para entrar a
 *   "Recetas" se necesita "Gestionar recetas" (lo revisa el Navegador).
 * - Alta, modificación y baja de insumos: solo la Administradora (RF-52, RF-53, RF-54).
 * - Entradas, mermas y alertas: la Administradora o el Personal con "Gestionar insumos" (RF-59, RF-60).
 * - Alta, modificación y baja de recetas: solo la Administradora (RF-56, RF-57, RF-58);
 *   el Personal las consulta (RF-55).
 */
public class PermisosInventario {

    public static final String MSG_SOLO_CONSULTA = "Acceso denegado: tu usuario solo puede consultar esta información";

    // Clase de apoyo: no se crean objetos de ella
    private PermisosInventario() {
    }

    // true si puede registrar, modificar o dar de baja insumos
    public static boolean puedeAdministrarInsumos() {
        return SesionUsuario.getInstancia().esAdministradora();
    }

    // true si puede registrar entradas y mermas y atender alertas
    public static boolean puedeRegistrarMovimientos() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.GESTIONAR_INSUMOS);
    }

    // true si puede entrar al apartado Recetas (lo usa el catálogo para mostrar "Ver receta")
    public static boolean puedeVerRecetas() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.GESTIONAR_RECETAS);
    }

    // true si puede registrar, modificar o dar de baja recetas
    public static boolean puedeEditarRecetas() {
        return SesionUsuario.getInstancia().esAdministradora();
    }

    // Id del usuario que se guarda en los movimientos: 0 si es la Administradora (no está en la BD)
    public static int idUsuarioSesion() {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (!sesion.estaActiva() || sesion.esAdministradora()) {
            return 0;
        }
        return sesion.getUsuario().getIdUsuario();
    }
}
