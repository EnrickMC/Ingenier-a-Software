package com.pac.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PATRÓN SINGLETON.
 * Guarda los parámetros generales del panel.
 *
 * De dónde sale cada valor (en este orden):
 *   1. La tabla Configuracion de la base de datos (la edita el apartado "Configuración").
 *      ConfiguracionService la lee al abrir el panel y la pasa aquí con aplicarValoresBD().
 *   2. Si la base no tiene el parámetro (o todavía no se ejecuta MIGRACION_COMPLETA.sql),
 *      se usa config/app.properties.
 *   3. Si tampoco está ahí, el valor por defecto escrito en este archivo.
 */
public class ConfiguracionApp {

    private static final String ARCHIVO_CONFIG = "/com/pac/config/app.properties";

    // La única instancia de la clase
    private static ConfiguracionApp instancia;

    private Properties propiedades;     // valores de app.properties
    private Properties valoresBD;       // valores de la tabla Configuracion (Clave → Valor)

    // Constructor privado: lee el archivo una sola vez
    private ConfiguracionApp() {
        propiedades = new Properties();
        valoresBD = new Properties();
        try {
            InputStream archivo = ConfiguracionApp.class.getResourceAsStream(ARCHIVO_CONFIG);
            if (archivo != null) {
                propiedades.load(archivo);
                archivo.close();
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer " + ARCHIVO_CONFIG);
        }
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized ConfiguracionApp getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionApp();
        }
        return instancia;
    }

    // Reemplaza los valores que vienen de la tabla Configuracion (Clave → Valor)
    public synchronized void aplicarValoresBD(Properties valores) {
        valoresBD = new Properties();
        valoresBD.putAll(valores);
    }

    // true si ya se cargaron valores de la base de datos
    public boolean usaValoresBD() {
        return !valoresBD.isEmpty();
    }

    // ----- Parámetros que no se guardan en la base de datos -----

    // true = el inicio de sesión no consulta MySQL y siempre deja entrar
    public boolean isModoDemo() {
        String valor = propiedades.getProperty("app.modoDemo", "true");
        return valor.equalsIgnoreCase("true");
    }

    // Carpeta donde están mysqldump y mysql (respaldos, RNF-8); vacía = se buscan solos
    public String getCarpetaMysql() {
        return propiedades.getProperty("respaldos.carpetaMysql", "").trim();
    }

    // ----- Parámetros del negocio (tabla Configuracion) -----

    // Nombre del negocio que se muestra en las pantallas
    public String getNombreNegocio() {
        String valor = leerTexto("NombreNegocio", "app.nombreNegocio", "Delicias de Postres");
        if (valor.isBlank()) {
            return "Delicias de Postres";
        }
        return valor;
    }

    // Intentos fallidos seguidos antes de bloquear una cuenta del Personal
    public int getIntentosMaximosLogin() {
        return leerEntero("IntentosMaximosLogin", "app.intentosMaximosLogin", 3, 1);
    }

    // Minutos sin usar el panel antes de cerrar la sesión; 0 = nunca
    public int getMinutosInactividadSesion() {
        return leerEntero("MinutosInactividadSesion", "app.minutosInactividadSesion", 30, 0);
    }

    // Días antes de la caducidad de un lote en que se genera la alerta "Próxima caducidad" (RF-62)
    public int getDiasAvisoCaducidad() {
        return leerEntero("DiasAvisoCaducidad", "inventario.diasAvisoCaducidad", 3, 0);
    }

    // Módulo 4: pedidos que caben en un día antes de avisar "Agenda saturada" (RF-33)
    public int getCapacidadDiaria() {
        return leerEntero("CapacidadDiaria", "pedidos.capacidadDiaria", 6, 1);
    }

    // Módulo 4: días antes de la entrega en que un pedido debe pasar a preparación (RF-40, RF-45)
    public int getDiasParaPreparar() {
        return leerEntero("DiasParaPreparar", "pedidos.diasParaPreparar", 2, 1);
    }

    // Módulo 4: días máximos de un rango personalizado en la agenda (RF-46)
    public int getDiasMaximosAgenda() {
        return leerEntero("DiasMaximosAgenda", "pedidos.diasMaximosAgenda", 62, 1);
    }

    // ----- Lectura de valores -----

    // Texto del parámetro: primero la base de datos, luego app.properties, luego el valor por defecto
    public synchronized String leerTexto(String claveBD, String claveArchivo, String porDefecto) {
        String valor = valoresBD.getProperty(claveBD);
        if (valor == null && claveArchivo != null) {
            valor = propiedades.getProperty(claveArchivo);
        }
        if (valor == null) {
            return porDefecto;
        }
        return valor.trim();
    }

    // Número entero del parámetro; si no es válido o es menor que el mínimo usa el valor por defecto
    private int leerEntero(String claveBD, String claveArchivo, int porDefecto, int minimo) {
        String valor = leerTexto(claveBD, claveArchivo, String.valueOf(porDefecto));
        try {
            int numero = Integer.parseInt(valor);
            if (numero < minimo) {
                return porDefecto;
            }
            return numero;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
