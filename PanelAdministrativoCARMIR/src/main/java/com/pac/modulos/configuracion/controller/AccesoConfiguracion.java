package com.pac.modulos.configuracion.controller;

import com.pac.modulos.usuarios.model.Permisos;
import com.pac.modulos.usuarios.service.ControlAcceso;
import com.pac.modulos.usuarios.service.ResultadoAcceso;
import com.pac.navigation.Navegador;
import com.pac.navigation.Vista;
import com.pac.session.SesionUsuario;
import com.pac.util.Alertas;

/**
 * Antes de guardar algo en Configuración se revisa otra vez que la sesión siga activa
 * y que tenga el permiso «Configurar sistema» (pudo cambiar con la pantalla abierta).
 */
public class AccesoConfiguracion {

    // Clase de apoyo: no se crean objetos de ella
    private AccesoConfiguracion() {
    }

    // true si puede continuar; si no, avisa (y regresa al inicio de sesión si la sesión terminó)
    public static boolean revisar() {
        ResultadoAcceso acceso = ControlAcceso.revisar(Permisos.CONFIGURAR_SISTEMA);
        if (acceso == ResultadoAcceso.PERMITIDO) {
            return true;
        }
        if (acceso == ResultadoAcceso.SESION_VENCIDA || acceso == ResultadoAcceso.CUENTA_INACTIVA) {
            String mensaje = ControlAcceso.MSG_SESION_VENCIDA;
            if (acceso == ResultadoAcceso.CUENTA_INACTIVA) {
                mensaje = ControlAcceso.MSG_CUENTA_INACTIVA;
            }
            SesionUsuario.getInstancia().cerrar();
            Alertas.aviso(mensaje);
            Navegador.getInstancia().mostrar(Vista.LOGIN_PERSONAL);
            return false;
        }
        if (acceso == ResultadoAcceso.PERMISOS_CAMBIARON) {
            Alertas.aviso(ControlAcceso.MSG_PERMISOS_CAMBIARON);
            Navegador.getInstancia().mostrar(Vista.PANEL_PRINCIPAL);
            return false;
        }
        if (acceso == ResultadoAcceso.SIN_VERIFICAR) {
            Alertas.error(ControlAcceso.MSG_SIN_VERIFICAR);
            return false;
        }
        Alertas.error(ControlAcceso.MSG_ACCESO_DENEGADO);
        return false;
    }
}
