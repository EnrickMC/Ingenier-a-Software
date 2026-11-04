package com.pac.modulos.reportes.service;

import com.pac.modulos.usuarios.model.Permisos;
import com.pac.modulos.usuarios.service.ControlAcceso;
import com.pac.modulos.usuarios.service.ResultadoAcceso;
import com.pac.session.SesionUsuario;

/**
 * Quién puede ver los reportes financieros (Módulo 6).
 * Se necesita el permiso "Ver datos financieros" (la Administradora siempre puede).
 * Antes de generar o exportar un reporte se vuelve a revisar en la base de datos
 * que la sesión siga activa y que el permiso no se haya quitado (paso 2 de RF-65, RF-66 y RF-67).
 */
public class PermisosReportes {

    // Clase de apoyo: no se crean objetos de ella
    private PermisosReportes() {
    }

    // true si la sesión tiene el permiso (con los datos ya cargados en memoria)
    public static boolean puedeVerReportes() {
        return SesionUsuario.getInstancia().tienePermiso(Permisos.VER_DATOS_FINANCIEROS);
    }

    // Vuelve a revisar la sesión y el permiso contra la base de datos
    // RF-65 / RF-66 / RF-67 · Paso 2: valida la sesión y el permiso para ver datos financieros.
    public static ResultadoAcceso revisarAcceso() {
        ResultadoAcceso acceso = ControlAcceso.revisar(Permisos.VER_DATOS_FINANCIEROS);
        // Si los permisos cambiaron, la sesión ya trae los nuevos: se revisa otra vez en memoria
        if (acceso == ResultadoAcceso.PERMISOS_CAMBIARON) {
            if (puedeVerReportes()) {
                return ResultadoAcceso.PERMITIDO;
            }
            return ResultadoAcceso.DENEGADO;
        }
        return acceso;
    }
}
