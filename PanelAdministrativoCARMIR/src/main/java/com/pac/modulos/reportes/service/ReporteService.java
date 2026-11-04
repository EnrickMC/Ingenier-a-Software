package com.pac.modulos.reportes.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.reportes.dao.ReporteDAO;
import com.pac.modulos.reportes.model.RenglonDia;
import com.pac.modulos.reportes.model.RenglonVenta;
import com.pac.modulos.reportes.model.ReporteIngresosGastos;
import com.pac.modulos.reportes.model.ReporteVentas;
import com.pac.util.Formatos;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de los reportes financieros (Módulo 6):
 * RF-65 Ingresos y gastos, RF-66 Productos más vendidos.
 * Los mensajes son los de las excepciones del documento de requerimientos.
 */
public class ReporteService {

    // ----- Mensajes de las excepciones (RF-65 y RF-66) -----
    public static final String MSG_SIN_PERMISO_FINANCIERO = "No tienes permiso para consultar reportes financieros";
    public static final String MSG_SIN_PERMISO_REPORTES = "No tienes permiso para consultar reportes";
    public static final String MSG_FECHA_VACIA = "Captura la fecha";
    public static final String MSG_FECHA_INVALIDA = "Fecha inválida";
    public static final String MSG_RANGO_INVALIDO = "Rango de fechas inválido";
    public static final String MSG_INICIO_FUTURO = "El periodo no puede iniciar en una fecha futura";
    public static final String MSG_FIN_AJUSTADO = "La fecha de fin era posterior a hoy; se ajustó a hoy";
    public static final String MSG_ERROR_CONEXION = "Error de conexión: no fue posible generar el reporte";
    public static final String MSG_SIN_REGISTROS = "Sin registros en el periodo seleccionado";
    public static final String MSG_SIN_INGRESOS = "Sin ingresos en el periodo: la sección de ingresos aparece en $0.00";
    public static final String MSG_SIN_GASTOS = "Sin gastos en el periodo: la sección de gastos aparece en $0.00";
    public static final String MSG_PERDIDA = "Pérdida en el periodo";
    public static final String MSG_POR_REVISAR = "Hay movimientos que no son consistentes: no se sumaron y aparecen en «Movimientos por revisar»";
    public static final String MSG_SIN_VENTAS = "Sin registros de ventas en el periodo";
    public static final String MSG_CATEGORIA_NO_EXISTE = "La categoría seleccionada ya no existe";
    public static final String MSG_PRODUCTO_INACTIVO = "Producto inactivo";
    public static final String MSG_VENTAS_INCOMPLETAS = "venta(s) con datos incompletos (sin precio) no se incluyeron";

    // Opciones de orden del reporte de más vendidos
    public static final String ORDEN_UNIDADES = "Unidades vendidas";
    public static final String ORDEN_UTILIDAD = "Utilidad";

    // Hasta este número de días la gráfica se agrupa por semana; con más, por mes
    private static final int DIAS_MAXIMOS_POR_SEMANA = 92;

    private static final Locale ESPANOL = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FORMATO_SEMANA = DateTimeFormatter.ofPattern("dd/MM", ESPANOL);
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MMM yyyy", ESPANOL);

    private ReporteDAO reporteDAO;

    public ReporteService() {
        reporteDAO = DAOFactory.crearReporteDAO();
    }

    // ===================== Paso 3: revisar el periodo =====================

