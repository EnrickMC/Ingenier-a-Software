package com.pac.modulos.pedidos.service;

import com.pac.modulos.usuarios.model.Permisos;
import com.pac.session.SesionUsuario;

/**
 * Quién puede hacer qué dentro del Gestor de Pedidos y Producción.
 *
 * - "Gestión de Pedidos" pide "Gestionar pedidos": ver, aprobar, cancelar y avanzar el estado (RF-37 a RF-41).
 * - "Agenda de Producción" pide "Ver agenda de producción" (RF-45, RF-46).
 * - "Validación de Pagos" pide "Validar pagos" (RF-42, RF-43). Desde el detalle de un pedido
 *   también se registran pagos si el usuario tiene ese permiso.
 * - Historial completo con montos (RF-36), modificar y dar de baja pedidos: solo la Administradora.
 */
public class PermisosPedidos {

    public static final String MSG_SOLO_ADMINISTRADORA = "Acceso denegado: solo la administradora puede hacer esta acción";
    public static final String MSG_SIN_PERMISO = "Acceso denegado";

    // Clase de apoyo: no se crean objetos de ella
    private PermisosPedidos() {
    }

    // true si puede aprobar, cancelar y avanzar pedidos
    public static boolean puedeGestionar() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.GESTIONAR_PEDIDOS);
    }

    // true si puede registrar, validar y rechazar pagos
    public static boolean puedeValidarPagos() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.VALIDAR_PAGOS);
    }

    // true si puede ver la agenda de producción
    public static boolean puedeVerAgenda() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.VER_AGENDA);
    }

    // Historial completo, modificar y dar de baja: solo la Administradora
    public static boolean esAdministradora() {
        return SesionUsuario.getInstancia().esAdministradora();
    }

    // Id que se guarda como "quién lo hizo": 0 si es la Administradora (se guarda NULL)
    public static int idUsuarioSesion() {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        if (!sesion.estaActiva() || sesion.esAdministradora()) {
            return 0;
        }
        return sesion.getUsuario().getIdUsuario();
    }
}
