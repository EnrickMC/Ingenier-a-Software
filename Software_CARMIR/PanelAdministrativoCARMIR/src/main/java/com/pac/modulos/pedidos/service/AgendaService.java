package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.inventario.service.DescuentoInsumosService;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.AgendaPeriodo;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.util.ConfiguracionApp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Agenda de Producción (RF-45, RF-46).
 * Muestra los pedidos no cancelados de «Aprobado» a «Pedido terminado» en su fecha de entrega,
 * los atrasados, los que deben pasar a preparación pronto y los insumos estimados.
 */
public class AgendaService {

    // Vistas del calendario
    public static final int VISTA_DIA = 0;
    public static final int VISTA_SEMANA = 1;
    public static final int VISTA_MES = 2;
    public static final int VISTA_RANGO = 3;

    public static final String MSG_SIN_PEDIDOS = "Sin pedidos programados para este periodo";
    public static final String MSG_NO_CARGADA = "No fue posible cargar la agenda";
    public static final String MSG_RANGO_AMPLIO = "El rango de fechas es demasiado amplio";
    public static final String MSG_INFO_INCOMPLETA = "Hay pedidos con información incompleta";

    private PedidoDAO pedidoDAO;
    private DescuentoInsumosService descuentoService;
    private PedidoService pedidoService;

    public AgendaService() {
        pedidoDAO = DAOFactory.crearPedidoDAO();
        descuentoService = new DescuentoInsumosService();
        pedidoService = new PedidoService();
    }

    // Primer día del periodo que contiene la fecha
    // RF-46 (Filtrar vistas de la agenda por periodo): periodos de Día, Semana y Mes.
    public static LocalDate inicioPeriodo(int vista, LocalDate fecha) {
        if (vista == VISTA_SEMANA) {
            return fecha.minusDays(fecha.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        }
        if (vista == VISTA_MES) {
            return fecha.withDayOfMonth(1);
        }
        return fecha;
    }

    // Último día del periodo que contiene la fecha
    public static LocalDate finPeriodo(int vista, LocalDate fecha) {
        if (vista == VISTA_SEMANA) {
            return inicioPeriodo(vista, fecha).plusDays(6);
        }
        if (vista == VISTA_MES) {
            return fecha.withDayOfMonth(fecha.lengthOfMonth());
        }
        return fecha;
    }

    // Fecha de referencia del periodo anterior (pasos = -1) o siguiente (pasos = 1)
    public static LocalDate mover(int vista, LocalDate fecha, int pasos) {
        if (vista == VISTA_SEMANA) {
            return fecha.plusWeeks(pasos);
        }
        if (vista == VISTA_MES) {
            return fecha.plusMonths(pasos);
        }
        return fecha.plusDays(pasos);
    }

    // Revisa un rango elegido a mano; devuelve el mensaje de error o null si está bien
    // RF-46 · Paso 3: valida las fechas del rango personalizado.
    public String validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            // RF-46 · Excepción paso 3: la fecha de inicio es mayor a la fecha de fin.
            return PedidoService.MSG_RANGO_INVALIDO;
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > ConfiguracionApp.getInstancia().getDiasMaximosAgenda()) {
            // RF-46 · Excepción paso 3: «El rango de fechas es demasiado amplio».
            return MSG_RANGO_AMPLIO + " (máximo " + ConfiguracionApp.getInstancia().getDiasMaximosAgenda() + " días)";
        }
        return null;
    }

    /**
     * Lee la agenda del periodo. Si algo falla lanza DAOException y la pantalla
     * conserva el periodo anterior (no se muestra información parcial).
     */
    // RF-45 (Visualización de agenda de producción) y RF-46 paso 4: consulta los pedidos no cancelados del periodo.
    public AgendaPeriodo cargar(LocalDate desde, LocalDate hasta) throws DAOException {
        AgendaPeriodo agenda = new AgendaPeriodo();
        agenda.setDesde(desde);
        agenda.setHasta(hasta);

        // RF-45 · Excepción paso 5: los pedidos incompletos van a «Pedidos con datos por revisar».
        // Pedidos del periodo: los incompletos van al panel "Pedidos con datos por revisar"
        for (Pedido pedido : pedidoDAO.listarAgenda(desde, hasta)) {
            if (pedidoService.datosFaltantes(pedido).isEmpty()) {
                agenda.getPedidos().add(pedido);
            } else {
                agenda.getPorRevisar().add(pedido);
            }
        }
        agenda.getAtrasados().addAll(pedidoDAO.listarAtrasados());

        // Insumos estimados de los pedidos que todavía no pasan a preparación (no se descuenta nada)
        List<Integer> folios = new ArrayList<Integer>();
        for (Pedido pedido : agenda.getPedidos()) {
            if (pedido.getEstado() == EstadoPedido.APROBADO || pedido.getEstado() == EstadoPedido.PAGADO) {
                folios.add(pedido.getFolio());
            }
        }
        if (!folios.isEmpty()) {
            agenda.setInsumos(descuentoService.estimarNecesidades(folios));
        }
        return agenda;
    }

    // true si el pedido debe pasar a «En preparación» en los próximos días (y todavía no lo hace)
    public static boolean porPreparar(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.APROBADO && pedido.getEstado() != EstadoPedido.PAGADO) {
            return false;
        }
        if (pedido.getFechaEntrega() == null) {
            return false;
        }
        LocalDate limite = LocalDate.now().plusDays(ConfiguracionApp.getInstancia().getDiasParaPreparar());
        return !pedido.getFechaEntrega().isAfter(limite) && !pedido.isAtrasado();
    }

    // true si sigue esperando el anticipo («Pendiente de pago» en la agenda)
    // RF-45 · Paso 6: marca «Pendiente de pago».
    public static boolean pendienteDePago(Pedido pedido) {
        return pedido.getEstado() == EstadoPedido.APROBADO;
    }
}
