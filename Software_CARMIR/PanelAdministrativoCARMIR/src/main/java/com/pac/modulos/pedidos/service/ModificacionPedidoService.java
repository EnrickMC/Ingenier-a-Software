package com.pac.modulos.pedidos.service;

import com.pac.dao.DAOException;
import com.pac.dao.DAOFactory;
import com.pac.modulos.catalogo.dao.ProductoDAO;
import com.pac.modulos.catalogo.model.Caracteristica;
import com.pac.modulos.catalogo.model.OpcionElegida;
import com.pac.modulos.catalogo.model.Producto;
import com.pac.modulos.catalogo.service.ConfiguracionPostre;
import com.pac.modulos.catalogo.service.ProductoService;
import com.pac.modulos.pedidos.dao.BitacoraPedidoDAO;
import com.pac.modulos.pedidos.dao.ClienteDAO;
import com.pac.modulos.pedidos.dao.PedidoDAO;
import com.pac.modulos.pedidos.model.Cliente;
import com.pac.modulos.pedidos.model.Direccion;
import com.pac.modulos.pedidos.model.EstadoPedido;
import com.pac.modulos.pedidos.model.LineaPedido;
import com.pac.modulos.pedidos.model.ModalidadEntrega;
import com.pac.modulos.pedidos.model.ModalidadPago;
import com.pac.modulos.pedidos.model.Pedido;
import com.pac.modulos.pedidos.notificacion.NotificacionService;
import com.pac.modulos.pedidos.notificacion.PlantillasMensaje;
import com.pac.util.ConfiguracionApp;
import com.pac.util.Formatos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modificación de pedidos (CRUD "Modificación de pedidos", solo la Administradora).
 *
 * Se pueden cambiar: datos del cliente, fecha y hora de entrega, modalidad y dirección,
 * postres (cantidad, dedicatoria, comentario y personalización).
 * Solo antes de «En preparación». La personalización se valida con ConfiguracionPostre (Módulo 3).
 * El total y el anticipo se recalculan; lo que cambió se guarda en ModificacionPedido.
 */
public class ModificacionPedidoService {

    public static final String MSG_NO_ADMITE_CAMBIOS = "Este pedido ya no admite cambios";
    public static final String MSG_YA_EN_PRODUCCION = "El pedido ya entró a producción y no puede modificarse";
    public static final String MSG_NO_MODIFICADO = "No fue posible modificar el pedido";
    public static final String MSG_SIN_CAMBIOS = "No hiciste ningún cambio al pedido";
    public static final String MSG_SIN_POSTRES = "El pedido debe tener al menos un postre";
    public static final String MSG_TELEFONO_EN_USO = "Ese teléfono ya pertenece a otro cliente";
    public static final String MSG_POSTRE_REPETIDO =
            "El mismo postre aparece dos veces; deja una sola línea y suma las piezas";

    private PedidoDAO pedidoDAO;
    private ClienteDAO clienteDAO;
    private BitacoraPedidoDAO bitacoraDAO;
    private ProductoDAO productoDAO;
    private ProductoService productoService;
    private PedidoService pedidoService;

    public ModificacionPedidoService() {
        pedidoDAO = DAOFactory.crearPedidoDAO();
        clienteDAO = DAOFactory.crearClienteDAO();
        bitacoraDAO = DAOFactory.crearBitacoraPedidoDAO();
        productoDAO = DAOFactory.crearProductoDAO();
        productoService = new ProductoService();
        pedidoService = new PedidoService();
    }

    // Motivo por el que el pedido no se puede modificar, o null si sí se puede
    // RF-34 · Paso 2: revisa si el pedido se puede modificar.
    public String motivoNoModificable(Pedido pedido) {
        if (!PermisosPedidos.esAdministradora()) {
            return PermisosPedidos.MSG_SOLO_ADMINISTRADORA;
        }
        if (pedido.isCancelado() || pedido.isDadoDeBaja()) {
            // RF-34 · Excepción paso 2: «Este pedido ya no admite cambios».
            return MSG_NO_ADMITE_CAMBIOS;
        }
        if (!pedido.getEstado().esAnteriorA(EstadoPedido.EN_PREPARACION)) {
            // RF-34 · Excepción paso 2: el pedido ya entró a producción.
            return MSG_YA_EN_PRODUCCION;
        }
        return null;
    }

