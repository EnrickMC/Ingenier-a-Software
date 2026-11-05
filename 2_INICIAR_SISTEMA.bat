@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title CARMIR - Sistema en Ejecucion (Consola Unica)
cd /d "%~dp0"
set "CARMIR_ROOT=%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

echo ======================================================================
echo        SISTEMA CARMIR - INICIANDO TODO EN ESTA MISMA CONSOLA
echo ======================================================================
echo.

REM --- 0. Verificar que la carpeta haya sido descomprimida ---
if not exist "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\PaginaWebCARMIR\package.json" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\PanelAdministrativoCARMIR\pom.xml" goto :falta_descomprimir

"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "try { Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned -Force -ErrorAction SilentlyContinue } catch {}; try { Get-ChildItem -LiteralPath $env:CARMIR_ROOT -ErrorAction SilentlyContinue | Unblock-File -ErrorAction SilentlyContinue } catch {}" >nul 2>nul

REM --- 1. Configurar PATH (Node.js 22+) y JAVA_HOME (JDK 25+) ---
call :configurar_entorno

REM --- 2. Encender MySQL (Servicio de Windows o XAMPP oculto, sin abrir otra ventana) ---
call :asegurar_mysql_activo

REM --- 2.5 Pedir la contrasena de root de MySQL de este equipo (Enter = sin contrasena) ---
call :pedir_root_mysql

REM --- 3. Verificar que todo este instalado y sincronizado en este equipo ---
if not defined NODE_ENCONTRADO goto :ejecutar_instalacion_previa
if not defined JAVA_HOME goto :ejecutar_instalacion_previa
if not exist "%JAVA_HOME%\bin\javac.exe" goto :ejecutar_instalacion_previa

if not exist "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\node_modules\express\package.json" goto :ejecutar_instalacion_previa
if not exist "%~dp0Software_CARMIR\PaginaWebCARMIR\node_modules\@angular\cli\bin\ng.js" goto :ejecutar_instalacion_previa
if not exist "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\.env" goto :ejecutar_instalacion_previa
if not exist "%~dp0Software_CARMIR\PanelAdministrativoCARMIR\db.local.properties" goto :ejecutar_instalacion_previa
if not exist "%~dp0Software_CARMIR\PanelAdministrativoCARMIR\target\classes\com\pac\CarmirApp.class" goto :ejecutar_instalacion_previa

REM Comprobar conexion real de la API (carmir_web + carmir_pedidos) y esquema actualizado (NumeroCapa)
pushd "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR"
call "%NODE_ENCONTRADO%\node.exe" -e "const mysql=require('mysql2/promise'),cfg=require('./src/config/configuracion');(async function(){const bd=cfg.baseDatos,c1=await mysql.createConnection({host:bd.host,port:bd.puerto,user:bd.usuario,password:bd.contrasena,database:bd.nombre,connectTimeout:5000});const [r]=await c1.execute(`SELECT COUNT(*) AS n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND LOWER(TABLE_NAME)='postreopcion' AND LOWER(COLUMN_NAME)='numerocapa'`);await c1.end();if(!Number(r[0].n))process.exit(1);const c2=await mysql.createConnection({host:bd.host,port:bd.puerto,user:cfg.baseDatosEscritura.usuario,password:cfg.baseDatosEscritura.contrasena,database:bd.nombre,connectTimeout:5000});await c2.end();})().catch(function(){process.exit(1);});" >nul 2>nul
if errorlevel 1 goto :instalar_desde_popd
popd
goto :arrancar_en_misma_consola

:instalar_desde_popd
popd

:ejecutar_instalacion_previa
echo [AVISO] Preparando o actualizando componentes automaticamente para este equipo...
echo         Ejecutando 1_INSTALAR_PRIMERA_VEZ.bat ...
echo.
set "LLAMADO_DESDE_INICIAR=1"
call "%~dp01_INSTALAR_PRIMERA_VEZ.bat"
if errorlevel 1 exit /b 1
call :configurar_entorno

