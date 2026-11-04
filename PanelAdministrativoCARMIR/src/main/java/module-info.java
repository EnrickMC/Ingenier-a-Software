module com.pac {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    // Guarda si el panel quedó en modo claro u oscuro
    requires java.prefs;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.bootstrapicons;
    requires org.kordamp.bootstrapfx.core;
    // Iconos animados del menú lateral (Lottie4J)
    requires com.lottie4j.core;
    requires com.lottie4j.fxplayer;
    requires eu.hansolo.tilesfx;

    // Driver JDBC de MySQL (módulo automático)
    requires mysql.connector.j;

    // Notificaciones del Módulo 4: HttpClient (WhatsApp Cloud API) y Jakarta Mail (correo)
    requires java.net.http;
    requires jakarta.mail;

    // RF-50: vista previa del comprobante de pago (PDF con PDFBox, WEBP con el lector de TwelveMonkeys)
    requires java.desktop;
    requires org.apache.pdfbox;
    // PDFBox escribe sus avisos con commons-logging (sin esta línea no carga en tiempo de ejecución)
    requires org.apache.commons.logging;
    requires com.twelvemonkeys.imageio.webp;

    // ----- Paquetes generales del panel -----
    opens com.pac to javafx.fxml;
    opens com.pac.controller to javafx.fxml;
    // IconoAyuda se crea desde los archivos FXML
    opens com.pac.util to javafx.fxml;
    opens com.pac.view.animacion to javafx.fxml;

    exports com.pac;
    exports com.pac.controller;
    exports com.pac.dao;
    exports com.pac.dao.conexion;
    exports com.pac.session;
    exports com.pac.navigation;
    exports com.pac.view.menu;
    exports com.pac.view.animacion;
    exports com.pac.util;

    // ----- Módulo 7: Gestión de Usuarios -----
    // javafx.fxml necesita entrar a los controladores y javafx.base a FilaUsuario (columnas de la tabla)
    opens com.pac.modulos.usuarios.controller to javafx.fxml, javafx.base;

    exports com.pac.modulos.usuarios.model;
    exports com.pac.modulos.usuarios.dao;
    exports com.pac.modulos.usuarios.service;
    exports com.pac.modulos.usuarios.controller;

    // ----- Módulo 3: Catálogo de Personalización -----
    // javafx.fxml necesita entrar a los controladores y javafx.base a las clases Fila* (columnas de las tablas)
    opens com.pac.modulos.catalogo.controller to javafx.fxml, javafx.base;

    exports com.pac.modulos.catalogo.model;
    exports com.pac.modulos.catalogo.dao;
    exports com.pac.modulos.catalogo.dao.mysql;
    exports com.pac.modulos.catalogo.service;
    exports com.pac.modulos.catalogo.controller;

    // ----- Módulo 5: Inventario y Recetas -----
    // javafx.fxml necesita entrar a los controladores y javafx.base a las clases Fila* (columnas de las tablas)
    opens com.pac.modulos.inventario.controller to javafx.fxml, javafx.base;

    exports com.pac.modulos.inventario.model;
    exports com.pac.modulos.inventario.dao;
    exports com.pac.modulos.inventario.dao.mysql;
    exports com.pac.modulos.inventario.service;
    exports com.pac.modulos.inventario.controller;

    // ----- Módulo 4: Gestor de Pedidos y Producción -----
    // javafx.fxml necesita entrar a los controladores y javafx.base a las clases Fila* (columnas de las tablas)
    opens com.pac.modulos.pedidos.controller to javafx.fxml, javafx.base;

    exports com.pac.modulos.pedidos.model;
    exports com.pac.modulos.pedidos.dao;
    exports com.pac.modulos.pedidos.dao.mysql;
    exports com.pac.modulos.pedidos.service;
    exports com.pac.modulos.pedidos.notificacion;
    exports com.pac.modulos.pedidos.controller;

    // ----- Módulo 6: Reportes Financieros -----
    // javafx.fxml necesita entrar a los controladores y javafx.base a las clases Fila* (columnas de las tablas)
    opens com.pac.modulos.reportes.controller to javafx.fxml, javafx.base;

    exports com.pac.modulos.reportes.model;
    exports com.pac.modulos.reportes.dao;
    exports com.pac.modulos.reportes.dao.mysql;
    exports com.pac.modulos.reportes.service;
    exports com.pac.modulos.reportes.exportacion;
    exports com.pac.modulos.reportes.controller;

    // ----- Configuración del sistema -----
    opens com.pac.modulos.configuracion.controller to javafx.fxml, javafx.base;

    exports com.pac.modulos.configuracion.model;
    exports com.pac.modulos.configuracion.dao;
    exports com.pac.modulos.configuracion.dao.mysql;
    exports com.pac.modulos.configuracion.service;
    exports com.pac.modulos.configuracion.controller;
}
