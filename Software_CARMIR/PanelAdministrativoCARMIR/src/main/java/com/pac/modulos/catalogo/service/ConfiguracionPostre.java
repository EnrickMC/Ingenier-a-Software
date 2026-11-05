package com.pac.modulos.catalogo.service;

import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;
import com.pac.modulos.catalogo.model.OpcionElegida;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas para armar un postre con las opciones de su categoría.
 * La usa el formulario de productos y la podrán usar el gestor de pedidos y la API.
 *
 * - validar(): revisa obligatorias, selección única o múltiple, presupuesto,
 *   disponibilidad y exclusiones. Devuelve la lista de errores (vacía = todo bien).
 * - claveConfiguracion(): arma la ClaveConfiguracion "IdOpcion:Cantidad|..." ordenada por id,
 *   así el mismo postre da la misma clave sin importar el orden en que se eligió.
 * - calcularCosto() y calcularPrecio(): hacen la misma cuenta que la vista vw_CostoPrecioPostre.
 */
public class ConfiguracionPostre {

    // Características de la categoría, cada una con su lista de opciones
    private List<Caracteristica> caracteristicas;
    // Exclusiones activas de la categoría
    private List<Exclusion> exclusiones;

    public ConfiguracionPostre(List<Caracteristica> caracteristicas, List<Exclusion> exclusiones) {
        this.caracteristicas = caracteristicas;
        this.exclusiones = exclusiones;
    }

    public List<Caracteristica> getCaracteristicas() {
        return caracteristicas;
    }

    // ===================== Validación =====================

    public List<String> validar(List<OpcionElegida> elegidas) {
        List<String> errores = new ArrayList<String>();

        if (elegidas.isEmpty()) {
            errores.add("Elige al menos una opción para el producto");
            return errores;
        }

        // 1. Cada opción debe ser de la categoría y estar disponible
        for (OpcionElegida elegida : elegidas) {
            Opcion opcion = elegida.getOpcion();
            Caracteristica suCaracteristica = buscarCaracteristica(opcion.getIdCaracteristica());
            if (suCaracteristica == null) {
                errores.add("La opción «" + opcion.getNombre() + "» no pertenece a esta categoría");
            } else if (!opcion.isDisponible() || !suCaracteristica.isDisponible()) {
                errores.add("La opción seleccionada ya no está disponible: " + opcion.getNombre());
            }
            if (elegida.getCantidad() < 1 || elegida.getCantidad() > 127) {
                errores.add("La cantidad de «" + opcion.getNombre() + "» debe estar entre 1 y 127");
            }
        }

        // 2. Reglas de cada característica
        double factorPresupuesto = factorPresupuesto(elegidas);
        for (Caracteristica caracteristica : caracteristicas) {
            if (!caracteristica.isDisponible()) {
                continue;
            }
            int opcionesElegidas = 0;
            int piezas = 0;
            double puntos = 0;
            for (OpcionElegida elegida : elegidas) {
                if (elegida.getOpcion().getIdCaracteristica() == caracteristica.getIdCaracteristica()) {
                    opcionesElegidas++;
                    piezas = piezas + elegida.getCantidad();
                    puntos = puntos + elegida.getOpcion().getPuntosSeleccion() * elegida.getCantidad();
                }
            }

            if (caracteristica.isObligatoria() && opcionesElegidas == 0) {
                errores.add("Falta elegir una opción en «" + caracteristica.getNombre() + "» (es obligatoria)");
            }

            if (!caracteristica.isSeleccionMultiple()) {
                if (piezas > 1) {
                    errores.add("En «" + caracteristica.getNombre() + "» solo se puede elegir una opción");
                }
            } else {
                double disponibles = caracteristica.getPresupuestoSeleccion() * factorPresupuesto;
                if (puntos > disponibles + 0.0001) {
                    errores.add("«" + caracteristica.getNombre() + "» excede su presupuesto de selección: usa "
                            + ValidacionCatalogo.numero(puntos) + " de "
                            + ValidacionCatalogo.numero(disponibles) + " puntos disponibles");
                }
            }
        }

        // 3. Exclusiones: dos opciones elegidas que no se pueden combinar
        for (Exclusion exclusion : exclusiones) {
            if (!exclusion.estaActiva()) {
                continue;
            }
            if (estaElegida(elegidas, exclusion.getIdOpcion()) && estaElegida(elegidas, exclusion.getIdOpcionExcluida())) {
                String motivo = exclusion.getMotivo();
                if (motivo == null) {
                    motivo = "combinación no permitida";
                }
                errores.add("No se pueden combinar «" + exclusion.getNombreOpcion() + "» y «"
                        + exclusion.getNombreOpcionExcluida() + "»: " + motivo + ". Cambia una de las dos opciones");
            }
        }
        return errores;
    }

