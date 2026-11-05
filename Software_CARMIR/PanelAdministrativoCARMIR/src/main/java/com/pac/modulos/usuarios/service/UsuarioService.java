package com.pac.modulos.usuarios.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.usuarios.dao.PermisoDAO;
import com.pac.modulos.usuarios.dao.UsuarioDAO;
import com.pac.modulos.usuarios.model.EstadoUsuario;
import com.pac.modulos.usuarios.model.Permiso;
import com.pac.modulos.usuarios.model.Permisos;
import com.pac.modulos.usuarios.model.Usuario;
import com.pac.session.SesionUsuario;
import com.pac.util.HashContrasena;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas del Módulo 7 (Gestión de Usuarios): registrar, consultar, modificar
 * y dar de baja usuarios del personal.
 *
 * Los mensajes son los de los casos de uso RNF-16, RNF-17, RNF-18 y RNF-19
 * del archivo "Requerimientos del sistema CARMIR.json".
 */
public class UsuarioService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_ENCONTRADO = "Usuario no encontrado";
    public static final String MSG_SIN_PERMISOS = "Asigna al menos un permiso al usuario";
    public static final String MSG_YA_REGISTRADO = "El usuario ya está registrado";
    public static final String MSG_EN_USO = "El nombre de usuario ya está en uso";
    public static final String MSG_NO_REGISTRADO = "No fue posible registrar el usuario";
    public static final String MSG_NO_ACTUALIZADO = "No fue posible guardar los cambios; el usuario conserva sus datos y permisos";
    public static final String MSG_YA_INACTIVA = "La cuenta ya está inactiva";
    public static final String MSG_BAJA_PROPIA = "No puedes dar de baja tu propia cuenta";
    public static final String MSG_NO_DADO_DE_BAJA = "No fue posible dar de baja al usuario";
    public static final String MSG_PERMISO_PROPIO = "No puedes quitarte el permiso «" + Permisos.GESTIONAR_USUARIOS + "»";
    public static final String MSG_NOMBRE_RESERVADO = "Ese nombre de usuario está reservado para la administración";
    public static final String MSG_ESTADO_PROPIO = "No puedes cambiar el estado de tu propia cuenta";

    // Reglas de formato
    private static final int MAX_NOMBRE = 100;
    private static final String PATRON_USUARIO = "[A-Za-z0-9._]{4,30}";
    private static final int MIN_CONTRASENA = 8;

    private UsuarioDAO usuarioDAO;
    private PermisoDAO permisoDAO;

    public UsuarioService() {
        usuarioDAO = DAOFactory.crearUsuarioDAO();
        permisoDAO = DAOFactory.crearPermisoDAO();
    }

    // ===================== Consultas (RNF-18) =====================

    // Devuelve los permisos que se pueden asignar (antes da de alta los que falten)
    public List<Permiso> listarPermisos() throws DAOException {
        permisoDAO.asegurarPermisos(Permisos.TODOS);
        return permisoDAO.listarTodos();
    }

    // Busca usuarios por nombre o usuario; estado null = todos los estados
    // RNF-18 · Paso 3: consulta los usuarios que cumplen el criterio.
    public List<Usuario> buscar(String texto, EstadoUsuario estado) throws DAOException {
        List<Usuario> encontrados = usuarioDAO.buscar(texto, estado);

        // Si en la BD quedó un usuario con el nombre de la Administradora, no se muestra
        List<Usuario> visibles = new ArrayList<Usuario>();
        for (Usuario usuario : encontrados) {
            if (!AdministradoraSistema.esNombreReservado(usuario.getNombreUsuario())) {
                visibles.add(usuario);
            }
        }
        return visibles;
    }

    // Lee un usuario por su id; devuelve null si ya no existe
    // RNF-18 · Paso 6 / RNF-19 · Paso 2: lee el usuario seleccionado (null = ya no existe).
    public Usuario buscarPorId(int idUsuario) throws DAOException {
        return usuarioDAO.buscarPorId(idUsuario);
    }

    // ===================== Registro (RNF-16) =====================

    // RNF-16 (Registro de usuarios): inicia aquí la regla de negocio (pasos 4 a 6).
    public ResultadoOperacion registrar(Usuario datos, String contrasena, String confirmacion) {
        // Paso 4: validar campos, contraseña y permisos
        ResultadoOperacion error = validarDatos(datos);
        // RNF-16 · Excepción paso 4: campos incompletos o con formato inválido.
        if (error != null) {
            return error;
        }
        error = validarContrasena(contrasena, confirmacion);
        // RNF-16 · Excepción paso 4: la contraseña no cumple los requisitos o no coincide con su confirmación.
        if (error != null) {
            return error;
        }
        error = validarPermisos(datos);
        // RNF-16 · Excepción paso 4: «Asigna al menos un permiso al usuario».
        if (error != null) {
            return error;
        }

        // Paso 5: revisar que el nombre de usuario no esté registrado
        Usuario existente;
        try {
            existente = usuarioDAO.buscarPorNombreUsuario(datos.getNombreUsuario());
        } catch (DAOException e) {
            // RNF-16 · Excepción paso 5: sin conexión, «Error de conexión».
            return ResultadoOperacion.error(MSG_ERROR_CONEXION, ResultadoOperacion.CAMPO_CONEXION);
        }
        if (existente != null) {
            // RNF-16 · Excepción paso 5: el usuario es de una cuenta dada de baja; se ofrece reactivarla.
            if (existente.getEstado() == EstadoUsuario.INACTIVO) {
                return ResultadoOperacion.reactivacion(existente.getIdUsuario(),
                        "El usuario «" + existente.getNombreUsuario() + "» pertenece a una cuenta dada de baja ("
                        + existente.getNombreCompleto() + ").\n¿Deseas reactivar esa cuenta con estos datos "
                        + "en lugar de crear una nueva?");
            }
            // RNF-16 · Excepción paso 5: «El usuario ya está registrado».
            return ResultadoOperacion.error(MSG_YA_REGISTRADO, ResultadoOperacion.CAMPO_USUARIO);
        }

        // Paso 6: guardar como Activo, con la contraseña cifrada y sus permisos
        datos.setEstado(EstadoUsuario.ACTIVO);
        datos.setIntentosFallidos(0);
        datos.setContrasenaHash(HashContrasena.generar(contrasena));
        try {
            usuarioDAO.insertar(datos);
        } catch (DAOException e) {
            // RNF-16 · Excepción paso 6: «No fue posible registrar el usuario».
            return ResultadoOperacion.error(MSG_NO_REGISTRADO, ResultadoOperacion.CAMPO_CONEXION);
        }
        // RNF-16 · Paso 7: informa el éxito.
        return ResultadoOperacion.correcto("Usuario «" + datos.getNombreUsuario() + "» registrado correctamente");
    }

    // Reactiva una cuenta dada de baja usando los datos capturados en el formulario
    // RNF-16 · Excepción paso 5: reactiva la cuenta dada de baja en lugar de crear una nueva.
    public ResultadoOperacion reactivar(int idUsuarioExistente, Usuario datos, String contrasena) {
        datos.setIdUsuario(idUsuarioExistente);
        datos.setEstado(EstadoUsuario.ACTIVO);
        datos.setIntentosFallidos(0);
        datos.setContrasenaHash(HashContrasena.generar(contrasena));
        try {
            usuarioDAO.actualizar(datos, true);
        } catch (DAOException e) {
            return ResultadoOperacion.error(MSG_NO_REGISTRADO, ResultadoOperacion.CAMPO_CONEXION);
        }
        return ResultadoOperacion.correcto("La cuenta «" + datos.getNombreUsuario() + "» se reactivó correctamente");
    }

    // ===================== Modificación (RNF-19) =====================

    // RNF-19 (Modificación de usuarios): inicia aquí la regla de negocio (pasos 5 a 7).
    public ResultadoOperacion modificar(Usuario datos, boolean restablecerContrasena,
                                        String contrasena, String confirmacion) {
        // Paso 5: validar campos, la nueva contraseña (si se restableció) y permisos
        ResultadoOperacion error = validarDatos(datos);
        // RNF-19 · Excepción paso 5: datos inválidos.
        if (error != null) {
            return error;
        }
        if (restablecerContrasena) {
            error = validarContrasena(contrasena, confirmacion);
            // RNF-19 · Excepción paso 5: la nueva contraseña no es válida.
            if (error != null) {
                return error;
            }
        }
        error = validarPermisos(datos);
        // RNF-19 · Excepción paso 5: debe conservar al menos un permiso.
        if (error != null) {
            return error;
        }

        // Paso 6: revisar que el usuario siga existiendo y que el nombre no esté en uso
        Usuario actual;
        Usuario mismoNombre;
        try {
            actual = usuarioDAO.buscarPorId(datos.getIdUsuario());
            mismoNombre = usuarioDAO.buscarPorNombreUsuario(datos.getNombreUsuario());
        } catch (DAOException e) {
            // RNF-19 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoOperacion.error(MSG_ERROR_CONEXION, ResultadoOperacion.CAMPO_CONEXION);
        }
        // RNF-19 · Excepción paso 2: «Usuario no encontrado».
        if (actual == null) {
            return ResultadoOperacion.noEncontrado(MSG_NO_ENCONTRADO);
        }
        // RNF-19 · Excepción paso 6: «El nombre de usuario ya está en uso».
        if (mismoNombre != null && mismoNombre.getIdUsuario() != datos.getIdUsuario()) {
            return ResultadoOperacion.error(MSG_EN_USO, ResultadoOperacion.CAMPO_USUARIO);
        }

        // La persona que tiene la sesión no puede quitarse el acceso a este módulo
        if (esUsuarioDeLaSesion(datos.getIdUsuario())) {
            // RNF-19 · Excepción paso 6: no puede quitarse el permiso de gestionar usuarios.
            if (!datos.tienePermiso(Permisos.GESTIONAR_USUARIOS)) {
                return ResultadoOperacion.error(MSG_PERMISO_PROPIO, ResultadoOperacion.CAMPO_PERMISO_PROPIO);
            }
            if (datos.getEstado() != EstadoUsuario.ACTIVO) {
                return ResultadoOperacion.error(MSG_ESTADO_PROPIO, ResultadoOperacion.CAMPO_ESTADO);
            }
        }

        // Si se reactiva una cuenta bloqueada o inactiva, se reinician sus intentos fallidos
        if (datos.getEstado() == EstadoUsuario.ACTIVO && actual.getEstado() != EstadoUsuario.ACTIVO) {
            datos.setIntentosFallidos(0);
        } else {
            datos.setIntentosFallidos(actual.getIntentosFallidos());
        }

        if (restablecerContrasena) {
            datos.setContrasenaHash(HashContrasena.generar(contrasena));
        }

        // Paso 7: guardar (los permisos se aplican la próxima vez que el usuario abra un apartado)
        try {
            usuarioDAO.actualizar(datos, restablecerContrasena);
        } catch (DAOException e) {
            // RNF-19 · Excepción paso 7: no se actualizó; el usuario conserva sus datos.
            return ResultadoOperacion.error(MSG_NO_ACTUALIZADO, ResultadoOperacion.CAMPO_CONEXION);
        }

        // Si se editó a sí misma, la sesión se queda con los permisos nuevos
        // RNF-19 · Paso 7: los nuevos permisos se aplican de inmediato (y ControlAcceso avisa «Tus permisos cambiaron»).
        if (esUsuarioDeLaSesion(datos.getIdUsuario())) {
            SesionUsuario.getInstancia().getUsuario().reemplazarPermisos(datos.getPermisos());
        }
        return ResultadoOperacion.correcto("Usuario «" + datos.getNombreUsuario() + "» actualizado correctamente");
    }

    // ===================== Baja (RNF-17) =====================

    // Paso 2: revisa que se pueda dar de baja y devuelve el usuario para la confirmación
    // RNF-17 (Eliminación de usuarios): inicia aquí la regla de negocio (paso 2).
    public ResultadoOperacion prepararBaja(int idUsuario) {
        Usuario usuario;
        try {
            usuario = usuarioDAO.buscarPorId(idUsuario);
        } catch (DAOException e) {
            // RNF-17 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoOperacion.error(MSG_ERROR_CONEXION, ResultadoOperacion.CAMPO_CONEXION);
        }
        // RNF-17 · Excepción paso 2: «Usuario no encontrado».
        if (usuario == null) {
            return ResultadoOperacion.noEncontrado(MSG_NO_ENCONTRADO);
        }
        // RNF-17 · Excepción paso 2: «La cuenta ya está inactiva».
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            return ResultadoOperacion.error(MSG_YA_INACTIVA, null);
        }
        // RNF-17 · Excepción paso 2: «No puedes dar de baja tu propia cuenta».
        if (esUsuarioDeLaSesion(idUsuario)) {
            return ResultadoOperacion.error(MSG_BAJA_PROPIA, null);
        }
        return ResultadoOperacion.correcto(usuario);
    }

    // Paso 5: el usuario pasa a Inactivo y pierde sus permisos (en una transacción)
    // RNF-17 · Paso 5 (después de confirmar en el paso 4).
    public ResultadoOperacion darDeBaja(Usuario usuario) {
        try {
            usuarioDAO.darDeBaja(usuario.getIdUsuario());
        } catch (DAOException e) {
            // RNF-17 · Excepción paso 5: «No fue posible dar de baja al usuario».
            return ResultadoOperacion.error(MSG_NO_DADO_DE_BAJA, ResultadoOperacion.CAMPO_CONEXION);
        }
        // RNF-17 · Paso 6: informa el éxito.
        return ResultadoOperacion.correcto("El usuario «" + usuario.getNombreUsuario() + "» fue dado de baja");
    }

    // ===================== Validaciones =====================

    // Revisa nombre y nombre de usuario; devuelve null si todo está bien
    // RNF-16 / RNF-19 · Paso 4 / Paso 5: valida nombre y formato del usuario.
    private ResultadoOperacion validarDatos(Usuario datos) {
        String nombre = datos.getNombreCompleto();
        if (nombre == null || nombre.isBlank()) {
            return ResultadoOperacion.error("Escribe el nombre completo", ResultadoOperacion.CAMPO_NOMBRE);
        }
        if (nombre.length() > MAX_NOMBRE) {
            return ResultadoOperacion.error("El nombre completo no puede pasar de " + MAX_NOMBRE + " caracteres",
                    ResultadoOperacion.CAMPO_NOMBRE);
        }

        String usuario = datos.getNombreUsuario();
        if (usuario == null || usuario.isBlank()) {
            return ResultadoOperacion.error("Escribe el nombre de usuario", ResultadoOperacion.CAMPO_USUARIO);
        }
        if (AdministradoraSistema.esNombreReservado(usuario)) {
            return ResultadoOperacion.error(MSG_NOMBRE_RESERVADO, ResultadoOperacion.CAMPO_USUARIO);
        }
        if (!usuario.matches(PATRON_USUARIO)) {
            return ResultadoOperacion.error("El usuario debe tener de 4 a 30 caracteres: letras, números, "
                    + "punto o guion bajo, sin espacios ni acentos", ResultadoOperacion.CAMPO_USUARIO);
        }
        return null;
    }

    // Revisa que se haya marcado al menos un permiso; null si está bien
    // RNF-16 / RNF-19: al menos un permiso.
    private ResultadoOperacion validarPermisos(Usuario datos) {
        if (datos.getPermisos().isEmpty()) {
            return ResultadoOperacion.error(MSG_SIN_PERMISOS, ResultadoOperacion.CAMPO_PERMISOS);
        }
        return null;
    }

    // Revisa que la contraseña sea segura y coincida con la confirmación; null si está bien
    // RNF-16 · Paso 4: la contraseña cumple los requisitos y coincide con su confirmación.
    private ResultadoOperacion validarContrasena(String contrasena, String confirmacion) {
        if (contrasena == null || contrasena.isEmpty()) {
            return ResultadoOperacion.error("Escribe la contraseña", ResultadoOperacion.CAMPO_CONTRASENA);
        }

        boolean tieneMayuscula = false;
        boolean tieneNumero = false;
        for (int i = 0; i < contrasena.length(); i++) {
            char letra = contrasena.charAt(i);
            if (Character.isUpperCase(letra)) {
                tieneMayuscula = true;
            }
            if (Character.isDigit(letra)) {
                tieneNumero = true;
            }
        }
        if (contrasena.length() < MIN_CONTRASENA || !tieneMayuscula || !tieneNumero) {
            return ResultadoOperacion.error("La contraseña debe tener al menos " + MIN_CONTRASENA
                    + " caracteres, una mayúscula y un número", ResultadoOperacion.CAMPO_CONTRASENA);
        }
        if (!contrasena.equals(confirmacion)) {
            return ResultadoOperacion.error("La contraseña y su confirmación no coinciden",
                    ResultadoOperacion.CAMPO_CONTRASENA);
        }
        return null;
    }

    // true si el id es el de la persona que tiene la sesión abierta
    private boolean esUsuarioDeLaSesion(int idUsuario) {
        Usuario usuarioSesion = SesionUsuario.getInstancia().getUsuario();
        if (usuarioSesion == null) {
            return false;
        }
        return usuarioSesion.getIdUsuario() == idUsuario;
    }
}
