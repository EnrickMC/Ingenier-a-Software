package com.pac.modulos.usuarios.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.usuarios.dao.UsuarioDAO;
import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.session.SesionUsuario;
import com.pac.util.ConfiguracionApp;

import java.util.List;

/**
 * Control de acceso a los apartados del panel (RNF-15).
 *
 * Los permisos se vuelven a leer de la base de datos cada vez que el Personal
 * abre un apartado, no solo al iniciar sesión. Así los cambios que haga la
 * administradora en "Usuarios y Permisos" se aplican de inmediato.
 */
public class ControlAcceso {

    public static final String MSG_ACCESO_DENEGADO = "Acceso denegado";
    public static final String MSG_SIN_VERIFICAR = "No fue posible verificar tus permisos";
    public static final String MSG_PERMISOS_CAMBIARON = "Tus permisos cambiaron";
    public static final String MSG_CUENTA_INACTIVA = "Tu cuenta está inactiva, comunícate con la administradora";
    public static final String MSG_SESION_VENCIDA = "Tu sesión terminó. Vuelve a iniciar sesión";

    // Revisa si la persona de la sesión puede abrir un apartado con ese permiso
    // RNF-15 · Paso: revisa si la sesión sigue activa y si tiene el permiso del apartado.
    public static ResultadoAcceso revisar(String permisoRequerido) {
        SesionUsuario sesion = SesionUsuario.getInstancia();

        // RNF-15 · Excepción: la sesión ya no está activa.
        if (!sesion.estaActiva()) {
            return ResultadoAcceso.SESION_VENCIDA;
        }
        // Apartados libres (por ejemplo el Dashboard)
        if (permisoRequerido == null) {
            return ResultadoAcceso.PERMITIDO;
        }
        // La Administradora entra a todo
        if (sesion.esAdministradora()) {
            return ResultadoAcceso.PERMITIDO;
        }
        // En modo demostración no se consulta MySQL
        if (ConfiguracionApp.getInstancia().isModoDemo()) {
            return revisarEnMemoria(sesion, permisoRequerido);
        }

        // Se vuelven a leer el estado y los permisos del usuario
        Usuario usuarioSesion = sesion.getUsuario();
        Usuario usuarioBD;
        try {
            UsuarioDAO usuarioDAO = DAOFactory.crearUsuarioDAO();
            usuarioBD = usuarioDAO.buscarPorId(usuarioSesion.getIdUsuario());
        } catch (DAOException e) {
            // RNF-15 · Excepción: sin conexión no se pueden verificar los permisos.
            return ResultadoAcceso.SIN_VERIFICAR;
        }

        // RNF-15 · Excepción: la cuenta fue dada de baja o inactivada.
        if (usuarioBD == null || usuarioBD.getEstado() != EstadoUsuario.ACTIVO) {
            return ResultadoAcceso.CUENTA_INACTIVA;
        }

        // Si los permisos cambiaron, la sesión se actualiza y se avisa
        // RNF-15 · Excepción: los permisos cambiaron mientras la sesión estaba abierta.
        if (!mismosPermisos(usuarioSesion.getPermisos(), usuarioBD.getPermisos())) {
            usuarioSesion.reemplazarPermisos(usuarioBD.getPermisos());
            return ResultadoAcceso.PERMISOS_CAMBIARON;
        }
        return revisarEnMemoria(sesion, permisoRequerido);
    }

    // Revisa con los permisos que ya están cargados en la sesión
    private static ResultadoAcceso revisarEnMemoria(SesionUsuario sesion, String permisoRequerido) {
        if (sesion.tienePermiso(permisoRequerido)) {
            return ResultadoAcceso.PERMITIDO;
        }
        // RNF-15 · Excepción: no tiene el permiso, «Acceso denegado».
        return ResultadoAcceso.DENEGADO;
    }

    // true si las dos listas tienen exactamente los mismos permisos
    private static boolean mismosPermisos(List<Permiso> lista1, List<Permiso> lista2) {
        if (lista1.size() != lista2.size()) {
            return false;
        }
        for (Permiso permiso : lista1) {
            boolean encontrado = false;
            for (Permiso otro : lista2) {
                if (otro.getIdPermiso() == permiso.getIdPermiso()) {
                    encontrado = true;
                }
            }
            if (!encontrado) {
                return false;
            }
        }
        return true;
    }
}