:arrancar_en_misma_consola
REM La contrasena de root ya quedo guardada en db.local.properties; no pasarla a los procesos
set "MYSQL_PASS="
set "CARMIR_ROOT_PASS="
set "CARMIR_BAT_FILE=%~f0"
set "CARMIR_JS_TMP=%TEMP%\carmir_consola_unica_%RANDOM%_%RANDOM%.js"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$lines = [IO.File]::ReadAllLines($env:CARMIR_BAT_FILE, [Text.Encoding]::ASCII); $idx = [Array]::IndexOf($lines, 'REM === CARMIR_NODE_LAUNCHER_START ==='); if ($idx -ge 0) { $js = $lines[($idx + 1)..($lines.Length - 1)]; [IO.File]::WriteAllLines($env:CARMIR_JS_TMP, $js, [Text.Encoding]::ASCII) }"
if not exist "%CARMIR_JS_TMP%" (
    echo [ERROR] No se pudo preparar el coordinador de consola unica.
    pause
    exit /b 1
)

"%NODE_ENCONTRADO%\node.exe" "%CARMIR_JS_TMP%"
set "EXIT_NODE=%ERRORLEVEL%"
if exist "%CARMIR_JS_TMP%" del /f /q "%CARMIR_JS_TMP%" >nul 2>nul
if not "%EXIT_NODE%"=="0" pause
exit /b %EXIT_NODE%

REM ===================== SUBRUTINAS DE APOYO =====================

:configurar_entorno
set "NODE_ENCONTRADO="
set "NODE_TMP=%TEMP%\carmir_node_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @('C:\Program Files\nodejs', (Join-Path $env:LOCALAPPDATA 'Programs\nodejs')); $wg = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'; if (Test-Path -LiteralPath $wg) { Get-ChildItem -LiteralPath $wg -Directory -Filter 'OpenJS.NodeJS*' -ErrorAction SilentlyContinue | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -Recurse -Filter 'node.exe' -ErrorAction SilentlyContinue | ForEach-Object { $dirs += $_.DirectoryName } } }; $cmd = Get-Command node -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path $cmd.Source -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp) { $dirs += $rp.Trim() } }; foreach ($d in $dirs) { if ($d) { $ne = Join-Path $d 'node.exe'; if (Test-Path -LiteralPath $ne) { $v = (& $ne -v 2>&1 | Out-String); if ($v -match 'v(\d+)' -and [int]$Matches[1] -ge 22) { [IO.File]::WriteAllText($env:NODE_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } } }" >nul 2>nul
if exist "%NODE_TMP%" set /p NODE_ENCONTRADO=<"%NODE_TMP%"
if exist "%NODE_TMP%" del /f /q "%NODE_TMP%" >nul 2>nul
if defined NODE_ENCONTRADO set "PATH=%NODE_ENCONTRADO%;%PATH%"

set "JDK_ENCONTRADO="
set "JDK_TMP=%TEMP%\carmir_jdk_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @(); if ($env:JAVA_HOME) { $dirs += $env:JAVA_HOME }; $roots = @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft', 'C:\Program Files\BellSoft', 'C:\Program Files\Amazon Corretto', 'C:\Program Files\Zulu', (Join-Path $env:USERPROFILE '.jdks')); foreach ($r in $roots) { if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $dirs += $_.FullName } } }; $cmd = Get-Command javac -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path (Split-Path $cmd.Source -Parent) -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp -and (Test-Path -LiteralPath (Join-Path $rp.Trim() 'javac.exe'))) { $dirs += (Split-Path $rp.Trim() -Parent) } }; foreach ($d in $dirs) { $jc = Join-Path $d 'bin\javac.exe'; if (Test-Path -LiteralPath $jc) { $ver = (& $jc -version 2>&1 | Out-String); if ($ver -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 25) { [IO.File]::WriteAllText($env:JDK_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } }" >nul 2>nul
if exist "%JDK_TMP%" set /p JDK_ENCONTRADO=<"%JDK_TMP%"
if exist "%JDK_TMP%" del /f /q "%JDK_TMP%" >nul 2>nul
if defined JDK_ENCONTRADO set "JAVA_HOME=%JDK_ENCONTRADO%"
if defined JDK_ENCONTRADO set "PATH=%JDK_ENCONTRADO%\bin;%PATH%"
exit /b 0

:asegurar_mysql_activo
call :puerto_activo 3306
if not errorlevel 1 exit /b 0

"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$svcs = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(MySQL|MariaDB)' -and $_.Status -ne 'Running' }; foreach ($s in $svcs) { try { Start-Service -Name $s.Name -ErrorAction Stop; break } catch { try { Start-Process sc.exe -ArgumentList 'start', $s.Name -Verb RunAs -Wait -WindowStyle Hidden; break } catch {} } }" >nul 2>nul
ping 127.0.0.1 -n 4 >nul
call :puerto_activo 3306
if not errorlevel 1 exit /b 0

