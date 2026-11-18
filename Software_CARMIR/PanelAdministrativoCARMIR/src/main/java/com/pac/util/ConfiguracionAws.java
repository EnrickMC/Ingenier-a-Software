package com.pac.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * PATRÓN SINGLETON.
 * Datos para trabajar con AWS (base de datos en Amazon RDS y fotos en S3).
 *
 * Ningún dato de AWS está escrito en el programa: todos salen del archivo
 *   C:\Users\<usuario>\.carmir\aws.properties
 * que crea 1_INSTALAR_PRIMERA_VEZ.bat (opción AWS) y que nunca se sube a Git.
 * Las llaves de acceso de AWS no están en ese archivo: las guarda AWS CLI en
 *   C:\Users\<usuario>\.aws\credentials
 * con el perfil que diga aws.perfil.
 *
 * El Panel trabaja en AWS solo cuando se abre con INICIAR_CARMIR_AWS.bat (variable CARMIR_MODO=aws).
 * Con 2_INICIAR_SISTEMA.bat usa la base local como siempre.
 */
public class ConfiguracionAws {

    // Nombres de las claves del archivo aws.properties
    public static final String CLAVE_PERFIL = "aws.perfil";
    public static final String CLAVE_REGION = "aws.region";
    public static final String CLAVE_INSTANCIA = "aws.ec2.instancia";
    public static final String CLAVE_SERVIDOR_RDS = "aws.rds.servidor";
    public static final String CLAVE_PUERTO_RDS = "aws.rds.puerto";
    public static final String CLAVE_PUERTO_LOCAL = "aws.tunel.puertoLocal";
    public static final String CLAVE_BASE = "aws.bd.nombre";
    public static final String CLAVE_USUARIO_BD = "aws.bd.usuario";
    public static final String CLAVE_CONTRASENA_BD = "aws.bd.contrasena";
    public static final String CLAVE_BUCKET = "aws.s3.bucketImagenes";
    public static final String CLAVE_DOMINIO = "aws.imagenes.dominio";
    public static final String CLAVE_CARPETA_CLI = "aws.cli.carpeta";

    // La única instancia de la clase
    private static ConfiguracionAws instancia;

    private Properties propiedades;

