package com.pac.modulos.usuarios.service;

/**
 * Respuestas posibles cuando alguien intenta abrir un apartado del panel (RNF-15).
 */
public enum ResultadoAcceso {

    // Puede entrar
    PERMITIDO,

    // No tiene el permiso del apartado
    DENEGADO,

    // La administradora le cambió los permisos: hay que rehacer el menú
    PERMISOS_CAMBIARON,

    // Su cuenta ya no está activa (dada de baja o bloqueada)
    CUENTA_INACTIVA,

    // No hay sesión abierta
    SESION_VENCIDA,

    // No hubo conexión con la base de datos para revisar sus permisos
    SIN_VERIFICAR
}