if exist "C:\xampp\mysql\bin\mysqld.exe" goto :arrancar_xampp_c
if exist "D:\xampp\mysql\bin\mysqld.exe" goto :arrancar_xampp_d
exit /b 0

:arrancar_xampp_c
echo [INFO] Encendiendo MySQL de XAMPP en segundo plano...
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'C:\xampp\mysql\bin\mysqld.exe' -ArgumentList '--defaults-file=C:\xampp\mysql\bin\my.ini','--standalone' -WindowStyle Hidden" >nul 2>nul
ping 127.0.0.1 -n 6 >nul
"C:\xampp\mysql\bin\mysql.exe" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
exit /b 0

:arrancar_xampp_d
echo [INFO] Encendiendo MySQL de XAMPP en segundo plano...
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'D:\xampp\mysql\bin\mysqld.exe' -ArgumentList '--defaults-file=D:\xampp\mysql\bin\my.ini','--standalone' -WindowStyle Hidden" >nul 2>nul
ping 127.0.0.1 -n 6 >nul
"D:\xampp\mysql\bin\mysql.exe" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
exit /b 0

:pedir_root_mysql
call :buscar_mysql
REM Si este equipo aun no tiene cliente MySQL, el instalador se encarga despues
if not defined MYSQL_EXE exit /b 0
set "MYSQL_CNF=%TEMP%\carmir_mysql_%RANDOM%_%RANDOM%.cnf"
set "MYSQL_ERR=%TEMP%\carmir_mysql_err_%RANDOM%_%RANDOM%.txt"

:pedir_pass_mysql
set "MYSQL_PASS="
echo Escribe la contrasena del usuario "root" de tu MySQL.
echo Si tu MySQL no tiene contrasena, solo presiona Enter.
set /p "MYSQL_PASS=Contrasena de root: "
call :probar_login_root
if not errorlevel 1 goto :root_mysql_correcto
call :explicar_error_mysql
echo.
goto :pedir_pass_mysql

:root_mysql_correcto
echo [OK] Conexion verificada con MySQL (usuario root).
echo.
REM Guardar la contrasena para el Panel Administrativo y dejarla lista para el instalador
call :escribir_db_local_properties
set "CARMIR_ROOT_PASS=%MYSQL_PASS%"
set "CARMIR_ROOT_VERIFICADO=1"
if exist "%MYSQL_CNF%" del /f /q "%MYSQL_CNF%" >nul 2>nul
if exist "%MYSQL_ERR%" del /f /q "%MYSQL_ERR%" >nul 2>nul
exit /b 0

:buscar_mysql
set "MYSQL_EXE="
set "MYSQL_TMP=%TEMP%\carmir_mysql_exe_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$cands = @(); Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(MySQL|MariaDB)' -and $_.State -eq 'Running' } | ForEach-Object { if ($_.PathName -match '(?i)^\W?(.+?\\bin)\\') { $cands += (Join-Path $Matches[1] 'mysql.exe') } }; foreach ($r in @('C:\Program Files\MySQL', 'C:\Program Files')) { if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Directory -Filter 'MySQL Server*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName 'bin\mysql.exe') }; Get-ChildItem -LiteralPath $r -Directory -Filter 'MariaDB*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName 'bin\mysql.exe') } } }; $cands += 'C:\xampp\mysql\bin\mysql.exe'; $cands += 'D:\xampp\mysql\bin\mysql.exe'; $cmd = Get-Command mysql -ErrorAction SilentlyContinue; if ($cmd) { $cands += $cmd.Source }; foreach ($c in $cands) { if ($c -and (Test-Path -LiteralPath $c)) { [IO.File]::WriteAllText($env:MYSQL_TMP, $c, (New-Object Text.UTF8Encoding($false))); break } }" >nul 2>nul
if exist "%MYSQL_TMP%" set /p MYSQL_EXE=<"%MYSQL_TMP%"
if exist "%MYSQL_TMP%" del /f /q "%MYSQL_TMP%" >nul 2>nul
exit /b 0

:probar_login_root
REM Primero por 127.0.0.1 y, si falla, por localhost (algunas instalaciones solo aceptan una de las dos)
set "MYSQL_HOST=127.0.0.1"
call :probar_login_host
if not errorlevel 1 exit /b 0
set "MYSQL_HOST=localhost"
call :probar_login_host
if not errorlevel 1 exit /b 0
exit /b 1

