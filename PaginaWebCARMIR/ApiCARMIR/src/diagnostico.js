/*
 * Diagnóstico de la API de CARMIR.
 * Uso (dentro de la carpeta ApiCARMIR):  npm run diagnostico
 *
 * Revisa paso por paso lo que necesita el catálogo público y, si algo falla,
 * explica qué hacer:
 *   1. Que exista el archivo .env y que su configuración sea válida.
 *   2. Que se pueda conectar a MySQL con ese usuario y contraseña.
 *   3. Que existan las tablas y vistas que usa el catálogo.
 *   4. Cuántos productos cumplen cada condición para verse en la página.
 * Solo hace consultas SELECT: no modifica nada de la base de datos.
 */
const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

const RUTA_ENV = path.join(__dirname, '..', '.env');
const CONTRASENA_DE_EJEMPLO = 'escribe_aqui_la_contrasena';

// Tablas y vistas que lee el catálogo público
const OBJETOS_REQUERIDOS = [
  'CategoriaPostre',
  'Postre',
  'PostreOpcion',
  'Caracteristica',
  'OpcionCaracteristica',
  'ExclusionOpciones',
  'Receta',
  'RecetaInsumo',
  'Insumo',
  'vw_CostoReceta',
  'vw_CostoPrecioPostre'
];

// Lleva la cuenta de problemas encontrados
let problemas = 0;

// ===================== Mensajes en consola =====================

function titulo(texto) {
  console.log('');
  console.log('== ' + texto + ' ==');
}

function bien(texto) {
  console.log('  [OK]    ' + texto);
}

function aviso(texto) {
  console.log('  [AVISO] ' + texto);
}

function falla(texto, instrucciones) {
  problemas = problemas + 1;
  console.log('  [FALLA] ' + texto);
  for (let i = 0; i < instrucciones.length; i++) {
    console.log('          -> ' + instrucciones[i]);
  }
}

// ===================== Paso 1: archivo .env =====================

function revisarEnv() {
  titulo('1. Archivo .env');
  if (!fs.existsSync(RUTA_ENV)) {
    falla('No existe el archivo ' + RUTA_ENV, [
      'Copia el archivo .env.example y nómbralo .env (en la carpeta ApiCARMIR).',
      'Después escribe en él tu usuario y contraseña de MySQL.'
    ]);
    return null;
  }
  bien('Existe el archivo .env');

  let configuracion = null;
  try {
    configuracion = require('./config/configuracion');
  } catch (error) {
    falla('La configuración del .env no es válida: ' + error.message, [
      'Compara tu .env con .env.example y corrige esa variable.'
    ]);
    return null;
  }

  const bd = configuracion.baseDatos;
  bien('Base: ' + bd.nombre + ' en ' + bd.host + ':' + bd.puerto + ' con el usuario ' + bd.usuario);
  if (bd.contrasena === CONTRASENA_DE_EJEMPLO) {
    falla('DB_CONTRASENA todavía tiene el texto de ejemplo', [
      'Escribe la contraseña real de ' + bd.usuario + ' en el .env.'
    ]);
  }
  if (bd.contrasena === '') {
    aviso('DB_CONTRASENA está vacía (solo funciona si ese usuario de MySQL no tiene contraseña).');
  }
  bien('La API escuchará en http://' + configuracion.servidor.host + ':' + configuracion.servidor.puerto + '/api');
  bien('Páginas permitidas (CORS): ' + configuracion.servidor.origenesPermitidos.join(', '));
  if (configuracion.servidor.origenesPermitidos.indexOf('http://localhost:4200') === -1) {
    aviso('http://localhost:4200 no está en CORS_ORIGENES: la página de Angular no podrá usar la API.');
  }
  return configuracion;
}

// ===================== Paso 2: conexión a MySQL =====================

