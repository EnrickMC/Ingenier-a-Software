package com.pac.modulos.catalogo.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.dao.CaracteristicaDAO;
import com.pac.modulos.catalogo.dao.ExclusionDAO;
import com.pac.modulos.catalogo.dao.OpcionDAO;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.Exclusion;
import com.pac.modulos.catalogo.model.Opcion;

import com.pac.util.SelectorImagen;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de las opciones de cada característica
 * (Registro, Modificación, Eliminación y Visualización de opción de característica).
 */
public class OpcionService {

    // ----- Mensajes de los casos de uso -----
    public static final String MSG_ERROR_CONEXION = "Error de conexión";
    public static final String MSG_NO_ENCONTRADA = "Opción no encontrada";
    public static final String MSG_CARACTERISTICA_NO_DISPONIBLE = "La característica ya no está disponible";
    public static final String MSG_CARACTERISTICA_NO_ENCONTRADA = "Característica no encontrada";
    public static final String MSG_SIN_OPCIONES = "Esta característica no tiene opciones registradas";
    public static final String MSG_INCOMPLETAS = "Algunas opciones no pudieron cargarse";
    public static final String MSG_SIN_RESULTADOS = "Sin resultados para el criterio ingresado";
    public static final String MSG_YA_EXISTE = "La opción ya existe en esta característica";
    public static final String MSG_PUNTOS_EXCEDEN = "Los puntos superan el presupuesto de la característica";
    public static final String MSG_NO_GUARDADA = "No fue posible guardar la opción";
    public static final String MSG_NO_ACTUALIZADA = "No fue posible actualizar la opción";
    public static final String MSG_NO_DADA_DE_BAJA = "No fue posible dar de baja la opción";
    public static final String MSG_EN_PEDIDOS = "La opción está en pedidos en curso";
    public static final String MSG_NOMBRE_INVALIDO =
            "Escribe el nombre de la opción (máximo 60 caracteres, solo letras, números y espacios)";
    public static final String MSG_MULTIPLICADOR_INVALIDO = "Los multiplicadores deben ser mayores a cero y menores a 100";
    public static final String MSG_PUNTOS_INVALIDOS = "Los puntos de selección deben ser mayores a cero";
    public static final String MSG_IMAGEN_INVALIDA = "La imagen debe ser un enlace http:// o https://, o un archivo existente de tipo " + SelectorImagen.FORMATOS_TEXTO;

    private static final int MAX_NOMBRE = 60;
    private static final int MAX_TEXTO = 255;
    private static final double MAX_MULTIPLICADOR = 99.99;
    private static final double MAX_PUNTOS = 9999.99;

    private OpcionDAO opcionDAO;
    private CaracteristicaDAO caracteristicaDAO;
    private ExclusionDAO exclusionDAO;

    public OpcionService() {
        opcionDAO = DAOFactory.crearOpcionDAO();
        caracteristicaDAO = DAOFactory.crearCaracteristicaDAO();
        exclusionDAO = DAOFactory.crearExclusionDAO();
    }

    // ===================== Consultas (Visualización) =====================

    // Opciones de una característica (con su costo de receta)
    // RF «Visualización de opciones de característica» · Paso 3: consulta las opciones de la característica.
    public List<Opcion> listarPorCaracteristica(int idCaracteristica) throws DAOException {
        return opcionDAO.listarPorCaracteristica(idCaracteristica);
    }

    // Opciones de todas las características de una categoría
    public List<Opcion> listarPorCategoria(int idCategoria) throws DAOException {
        return opcionDAO.listarPorCategoria(idCategoria);
    }

    // Lee una opción; devuelve null si ya no existe
    public Opcion buscarPorId(int idOpcion) throws DAOException {
        return opcionDAO.buscarPorId(idOpcion);
    }

    // Exclusiones en las que participa la opción
    public List<Exclusion> listarExclusiones(int idOpcion) throws DAOException {
        return exclusionDAO.listarPorOpcion(idOpcion);
    }

    // Lee la característica (para saber su presupuesto y si es múltiple); null si no existe
    public Caracteristica buscarCaracteristica(int idCaracteristica) throws DAOException {
        return caracteristicaDAO.buscarPorId(idCaracteristica);
    }

    // Una opción tiene datos completos si tiene nombre
    // RF «Visualización de opciones de característica» · Paso 4: valida que los datos de la opción estén completos.
    public boolean datosCompletos(Opcion opcion) {
        return !ValidacionCatalogo.estaVacio(opcion.getNombre());
    }