:probar_login_host
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$pw = $env:MYSQL_PASS; if ($null -eq $pw) { $pw = '' }; $pw = $pw.Trim(); $esc = $pw.Replace('\', '\\').Replace([char]34, '\' + [char]34); $lines = @('[client]', 'user=root', ('password=' + [char]34 + $esc + [char]34), ('host=' + $env:MYSQL_HOST), 'port=3306', 'default-character-set=utf8mb4'); [IO.File]::WriteAllLines($env:MYSQL_CNF, $lines, (New-Object Text.UTF8Encoding($false)))" >nul 2>nul
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" -h %MYSQL_HOST% -P 3306 -e "SELECT 1;" >nul 2>"%MYSQL_ERR%"
exit /b %ERRORLEVEL%

:explicar_error_mysql
echo.
echo [ERROR] MySQL no acepto la conexion como root. Esto respondio MySQL:
if exist "%MYSQL_ERR%" type "%MYSQL_ERR%"
echo.
findstr /c:"1045" "%MYSQL_ERR%" >nul 2>nul
if not errorlevel 1 echo   - Error 1045: la contrasena de root de ESTE equipo no es la que escribiste.
findstr /c:"2059" /c:"caching_sha2" /c:"plugin" "%MYSQL_ERR%" >nul 2>nul
if not errorlevel 1 echo   - Problema de autenticacion entre el cliente y el servidor MySQL (version distinta).
findstr /c:"2003" /c:"2005" /c:"2013" "%MYSQL_ERR%" >nul 2>nul
if not errorlevel 1 echo   - No se pudo conectar al puerto 3306: revisa que el servicio MySQL este encendido.
echo   Cliente usado: "%MYSQL_EXE%"
if defined MYSQL_PASS echo   (El script SI envio una contrasena a MySQL.)
if not defined MYSQL_PASS echo   (El script envio la contrasena VACIA: no se leyo lo que escribiste.)
exit /b 0

:escribir_db_local_properties
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$pw = $env:MYSQL_PASS; if ($null -eq $pw) { $pw = '' }; $esc = $pw.Replace('\', '\\'); $dest = Join-Path $env:CARMIR_ROOT 'Software_CARMIR\PanelAdministrativoCARMIR\db.local.properties'; $lines = @('# Configuracion local autogenerada para este equipo (no se sube a Git)', 'db.url=jdbc:mysql://localhost:3306/CARMIR_DB?useSSL=false&serverTimezone=America/Mexico_City&allowPublicKeyRetrieval=true', 'db.usuario=root', ('db.contrasena=' + $esc)); [IO.File]::WriteAllLines($dest, $lines, [Text.Encoding]::GetEncoding('ISO-8859-1'))" >nul 2>nul
exit /b 0

:puerto_activo
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "try { $t = New-Object Net.Sockets.TcpClient('127.0.0.1', %~1); $t.Close(); exit 0 } catch { try { $t2 = New-Object Net.Sockets.TcpClient('localhost', %~1); $t2.Close(); exit 0 } catch { exit 1 } }" >nul 2>nul
exit /b %ERRORLEVEL%

:falta_descomprimir
echo.
echo [ATENCION] No se encontro la carpeta "Software_CARMIR" junto a este archivo.
echo Si descargaste un archivo .ZIP de Google Drive, primero descomprimelo
echo completo (clic derecho -^> "Extraer todo...") y luego abre este archivo
echo desde la carpeta ya descomprimida.
echo.
pause
exit /b 1

REM === CARMIR_NODE_LAUNCHER_START ===
const path = require('path');
const net = require('net');
const readline = require('readline');
const childProcess = require('child_process');

const raiz = process.env.CARMIR_ROOT || process.cwd();
const dirApi = path.join(raiz, 'Software_CARMIR', 'PaginaWebCARMIR', 'ApiCARMIR');
const dirWeb = path.join(raiz, 'Software_CARMIR', 'PaginaWebCARMIR');
const dirPanel = path.join(raiz, 'Software_CARMIR', 'PanelAdministrativoCARMIR');

const procesos = {
  api: null,
  web: null,
  panel: null
};
let cerrando = false;
let resumenMostrado = false;

function limpiarAnsi(texto) {
  return texto.replace(/\x1B\[[0-9;]*[A-Za-z]/g, '');
}

function conectarLineas(flujo, etiqueta, filtro) {
  let pendiente = '';
  flujo.on('data', function (trozo) {
    pendiente = pendiente + trozo.toString('utf8');
    const partes = pendiente.split(/\r?\n/);
    pendiente = partes.pop();
    for (let i = 0; i < partes.length; i++) {
      const limpia = limpiarAnsi(partes[i]).trim();
      if (limpia.length > 0 && filtro(limpia)) {
        console.log('[' + etiqueta + '] ' + limpia);
      }
    }
  });
}

function filtroApi(linea) {
  return true;
}

function filtroWeb(linea) {
  if (linea.indexOf('Application bundle generation') >= 0) {
    return false;
  }
  if (linea.indexOf('Building...') >= 0 || linea.indexOf('Changes detected') >= 0) {
    return false;
  }
  if (linea.indexOf('Watch mode enabled') >= 0) {
    return false;
  }
  if (linea.indexOf('Local:') >= 0 || linea.indexOf('ERROR') >= 0 || linea.indexOf('Error') >= 0) {
    return true;
  }
  return false;
}

function filtroPanel(linea) {
  if (linea.indexOf('[INFO]') === 0 || linea.indexOf('Downloading') === 0 || linea.indexOf('Progress') === 0) {
    return false;
  }
  if (linea.indexOf('WARNING: A terminally deprecated') >= 0 || linea.indexOf('WARNING: sun.misc.Unsafe') >= 0) {
    return false;
  }
  if (linea.indexOf('WARNING: Please consider reporting') >= 0 || linea.indexOf('WARNING: JavaFX') >= 0) {
    return false;
  }
  return true;
}

function revisarPuerto(host, puerto) {
  return new Promise(function (resolver) {
    const socket = new net.Socket();
    let resuelto = false;
    function terminar(activo) {
      if (!resuelto) {
        resuelto = true;
        socket.destroy();
        resolver(activo);
      }
    }
    socket.setTimeout(800);
    socket.once('connect', function () {
      terminar(true);
    });
    socket.once('timeout', function () {
      terminar(false);
    });
    socket.once('error', function () {
      terminar(false);
    });
    socket.connect(puerto, host);
  });
}

async function puertoDisponible(puerto) {
  const v4 = await revisarPuerto('127.0.0.1', puerto);
  if (v4) {
    return true;
  }
  return revisarPuerto('localhost', puerto);
}

function esperar(ms) {
  return new Promise(function (resolver) {
    setTimeout(resolver, ms);
  });
}

function abrirNavegador(url) {
  try {
    const p = childProcess.spawn('cmd.exe', ['/d', '/s', '/c', '"start "" "' + url + '""'], {
      stdio: 'ignore',
      windowsHide: true,
      windowsVerbatimArguments: true
    });
    p.unref();
  } catch (e) {
    // Ignorar si no hay navegador predeterminado
  }
}

function matarArbol(proc) {
  if (proc && proc.pid) {
    try {
      childProcess.spawnSync('taskkill', ['/PID', String(proc.pid), '/T', '/F'], {
        stdio: 'ignore',
        windowsHide: true
      });
    } catch (e) {
      // Ignorar si ya habia terminado
    }
  }
}

function apagarTodo(codigo) {
  if (cerrando) {
    return;
  }
  cerrando = true;
  console.log('');
  console.log('[INFO] Cerrando Panel Administrativo, Pagina Web y API REST...');
  matarArbol(procesos.panel);
  matarArbol(procesos.web);
  matarArbol(procesos.api);
  console.log('[OK] Todos los servicios de CARMIR se detuvieron correctamente.');
  process.exit(codigo);
}

function iniciarApi() {
  const proc = childProcess.spawn(process.execPath, ['src/servidor.js'], {
    cwd: dirApi,
    env: process.env,
    stdio: ['ignore', 'pipe', 'pipe']
  });
  procesos.api = proc;
  conectarLineas(proc.stdout, 'API', filtroApi);
  conectarLineas(proc.stderr, 'API', filtroApi);
  proc.on('exit', function (codigo) {
    if (!cerrando && codigo !== 0) {
      console.log('[API] El servidor de la API se detuvo (codigo ' + codigo + ').');
    }
    procesos.api = null;
  });
}

function iniciarWeb() {
  const envWeb = Object.assign({}, process.env, {
    NG_CLI_ANALYTICS: 'false',
    NO_COLOR: '1'
  });
  const ngBin = path.join(dirWeb, 'node_modules', '@angular', 'cli', 'bin', 'ng.js');
  const proc = childProcess.spawn(process.execPath, [ngBin, 'serve'], {
    cwd: dirWeb,
    env: envWeb,
    stdio: ['ignore', 'pipe', 'pipe']
  });
  procesos.web = proc;
  conectarLineas(proc.stdout, 'WEB', filtroWeb);
  conectarLineas(proc.stderr, 'WEB', filtroWeb);
  proc.on('exit', function (codigo) {
    if (!cerrando && codigo !== 0) {
      console.log('[WEB] El servidor Angular se detuvo (codigo ' + codigo + ').');
    }
    procesos.web = null;
  });
}

function iniciarPanel() {
  if (procesos.panel !== null) {
    console.log('[PANEL] El Panel Administrativo ya se encuentra abierto.');
    return;
  }
  const proc = childProcess.spawn('cmd.exe', ['/d', '/s', '/c', '".\\mvnw.cmd -q javafx:run"'], {
    cwd: dirPanel,
    env: process.env,
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsVerbatimArguments: true
  });
  procesos.panel = proc;
  conectarLineas(proc.stdout, 'PANEL', filtroPanel);
  conectarLineas(proc.stderr, 'PANEL', filtroPanel);
  proc.on('exit', function () {
    procesos.panel = null;
    if (!cerrando && resumenMostrado) {
      console.log('[PANEL] Ventana del Panel cerrada. (Escribe P y Enter si deseas abrirlo de nuevo).');
    }
  });
}

function mostrarResumen() {
  resumenMostrado = true;
  console.log('');
  console.log('======================================================================');
  console.log('            SISTEMA CARMIR EN EJECUCION (CONSOLA UNICA)');
  console.log('======================================================================');
  console.log('  - Tienda en linea (Clientes):  http://localhost:4200');
  console.log('  - Estado de la API REST:       http://127.0.0.1:3000/api/salud');
  console.log('  - Panel Administrativo:        Abierto en su ventana de escritorio');
  console.log('----------------------------------------------------------------------');
  console.log('  -> Presiona [ENTER] aqui cuando termines para detener y cerrar todo.');
  console.log('     (Opcional: P + Enter reabre el Panel | W + Enter reabre la Web)');
  console.log('======================================================================');
  console.log('');
}

async function principal() {
  process.on('SIGINT', function () {
    apagarTodo(0);
  });
  process.on('SIGBREAK', function () {
    apagarTodo(0);
  });
  process.on('SIGHUP', function () {
    apagarTodo(0);
  });
  process.on('SIGTERM', function () {
    apagarTodo(0);
  });

  console.log('[1/3] Iniciando API REST en http://127.0.0.1:3000 ...');
  if (await puertoDisponible(3000)) {
    console.log('      La API ya estaba activa en el puerto 3000.');
  } else {
    iniciarApi();
    for (let i = 0; i < 20; i++) {
      if (await puertoDisponible(3000)) {
        break;
      }
      await esperar(500);
    }
  }

  console.log('[2/3] Iniciando Pagina Web Angular en http://localhost:4200 ...');
  const webYaEstaba = await puertoDisponible(4200);
  if (webYaEstaba) {
    console.log('      La Pagina Web ya estaba activa en el puerto 4200.');
  } else {
    iniciarWeb();
  }

  if (process.env.CARMIR_SIN_PANEL !== '1') {
    console.log('[3/3] Abriendo Panel Administrativo de Escritorio (JavaFX) ...');
    iniciarPanel();
  }

  if (!webYaEstaba) {
    console.log('      Compilando tienda web por primera vez, espera unos segundos...');
    for (let i = 0; i < 90; i++) {
      if (await puertoDisponible(4200)) {
        break;
      }
      await esperar(1000);
    }
  }

  if (process.env.CARMIR_SIN_NAVEGADOR !== '1') {
    abrirNavegador('http://localhost:4200');
  }

  mostrarResumen();

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: false
  });

  rl.on('line', function (entrada) {
    const cmd = entrada.trim().toUpperCase();
    if (cmd === 'P') {
      console.log('[INFO] Abriendo nuevamente el Panel Administrativo...');
      iniciarPanel();
    } else if (cmd === 'W') {
      console.log('[INFO] Abriendo http://localhost:4200 en tu navegador...');
      abrirNavegador('http://localhost:4200');
    } else {
      apagarTodo(0);
    }
  });
}

principal().catch(function (err) {
  console.error('[ERROR]', err);
  apagarTodo(1);
});
