package com.pac.modulos.usuarios.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.usuarios.dao.UsuarioDAO;
import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Permisos;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.util.ConfiguracionApp;
import com.pac.session.TipoAcceso;
import com.pac.util.HashContrasena;

/**
 * Lógica del inicio de sesión (RNF-14). Está entre los controladores y los DAO.
 * Después, cada apartado revisa sus permisos con ControlAcceso (RNF-12).
 *
 * En modo demostración (app.modoDemo=true) no consulta MySQL y siempre deja entrar.
 * Con app.modoDemo=false:
 *  - La Administradora se valida con AdministradoraSistema (no está en la BD).
 *  - El Personal se valida contra la tabla Usuario.
 */
public class AutenticacionService {

    // Mensajes del caso de uso RNF-14 (inicio de sesión) / RNF-13
    private static final String MSG_CREDENCIALES = "Usuario o contraseña incorrectos";
    private static final String MSG_INACTIVO = "Tu cuenta está inactiva, comunícate con la administradora";
    private static final String MSG_BLOQUEADO = "Tu cuenta está bloqueada por varios intentos fallidos, comunícate con la administradora";
    private static final String MSG_SIN_PERMISOS = "No tienes módulos asignados, comunícate con la administradora";
    private static final String MSG_SOLO_BLOQUEO = "Esta cuenta solo entra desde la pantalla de bloqueo de la administración";
    private static final String MSG_SIN_CONEXION = "No fue posible iniciar sesión, intenta de nuevo";

    private ConfiguracionApp configuracion;

    public AutenticacionService() {
        configuracion = ConfiguracionApp.getInstancia();
    }

    /**
     * Inicio de sesión único: aquí entran la Administradora y el Personal.
     * Si el usuario es el de la Administradora se valida como ella; con cualquier otro
     * nombre se valida como Personal contra la base de datos. Los permisos no cambian.
     */
    // RNF-14 (Inicio de sesión en el panel administrativo): inicia aquí. Pasos 1 a 3: decide si es la
    // Administradora (credenciales fijas) o el Personal (base de datos).
    public ResultadoAutenticacion iniciarSesion(String nombreUsuario, String contrasena) {
        boolean esAdministradora = AdministradoraSistema.esNombreReservado(nombreUsuario);
        // En modo demostración, dejar el usuario vacío entra como Administradora (como antes)
        boolean sinUsuario = nombreUsuario == null || nombreUsuario.isBlank();
        if (configuracion.isModoDemo() && sinUsuario) {
            esAdministradora = true;
        }
        if (esAdministradora) {
            ResultadoAutenticacion resultado = desbloquearAdministradora(nombreUsuario, contrasena);
            return resultado.conTipoAcceso(TipoAcceso.ADMINISTRADORA);
        }
        ResultadoAutenticacion resultado = iniciarSesionPersonal(nombreUsuario, contrasena);
        return resultado.conTipoAcceso(TipoAcceso.PERSONAL);
    }

