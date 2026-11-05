package com.pac.modulos.configuracion.model;

/**
 * Nombres de los parámetros de la tabla Configuracion.
 * Deben escribirse igual que en la columna Configuracion.Clave (ver MIGRACION_COMPLETA.sql).
 */
public class ClavesConfiguracion {

    // Negocio
    public static final String NOMBRE_NEGOCIO = "NombreNegocio";

    // Pedidos y entregas (Módulo 4 y tienda en línea)
    public static final String CAPACIDAD_DIARIA = "CapacidadDiaria";
    public static final String DIAS_PARA_PREPARAR = "DiasParaPreparar";
    public static final String DIAS_MAXIMOS_AGENDA = "DiasMaximosAgenda";
    public static final String DIAS_ANTICIPACION_MINIMA = "DiasAnticipacionMinima";
    public static final String HORA_APERTURA = "HoraApertura";
    public static final String HORA_CIERRE = "HoraCierre";
    public static final String DIAS_LABORABLES = "DiasLaborables";

    // Pagos
    public static final String PORCENTAJE_ANTICIPO = "PorcentajeAnticipo";
    public static final String DATOS_BANCARIOS = "DatosBancarios";

    // Inventario (Módulo 5)
    public static final String DIAS_AVISO_CADUCIDAD = "DiasAvisoCaducidad";

    // Seguridad (Módulo 7)
    public static final String INTENTOS_MAXIMOS_LOGIN = "IntentosMaximosLogin";
    public static final String MINUTOS_INACTIVIDAD = "MinutosInactividadSesion";

    // Letras de los días de la semana, de lunes a domingo (DiasLaborables = "L,M,X,J,V,S")
    public static final String[] LETRAS_DIAS = {"L", "M", "X", "J", "V", "S", "D"};

    // Clase de constantes: no se crean objetos de ella
    private ClavesConfiguracion() {
    }
}
