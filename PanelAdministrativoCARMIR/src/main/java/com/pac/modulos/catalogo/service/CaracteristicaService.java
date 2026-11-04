package com.pac.modulos.catalogo.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.dao.CaracteristicaDAO;
import com.pac.modulos.catalogo.dao.CategoriaDAO;
import com.pac.modulos.catalogo.dao.ExclusionDAO;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Categoria;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de las características de cada categoría
 * (Registro, Modificación, Eliminación y Visualización de característica).
 *
 * El presupuesto de selección son los puntos que pueden gastar las opciones de una
 * característica de selección múltiple. Ejemplo: Decoración con presupuesto 3,
 * moño = 1 punto y listón = 2 puntos: caben tres moños, o un moño y un listón.
 */
public class CaracteristicaService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_ENCONTRADA = "Característica no encontrada";
    public static final String MSG_CATEGORIA_NO_DISPONIBLE = "La categoría ya no está disponible";
    public static final String MSG_CATEGORIA_NO_ENCONTRADA = "Categoría no encontrada";
    public static final String MSG_SIN_CARACTERISTICAS = "Esta categoría no tiene características registradas";
    public static final String MSG_YA_EXISTE = "La característica ya existe en esta categoría";
    public static final String MSG_NO_GUARDADA = "No fue posible guardar la característica";
    public static final String MSG_NO_ACTUALIZADA = "No fue posible actualizar la característica";
    public static final String MSG_NO_DADA_DE_BAJA = "No fue posible dar de baja la característica";
    public static final String MSG_EN_PEDIDOS = "La característica está en pedidos en curso";
    public static final String MSG_NOMBRE_INVALIDO =
            "Escribe el nombre de la característica (máximo 50 caracteres, solo letras, números y espacios)";
    public static final String MSG_PRESUPUESTO_INVALIDO = "El presupuesto de selección debe ser un número entero mayor a cero";
    public static final String MSG_ORDEN_INVALIDO = "El orden de muestra debe ser un número entero entre 1 y 127";

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_DESCRIPCION = 255;
    private static final int MAX_ORDEN = 127;
    private static final int MAX_PRESUPUESTO = 9999;

    private CaracteristicaDAO caracteristicaDAO;
    private CategoriaDAO categoriaDAO;
    private OpcionDAO opcionDAO;
    private ExclusionDAO exclusionDAO;

    public CaracteristicaService() {
        caracteristicaDAO = DAOFactory.crearCaracteristicaDAO();
        categoriaDAO = DAOFactory.crearCategoriaDAO();
        opcionDAO = DAOFactory.crearOpcionDAO();
        exclusionDAO = DAOFactory.crearExclusionDAO();
    }

    // ===================== Consultas (Visualización) =====================

    // Características de la categoría ordenadas por su orden de muestra
    // RF-22 · Paso 4: características de la categoría por orden de muestra.
    public List<Caracteristica> listarPorCategoria(int idCategoria) throws DAOException {
        return caracteristicaDAO.listarPorCategoria(idCategoria);
    }

    // Lee una característica con sus opciones; devuelve null si ya no existe
    // RF-22 · Paso 7: detalle de la característica con sus opciones (null = ya no existe).
    public Caracteristica buscarConOpciones(int idCaracteristica) throws DAOException {
        Caracteristica caracteristica = caracteristicaDAO.buscarPorId(idCaracteristica);
        if (caracteristica != null) {
            caracteristica.setOpciones(opcionDAO.listarPorCaracteristica(idCaracteristica));
        }
        return caracteristica;
    }

    // Siguiente orden libre (para proponerlo en el formulario de alta)
    public int siguienteOrden(int idCategoria) throws DAOException {
        int mayor = 0;
        for (Caracteristica c : caracteristicaDAO.listarPorCategoria(idCategoria)) {
            if (c.getOrdenMostrar() > mayor) {
                mayor = c.getOrdenMostrar();
            }
        }
        if (mayor >= MAX_ORDEN) {
            return MAX_ORDEN;
        }
        return mayor + 1;
    }

    // Paso 2 del registro: la categoría debe seguir existiendo y activa
    // RF-19 · Paso 2: la categoría debe seguir existiendo y activa.
    public ResultadoCatalogo revisarCategoriaParaAlta(int idCategoria) {
        try {
            Categoria categoria = categoriaDAO.buscarPorId(idCategoria);
            // RF-19 · Excepción paso 2: «La categoría ya no está disponible».
            if (categoria == null || !categoria.estaActiva()) {
                return ResultadoCatalogo.noEncontrado(MSG_CATEGORIA_NO_DISPONIBLE);
            }
            return ResultadoCatalogo.correcto("");
        } catch (DAOException e) {
            // RF-19 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }
    }

    // ===================== Registro =====================

    // RF-19 (Registro de característica): inicia aquí la regla de negocio (pasos 5 a 7).
    public ResultadoCatalogo registrar(Caracteristica datos) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        ResultadoCatalogo validacion = validarCampos(datos);
        // RF-19 · Excepción paso 5: campos incompletos o valores inválidos.
        if (validacion != null) {
            return validacion;
        }

        boolean recorrer;
        try {
            // La categoría debe seguir activa
            Categoria categoria = categoriaDAO.buscarPorId(datos.getIdCategoria());
            // RF-19 · Excepción paso 2: la categoría ya no está disponible.
            if (categoria == null || !categoria.estaActiva()) {
                return ResultadoCatalogo.noEncontrado(MSG_CATEGORIA_NO_DISPONIBLE);
            }
            // Paso 6: nombre repetido en la categoría
            // RF-19 · Excepción paso 6: «La característica ya existe en esta categoría».
            if (caracteristicaDAO.buscarPorNombre(datos.getIdCategoria(), datos.getNombre()) != null) {
                return ResultadoCatalogo.error(MSG_YA_EXISTE, ResultadoCatalogo.CAMPO_NOMBRE);
            }
            // RF-19 · Excepción paso 6: el orden está ocupado; se avisa y se recorren las siguientes.
            // Paso 6: si el orden está ocupado, se recorren las siguientes
            recorrer = caracteristicaDAO.ordenOcupado(datos.getIdCategoria(), datos.getOrdenMostrar(), 0);
        } catch (DAOException e) {
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // RF-19 · Paso 7: almacena la característica.
        try {
            caracteristicaDAO.insertar(datos, recorrer);
        } catch (DAOException e) {
            // RF-19 · Excepción paso 7: «No fue posible guardar la característica».
            return ResultadoCatalogo.error(MSG_NO_GUARDADA, ResultadoCatalogo.CAMPO_CONEXION);
        }

        String mensaje = "La característica «" + datos.getNombre() + "» se registró correctamente";
        if (recorrer) {
            mensaje = mensaje + ".\nEl orden " + datos.getOrdenMostrar()
                    + " ya estaba ocupado: las características siguientes se recorrieron una posición.";
        }
        return ResultadoCatalogo.correcto(mensaje);
    }

    // ===================== Modificación =====================

    // confirmado = la administradora ya aceptó el aviso sobre los carritos
    // RF-20 (Modificación de característica): inicia aquí la regla de negocio (pasos 5 a 8).
    public ResultadoCatalogo modificar(Caracteristica datos, boolean confirmado) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        ResultadoCatalogo validacion = validarCampos(datos);
        // RF-20 · Excepción paso 5: datos inválidos.
        if (validacion != null) {
            return validacion;
        }

        Caracteristica actual;
        boolean recorrer;
        try {
            actual = caracteristicaDAO.buscarPorId(datos.getIdCaracteristica());
            // RF-20 · Excepción paso 2: «Característica no encontrada».
            if (actual == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }
            datos.setIdCategoria(actual.getIdCategoria());

            // Paso 6: nombre disponible en la categoría
            Caracteristica conMismoNombre = caracteristicaDAO.buscarPorNombre(actual.getIdCategoria(), datos.getNombre());
            // RF-20 · Excepción paso 6: «La característica ya existe en esta categoría».
            if (conMismoNombre != null && conMismoNombre.getIdCaracteristica() != datos.getIdCaracteristica()) {
                return ResultadoCatalogo.error(MSG_YA_EXISTE, ResultadoCatalogo.CAMPO_NOMBRE);
            }

            // Paso 6: el presupuesto debe alcanzar para los puntos de cada opción
            if (datos.isSeleccionMultiple()) {
                List<String> fueraDePresupuesto = new ArrayList<String>();
                for (Opcion opcion : opcionDAO.listarPorCaracteristica(datos.getIdCaracteristica())) {
                    if (opcion.getPuntosSeleccion() > datos.getPresupuestoSeleccion()) {
                        fueraDePresupuesto.add(opcion.getNombre() + " ("
                                + ValidacionCatalogo.numero(opcion.getPuntosSeleccion()) + " puntos)");
                    }
                }
                // RF-20 · Excepción paso 6: el presupuesto no alcanza; se listan las opciones que ya no podrían elegirse.
                if (!fueraDePresupuesto.isEmpty()) {
                    return ResultadoCatalogo.error("Con ese presupuesto estas opciones ya no podrían elegirse: "
                            + ValidacionCatalogo.unir(fueraDePresupuesto), ResultadoCatalogo.CAMPO_PRESUPUESTO);
                }
            }

            recorrer = false;
            if (datos.getOrdenMostrar() != actual.getOrdenMostrar()) {
                recorrer = caracteristicaDAO.ordenOcupado(actual.getIdCategoria(), datos.getOrdenMostrar(),
                        datos.getIdCaracteristica());
            }
        } catch (DAOException e) {
            // RF-20 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 7: si se vuelve obligatoria o pasa de múltiple a única, se avisa y se confirma
        boolean ahoraObligatoria = !actual.isObligatoria() && datos.isObligatoria();
        boolean ahoraUnica = actual.isSeleccionMultiple() && !datos.isSeleccionMultiple();
        // RF-20 · Paso 7: aviso de carritos que deberán actualizar su personalización (si no confirma, regresa al formulario).
        if ((ahoraObligatoria || ahoraUnica) && !confirmado) {
            String mensaje = "Este cambio afecta la forma en que el cliente personaliza los productos de la categoría."
                    + " Los carritos con productos de esta categoría deberán actualizar su personalización."
                    + " Los pedidos ya enviados conservan las opciones que eligió el cliente.\n\n¿Deseas continuar?";
            return ResultadoCatalogo.confirmacion(mensaje, null);
        }

        // RF-20 · Paso 8: actualiza la característica.
        try {
            caracteristicaDAO.actualizar(datos, recorrer);
        } catch (DAOException e) {
            // RF-20 · Excepción paso 8: «No fue posible actualizar la característica».
            return ResultadoCatalogo.error(MSG_NO_ACTUALIZADA, ResultadoCatalogo.CAMPO_CONEXION);
        }

        String mensaje = "La característica «" + datos.getNombre() + "» se actualizó correctamente";
        if (recorrer) {
            mensaje = mensaje + ".\nEl orden " + datos.getOrdenMostrar()
                    + " ya estaba ocupado: las características siguientes se recorrieron una posición.";
        }
        return ResultadoCatalogo.correcto(mensaje);
    }

    // Botones subir / bajar: intercambia el orden con la característica vecina
    public ResultadoCatalogo mover(Caracteristica caracteristica, Caracteristica vecina) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            caracteristicaDAO.intercambiarOrden(caracteristica, vecina);
            return ResultadoCatalogo.correcto("");
        } catch (DAOException e) {
            return ResultadoCatalogo.error(MSG_NO_ACTUALIZADA, null);
        }
    }

    // ===================== Baja (Eliminación de característica) =====================

    // Pasos 2 a 4: revisa si se puede dar de baja y arma la confirmación doble
    // RF-21 (Eliminación de característica): inicia aquí la regla de negocio (pasos 2 a 4).
    public ResultadoCatalogo prepararBaja(int idCaracteristica) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            Caracteristica caracteristica = caracteristicaDAO.buscarPorId(idCaracteristica);
            // RF-21 · Excepción paso 2: «Característica no encontrada».
            if (caracteristica == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }

            // Paso 3: pedidos vigentes
            List<Integer> folios = caracteristicaDAO.listarFoliosVigentes(idCaracteristica);
            // RF-21 · Excepción paso 3: «La característica está en pedidos en curso» con sus folios.
            if (!folios.isEmpty()) {
                return ResultadoCatalogo.error(MSG_EN_PEDIDOS + ": " + ValidacionCatalogo.unirFolios(folios), null);
            }

            // Opciones y exclusiones que también se darán de baja
            List<String> nombresOpciones = new ArrayList<String>();
            List<String> nombresExclusiones = new ArrayList<String>();
            for (Opcion opcion : opcionDAO.listarPorCaracteristica(idCaracteristica)) {
                nombresOpciones.add(opcion.getNombre());
                for (Exclusion exclusion : exclusionDAO.listarPorOpcion(opcion.getIdOpcion())) {
                    String par = exclusion.getNombreOpcion() + " ✕ " + exclusion.getNombreOpcionExcluida();
                    if (!nombresExclusiones.contains(par)) {
                        nombresExclusiones.add(par);
                    }
                }
            }

            boolean historial = caracteristicaDAO.tieneHistorial(idCaracteristica);
            String mensaje = "Característica: " + caracteristica.getNombre();
            if (nombresOpciones.isEmpty()) {
                mensaje = mensaje + "\nOpciones: ninguna";
            } else {
                mensaje = mensaje + "\nOpciones: " + ValidacionCatalogo.unir(nombresOpciones);
            }
            if (nombresExclusiones.isEmpty()) {
                mensaje = mensaje + "\nExclusiones: ninguna";
            } else {
                mensaje = mensaje + "\nExclusiones: " + ValidacionCatalogo.unir(nombresExclusiones);
            }
            if (historial) {
                mensaje = mensaje + "\n\nSus opciones ya se usaron en postres o tienen receta, así que todo se"
                        + " INHABILITARÁ (no disponible; recetas y exclusiones inactivas).";
            } else {
                mensaje = mensaje + "\n\nNo tiene relaciones, así que se ELIMINARÁ junto con sus opciones y"
                        + " exclusiones. Esta acción no se puede deshacer.";
            }
            // RF-21 · Paso 4: mensaje de la confirmación doble con opciones y exclusiones afectadas.
            return ResultadoCatalogo.bajaPosible(mensaje, !historial);
        } catch (DAOException e) {
            // RF-21 · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }
    }

    // Paso 6: registra la baja
    // RF-21 · Paso 6 (después de que la Administradora confirmó en el paso 5).
    public ResultadoCatalogo darDeBaja(int idCaracteristica) {
        ResultadoCatalogo revision = prepararBaja(idCaracteristica);
        if (!revision.isExitoso()) {
            return revision;
        }
        try {
            // RF-21 · Paso 6: elimina si no tiene relaciones; si tiene historial, inhabilita la característica, sus opciones y exclusiones.
            if (revision.isSeEliminara()) {
                caracteristicaDAO.eliminar(idCaracteristica);
                return ResultadoCatalogo.correcto("La característica se eliminó correctamente");
            }
            caracteristicaDAO.inhabilitar(idCaracteristica);
            return ResultadoCatalogo.correcto("La característica se inhabilitó correctamente");
        } catch (DAOException e) {
            // RF-21 · Excepción paso 6: «No fue posible dar de baja la característica».
            return ResultadoCatalogo.error(MSG_NO_DADA_DE_BAJA, null);
        }
    }

    // ===================== Apoyo =====================

    // Revisa los campos del formulario; devuelve null si todo está bien
    // RF-19 · Paso 5 / RF-20 · Paso 5: campos obligatorios, presupuesto y orden de muestra.
    private ResultadoCatalogo validarCampos(Caracteristica datos) {
        if (!ValidacionCatalogo.nombreValido(datos.getNombre(), MAX_NOMBRE)) {
            return ResultadoCatalogo.error(MSG_NOMBRE_INVALIDO, ResultadoCatalogo.CAMPO_NOMBRE);
        }
        if (!ValidacionCatalogo.largoValido(datos.getDescripcion(), MAX_DESCRIPCION)) {
            return ResultadoCatalogo.error("La descripción no puede pasar de 255 caracteres",
                    ResultadoCatalogo.CAMPO_DESCRIPCION);
        }
        double presupuesto = datos.getPresupuestoSeleccion();
        if (presupuesto <= 0 || presupuesto > MAX_PRESUPUESTO || !ValidacionCatalogo.sinDecimales(presupuesto)) {
            return ResultadoCatalogo.error(MSG_PRESUPUESTO_INVALIDO, ResultadoCatalogo.CAMPO_PRESUPUESTO);
        }
        if (datos.getOrdenMostrar() < 1 || datos.getOrdenMostrar() > MAX_ORDEN) {
            return ResultadoCatalogo.error(MSG_ORDEN_INVALIDO, ResultadoCatalogo.CAMPO_ORDEN);
        }
        datos.setNombre(datos.getNombre().trim());
        return null;
    }
}
