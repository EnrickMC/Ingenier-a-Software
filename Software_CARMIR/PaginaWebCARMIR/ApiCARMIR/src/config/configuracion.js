/*
 * Configuración de la API.
 * Todos los datos sensibles (usuario y contraseña de MySQL) vienen del archivo .env,
 * que NO se sube a Git. El archivo .env.example muestra qué variables hay que llenar.
 */
const path = require('path');
const dotenv = require('dotenv');

// Se carga el .env que está en la carpeta ApiCARMIR (no importa desde dónde se ejecute)
dotenv.config({ path: path.join(__dirname, '..', '..', '.env'), quiet: true });

// Lee una variable de entorno; si no existe regresa el valor por defecto
function leerTexto(nombre, valorPorDefecto) {
  const valor = process.env[nombre];
  if (valor === undefined || valor === null) {
    return valorPorDefecto;
  }
  return valor.trim();
}

// Lee una variable que debe ser un número entero positivo
function leerEntero(nombre, valorPorDefecto) {
  const texto = leerTexto(nombre, '');
  if (texto === '') {
    return valorPorDefecto;
  }
  const numero = Number(texto);
  if (!Number.isInteger(numero) || numero <= 0) {
    throw new Error('La variable ' + nombre + ' debe ser un número entero positivo');
  }
  return numero;
}

// Lee una variable obligatoria; si falta, la API no arranca
function leerObligatoria(nombre) {
  const valor = leerTexto(nombre, '');
  if (valor === '') {
    throw new Error('Falta la variable ' + nombre + ' en el archivo .env (copia .env.example como .env)');
  }
  return valor;
}

// Convierte "http://a,http://b" en una lista de orígenes permitidos
function leerLista(nombre, valorPorDefecto) {
  const texto = leerTexto(nombre, valorPorDefecto);
  const lista = [];
  const partes = texto.split(',');
  for (let i = 0; i < partes.length; i++) {
    const parte = partes[i].trim();
    if (parte !== '') {
      lista.push(parte);
    }
  }
  return lista;
}

const configuracion = {
  entorno: leerTexto('NODE_ENV', 'development'),
  servidor: {
    // 127.0.0.1 = solo se puede usar desde esta computadora
    host: leerTexto('API_HOST', '127.0.0.1'),
    puerto: leerEntero('API_PUERTO', 3000),
    origenesPermitidos: leerLista('CORS_ORIGENES', 'http://localhost:4200'),
    peticionesPorMinuto: leerEntero('LIMITE_PETICIONES_MINUTO', 300),
    // Módulo 2: pedidos que se pueden registrar por hora desde la misma conexión
    pedidosPorHora: leerEntero('LIMITE_PEDIDOS_HORA', 10)
  },
  baseDatos: {
    host: leerTexto('DB_HOST', '127.0.0.1'),
    puerto: leerEntero('DB_PUERTO', 3306),
    nombre: leerTexto('DB_NOMBRE', 'CARMIR_DB'),
    usuario: leerObligatoria('DB_USUARIO'),
    // Puede venir vacía (por ejemplo root sin contraseña en una instalación local)
    contrasena: leerTexto('DB_CONTRASENA', ''),
    conexionesMaximas: leerEntero('DB_CONEXIONES_MAXIMAS', 10),
    tiempoEsperaMs: leerEntero('DB_TIEMPO_ESPERA_MS', 10000)
  },
  // Módulo 2 · Portal de Pedidos: usuario de MySQL aparte que SÍ puede guardar pedidos,
  // pero solo en las tablas que lo necesitan (ver sql/crear_usuario_pedidos.sql).
  // Si se deja vacío, el catálogo funciona igual y los pedidos responden "no disponible".
  baseDatosEscritura: {
    usuario: leerTexto('DB_ESCRITURA_USUARIO', ''),
    contrasena: leerTexto('DB_ESCRITURA_CONTRASENA', ''),
    conexionesMaximas: leerEntero('DB_ESCRITURA_CONEXIONES_MAXIMAS', 5)
  }
};

module.exports = configuracion;
