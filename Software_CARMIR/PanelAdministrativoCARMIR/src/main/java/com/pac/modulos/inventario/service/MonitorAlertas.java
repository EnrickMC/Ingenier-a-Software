package com.pac.modulos.inventario.service;

import com.pac.dao.DAOException;

import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN SINGLETON + PATRÓN OBSERVER (sujeto).
 * Guarda cuántas alertas de inventario están pendientes y avisa a los observadores
 * (el menú lateral muestra el número en "Inventario de Insumos").
 *
 * revisar() se llama al iniciar sesión, cada hora y después de cada entrada,
 * merma, cambio de stock mínimo o descuento por producción (RF-61 y RF-62).
 * Si falla la conexión no interrumpe nada: la revisión queda para la siguiente vez.
 */
public class MonitorAlertas {

    // La única instancia de la clase
    private static MonitorAlertas instancia;

    private List<AlertasObserver> observadores;
    private int pendientes;
    private boolean ultimaRevisionCorrecta = true;

    // Constructor privado
    private MonitorAlertas() {
        observadores = new ArrayList<AlertasObserver>();
    }

    // Devuelve la única instancia; la crea la primera vez que se pide
    public static synchronized MonitorAlertas getInstancia() {
        if (instancia == null) {
            instancia = new MonitorAlertas();
        }
        return instancia;
    }

    // Revisa todo el inventario y avisa a los observadores; devuelve false si no hubo conexión
    // RF-61 / RF-62: punto de entrada de la revisión de alertas (paso 1 del sistema).
    public boolean revisar() {
        try {
            AlertaService servicio = new AlertaService();
            pendientes = servicio.revisar();
            ultimaRevisionCorrecta = true;
        } catch (DAOException e) {
            // RF-61 / RF-62 · Excepción paso 2: sin conexión, la revisión queda pendiente.
            // RF-61 / RF-62: la revisión queda pendiente para la siguiente vez
            System.out.println("No fue posible revisar las alertas de inventario: " + e.getMessage());
            ultimaRevisionCorrecta = false;
            return false;
        }
        avisar();
        return true;
    }

    // Solo vuelve a contar las pendientes (por ejemplo, después de marcar una como atendida)
    public void actualizarConteo() {
        try {
            AlertaService servicio = new AlertaService();
            pendientes = servicio.contarPendientes();
        } catch (DAOException e) {
            return;
        }
        avisar();
    }

    public int getPendientes() {
        return pendientes;
    }

    public boolean isUltimaRevisionCorrecta() {
        return ultimaRevisionCorrecta;
    }

    // ----- Manejo de observadores -----

    public void agregarObservador(AlertasObserver observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void quitarObservador(AlertasObserver observador) {
        observadores.remove(observador);
    }

    // RF-61 · Paso 7: avisa a las pantallas para mostrar el contador de alertas.
    private void avisar() {
        // Se copia la lista para que un observador pueda quitarse sin causar errores
        List<AlertasObserver> copia = new ArrayList<AlertasObserver>(observadores);
        for (AlertasObserver observador : copia) {
            observador.alertasActualizadas(pendientes);
        }
    }
}
