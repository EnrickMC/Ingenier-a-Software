package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.pedidos.dao.PagoDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN SINGLETON + PATRÓN OBSERVER (sujeto).
 * Cuenta las solicitudes por revisar y los pagos por validar, y detecta los pedidos nuevos
 * que llegan desde la web. El panel lo revisa cada 60 segundos (RF-21/RF-23) y avisa a los
 * observadores solo cuando algo cambió.
 */
public class MonitorPedidos {

    private static MonitorPedidos instancia;

    private List<PedidosObserver> observadores = new ArrayList<PedidosObserver>();
    private PedidoDAO pedidoDAO;
    private PagoDAO pagoDAO;
    private int porRevisar = 0;
    private int pagosPendientes = 0;
    private int ultimoFolio = -1;     // -1 = todavía no se revisa

    private MonitorPedidos() {
        pedidoDAO = DAOFactory.crearPedidoDAO();
        pagoDAO = DAOFactory.crearPagoDAO();
    }

    public static synchronized MonitorPedidos getInstancia() {
        if (instancia == null) {
            instancia = new MonitorPedidos();
        }
        return instancia;
    }

    /**
     * Vuelve a contar en la base de datos; devuelve false si no hubo conexión.
     * Debe llamarse desde el hilo de la pantalla (los observadores cambian controles de JavaFX).
     */
    // RF-23 · Paso 1: revisa si llegaron solicitudes nuevas para mostrar el contador en el menú.
    public boolean revisar() {
        int nuevosPorRevisar;
        int nuevosPagos;
        int folioMayor;
        try {
            nuevosPorRevisar = pedidoDAO.contarPorRevisar();
            nuevosPagos = pagoDAO.contarPendientes();
            folioMayor = pedidoDAO.ultimoFolio();
        } catch (DAOException e) {
            return false;
        }

        // Llegó un pedido nuevo si el folio más alto creció (no cuenta la primera revisión)
        int folioNuevo = 0;
        if (ultimoFolio >= 0 && folioMayor > ultimoFolio) {
            folioNuevo = folioMayor;
        }
        boolean cambio = nuevosPorRevisar != porRevisar || nuevosPagos != pagosPendientes || folioNuevo > 0;
        porRevisar = nuevosPorRevisar;
        pagosPendientes = nuevosPagos;
        ultimoFolio = folioMayor;

        if (cambio) {
            avisar(folioNuevo);
        }
        return true;
    }

    public int getPorRevisar() {
        return porRevisar;
    }

    public int getPagosPendientes() {
        return pagosPendientes;
    }

    // ----- Manejo de observadores -----

    public void agregarObservador(PedidosObserver observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void quitarObservador(PedidosObserver observador) {
        observadores.remove(observador);
    }

    private void avisar(int folioNuevo) {
        // Se copia la lista para que un observador pueda quitarse sin causar errores
        List<PedidosObserver> copia = new ArrayList<PedidosObserver>(observadores);
        for (PedidosObserver observador : copia) {
            observador.pedidosActualizados(porRevisar, pagosPendientes, folioNuevo);
        }
    }
}
