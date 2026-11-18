package com.pac.modulos.configuracion.service;

import com.pac.util.ConfiguracionAws;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Revisa la conexión con AWS para la tarjeta "Conexión con AWS" del apartado Configuración.
 * Solo lee: nunca cambia la configuración ni muestra claves secretas.
 */
public class ConexionAwsService {

    // Tiempo máximo que se espera a AWS CLI
    private static final int SEGUNDOS_MAXIMOS = 30;

    // Access Key ID con el centro tapado: AKIA••••••••WXYZ
    public String ocultarLlave(String llave) {
        if (llave == null || llave.isEmpty()) {
            return "";
        }
        if (llave.length() <= 8) {
            return "••••••••";
        }
        return llave.substring(0, 4) + "••••••••" + llave.substring(llave.length() - 4);
    }

    // true si en esta PC hay algo escuchando en el puerto del túnel (127.0.0.1:puerto)
    public boolean tunelAbierto(String puerto) {
        int numero;
        try {
            numero = Integer.parseInt(puerto.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", numero), 500);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // No importa si no se pudo cerrar
            }
        }
    }

    /**
     * Pregunta a AWS con qué usuario entran las llaves del perfil (aws sts get-caller-identity).
     * Devuelve el texto para mostrar: el ARN del usuario si funcionó, o el motivo del error.
     */
    public ResultadoConfiguracion probarCredenciales() {
        ConfiguracionAws configuracion = ConfiguracionAws.getInstancia();
        File aws = configuracion.buscarAwsCli();
        if (aws == null) {
            return ResultadoConfiguracion.error("No se encontró AWS CLI en esta PC", null);
        }
        if (configuracion.getPerfil().isEmpty() || configuracion.getRegion().isEmpty()) {
            return ResultadoConfiguracion.error("Falta el perfil o la región en " + configuracion.getArchivo().getPath(), null);
        }
        List<String> comando = new ArrayList<String>();
        comando.add(aws.getAbsolutePath());
        comando.add("sts");
        comando.add("get-caller-identity");
        comando.add("--profile");
        comando.add(configuracion.getPerfil());
        comando.add("--region");
        comando.add(configuracion.getRegion());
        comando.add("--query");
        comando.add("Arn");
        comando.add("--output");
        comando.add("text");

        File bitacora = null;
        try {
            bitacora = File.createTempFile("carmir_sts", ".log");
            ProcessBuilder constructor = new ProcessBuilder(comando);
            constructor.redirectErrorStream(true);
            constructor.redirectOutput(bitacora);
            Process proceso = constructor.start();
            boolean termino = proceso.waitFor(SEGUNDOS_MAXIMOS, TimeUnit.SECONDS);
            if (!termino) {
                proceso.destroyForcibly();
                return ResultadoConfiguracion.error("AWS no respondió. Revisa la conexión a internet", null);
            }
            String salida = ultimaLinea(bitacora);
            if (proceso.exitValue() != 0) {
                return ResultadoConfiguracion.error("Las credenciales no funcionan: " + salida, null);
            }
            return ResultadoConfiguracion.correcto("Credenciales correctas. Usuario de AWS: " + salida);
        } catch (IOException e) {
            return ResultadoConfiguracion.error("No se pudo ejecutar AWS CLI: " + e.getMessage(), null);
        } catch (InterruptedException e) {
            return ResultadoConfiguracion.error("Se interrumpió la prueba", null);
        } finally {
            if (bitacora != null && !bitacora.delete()) {
                bitacora.deleteOnExit();
            }
        }
    }

    // Última línea con texto de lo que escribió AWS CLI
    private String ultimaLinea(File bitacora) throws IOException {
        List<String> lineas = Files.readAllLines(bitacora.toPath(), StandardCharsets.UTF_8);
        String mensaje = "sin respuesta";
        for (String linea : lineas) {
            if (!linea.isBlank()) {
                mensaje = linea.trim();
            }
        }
        return mensaje;
    }
}
