package com.pac.navigation;

import com.pac.modulos.usuarios.model.Permisos;

/**
 * Apartados del menú lateral del panel.
 * Están agrupados según los módulos del documento Prompts_Modulos_CARMIR.docx:
 * M3 Catálogo de Personalización, M4 Gestor de Pedidos y Producción,
 * M5 Inventario y Recetas, M6 Reportes Financieros y M7 Gestión de Usuarios.
 *
 * Los permisos están en la clase Permisos (igual que en la columna Permiso.NombrePermiso).
 * Si el archivo FXML es null, se muestra la pantalla "en construcción".
 * El FXML puede ir en la carpeta de su módulo, por ejemplo "usuarios/usuarios.fxml".
 */
public enum SeccionPanel {

    //          texto del menú, título de la pantalla, icono (respaldo), JSON de Lottie, cuadro de reposo,
    //          módulo, permiso, FXML, funciones previstas
    DASHBOARD("Dashboard Principal", "Panel de Control", "bi-speedometer2",
            "dashboard.json", 0,
            "General", null, "dashboard.fxml",
            new String[] {"Resumen del día: pedidos, ingresos y alertas de inventario"}),

    PEDIDOS("Gestión de Pedidos", "Gestión de Pedidos", "bi-basket-fill",
            "pedidos.json", 0,
            "Módulo 4 · Gestor de Pedidos y Producción", Permisos.GESTIONAR_PEDIDOS, "pedidos/pedidos.fxml",
            new String[] {"RF-36 Historial de pedidos", "RF-37 Filtrar pedidos por estados",
                    "RF-33 Visualización de pedidos", "RF-38 Aprobación de pedidos",
                    "RF-39 Cancelación de pedidos", "RF-40/41 Estados del pedido",
                    "RF-47 Notificaciones por WhatsApp y correo"}),

    AGENDA("Agenda de Producción", "Agenda de Producción", "bi-calendar-check-fill",
            "agenda.json", 44,
            "Módulo 4 · Gestor de Pedidos y Producción", Permisos.VER_AGENDA, "pedidos/agenda.fxml",
            new String[] {"RF-45 Visualización de agenda de producción",
                    "RF-46 Filtrar vistas de la agenda por periodo"}),

    PAGOS("Validación de Pagos", "Validación de Pagos", "bi-credit-card-fill",
            "pagos.json", 46,
            "Módulo 4 · Gestor de Pedidos y Producción", Permisos.VALIDAR_PAGOS, "pedidos/pagos.fxml",
            new String[] {"RF-42 Validación de pagos", "RF-43 Seguimiento de saldos y pagos",
                    "RF-44 Bloqueo de preparación sin pago confirmado"}),

    CATALOGO("Catálogo de Postres", "Catálogo y Personalización", "bi-palette-fill",
            "catalogo.json", 0,
            "Módulo 3 · Catálogo de Personalización", Permisos.GESTIONAR_CATALOGO, "catalogo/catalogo.fxml",
            new String[] {"RF-15 a RF-18 Categorías", "Características y opciones",
                    "Exclusiones entre opciones", "RF-28 a RF-31 Productos"}),

    INVENTARIO("Inventario de Insumos", "Control de Inventario", "bi-box-seam",
            "inventario.json", 0,
            "Módulo 5 · Inventario y Recetas", Permisos.GESTIONAR_INSUMOS, "inventario/inventario.fxml",
            new String[] {"RF-51 a RF-54 Insumos", "RF-59 Entrada de insumos",
                    "RF-60 Salida de insumos", "RF-61 Alerta de existencias bajas",
                    "RF-62 Alerta de caducidad próxima"}),

    RECETAS("Recetas", "Gestión de Recetas", "bi-journal-bookmark-fill",
            "recetas.json", 0,
            "Módulo 5 · Inventario y Recetas", Permisos.GESTIONAR_RECETAS, "inventario/recetas.fxml",
            new String[] {"RF-55 a RF-58 Recetas", "RF-63 Descuento automático de insumos"}),

    REPORTES("Reportes Financieros", "Reportes Financieros", "bi-bar-chart-line-fill",
            "reportes.json", 46,
            "Módulo 6 · Reportes Financieros", Permisos.VER_DATOS_FINANCIEROS, "reportes/reportes.fxml",
            new String[] {"RF-65 Reporte de ingresos y gastos",
                    "RF-66 Productos más vendidos", "RF-67 Exportación a Excel"}),

    USUARIOS("Usuarios y Permisos", "Usuarios y Permisos", "bi-people-fill",
            "usuarios.json", 0,
            "Módulo 7 · Gestión de Usuarios", Permisos.GESTIONAR_USUARIOS, "usuarios/usuarios.fxml",
            new String[] {"Alta, edición y baja de personal", "Asignación de permisos por módulo",
                    "Sesión única por usuario"}),

    CONFIGURACION("Configuración", "Configuración del Sistema", "bi-gear-fill",
            "configuracion.json", 0,
            "General", Permisos.CONFIGURAR_SISTEMA, "configuracion/configuracion.fxml",
            new String[] {"Parámetros del negocio y de producción", "Cobertura de envío por código postal",
                    "Historial de cambios", "Respaldos (RNF-8) y conexión a la base de datos"});

    // Pantalla que se usa mientras un apartado no tiene la suya
    private static final String FXML_EN_CONSTRUCCION = "seccion-en-construccion.fxml";

    private final String textoMenu;
    private final String tituloPantalla;
    private final String icono;
    private final String archivoLottie;
    private final double cuadroReposo;
    private final String modulo;
    private final String permisoRequerido;
    private final String archivoFxml;
    private final String[] funciones;

    SeccionPanel(String textoMenu, String tituloPantalla, String icono, String archivoLottie, double cuadroReposo, String modulo,
                 String permisoRequerido, String archivoFxml, String[] funciones) {
        this.textoMenu = textoMenu;
        this.tituloPantalla = tituloPantalla;
        this.icono = icono;
        this.archivoLottie = archivoLottie;
        this.cuadroReposo = cuadroReposo;
        this.modulo = modulo;
        this.permisoRequerido = permisoRequerido;
        this.archivoFxml = archivoFxml;
        this.funciones = funciones;
    }

    public String getTextoMenu() {
        return textoMenu;
    }

    public String getTituloPantalla() {
        return tituloPantalla;
    }

    // Código del icono de Ikonli Bootstrap Icons, por ejemplo "bi-gear-fill" (se usa si Lottie no carga)
    public String getIcono() {
        return icono;
    }

    // JSON de Lottie (carpeta resources/com/pac/lottie) con el icono animado del menú
    public String getArchivoLottie() {
        return archivoLottie;
    }

    // Cuadro de la animación que se ve cuando el mouse no está encima
    public double getCuadroReposo() {
        return cuadroReposo;
    }

    public String getModulo() {
        return modulo;
    }

    // null significa que cualquier usuario con sesión puede entrar
    public String getPermisoRequerido() {
        return permisoRequerido;
    }

    public String[] getFunciones() {
        return funciones;
    }

    // Ruta del FXML del apartado; si no tiene uno propio, usa el de "en construcción"
    public String getRutaFxml() {
        if (archivoFxml == null) {
            return "/com/pac/view/" + FXML_EN_CONSTRUCCION;
        }
        return "/com/pac/view/" + archivoFxml;
    }
}
