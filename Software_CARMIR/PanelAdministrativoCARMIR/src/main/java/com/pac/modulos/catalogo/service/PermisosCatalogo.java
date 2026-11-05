package com.pac.modulos.catalogo.service;

import com.pac.modulos.usuarios.model.Permisos;
import com.pac.session.SesionUsuario;

/**
 * Quién puede cambiar qué dentro del Catálogo de Personalización.
 *
 * - Para entrar al apartado se necesita el permiso "Gestionar catálogo" (lo revisa el Navegador).
 * - Categorías, características, opciones y exclusiones: solo la Administradora las cambia;
 *   el Personal las consulta (los casos de uso dicen "Actor: Administradora").
 * - Productos: la Administradora o el Personal con el permiso "Gestionar productos".
 */
public class PermisosCatalogo {

    public static final String MSG_SOLO_CONSULTA = "Acceso denegado: tu usuario solo puede consultar esta información";

    // Clase de apoyo: no se crean objetos de ella
    private PermisosCatalogo() {
    }

    // true si puede registrar, modificar o dar de baja categorías, características, opciones y exclusiones
    public static boolean puedeEditarCatalogo() {
        return SesionUsuario.getInstancia().esAdministradora();
    }

    // true si puede registrar, modificar o dar de baja productos
    public static boolean puedeEditarProductos() {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (sesion.esAdministradora()) {
            return true;
        }
        return sesion.tienePermiso(Permisos.GESTIONAR_PRODUCTOS);
    }
}