    /**
     * Lee el pedido para el formulario: el detalle completo y las opciones de cada postre
     * (para poder cambiar su personalización).
     */
    // RF-34 (Modificación de pedidos): inicia aquí. Pasos 2 y 3: lee el pedido para mostrar el formulario.
    public ResultadoPedido prepararModificacion(int folio) {
        ResultadoPedido lectura = pedidoService.cargarDetalle(folio);
        if (!lectura.isExitoso()) {
            return lectura;
        }
        Pedido pedido = lectura.getPedido();
        String motivo = motivoNoModificable(pedido);
        if (motivo != null) {
            return ResultadoPedido.error(motivo, null);
        }
        try {
            for (LineaPedido linea : pedido.getLineas()) {
                Producto postre = productoDAO.buscarPorId(linea.getIdPostre());
                if (postre != null) {
                    linea.setOpciones(postre.getOpciones());
                }
            }
        } catch (DAOException e) {
            // RF-34 · Excepción paso 2: sin conexión, «Error de conexión»; no se abre el formulario.
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        }
        return lectura;
    }

    // Configuración de la categoría para el diálogo de personalización
    public ConfiguracionPostre cargarConfiguracion(int idCategoria) throws DAOException {
        return productoService.cargarConfiguracion(idCategoria);
    }

    /**
     * Revisa los cambios sin guardar nada: valida los datos, calcula los precios de los postres
     * cuya personalización cambió y el nuevo total y anticipo.
     * Si todo está bien, el mensaje trae el resumen "antes → después" y los avisos de pagos.
     */
    // RF-34 · Pasos 5 a 7: valida los cambios y recalcula el total.
    public ResultadoPedido revisarCambios(Pedido original, Pedido cambios) {
        ResultadoPedido error = validarDatos(original, cambios);
        // RF-34 · Excepción paso 5: datos inválidos; se resalta el campo.
        if (error != null) {
            return error;
        }
        try {
            error = prepararLineas(cambios);
        } catch (DAOException e) {
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        }
        // RF-34 · Excepción paso 6: opciones no disponibles o incompatibles.
        if (error != null) {
            return error;
        }

        // RF-34 · Paso 7: nuevo total y anticipo.
        // Nuevo total y anticipo (se conserva la proporción del anticipo original)
        double total = 0;
        for (LineaPedido linea : cambios.getLineas()) {
            total = total + linea.getSubtotal();
        }
        // Pedidos de la página web: el envío va dentro del total y se conserva si sigue a domicilio
        if (cambios.getModalidadEntrega() == ModalidadEntrega.DOMICILIO) {
            total = total + original.getCostoEnvio();
        }
        total = PedidoService.redondear(total);
        cambios.setMontoTotal(total);
        cambios.setModalidadPago(original.getModalidadPago());
        cambios.setTotalPagado(original.getTotalPagado());
        cambios.setEstado(original.getEstado());
        if (original.getModalidadPago() == ModalidadPago.PAGO_TOTAL) {
            cambios.setAnticipoRequerido(total);
        } else if (original.getMontoTotal() > 0) {
            double proporcion = original.getAnticipoRequerido() / original.getMontoTotal();
            cambios.setAnticipoRequerido(PedidoService.redondear(total * proporcion));
        } else {
            cambios.setAnticipoRequerido(PedidoService.redondear(total / 2));
        }

        String[] textos = describirCambios(original, cambios);
        if (textos[0].isEmpty() && textos[1].isEmpty()) {
            return ResultadoPedido.error(MSG_SIN_CAMBIOS, null);
        }
        ResultadoPedido resultado = ResultadoPedido.correcto("Antes:\n" + textos[0] + "\n\nDespués:\n" + textos[1]);
        agregarAvisosDePago(resultado, original, cambios);
        try {
            if (cambios.getFechaEntrega() != null && !cambios.getFechaEntrega().equals(original.getFechaEntrega())) {
                int carga = pedidoService.cargaDelDia(cambios.getFechaEntrega(), original.getFolio());
                if (carga >= ConfiguracionApp.getInstancia().getCapacidadDiaria()) {
                    // RF-33 · Excepción paso 5: «Agenda saturada para esa fecha» en la nueva fecha.
                    resultado.agregarAviso(PedidoService.MSG_AGENDA_SATURADA + ".");
                }
            }
        } catch (DAOException e) {
            System.out.println("No se pudo revisar la carga del día: " + e.getMessage());
        }
        resultado.setPedido(cambios);
        return resultado;
    }

