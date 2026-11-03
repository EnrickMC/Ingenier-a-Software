/*
 * Conexión a MySQL con dos "pools" (grupos de conexiones reutilizables).
 *
 *  1. Pool de LECTURA (catálogo, reglas de entrega, seguimiento):
 *     usuario DB_USUARIO (recomendado carmir_web, solo SELECT) y cada conexión en modo SOLO LECTURA.
 *  2. Pool de ESCRITURA (Módulo 2 · registrar pedidos y comprobantes):
 *     usuario DB_ESCRITURA_USUARIO (carmir_pedidos), que solo puede INSERTAR en las tablas del pedido.
 *     Siempre se usa dentro de una transacción (conTransaccion): si algo falla se deshace todo.
 *
 * Por qué un pool:
 *  - Abrir una conexión por cada petición es lento; el pool las reutiliza.
 *  - Si una conexión se cae, el pool la descarta y abre otra (conexión estable).
 *  - Limita cuántas conexiones se abren a la vez (no satura MySQL).
 *
 * Seguridad:
 *  - Todas las consultas usan "?" (consultas parametrizadas): nunca se pega texto del usuario en el SQL.
 *  - multipleStatements = false: no se pueden mandar dos sentencias en una sola consulta.
 */
const mysql = require('mysql2/promise');
const configuracion = require('./configuracion');
const ErrorApi = require('../utilidades/ErrorApi');

// Únicas instancias de los pools para toda la API (patrón Singleton)
let pool = null;
let poolEscritura = null;

// Opciones comunes de los dos pools
function opcionesPool(usuario, contrasena, conexionesMaximas) {
  const datos = configuracion.baseDatos;
  return {
    host: datos.host,
    port: datos.puerto,
    user: usuario,
    password: contrasena,
    database: datos.nombre,
    charset: 'utf8mb4',
    connectionLimit: conexionesMaximas,
    waitForConnections: true,
    queueLimit: 50,
    connectTimeout: datos.tiempoEsperaMs,
    // Mantiene vivas las conexiones que no se usan para que MySQL no las cierre
    enableKeepAlive: true,
    keepAliveInitialDelay: 10000,
    // Cierra conexiones que lleven 60 s sin usarse
    maxIdle: conexionesMaximas,
    idleTimeout: 60000,
    multipleStatements: false,
    // Los DECIMAL llegan como número y no como texto
    decimalNumbers: true,
    // Las fechas (DATE, DATETIME) llegan como texto "2026-11-02" y no se mueven por la zona horaria
    dateStrings: true
  };
}

// ===================== Pool de lectura =====================

function obtenerPool() {
  if (pool === null) {
    const datos = configuracion.baseDatos;
    pool = mysql.createPool(opcionesPool(datos.usuario, datos.contrasena, datos.conexionesMaximas));
    // Cada conexión nueva queda en modo solo lectura
    pool.pool.on('connection', marcarSoloLectura);
  }
  return pool;
}

// Se ejecuta cada vez que el pool abre una conexión nueva.
// Aquí la conexión es la "clásica" (con callbacks), por eso se usa query sin await.
function marcarSoloLectura(conexion) {
  conexion.query('SET SESSION TRANSACTION READ ONLY');
}

// Ejecuta una consulta parametrizada de lectura y regresa las filas
async function consultar(sql, parametros) {
  let valores = parametros;
  if (valores === undefined) {
    valores = [];
  }
  // execute() usa sentencias preparadas del servidor: los valores viajan separados del SQL
  const resultado = await obtenerPool().execute(sql, valores);
  return resultado[0];
}

// ===================== Pool de escritura (Módulo 2) =====================

// true si el .env tiene el usuario de escritura (sin él no se pueden registrar pedidos)
function escrituraConfigurada() {
  return configuracion.baseDatosEscritura.usuario !== '';
}

