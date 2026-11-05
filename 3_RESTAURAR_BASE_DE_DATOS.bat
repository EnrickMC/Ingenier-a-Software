@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title CARMIR - Restaurar Base de Datos de Fabrica
cd /d "%~dp0"
set "CARMIR_ROOT=%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

echo ======================================================================
echo       SISTEMA CARMIR - BORRAR Y RESTAURAR BASE DE DATOS (RESET)
echo ======================================================================
echo.

REM --- 0. Verificar que la carpeta haya sido descomprimida ---
if not exist "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\DBCarmir\POBLAR_BASE.sql" goto :falta_descomprimir

echo   ADVERTENCIA IMPORTANTE:
echo   ------------------------------------------------------------------
echo   Esta accion BORRARA por completo la base de datos CARMIR_DB actual
echo   (incluyendo cualquier pedido nuevo, producto o cambio que hayas
echo   hecho) y la dejara como recien instalada con los datos de ejemplo:
echo.
echo     - 7 categorias, 43 caracteristicas y mas de 175 opciones 3D
echo     - 47 reglas de exclusion de combinaciones incompatibles
echo     - 20 productos visibles en la tienda web
echo     - 41 insumos con lote inicial y recetas activas
echo     - Usuarios de prueba y pedidos de demostracion originales
echo   ------------------------------------------------------------------
echo.

if "%CARMIR_CONFIRMAR_SI%"=="1" goto :confirmado

set "CONFIRMACION="
set /p "CONFIRMACION=Estas seguro? Escribe SI para borrar y restaurar (o presiona Enter para cancelar): "
if defined CONFIRMACION set "CONFIRMACION=%CONFIRMACION: =%"
if /i "%CONFIRMACION%"=="SI" goto :confirmado

echo.
echo [CANCELADO] Operacion cancelada. No se borro ni modifico nada en tu base de datos.
echo.
if not "%CARMIR_NO_PAUSE%"=="1" pause
exit /b 0

:confirmado
echo.
echo [INFO] Confirmacion recibida. Preparando conexion con MySQL...

REM --- 1. Localizar cliente MySQL / MariaDB ---
call :buscar_mysql
if not defined MYSQL_EXE goto :falta_mysql

REM --- 2. Asegurar que el servidor MySQL (puerto 3306) este encendido ---
:revisar_puerto_mysql
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido

"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$svcs = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(MySQL|MariaDB)' -and $_.Status -ne 'Running' }; foreach ($s in $svcs) { try { Start-Service -Name $s.Name -ErrorAction Stop; break } catch { try { Start-Process sc.exe -ArgumentList 'start', $s.Name -Verb RunAs -Wait -WindowStyle Hidden; break } catch {} } }" >nul 2>nul
ping 127.0.0.1 -n 4 >nul
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido

if exist "C:\xampp\mysql\bin\mysqld.exe" goto :iniciar_xampp_c
if exist "D:\xampp\mysql\bin\mysqld.exe" goto :iniciar_xampp_d
goto :pedir_encender_mysql

:iniciar_xampp_c
echo [INFO] Encendiendo MySQL de XAMPP (C:\xampp) automaticamente...
start "MySQL CARMIR (XAMPP)" /min "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
ping 127.0.0.1 -n 6 >nul
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido
goto :pedir_encender_mysql

:iniciar_xampp_d
echo [INFO] Encendiendo MySQL de XAMPP (D:\xampp) automaticamente...
start "MySQL CARMIR (XAMPP)" /min "D:\xampp\mysql\bin\mysqld.exe" --defaults-file="D:\xampp\mysql\bin\my.ini" --standalone
ping 127.0.0.1 -n 6 >nul
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido

:pedir_encender_mysql
echo.
echo [ATENCION] El servidor MySQL todavia no responde en el puerto 3306.
echo Inicia MySQL (en XAMPP Control Panel o Servicios de Windows)
echo y presiona cualquier tecla aqui para continuar...
pause >nul
goto :revisar_puerto_mysql

:mysql_encendido
echo [OK] Servidor MySQL activo en el puerto 3306.

REM --- 3. Conectar como root (deteccion automatica o solicitud) ---
set "MYSQL_CNF=%TEMP%\carmir_reset_%RANDOM%_%RANDOM%.cnf"
set "MYSQL_PASS="

if not exist "%~dp0Software_CARMIR\PanelAdministrativoCARMIR\db.local.properties" goto :probar_claves_comunes
call :leer_pass_local
call :probar_login_root
if not errorlevel 1 goto :root_conectado

:probar_claves_comunes
set "MYSQL_PASS="
call :probar_login_root
if not errorlevel 1 goto :root_conectado

set "MYSQL_PASS=admin"
call :probar_login_root
if not errorlevel 1 goto :root_conectado

set "MYSQL_PASS=root"
call :probar_login_root
if not errorlevel 1 goto :root_conectado

