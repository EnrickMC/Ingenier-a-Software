/*
 * Punto de entrada de la API de CARMIR.
 * Ejecutar con:  npm start
 */
const crearApp = require('./app');
const configuracion = require('./config/configuracion');
const baseDatos = require('./config/baseDatos');

const app = crearApp();
const servidor = configuracion.servidor;

const instancia = app.listen(servidor.puerto, servidor.host, alIniciar);

async function alIniciar() {
  console.log('API de CARMIR escuchando en http://' + servidor.host + ':' + servidor.puerto + '/api');
  console.log('Orígenes permitidos (CORS): ' + servidor.origenesPermitidos.join(', '));
  try {
    await baseDatos.probarConexion();
    console.log('Conexión a MySQL correcta (base ' + configuracion.baseDatos.nombre + ')');
  } catch (error) {
    // La API sigue encendida: cuando MySQL vuelva, el pool se reconecta solo
    console.error('AVISO: no se pudo conectar a MySQL (' + error.code + '). Ejecuta "npm run diagnostico" para ver la causa y cómo corregirla.');
  }
  await revisarEscritura();
}

// Módulo 2: revisa el usuario que guarda los pedidos (DB_ESCRITURA_USUARIO)
async function revisarEscritura() {
  if (!baseDatos.escrituraConfigurada()) {
    console.error('AVISO: falta DB_ESCRITURA_USUARIO en el .env: el catálogo funciona, pero no se pueden registrar pedidos.');
    return;
  }
  try {
    await baseDatos.probarConexionEscritura();
    console.log('Usuario de pedidos correcto (' + configuracion.baseDatosEscritura.usuario + ')');
  } catch (error) {
    console.error('AVISO: el usuario de pedidos no pudo conectarse (' + error.code + '). Revisa DB_ESCRITURA_USUARIO y DB_ESCRITURA_CONTRASENA.');
  }
}

// Apagado ordenado: deja de recibir peticiones y cierra las conexiones a MySQL
async function apagar(senal) {
  console.log('Recibí ' + senal + ', cerrando la API...');
  instancia.close();
  try {
    await baseDatos.cerrar();
  } catch (error) {
    console.error('No se pudieron cerrar las conexiones: ' + error.message);
  }
  process.exit(0);
}

process.on('SIGINT', apagar);
process.on('SIGTERM', apagar);