function obtenerPoolEscritura() {
  if (!escrituraConfigurada()) {
    // Se responde como "sin conexión" para no dar detalles de la configuración al navegador
    console.error('[pedidos] Falta DB_ESCRITURA_USUARIO en el archivo .env: no se pueden registrar pedidos');
    throw new ErrorApi(503, 'SIN_CONEXION_BD', 'No fue posible conectar con el servidor. Intenta de nuevo en unos momentos.');
  }
  if (poolEscritura === null) {
    const datos = configuracion.baseDatosEscritura;
    poolEscritura = mysql.createPool(opcionesPool(datos.usuario, datos.contrasena, datos.conexionesMaximas));
  }
  return poolEscritura;
}

/*
 * Ejecuta "trabajo(conexion)" dentro de una transacción del pool de escritura.
 *  - Si trabajo termina bien: COMMIT y regresa lo que regresó trabajo.
 *  - Si lanza un error: ROLLBACK (no queda nada a medias) y el error sigue su camino.
 * nombreCandado (opcional): candado con nombre de MySQL (GET_LOCK) para que dos pedidos
 * no se registren al mismo tiempo (así la capacidad diaria no se rebasa). No requiere permisos extra.
 */
async function conTransaccion(trabajo, nombreCandado) {
  const conexion = await obtenerPoolEscritura().getConnection();
  let conCandado = false;
  try {
    if (nombreCandado !== undefined && nombreCandado !== null) {
      const filas = await ejecutar(conexion, 'SELECT GET_LOCK(?, 10) AS Obtenido', [nombreCandado]);
      if (filas.length === 0 || Number(filas[0].Obtenido) !== 1) {
        throw new ErrorApi(503, 'SERVIDOR_OCUPADO', 'Estamos recibiendo muchos pedidos. Intenta de nuevo en unos segundos.');
      }
      conCandado = true;
    }
    await conexion.beginTransaction();
    try {
      const resultado = await trabajo(conexion);
      await conexion.commit();
      return resultado;
    } catch (error) {
      await deshacer(conexion);
      throw error;
    }
  } finally {
    if (conCandado) {
      await soltarCandado(conexion, nombreCandado);
    }
    conexion.release();
  }
}

// ROLLBACK sin tapar el error original si el rollback también falla
async function deshacer(conexion) {
  try {
    await conexion.rollback();
  } catch (error) {
    console.error('[pedidos] No se pudo deshacer la transacción: ' + error.code);
  }
}

async function soltarCandado(conexion, nombreCandado) {
  try {
    await conexion.execute('SELECT RELEASE_LOCK(?)', [nombreCandado]);
  } catch (error) {
    // Si la conexión se cayó, MySQL suelta el candado solo
    console.error('[pedidos] No se pudo soltar el candado: ' + error.code);
  }
}

// Ejecuta una consulta parametrizada en una conexión de transacción.
// Regresa las filas (SELECT) o el resultado con insertId / affectedRows (INSERT, UPDATE).
async function ejecutar(conexion, sql, parametros) {
  let valores = parametros;
  if (valores === undefined) {
    valores = [];
  }
  const resultado = await conexion.execute(sql, valores);
  return resultado[0];
}

// ===================== Apoyo =====================

// Revisa que MySQL responda (lo usa /api/salud y el arranque del servidor)
async function probarConexion() {
  const filas = await consultar('SELECT 1 AS ok');
  return filas.length === 1;
}

// Revisa el usuario de escritura (solo al arrancar el servidor)
async function probarConexionEscritura() {
  const filas = await obtenerPoolEscritura().execute('SELECT 1 AS ok');
  return filas[0].length === 1;
}

// Cierra todas las conexiones (al apagar el servidor)
async function cerrar() {
  if (pool !== null) {
    await pool.end();
    pool = null;
  }
  if (poolEscritura !== null) {
    await poolEscritura.end();
    poolEscritura = null;
  }
}

module.exports = {
  consultar: consultar,
  conTransaccion: conTransaccion,
  ejecutar: ejecutar,
  escrituraConfigurada: escrituraConfigurada,
  probarConexion: probarConexion,
  probarConexionEscritura: probarConexionEscritura,
  cerrar: cerrar
};
