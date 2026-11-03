package com.pac.modulos.configuracion.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.dao.conexion.ConexionBD;
import com.pac.modulos.catalogo.dao.mysql.ApoyoMySQL;
import com.pac.modulos.configuracion.dao.CoberturaDAO;
import com.pac.modulos.configuracion.dao.ConfiguracionDAO;
import com.pac.modulos.configuracion.model.CambioConfiguracion;
import com.pac.modulos.configuracion.model.CodigoPostalCobertura;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Zona de cobertura de envío a domicilio (tabla CodigoPostalCobertura, RF-9).
 * Cada alta, cambio de costo o baja se anota en CambioConfiguracion con la clave "CP 12345".
 */
public class CoberturaService {

    public static final String MSG_CP_INVALIDO = "El código postal debe tener 5 dígitos";
    public static final String MSG_COSTO_INVALIDO = "El costo de envío debe ser un número de 0 a 9,999.99";
    public static final String MSG_CP_REPETIDO = "Ese código postal ya está en la cobertura";
    public static final String MSG_CP_NO_EXISTE = "Ese código postal ya no está en la cobertura";
    public static final String MSG_ERROR_GUARDAR = "No fue posible guardar la cobertura";

    private static final double COSTO_MAXIMO = 9999.99;

    private CoberturaDAO coberturaDAO;
    private ConfiguracionDAO configuracionDAO;

    public CoberturaService() {
        coberturaDAO = DAOFactory.crearCoberturaDAO();
        configuracionDAO = DAOFactory.crearConfiguracionDAO();
    }

    public List<CodigoPostalCobertura> buscar(String inicioCodigo) throws DAOException {
        return coberturaDAO.buscar(inicioCodigo);
    }

    // Agrega un código postal nuevo
    public ResultadoConfiguracion agregar(String codigoTexto, String costoTexto) {
        String codigo = limpiar(codigoTexto);
        if (!codigoValido(codigo)) {
            return ResultadoConfiguracion.error(MSG_CP_INVALIDO, ResultadoConfiguracion.CAMPO_CODIGO_POSTAL);
        }
        double costo = leerCosto(costoTexto);
        if (costo < 0) {
            return ResultadoConfiguracion.error(MSG_COSTO_INVALIDO, ResultadoConfiguracion.CAMPO_COSTO);
        }
        try {
            if (coberturaDAO.buscarPorCodigo(codigo) != null) {
                return ResultadoConfiguracion.error(MSG_CP_REPETIDO, ResultadoConfiguracion.CAMPO_CODIGO_POSTAL);
            }
        } catch (DAOException e) {
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        }
        CodigoPostalCobertura nueva = new CodigoPostalCobertura(codigo, costo);
        CambioConfiguracion cambio = new CambioConfiguracion("CP " + codigo, null, textoCosto(costo),
                PermisosConfiguracion.idUsuarioSesion());
        return guardar(nueva, cambio, false);
    }

    // Cambia el costo de un código postal que ya está en la cobertura
    public ResultadoConfiguracion cambiarCosto(String codigo, String costoTexto) {
        double costo = leerCosto(costoTexto);
        if (costo < 0) {
            return ResultadoConfiguracion.error(MSG_COSTO_INVALIDO, ResultadoConfiguracion.CAMPO_COSTO);
        }
        CodigoPostalCobertura actual;
        try {
            actual = coberturaDAO.buscarPorCodigo(codigo);
        } catch (DAOException e) {
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        }
        if (actual == null) {
            return ResultadoConfiguracion.error(MSG_CP_NO_EXISTE, ResultadoConfiguracion.CAMPO_CODIGO_POSTAL);
        }
        if (Math.abs(actual.getCostoEnvio() - costo) < 0.005) {
            return ResultadoConfiguracion.correcto(ConfiguracionService.MSG_SIN_CAMBIOS);
        }
        CambioConfiguracion cambio = new CambioConfiguracion("CP " + codigo, textoCosto(actual.getCostoEnvio()),
                textoCosto(costo), PermisosConfiguracion.idUsuarioSesion());
        actual.setCostoEnvio(costo);
        return guardar(actual, cambio, true);
    }

    // Quita un código postal de la cobertura
    public ResultadoConfiguracion eliminar(String codigo) {
        CodigoPostalCobertura actual;
        try {
            actual = coberturaDAO.buscarPorCodigo(codigo);
        } catch (DAOException e) {
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        }
        if (actual == null) {
            return ResultadoConfiguracion.error(MSG_CP_NO_EXISTE, ResultadoConfiguracion.CAMPO_CODIGO_POSTAL);
        }
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);
            coberturaDAO.eliminar(conexion, codigo);
            configuracionDAO.registrarCambio(conexion, new CambioConfiguracion("CP " + codigo,
                    textoCosto(actual.getCostoEnvio()), null, PermisosConfiguracion.idUsuarioSesion()));
            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoConfiguracion.error(MSG_ERROR_GUARDAR, ResultadoConfiguracion.CAMPO_CONEXION);
        } catch (DAOException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
        return ResultadoConfiguracion.correcto("Código postal " + codigo + " quitado de la cobertura");
    }

    // Inserta o actualiza el código postal junto con su bitácora (en una sola transacción)
    private ResultadoConfiguracion guardar(CodigoPostalCobertura cobertura, CambioConfiguracion cambio, boolean existente) {
        Connection conexion = null;
        try {
            conexion = ConexionBD.getInstancia().getConexion();
            conexion.setAutoCommit(false);
            if (existente) {
                coberturaDAO.actualizar(conexion, cobertura);
            } else {
                coberturaDAO.insertar(conexion, cobertura);
            }
            configuracionDAO.registrarCambio(conexion, cambio);
            conexion.commit();
        } catch (SQLException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoConfiguracion.error(MSG_ERROR_GUARDAR, ResultadoConfiguracion.CAMPO_CONEXION);
        } catch (DAOException e) {
            ApoyoMySQL.deshacer(conexion);
            return ResultadoConfiguracion.error(e.getMessage(), ResultadoConfiguracion.CAMPO_CONEXION);
        } finally {
            ApoyoMySQL.regresarAutoCommit(conexion);
        }
        if (existente) {
            return ResultadoConfiguracion.correcto("Costo de envío de " + cobertura.getCodigoPostal() + " actualizado");
        }
        return ResultadoConfiguracion.correcto("Código postal " + cobertura.getCodigoPostal() + " agregado a la cobertura");
    }

    // ===================== Apoyo =====================

    private String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim();
    }

    // 5 dígitos exactos
    public boolean codigoValido(String codigo) {
        return codigo != null && codigo.matches("[0-9]{5}");
    }

    // Lee el costo; devuelve -1 si no es válido. Acepta "$" y comas de miles
    public double leerCosto(String texto) {
        String limpio = limpiar(texto).replace("$", "").replace(",", "");
        if (limpio.isEmpty()) {
            return -1;
        }
        try {
            double costo = Double.parseDouble(limpio);
            if (costo < 0 || costo > COSTO_MAXIMO) {
                return -1;
            }
            return Math.round(costo * 100) / 100.0;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String textoCosto(double costo) {
        return String.format(Locale.US, "%.2f", costo);
    }
}