    // Paso 2 del registro: la característica debe existir y estar disponible
    // RF «Registro de opción de característica» · Paso 2: la característica debe seguir existiendo y activa.
    public ResultadoCatalogo revisarCaracteristicaParaAlta(int idCaracteristica) {
        try {
            Caracteristica caracteristica = caracteristicaDAO.buscarPorId(idCaracteristica);
            // RF «Registro de opción de característica» · Excepción paso 2: «La característica ya no está disponible».
            if (caracteristica == null || !caracteristica.isDisponible()) {
                return ResultadoCatalogo.noEncontrado(MSG_CARACTERISTICA_NO_DISPONIBLE);
            }
            return ResultadoCatalogo.correcto("");
        } catch (DAOException e) {
            // RF «Registro de opción de característica» · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }
    }

    // ===================== Registro =====================

    // RF «Registro de opción de característica»: inicia aquí la regla de negocio (pasos 5 a 7).
    public ResultadoCatalogo registrar(Opcion datos) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        ResultadoCatalogo validacion = validarCampos(datos);
        // RF «Registro de opción de característica» · Excepción paso 5: campos incompletos o valores inválidos.
        if (validacion != null) {
            return validacion;
        }

        try {
            Caracteristica caracteristica = caracteristicaDAO.buscarPorId(datos.getIdCaracteristica());
            // RF «Registro de opción de característica» · Excepción paso 2: la característica ya no está disponible.
            if (caracteristica == null || !caracteristica.isDisponible()) {
                return ResultadoCatalogo.noEncontrado(MSG_CARACTERISTICA_NO_DISPONIBLE);
            }
            // Paso 6: nombre y presupuesto
            // RF «Registro de opción de característica» · Excepción paso 6: «La opción ya existe en esta característica».
            if (opcionDAO.buscarPorNombre(datos.getIdCaracteristica(), datos.getNombre()) != null) {
                return ResultadoCatalogo.error(MSG_YA_EXISTE, ResultadoCatalogo.CAMPO_NOMBRE);
            }
            ResultadoCatalogo presupuesto = revisarPresupuesto(datos, caracteristica);
            // RF «Registro de opción de característica» · Excepción paso 6: «Los puntos superan el presupuesto de la característica».
            if (presupuesto != null) {
                return presupuesto;
            }
        } catch (DAOException e) {
            // RF «Registro de opción de característica» · Excepción: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // RF «Registro de opción de característica» · Paso 7: almacena la opción e informa el éxito.
        try {
            opcionDAO.insertar(datos);
        } catch (DAOException e) {
            // RF «Registro de opción de característica» · Excepción paso 7: «No fue posible guardar la opción».
            return ResultadoCatalogo.error(MSG_NO_GUARDADA, ResultadoCatalogo.CAMPO_CONEXION);
        }
        return ResultadoCatalogo.correcto("La opción «" + datos.getNombre() + "» se registró correctamente");
    }

    // ===================== Modificación =====================

    // confirmado = la administradora ya aceptó el aviso sobre precios y carritos
    // RF «Modificación de opción de característica»: inicia aquí la regla de negocio (pasos 5 a 8).
    public ResultadoCatalogo modificar(Opcion datos, boolean confirmado) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }

        ResultadoCatalogo validacion = validarCampos(datos);
        // RF «Modificación de opción de característica» · Excepción paso 5: datos inválidos.
        if (validacion != null) {
            return validacion;
        }

        Opcion actual;
        try {
            actual = opcionDAO.buscarPorId(datos.getIdOpcion());
            // RF «Modificación de opción de característica» · Excepción paso 2: «Opción no encontrada».
            if (actual == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }
            datos.setIdCaracteristica(actual.getIdCaracteristica());

            Opcion conMismoNombre = opcionDAO.buscarPorNombre(actual.getIdCaracteristica(), datos.getNombre());
            // RF «Modificación de opción de característica» · Excepción paso 6: el nombre ya existe en la característica.
            if (conMismoNombre != null && conMismoNombre.getIdOpcion() != datos.getIdOpcion()) {
                return ResultadoCatalogo.error(MSG_YA_EXISTE, ResultadoCatalogo.CAMPO_NOMBRE);
            }
            Caracteristica caracteristica = caracteristicaDAO.buscarPorId(actual.getIdCaracteristica());
            if (caracteristica != null) {
                ResultadoCatalogo presupuesto = revisarPresupuesto(datos, caracteristica);
                // RF «Modificación de opción de característica» · Excepción paso 6: los puntos no caben en el presupuesto.
                if (presupuesto != null) {
                    return presupuesto;
                }
            }
        } catch (DAOException e) {
            // RF «Modificación de opción de característica» · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }

        // Paso 7: si cambió el multiplicador de costo o la disponibilidad, se avisa
        boolean cambioCosto = Math.abs(actual.getMultiplicadorCosto() - datos.getMultiplicadorCosto()) > 0.001;
        boolean cambioDisponible = actual.isDisponible() != datos.isDisponible();
        // RF «Modificación de opción de característica» · Paso 7: los pedidos enviados conservan su precio y los carritos se recalculan; se pide confirmar.
        if ((cambioCosto || cambioDisponible) && !confirmado) {
            String mensaje = "Los pedidos ya enviados conservan su precio y sus opciones originales."
                    + " Los carritos que tengan esta opción se recalcularán.\n\n¿Deseas continuar?";
            return ResultadoCatalogo.confirmacion(mensaje, null);
        }

        // RF «Modificación de opción de característica» · Paso 8: actualiza la opción.
        try {
            opcionDAO.actualizar(datos);
        } catch (DAOException e) {
            // RF «Modificación de opción de característica» · Excepción paso 8: «No fue posible actualizar la opción».
            return ResultadoCatalogo.error(MSG_NO_ACTUALIZADA, ResultadoCatalogo.CAMPO_CONEXION);
        }
        return ResultadoCatalogo.correcto("La opción «" + datos.getNombre() + "» se actualizó correctamente");
    }

