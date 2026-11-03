package com.pac.modulos.configuracion.dao;

import com.pac.dao.DAOException;

import java.sql.Connection;
import java.util.Properties;

/**
 * Acceso a la tabla ConfiguracionNotificacion: datos de WhatsApp y correo (RF-32).
 * Los secretos (token y contraseña) llegan y salen cifrados; el servicio es quien los cifra.
 * La página web no tiene permiso de leer esta tabla.
 */
public interface IntegracionesDAO {

    String MSG_FALTA_MIGRACION = "La tabla ConfiguracionNotificacion no existe: ejecuta "
            + "DBCarmir/migraciones/MIGRACION_COMPLETA.sql";

    // Todos los datos guardados (Clave → Valor)
    Properties listar() throws DAOException;

    // Cambia el valor de una clave (la crea si no existe)
    void guardar(Connection conexion, String clave, String valor, boolean esSecreto) throws DAOException;
}
