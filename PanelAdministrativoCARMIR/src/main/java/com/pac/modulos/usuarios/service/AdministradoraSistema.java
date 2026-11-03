package com.pac.modulos.usuarios.service;

import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Permisos;
import com.pac.modulos.usuarios.model.Usuario;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Cuenta de la Administradora, fija dentro del sistema.
 *
 * - NO está en la base de datos: no aparece en "Usuarios y Permisos" y no se puede
 *   modificar ni dar de baja desde el panel.
 * - Solo entra por la pantalla de bloqueo.
 * - Tiene todos los permisos.
 *
 * Para cambiar el usuario o la contraseña, edita las dos constantes de abajo
 * y vuelve a compilar.
 */
public class AdministradoraSistema {

    // ===== Datos de acceso de la Administradora (editar aquí) =====
    private static final String USUARIO = "admin";
    private static final String CONTRASENA = "admin";

    private static final String NOMBRE_COMPLETO = "Administradora";

    // Id que usa en la sesión. Ningún usuario de la BD tiene id 0 (AUTO_INCREMENT empieza en 1)
    public static final int ID_ADMINISTRADORA = 0;

    // Clase de utilidades: no se crean objetos de ella
    private AdministradoraSistema() {
    }

    // true si el usuario y la contraseña son los de la Administradora
    public static boolean credencialesCorrectas(String usuario, String contrasena) {
        if (usuario == null || contrasena == null) {
            return false;
        }
        boolean usuarioCorrecto = esNombreReservado(usuario);
        // MessageDigest.isEqual compara sin dar pistas por el tiempo que tarda
        boolean contrasenaCorrecta = MessageDigest.isEqual(
                contrasena.getBytes(StandardCharsets.UTF_8),
                CONTRASENA.getBytes(StandardCharsets.UTF_8));
        return usuarioCorrecto && contrasenaCorrecta;
    }

    // true si el nombre es el de la Administradora (nadie más lo puede usar)
    public static boolean esNombreReservado(String nombreUsuario) {
        if (nombreUsuario == null) {
            return false;
        }
        return nombreUsuario.trim().equalsIgnoreCase(USUARIO);
    }

    // Nombre de usuario de la Administradora (para filtrar la lista de usuarios)
    public static String getNombreUsuario() {
        return USUARIO;
    }

    // Crea el objeto Usuario que se guarda en la sesión, con todos los permisos
    public static Usuario crearUsuario() {
        Usuario administradora = new Usuario(ID_ADMINISTRADORA, NOMBRE_COMPLETO, USUARIO);
        for (int i = 0; i < Permisos.TODOS.length; i++) {
            administradora.agregarPermiso(new Permiso(0, Permisos.TODOS[i]));
        }
        return administradora;
    }
}
