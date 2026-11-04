package com.pac.modulos.inventario.controller;

import com.pac.modulos.inventario.model.AlertaInventario;
import com.pac.modulos.inventario.model.TipoAlerta;
import com.pac.modulos.inventario.service.AlertaService;
import com.pac.util.Formatos;

/**
 * Una fila de la tabla de alertas (RF-61 existencia baja, RF-62 caducidad).
 */
public class FilaAlerta {

    private AlertaInventario alerta;

    public FilaAlerta(AlertaInventario alerta) {
        this.alerta = alerta;
    }

    public AlertaInventario getAlerta() {
        return alerta;
    }

    public String getFecha() {
        return Formatos.fechaHoraCorta(alerta.getFecha());
    }

    // "Agotado" no es un tipo en la BD: es una alerta de existencia baja con existencia cero
    public String getTipo() {
        if (alerta.estaAgotado()) {
            return "Agotado";
        }
        return alerta.getTipo().toString();
    }

    public String getInsumo() {
        return alerta.getNombreInsumo();
    }

    // Texto que explica la alerta
    public String getDetalle() {
        String unidad = "";
        if (alerta.getUnidad() != null) {
            unidad = " " + alerta.getUnidad().getValorBD();
        }
        if (alerta.getTipo() == TipoAlerta.EXISTENCIA_BAJA) {
            return "Existencia: " + Formatos.cantidad(alerta.getExistenciaEnAlerta()) + unidad;
        }
        String lote = "Lote L-" + String.format("%04d", alerta.getIdLote());
        String quedan = " · quedan " + Formatos.cantidad(alerta.getExistenciaLote()) + unidad;
        int dias = AlertaService.diasParaCaducar(alerta);
        String cuando;
        if (dias < 0) {
            cuando = "caducó el " + Formatos.fecha(alerta.getFechaCaducidad());
        } else if (dias == 0) {
            cuando = "caduca hoy";
        } else if (dias == 1) {
            cuando = "caduca mañana";
        } else {
            cuando = "caduca en " + dias + " días (" + Formatos.fecha(alerta.getFechaCaducidad()) + ")";
        }
        return lote + " " + cuando + quedan;
    }

    public String getEstado() {
        if (alerta.isPendiente()) {
            return "Pendiente";
        }
        return "Atendida";
    }
}