    /**
     * Revisa las fechas del periodo en el orden del caso de uso.
     * desdeInvalida / hastaInvalida = true si el texto escrito no es una fecha.
     */
    // RF-65 / RF-66 · Paso 3: valida el formato y el rango del periodo (lo usan los dos reportes).
    public RevisionPeriodo revisarPeriodo(LocalDate desde, boolean desdeInvalida,
                                          LocalDate hasta, boolean hastaInvalida) {
        // ¿Las fechas tienen un formato válido?
        if (desdeInvalida && hastaInvalida) {
            // RF-65 / RF-66 · Excepción paso 3: «Fecha inválida».
            return RevisionPeriodo.error(MSG_FECHA_INVALIDA, RevisionPeriodo.CAMPO_AMBOS);
        }
        if (desdeInvalida) {
            return RevisionPeriodo.error(MSG_FECHA_INVALIDA, RevisionPeriodo.CAMPO_DESDE);
        }
        if (hastaInvalida) {
            return RevisionPeriodo.error(MSG_FECHA_INVALIDA, RevisionPeriodo.CAMPO_HASTA);
        }
        // ¿Se capturaron ambas fechas?
        if (desde == null && hasta == null) {
            // RF-65 / RF-66 · Excepción paso 3: falta capturar una fecha.
            return RevisionPeriodo.error(MSG_FECHA_VACIA, RevisionPeriodo.CAMPO_AMBOS);
        }
        if (desde == null) {
            return RevisionPeriodo.error(MSG_FECHA_VACIA, RevisionPeriodo.CAMPO_DESDE);
        }
        if (hasta == null) {
            return RevisionPeriodo.error(MSG_FECHA_VACIA, RevisionPeriodo.CAMPO_HASTA);
        }
        // ¿La fecha de inicio es mayor que la fecha de fin?
        if (desde.isAfter(hasta)) {
            // RF-65 / RF-66 · Excepción paso 3: «Rango de fechas inválido».
            return RevisionPeriodo.error(MSG_RANGO_INVALIDO, RevisionPeriodo.CAMPO_AMBOS);
        }
        // ¿La fecha de inicio es posterior a hoy?
        LocalDate hoy = LocalDate.now();
        if (desde.isAfter(hoy)) {
            // RF-65 / RF-66 · Excepción paso 3: «El periodo no puede iniciar en una fecha futura».
            return RevisionPeriodo.error(MSG_INICIO_FUTURO, RevisionPeriodo.CAMPO_DESDE);
        }
        // ¿La fecha de fin es posterior a hoy? Se ajusta a hoy y se avisa
        if (hasta.isAfter(hoy)) {
            // RF-65 / RF-66 · Excepción paso 3: la fecha de fin se ajusta a hoy y se avisa.
            return RevisionPeriodo.correcto(desde, hoy, MSG_FIN_AJUSTADO + " (" + Formatos.fecha(hoy) + ")");
        }
        return RevisionPeriodo.correcto(desde, hasta, null);
    }

    // ===================== RF-65 · Ingresos y gastos =====================

    // Consulta y calcula el reporte de ingresos y gastos del periodo (ya revisado)
    // RF-65 (Generación de reporte de ingresos y gastos): inicia aquí la consulta. Pasos 4 a 6: pagos validados, compras y mermas; totales y utilidad.
    public ReporteIngresosGastos generarIngresosGastos(LocalDate desde, LocalDate hasta) throws DAOException {
        ReporteIngresosGastos reporte = new ReporteIngresosGastos(desde, hasta);
        reporteDAO.cargarIngresosGastos(reporte);
        return reporte;
    }

    // Ingresos del mes en curso (tarjeta del dashboard)
    public double ingresosDelMes() throws DAOException {
        LocalDate hoy = LocalDate.now();
        return generarIngresosGastos(hoy.withDayOfMonth(1), hoy).getIngresos();
    }

    // Avisos que acompañan al reporte (sección faltante, pagos por reembolsar, movimientos por revisar)
    // RF-65 · Paso 5: revisa que haya transacciones y que sean consistentes.
    public List<String> avisosDe(ReporteIngresosGastos reporte) {
        List<String> avisos = new ArrayList<String>();
        if (reporte.estaVacio()) {
            // RF-65 · Excepción paso 5: «Sin registros en el periodo seleccionado».
            avisos.add(MSG_SIN_REGISTROS);
            return avisos;
        }
        if (!reporte.hayIngresos()) {
            // RF-65 · Excepción paso 5: falta la sección de ingresos o de gastos; se muestra en $0.00.
            avisos.add(MSG_SIN_INGRESOS);
        }
        if (!reporte.hayGastos()) {
            avisos.add(MSG_SIN_GASTOS);
        }
        if (reporte.getPorReembolsar() > 0) {
            avisos.add("Pagos de pedidos cancelados con reembolso pendiente: " + Formatos.moneda(reporte.getPorReembolsar())
                    + " (no se cuentan como ingreso)");
        }
        if (!reporte.getPorRevisar().isEmpty()) {
            // RF-65 · Excepción paso 5: movimientos inconsistentes en «Movimientos por revisar».
            avisos.add(MSG_POR_REVISAR);
        }
        return avisos;
    }