    /**
     * Solo entra la Administradora.
     * Sus datos están fijos en AdministradoraSistema; no se consulta la base de datos.
     */
    // RNF-14 · Paso 3: inicio de sesión de la Administradora (credenciales fijas, no está en la BD).
    public ResultadoAutenticacion desbloquearAdministradora(String nombreUsuario, String contrasena) {
        if (configuracion.isModoDemo()) {
            return ResultadoAutenticacion.correcto(AdministradoraSistema.crearUsuario());
        }
        // RNF-14 · Paso 2 y Excepción paso 2: «Escribe tu usuario» o «Escribe tu contraseña».
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return ResultadoAutenticacion.error("Escribe tu usuario");
        }
        if (contrasena == null || contrasena.isEmpty()) {
            return ResultadoAutenticacion.error("Escribe tu contraseña");
        }
        // RNF-14 · Excepción paso 3: «Usuario o contraseña incorrectos».
        if (!AdministradoraSistema.credencialesCorrectas(nombreUsuario, contrasena)) {
            // Los usuarios del Personal no entran por aquí, aunque su contraseña sea correcta
            return ResultadoAutenticacion.error(MSG_CREDENCIALES);
        }
        return ResultadoAutenticacion.correcto(AdministradoraSistema.crearUsuario());
    }

    // Inicio de sesión del Personal con usuario y contraseña (solo usuarios de la BD)
    // RNF-14 · Pasos 2 a 7: inicio de sesión del Personal.
    public ResultadoAutenticacion iniciarSesionPersonal(String nombreUsuario, String contrasena) {
        if (configuracion.isModoDemo()) {
            return crearPersonalDemo(nombreUsuario);
        }
        // RNF-14 · Excepción paso 2: «Escribe tu usuario».
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return ResultadoAutenticacion.error("Escribe tu usuario");
        }
        // La cuenta de la Administradora solo entra por la pantalla de bloqueo
        // RNF-14 · Excepción paso 3: la cuenta de la Administradora solo entra por la pantalla de bloqueo
        // (el inicio de sesión único).
        if (AdministradoraSistema.esNombreReservado(nombreUsuario)) {
            return ResultadoAutenticacion.error(MSG_SOLO_BLOQUEO);
        }
        return validarConBaseDeDatos(nombreUsuario.trim(), contrasena);
    }

    // Busca al usuario en MySQL y revisa su estado, contraseña y permisos
    // RNF-14 · Pasos 4 a 7: valida existencia, estado, contraseña y permisos en la BD.
    private ResultadoAutenticacion validarConBaseDeDatos(String nombreUsuario, String contrasena) {
        // RNF-14 · Excepción paso 2: «Escribe tu contraseña».
        if (contrasena == null || contrasena.isEmpty()) {
            return ResultadoAutenticacion.error("Escribe tu contraseña");
        }
        try {
            // RNF-14 · Paso 4: busca la cuenta en la BD y revisa su estado.
            UsuarioDAO usuarioDAO = DAOFactory.crearUsuarioDAO();
            Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);

            // RNF-14 · Excepción paso 4: el usuario no existe, «Usuario o contraseña incorrectos» (sin decir cuál dato falló).
            if (usuario == null) {
                return ResultadoAutenticacion.error(MSG_CREDENCIALES);
            }

            // RNF-14 · Excepción paso 4: la cuenta está bloqueada por intentos fallidos.
            if (usuario.getEstado() == EstadoUsuario.BLOQUEADO) {
                return ResultadoAutenticacion.error(MSG_BLOQUEADO);
            }
            // RNF-14 · Excepción paso 4: «Tu cuenta está inactiva, comunícate con la administradora».
            if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
                return ResultadoAutenticacion.error(MSG_INACTIVO);
            }

            // RNF-14 · Paso 5: compara la contraseña con la guardada (hash); si no coincide suma un intento fallido.
            if (!HashContrasena.coincide(contrasena, usuario.getContrasenaHash())) {
                usuarioDAO.registrarIntentoFallido(usuario.getIdUsuario());

                // Al llegar al límite de intentos, la cuenta del Personal se bloquea
                int intentos = usuario.getIntentosFallidos() + 1;
                // RNF-14 · Excepción paso 5: al llegar al límite de intentos (IntentosMaximosLogin) se bloquea la cuenta.
                if (intentos >= configuracion.getIntentosMaximosLogin()) {
                    usuarioDAO.cambiarEstado(usuario.getIdUsuario(), EstadoUsuario.BLOQUEADO);
                    return ResultadoAutenticacion.error(MSG_BLOQUEADO);
                }
                return ResultadoAutenticacion.error(MSG_CREDENCIALES);
            }

            // RNF-14 · Paso 6 y Excepción paso 6: «No tienes módulos asignados, comunícate con la administradora».
            if (usuario.getPermisos().isEmpty()) {
                return ResultadoAutenticacion.error(MSG_SIN_PERMISOS);
            }

            // RNF-14 · Paso 7: registra el último acceso y reinicia los intentos fallidos.
            usuarioDAO.registrarAccesoExitoso(usuario.getIdUsuario());
            // RNF-13 (Aislamiento de sesiones simultáneas): pendiente; necesita la tabla SesionUsuario.
            // Pendiente (RNF-13): revisar que el usuario no tenga otra sesión abierta
            return ResultadoAutenticacion.correcto(usuario);
        } catch (DAOException e) {
            // RNF-14 · Excepción paso 4 / RNF-13 · Excepción paso 2: sin conexión, «No fue posible iniciar sesión, intenta de nuevo».
            return ResultadoAutenticacion.error(MSG_SIN_CONEXION);
        }
    }

    // Usuario de prueba para el modo demostración, con algunos permisos de ejemplo
    private ResultadoAutenticacion crearPersonalDemo(String nombreUsuario) {
        String nombre = "personal";
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            nombre = nombreUsuario.trim();
        }
        Usuario personal = new Usuario(2, "Personal (demo)", nombre);
        personal.agregarPermiso(new Permiso(2, Permisos.GESTIONAR_PEDIDOS));
        personal.agregarPermiso(new Permiso(3, Permisos.VER_AGENDA));
        personal.agregarPermiso(new Permiso(7, Permisos.GESTIONAR_INSUMOS));
        personal.agregarPermiso(new Permiso(8, Permisos.GESTIONAR_RECETAS));
        return ResultadoAutenticacion.correcto(personal);
    }
}
