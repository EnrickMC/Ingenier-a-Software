package com.pac.dao.conexion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.pac.util.ConfiguracionAws;

/**
 * PATRÓN SINGLETON.
 * Solo existe un objeto de esta clase en todo el programa y se encarga de la
 * conexión con la base de datos MySQL CARMIR_DB.
 * Los datos de conexión se leen del archivo config/db.properties y pueden
 * sobrescribirse localmente con db.local.properties sin afectar a otros equipos.
 *
 * En AWS (Panel abierto con INICIAR_CARMIR_AWS.bat, variable CARMIR_MODO=aws) los datos salen de
 * C:\Users\<usuario>\.carmir\aws.properties (lo crea 1_INSTALAR_PRIMERA_VEZ.bat, fuera de Git):
 * la base está en Amazon RDS y se llega por el túnel de SSM que abre INICIAR_CARMIR_AWS.bat.
 */
public class ConexionBD {

    private static final String ARCHIVO_CONFIG = "/com/pac/config/db.properties";
    private static final String ARCHIVO_LOCAL = "db.local.properties";
    private static final String ARCHIVO_LOCAL_RAIZ = "Software_CARMIR/PanelAdministrativoCARMIR/db.local.properties";
    // Segundos que se espera al revisar si la conexión sigue viva
    private static final int SEGUNDOS_REVISION = 3;
    private static final int CODIGO_ACCESO_DENEGADO = 1045;

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
        cargarConfiguracionLocal(propiedades);

        url = propiedades.getProperty("db.url", "jdbc:mysql://localhost:3306/CARMIR_DB");
        usuario = propiedades.getProperty("db.usuario", "root");
        contrasena = propiedades.getProperty("db.contrasena", "");

        String contrasenaEntorno = System.getenv("CARMIR_DB_CONTRASENA");
        if (contrasenaEntorno != null) {
            contrasena = contrasenaEntorno;
        }
    }

    // Si existe db.local.properties en este equipo, sobrescribe los datos sin tocar Git.
    // En modo AWS los datos salen de .carmir\aws.properties de la carpeta del usuario.
    private void cargarConfiguracionLocal(Properties propiedades) {
        ConfiguracionAws aws = ConfiguracionAws.getInstancia();
        if (aws.isActivo()) {
            if (!aws.existeArchivo()) {
                System.out.println("Falta " + aws.getArchivo().getPath() + " (ejecuta 1_INSTALAR_PRIMERA_VEZ.bat, opción AWS)");
                return;
            }
            propiedades.setProperty("db.url", aws.getUrlJdbc());
            propiedades.setProperty("db.usuario", aws.getUsuarioBD());
            propiedades.setProperty("db.contrasena", aws.getContrasenaBD());
            return;
        }
        File archivoLocal = new File(ARCHIVO_LOCAL);
        if (!archivoLocal.isFile()) {
            archivoLocal = new File(ARCHIVO_LOCAL_RAIZ);
        }
        if (!archivoLocal.isFile()) {
            return;
        }
        leerArchivo(archivoLocal, propiedades);
    }

    // Carga un archivo .properties encima de los valores que ya había
    private void leerArchivo(File archivo, Properties propiedades) {
        try {
            FileInputStream flujo = new FileInputStream(archivo);
            propiedades.load(flujo);
            flujo.close();
        } catch (IOException e) {
            System.out.println("No se pudo leer " + archivo.getPath());
        }
    }

    // Abre una conexión con MySQL; si root falla por contraseña distinta entre equipos, prueba las habituales
    private Connection abrirConexionMysql() throws SQLException {
        try {
            return DriverManager.getConnection(url, usuario, contrasena);
        } catch (SQLException errorOriginal) {
            if (errorOriginal.getErrorCode() != CODIGO_ACCESO_DENEGADO || !"root".equalsIgnoreCase(usuario)) {
                throw errorOriginal;
            }
            String[] alternativas = new String[]{"", "admin", "root", "1234", "123456"};
            for (int i = 0; i < alternativas.length; i++) {
                String candidata = alternativas[i];
                if (candidata.equals(contrasena)) {
                    continue;
                }
                try {
                    Connection conexionAlternativa = DriverManager.getConnection(url, usuario, candidata);
                    contrasena = candidata;
                    return conexionAlternativa;
                } catch (SQLException ignorado) {
                    // Se sigue probando con la siguiente contraseña habitual
                }
            }
            throw errorOriginal;
        }
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    // Devuelve la conexión abierta; si no existe, se cerró o se cayó, abre una nueva.
    // En AWS el túnel de SSM corta las conexiones que pasan mucho tiempo sin usarse:
    // isValid() lo detecta y se abre otra sin que el usuario tenga que reiniciar el Panel.
    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = abrirConexionMysql();
            return conexion;
        }
        if (!conexion.isValid(SEGUNDOS_REVISION)) {
            cerrar();
            conexion = abrirConexionMysql();
        }
        return conexion;
    }

    // Abre una conexión nueva, aparte de la principal.
    // La usan los procesos en segundo plano (por ejemplo el envío de notificaciones del Módulo 4)
    // para no mezclarse con las transacciones de la pantalla. Quien la pide debe cerrarla.
    public Connection abrirConexionAparte() throws SQLException {
        return abrirConexionMysql();
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
