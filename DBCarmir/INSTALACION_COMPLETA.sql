-- =====================================================================
-- CARMIR · INSTALACIÓN COMPLETA (un solo script)
-- Deja la base CARMIR_DB LIMPIA (sin datos de prueba) lista para el Panel, la API y la página web.
-- Solo trae los 13 parámetros de fábrica de Configuracion (los necesita el sistema).
-- La Administradora no está en la base: entra con admin / admin (fijo en el Panel).
-- Para llenarla con datos de prueba, ejecuta después semillas/POBLAR_BASE.sql.
--
-- CUIDADO: BORRA la base CARMIR_DB si ya existe y la crea de nuevo
-- (lo hace CARMIR_BaseDeDatos.sql). Úsalo solo en tu computadora de pruebas.
--
-- Cómo usarlo en MySQL Workbench (conectado como root):
--   File > Open SQL Script > este archivo, y luego el rayo (Execute All).
--
-- Este archivo junta, en este orden y sin cambiarlos:
--   1. CARMIR_BaseDeDatos.sql               (tablas)
--   2. migraciones/MIGRACION_COMPLETA.sql   (cambios de los módulos, Partes 1 a 7)
-- Si cambias alguno de esos archivos, vuelve a generar este.
-- No crea usuarios de MySQL: en local la API usa root (ver ApiCARMIR/.env).
-- =====================================================================
SET NAMES utf8mb4;



-- #####################################################################
-- ##### INICIO: CARMIR_BaseDeDatos.sql
-- #####################################################################
-- =====================================================================
-- CARMIR – Base de datos del sistema web de Delicias de Postres
-- Modelo relacional en tercera forma normal (3FN): 25 tablas, 31 relaciones
-- Compatible con MySQL 8.0.16 o superior (CHECK) y MariaDB 10.4 o superior
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Base de datos
-- ---------------------------------------------------------------------
DROP DATABASE IF EXISTS CARMIR_DB;
CREATE DATABASE CARMIR_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE CARMIR_DB;

-- ---------------------------------------------------------------------
-- 2. Tablas (en orden de dependencia: primero las que no tienen FK)
-- ---------------------------------------------------------------------

-- Cliente: Persona que hace pedidos desde la tienda en línea.
CREATE TABLE Cliente (
  IdCliente INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del cliente',
  NombreCliente VARCHAR(100) NOT NULL COMMENT 'Nombre completo para identificar el pedido',
  TelefonoWhatsApp VARCHAR(10) NOT NULL COMMENT 'Número a 10 dígitos al que llegan las notificaciones',
  CorreoCliente VARCHAR(100) NULL COMMENT 'Correo de contacto',
  CONSTRAINT pk_cliente PRIMARY KEY (IdCliente),
  CONSTRAINT uq_cliente_telefonowhatsapp UNIQUE (TelefonoWhatsApp),
  CONSTRAINT chk_cliente_telefono CHECK (TelefonoWhatsApp REGEXP '^[0-9]{10}$')
) ENGINE=InnoDB COMMENT='Persona que hace pedidos desde la tienda en línea.';

-- Direccion: Direcciones de entrega que registra un cliente.
CREATE TABLE Direccion (
  IdDireccion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la dirección',
  IdCliente INT NOT NULL COMMENT 'Cliente dueño de la dirección (relación Registra)',
  Calle VARCHAR(100) NOT NULL COMMENT 'Nombre de la calle',
  NumeroExterior VARCHAR(10) NOT NULL COMMENT 'Número exterior',
  NumeroInterior VARCHAR(10) NULL COMMENT 'Número interior, si existe',
  Colonia VARCHAR(80) NOT NULL COMMENT 'Colonia o fraccionamiento',
  CodigoPostal CHAR(5) NOT NULL COMMENT 'Código postal a 5 dígitos',
  Referencias VARCHAR(255) NULL COMMENT 'Indicaciones para encontrar el domicilio',
  CONSTRAINT pk_direccion PRIMARY KEY (IdDireccion),
  CONSTRAINT fk_direccion_idcliente FOREIGN KEY (IdCliente) REFERENCES Cliente (IdCliente) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_direccion_cp CHECK (CodigoPostal REGEXP '^[0-9]{5}$')
) ENGINE=InnoDB COMMENT='Direcciones de entrega que registra un cliente.';

-- Pedido: Solicitud del cliente; controla fechas, montos y el estado del proceso.
CREATE TABLE Pedido (
  Folio INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del pedido que ve el cliente',
  IdCliente INT NOT NULL COMMENT 'Cliente que hizo el pedido (relación Realiza)',
  FechaSolicitud DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora en que se creó el pedido',
  FechaEntrega DATE NOT NULL COMMENT 'Día en que se entrega',
  HorarioEntrega TIME NOT NULL COMMENT 'Hora acordada de entrega',
  ModalidadEntrega ENUM('Recoger en tienda','Envio a domicilio') NOT NULL COMMENT 'Si se recoge en tienda o se envía a domicilio',
  ModalidadPago ENUM('Anticipo','Pago total') NOT NULL COMMENT 'Si se paga con anticipo o el total al inicio',
  MontoTotal DECIMAL(10,2) NOT NULL COMMENT 'Suma de Cantidad × PrecioUnitario de sus postres',
  AnticipoRequerido DECIMAL(10,2) NOT NULL COMMENT 'Monto mínimo a pagar para iniciar el pedido',
  EstadoPedido ENUM('En proceso de aceptacion','Aprobado','Pagado','En preparacion','Pedido terminado','Entregado') NOT NULL DEFAULT 'En proceso de aceptacion' COMMENT 'Estado actual del pedido',
  CONSTRAINT pk_pedido PRIMARY KEY (Folio),
  CONSTRAINT fk_pedido_idcliente FOREIGN KEY (IdCliente) REFERENCES Cliente (IdCliente) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_pedido_montos CHECK (MontoTotal >= 0 AND AnticipoRequerido >= 0 AND AnticipoRequerido <= MontoTotal)
) ENGINE=InnoDB COMMENT='Solicitud del cliente; controla fechas, montos y el estado del proceso.';

