package com.pac.modulos.reportes.exportacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Una hoja del archivo de Excel: un nombre y sus filas de celdas.
 */
public class HojaExcel {

    // Excel no permite más de 31 letras en el nombre de una hoja
    private static final int LARGO_MAXIMO_NOMBRE = 31;

    private String nombre;
    private List<List<CeldaExcel>> filas = new ArrayList<List<CeldaExcel>>();

    public HojaExcel(String nombre) {
        // Se quitan los caracteres que Excel no acepta en el nombre de una hoja
        String limpio = nombre.replace("/", "-").replace("\\", "-").replace("?", "").replace("*", "")
                .replace("[", "(").replace("]", ")").replace(":", "");
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            limpio = limpio.substring(0, LARGO_MAXIMO_NOMBRE);
        }
        this.nombre = limpio;
    }

    // Agrega una fila con las celdas indicadas
    public void agregarFila(CeldaExcel... celdas) {
        List<CeldaExcel> fila = new ArrayList<CeldaExcel>();
        for (CeldaExcel celda : celdas) {
            fila.add(celda);
        }
        filas.add(fila);
    }

    // Agrega una fila a partir de una lista ya armada
    public void agregarFila(List<CeldaExcel> celdas) {
        filas.add(new ArrayList<CeldaExcel>(celdas));
    }

    // Deja una fila en blanco (para separar secciones)
    public void agregarFilaVacia() {
        filas.add(new ArrayList<CeldaExcel>());
    }

    public String getNombre() {
        return nombre;
    }

    public List<List<CeldaExcel>> getFilas() {
        return filas;
    }

    // Número de columnas de la fila más larga
    public int getNumeroColumnas() {
        int maximo = 0;
        for (List<CeldaExcel> fila : filas) {
            if (fila.size() > maximo) {
                maximo = fila.size();
            }
        }
        return maximo;
    }
}
