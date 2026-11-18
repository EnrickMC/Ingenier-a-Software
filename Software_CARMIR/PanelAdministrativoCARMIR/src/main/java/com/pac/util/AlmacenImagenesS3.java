package com.pac.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sube las fotos del catálogo al bucket de S3 cuando el Panel trabaja en AWS.
 *
 * Cómo funciona:
 *  - En el formulario se elige la foto como siempre (un archivo de esta computadora).
 *  - Al guardar el producto o la opción, la foto se sube al bucket de aws.properties:
 *      s3://<bucket>/imagenes/postres/...   (productos)
 *      s3://<bucket>/imagenes/opciones/...  (opciones)
 *    y en la base se guarda la ruta relativa /imagenes/postres/..., que CloudFront sirve en el mismo
 *    dominio de la página web.
 *  - Para subirla se usa AWS CLI (aws.exe) con el perfil de aws.properties, el mismo que abre el túnel
 *    de la base de datos. Así el Panel no necesita librerías nuevas ni guardar llaves.
 *
 * Fuera de AWS (2_INICIAR_SISTEMA.bat con MySQL local) no sube nada: la ruta del archivo se guarda
 * igual que antes.
 */
public class AlmacenImagenesS3 {

    // Carpetas dentro de imagenes/ (deben coincidir con el comportamiento /imagenes/* de CloudFront)
    public static final String CARPETA_POSTRES = "postres";
    public static final String CARPETA_OPCIONES = "opciones";

    // Tamaño máximo de una foto (5 MB)
    private static final long BYTES_MAXIMOS = 5L * 1024L * 1024L;

    // Tiempo máximo que se espera a que termine la subida
    private static final int SEGUNDOS_MAXIMOS = 120;

    private static final DateTimeFormatter FECHA_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    // Clase de apoyo: no se crean objetos de ella
    private AlmacenImagenesS3() {
    }

    /**
     * Si hace falta, sube la foto a S3 y devuelve la ruta que se debe guardar en la base.
     *  - Fuera de AWS, o si el texto es un enlace http/https o ya es /imagenes/..., lo devuelve igual.
     *  - Si es un archivo de esta computadora, lo sube y devuelve /imagenes/<carpeta>/<nombre>.
     * Lanza IOException con un mensaje para el usuario si no se pudo subir.
     */
    public static String subirSiHaceFalta(String texto, String carpeta) throws IOException {
        if (!ConfiguracionAws.getInstancia().isActivo()) {
            return texto;
        }
        if (!SelectorImagen.esRutaLocal(texto)) {
            return texto;
        }
        File archivo = new File(texto.trim());
        if (texto.trim().toLowerCase().startsWith("file:")) {
            try {
                archivo = new File(new java.net.URI(texto.trim()));
            } catch (Exception e) {
                throw new IOException("La ruta de la imagen no es válida");
            }
        }
        if (!archivo.isFile()) {
            throw new IOException("No se encontró el archivo de la imagen");
        }
        if (archivo.length() > BYTES_MAXIMOS) {
            throw new IOException("La imagen pesa más de 5 MB");
        }

        String clave = "imagenes/" + carpeta + "/" + nombreEnS3(archivo.getName());
        subir(archivo, clave);
        return "/" + clave;
    }

    // Nombre único y sin espacios ni acentos: 20261118-153000-pastel-de-fresa.jpg
    private static String nombreEnS3(String nombreOriginal) {
        String extension = "";
        String base = nombreOriginal;
        int punto = nombreOriginal.lastIndexOf('.');
        if (punto > 0) {
            extension = nombreOriginal.substring(punto).toLowerCase();
            base = nombreOriginal.substring(0, punto);
        }
        // Quita acentos (á → a) y deja solo letras, números y guiones
        String sinAcentos = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String limpio = sinAcentos.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        limpio = limpio.replaceAll("^-+", "").replaceAll("-+$", "");
        if (limpio.isEmpty()) {
            limpio = "foto";
        }
        if (limpio.length() > 60) {
            limpio = limpio.substring(0, 60);
        }
        return FECHA_NOMBRE.format(LocalDateTime.now()) + "-" + limpio + extension;
    }

    // Tipo de archivo para que el navegador muestre la foto en lugar de descargarla
    private static String tipoDeContenido(String clave) {
        String minusculas = clave.toLowerCase();
        if (minusculas.endsWith(".png")) {
            return "image/png";
        }
        if (minusculas.endsWith(".gif")) {
            return "image/gif";
        }
        if (minusculas.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (minusculas.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    // Ejecuta: aws s3 cp archivo s3://bucket/clave --profile ... --region ...
    private static void subir(File archivo, String clave) throws IOException {
        ConfiguracionAws configuracion = ConfiguracionAws.getInstancia();
        File aws = configuracion.buscarAwsCli();
        if (aws == null) {
            throw new IOException("No se encontró AWS CLI (aws.exe). Ejecuta 1_INSTALAR_PRIMERA_VEZ.bat (opción AWS)");
        }
        if (configuracion.getBucketImagenes().isEmpty() || configuracion.getPerfil().isEmpty()
                || configuracion.getRegion().isEmpty()) {
            throw new IOException("Faltan el bucket, el perfil o la región en " + configuracion.getArchivo().getPath());
        }
        List<String> comando = new ArrayList<String>();
        comando.add(aws.getAbsolutePath());
        comando.add("s3");
        comando.add("cp");
        comando.add(archivo.getAbsolutePath());
        comando.add("s3://" + configuracion.getBucketImagenes() + "/" + clave);
        comando.add("--profile");
        comando.add(configuracion.getPerfil());
        comando.add("--region");
        comando.add(configuracion.getRegion());
        comando.add("--content-type");
        comando.add(tipoDeContenido(clave));
        // El nombre lleva la fecha, así que el archivo nunca cambia: se puede guardar en caché un año
        comando.add("--cache-control");
        comando.add("public,max-age=31536000");
        comando.add("--only-show-errors");

        File bitacora = File.createTempFile("carmir_s3", ".log");
        try {
            ProcessBuilder constructor = new ProcessBuilder(comando);
            constructor.redirectErrorStream(true);
            constructor.redirectOutput(bitacora);
            Process proceso = constructor.start();
            boolean termino = proceso.waitFor(SEGUNDOS_MAXIMOS, TimeUnit.SECONDS);
            if (!termino) {
                proceso.destroyForcibly();
                throw new IOException("La subida de la imagen tardó demasiado. Revisa la conexión a internet");
            }
            if (proceso.exitValue() != 0) {
                throw new IOException("No se pudo subir la imagen a AWS: " + ultimaLinea(bitacora));
            }
        } catch (InterruptedException e) {
            throw new IOException("Se interrumpió la subida de la imagen");
        } finally {
            if (!bitacora.delete()) {
                bitacora.deleteOnExit();
            }
        }
    }

    // Última línea con texto de lo que escribió AWS CLI
    private static String ultimaLinea(File bitacora) throws IOException {
        List<String> lineas = Files.readAllLines(bitacora.toPath(), StandardCharsets.UTF_8);
        String mensaje = "error desconocido";
        for (String linea : lineas) {
            if (!linea.isBlank()) {
                mensaje = linea.trim();
            }
        }
        return mensaje;
    }
}
