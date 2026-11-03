package com.pac.modulos.configuracion.service;

import com.pac.modulos.usuarios.model.Permisos;
import com.pac.session.SesionUsuario;

/**
 * Quién puede hacer qué en el apartado Configuración.
 * - Entrar, cambiar parámetros, cobertura y generar respaldos: permiso "Configurar sistema"
 *   (la Administradora siempre puede; lo revisa el Navegador al abrir el apartado).
 * - Restaurar un respaldo (reemplaza TODA la base de datos): solo la Administradora.
 */
public class PermisosConfiguracion {

    public static final String MSG_SOLO_ADMINISTRADORA = "Solo la administradora puede restaurar un respaldo";
    public static final String MSG_SIN_PERMISO = "Acceso denegado: necesitas el permiso «Configurar sistema»";

    // Clase de apoyo: no se crean objetos de ella
    private PermisosConfiguracion() {
    }

    public static boolean puedeConfigurar() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.CONFIGURAR_SISTEMA);
    }

    public static boolean puedeRestaurar() {
        return SesionUsuario.getInstancia().esAdministradora();
    }

    // Id del usuario que se guarda en la bitácora: 0 si es la Administradora (no está en la BD)
    public static int idUsuarioSesion() {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (!sesion.estaActiva() || sesion.esAdministradora()) {
            return 0;
        }
        return sesion.getUsuario().getIdUsuario();
    }
}
