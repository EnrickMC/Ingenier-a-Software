package com.pac.modulos.configuracion.service;

import com.pac.dao.conexion.ConexionBD;
import com.pac.util.ConfiguracionApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Respaldos de la base de datos (RF-72 generar y RF-73 restaurar; RNF-8, Prompt 8.1).
 * Los respaldos solo se hacen cuando el usuario los pide en Configuración (no hay copias automáticas).
 *
 * - Generar respaldo: ejecuta mysqldump y guarda toda CARMIR_DB en un archivo .sql.
 * - Restaurar respaldo: ejecuta mysql con ese archivo (reemplaza los datos actuales).
 *
 * mysqldump y mysql vienen con MySQL (por ejemplo C:\Program Files\MySQL\MySQL Server 8.0\bin).
 * Se buscan en: la carpeta elegida en pantalla, respaldos.carpetaMysql de app.properties,
 * el PATH del sistema y las carpetas comunes de instalación en Windows.
 * La contraseña no se escribe en la línea de comandos: va en un archivo temporal que se borra al terminar.
 */
public class RespaldoService {

    public static final String MSG_SIN_PROGRAMA = "No se encontró %s. Elige la carpeta \"bin\" de tu instalación de MySQL";
    public static final String MSG_RESPALDO_OK = "Respaldo generado";
    public static final String MSG_RESPALDO_ERROR = "No fue posible generar el respaldo";
    public static final String MSG_RESTAURAR_OK = "Respaldo restaurado. Vuelve a abrir los apartados para ver los datos";
    public static final String MSG_RESTAURAR_ERROR = "No fue posible restaurar el respaldo";
    public static final String MSG_ARCHIVO_NO_VALIDO = "El archivo no es un respaldo de CARMIR_DB generado con mysqldump";

    // Tiempo máximo que se espera a mysqldump o mysql
    private static final int MINUTOS_MAXIMOS = 10;

    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    // Carpeta de MySQL elegida en pantalla (dura mientras el panel esté abierto)
    private static String carpetaElegida = "";

    // ===================== Datos de la conexión =====================

    // RF-72 · Paso 4: nombre sugerido del respaldo, CARMIR_Respaldo_2026-11-01_1830.sql
    public String nombreSugerido() {
        return "CARMIR_Respaldo_" + FECHA_ARCHIVO.format(LocalDateTime.now()) + ".sql";
    }

    // Servidor de la URL jdbc:mysql://servidor:puerto/base
    public String getServidor() {
        String direccion = direccionDeUrl();
        int dosPuntos = direccion.indexOf(':');
        if (dosPuntos >= 0) {
            return direccion.substring(0, dosPuntos);
        }
        return direccion;
    }

    public String getPuerto() {
        String direccion = direccionDeUrl();
        int dosPuntos = direccion.indexOf(':');
        if (dosPuntos >= 0) {
            return direccion.substring(dosPuntos + 1);
        }
        return "3306";
    }

    public String getBaseDeDatos() {
        String url = ConexionBD.getInstancia().getUrl();
        int inicio = url.indexOf("//");
        int diagonal = url.indexOf('/', inicio + 2);
        if (diagonal < 0) {
            return "CARMIR_DB";
        }
        int fin = url.indexOf('?', diagonal);
        if (fin < 0) {
            fin = url.length();
        }
        return url.substring(diagonal + 1, fin);
    }

    // "servidor:puerto" de la URL
    private String direccionDeUrl() {
        String url = ConexionBD.getInstancia().getUrl();
        int inicio = url.indexOf("//");
        if (inicio < 0) {
            return "localhost";
        }
        int fin = url.indexOf('/', inicio + 2);
        if (fin < 0) {
            fin = url.length();
        }
        return url.substring(inicio + 2, fin);
    }

    // ===================== Programas de MySQL =====================

    public static void setCarpetaElegida(String carpeta) {
        if (carpeta == null) {
            carpetaElegida = "";
        } else {
            carpetaElegida = carpeta.trim();
        }
    }