-- EntregaDomicilio: Dirección a la que se entrega un pedido; solo existe para pedidos a domicilio.
CREATE TABLE EntregaDomicilio (
  Folio INT NOT NULL COMMENT 'Pedido que se entrega a domicilio (relación Se entrega)',
  IdDireccion INT NOT NULL COMMENT 'Dirección a la que se entrega el pedido (relación Se entrega en)',
  CONSTRAINT pk_entregadomicilio PRIMARY KEY (Folio),
  CONSTRAINT fk_entregadomicilio_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_entregadomicilio_iddireccion FOREIGN KEY (IdDireccion) REFERENCES Direccion (IdDireccion) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Dirección a la que se entrega un pedido; solo existe para pedidos a domicilio.';

-- CategoriaPostre: Tipo de postre que se vende: Pastel, Gelatina, Galletas.
CREATE TABLE CategoriaPostre (
  IdCategoria INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la categoría',
  NombreCategoria VARCHAR(50) NOT NULL COMMENT 'Nombre que se muestra al cliente',
  DescripcionCategoria VARCHAR(255) NULL COMMENT 'Explicación breve de la categoría',
  EstadoCategoria ENUM('Activa','Inactiva') NOT NULL DEFAULT 'Activa' COMMENT 'Si la categoría está en uso (baja lógica)',
  CONSTRAINT pk_categoriapostre PRIMARY KEY (IdCategoria),
  CONSTRAINT uq_categoriapostre_nombrecategoria UNIQUE (NombreCategoria)
) ENGINE=InnoDB COMMENT='Tipo de postre que se vende: Pastel, Gelatina, Galletas.';

-- Postre: Configuración única de un postre; la usan los pedidos y el catálogo.
CREATE TABLE Postre (
  IdPostre INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del postre',
  IdCategoria INT NOT NULL COMMENT 'Categoría del postre (relación Clasifica)',
  ClaveConfiguracion VARCHAR(255) NOT NULL COMMENT 'Texto con las opciones elegidas en orden (IdOpcion × cantidad); evita duplicar postres iguales',
  NombrePostre VARCHAR(100) NULL COMMENT 'Nombre comercial; solo en postres del catálogo',
  DescripcionPostre TEXT NULL COMMENT 'Descripción para el catálogo',
  LinkFotografia VARCHAR(255) NULL COMMENT 'Enlace a la foto del postre',
  MostrarEnTienda TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Si aparece en el catálogo público',
  CONSTRAINT pk_postre PRIMARY KEY (IdPostre),
  CONSTRAINT uq_postre_claveconfiguracion UNIQUE (ClaveConfiguracion),
  CONSTRAINT fk_postre_idcategoria FOREIGN KEY (IdCategoria) REFERENCES CategoriaPostre (IdCategoria) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Configuración única de un postre; la usan los pedidos y el catálogo.';

-- DetallePedido: Postres incluidos en cada pedido, con los datos propios de ese pedido.
CREATE TABLE DetallePedido (
  Folio INT NOT NULL COMMENT 'Pedido al que pertenece la línea (relación Contiene)',
  IdPostre INT NOT NULL COMMENT 'Postre pedido',
  Cantidad INT NOT NULL DEFAULT 1 COMMENT 'Piezas de ese postre',
  Dedicatoria VARCHAR(150) NULL COMMENT 'Texto que va escrito en el postre',
  Comentario VARCHAR(255) NULL COMMENT 'Indicaciones especiales del cliente',
  PrecioUnitario DECIMAL(10,2) NOT NULL COMMENT 'PrecioVenta del postre al momento del pedido',
  CostoUnitario DECIMAL(10,2) NOT NULL COMMENT 'CostoIngredientes del postre al momento del pedido',
  CONSTRAINT pk_detallepedido PRIMARY KEY (Folio, IdPostre),
  CONSTRAINT fk_detallepedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_detallepedido_idpostre FOREIGN KEY (IdPostre) REFERENCES Postre (IdPostre) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_detalle_cantidad CHECK (Cantidad > 0),
  CONSTRAINT chk_detalle_importes CHECK (PrecioUnitario >= 0 AND CostoUnitario >= 0)
) ENGINE=InnoDB COMMENT='Postres incluidos en cada pedido, con los datos propios de ese pedido.';

-- Usuario: Persona del negocio que entra al sistema (Administradora o Personal). La sesión se maneja en el programa y no se guarda.
CREATE TABLE Usuario (
  IdUsuario INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del usuario',
  NombreCompleto VARCHAR(100) NOT NULL COMMENT 'Nombre de la persona',
  NombreUsuario VARCHAR(30) NOT NULL COMMENT 'Nombre con el que inicia sesión',
  Contraseña VARCHAR(255) NOT NULL COMMENT 'Contraseña cifrada (hash), nunca en texto plano',
  EstadoUsuario ENUM('Activo','Inactivo','Bloqueado') NOT NULL DEFAULT 'Activo' COMMENT 'Si puede entrar al sistema',
  UltimoAcceso DATETIME NULL COMMENT 'Fecha y hora de su último inicio de sesión',
  IntentosFallidos TINYINT NOT NULL DEFAULT 0 COMMENT 'Intentos fallidos seguidos; bloquea al llegar al límite',
  CONSTRAINT pk_usuario PRIMARY KEY (IdUsuario),
  CONSTRAINT uq_usuario_nombreusuario UNIQUE (NombreUsuario),
  CONSTRAINT chk_usuario_intentos CHECK (IntentosFallidos >= 0)
) ENGINE=InnoDB COMMENT='Persona del negocio que entra al sistema (Administradora o Personal). La sesión se maneja en el programa y no se guarda.';

-- Pago: Cada pago o comprobante que envía el cliente.
CREATE TABLE Pago (
  IdPago INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del pago',
  Folio INT NOT NULL COMMENT 'Pedido que se paga (relación Recibe)',
  IdUsuario INT NULL COMMENT 'Usuario que validó o rechazó el pago (relación Valida)',
  MontoPago DECIMAL(10,2) NOT NULL COMMENT 'Cantidad pagada',
  FechaPago DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora del pago',
  MetodoPago ENUM('Transferencia','Efectivo') NOT NULL COMMENT 'Forma de pago',
  Referencia VARCHAR(50) NULL COMMENT 'Número de referencia o folio bancario',
  LinkComprobante VARCHAR(255) NULL COMMENT 'Enlace a la imagen del comprobante',
  EstadoValidacion ENUM('Pendiente','Validado','Rechazado') NOT NULL DEFAULT 'Pendiente' COMMENT 'Resultado de la revisión',
  MotivoRechazo VARCHAR(255) NULL COMMENT 'Por qué se rechazó el pago',
  CONSTRAINT pk_pago PRIMARY KEY (IdPago),
  CONSTRAINT fk_pago_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_pago_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_pago_monto CHECK (MontoPago > 0)
) ENGINE=InnoDB COMMENT='Cada pago o comprobante que envía el cliente.';

-- Cancelacion: Registro de un pedido cancelado; el pedido conserva su último estado.
CREATE TABLE Cancelacion (
  IdCancelacion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la cancelación',
  Folio INT NOT NULL COMMENT 'Pedido cancelado (relación Puede tener)',
  MotivoCancelacion VARCHAR(255) NOT NULL COMMENT 'Motivo escrito en texto libre',
  FechaHoraCancelacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la cancelación',
  MontoReembolso DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Dinero a devolver, a partir de los pagos validados',
  EstadoReembolso ENUM('No aplica','Pendiente','Realizado') NOT NULL DEFAULT 'No aplica' COMMENT 'Situación de la devolución',
  CONSTRAINT pk_cancelacion PRIMARY KEY (IdCancelacion),
  CONSTRAINT uq_cancelacion_folio UNIQUE (Folio),
  CONSTRAINT fk_cancelacion_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_cancelacion_reembolso CHECK (MontoReembolso >= 0)
) ENGINE=InnoDB COMMENT='Registro de un pedido cancelado; el pedido conserva su último estado.';

-- HistorialEstado: Bitácora de cada cambio de estado de un pedido.
CREATE TABLE HistorialEstado (
  IdHistorial INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del cambio',
  Folio INT NOT NULL COMMENT 'Pedido que cambió (relación Registra)',
  IdUsuario INT NOT NULL COMMENT 'Usuario que hizo el cambio (relación Realiza)',
  EstadoAnterior VARCHAR(30) NULL COMMENT 'Estado antes del cambio; vacío en el primer registro',
  EstadoNuevo VARCHAR(30) NOT NULL COMMENT 'Estado después del cambio',
  FechaHoraCambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento del cambio',
  CONSTRAINT pk_historialestado PRIMARY KEY (IdHistorial),
  CONSTRAINT fk_historialestado_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_historialestado_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Bitácora de cada cambio de estado de un pedido.';

-- Notificacion: Mensajes enviados al cliente por eventos del pedido.
CREATE TABLE Notificacion (
  IdNotificacion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del envío',
  Folio INT NOT NULL COMMENT 'Pedido que generó el aviso (relación Genera)',
  TipoEvento VARCHAR(50) NOT NULL COMMENT 'Evento que disparó el aviso, por ejemplo "Pago validado"',
  Canal ENUM('WhatsApp','Correo') NOT NULL COMMENT 'Medio de envío',
  Destino VARCHAR(100) NOT NULL COMMENT 'Número o correo al que se envió',
  Mensaje TEXT NOT NULL COMMENT 'Texto enviado',
  FechaHoraEnvio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento del envío',
  ResultadoEnvio ENUM('Enviado','Fallido') NOT NULL COMMENT 'Si el mensaje se entregó',
  CONSTRAINT pk_notificacion PRIMARY KEY (IdNotificacion),
  CONSTRAINT fk_notificacion_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Mensajes enviados al cliente por eventos del pedido.';

-- Caracteristica: Característica que se puede personalizar dentro de una categoría (Tipo, Sabor, Pisos, Decoración) y sus reglas de selección.
CREATE TABLE Caracteristica (
  IdCaracteristica INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la característica',
  IdCategoria INT NOT NULL COMMENT 'Categoría a la que pertenece (relación Incluye)',
  NombreCaracteristica VARCHAR(50) NOT NULL COMMENT 'Nombre que ve el cliente, por ejemplo "Decoración"',
  DescripcionCaracteristica VARCHAR(255) NULL COMMENT 'Explicación para el cliente o el personal',
  Obligatoria TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Si el cliente debe elegir al menos una opción',
  SeleccionMultiple TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Falso: solo se elige una opción (Sabor). Verdadero: varias opciones, limitadas por el presupuesto',
  PresupuestoSeleccion DECIMAL(6,2) NOT NULL DEFAULT 1.00 COMMENT 'Puntos disponibles para elegir opciones; solo aplica si SeleccionMultiple es verdadero',
  OrdenMostrar TINYINT NOT NULL DEFAULT 1 COMMENT 'Orden en que aparece al armar el postre',
  Disponible TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Si se muestra al armar postres de esa categoría',
  CONSTRAINT pk_caracteristica PRIMARY KEY (IdCaracteristica),
  CONSTRAINT uq_caracteristica_idcategoria_nombrecaracteristica UNIQUE (IdCategoria, NombreCaracteristica),
  CONSTRAINT fk_caracteristica_idcategoria FOREIGN KEY (IdCategoria) REFERENCES CategoriaPostre (IdCategoria) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_caracteristica_presupuesto CHECK (PresupuestoSeleccion > 0)
) ENGINE=InnoDB COMMENT='Característica que se puede personalizar dentro de una categoría (Tipo, Sabor, Pisos, Decoración) y sus reglas de selección.';

-- OpcionCaracteristica: Cada valor que el cliente puede elegir dentro de una característica de una categoría.
CREATE TABLE OpcionCaracteristica (
  IdOpcion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la opción',
  IdCaracteristica INT NOT NULL COMMENT 'Característica a la que pertenece (relación Ofrece)',
  NombreOpcion VARCHAR(60) NOT NULL COMMENT 'Valor que ve el cliente, por ejemplo "Tres leches"',
  DescripcionOpcion VARCHAR(255) NULL COMMENT 'Detalle de la opción',
  LinkImagen VARCHAR(255) NULL COMMENT 'Enlace a la foto de la opción',
  MultiplicadorIngredientes DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT 'Factor que escala las opciones con SeEscala; por ejemplo 2 para 2 pisos',
  MultiplicadorCosto DECIMAL(4,2) NOT NULL DEFAULT 3.00 COMMENT 'Factor que convierte el costo de la opción en precio de venta',
  SeEscala TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Si su costo se multiplica por el MultiplicadorIngredientes del tamaño o pisos',
  Disponible TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Si el cliente la puede elegir',
  PuntosSeleccion DECIMAL(6,2) NOT NULL DEFAULT 1.00 COMMENT 'Puntos que consume del presupuesto de su característica por cada vez que se elige',
  MultiplicadorPresupuesto DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT 'Factor que amplía el presupuesto de las características múltiples de la categoría; por ejemplo 2 en tamaño para 200 personas',
  CONSTRAINT pk_opcioncaracteristica PRIMARY KEY (IdOpcion),
  CONSTRAINT fk_opcioncaracteristica_idcaracteristica FOREIGN KEY (IdCaracteristica) REFERENCES Caracteristica (IdCaracteristica) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_opcion_multiplicadores CHECK (MultiplicadorIngredientes > 0 AND MultiplicadorCosto > 0 AND MultiplicadorPresupuesto > 0),
  CONSTRAINT chk_opcion_puntos CHECK (PuntosSeleccion > 0)
) ENGINE=InnoDB COMMENT='Cada valor que el cliente puede elegir dentro de una característica de una categoría.';

-- PostreOpcion: Opciones que forman cada postre.
CREATE TABLE PostreOpcion (
  IdPostre INT NOT NULL COMMENT 'Postre configurado',
  IdOpcion INT NOT NULL COMMENT 'Opción elegida',
  CantidadElegida TINYINT NOT NULL DEFAULT 1 COMMENT 'Veces que se eligió la opción; 1 si la característica no es de selección múltiple. La suma de PuntosSeleccion × CantidadElegida no debe pasar del PresupuestoSeleccion × los MultiplicadorPresupuesto elegidos',
  CONSTRAINT pk_postreopcion PRIMARY KEY (IdPostre, IdOpcion),
  CONSTRAINT fk_postreopcion_idpostre FOREIGN KEY (IdPostre) REFERENCES Postre (IdPostre) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_postreopcion_idopcion FOREIGN KEY (IdOpcion) REFERENCES OpcionCaracteristica (IdOpcion) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_postreopcion_cantidad CHECK (CantidadElegida > 0)
) ENGINE=InnoDB COMMENT='Opciones que forman cada postre.';

-- ExclusionOpciones: Pares de opciones que no pueden elegirse juntas, por ejemplo tres leches con frambuesa.
CREATE TABLE ExclusionOpciones (
  IdExclusion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la exclusión',
  IdOpcion INT NOT NULL COMMENT 'Primera opción del par (relación Excluye)',
  IdOpcionExcluida INT NOT NULL COMMENT 'Segunda opción del par (relación Es excluida)',
  MotivoExclusion VARCHAR(255) NULL COMMENT 'Por qué no se pueden combinar',
  EstadoExclusion ENUM('Activa','Inactiva') NOT NULL DEFAULT 'Activa' COMMENT 'Si la regla está vigente',
  CONSTRAINT pk_exclusionopciones PRIMARY KEY (IdExclusion),
  CONSTRAINT uq_exclusionopciones_idopcion_idopcionexcluida UNIQUE (IdOpcion, IdOpcionExcluida),
  CONSTRAINT fk_exclusionopciones_idopcion FOREIGN KEY (IdOpcion) REFERENCES OpcionCaracteristica (IdOpcion),
  CONSTRAINT fk_exclusionopciones_idopcionexcluida FOREIGN KEY (IdOpcionExcluida) REFERENCES OpcionCaracteristica (IdOpcion),
  CONSTRAINT chk_exclusion_distintas CHECK (IdOpcion <> IdOpcionExcluida)
) ENGINE=InnoDB COMMENT='Pares de opciones que no pueden elegirse juntas, por ejemplo tres leches con frambuesa.';

-- Receta: Receta de una opción; sus ingredientes están en la tabla intermedia RecetaInsumo.
CREATE TABLE Receta (
  IdReceta INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la receta',
  IdOpcion INT NOT NULL COMMENT 'Opción que se prepara con esta receta',
  NombreReceta VARCHAR(100) NOT NULL COMMENT 'Nombre de la receta, por ejemplo "Bizcocho tres leches"',
  PasosPreparacion TEXT NOT NULL COMMENT 'Instrucciones de elaboración',
  TiempoElaboracion INT NOT NULL COMMENT 'Minutos que tarda en prepararse',
  EstadoReceta ENUM('Activa','Inactiva') NOT NULL DEFAULT 'Activa' COMMENT 'Si la receta está en uso (baja lógica)',
  CONSTRAINT pk_receta PRIMARY KEY (IdReceta),
  CONSTRAINT uq_receta_idopcion UNIQUE (IdOpcion),
  CONSTRAINT fk_receta_idopcion FOREIGN KEY (IdOpcion) REFERENCES OpcionCaracteristica (IdOpcion) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_receta_tiempo CHECK (TiempoElaboracion > 0)
) ENGINE=InnoDB COMMENT='Receta de una opción; sus ingredientes están en la tabla intermedia RecetaInsumo.';

-- Insumo: Materia prima que se compra y se descuenta al preparar pedidos.
CREATE TABLE Insumo (
  IdInsumo INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del insumo',
  NombreInsumo VARCHAR(80) NOT NULL COMMENT 'Nombre del insumo, por ejemplo "Harina de trigo"',
  CategoriaInsumo VARCHAR(50) NOT NULL COMMENT 'Grupo del insumo, por ejemplo "Lácteos"',
  UnidadMedida ENUM('kg','g','L','ml','pieza') NOT NULL COMMENT 'Unidad en que se mide',
  CostoUnitario DECIMAL(10,2) NOT NULL COMMENT 'Costo por unidad de medida',
  StockMinimo DECIMAL(10,3) NOT NULL COMMENT 'Cantidad por debajo de la cual se genera alerta',
  Perecedero TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Si caduca; activa el control de caducidad por lote',
  EstadoInsumo ENUM('Activo','Inactivo') NOT NULL DEFAULT 'Activo' COMMENT 'Si el insumo está en uso (baja lógica), no si hay existencia',
  CONSTRAINT pk_insumo PRIMARY KEY (IdInsumo),
  CONSTRAINT uq_insumo_nombreinsumo UNIQUE (NombreInsumo),
  CONSTRAINT chk_insumo_valores CHECK (CostoUnitario >= 0 AND StockMinimo >= 0)
) ENGINE=InnoDB COMMENT='Materia prima que se compra y se descuenta al preparar pedidos.';

-- RecetaInsumo: Ingredientes de cada receta.
CREATE TABLE RecetaInsumo (
  IdReceta INT NOT NULL COMMENT 'Receta',
  IdInsumo INT NOT NULL COMMENT 'Insumo que lleva',
  CantidadInsumo DECIMAL(10,3) NOT NULL COMMENT 'Cantidad del insumo, en la UnidadMedida del insumo',
  CONSTRAINT pk_recetainsumo PRIMARY KEY (IdReceta, IdInsumo),
  CONSTRAINT fk_recetainsumo_idreceta FOREIGN KEY (IdReceta) REFERENCES Receta (IdReceta) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_recetainsumo_idinsumo FOREIGN KEY (IdInsumo) REFERENCES Insumo (IdInsumo) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_recetainsumo_cantidad CHECK (CantidadInsumo > 0)
) ENGINE=InnoDB COMMENT='Ingredientes de cada receta.';

-- Lote: Cada compra de un insumo, con su propia existencia y caducidad.
CREATE TABLE Lote (
  IdLote INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del lote',
  IdInsumo INT NOT NULL COMMENT 'Insumo comprado (relación Tiene)',
  CantidadComprada DECIMAL(10,3) NOT NULL COMMENT 'Cantidad que entró en la compra',
  ExistenciaLote DECIMAL(10,3) NOT NULL COMMENT 'Lo que queda de esa compra; baja al descontar',
  CostoCompra DECIMAL(10,2) NOT NULL COMMENT 'Total pagado por el lote',
  FechaCompra DATE NOT NULL COMMENT 'Fecha de la compra',
  Proveedor VARCHAR(100) NULL COMMENT 'Proveedor de la compra',
  FechaCaducidad DATE NULL COMMENT 'Fecha en que caduca; vacía si no es perecedero',
  CONSTRAINT pk_lote PRIMARY KEY (IdLote),
  CONSTRAINT fk_lote_idinsumo FOREIGN KEY (IdInsumo) REFERENCES Insumo (IdInsumo) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_lote_cantidades CHECK (CantidadComprada > 0 AND ExistenciaLote >= 0 AND ExistenciaLote <= CantidadComprada),
  CONSTRAINT chk_lote_costo CHECK (CostoCompra >= 0),
  CONSTRAINT chk_lote_fechas CHECK (FechaCaducidad IS NULL OR FechaCaducidad >= FechaCompra)
) ENGINE=InnoDB COMMENT='Cada compra de un insumo, con su propia existencia y caducidad.';

-- MovimientoInventario: Cada entrada o salida de un insumo.
CREATE TABLE MovimientoInventario (
  IdMovimiento INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del movimiento',
  IdInsumo INT NOT NULL COMMENT 'Insumo afectado (relación Afecta)',
  IdUsuario INT NOT NULL COMMENT 'Usuario que lo registró (relación Captura)',
  Folio INT NULL COMMENT 'Pedido que lo provocó; vacío en compras, mermas y ajustes (relación Provoca)',
  TipoMovimiento ENUM('Compra','Produccion','Merma','Ajuste') NOT NULL COMMENT 'Clase de movimiento',
  CantidadMovimiento DECIMAL(10,3) NOT NULL COMMENT 'Cantidad que entra (+) o sale (−)',
  Motivo VARCHAR(255) NULL COMMENT 'Explicación, sobre todo en mermas y ajustes',
  FechaMovimiento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento del movimiento',
  CostoMovimiento DECIMAL(10,2) NOT NULL COMMENT 'Valor en dinero del movimiento',
  CONSTRAINT pk_movimientoinventario PRIMARY KEY (IdMovimiento),
  CONSTRAINT fk_movimientoinventario_idinsumo FOREIGN KEY (IdInsumo) REFERENCES Insumo (IdInsumo) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_movimientoinventario_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_movimientoinventario_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_movimiento_cantidad CHECK (CantidadMovimiento <> 0)
) ENGINE=InnoDB COMMENT='Cada entrada o salida de un insumo.';

-- AlertaInventario: Aviso de existencia baja o de caducidad próxima de un insumo.
CREATE TABLE AlertaInventario (
  IdAlerta INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la alerta',
  IdInsumo INT NOT NULL COMMENT 'Insumo que generó la alerta (relación Genera)',
  TipoAlerta ENUM('Existencia baja','Proxima caducidad','Caducado') NOT NULL COMMENT 'Motivo de la alerta',
  FechaAlerta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento en que se generó',
  ExistenciaEnAlerta DECIMAL(10,3) NOT NULL COMMENT 'ExistenciaTotal del insumo al generarse la alerta',
  EstadoAlerta ENUM('Pendiente','Atendida') NOT NULL DEFAULT 'Pendiente' COMMENT 'Si ya se atendió',
  CONSTRAINT pk_alertainventario PRIMARY KEY (IdAlerta),
  CONSTRAINT fk_alertainventario_idinsumo FOREIGN KEY (IdInsumo) REFERENCES Insumo (IdInsumo) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Aviso de existencia baja o de caducidad próxima de un insumo.';

-- AlertaCaducidad: Datos extra de una alerta de caducidad: el lote que está por caducar. Los días para caducar se calculan con Lote.FechaCaducidad − FechaAlerta.
CREATE TABLE AlertaCaducidad (
  IdAlerta INT NOT NULL COMMENT 'Alerta de inventario de tipo caducidad que se detalla',
  IdLote INT NOT NULL COMMENT 'Lote que está por caducar (relación Advierte)',
  CONSTRAINT pk_alertacaducidad PRIMARY KEY (IdAlerta),
  CONSTRAINT fk_alertacaducidad_idalerta FOREIGN KEY (IdAlerta) REFERENCES AlertaInventario (IdAlerta) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_alertacaducidad_idlote FOREIGN KEY (IdLote) REFERENCES Lote (IdLote) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Datos extra de una alerta de caducidad: el lote que está por caducar. Los días para caducar se calculan con Lote.FechaCaducidad − FechaAlerta.';

-- Permiso: Cada acción del sistema que se le puede asignar a un usuario.
CREATE TABLE Permiso (
  IdPermiso INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del permiso',
  NombrePermiso VARCHAR(50) NOT NULL COMMENT 'Acción permitida, por ejemplo "Validar pagos"',
  CONSTRAINT pk_permiso PRIMARY KEY (IdPermiso),
  CONSTRAINT uq_permiso_nombrepermiso UNIQUE (NombrePermiso)
) ENGINE=InnoDB COMMENT='Cada acción del sistema que se le puede asignar a un usuario.';

-- UsuarioPermiso: Permisos asignados a cada usuario.
CREATE TABLE UsuarioPermiso (
  IdUsuario INT NOT NULL COMMENT 'Usuario',
  IdPermiso INT NOT NULL COMMENT 'Permiso asignado',
  CONSTRAINT pk_usuariopermiso PRIMARY KEY (IdUsuario, IdPermiso),
  CONSTRAINT fk_usuariopermiso_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_usuariopermiso_idpermiso FOREIGN KEY (IdPermiso) REFERENCES Permiso (IdPermiso) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Permisos asignados a cada usuario.';

-- ---------------------------------------------------------------------
-- 3. Índices para las consultas más frecuentes
--    (las FK ya crean su propio índice en InnoDB)
-- ---------------------------------------------------------------------
CREATE INDEX idx_pedido_estado_entrega ON Pedido (EstadoPedido, FechaEntrega);
CREATE INDEX idx_pago_estado ON Pago (EstadoValidacion);
CREATE INDEX idx_lote_caducidad ON Lote (FechaCaducidad);
CREATE INDEX idx_movimiento_fecha ON MovimientoInventario (FechaMovimiento);
CREATE INDEX idx_alerta_estado ON AlertaInventario (EstadoAlerta, TipoAlerta);

-- ---------------------------------------------------------------------
-- 4. Vistas para los atributos derivados
--    En 3FN no se guardan; se calculan con estas vistas cuando se necesitan.
-- ---------------------------------------------------------------------

-- Existencia total de cada insumo = suma de la existencia de sus lotes
CREATE VIEW vw_ExistenciaInsumo AS
SELECT i.IdInsumo, i.NombreInsumo, i.UnidadMedida, i.StockMinimo,
       COALESCE(SUM(l.ExistenciaLote), 0) AS ExistenciaTotal,
       COALESCE(SUM(l.ExistenciaLote), 0) < i.StockMinimo AS BajoMinimo
FROM Insumo i
LEFT JOIN Lote l ON l.IdInsumo = i.IdInsumo
GROUP BY i.IdInsumo, i.NombreInsumo, i.UnidadMedida, i.StockMinimo;

-- Costo de cada receta = suma de CantidadInsumo × CostoUnitario
CREATE VIEW vw_CostoReceta AS
SELECT r.IdReceta, r.IdOpcion, r.NombreReceta,
       COALESCE(SUM(ri.CantidadInsumo * i.CostoUnitario), 0) AS CostoReceta
FROM Receta r
LEFT JOIN RecetaInsumo ri ON ri.IdReceta = r.IdReceta
LEFT JOIN Insumo i ON i.IdInsumo = ri.IdInsumo
GROUP BY r.IdReceta, r.IdOpcion, r.NombreReceta;

-- Costo y precio actual de cada postre.
-- Factor de escala = producto de MultiplicadorIngredientes de las opciones elegidas
-- (por ejemplo tamaño o pisos); solo se aplica a las opciones con SeEscala = 1.
-- Costo de la opción = CostoReceta × CantidadElegida × (factor si SeEscala)
-- Precio de la opción = costo de la opción × MultiplicadorCosto
CREATE VIEW vw_CostoPrecioPostre AS
SELECT po.IdPostre,
       ROUND(SUM(COALESCE(cr.CostoReceta, 0) * po.CantidadElegida
                 * IF(o.SeEscala = 1, COALESCE(f.Factor, 1), 1)), 2) AS CostoIngredientes,
       ROUND(SUM(COALESCE(cr.CostoReceta, 0) * po.CantidadElegida
                 * IF(o.SeEscala = 1, COALESCE(f.Factor, 1), 1) * o.MultiplicadorCosto), 2) AS PrecioVenta
FROM PostreOpcion po
JOIN OpcionCaracteristica o ON o.IdOpcion = po.IdOpcion
LEFT JOIN vw_CostoReceta cr ON cr.IdOpcion = po.IdOpcion
LEFT JOIN (SELECT po2.IdPostre, EXP(SUM(LN(o2.MultiplicadorIngredientes))) AS Factor
      FROM PostreOpcion po2
      JOIN OpcionCaracteristica o2 ON o2.IdOpcion = po2.IdOpcion
      WHERE o2.SeEscala = 0
      GROUP BY po2.IdPostre) f ON f.IdPostre = po.IdPostre
GROUP BY po.IdPostre;

-- Saldo pendiente de cada pedido = MontoTotal − pagos validados
CREATE VIEW vw_SaldoPedido AS
SELECT p.Folio, p.MontoTotal, p.AnticipoRequerido,
       COALESCE(SUM(CASE WHEN pg.EstadoValidacion = 'Validado' THEN pg.MontoPago END), 0) AS TotalPagado,
       p.MontoTotal - COALESCE(SUM(CASE WHEN pg.EstadoValidacion = 'Validado' THEN pg.MontoPago END), 0) AS SaldoPendiente
FROM Pedido p
LEFT JOIN Pago pg ON pg.Folio = p.Folio
GROUP BY p.Folio, p.MontoTotal, p.AnticipoRequerido;

-- Días que le faltan a cada lote de una alerta de caducidad
CREATE VIEW vw_AlertaCaducidad AS
SELECT a.IdAlerta, a.IdInsumo, ac.IdLote, a.TipoAlerta, a.FechaAlerta, a.EstadoAlerta,
       l.FechaCaducidad, DATEDIFF(l.FechaCaducidad, DATE(a.FechaAlerta)) AS DiasParaCaducar
FROM AlertaInventario a
JOIN AlertaCaducidad ac ON ac.IdAlerta = a.IdAlerta
JOIN Lote l ON l.IdLote = ac.IdLote;

-- Por si el archivo anterior dejó otro delimitador
DELIMITER ;



-- #####################################################################
-- ##### INICIO: migraciones/MIGRACION_COMPLETA.sql
-- #####################################################################
-- =====================================================================
-- CARMIR · MIGRACIÓN COMPLETA (un solo script)
--
-- Reúne, en orden, TODOS los cambios que el Panel Administrativo necesita:
--   Parte 1 = PROPUESTA_V2__catalogo.sql     (Módulo 3 · Catálogo)
--   Parte 2 = PROPUESTA_V3__inventario.sql   (Módulo 5 · Inventario y Recetas)
--   Parte 3 = PROPUESTA_V4__pedidos.sql      (Módulo 4 · Pedidos y Producción)
--   Parte 4 = NUEVO: Reportes Financieros y Configuración (Módulo 6 y apartado Configuración)
--   Parte 5 = NUEVO: cuentas de WhatsApp y correo de las notificaciones (tabla ConfiguracionNotificacion)
--   Parte 6 = NUEVO: casilla «Sus opciones pueden tener receta» en las características (Caracteristica.PermiteReceta)
--   Parte 7 = NUEVO: foto o PDF del comprobante que sube el cliente desde la página web (tabla ComprobanteArchivo)
--   Parte 8 = NUEVO: se quita el método de pago «Depósito» (los pagos viejos pasan a «Transferencia»)
--
-- CÓMO USARLO
--   1. Ten tu base creada con DBCarmir/CARMIR_BaseDeDatos.sql (con o sin datos).
--   2. Ejecuta ESTE archivo completo (MySQL Workbench: abrir el archivo y el rayo ⚡,
--      o en consola:  mysql -u root -p < MIGRACION_COMPLETA.sql).
--   3. Listo. NO ejecutes además las PROPUESTA_V2, V3 ni V4: ya están aquí dentro.
--
-- Es seguro ejecutarlo más de una vez: cada cambio revisa primero si ya existe
-- (columnas, llaves foráneas, índices, tablas y valores). No borra datos ni tablas.
-- Funciona en MySQL 8.0 o superior y en MariaDB 10.4 o superior.
--
-- Semillas (datos de prueba, TODAS OPCIONALES; ejecútalas después de este script y en este orden):
--   semillas/semilla_modulo7_usuarios.sql    permisos + usuario de prueba maria / Maria2026
--   semillas/semilla_modulo3_catalogo.sql    categorías, opciones y 3 productos
--   semillas/semilla_modulo5_inventario.sql  22 insumos con compra inicial y recetas
--   semillas/semilla_modulo4_pedidos.sql     9 pedidos de prueba (necesita las semillas 3 y 5)
--   semillas/semilla_modulo6_reportes.sql    pagos, compras y mermas de meses pasados para los reportes
-- =====================================================================
USE CARMIR_DB;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 0. Procedimientos de apoyo (se borran al final)
--    MySQL no tiene "ADD COLUMN IF NOT EXISTS", así que estos procedimientos
--    revisan information_schema antes de hacer cada cambio.
--    Se compara con LOWER() porque en Windows MySQL guarda los nombres en minúsculas.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS carmir_agregar_columna;
DROP PROCEDURE IF EXISTS carmir_agregar_llave_foranea;
DROP PROCEDURE IF EXISTS carmir_agregar_indice;

DELIMITER $$

-- Agrega una columna solo si la tabla todavía no la tiene
CREATE PROCEDURE carmir_agregar_columna(IN tabla VARCHAR(64), IN columna VARCHAR(64), IN definicion TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND LOWER(TABLE_NAME) = LOWER(tabla)
                   AND LOWER(COLUMN_NAME) = LOWER(columna)) THEN
    SET @carmir_sql = CONCAT('ALTER TABLE ', tabla, ' ADD COLUMN ', columna, ' ', definicion);
    PREPARE sentencia FROM @carmir_sql;
    EXECUTE sentencia;
    DEALLOCATE PREPARE sentencia;
  END IF;
END$$

-- Agrega una llave foránea solo si no existe una restricción con ese nombre
CREATE PROCEDURE carmir_agregar_llave_foranea(IN tabla VARCHAR(64), IN nombre VARCHAR(64), IN definicion TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = DATABASE()
                   AND LOWER(TABLE_NAME) = LOWER(tabla)
                   AND LOWER(CONSTRAINT_NAME) = LOWER(nombre)) THEN
    SET @carmir_sql = CONCAT('ALTER TABLE ', tabla, ' ADD CONSTRAINT ', nombre, ' ', definicion);
    PREPARE sentencia FROM @carmir_sql;
    EXECUTE sentencia;
    DEALLOCATE PREPARE sentencia;
  END IF;
END$$

-- Crea un índice solo si la tabla todavía no tiene uno con ese nombre
CREATE PROCEDURE carmir_agregar_indice(IN tabla VARCHAR(64), IN nombre VARCHAR(64), IN columnas TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND LOWER(TABLE_NAME) = LOWER(tabla)
                   AND LOWER(INDEX_NAME) = LOWER(nombre)) THEN
    SET @carmir_sql = CONCAT('CREATE INDEX ', nombre, ' ON ', tabla, ' (', columnas, ')');
    PREPARE sentencia FROM @carmir_sql;
    EXECUTE sentencia;
    DEALLOCATE PREPARE sentencia;
  END IF;
END$$

DELIMITER ;


-- =====================================================================
-- PARTE 1 · Módulo 3 · Catálogo (antes PROPUESTA_V2__catalogo.sql)
-- =====================================================================

-- 1.1 RF-4: distinguir las plantillas de postre de los productos normales
CALL carmir_agregar_columna('Postre', 'EsPlantilla',
  'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Si el postre es una plantilla base que el cliente puede personalizar (RF-4)'' AFTER MostrarEnTienda');


-- =====================================================================
-- PARTE 2 · Módulo 5 · Inventario y Recetas (antes PROPUESTA_V3__inventario.sql)
-- =====================================================================

-- 2.1 La Administradora no está en la tabla Usuario: sus compras y mermas se guardan con IdUsuario vacío
--     (volver a ejecutarlo no cambia nada)
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE MovimientoInventario
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que lo registró (relación Captura); vacío = Administradora o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

-- 2.2 El costo de una receta solo cuenta las recetas activas (RF-58)
CREATE OR REPLACE VIEW vw_CostoReceta AS
SELECT r.IdReceta, r.IdOpcion, r.NombreReceta,
       COALESCE(SUM(ri.CantidadInsumo * i.CostoUnitario), 0) AS CostoReceta
FROM Receta r
LEFT JOIN RecetaInsumo ri ON ri.IdReceta = r.IdReceta
LEFT JOIN Insumo i ON i.IdInsumo = ri.IdInsumo
WHERE r.EstadoReceta = 'Activa'
GROUP BY r.IdReceta, r.IdOpcion, r.NombreReceta;


-- =====================================================================
-- PARTE 3 · Módulo 4 · Pedidos y Producción (antes PROPUESTA_V4__pedidos.sql)
-- =====================================================================

-- 3.1 Historial de estados sin usuario (Administradora, cliente desde la web o el sistema)
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE HistorialEstado
  MODIFY IdUsuario INT NULL COMMENT 'Usuario que hizo el cambio (relación Realiza); vacío = Administradora, cliente (web) o el sistema';
SET FOREIGN_KEY_CHECKS = 1;

-- 3.2 Quién canceló el pedido (RF-39)
CALL carmir_agregar_columna('Cancelacion', 'IdUsuario',
  'INT NULL COMMENT ''Usuario que canceló; vacío = Administradora o el cliente'' AFTER Folio');
CALL carmir_agregar_llave_foranea('Cancelacion', 'fk_cancelacion_idusuario',
  'FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE');

-- 3.3 Baja de pedidos registrados por error (no es un estado ni una cancelación)
CREATE TABLE IF NOT EXISTS BajaPedido (
  Folio INT NOT NULL COMMENT 'Pedido dado de baja',
  MotivoBaja VARCHAR(255) NOT NULL COMMENT 'Por qué se dio de baja (duplicado, de prueba, datos falsos...)',
  FechaHoraBaja DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la baja',
  IdUsuario INT NULL COMMENT 'Usuario que dio la baja; vacío = Administradora',
  CONSTRAINT pk_bajapedido PRIMARY KEY (Folio),
  CONSTRAINT fk_bajapedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_bajapedido_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Pedidos dados de baja: se conservan en el historial pero salen de las listas de trabajo.';

-- 3.4 Bitácora de modificaciones del pedido
CREATE TABLE IF NOT EXISTS ModificacionPedido (
  IdModificacion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único de la modificación',
  Folio INT NOT NULL COMMENT 'Pedido modificado',
  IdUsuario INT NULL COMMENT 'Usuario que modificó; vacío = Administradora',
  DatosAnteriores TEXT NOT NULL COMMENT 'Datos que cambiaron, como estaban antes',
  DatosNuevos TEXT NOT NULL COMMENT 'Datos que cambiaron, como quedaron',
  FechaHoraModificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento de la modificación',
  CONSTRAINT pk_modificacionpedido PRIMARY KEY (IdModificacion),
  CONSTRAINT fk_modificacionpedido_folio FOREIGN KEY (Folio) REFERENCES Pedido (Folio) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_modificacionpedido_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Qué cambió en cada modificación de un pedido, cuándo y quién lo hizo.';

-- 3.5 Notificaciones pendientes de reenvío (RF-47) y motivo del fallo
ALTER TABLE Notificacion
  MODIFY ResultadoEnvio ENUM('Enviado','Fallido','Pendiente') NOT NULL COMMENT 'Si el mensaje se entregó; Pendiente = se reintentará';
CALL carmir_agregar_columna('Notificacion', 'MotivoFallo',
  'VARCHAR(255) NULL COMMENT ''Por qué falló el envío'' AFTER ResultadoEnvio');


-- =====================================================================
-- PARTE 4 · NUEVO · Reportes Financieros (Módulo 6) y apartado Configuración
-- =====================================================================

-- 4.1 Parámetros del negocio que antes estaban fijos en config/app.properties.
--     Los edita el apartado "Configuración" del panel y también los leerá la tienda en línea.
CREATE TABLE IF NOT EXISTS Configuracion (
  Clave VARCHAR(50) NOT NULL COMMENT 'Nombre del parámetro, por ejemplo CapacidadDiaria',
  Valor VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Valor guardado como texto',
  Descripcion VARCHAR(255) NULL COMMENT 'Para qué sirve el parámetro',
  CONSTRAINT pk_configuracion PRIMARY KEY (Clave)
) ENGINE=InnoDB COMMENT='Parámetros del negocio (horario, anticipo, capacidad, avisos...).';

-- 4.2 Bitácora de cambios de la configuración: quién cambió qué y cuándo
CREATE TABLE IF NOT EXISTS CambioConfiguracion (
  IdCambio INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador único del cambio',
  Clave VARCHAR(50) NOT NULL COMMENT 'Parámetro o código postal que cambió',
  ValorAnterior VARCHAR(255) NULL COMMENT 'Valor antes del cambio; vacío si es nuevo',
  ValorNuevo VARCHAR(255) NULL COMMENT 'Valor después del cambio; vacío si se eliminó',
  IdUsuario INT NULL COMMENT 'Usuario que hizo el cambio; vacío = Administradora',
  FechaHoraCambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento del cambio',
  CONSTRAINT pk_cambioconfiguracion PRIMARY KEY (IdCambio),
  CONSTRAINT fk_cambioconfiguracion_idusuario FOREIGN KEY (IdUsuario) REFERENCES Usuario (IdUsuario) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Historial de cambios de la configuración y de la cobertura de envío.';

-- 4.3 Códigos postales a los que se hace envío a domicilio (RF-10) y su costo
CREATE TABLE IF NOT EXISTS CodigoPostalCobertura (
  CodigoPostal CHAR(5) NOT NULL COMMENT 'Código postal con servicio a domicilio',
  CostoEnvio DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Lo que se cobra por enviar a ese código postal',
  CONSTRAINT pk_codigopostalcobertura PRIMARY KEY (CodigoPostal),
  CONSTRAINT chk_cobertura_cp CHECK (CodigoPostal REGEXP '^[0-9]{5}$'),
  CONSTRAINT chk_cobertura_costo CHECK (CostoEnvio >= 0)
) ENGINE=InnoDB COMMENT='Zona de cobertura de envío a domicilio.';

-- 4.4 Valores iniciales (INSERT IGNORE: si ya existen, se respetan los que haya cambiado la administradora)
INSERT IGNORE INTO Configuracion (Clave, Valor, Descripcion) VALUES
  ('NombreNegocio',            'Delicias de Postres', 'Nombre del negocio que aparece en pantallas, tickets y mensajes'),
  ('CapacidadDiaria',          '6',     'Pedidos que se pueden entregar en un mismo día antes de avisar "Agenda saturada"'),
  ('DiasParaPreparar',         '2',     'Días antes de la entrega en que un pedido debe pasar a "En preparación"'),
  ('DiasMaximosAgenda',        '62',    'Días máximos de un rango personalizado en la agenda de producción'),
  ('DiasAnticipacionMinima',   '3',     'Días mínimos entre el pedido y la entrega (tienda en línea)'),
  ('HoraApertura',             '09:00', 'Primera hora de entrega (formato HH:mm)'),
  ('HoraCierre',               '19:00', 'Última hora de entrega (formato HH:mm)'),
  ('DiasLaborables',           'L,M,X,J,V,S', 'Días con entregas: L M X J V S D separados por coma'),
  ('PorcentajeAnticipo',       '50',    'Porcentaje del total que se pide como anticipo'),
  ('DatosBancarios',           '',      'Datos para transferencia; vacío = solo pago en sucursal'),
  ('DiasAvisoCaducidad',       '3',     'Días antes de la caducidad de un lote en que se avisa "Próxima caducidad"'),
  ('IntentosMaximosLogin',     '3',     'Intentos fallidos seguidos antes de bloquear una cuenta del personal'),
  ('MinutosInactividadSesion', '30',    'Minutos sin usar el panel antes de cerrar la sesión (0 = nunca)');

-- 4.5 Índices para que los reportes por periodo sean rápidos
CALL carmir_agregar_indice('Pago', 'idx_pago_validacion_fecha', 'EstadoValidacion, FechaPago');
CALL carmir_agregar_indice('Pedido', 'idx_pedido_solicitud', 'FechaSolicitud');
CALL carmir_agregar_indice('MovimientoInventario', 'idx_movimiento_tipo_fecha', 'TipoMovimiento, FechaMovimiento');


-- =====================================================================
-- PARTE 5 · NUEVO · Cuentas de WhatsApp y correo de las notificaciones (RF-47)
-- =====================================================================
-- 5.1 Datos de WhatsApp Business Cloud API y del correo SMTP que se escriben en el Panel
--     (Configuración → Respaldos y sistema → Notificaciones al cliente).
--     El token y la contraseña se guardan CIFRADOS por el Panel (EsSecreto = 1).
--     Va en una tabla aparte de Configuracion para que el usuario de la página web
--     (carmir_pedidos) no pueda leerla: no tiene permiso sobre esta tabla.
--     No lleva valores iniciales: mientras esté vacía el Panel usa notificaciones.properties (modo simulado).
CREATE TABLE IF NOT EXISTS ConfiguracionNotificacion (
  Clave VARCHAR(50) NOT NULL COMMENT 'Nombre del dato, por ejemplo SmtpServidor o WhatsAppToken',
  Valor VARCHAR(2000) NOT NULL DEFAULT '' COMMENT 'Valor como texto; si EsSecreto = 1 va cifrado',
  EsSecreto TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = token o contraseña (cifrado, nunca se muestra)',
  FechaModificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Último cambio',
  CONSTRAINT pk_configuracionnotificacion PRIMARY KEY (Clave)
) ENGINE=InnoDB COMMENT='Cuentas de WhatsApp y correo para avisar a los clientes (RF-47).';


-- =====================================================================
-- PARTE 6 · NUEVO · Características que pueden tener receta
-- =====================================================================
-- 6.1 Casilla del Catálogo (Característica → «Sus opciones pueden tener receta»).
--     1 = las opciones de esa característica pueden tener receta (valor de todas las existentes).
--     0 = no admite receta (por ejemplo, una nota o un mensaje escrito); el Panel ya no
--         las ofrece al registrar recetas.
CALL carmir_agregar_columna('Caracteristica', 'PermiteReceta',
  'TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''Si las opciones de la característica pueden tener receta'' AFTER Disponible');


-- =====================================================================
-- PARTE 7 · NUEVO · Comprobantes de pago subidos desde la página web (RF-13 y RF-42)
-- =====================================================================
-- 7.1 El cliente ya puede subir la foto (JPG, PNG o WEBP) o el PDF de su comprobante desde sus archivos,
--     además de pegar un enlace (Pago.LinkComprobante). El archivo se guarda aquí, uno por pago,
--     con el mismo IdPago. La API revisa el tipo real del archivo y que no pase de 5 MB.
--     Se guarda en la base de datos (no en una carpeta pública) para que nadie lo abra desde internet;
--     el Panel lo muestra en «Validación de pagos». Si se borra el pago, se borra su archivo.
--     MEDIUMBLOB admite hasta 16 MB.
CREATE TABLE IF NOT EXISTS ComprobanteArchivo (
  IdPago INT NOT NULL COMMENT 'Pago al que pertenece el comprobante (mismo IdPago)',
  NombreArchivo VARCHAR(120) NOT NULL COMMENT 'Nombre del archivo que subió el cliente (limpio, sin rutas)',
  TipoContenido VARCHAR(40) NOT NULL COMMENT 'image/jpeg, image/png, image/webp o application/pdf',
  TamanoBytes INT NOT NULL COMMENT 'Tamaño del archivo en bytes (máximo 5 MB)',
  Contenido MEDIUMBLOB NOT NULL COMMENT 'El archivo tal cual',
  FechaSubida DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuándo lo subió el cliente',
  CONSTRAINT pk_comprobantearchivo PRIMARY KEY (IdPago),
  CONSTRAINT fk_comprobantearchivo_idpago FOREIGN KEY (IdPago) REFERENCES Pago (IdPago) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_comprobantearchivo_tamano CHECK (TamanoBytes > 0 AND TamanoBytes <= 5242880),
  CONSTRAINT chk_comprobantearchivo_tipo CHECK (TipoContenido IN ('image/jpeg', 'image/png', 'image/webp', 'application/pdf'))
) ENGINE=InnoDB COMMENT='Foto o PDF del comprobante que el cliente subió desde la página web.';

-- 7.2 Permiso para el usuario de la página web (solo si ya existe; si no, lo da crear_usuario_pedidos.sql).
--     Solo puede AGREGAR comprobantes; no puede leerlos, cambiarlos ni borrarlos.
SET @carmir_hay_usuario_web = (SELECT COUNT(*) FROM mysql.user WHERE User = 'carmir_pedidos' AND Host = 'localhost');
SET @carmir_sql = IF(@carmir_hay_usuario_web > 0,
  'GRANT INSERT ON ComprobanteArchivo TO ''carmir_pedidos''@''localhost''',
  'SELECT ''Sin usuario carmir_pedidos: se omite el permiso'' AS Aviso');
PREPARE carmir_sentencia FROM @carmir_sql;
EXECUTE carmir_sentencia;
DEALLOCATE PREPARE carmir_sentencia;
SET @carmir_hay_usuario_web = (SELECT COUNT(*) FROM mysql.user WHERE User = 'carmir_pedidos' AND Host = '127.0.0.1');
SET @carmir_sql = IF(@carmir_hay_usuario_web > 0,
  'GRANT INSERT ON ComprobanteArchivo TO ''carmir_pedidos''@''127.0.0.1''',
  'SELECT ''Sin usuario carmir_pedidos@127.0.0.1: se omite el permiso'' AS Aviso');
PREPARE carmir_sentencia FROM @carmir_sql;
EXECUTE carmir_sentencia;
DEALLOCATE PREPARE carmir_sentencia;


-- =====================================================================
-- PARTE 8 · NUEVO · Se quita el método de pago «Depósito» (RF-11 y RF-42)
-- =====================================================================
-- 8.1 La página web y el Panel solo ofrecen «Transferencia» y «Efectivo».
--     Los pagos que se hayan guardado como «Deposito» pasan a «Transferencia»
--     (los dos son pagos al banco con número de referencia) y después se quita
--     «Deposito» de la lista de valores. Si ya se aplicó, no cambia nada.
--     (IdPago > 0 es para que MySQL Workbench lo acepte con el modo de actualización segura)
UPDATE Pago SET MetodoPago = 'Transferencia' WHERE IdPago > 0 AND MetodoPago = 'Deposito';
ALTER TABLE Pago
  MODIFY MetodoPago ENUM('Transferencia','Efectivo') NOT NULL COMMENT 'Forma de pago';


-- ---------------------------------------------------------------------
-- 5. Limpieza: se borran los procedimientos de apoyo
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS carmir_agregar_columna;
DROP PROCEDURE IF EXISTS carmir_agregar_llave_foranea;
DROP PROCEDURE IF EXISTS carmir_agregar_indice;

-- Resumen: debe mostrar las tablas nuevas y el número de parámetros
SELECT 'Migración completa aplicada' AS Resultado,
       (SELECT COUNT(*) FROM Configuracion) AS Parametros,
       (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND LOWER(TABLE_NAME) IN ('bajapedido', 'modificacionpedido', 'configuracion',
                                     'cambioconfiguracion', 'codigopostalcobertura',
                                     'configuracionnotificacion', 'comprobantearchivo')) AS TablasNuevas;

-- Por si el archivo anterior dejó otro delimitador
DELIMITER ;

-- ===================== Instalación terminada =====================
SELECT 'Instalación completa de CARMIR_DB' AS Resultado;
