@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title CARMIR - Iniciar Sistema Completo
cd /d "%~dp0"
set "CARMIR_ROOT=%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

echo ======================================================================
echo              INICIANDO SISTEMA CARMIR (3 COMPONENTES)
echo ======================================================================
echo.

REM --- 0. Verificar que la carpeta haya sido descomprimida ---
if not exist "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\PaginaWebCARMIR\package.json" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\PanelAdministrativoCARMIR\pom.xml" goto :falta_descomprimir

"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "try { Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned -Force -ErrorAction SilentlyContinue } catch {}; try { Get-ChildItem -LiteralPath $env:CARMIR_ROOT -ErrorAction SilentlyContinue | Unblock-File -ErrorAction SilentlyContinue } catch {}" >nul 2>nul

REM --- 1. Configurar PATH (Node.js 22+) y JAVA_HOME (JDK 25+) ---
call :configurar_entorno

REM --- 2. Encender MySQL (Servicio de Windows o XAMPP) si esta apagado ---
call :asegurar_mysql_activo

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
call node -e "const mysql=require('mysql2/promise'),cfg=require('./src/config/configuracion');(async()=>{const bd=cfg.baseDatos,c1=await mysql.createConnection({host:bd.host,port:bd.puerto,user:bd.usuario,password:bd.contrasena,database:bd.nombre,connectTimeout:5000});const [r]=await c1.execute(`SELECT COUNT(*) AS n FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND LOWER(TABLE_NAME)='postreopcion' AND LOWER(COLUMN_NAME)='numerocapa'`);await c1.end();if(!Number(r[0].n))process.exit(1);const c2=await mysql.createConnection({host:bd.host,port:bd.puerto,user:cfg.baseDatosEscritura.usuario,password:cfg.baseDatosEscritura.contrasena,database:bd.nombre,connectTimeout:5000});await c2.end();})().catch(()=>process.exit(1));" >nul 2>nul
if errorlevel 1 goto :instalar_desde_popd
popd
goto :arrancar_servicios

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

:arrancar_servicios
echo [1/3] Iniciando API REST (http://127.0.0.1:3000)...
call :puerto_activo 3000
if not errorlevel 1 goto :api_ya_activa
start "1. API CARMIR - Puerto 3000 (No cerrar)" /d "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR" cmd /k "npm.cmd start"
goto :iniciar_web

:api_ya_activa
echo       La API ya estaba encendida en el puerto 3000.

:iniciar_web
echo [2/3] Iniciando Pagina Web en Angular (http://localhost:4200)...
call :puerto_activo 4200
if not errorlevel 1 goto :web_ya_activa
start "2. Pagina Web CARMIR - Puerto 4200 (No cerrar)" /d "%~dp0Software_CARMIR\PaginaWebCARMIR" cmd /k "set NG_CLI_ANALYTICS=false&& npm.cmd start"
goto :iniciar_panel

:web_ya_activa
echo       La Pagina Web ya estaba encendida en el puerto 4200.

:iniciar_panel
echo [3/3] Abriendo Panel Administrativo CARMIR (Escritorio JavaFX)...
start "3. Panel Administrativo CARMIR (No cerrar)" /d "%~dp0Software_CARMIR\PanelAdministrativoCARMIR" cmd /k ".\mvnw.cmd javafx:run"

echo.
echo Esperando unos segundos a que cargue la tienda web para abrir tu navegador...
set /a INTENTOS=0
:esperar_angular
call :puerto_activo 4200
if not errorlevel 1 goto :abrir_navegador
set /a INTENTOS+=1
if %INTENTOS% GEQ 30 goto :abrir_navegador
ping 127.0.0.1 -n 3 >nul
goto :esperar_angular

:abrir_navegador
start "" "http://localhost:4200"

echo.
echo ======================================================================
echo                  SISTEMA CARMIR EN EJECUCION
echo ======================================================================
echo.
echo   - Tienda en linea (Clientes):  http://localhost:4200
echo   - Estado de la API:            http://127.0.0.1:3000/api/salud
echo   - Panel Administrativo:        Se abrira en su propia ventana
echo.
echo   CREDENCIALES DEL PANEL ADMINISTRATIVO:
echo     * Administradora (acceso total):   admin  /  admin
echo     * Acceso del Personal (enlace inferior):
echo         - maria / Maria2026   (Pedidos, agenda, insumos, recetas)
echo         - jose  / Jose2026    (Agenda, insumos, recetas)
echo         - lucia / Lucia2026   (Pedidos, pagos, finanzas)
echo         - pedro / Pedro2026   (Cuenta bloqueada para prueba)
echo.
echo   NOTA: No cierres las ventanas negras mientras uses el sistema.
echo ======================================================================
echo.
pause
exit /b 0

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
echo [INFO] Encendiendo MySQL de XAMPP...
start "MySQL CARMIR (XAMPP)" /min "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
ping 127.0.0.1 -n 6 >nul
"C:\xampp\mysql\bin\mysql.exe" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
exit /b 0

:arrancar_xampp_d
echo [INFO] Encendiendo MySQL de XAMPP...
start "MySQL CARMIR (XAMPP)" /min "D:\xampp\mysql\bin\mysqld.exe" --defaults-file="D:\xampp\mysql\bin\my.ini" --standalone
ping 127.0.0.1 -n 6 >nul
"D:\xampp\mysql\bin\mysql.exe" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
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
