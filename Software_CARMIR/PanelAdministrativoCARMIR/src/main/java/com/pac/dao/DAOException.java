package com.pac.dao;

/**
 * Error que lanzan los DAO cuando algo falla con la base de datos.
 * Así los controladores no tienen que conocer las clases de JDBC.
 */
public class DAOException extends Exception {

    public DAOException(String mensaje) {
        super(mensaje);
    }

    public DAOException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