    // Busca mysqldump o mysql; devuelve null si no se encontró
    public File buscarPrograma(String nombre) {
        List<String> carpetas = new ArrayList<String>();
        if (!carpetaElegida.isEmpty()) {
            carpetas.add(carpetaElegida);
        }
        String carpetaConfigurada = ConfiguracionApp.getInstancia().getCarpetaMysql();
        if (!carpetaConfigurada.isEmpty()) {
            carpetas.add(carpetaConfigurada);
        }
        // Carpetas del PATH del sistema
        String path = System.getenv("PATH");
        if (path != null) {
            String[] partes = path.split(File.pathSeparator);
            for (String parte : partes) {
                carpetas.add(parte);
            }
        }
        // Instalaciones comunes en Windows (MySQL, MariaDB y XAMPP)
        agregarInstalaciones(carpetas, "C:\\Program Files\\MySQL");
        agregarInstalaciones(carpetas, "C:\\Program Files\\MariaDB");
        carpetas.add("C:\\xampp\\mysql\\bin");

        for (String carpeta : carpetas) {
            File programa = new File(carpeta, nombre);
            if (programa.isFile()) {
                return programa;
            }
            File programaExe = new File(carpeta, nombre + ".exe");
            if (programaExe.isFile()) {
                return programaExe;
            }
        }
        return null;
    }

    // Agrega la carpeta "bin" de cada versión instalada (por ejemplo MySQL Server 8.0\bin)
    private void agregarInstalaciones(List<String> carpetas, String raiz) {
        File carpetaRaiz = new File(raiz);
        File[] versiones = carpetaRaiz.listFiles();
        if (versiones == null) {
            return;
        }
        for (File version : versiones) {
            if (version.isDirectory()) {
                carpetas.add(new File(version, "bin").getAbsolutePath());
            }
        }
    }

    // ===================== Generar y restaurar =====================

    // RF-72 · Paso 6: guarda toda la base de datos en el archivo indicado
    public ResultadoConfiguracion generarRespaldo(File destino) {
        File mysqldump = buscarPrograma("mysqldump");
        // RF-72 · Excepción paso 6: «No se encontró mysqldump. Elige la carpeta "bin" de tu instalación de MySQL».
        if (mysqldump == null) {
            return ResultadoConfiguracion.error(String.format(MSG_SIN_PROGRAMA, "mysqldump"), null);
        }
        File credenciales = null;
        try {
            credenciales = crearArchivoCredenciales();
            List<String> comando = new ArrayList<String>();
            comando.add(mysqldump.getAbsolutePath());
            // El archivo de credenciales debe ir primero
            comando.add("--defaults-extra-file=" + credenciales.getAbsolutePath());
            comando.add("--host=" + getServidor());
            comando.add("--port=" + getPuerto());
            // Siempre por TCP, igual que la conexión del panel
            comando.add("--protocol=TCP");
            comando.add("--default-character-set=utf8mb4");
            comando.add("--single-transaction");
            comando.add("--routines");
            comando.add("--triggers");
            comando.add("--result-file=" + destino.getAbsolutePath());
            comando.add("--databases");
            comando.add(getBaseDeDatos());

            String salida = ejecutar(comando, null);
            // RF-72 · Excepción paso 6: «No fue posible generar el respaldo» con el motivo.
            if (salida != null) {
                return ResultadoConfiguracion.error(MSG_RESPALDO_ERROR + ": " + salida, null);
            }
            if (!destino.isFile() || destino.length() == 0) {
                return ResultadoConfiguracion.error(MSG_RESPALDO_ERROR, null);
            }
            // RF-72 · Paso 7: «Respaldo generado: [ruta del archivo]».
            return ResultadoConfiguracion.correcto(MSG_RESPALDO_OK + ": " + destino.getAbsolutePath());
        } catch (IOException e) {
            return ResultadoConfiguracion.error(MSG_RESPALDO_ERROR + ": " + e.getMessage(), null);
        } finally {
            borrar(credenciales);
        }
    }

