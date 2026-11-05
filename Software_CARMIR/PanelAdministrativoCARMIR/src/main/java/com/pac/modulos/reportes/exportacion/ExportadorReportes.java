package com.pac.modulos.reportes.exportacion;

import com.pac.modulos.reportes.model.MovimientoRevisar;
import com.pac.modulos.reportes.model.RenglonDia;
import com.pac.modulos.reportes.model.RenglonVenta;
import com.pac.modulos.reportes.model.ReporteIngresosGastos;
import com.pac.modulos.reportes.model.ReporteVentas;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportación de los reportes a Excel o CSV (RF-67, RNF-9).
 * Arma el archivo con la misma información que se ve en pantalla:
 * una hoja de resumen (periodo, fecha de generación y totales) y hojas de detalle.
 */
public class ExportadorReportes {

    public static final String MSG_GENERA_ANTES = "Genera el reporte antes de exportarlo";
    public static final String MSG_SIN_DATOS = "No hay datos para exportar";
    public static final String MSG_ERROR_ARCHIVO = "No fue posible generar el archivo de Excel";
    public static final String MSG_DESCARGA_INCOMPLETA = "La descarga no se completó";
    public static final String MSG_EXITO = "Reporte exportado correctamente";

    public static final String TIPO_INGRESOS = "IngresosGastos";
    public static final String TIPO_VENTAS = "MasVendidos";

    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Nombre sugerido: CARMIR_Reporte_<tipo>_<inicio>_<fin>.xlsx
    public static String nombreSugerido(String tipo, LocalDate desde, LocalDate hasta) {
        return "CARMIR_Reporte_" + tipo + "_" + FECHA_ARCHIVO.format(desde) + "_" + FECHA_ARCHIVO.format(hasta) + ".xlsx";
    }

    // Guarda el libro como .csv si el nombre termina así; si no, como .xlsx.
    // Después revisa que el archivo exista y tenga contenido.
    // RF-67 · Paso 5: guarda el archivo y revisa que la descarga se completó.
    public static void guardar(LibroExcel libro, File archivo) throws IOException {
        if (archivo.getName().toLowerCase().endsWith(".csv")) {
            libro.guardarCsv(archivo);
        } else {
            libro.guardarXlsx(archivo);
        }
        if (!archivo.exists() || archivo.length() == 0) {
            // RF-67 · Excepción paso 5: «La descarga no se completó».
            throw new IOException(MSG_DESCARGA_INCOMPLETA);
        }
    }

    // ===================== RF-65 · Ingresos y gastos =====================