// Explica los errores de conexión más comunes
function explicarErrorConexion(error, bd) {
  const codigo = error.code;
  if (codigo === 'ECONNREFUSED' || codigo === 'ETIMEDOUT' || codigo === 'ENOTFOUND') {
    return [
      'MySQL no está encendido o no escucha en ' + bd.host + ':' + bd.puerto + '.',
      'Windows: abre "Servicios" y arranca el servicio MySQL80 (o abre MySQL Workbench y revisa que conecte).',
      'Si tu MySQL usa otro puerto, cámbialo en DB_PUERTO.'
    ];
  }
  if (codigo === 'ER_ACCESS_DENIED_ERROR' || codigo === 'ER_ACCESS_DENIED_NO_PASSWORD_ERROR') {
    const pasos = ['El usuario "' + bd.usuario + '" o su contraseña no son correctos.'];
    if (bd.usuario === 'carmir_web') {
      pasos.push('Si no has creado ese usuario: abre sql/crear_usuario_lectura.sql en MySQL Workbench (como root) y ejecútalo.');
      pasos.push('La contraseña del .env debe ser la misma que escribiste en ese script.');
    } else {
      pasos.push('Revisa DB_USUARIO y DB_CONTRASENA en el .env (por ejemplo root y tu contraseña de MySQL).');
    }
    return pasos;
  }
  if (codigo === 'ER_BAD_DB_ERROR') {
    return [
      'La base "' + bd.nombre + '" no existe en ese MySQL.',
      'Revisa DB_NOMBRE en el .env o crea la base con los scripts de la carpeta DBCarmir.'
    ];
  }
  if (codigo === 'ER_DBACCESS_DENIED_ERROR') {
    return [
      'El usuario "' + bd.usuario + '" no tiene permiso sobre la base "' + bd.nombre + '" (o esa base no existe).',
      'Revisa que DB_NOMBRE esté bien escrito (normalmente CARMIR_DB).',
      'Ejecuta como root: GRANT SELECT ON ' + bd.nombre + '.* TO \'' + bd.usuario + '\'@\'localhost\';'
    ];
  }
  if (codigo === 'ER_NOT_SUPPORTED_AUTH_MODE') {
    return ['El método de autenticación del usuario no es compatible; usa sql/crear_usuario_lectura.sql para crear carmir_web.'];
  }
  return ['Error no esperado (' + codigo + '): ' + error.message];
}

async function conectar(configuracion) {
  titulo('2. Conexión a MySQL');
  const bd = configuracion.baseDatos;
  try {
    const conexion = await mysql.createConnection({
      host: bd.host,
      port: bd.puerto,
      user: bd.usuario,
      password: bd.contrasena,
      database: bd.nombre,
      connectTimeout: bd.tiempoEsperaMs
    });
    bien('Conexión correcta');
    return conexion;
  } catch (error) {
    falla('No se pudo conectar (' + error.code + ')', explicarErrorConexion(error, bd));
    return null;
  }
}

// ===================== Paso 3: tablas y vistas =====================

async function revisarObjetos(conexion, nombreBase) {
  titulo('3. Tablas y vistas del catálogo');
  const resultado = await conexion.execute(
    'SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?',
    [nombreBase]
  );
  const existentes = [];
  for (const fila of resultado[0]) {
    existentes.push(String(fila.TABLE_NAME).toLowerCase());
  }

  const faltantes = [];
  for (const nombre of OBJETOS_REQUERIDOS) {
    if (existentes.indexOf(nombre.toLowerCase()) === -1) {
      faltantes.push(nombre);
    }
  }
  if (faltantes.length > 0) {
    falla('Faltan: ' + faltantes.join(', '), [
      'Ejecuta en orden los scripts de DBCarmir: la base original y después MIGRACION_COMPLETA.sql.',
      'Si el usuario es carmir_web, vuelve a ejecutar sql/crear_usuario_lectura.sql para que vea las tablas nuevas.'
    ]);
    return false;
  }
  bien('Están las ' + OBJETOS_REQUERIDOS.length + ' tablas y vistas que usa el catálogo');

  // Columna que agrega MIGRACION_COMPLETA.sql
  const columna = await conexion.execute(
    "SELECT COUNT(*) AS total FROM information_schema.COLUMNS " +
    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'Postre' AND COLUMN_NAME = 'MostrarEnTienda'",
    [nombreBase]
  );
  if (Number(columna[0][0].total) === 0) {
    falla('La tabla Postre no tiene la columna MostrarEnTienda', [
      'Ejecuta DBCarmir/MIGRACION_COMPLETA.sql: la página la necesita para saber qué productos mostrar.'
    ]);
    return false;
  }
  bien('La tabla Postre tiene la columna MostrarEnTienda (MIGRACION_COMPLETA aplicada)');
  return true;
}

// ===================== Paso 4: datos del catálogo =====================

async function contar(conexion, sql) {
  const resultado = await conexion.execute(sql);
  return Number(resultado[0][0].total);
}