    // ===================== Baja (Eliminación de opción) =====================

    // Pasos 2 a 5: revisa si se puede dar de baja y arma la confirmación doble
    // RF «Eliminación de opción de característica»: inicia aquí la regla de negocio (pasos 2 a 5).
    public ResultadoCatalogo prepararBaja(int idOpcion) {
        if (!PermisosCatalogo.puedeEditarCatalogo()) {
            return ResultadoCatalogo.error(PermisosCatalogo.MSG_SOLO_CONSULTA, null);
        }
        try {
            Opcion opcion = opcionDAO.buscarPorId(idOpcion);
            // RF «Eliminación de opción de característica» · Excepción paso 2: «Opción no encontrada».
            if (opcion == null) {
                return ResultadoCatalogo.noEncontrado(MSG_NO_ENCONTRADA);
            }

            // Paso 3: pedidos vigentes
            List<Integer> folios = opcionDAO.listarFoliosVigentes(idOpcion);
            // RF «Eliminación de opción de característica» · Excepción paso 3: «La opción está en pedidos en curso» con sus folios.
            if (!folios.isEmpty()) {
                return ResultadoCatalogo.error(MSG_EN_PEDIDOS + ": " + ValidacionCatalogo.unirFolios(folios), null);
            }

            String mensaje = "Opción: " + opcion.getNombreCaracteristica() + " · " + opcion.getNombre();

            // Exclusiones que también se darán de baja
            List<String> pares = new ArrayList<String>();
            for (Exclusion exclusion : exclusionDAO.listarPorOpcion(idOpcion)) {
                pares.add(exclusion.getNombreOpcion() + " ✕ " + exclusion.getNombreOpcionExcluida());
            }
            if (pares.isEmpty()) {
                mensaje = mensaje + "\nExclusiones: ninguna";
            } else {
                mensaje = mensaje + "\nExclusiones: " + ValidacionCatalogo.unir(pares);
            }

            // Paso 4: última opción disponible de una característica obligatoria
            Caracteristica caracteristica = caracteristicaDAO.buscarPorId(opcion.getIdCaracteristica());
            // RF «Eliminación de opción de característica» · Excepción paso 4: última opción disponible de una obligatoria; se agrega el aviso a la confirmación.
            if (caracteristica != null && caracteristica.isObligatoria() && opcion.isDisponible()
                    && opcionDAO.contarDisponibles(opcion.getIdCaracteristica()) <= 1) {
                mensaje = mensaje + "\n\nAVISO: es la última opción disponible de «" + caracteristica.getNombre()
                        + "», que es obligatoria. Los productos de esa categoría no podrán personalizarse"
                        + " hasta registrar otra opción.";
            }

            boolean historial = opcionDAO.tieneHistorial(idOpcion);
            if (historial) {
                mensaje = mensaje + "\n\nLa opción ya se usó en postres o tiene receta, así que se INHABILITARÁ"
                        + " (no disponible; su receta y sus exclusiones quedarán inactivas).";
            } else {
                mensaje = mensaje + "\n\nNo tiene relaciones, así que se ELIMINARÁ junto con sus exclusiones."
                        + " Esta acción no se puede deshacer.";
            }
            // RF «Eliminación de opción de característica» · Paso 5: mensaje de la confirmación doble con las exclusiones afectadas.
            return ResultadoCatalogo.bajaPosible(mensaje, !historial);
        } catch (DAOException e) {
            // RF «Eliminación de opción de característica» · Excepción paso 2: sin conexión, «Error de conexión».
            return ResultadoCatalogo.error(MSG_ERROR_CONEXION, ResultadoCatalogo.CAMPO_CONEXION);
        }
    }