set "MYSQL_PASS=1234"
call :probar_login_root
if not errorlevel 1 goto :root_conectado

set "MYSQL_PASS=123456"
call :probar_login_root
if not errorlevel 1 goto :root_conectado

:pedir_pass_mysql
set "MYSQL_PASS="
echo Tu instalacion de MySQL utiliza una contrasena personalizada para "root".
set /p "MYSQL_PASS=Escribe la contrasena de root de tu MySQL y presiona Enter: "
call :probar_login_root
if not errorlevel 1 goto :root_conectado
echo [ERROR] Contrasena incorrecta para root@127.0.0.1:3306. Intenta de nuevo.
echo.
goto :pedir_pass_mysql

:root_conectado
echo [OK] Conexion verificada con MySQL (usuario root).
echo.

REM --- 4. Borrar y restaurar CARMIR_DB desde cero ---
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul

echo [1/4] Recreando estructura limpia de CARMIR_DB (INSTALACION_COMPLETA.sql)...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" >nul
if errorlevel 1 goto :error_sql

echo [2/4] Cargando catalogo completo, exclusiones, recetas y datos iniciales (POBLAR_BASE.sql)...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\DBCarmir\POBLAR_BASE.sql" >nul
if errorlevel 1 goto :error_sql

echo [3/4] Restaurando permisos de los usuarios de la API web (carmir_web y carmir_pedidos)...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "FLUSH PRIVILEGES;" >nul 2>nul
if errorlevel 1 call :reparar_tablas_sistema_mysql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_lectura.sql" >nul
if errorlevel 1 goto :error_sql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_pedidos.sql" >nul
if errorlevel 1 goto :error_sql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "ALTER USER 'carmir_web'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_web'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_pedidos'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_pedidos'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026'; FLUSH PRIVILEGES;" >nul 2>nul

echo [4/4] Sincronizando credenciales locales y verificando restauracion...
call :escribir_db_local_properties
call :escribir_env_api

echo.
echo   Resumen de datos restaurados en CARMIR_DB:
echo   ------------------------------------------------------------------
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -N -B -e "USE CARMIR_DB; SELECT CONCAT('    Categorias:          ', (SELECT COUNT(*) FROM CategoriaPostre)); SELECT CONCAT('    Caracteristicas:     ', (SELECT COUNT(*) FROM Caracteristica)); SELECT CONCAT('    Opciones:            ', (SELECT COUNT(*) FROM OpcionCaracteristica)); SELECT CONCAT('    Exclusiones activas: ', (SELECT COUNT(*) FROM ExclusionOpciones WHERE EstadoExclusion = 'Activa')); SELECT CONCAT('    Productos en tienda: ', (SELECT COUNT(*) FROM Postre WHERE MostrarEnTienda = 1)); SELECT CONCAT('    Insumos / Recetas:   ', (SELECT COUNT(*) FROM Insumo), ' insumos / ', (SELECT COUNT(*) FROM Receta), ' recetas'); SELECT CONCAT('    Pedidos iniciales:   ', (SELECT COUNT(*) FROM Pedido));"
echo   ------------------------------------------------------------------

del /f /q "%MYSQL_CNF%" >nul 2>nul

echo.
echo ======================================================================
echo   BASE DE DATOS RESTAURADA CON EXITO A SU ESTADO ORIGINAL
echo ======================================================================
echo   Si tenias el sistema abierto, cierra y vuelve a abrir el Panel o
echo   recarga la pagina web en http://localhost:4200 para ver los datos.
echo ======================================================================
echo.
if not "%CARMIR_NO_PAUSE%"=="1" pause
exit /b 0

REM ===================== SUBRUTINAS DE APOYO =====================

:buscar_mysql
set "MYSQL_EXE="
set "MYSQL_TMP=%TEMP%\carmir_mysql_exe_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$cands = @(); foreach ($r in @('C:\Program Files\MySQL', 'C:\Program Files')) { if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Directory -Filter 'MySQL Server*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName 'bin\mysql.exe') }; Get-ChildItem -LiteralPath $r -Directory -Filter 'MariaDB*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName 'bin\mysql.exe') } } }; $cands += 'C:\xampp\mysql\bin\mysql.exe'; $cands += 'D:\xampp\mysql\bin\mysql.exe'; $cmd = Get-Command mysql -ErrorAction SilentlyContinue; if ($cmd) { $cands += $cmd.Source }; foreach ($c in $cands) { if ($c -and (Test-Path -LiteralPath $c)) { [IO.File]::WriteAllText($env:MYSQL_TMP, $c, (New-Object Text.UTF8Encoding($false))); break } }" >nul 2>nul
if exist "%MYSQL_TMP%" set /p MYSQL_EXE=<"%MYSQL_TMP%"
if exist "%MYSQL_TMP%" del /f /q "%MYSQL_TMP%" >nul 2>nul
exit /b 0