    // Barras de la gráfica: por semana (lunes a domingo) o por mes si el periodo es largo
    public List<RenglonDia> agruparParaGrafica(ReporteIngresosGastos reporte) {
        long dias = ChronoUnit.DAYS.between(reporte.getDesde(), reporte.getHasta()) + 1;
        boolean porSemana = dias <= DIAS_MAXIMOS_POR_SEMANA;

        // Se crea una barra por cada semana o mes del periodo, aunque no tenga movimientos
        List<RenglonDia> barras = new ArrayList<RenglonDia>();
        LocalDate inicio = inicioDeGrupo(reporte.getDesde(), porSemana);
        while (!inicio.isAfter(reporte.getHasta())) {
            RenglonDia barra = new RenglonDia(inicio);
            if (porSemana) {
                barra.setEtiqueta("Sem. " + FORMATO_SEMANA.format(inicio));
            } else {
                String mes = FORMATO_MES.format(inicio);
                barra.setEtiqueta(mes.substring(0, 1).toUpperCase() + mes.substring(1));
            }
            barras.add(barra);
            if (porSemana) {
                inicio = inicio.plusWeeks(1);
            } else {
                inicio = inicio.plusMonths(1);
            }
        }

        // Cada día suma en la barra de su semana o mes
        for (RenglonDia dia : reporte.getDias()) {
            LocalDate grupo = inicioDeGrupo(dia.getFecha(), porSemana);
            for (RenglonDia barra : barras) {
                if (barra.getFecha().equals(grupo)) {
                    barra.sumar(dia);
                }
            }
        }
        return barras;
    }

    // true si la gráfica se agrupa por semana (para el título)
    public boolean graficaPorSemana(ReporteIngresosGastos reporte) {
        long dias = ChronoUnit.DAYS.between(reporte.getDesde(), reporte.getHasta()) + 1;
        return dias <= DIAS_MAXIMOS_POR_SEMANA;
    }

    // Lunes de la semana o día 1 del mes de una fecha
    private LocalDate inicioDeGrupo(LocalDate fecha, boolean porSemana) {
        if (porSemana) {
            int diasDesdeLunes = fecha.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            return fecha.minusDays(diasDesdeLunes);
        }
        return fecha.withDayOfMonth(1);
    }

    // ===================== RF-66 · Productos más vendidos =====================

    // true si la categoría elegida todavía existe (paso 3 de RF-66)
    // RF-66 · Excepción paso 3: revisa que la categoría elegida exista.
    public boolean existeCategoria(int idCategoria) throws DAOException {
        if (idCategoria <= 0) {
            return true;
        }
        return reporteDAO.existeCategoria(idCategoria);
    }

    /**
     * Consulta las ventas del periodo agrupadas por producto, categoría y opción.
     * idCategoria = 0 para todas; orden = ORDEN_UNIDADES u ORDEN_UTILIDAD.
     */
    // RF-66 (Generación de reporte de productos más vendidos): inicia aquí la consulta. Pasos 4 y 6: agrupa, ordena y calcula importe y porcentaje.
    public ReporteVentas generarVentas(LocalDate desde, LocalDate hasta, int idCategoria,
                                       String nombreCategoria, String orden) throws DAOException {
        ReporteVentas reporte = new ReporteVentas(desde, hasta);
        reporte.setIdCategoria(idCategoria);
        reporte.setNombreCategoria(nombreCategoria);
        reporte.setOrden(orden);
        reporteDAO.cargarVentas(reporte, ORDEN_UTILIDAD.equals(orden));

        // Porcentaje de los ingresos del periodo que aporta cada renglón
        double total = reporte.getIngresosTotales();
        calcularPorcentajes(reporte.getPorProducto(), total);
        calcularPorcentajes(reporte.getPorCategoria(), total);
        calcularPorcentajes(reporte.getPorOpcion(), total);
        return reporte;
    }

    // Avisos del reporte de ventas (sin ventas, ventas incompletas, productos inactivos)
    // RF-66 · Paso 5: revisa que haya ventas y que estén completas.
    public List<String> avisosDe(ReporteVentas reporte) {
        List<String> avisos = new ArrayList<String>();
        if (!reporte.tieneDatos()) {
            // RF-66 · Excepción paso 5: «Sin registros de ventas en el periodo».
            avisos.add(MSG_SIN_VENTAS);
        }
        if (reporte.getLineasIncompletas() > 0) {
            // RF-66 · Excepción paso 5: ventas incompletas excluidas.
            avisos.add(reporte.getLineasIncompletas() + " " + MSG_VENTAS_INCOMPLETAS);
        }
        int inactivos = 0;
        for (RenglonVenta renglon : reporte.getPorProducto()) {
            if (renglon.isInactivo()) {
                inactivos++;
            }
        }
        // RF-66 · Excepción paso 5: ventas de productos dados de baja, marca «Producto inactivo».
        if (inactivos > 0) {
            avisos.add(inactivos + " producto(s) vendido(s) ya están dados de baja: aparecen con la marca «"
                    + MSG_PRODUCTO_INACTIVO + "»");
        }
        return avisos;
    }

    private void calcularPorcentajes(List<RenglonVenta> renglones, double total) {
        for (RenglonVenta renglon : renglones) {
            if (total > 0) {
                renglon.setPorcentajeIngresos(renglon.getIngresos() / total * 100);
            } else {
                renglon.setPorcentajeIngresos(0);
            }
        }
    }
}