    // Producto de los MultiplicadorPresupuesto de las opciones elegidas (por ejemplo tamaño grande = 2)
    public double factorPresupuesto(List<OpcionElegida> elegidas) {
        double factor = 1;
        for (OpcionElegida elegida : elegidas) {
            factor = factor * elegida.getOpcion().getMultiplicadorPresupuesto();
        }
        return factor;
    }

    // ===================== Clave de configuración =====================

    // "IdOpcion:Cantidad" separados por "|", ordenados de menor a mayor id
    public static String claveConfiguracion(List<OpcionElegida> elegidas) {
        // Se copia la lista y se ordena con el método de inserción (sencillo y suficiente)
        List<OpcionElegida> ordenadas = new ArrayList<OpcionElegida>(elegidas);
        for (int i = 1; i < ordenadas.size(); i++) {
            OpcionElegida actual = ordenadas.get(i);
            int j = i - 1;
            while (j >= 0 && ordenadas.get(j).getOpcion().getIdOpcion() > actual.getOpcion().getIdOpcion()) {
                ordenadas.set(j + 1, ordenadas.get(j));
                j--;
            }
            ordenadas.set(j + 1, actual);
        }

        String clave = "";
        for (int i = 0; i < ordenadas.size(); i++) {
            if (i > 0) {
                clave = clave + "|";
            }
            clave = clave + ordenadas.get(i).getOpcion().getIdOpcion() + ":" + ordenadas.get(i).getCantidad();
        }
        return clave;
    }

    // ===================== Costo y precio (igual que vw_CostoPrecioPostre) =====================

    // Factor de escala = producto de MultiplicadorIngredientes de las opciones que NO se escalan
    // (por ejemplo el tamaño o los pisos)
    public static double factorEscala(List<OpcionElegida> elegidas) {
        double factor = 1;
        for (OpcionElegida elegida : elegidas) {
            if (!elegida.getOpcion().isSeEscala()) {
                factor = factor * elegida.getOpcion().getMultiplicadorIngredientes();
            }
        }
        return factor;
    }

    // Costo de ingredientes = suma de CostoReceta × Cantidad × (factor si la opción se escala)
    public static double calcularCosto(List<OpcionElegida> elegidas) {
        double factor = factorEscala(elegidas);
        double costo = 0;
        for (OpcionElegida elegida : elegidas) {
            costo = costo + costoOpcion(elegida, factor);
        }
        return redondear(costo);
    }

    // Precio de venta = suma de costo de cada opción × su MultiplicadorCosto
    public static double calcularPrecio(List<OpcionElegida> elegidas) {
        double factor = factorEscala(elegidas);
        double precio = 0;
        for (OpcionElegida elegida : elegidas) {
            precio = precio + costoOpcion(elegida, factor) * elegida.getOpcion().getMultiplicadorCosto();
        }
        return redondear(precio);
    }

    // Nombres de las opciones elegidas que todavía no tienen receta (su costo cuenta como 0)
    public static List<String> opcionesSinReceta(List<OpcionElegida> elegidas) {
        List<String> nombres = new ArrayList<String>();
        for (OpcionElegida elegida : elegidas) {
            if (!elegida.getOpcion().isTieneReceta()) {
                nombres.add(elegida.getOpcion().getNombre());
            }
        }
        return nombres;
    }

    // ===================== Apoyo =====================

    private static double costoOpcion(OpcionElegida elegida, double factor) {
        double costo = elegida.getOpcion().getCostoReceta() * elegida.getCantidad();
        if (elegida.getOpcion().isSeEscala()) {
            costo = costo * factor;
        }
        return costo;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    private Caracteristica buscarCaracteristica(int idCaracteristica) {
        for (Caracteristica caracteristica : caracteristicas) {
            if (caracteristica.getIdCaracteristica() == idCaracteristica) {
                return caracteristica;
            }
        }
        return null;
    }

    private boolean estaElegida(List<OpcionElegida> elegidas, int idOpcion) {
        for (OpcionElegida elegida : elegidas) {
            if (elegida.getOpcion().getIdOpcion() == idOpcion) {
                return true;
            }
        }
        return false;
    }
}