    // Constructor privado: lee el archivo una vez
    private ConfiguracionAws() {
        recargar();
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized ConfiguracionAws getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionAws();
        }
        return instancia;
    }

    // Vuelve a leer aws.properties (por ejemplo, después de correr otra vez el instalador)
    public synchronized void recargar() {
        propiedades = new Properties();
        File archivo = getArchivo();
        if (!archivo.isFile()) {
            return;
        }
        try {
            FileInputStream flujo = new FileInputStream(archivo);
            propiedades.load(new InputStreamReader(flujo, StandardCharsets.UTF_8));
            flujo.close();
        } catch (IOException e) {
            System.out.println("No se pudo leer " + archivo.getPath());
        }
    }

    // ===================== Modo AWS =====================

    // true si el Panel se abrió con INICIAR_CARMIR_AWS.bat (variable CARMIR_MODO=aws)
    public boolean isActivo() {
        String modo = System.getenv("CARMIR_MODO");
        if (modo == null) {
            return false;
        }
        return modo.trim().equalsIgnoreCase("aws");
    }

    // C:\Users\<usuario>\.carmir\aws.properties
    public File getArchivo() {
        File carpeta = new File(System.getProperty("user.home"), ".carmir");
        return new File(carpeta, "aws.properties");
    }

    public boolean existeArchivo() {
        return getArchivo().isFile();
    }

    // ===================== Datos del archivo =====================

    // Texto de una clave; vacío si no está
    public synchronized String leer(String clave) {
        String valor = propiedades.getProperty(clave);
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }

    public String getPerfil() {
        return leer(CLAVE_PERFIL);
    }

    public String getRegion() {
        return leer(CLAVE_REGION);
    }

    public String getInstanciaEc2() {
        return leer(CLAVE_INSTANCIA);
    }

    public String getServidorRds() {
        return leer(CLAVE_SERVIDOR_RDS);
    }

    public String getPuertoRds() {
        return leer(CLAVE_PUERTO_RDS);
    }

    // Puerto de esta PC donde queda el túnel hacia RDS
    public String getPuertoLocal() {
        return leer(CLAVE_PUERTO_LOCAL);
    }

    public String getBaseDatos() {
        return leer(CLAVE_BASE);
    }

    public String getUsuarioBD() {
        return leer(CLAVE_USUARIO_BD);
    }

    // Solo la usa ConexionBD; nunca se muestra en pantalla
    public String getContrasenaBD() {
        return leer(CLAVE_CONTRASENA_BD);
    }

    public String getBucketImagenes() {
        return leer(CLAVE_BUCKET);
    }

    // Dominio de CloudFront que sirve /imagenes/... (sin "/" al final)
    public String getDominioImagenes() {
        String dominio = leer(CLAVE_DOMINIO);
        while (dominio.endsWith("/")) {
            dominio = dominio.substring(0, dominio.length() - 1);
        }
        return dominio;
    }

    public String getCarpetaCli() {
        return leer(CLAVE_CARPETA_CLI);
    }

    // URL de JDBC hacia RDS a través del túnel (127.0.0.1:puertoLocal).
    // sslMode=REQUIRED: RDS solo acepta conexiones cifradas. No se revisa el nombre del servidor porque
    // por el túnel se conecta a 127.0.0.1 y el certificado trae el nombre de RDS.
    public String getUrlJdbc() {
        return "jdbc:mysql://127.0.0.1:" + getPuertoLocal() + "/" + getBaseDatos()
                + "?sslMode=REQUIRED&serverTimezone=America/Mexico_City&connectTimeout=10000&tcpKeepAlive=true";
    }

    // Lista de las claves obligatorias que faltan en el archivo (vacía si está completo)
    public List<String> clavesFaltantes() {
        String[] obligatorias = {CLAVE_PERFIL, CLAVE_REGION, CLAVE_INSTANCIA, CLAVE_SERVIDOR_RDS, CLAVE_PUERTO_RDS,
                CLAVE_PUERTO_LOCAL, CLAVE_BASE, CLAVE_USUARIO_BD, CLAVE_CONTRASENA_BD, CLAVE_BUCKET, CLAVE_DOMINIO};
        List<String> faltan = new ArrayList<String>();
        for (String clave : obligatorias) {
            if (leer(clave).isEmpty()) {
                faltan.add(clave);
            }
        }
        return faltan;
    }

    // ===================== Llaves de AWS CLI (solo lectura) =====================

    // Archivo de credenciales de AWS CLI (C:\Users\<usuario>\.aws\credentials)
    public File getArchivoCredenciales() {
        String otraRuta = System.getenv("AWS_SHARED_CREDENTIALS_FILE");
        if (otraRuta != null && !otraRuta.isBlank()) {
            return new File(otraRuta.trim());
        }
        File carpeta = new File(System.getProperty("user.home"), ".aws");
        return new File(carpeta, "credentials");
    }

    // Access Key ID del perfil; vacío si no está
    public String getAccessKeyId() {
        return leerCredencial("aws_access_key_id");
    }

    // true si el perfil tiene clave secreta guardada (la clave nunca se lee para mostrarla)
    public boolean tieneClaveSecreta() {
        return !leerCredencial("aws_secret_access_key").isEmpty();
    }

    // Busca una clave dentro de la sección [perfil] del archivo de credenciales
    private String leerCredencial(String nombre) {
        File archivo = getArchivoCredenciales();
        String perfil = getPerfil();
        if (!archivo.isFile() || perfil.isEmpty()) {
            return "";
        }
        String valor = "";
        try {
            BufferedReader lector = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8));
            boolean enPerfil = false;
            String linea = lector.readLine();
            while (linea != null) {
                String limpia = linea.trim();
                if (limpia.startsWith("[") && limpia.endsWith("]")) {
                    String seccion = limpia.substring(1, limpia.length() - 1).trim();
                    enPerfil = seccion.equals(perfil);
                } else if (enPerfil && limpia.contains("=")) {
                    int igual = limpia.indexOf('=');
                    String clave = limpia.substring(0, igual).trim();
                    if (clave.equalsIgnoreCase(nombre)) {
                        valor = limpia.substring(igual + 1).trim();
                    }
                }
                linea = lector.readLine();
            }
            lector.close();
        } catch (IOException e) {
            return "";
        }
        return valor;
    }

    // ===================== AWS CLI =====================

    // Busca aws.exe: carpeta de aws.properties, PATH e instalación normal de AWS CLI v2. null si no está.
    public File buscarAwsCli() {
        List<String> carpetas = new ArrayList<String>();
        String configurada = getCarpetaCli();
        if (!configurada.isEmpty()) {
            carpetas.add(configurada);
        }
        String path = System.getenv("PATH");
        if (path != null) {
            String[] partes = path.split(File.pathSeparator);
            for (String parte : partes) {
                carpetas.add(parte);
            }
        }
        // Carpeta donde el instalador de Amazon deja AWS CLI v2 (dentro de Archivos de programa)
        String programas = System.getenv("ProgramFiles");
        if (programas != null) {
            carpetas.add(programas + File.separator + "Amazon" + File.separator + "AWSCLIV2");
        }

        for (String carpeta : carpetas) {
            File programaExe = new File(carpeta, "aws.exe");
            if (programaExe.isFile()) {
                return programaExe;
            }
            File programa = new File(carpeta, "aws");
            if (programa.isFile()) {
                return programa;
            }
        }
        return null;
    }
}