async function revisarDatos(conexion) {
  titulo('4. Datos para que un producto aparezca en la página');

  const categoriasActivas = await contar(
    conexion,
    "SELECT COUNT(*) AS total FROM CategoriaPostre WHERE EstadoCategoria = 'Activa'"
  );
  if (categoriasActivas === 0) {
    falla('No hay categorías activas', [
      'En el Panel: Catálogo de Postres > Categorías, crea una categoría con estado Activa.'
    ]);
  } else {
    bien('Categorías activas: ' + categoriasActivas);
  }

  const conNombre = await contar(
    conexion,
    "SELECT COUNT(*) AS total FROM Postre WHERE NombrePostre IS NOT NULL AND TRIM(NombrePostre) <> ''"
  );
  const enTienda = await contar(
    conexion,
    "SELECT COUNT(*) AS total FROM Postre WHERE MostrarEnTienda = 1 AND NombrePostre IS NOT NULL AND TRIM(NombrePostre) <> ''"
  );
  const enCategoriaActiva = await contar(
    conexion,
    'SELECT COUNT(*) AS total FROM Postre p JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria ' +
    "WHERE p.MostrarEnTienda = 1 AND p.NombrePostre IS NOT NULL AND TRIM(p.NombrePostre) <> '' " +
    "AND c.EstadoCategoria = 'Activa'"
  );
  const visibles = await contar(
    conexion,
    'SELECT COUNT(*) AS total FROM Postre p JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria ' +
    'JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre ' +
    "WHERE p.MostrarEnTienda = 1 AND p.NombrePostre IS NOT NULL AND TRIM(p.NombrePostre) <> '' " +
    "AND c.EstadoCategoria = 'Activa' AND v.PrecioVenta IS NOT NULL AND v.PrecioVenta > 0"
  );

  console.log('  Postres con nombre (productos base): ' + conNombre);
  console.log('    ... con "Mostrar en tienda" marcado: ' + enTienda);
  console.log('    ... y de una categoría activa:      ' + enCategoriaActiva);
  console.log('    ... y con precio mayor a 0:         ' + visibles + '  <- estos se ven en la página');

  if (conNombre === 0) {
    falla('No hay productos base', [
      'En el Panel: Catálogo de Postres, crea un postre con nombre, categoría y sus opciones (sabor, tamaño, etc.).'
    ]);
  } else if (enTienda === 0) {
    falla('Ningún producto tiene "Mostrar en tienda" marcado', [
      'En el Panel: Catálogo de Postres, edita el producto y marca "Mostrar en tienda".'
    ]);
  } else if (enCategoriaActiva === 0) {
    falla('Los productos marcados están en categorías inactivas', [
      'En el Panel: Catálogo de Postres > Categorías, pon la categoría como Activa.'
    ]);
  } else if (visibles === 0) {
    falla('Los productos marcados tienen precio 0', [
      'El precio sale de las recetas: cada opción del producto necesita una receta con insumos que tengan costo.',
      'En el Panel: Recetas (asigna receta a cada opción) e Inventario (costo de cada insumo).'
    ]);
  } else {
    bien('El catálogo mostrará ' + visibles + ' producto(s)');
  }

  // Lista los productos marcados que no se ven por falta de precio
  if (enCategoriaActiva > visibles) {
    const resultado = await conexion.execute(
      'SELECT p.IdPostre, p.NombrePostre FROM Postre p ' +
      'JOIN CategoriaPostre c ON c.IdCategoria = p.IdCategoria ' +
      'LEFT JOIN vw_CostoPrecioPostre v ON v.IdPostre = p.IdPostre ' +
      "WHERE p.MostrarEnTienda = 1 AND p.NombrePostre IS NOT NULL AND TRIM(p.NombrePostre) <> '' " +
      "AND c.EstadoCategoria = 'Activa' AND (v.PrecioVenta IS NULL OR v.PrecioVenta <= 0) " +
      'ORDER BY p.NombrePostre LIMIT 10'
    );
    aviso('Estos productos están marcados para la tienda pero su precio es 0 (revisa sus recetas):');
    for (const fila of resultado[0]) {
      console.log('            #' + fila.IdPostre + ' ' + fila.NombrePostre);
    }
  }
}

// ===================== Programa principal =====================

async function principal() {
  console.log('Diagnóstico de ApiCARMIR');
  const configuracion = revisarEnv();
  if (configuracion === null) {
    terminar();
    return;
  }
  const conexion = await conectar(configuracion);
  if (conexion === null) {
    terminar();
    return;
  }
  try {
    const objetosCompletos = await revisarObjetos(conexion, configuracion.baseDatos.nombre);
    if (objetosCompletos) {
      await revisarDatos(conexion);
    }
  } catch (error) {
    falla('Una consulta de revisión falló (' + error.code + '): ' + error.message, [
      'Revisa que el usuario tenga permiso SELECT sobre todas las tablas y vistas (sql/crear_usuario_lectura.sql).'
    ]);
  } finally {
    await conexion.end();
  }
  terminar();
}

function terminar() {
  console.log('');
  if (problemas === 0) {
    console.log('Todo bien. Arranca la API con "npm start" y abre http://localhost:4200');
    process.exitCode = 0;
  } else {
    console.log('Se encontraron ' + problemas + ' problema(s). Corrige lo marcado como [FALLA] y vuelve a ejecutar npm run diagnostico.');
    process.exitCode = 1;
  }
}

principal();