    // RF-67 · Pasos 3 y 4: arma el Excel del reporte de ingresos y gastos.
    public static LibroExcel armar(ReporteIngresosGastos reporte) {
        LibroExcel libro = new LibroExcel();

        // Hoja 1: resumen
        HojaExcel resumen = libro.agregarHoja("Resumen");
        encabezadoReporte(resumen, "Reporte de ingresos y gastos", reporte.getDesde(), reporte.getHasta(),
                reporte.getFechaGeneracion());
        resumen.agregarFila(CeldaExcel.encabezado("Concepto"), CeldaExcel.encabezado("Importe"));
        resumen.agregarFila(CeldaExcel.texto("Anticipos"), CeldaExcel.moneda(reporte.getAnticipos()));
        resumen.agregarFila(CeldaExcel.texto("Liquidaciones"), CeldaExcel.moneda(reporte.getLiquidaciones()));
        resumen.agregarFila(CeldaExcel.texto("Pagos totales"), CeldaExcel.moneda(reporte.getPagosTotales()));
        resumen.agregarFila(CeldaExcel.texto("Reembolsos realizados"), CeldaExcel.moneda(-reporte.getReembolsos()));
        resumen.agregarFila(CeldaExcel.textoTotal("Total de ingresos"), CeldaExcel.monedaTotal(reporte.getIngresos()));
        resumen.agregarFilaVacia();
        resumen.agregarFila(CeldaExcel.texto("Compras de insumos"), CeldaExcel.moneda(reporte.getCompras()));
        resumen.agregarFila(CeldaExcel.texto("Mermas"), CeldaExcel.moneda(reporte.getMermas()));
        resumen.agregarFila(CeldaExcel.textoTotal("Total de gastos"), CeldaExcel.monedaTotal(reporte.getGastos()));
        resumen.agregarFilaVacia();
        String etiquetaUtilidad = "Utilidad bruta";
        if (reporte.esPerdida()) {
            etiquetaUtilidad = "Pérdida en el periodo";
        }
        resumen.agregarFila(CeldaExcel.textoTotal(etiquetaUtilidad), CeldaExcel.monedaTotal(reporte.getUtilidad()));
        resumen.agregarFilaVacia();
        resumen.agregarFila(CeldaExcel.texto("Costo de producción (informativo)"), CeldaExcel.moneda(reporte.getProduccion()));
        resumen.agregarFila(CeldaExcel.texto("Pagos de pedidos cancelados por reembolsar (aparte)"),
                CeldaExcel.moneda(reporte.getPorReembolsar()));

        // Hoja 2: detalle por fecha
        HojaExcel detalle = libro.agregarHoja("Detalle por fecha");
        detalle.agregarFila(CeldaExcel.encabezado("Fecha"), CeldaExcel.encabezado("Cobros"),
                CeldaExcel.encabezado("Reembolsos"), CeldaExcel.encabezado("Ingresos"),
                CeldaExcel.encabezado("Compras"), CeldaExcel.encabezado("Mermas"),
                CeldaExcel.encabezado("Gastos"), CeldaExcel.encabezado("Utilidad"));
        for (RenglonDia dia : reporte.getDias()) {
            detalle.agregarFila(CeldaExcel.fecha(dia.getFecha()), CeldaExcel.moneda(dia.getCobros()),
                    CeldaExcel.moneda(dia.getReembolsos()), CeldaExcel.moneda(dia.getIngresos()),
                    CeldaExcel.moneda(dia.getCompras()), CeldaExcel.moneda(dia.getMermas()),
                    CeldaExcel.moneda(dia.getGastos()), CeldaExcel.moneda(dia.getUtilidad()));
        }
        detalle.agregarFila(CeldaExcel.textoTotal("Total"), CeldaExcel.monedaTotal(reporte.getCobros()),
                CeldaExcel.monedaTotal(reporte.getReembolsos()), CeldaExcel.monedaTotal(reporte.getIngresos()),
                CeldaExcel.monedaTotal(reporte.getCompras()), CeldaExcel.monedaTotal(reporte.getMermas()),
                CeldaExcel.monedaTotal(reporte.getGastos()), CeldaExcel.monedaTotal(reporte.getUtilidad()));

        // Hoja 3: movimientos por revisar (solo si hay)
        if (!reporte.getPorRevisar().isEmpty()) {
            HojaExcel revisar = libro.agregarHoja("Movimientos por revisar");
            revisar.agregarFila(CeldaExcel.encabezado("Fecha"), CeldaExcel.encabezado("Origen"),
                    CeldaExcel.encabezado("Referencia"), CeldaExcel.encabezado("Monto"), CeldaExcel.encabezado("Motivo"));
            for (MovimientoRevisar movimiento : reporte.getPorRevisar()) {
                revisar.agregarFila(CeldaExcel.texto(Formatos.fechaHoraCorta(movimiento.getFecha())),
                        CeldaExcel.texto(movimiento.getOrigen()), CeldaExcel.texto(movimiento.getReferencia()),
                        CeldaExcel.moneda(movimiento.getMonto()), CeldaExcel.texto(movimiento.getMotivo()));
            }
        }
        return libro;
    }

    // ===================== RF-66 · Más vendidos =====================

    // RF-67 · Pasos 3 y 4: arma el Excel del reporte de más vendidos.
    public static LibroExcel armar(ReporteVentas reporte) {
        LibroExcel libro = new LibroExcel();

        HojaExcel resumen = libro.agregarHoja("Resumen");
        encabezadoReporte(resumen, "Reporte de productos más vendidos", reporte.getDesde(), reporte.getHasta(),
                reporte.getFechaGeneracion());
        resumen.agregarFila(CeldaExcel.texto("Categoría"), CeldaExcel.texto(reporte.getNombreCategoria()));
        resumen.agregarFila(CeldaExcel.texto("Ordenado por"), CeldaExcel.texto(reporte.getOrden()));
        resumen.agregarFilaVacia();
        resumen.agregarFila(CeldaExcel.encabezado("Concepto"), CeldaExcel.encabezado("Valor"));
        resumen.agregarFila(CeldaExcel.texto("Pedidos"), CeldaExcel.numero(reporte.getPedidos()));
        resumen.agregarFila(CeldaExcel.texto("Unidades vendidas"), CeldaExcel.numero(reporte.getUnidadesTotales()));
        resumen.agregarFila(CeldaExcel.texto("Ingresos por ventas"), CeldaExcel.moneda(reporte.getIngresosTotales()));
        resumen.agregarFila(CeldaExcel.texto("Costo de los postres"), CeldaExcel.moneda(reporte.getCostoTotal()));
        resumen.agregarFila(CeldaExcel.textoTotal("Utilidad"), CeldaExcel.monedaTotal(reporte.getUtilidadTotal()));
        resumen.agregarFila(CeldaExcel.texto("Margen"), CeldaExcel.porcentaje(reporte.getMargenTotal()));
        if (reporte.getLineasIncompletas() > 0) {
            resumen.agregarFila(CeldaExcel.texto("Ventas no incluidas (datos incompletos)"),
                    CeldaExcel.numero(reporte.getLineasIncompletas()));
        }

        hojaVentas(libro.agregarHoja("Por producto"), "Producto", "Categoría", reporte.getPorProducto(), true);
        hojaVentas(libro.agregarHoja("Por categoría"), "Categoría", "Tipo", reporte.getPorCategoria(), false);
        hojaVentas(libro.agregarHoja("Por personalización"), "Opción", "Característica", reporte.getPorOpcion(), false);
        return libro;
    }