    /**
     * Guarda la modificación en una sola transacción y avisa al cliente.
     */
    // RF-34 · Pasos 9 a 11: guarda los cambios (después de que la administradora confirma, paso 8).
    public ResultadoPedido modificar(Pedido original, Pedido cambios) {
        String motivo = motivoNoModificable(original);
        if (motivo != null) {
            return ResultadoPedido.error(motivo, null);
        }
        ResultadoPedido revision = revisarCambios(original, cambios);
        if (!revision.isExitoso()) {
            return revision;
        }
        String[] textos = describirCambios(original, cambios);

        // Los postres personalizados nuevos se registran antes (ocultos en la tienda, se reutilizan después)
        try {
            for (LineaPedido linea : cambios.getLineas()) {
                if (linea.isPersonalizacionCambiada() && linea.getIdPostre() == 0) {
                    linea.setIdPostre(registrarPostre(linea));
                }
            }
        } catch (DAOException e) {
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_MODIFICADO, e), null);
        }

        Connection conexion = null;
        try {
            conexion = ApoyoTransaccion.abrir();
            ResultadoPedido bloqueo = ApoyoTransaccion.bloquearYRevisar(pedidoDAO, conexion, original);
            if (bloqueo != null) {
                ApoyoTransaccion.deshacer(conexion);
                return bloqueo;
            }
            Pedido actual = pedidoDAO.buscarPorFolio(original.getFolio());
            String impedimento = motivoNoModificable(actual);
            if (impedimento != null) {
                ApoyoTransaccion.deshacer(conexion);
                // RF-34 · Excepción paso 9: el pedido fue actualizado por otro usuario; se recarga.
                return ResultadoPedido.recargar(impedimento);
            }

            // 1. Cliente
            Cliente cliente = cambios.getCliente();
            cliente.setIdCliente(actual.getCliente().getIdCliente());
            if (!mismoCliente(actual.getCliente(), cliente)) {
                if (clienteDAO.telefonoEnUso(cliente.getTelefono().trim(), cliente.getIdCliente())) {
                    ApoyoTransaccion.deshacer(conexion);
                    return ResultadoPedido.error(MSG_TELEFONO_EN_USO, ResultadoPedido.CAMPO_TELEFONO);
                }
                clienteDAO.actualizar(conexion, cliente);
            }

            // 2. Dirección de entrega
            if (cambios.getModalidadEntrega() == ModalidadEntrega.DOMICILIO) {
                Direccion nueva = cambios.getDireccion();
                if (!nueva.mismosDatos(actual.getDireccion())) {
                    nueva.setIdCliente(cliente.getIdCliente());
                    int idDireccion = clienteDAO.insertarDireccion(conexion, nueva);
                    clienteDAO.asignarDireccion(conexion, actual.getFolio(), idDireccion);
                }
            } else if (actual.getDireccion() != null) {
                clienteDAO.quitarDireccion(conexion, actual.getFolio());
            }

            // 3. Fecha, entrega, total y anticipo
            cambios.setFolio(actual.getFolio());
            pedidoDAO.actualizarDatos(conexion, cambios);

            // 4. Postres (solo si cambiaron)
            if (!textoLineas(original.getLineas()).equals(textoLineas(cambios.getLineas()))) {
                pedidoDAO.reemplazarLineas(conexion, actual.getFolio(), cambios.getLineas());
            }

            // RF-34 · Paso 9: registra en el historial qué cambió, la fecha y el usuario.
            // 5. Bitácora de la modificación
            bitacoraDAO.registrarModificacion(conexion, actual.getFolio(), PermisosPedidos.idUsuarioSesion(),
                    textos[0], textos[1]);
            conexion.commit();
        } catch (DAOException e) {
            // RF-34 · Excepción paso 9: se deshace la operación, «No fue posible modificar el pedido».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.mensajeFallo(MSG_NO_MODIFICADO, e), null);
        } catch (SQLException e) {
            // RF-34 · Excepción paso 2: sin conexión, «Error de conexión».
            ApoyoTransaccion.deshacer(conexion);
            return ResultadoPedido.error(ApoyoTransaccion.MSG_ERROR_CONEXION, ResultadoPedido.CAMPO_CONEXION);
        } finally {
            ApoyoTransaccion.cerrar(conexion);
        }

        ResultadoPedido resultado = ResultadoPedido.correcto("Pedido #" + original.getFolio() + " modificado");
        for (String aviso : revision.getAvisos()) {
            resultado.agregarAviso(aviso);
        }
        resultado.agregarAviso(NotificacionService.getInstancia().notificar(original.getFolio(),
                PlantillasMensaje.MODIFICACION, resumenParaCliente(original, cambios)));
        MonitorPedidos.getInstancia().revisar();
        return resultado;
    }

    // ===================== Validaciones =====================

    // RF-34 · Paso 5: campos obligatorios, fecha futura y formato de teléfono y correo.
    private ResultadoPedido validarDatos(Pedido original, Pedido cambios) {
        // Cliente
        Cliente cliente = cambios.getCliente();
        String nombre = texto(cliente.getNombre());
        if (nombre.isEmpty() || nombre.length() > 100) {
            return ResultadoPedido.error("Indica el nombre del cliente (máximo 100 caracteres)", ResultadoPedido.CAMPO_NOMBRE);
        }
        if (!texto(cliente.getTelefono()).matches("[0-9]{10}")) {
            return ResultadoPedido.error("El teléfono debe tener 10 dígitos", ResultadoPedido.CAMPO_TELEFONO);
        }
        String correo = texto(cliente.getCorreo());
        if (!correo.isEmpty() && (correo.length() > 100 || !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
            return ResultadoPedido.error("Correo electrónico no válido", ResultadoPedido.CAMPO_CORREO);
        }

        // Fecha y hora de entrega: deben ser futuras
        if (cambios.getFechaEntrega() == null) {
            return ResultadoPedido.error("Indica la fecha de entrega", ResultadoPedido.CAMPO_FECHA);
        }
        if (cambios.getHorarioEntrega() == null) {
            return ResultadoPedido.error("Indica la hora de entrega (formato HH:mm)", ResultadoPedido.CAMPO_HORARIO);
        }
        if (cambios.getFechaEntrega().isBefore(LocalDate.now())) {
            return ResultadoPedido.error("La fecha de entrega no puede ser anterior a hoy", ResultadoPedido.CAMPO_FECHA);
        }
        if (PedidoService.yaPaso(cambios.getFechaHoraEntrega())) {
            return ResultadoPedido.error("La hora de entrega ya pasó", ResultadoPedido.CAMPO_HORARIO);
        }

        // Dirección si es a domicilio
        if (cambios.getModalidadEntrega() == null) {
            return ResultadoPedido.error("Elige la modalidad de entrega", ResultadoPedido.CAMPO_DIRECCION);
        }
        if (cambios.getModalidadEntrega() == ModalidadEntrega.DOMICILIO) {
            String error = validarDireccion(cambios.getDireccion());
            if (error != null) {
                return ResultadoPedido.error(error, ResultadoPedido.CAMPO_DIRECCION);
            }
        }

        // Postres
        if (cambios.getLineas().isEmpty()) {
            return ResultadoPedido.error(MSG_SIN_POSTRES, ResultadoPedido.CAMPO_PRODUCTOS);
        }
        for (LineaPedido linea : cambios.getLineas()) {
            String nombrePostre = linea.getNombreMostrar();
            if (linea.getCantidad() < 1 || linea.getCantidad() > 99) {
                return ResultadoPedido.error("La cantidad de «" + nombrePostre + "» debe estar entre 1 y 99",
                        ResultadoPedido.CAMPO_PRODUCTOS);
            }
            if (texto(linea.getDedicatoria()).length() > 150) {
                return ResultadoPedido.error("La dedicatoria de «" + nombrePostre + "» admite máximo 150 caracteres",
                        ResultadoPedido.CAMPO_PRODUCTOS);
            }
            if (texto(linea.getComentario()).length() > 255) {
                return ResultadoPedido.error("El comentario de «" + nombrePostre + "» admite máximo 255 caracteres",
                        ResultadoPedido.CAMPO_PRODUCTOS);
            }
        }
        return null;
    }

    private String validarDireccion(Direccion direccion) {
        if (direccion == null) {
            return "Indica la dirección de entrega";
        }
        if (texto(direccion.getCalle()).isEmpty() || texto(direccion.getCalle()).length() > 100) {
            return "Indica la calle (máximo 100 caracteres)";
        }
        if (texto(direccion.getNumeroExterior()).isEmpty() || texto(direccion.getNumeroExterior()).length() > 10) {
            return "Indica el número exterior (máximo 10 caracteres)";
        }
        if (texto(direccion.getNumeroInterior()).length() > 10) {
            return "El número interior admite máximo 10 caracteres";
        }
        if (texto(direccion.getColonia()).isEmpty() || texto(direccion.getColonia()).length() > 80) {
            return "Indica la colonia (máximo 80 caracteres)";
        }
        if (!texto(direccion.getCodigoPostal()).matches("[0-9]{5}")) {
            return "El código postal debe tener 5 dígitos";
        }
        if (texto(direccion.getReferencias()).length() > 255) {
            return "Las referencias admiten máximo 255 caracteres";
        }
        return null;
    }

    /**
     * Para cada postre con la personalización cambiada: valida las opciones con ConfiguracionPostre,
     * calcula su precio y costo y busca si ya existe un postre con esa configuración.
     */
    // RF-34 · Paso 6: valida la personalización (opciones, exclusiones y presupuesto).
    private ResultadoPedido prepararLineas(Pedido cambios) throws DAOException {
        List<String> claves = new ArrayList<String>();
        for (LineaPedido linea : cambios.getLineas()) {
            if (linea.isPersonalizacionCambiada()) {
                ConfiguracionPostre configuracion = productoService.cargarConfiguracion(linea.getIdCategoria());
                List<String> errores = configuracion.validar(linea.getOpciones());
                if (!errores.isEmpty()) {
                    return ResultadoPedido.error("«" + linea.getNombreMostrar() + "»: " + String.join("\n", errores),
                            ResultadoPedido.CAMPO_PRODUCTOS);
                }
                linea.setPrecioUnitario(ConfiguracionPostre.calcularPrecio(linea.getOpciones()));
                linea.setCostoUnitario(ConfiguracionPostre.calcularCosto(linea.getOpciones()));
                linea.setEspecificaciones(especificaciones(configuracion, linea.getOpciones()));

                // Si ya existe un postre con la misma configuración se reutiliza
                String clave = ConfiguracionPostre.claveConfiguracion(linea.getOpciones());
                Producto existente = productoDAO.buscarPorClave(clave);
                if (existente != null) {
                    linea.setIdPostre(existente.getIdPostre());
                    linea.setNombrePostre(existente.getNombre());
                } else {
                    linea.setIdPostre(0);
                    linea.setNombrePostre(null);
                }
            }
            // No se permite el mismo postre en dos líneas (la tabla usa Folio + IdPostre como llave)
            String llave = "id:" + linea.getIdPostre();
            if (linea.getIdPostre() == 0) {
                llave = "clave:" + ConfiguracionPostre.claveConfiguracion(linea.getOpciones());
            }
            if (claves.contains(llave)) {
                return ResultadoPedido.error(MSG_POSTRE_REPETIDO, ResultadoPedido.CAMPO_PRODUCTOS);
            }
            claves.add(llave);
        }
        return null;
    }

    // "Característica: Opción ×n" de cada opción elegida
    private List<String> especificaciones(ConfiguracionPostre configuracion, List<OpcionElegida> elegidas) {
        List<String> textos = new ArrayList<String>();
        for (Caracteristica caracteristica : configuracion.getCaracteristicas()) {
            for (OpcionElegida elegida : elegidas) {
                if (elegida.getOpcion().getIdCaracteristica() == caracteristica.getIdCaracteristica()) {
                    String texto = caracteristica.getNombre() + ": " + elegida.getOpcion().getNombre();
                    if (elegida.getCantidad() > 1) {
                        texto = texto + " ×" + elegida.getCantidad();
                    }
                    textos.add(texto);
                }
            }
        }
        return textos;
    }

    // Registra el postre personalizado (oculto en la tienda) y devuelve su id
    private int registrarPostre(LineaPedido linea) throws DAOException {
        Producto postre = new Producto();
        postre.setIdCategoria(linea.getIdCategoria());
        postre.setClaveConfiguracion(ConfiguracionPostre.claveConfiguracion(linea.getOpciones()));
        postre.setMostrarEnTienda(false);
        postre.setOpciones(linea.getOpciones());
        return productoDAO.insertar(postre);
    }

    // ===================== Textos de la bitácora =====================

    /**
     * Arma los textos "antes" y "después" solo con las partes que cambiaron.
     * [0] = antes, [1] = después.
     */
    private String[] describirCambios(Pedido original, Pedido cambios) {
        String antes = "";
        String despues = "";

        String clienteAntes = textoCliente(original.getCliente());
        String clienteDespues = textoCliente(cambios.getCliente());
        if (!clienteAntes.equals(clienteDespues)) {
            antes = antes + "Cliente: " + clienteAntes + "\n";
            despues = despues + "Cliente: " + clienteDespues + "\n";
        }

        String entregaAntes = textoEntrega(original);
        String entregaDespues = textoEntrega(cambios);
        if (!entregaAntes.equals(entregaDespues)) {
            antes = antes + "Entrega: " + entregaAntes + "\n";
            despues = despues + "Entrega: " + entregaDespues + "\n";
        }

        String postresAntes = textoLineas(original.getLineas());
        String postresDespues = textoLineas(cambios.getLineas());
        if (!postresAntes.equals(postresDespues)) {
            antes = antes + "Postres:\n" + postresAntes;
            despues = despues + "Postres:\n" + postresDespues;
        }

        if (Math.abs(original.getMontoTotal() - cambios.getMontoTotal()) > 0.004) {
            antes = antes + "Total: " + Formatos.moneda(original.getMontoTotal()) + " · Anticipo: "
                    + Formatos.moneda(original.getAnticipoRequerido()) + "\n";
            despues = despues + "Total: " + Formatos.moneda(cambios.getMontoTotal()) + " · Anticipo: "
                    + Formatos.moneda(cambios.getAnticipoRequerido()) + "\n";
            double saldo = PedidoService.redondear(cambios.getMontoTotal() - original.getTotalPagado());
            if (saldo < 0) {
                despues = despues + "Saldo a favor del cliente (reembolso): " + Formatos.moneda(-saldo) + "\n";
            }
        }
        String[] textos = {antes.trim(), despues.trim()};
        return textos;
    }

    private String textoCliente(Cliente cliente) {
        String texto = texto(cliente.getNombre()) + ", tel. " + texto(cliente.getTelefono());
        if (!texto(cliente.getCorreo()).isEmpty()) {
            texto = texto + ", " + texto(cliente.getCorreo());
        }
        return texto;
    }

    private String textoEntrega(Pedido pedido) {
        String texto = Formatos.fecha(pedido.getFechaEntrega()) + " ";
        if (pedido.getHorarioEntrega() != null) {
            texto = texto + pedido.getHorarioEntrega().toString();
        }
        if (pedido.getModalidadEntrega() == ModalidadEntrega.DOMICILIO && pedido.getDireccion() != null) {
            texto = texto + ", envío a " + pedido.getDireccion().getTexto();
            if (!texto(pedido.getDireccion().getReferencias()).isEmpty()) {
                texto = texto + " (" + texto(pedido.getDireccion().getReferencias()) + ")";
            }
        } else if (pedido.getModalidadEntrega() != null) {
            texto = texto + ", " + pedido.getModalidadEntrega().toString().toLowerCase();
        }
        return texto;
    }

    // Una línea de texto por postre con sus datos (sirve para comparar y para la bitácora)
    private String textoLineas(List<LineaPedido> lineas) {
        String texto = "";
        for (LineaPedido linea : lineas) {
            texto = texto + "• " + linea.getCantidad() + " × " + linea.getNombreMostrar() + " ("
                    + Formatos.moneda(linea.getPrecioUnitario()) + " c/u)";
            if (!linea.getEspecificaciones().isEmpty()) {
                texto = texto + " [" + String.join("; ", linea.getEspecificaciones()) + "]";
            }
            if (!texto(linea.getDedicatoria()).isEmpty()) {
                texto = texto + " · Dedicatoria: " + texto(linea.getDedicatoria());
            }
            if (!texto(linea.getComentario()).isEmpty()) {
                texto = texto + " · Comentario: " + texto(linea.getComentario());
            }
            texto = texto + "\n";
        }
        return texto;
    }

    // Resumen corto para el mensaje al cliente
    private String resumenParaCliente(Pedido original, Pedido cambios) {
        List<String> partes = new ArrayList<String>();
        if (!textoCliente(original.getCliente()).equals(textoCliente(cambios.getCliente()))) {
            partes.add("datos de contacto");
        }
        if (!textoEntrega(original).equals(textoEntrega(cambios))) {
            partes.add("datos de entrega");
        }
        if (!textoLineas(original.getLineas()).equals(textoLineas(cambios.getLineas()))) {
            partes.add("postres");
        }
        if (partes.isEmpty()) {
            return null;
        }
        return "Se actualizaron: " + String.join(", ", partes) + ".";
    }

    // Avisos de pagos después del cambio de total
    // RF-34 · Excepciones paso 7: reembolso pendiente o anticipo sin cubrir con el nuevo total.
    private void agregarAvisosDePago(ResultadoPedido resultado, Pedido original, Pedido cambios) {
        double diferencia = PedidoService.redondear(cambios.getMontoTotal() - original.getMontoTotal());
        if (Math.abs(diferencia) < 0.005) {
            return;
        }
        String signo = "+";
        if (diferencia < 0) {
            signo = "−";
        }
        resultado.agregarAviso("Diferencia en el total: " + signo + Formatos.moneda(Math.abs(diferencia)));
        double pagado = original.getTotalPagado();
        if (pagado > 0) {
            double saldo = PedidoService.redondear(cambios.getMontoTotal() - pagado);
            if (saldo < 0) {
                resultado.agregarAviso("El cliente ya pagó más que el nuevo total: queda un reembolso de "
                        + Formatos.moneda(-saldo));
            } else {
                resultado.agregarAviso("Saldo pendiente con el nuevo total: " + Formatos.moneda(saldo));
            }
        }
        if (original.getEstado() == EstadoPedido.PAGADO && !cambios.anticipoCubierto()) {
            resultado.agregarAviso("Con el nuevo total el anticipo ya no está cubierto: faltan "
                    + Formatos.moneda(cambios.getFaltaParaIniciar()) + " para pasar a «En preparación»");
        }
    }

    // ===================== Apoyo =====================

    private boolean mismoCliente(Cliente uno, Cliente dos) {
        return textoCliente(uno).equals(textoCliente(dos));
    }

    private String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }
}
