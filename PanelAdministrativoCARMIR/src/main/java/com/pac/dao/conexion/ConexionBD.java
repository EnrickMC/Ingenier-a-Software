package com.pac.dao.conexion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * PATRÓN SINGLETON.
 * Solo existe un objeto de esta clase en todo el programa y se encarga de la
 * conexión con la base de datos MySQL CARMIR_DB.
 * Los datos de conexión se leen del archivo config/db.properties.
 */
public class ConexionBD {

    private static final String ARCHIVO_CONFIG = "/com/pac/config/db.properties";

    // La única instancia de la clase
    private static ConexionBD instancia;

    private String url;
    private String usuario;
    private String contrasena;
    private Connection conexion;

    // Constructor privado: nadie fuera de esta clase puede hacer "new ConexionBD()"
    private ConexionBD() {
        Properties propiedades = new Properties();
        try {
            InputStream archivo = ConexionBD.class.getResourceAsStream(ARCHIVO_CONFIG);
            if (archivo != null) {
                propiedades.load(archivo);
                archivo.close();
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ARCHIVO_CONFIG);
        }
        url = propiedades.getProperty("db.url", "jdbc:mysql://localhost:3306/CARMIR_DB");
        usuario = propiedades.getProperty("db.usuario", "root");
        contrasena = propiedades.getProperty("db.contrasena", "");
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    // Devuelve la conexión abierta; si no existe o se cerró, abre una nueva
    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(url, usuario, contrasena);
        }
        return conexion;
    }

    // Abre una conexión nueva, aparte de la principal.
    // La usan los procesos en segundo plano (por ejemplo el envío de notificaciones del Módulo 4)
    // para no mezclarse con las transacciones de la pantalla. Quien la pide debe cerrarla.
    public Connection abrirConexionAparte() throws SQLException {
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    // ----- Datos de conexión (los usa el apartado Configuración para mostrarlos y para los respaldos) -----

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }

    // Solo la usa RespaldoService para llamar a mysqldump; no se muestra en pantalla
    public String getContrasena() {
        return contrasena;
    }

    // Revisa si se puede hablar con la base de datos
    public boolean probarConexion() {
        try {
            Connection prueba = getConexion();
            return prueba.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    // Cierra la conexión al salir del programa
    public synchronized void cerrar() {
        if (conexion == null) {
            return;
        }
        try {
            conexion.close();
        } catch (SQLException e) {
            System.out.println("Error al cerrar la conexión: " + e.getMessage());
        }
        conexion = null;
    }
}
