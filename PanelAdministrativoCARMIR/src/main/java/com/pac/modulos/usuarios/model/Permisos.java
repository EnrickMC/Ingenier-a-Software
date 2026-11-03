package com.pac.modulos.usuarios.model;

/**
 * Nombres de los permisos del sistema.
 * Deben escribirse igual que en la columna Permiso.NombrePermiso.
 * Si se agrega un permiso nuevo, se pone aquí y en el arreglo TODOS;
 * el módulo de usuarios lo da de alta solo en la base de datos.
 */
public class Permisos {

    public static final String GESTIONAR_USUARIOS = "Gestionar usuarios";
    public static final String GESTIONAR_PEDIDOS = "Gestionar pedidos";
    public static final String VER_AGENDA = "Ver agenda de producción";
    public static final String VALIDAR_PAGOS = "Validar pagos";
    public static final String GESTIONAR_CATALOGO = "Gestionar catálogo";
    public static final String GESTIONAR_PRODUCTOS = "Gestionar productos";
    public static final String GESTIONAR_INSUMOS = "Gestionar insumos";
    public static final String GESTIONAR_RECETAS = "Gestionar recetas";
    public static final String VER_DATOS_FINANCIEROS = "Ver datos financieros";
    public static final String CONFIGURAR_SISTEMA = "Configurar sistema";

    // Lista completa, en el orden en que aparecen en el formulario de usuarios
    public static final String[] TODOS = {
            GESTIONAR_USUARIOS,
            GESTIONAR_PEDIDOS,
            VER_AGENDA,
            VALIDAR_PAGOS,
            GESTIONAR_CATALOGO,
            GESTIONAR_PRODUCTOS,
            GESTIONAR_INSUMOS,
            GESTIONAR_RECETAS,
            VER_DATOS_FINANCIEROS,
            CONFIGURAR_SISTEMA
    };

    // Clase de constantes: no se crean objetos de ella
    private Permisos() {
    }
}