    // Paso 7: registra la baja
    // RF «Eliminación de opción de característica» · Paso 7 (después de que la Administradora confirmó en el paso 6).
    public ResultadoCatalogo darDeBaja(int idOpcion) {
        ResultadoCatalogo revision = prepararBaja(idOpcion);
        if (!revision.isExitoso()) {
            return revision;
        }
        try {
            // RF «Eliminación de opción de característica» · Paso 7: elimina si no tiene relaciones; si tiene historial, inhabilita la opción, sus exclusiones y su receta.
            if (revision.isSeEliminara()) {
                opcionDAO.eliminar(idOpcion);
                return ResultadoCatalogo.correcto("La opción se eliminó correctamente");
            }
            opcionDAO.inhabilitar(idOpcion);
            return ResultadoCatalogo.correcto("La opción se inhabilitó correctamente");
        } catch (DAOException e) {
            // RF «Eliminación de opción de característica» · Excepción paso 7: «No fue posible dar de baja la opción».
            return ResultadoCatalogo.error(MSG_NO_DADA_DE_BAJA, null);
        }
    }

    // ===================== Apoyo =====================

    // En características de selección múltiple, los puntos no pueden pasar del presupuesto
    // RF «Registro de opción de característica» / RF «Modificación de opción de característica» · Paso 6: los puntos de selección no pueden superar el presupuesto.
    private ResultadoCatalogo revisarPresupuesto(Opcion datos, Caracteristica caracteristica) {
        if (!caracteristica.isSeleccionMultiple()) {
            return null;
        }
        if (datos.getPuntosSeleccion() > caracteristica.getPresupuestoSeleccion()) {
            return ResultadoCatalogo.error(MSG_PUNTOS_EXCEDEN + " (presupuesto actual: "
                    + ValidacionCatalogo.numero(caracteristica.getPresupuestoSeleccion()) + " puntos)",
                    ResultadoCatalogo.CAMPO_PUNTOS);
        }
        return null;
    }

    // Revisa los campos del formulario; devuelve null si todo está bien
    // RF «Registro de opción de característica» / RF «Modificación de opción de característica» · Paso 5: campos obligatorios, multiplicadores mayores a cero y puntos válidos.
    private ResultadoCatalogo validarCampos(Opcion datos) {
        if (!ValidacionCatalogo.nombreValido(datos.getNombre(), MAX_NOMBRE)) {
            return ResultadoCatalogo.error(MSG_NOMBRE_INVALIDO, ResultadoCatalogo.CAMPO_NOMBRE);
        }
        if (!ValidacionCatalogo.largoValido(datos.getDescripcion(), MAX_TEXTO)) {
            return ResultadoCatalogo.error("La descripción no puede pasar de 255 caracteres",
                    ResultadoCatalogo.CAMPO_DESCRIPCION);
        }
        if (!ValidacionCatalogo.imagenOpcionalValida(datos.getLinkImagen())
                || !ValidacionCatalogo.largoValido(datos.getLinkImagen(), MAX_TEXTO)) {
            return ResultadoCatalogo.error(MSG_IMAGEN_INVALIDA, ResultadoCatalogo.CAMPO_IMAGEN);
        }
        if (!multiplicadorValido(datos.getMultiplicadorIngredientes())) {
            return ResultadoCatalogo.error(MSG_MULTIPLICADOR_INVALIDO, ResultadoCatalogo.CAMPO_MULT_INGREDIENTES);
        }
        if (!multiplicadorValido(datos.getMultiplicadorCosto())) {
            return ResultadoCatalogo.error(MSG_MULTIPLICADOR_INVALIDO, ResultadoCatalogo.CAMPO_MULT_COSTO);
        }
        if (!multiplicadorValido(datos.getMultiplicadorPresupuesto())) {
            return ResultadoCatalogo.error(MSG_MULTIPLICADOR_INVALIDO, ResultadoCatalogo.CAMPO_MULT_PRESUPUESTO);
        }
        if (datos.getPuntosSeleccion() <= 0 || datos.getPuntosSeleccion() > MAX_PUNTOS) {
            return ResultadoCatalogo.error(MSG_PUNTOS_INVALIDOS, ResultadoCatalogo.CAMPO_PUNTOS);
        }
        datos.setNombre(datos.getNombre().trim());
        return null;
    }

    // Columnas DECIMAL(4,2) con CHECK > 0
    private boolean multiplicadorValido(double valor) {
        return valor > 0 && valor <= MAX_MULTIPLICADOR;
    }
}