:puerto_activo
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "try { $t = New-Object Net.Sockets.TcpClient('127.0.0.1', %~1); $t.Close(); exit 0 } catch { exit 1 }" >nul 2>nul
exit /b %ERRORLEVEL%

:leer_pass_local
set "MYSQL_PASS="
set "PASS_TMP=%TEMP%\carmir_pass_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$p = Join-Path $env:CARMIR_ROOT 'Software_CARMIR\PanelAdministrativoCARMIR\db.local.properties'; foreach ($line in [IO.File]::ReadAllLines($p)) { if ($line -match '^db\.contrasena=(.*)$') { [IO.File]::WriteAllText($env:PASS_TMP, $Matches[1], [Text.Encoding]::ASCII); break } }" >nul 2>nul
if exist "%PASS_TMP%" set /p MYSQL_PASS=<"%PASS_TMP%"
if exist "%PASS_TMP%" del /f /q "%PASS_TMP%" >nul 2>nul
exit /b 0

:probar_login_root
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$pw = $env:MYSQL_PASS; if ($null -eq $pw) { $pw = '' }; $esc = $pw.Replace('\', '\\').Replace([char]34, '\' + [char]34); $lines = @('[client]', 'user=root', ('password=' + [char]34 + $esc + [char]34), 'host=127.0.0.1', 'port=3306', 'default-character-set=utf8mb4'); [IO.File]::WriteAllLines($env:MYSQL_CNF, $lines, [Text.Encoding]::ASCII)" >nul 2>nul
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "SELECT 1;" >nul 2>nul
exit /b %ERRORLEVEL%

:reparar_tablas_sistema_mysql
for %%F in ("%MYSQL_EXE%") do set "MYSQLCHECK_EXE=%%~dpFmysqlcheck.exe"
if exist "%MYSQLCHECK_EXE%" "%MYSQLCHECK_EXE%" "--defaults-extra-file=%MYSQL_CNF%" --auto-repair --databases mysql >nul 2>nul
exit /b 0

:escribir_db_local_properties
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$pw = $env:MYSQL_PASS; if ($null -eq $pw) { $pw = '' }; $esc = $pw.Replace('\', '\\'); $dest = Join-Path $env:CARMIR_ROOT 'Software_CARMIR\PanelAdministrativoCARMIR\db.local.properties'; $lines = @('# Configuracion local autogenerada para este equipo (no se sube a Git)', 'db.url=jdbc:mysql://localhost:3306/CARMIR_DB?useSSL=false&serverTimezone=America/Mexico_City&allowPublicKeyRetrieval=true', 'db.usuario=root', ('db.contrasena=' + $esc)); [IO.File]::WriteAllLines($dest, $lines, [Text.Encoding]::GetEncoding('ISO-8859-1'))" >nul 2>nul
exit /b 0

:escribir_env_api
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dest = Join-Path $env:CARMIR_ROOT 'Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\.env'; $lines = @('API_HOST=127.0.0.1', 'API_PUERTO=3000', 'CORS_ORIGENES=http://localhost:4200', 'LIMITE_PETICIONES_MINUTO=300', 'LIMITE_PEDIDOS_HORA=10', 'DB_HOST=127.0.0.1', 'DB_PUERTO=3306', 'DB_NOMBRE=CARMIR_DB', 'DB_USUARIO=carmir_web', 'DB_CONTRASENA=CambiaEstaContrasena2026', 'DB_CONEXIONES_MAXIMAS=10', 'DB_TIEMPO_ESPERA_MS=10000', 'DB_ESCRITURA_USUARIO=carmir_pedidos', 'DB_ESCRITURA_CONTRASENA=CambiaEstaContrasena2026'); [IO.File]::WriteAllLines($dest, $lines, [Text.Encoding]::ASCII)" >nul 2>nul
exit /b 0

:falta_descomprimir
echo.
echo [ATENCION] No se encontro la carpeta "Software_CARMIR" junto a este archivo.
echo Si descargaste un archivo .ZIP de Google Drive, primero descomprimelo
echo completo (clic derecho -^> "Extraer todo...") y luego abre este archivo
echo desde la carpeta ya descomprimida.
echo.
if not "%CARMIR_NO_PAUSE%"=="1" pause
exit /b 1

:falta_mysql
echo.
echo [FALTA MYSQL O XAMPP]
echo No se encontro un cliente MySQL instalado en este equipo.
echo Ejecuta primero 1_INSTALAR_PRIMERA_VEZ.bat para instalar todo automaticamente.
echo.
if not "%CARMIR_NO_PAUSE%"=="1" pause
exit /b 1

:error_sql
if defined MYSQL_CNF if exist "%MYSQL_CNF%" del /f /q "%MYSQL_CNF%" >nul 2>nul
echo.
echo [ERROR] Ocurrio un problema al restaurar la base de datos CARMIR_DB en MySQL.
if not "%CARMIR_NO_PAUSE%"=="1" pause
exit /b 1