    // Una hoja de ranking: posición, nombre, grupo, unidades, ingresos, costo, utilidad, margen y % de ingresos
    private static void hojaVentas(HojaExcel hoja, String titulo, String tituloGrupo, List<RenglonVenta> renglones,
                                   boolean conEstado) {
        if (conEstado) {
            hoja.agregarFila(CeldaExcel.encabezado("#"), CeldaExcel.encabezado(titulo), CeldaExcel.encabezado(tituloGrupo),
                    CeldaExcel.encabezado("Unidades"), CeldaExcel.encabezado("Ingresos"), CeldaExcel.encabezado("Costo"),
                    CeldaExcel.encabezado("Utilidad"), CeldaExcel.encabezado("Margen"),
                    CeldaExcel.encabezado("% de ingresos"), CeldaExcel.encabezado("Estado"));
        } else {
            hoja.agregarFila(CeldaExcel.encabezado("#"), CeldaExcel.encabezado(titulo), CeldaExcel.encabezado(tituloGrupo),
                    CeldaExcel.encabezado("Unidades"), CeldaExcel.encabezado("Ingresos"), CeldaExcel.encabezado("Costo"),
                    CeldaExcel.encabezado("Utilidad"), CeldaExcel.encabezado("Margen"),
                    CeldaExcel.encabezado("% de ingresos"));
        }
        int posicion = 1;
        for (RenglonVenta renglon : renglones) {
            if (conEstado) {
                String estado = renglon.getEstadoProducto();
                hoja.agregarFila(CeldaExcel.numero(posicion), CeldaExcel.texto(renglon.getNombre()),
                        CeldaExcel.texto(renglon.getGrupo()), CeldaExcel.numero(renglon.getUnidades()),
                        CeldaExcel.moneda(renglon.getIngresos()), CeldaExcel.moneda(renglon.getCosto()),
                        CeldaExcel.moneda(renglon.getUtilidad()), CeldaExcel.porcentaje(renglon.getMargen()),
                        CeldaExcel.porcentaje(renglon.getPorcentajeIngresos()), CeldaExcel.texto(estado));
            } else {
                hoja.agregarFila(CeldaExcel.numero(posicion), CeldaExcel.texto(renglon.getNombre()),
                        CeldaExcel.texto(renglon.getGrupo()), CeldaExcel.numero(renglon.getUnidades()),
                        CeldaExcel.moneda(renglon.getIngresos()), CeldaExcel.moneda(renglon.getCosto()),
                        CeldaExcel.moneda(renglon.getUtilidad()), CeldaExcel.porcentaje(renglon.getMargen()),
                        CeldaExcel.porcentaje(renglon.getPorcentajeIngresos()));
            }
            posicion++;
        }
    }

    // Título, negocio, periodo y fecha de generación al inicio de la hoja de resumen
    private static void encabezadoReporte(HojaExcel hoja, String titulo, LocalDate desde,
                                          LocalDate hasta, LocalDateTime generado) {
        hoja.agregarFila(CeldaExcel.titulo(titulo));
        hoja.agregarFila(CeldaExcel.texto(ConfiguracionApp.getInstancia().getNombreNegocio()));
        hoja.agregarFila(CeldaExcel.texto("Periodo"), CeldaExcel.fecha(desde), CeldaExcel.texto("al"), CeldaExcel.fecha(hasta));
        hoja.agregarFila(CeldaExcel.texto("Generado el"), CeldaExcel.texto(Formatos.fechaHoraCorta(generado)));
        hoja.agregarFilaVacia();
    }
}
