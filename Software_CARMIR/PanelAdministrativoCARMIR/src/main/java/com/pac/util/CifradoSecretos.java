package com.pac.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra y descifra los secretos de las integraciones (token de WhatsApp y contraseña del correo)
 * antes de guardarlos en la base de datos. Así no quedan en claro en la tabla ni en los respaldos .sql.
 *
 * Usa AES-GCM (viene incluido en Java, no necesita librerías).
 * La llave se crea sola la primera vez y se guarda en la carpeta del usuario de Windows:
 *   C:\Users\<usuario>\.carmir\clave-secretos.key
 * Si se borra esa llave, o se abre la base de datos en otra computadora, los secretos ya no se pueden
 * leer y hay que volver a escribirlos en Configuración → Respaldos y sistema.
 */
public class CifradoSecretos {

    // Prefijo para reconocer un valor cifrado por esta clase
    private static final String PREFIJO = "aes1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_VECTOR = 12;      // tamaño del vector inicial de GCM
    private static final int BITS_ETIQUETA = 128;    // tamaño de la etiqueta de verificación

    private static SecretKey llave;

    // Clase de apoyo: no se crean objetos de ella
    private CifradoSecretos() {
    }

    // Devuelve el texto cifrado (con el prefijo). Un texto vacío se queda vacío.
    public static String cifrar(String texto) throws GeneralSecurityException {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        byte[] vector = new byte[BYTES_VECTOR];
        new SecureRandom().nextBytes(vector);

        Cipher cifrador = Cipher.getInstance(ALGORITMO);
        cifrador.init(Cipher.ENCRYPT_MODE, obtenerLlave(), new GCMParameterSpec(BITS_ETIQUETA, vector));
        byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));

        // Se guardan juntos: primero el vector y después el texto cifrado
        byte[] todo = new byte[vector.length + cifrado.length];
        System.arraycopy(vector, 0, todo, 0, vector.length);
        System.arraycopy(cifrado, 0, todo, vector.length, cifrado.length);
        return PREFIJO + Base64.getEncoder().encodeToString(todo);
    }

    // Devuelve el texto original, o null si no se pudo descifrar (otra llave u otro equipo)
    public static String descifrar(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        if (!valor.startsWith(PREFIJO)) {
            return null;
        }
        try {
            byte[] todo = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            if (todo.length <= BYTES_VECTOR) {
                return null;
            }
            byte[] vector = new byte[BYTES_VECTOR];
            System.arraycopy(todo, 0, vector, 0, BYTES_VECTOR);

            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.DECRYPT_MODE, obtenerLlave(), new GCMParameterSpec(BITS_ETIQUETA, vector));
            byte[] original = cifrador.doFinal(todo, BYTES_VECTOR, todo.length - BYTES_VECTOR);
            return new String(original, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ===================== Llave =====================

    // Lee la llave del archivo; si no existe la crea
    private static synchronized SecretKey obtenerLlave() throws GeneralSecurityException {
        if (llave != null) {
            return llave;
        }
        File archivo = archivoLlave();
        try {
            if (archivo.exists()) {
                String texto = new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8).trim();
                llave = new SecretKeySpec(Base64.getDecoder().decode(texto), "AES");
                return llave;
            }
            KeyGenerator generador = KeyGenerator.getInstance("AES");
            generador.init(256);
            SecretKey nueva = generador.generateKey();
            archivo.getParentFile().mkdirs();
            Files.write(archivo.toPath(), Base64.getEncoder().encode(nueva.getEncoded()));
            llave = nueva;
            return llave;
        } catch (IOException e) {
            throw new GeneralSecurityException("No se pudo leer ni crear la llave " + archivo.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("La llave " + archivo.getAbsolutePath() + " está dañada");
        }
    }

    // C:\Users\<usuario>\.carmir\clave-secretos.key
    private static File archivoLlave() {
        File carpeta = new File(System.getProperty("user.home"), ".carmir");
        return new File(carpeta, "clave-secretos.key");
    }
}
