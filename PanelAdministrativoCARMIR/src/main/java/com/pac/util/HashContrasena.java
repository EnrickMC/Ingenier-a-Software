package com.pac.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Cifra contraseñas para guardarlas en la columna Usuario.`Contraseña`.
 *
 * Usa PBKDF2 (viene incluido en Java, no necesita librerías) con una "sal" aleatoria,
 * así dos personas con la misma contraseña tienen un hash distinto.
 * El texto guardado tiene esta forma:  pbkdf2$iteraciones$sal$hash
 *
 * También acepta hashes SHA-256 simples (64 caracteres hexadecimales) por si
 * se crean usuarios a mano en MySQL con SHA2('contraseña', 256).
 */
public class HashContrasena {

    private static final String PREFIJO = "pbkdf2";
    private static final int ITERACIONES = 120000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;

    // Devuelve el hash de la contraseña listo para guardarse en la base de datos
    public static String generar(String contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        SecureRandom aleatorio = new SecureRandom();
        aleatorio.nextBytes(sal);

        byte[] hash = calcularPbkdf2(contrasena, sal, ITERACIONES);

        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + base64.encodeToString(sal) + "$" + base64.encodeToString(hash);
    }

    // Compara la contraseña escrita con el hash guardado en la base de datos
    public static boolean coincide(String contrasena, String hashGuardado) {
        if (contrasena == null || hashGuardado == null) {
            return false;
        }

        // Hash nuevo (PBKDF2)
        if (hashGuardado.startsWith(PREFIJO + "$")) {
            String[] partes = hashGuardado.split("\\$");
            if (partes.length != 4) {
                return false;
            }
            int iteraciones = Integer.parseInt(partes[1]);
            Base64.Decoder base64 = Base64.getDecoder();
            byte[] sal = base64.decode(partes[2]);
            byte[] hashEsperado = base64.decode(partes[3]);
            byte[] hashEscrito = calcularPbkdf2(contrasena, sal, iteraciones);
            // MessageDigest.isEqual compara sin dar pistas por el tiempo que tarda
            return MessageDigest.isEqual(hashEsperado, hashEscrito);
        }

        // Hash antiguo (SHA-256 en hexadecimal)
        String hashEscrito = generarSha256(contrasena);
        return hashEscrito.equalsIgnoreCase(hashGuardado);
    }

    // Calcula PBKDF2 con HMAC-SHA256
    private static byte[] calcularPbkdf2(String contrasena, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec especificacion = new PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, BITS_HASH);
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = fabrica.generateSecret(especificacion).getEncoded();
            especificacion.clearPassword();
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("PBKDF2 no está disponible", e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("No se pudo cifrar la contraseña", e);
        }
    }

    // Devuelve el hash SHA-256 de la contraseña en texto hexadecimal
    private static String generarSha256(String contrasena) {
        try {
            MessageDigest algoritmo = MessageDigest.getInstance("SHA-256");
            byte[] bytes = algoritmo.digest(contrasena.getBytes(StandardCharsets.UTF_8));

            StringBuilder texto = new StringBuilder();
            for (byte b : bytes) {
                texto.append(String.format("%02x", b));
            }
            return texto.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no está disponible", e);
        }
    }
}