    // RF-73 · Paso 5: revisa que el archivo parezca un respaldo de mysqldump de esta base
    public boolean esRespaldoValido(File archivo) {
        try {
            BufferedReader lector = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8));
            boolean esDump = false;
            boolean tieneBase = false;
            String linea = lector.readLine();
            int leidas = 0;
            // Se revisan solo las primeras líneas (el encabezado de mysqldump)
            while (linea != null && leidas < 60) {
                if (linea.contains("MySQL dump") || linea.contains("MariaDB dump")) {
                    esDump = true;
                }
                if (linea.contains(getBaseDeDatos())) {
                    tieneBase = true;
                }
                linea = lector.readLine();
                leidas++;
            }
            lector.close();
            return esDump && tieneBase;
        } catch (IOException e) {
            return false;
        }
    }

    // RF-73 · Paso 8: ejecuta el archivo .sql del respaldo con el programa mysql
    // (vuelve a revisar que sea la administradora y que el archivo sea válido)
    public ResultadoConfiguracion restaurarRespaldo(File origen) {
        if (!PermisosConfiguracion.puedeRestaurar()) {
            return ResultadoConfiguracion.error(PermisosConfiguracion.MSG_SOLO_ADMINISTRADORA, null);
        }
        if (!origen.isFile() || !esRespaldoValido(origen)) {
            return ResultadoConfiguracion.error(MSG_ARCHIVO_NO_VALIDO, null);
        }
        File mysql = buscarPrograma("mysql");
        if (mysql == null) {
            // RF-73 · Excepción paso 8: «No se encontró mysql…».
            return ResultadoConfiguracion.error(String.format(MSG_SIN_PROGRAMA, "mysql"), null);
        }
        File credenciales = null;
        try {
            credenciales = crearArchivoCredenciales();
            List<String> comando = new ArrayList<String>();
            comando.add(mysql.getAbsolutePath());
            comando.add("--defaults-extra-file=" + credenciales.getAbsolutePath());
            comando.add("--host=" + getServidor());
            comando.add("--port=" + getPuerto());
            // Siempre por TCP, igual que la conexión del panel
            comando.add("--protocol=TCP");
            comando.add("--default-character-set=utf8mb4");

            // RF-73 · Paso 8: la conexión del panel se cierra para no chocar con la restauración
            ConexionBD.getInstancia().cerrar();
            String salida = ejecutar(comando, origen);
            if (salida != null) {
                // RF-73 · Excepción paso 8: «No fue posible restaurar el respaldo» con el motivo.
                return ResultadoConfiguracion.error(MSG_RESTAURAR_ERROR + ": " + salida, null);
            }
            // RF-73 · Paso 9: los parámetros pudieron cambiar con el respaldo; se vuelven a cargar
            new ConfiguracionService().cargarEnAplicacion();
            return ResultadoConfiguracion.correcto(MSG_RESTAURAR_OK);
        } catch (IOException e) {
            return ResultadoConfiguracion.error(MSG_RESTAURAR_ERROR + ": " + e.getMessage(), null);
        } finally {
            borrar(credenciales);
        }
    }

    // ===================== Apoyo =====================

    /**
     * Ejecuta el comando y espera a que termine. entrada = archivo que se manda al programa (o null).
     * Devuelve null si terminó bien; si no, el mensaje de error del programa.
     */
    private String ejecutar(List<String> comando, File entrada) throws IOException {
        File bitacora = File.createTempFile("carmir_respaldo", ".log");
        try {
            ProcessBuilder constructor = new ProcessBuilder(comando);
            constructor.redirectErrorStream(true);
            constructor.redirectOutput(bitacora);
            if (entrada != null) {
                constructor.redirectInput(entrada);
            }
            Process proceso = constructor.start();
            boolean termino = proceso.waitFor(MINUTOS_MAXIMOS, TimeUnit.MINUTES);
            if (!termino) {
                proceso.destroyForcibly();
                return "el programa tardó más de " + MINUTOS_MAXIMOS + " minutos";
            }
            if (proceso.exitValue() != 0) {
                return leerMensaje(bitacora);
            }
            return null;
        } catch (InterruptedException e) {
            return "se interrumpió la operación";
        } finally {
            borrar(bitacora);
        }
    }

    // Últimas líneas útiles de lo que escribió el programa (sin el aviso de la contraseña)
    private String leerMensaje(File bitacora) throws IOException {
        List<String> lineas = Files.readAllLines(bitacora.toPath(), StandardCharsets.UTF_8);
        String mensaje = "";
        for (String linea : lineas) {
            if (!linea.isBlank() && !linea.contains("[Warning]")) {
                mensaje = linea.trim();
            }
        }
        if (mensaje.isEmpty()) {
            return "error desconocido";
        }
        return mensaje;
    }

    // Archivo temporal [client] con usuario y contraseña (así no aparecen en la lista de procesos)
    private File crearArchivoCredenciales() throws IOException {
        File archivo = File.createTempFile("carmir_cliente", ".cnf");
        FileWriter escritor = new FileWriter(archivo, StandardCharsets.UTF_8);
        escritor.write("[client]\n");
        escritor.write("user=\"" + ConexionBD.getInstancia().getUsuario() + "\"\n");
        escritor.write("password=\"" + ConexionBD.getInstancia().getContrasena().replace("\"", "\\\"") + "\"\n");
        escritor.close();
        return archivo;
    }

    private void borrar(File archivo) {
        if (archivo != null && archivo.exists()) {
            boolean borrado = archivo.delete();
            if (!borrado) {
                archivo.deleteOnExit();
            }
        }
    }
}
