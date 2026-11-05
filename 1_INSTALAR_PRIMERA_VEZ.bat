@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title CARMIR - Paso 1: Instalacion Inicial
cd /d "%~dp0"
set "CARMIR_ROOT=%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

echo ======================================================================
echo        SISTEMA CARMIR - CONFIGURACION E INSTALACION AUTOMATICA
echo ======================================================================
echo.

REM --- 0. Verificar que la carpeta haya sido descomprimida ---
if not exist "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\PaginaWebCARMIR\package.json" goto :falta_descomprimir
if not exist "%~dp0Software_CARMIR\PanelAdministrativoCARMIR\pom.xml" goto :falta_descomprimir

REM Desbloquear archivos descargados de Internet / Google Drive y permitir scripts locales
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "try { Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned -Force -ErrorAction SilentlyContinue } catch {}; try { Get-ChildItem -LiteralPath $env:CARMIR_ROOT -Recurse -ErrorAction SilentlyContinue | Unblock-File -ErrorAction SilentlyContinue } catch {}" >nul 2>nul

REM --- 1. Verificar o instalar Node.js (version 22 o superior) ---
call :buscar_node
if defined NODE_ENCONTRADO goto :node_listo

echo [AUTO-INSTALACION] Se requiere Node.js 22 LTS o superior.
echo Descargando e instalando Node.js LTS automaticamente desde Internet...
echo (Si Windows abre una ventana pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :falta_node
winget install --id OpenJS.NodeJS.LTS --exact --accept-package-agreements --accept-source-agreements
call :buscar_node
if not defined NODE_ENCONTRADO goto :falta_node

:node_listo
set "PATH=%NODE_ENCONTRADO%;%PATH%"
echo [OK] Node.js listo:
call "%NODE_ENCONTRADO%\node.exe" -v
echo.

REM --- 2. Detectar o instalar Java JDK 25 o superior (JAVA_HOME) ---
call :buscar_jdk
if defined JDK_ENCONTRADO goto :java_listo

echo [AUTO-INSTALACION] Descargando e instalando Java JDK 25 desde Internet...
echo (No borra ni afecta otras versiones de Java que ya tengas instaladas)
echo (Si Windows abre una ventana pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :falta_java
winget install --id Oracle.JDK.25 --exact --accept-package-agreements --accept-source-agreements
call :buscar_jdk
if not defined JDK_ENCONTRADO goto :falta_java

:java_listo
set "JAVA_HOME=%JDK_ENCONTRADO%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
setx JAVA_HOME "%JAVA_HOME%" >nul 2>nul
echo [OK] Java JDK configurado en: "%JAVA_HOME%"
echo.

REM --- 3. Localizar o instalar MySQL / MariaDB (MySQL Server o XAMPP) ---
call :buscar_mysql
if defined MYSQL_EXE goto :mysql_encontrado

echo [AUTO-INSTALACION] No se encontro MySQL ni XAMPP en este equipo.
echo Descargando e instalando XAMPP desde Internet...
echo (Si Windows abre una ventana pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :falta_mysql
winget install --id ApacheFriends.Xampp.8.2 --exact --accept-package-agreements --accept-source-agreements
call :buscar_mysql
if not defined MYSQL_EXE goto :falta_mysql

:mysql_encontrado
echo [OK] Cliente MySQL encontrado en: "%MYSQL_EXE%"

REM --- 4. Asegurar que el servidor MySQL (puerto 3306) este encendido ---
:revisar_puerto_mysql
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido

REM Intentar encender servicio de Windows MySQL / MariaDB si existe
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$svcs = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(MySQL|MariaDB)' -and $_.Status -ne 'Running' }; foreach ($s in $svcs) { try { Start-Service -Name $s.Name -ErrorAction Stop; break } catch { try { Start-Process sc.exe -ArgumentList 'start', $s.Name -Verb RunAs -Wait -WindowStyle Hidden; break } catch {} } }" >nul 2>nul
ping 127.0.0.1 -n 4 >nul
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido

REM Si no hay servicio o sigue apagado, intentar con XAMPP en C:\ o D:\
if exist "C:\xampp\mysql\bin\mysqld.exe" goto :iniciar_xampp_c
if exist "D:\xampp\mysql\bin\mysqld.exe" goto :iniciar_xampp_d
goto :pedir_encender_mysql

:iniciar_xampp_c
echo [INFO] Encendiendo MySQL de XAMPP (C:\xampp) automaticamente...
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'C:\xampp\mysql\bin\mysqld.exe' -ArgumentList '--defaults-file=C:\xampp\mysql\bin\my.ini','--standalone' -WindowStyle Hidden" >nul 2>nul
ping 127.0.0.1 -n 6 >nul
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido
goto :pedir_encender_mysql

:iniciar_xampp_d
echo [INFO] Encendiendo MySQL de XAMPP (D:\xampp) automaticamente...
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'D:\xampp\mysql\bin\mysqld.exe' -ArgumentList '--defaults-file=D:\xampp\mysql\bin\my.ini','--standalone' -WindowStyle Hidden" >nul 2>nul
ping 127.0.0.1 -n 6 >nul
call :puerto_activo 3306
if not errorlevel 1 goto :mysql_encendido

:pedir_encender_mysql
echo.
echo [ATENCION] El servidor MySQL todavia no responde en el puerto 3306.
echo Por favor inicia MySQL (en XAMPP Control Panel o en Servicios de Windows)
echo y luego presiona cualquier tecla aqui para continuar...
pause >nul
goto :revisar_puerto_mysql

:mysql_encendido
echo [OK] Servidor MySQL activo en el puerto 3306.
echo.

REM --- 5. Conectar como root (deteccion automatica o solicitud de contrasena) ---
set "MYSQL_CNF=%TEMP%\carmir_mysql_%RANDOM%_%RANDOM%.cnf"
set "MYSQL_PASS="

REM 5.1 Probar primero contrasena guardada localmente en este equipo (si existe)
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

REM --- 6. Configurar Base de Datos CARMIR_DB sin conflictos ---
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul

REM Verificar si CARMIR_DB ya existe y ya tiene productos registrados
set "TIENE_DATOS_PREVIOS=0"
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -N -B -e "SELECT 1 FROM CARMIR_DB.Postre LIMIT 1;" >nul 2>nul
if not errorlevel 1 set "TIENE_DATOS_PREVIOS=1"

if "%TIENE_DATOS_PREVIOS%"=="0" goto :instalar_bd_limpia
if defined LLAMADO_DESDE_INICIAR goto :actualizar_bd_existente

echo ======================================================================
echo   BASE DE DATOS EXISTENTE DETECTADA EN ESTE EQUIPO (CARMIR_DB)
echo ======================================================================
echo   [1] CONSERVAR tus datos actuales y solo actualizar estructura/capas (Recomendado)
echo   [2] REINSTALAR desde cero con los datos de prueba oficiales (borra datos previos)
echo.
set "OPCION_BD=1"
set /p "OPCION_BD=Elige 1 o 2 (presiona Enter para elegir 1): "
if "%OPCION_BD%"=="2" goto :instalar_bd_limpia

:actualizar_bd_existente
echo.
echo [1/3] Actualizando estructura de CARMIR_DB sin borrar tus datos...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\DBCarmir\ACTUALIZAR_SIN_BORRAR.sql" >nul
if errorlevel 1 goto :error_sql
goto :configurar_usuarios_api

:instalar_bd_limpia
echo [1/4] Creando estructura completa de la base de datos CARMIR_DB...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" >nul
if errorlevel 1 goto :error_sql

echo [2/4] Cargando catalogo, recetas, insumos, usuarios y pedidos de prueba...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\DBCarmir\POBLAR_BASE.sql" >nul
if errorlevel 1 goto :error_sql

:configurar_usuarios_api
echo [+] Configurando permisos de seguridad para la API web (carmir_web y carmir_pedidos)...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "FLUSH PRIVILEGES;" >nul 2>nul
if errorlevel 1 call :reparar_tablas_sistema_mysql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_lectura.sql" >nul
if errorlevel 1 goto :error_sql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" < "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_pedidos.sql" >nul
if errorlevel 1 goto :error_sql

REM Sincronizar contrasena de usuarios de la API y escribir archivo .env local
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -e "ALTER USER 'carmir_web'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_web'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_pedidos'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_pedidos'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026'; FLUSH PRIVILEGES;" >nul 2>nul

del /f /q "%MYSQL_CNF%" >nul 2>nul

REM Guardar configuracion local del Panel Administrativo (db.local.properties, ignorado por Git)
call :escribir_db_local_properties

REM Escribir archivo .env limpio para ApiCARMIR en este equipo
call :escribir_env_api

echo [OK] Base de datos y credenciales locales configuradas correctamente.

REM --- 7. Instalar paquetes de ApiCARMIR, PaginaWebCARMIR y compilar Panel ---
echo.
echo ======================================================================
echo   [1/3] Instalando librerias de ApiCARMIR (Node.js / Express)...
echo ======================================================================
pushd "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR"
call npm.cmd install --no-audit --no-fund
if errorlevel 1 goto :error_npm_popd
call npm.cmd run diagnostico
popd

echo.
echo ======================================================================
echo   [2/3] Instalando librerias de PaginaWebCARMIR (Angular)...
echo   (Esto puede tardar de 1 a 3 minutos la primera vez)
echo ======================================================================
pushd "%~dp0Software_CARMIR\PaginaWebCARMIR"
if exist ".angular" rmdir /s /q ".angular" >nul 2>nul
set "NG_CLI_ANALYTICS=false"
call npm.cmd install --no-audit --no-fund
if errorlevel 1 goto :error_npm_popd
popd

echo.
echo ======================================================================
echo   [3/3] Compilando PanelAdministrativoCARMIR (JavaFX + Maven)...
echo   (Descargando dependencias la primera vez, por favor espera...)
echo ======================================================================
pushd "%~dp0Software_CARMIR\PanelAdministrativoCARMIR"
call .\mvnw.cmd compile
if errorlevel 1 goto :error_maven_popd
popd

echo.
echo ======================================================================
echo   INSTALACION Y CONFIGURACION COMPLETADAS CON EXITO
echo ======================================================================
echo   Todo quedo listo en este equipo. Ahora solo haz doble clic en:
echo       2_INICIAR_SISTEMA.bat
echo ======================================================================
echo.
if not defined LLAMADO_DESDE_INICIAR pause
exit /b 0

REM ===================== SUBRUTINAS DE APOYO =====================

:buscar_node
set "NODE_ENCONTRADO="
set "NODE_TMP=%TEMP%\carmir_node_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @('C:\Program Files\nodejs', (Join-Path $env:LOCALAPPDATA 'Programs\nodejs')); $wg = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'; if (Test-Path -LiteralPath $wg) { Get-ChildItem -LiteralPath $wg -Directory -Filter 'OpenJS.NodeJS*' -ErrorAction SilentlyContinue | ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -Recurse -Filter 'node.exe' -ErrorAction SilentlyContinue | ForEach-Object { $dirs += $_.DirectoryName } } }; $cmd = Get-Command node -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path $cmd.Source -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp) { $dirs += $rp.Trim() } }; foreach ($d in $dirs) { if ($d) { $ne = Join-Path $d 'node.exe'; if (Test-Path -LiteralPath $ne) { $v = (& $ne -v 2>&1 | Out-String); if ($v -match 'v(\d+)' -and [int]$Matches[1] -ge 22) { [IO.File]::WriteAllText($env:NODE_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } } }" >nul 2>nul
if exist "%NODE_TMP%" set /p NODE_ENCONTRADO=<"%NODE_TMP%"
if exist "%NODE_TMP%" del /f /q "%NODE_TMP%" >nul 2>nul
exit /b 0

:buscar_jdk
set "JDK_ENCONTRADO="
set "JDK_TMP=%TEMP%\carmir_jdk_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @(); if ($env:JAVA_HOME) { $dirs += $env:JAVA_HOME }; $roots = @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft', 'C:\Program Files\BellSoft', 'C:\Program Files\Amazon Corretto', 'C:\Program Files\Zulu', (Join-Path $env:USERPROFILE '.jdks')); foreach ($r in $roots) { if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $dirs += $_.FullName } } }; $cmd = Get-Command javac -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path (Split-Path $cmd.Source -Parent) -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp -and (Test-Path -LiteralPath (Join-Path $rp.Trim() 'javac.exe'))) { $dirs += (Split-Path $rp.Trim() -Parent) } }; foreach ($d in $dirs) { $jc = Join-Path $d 'bin\javac.exe'; if (Test-Path -LiteralPath $jc) { $ver = (& $jc -version 2>&1 | Out-String); if ($ver -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 25) { [IO.File]::WriteAllText($env:JDK_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } }" >nul 2>nul
if exist "%JDK_TMP%" set /p JDK_ENCONTRADO=<"%JDK_TMP%"
if exist "%JDK_TMP%" del /f /q "%JDK_TMP%" >nul 2>nul
exit /b 0

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
pause
exit /b 1

:falta_node
if defined MYSQL_CNF if exist "%MYSQL_CNF%" del /f /q "%MYSQL_CNF%" >nul 2>nul
echo.
echo [FALTA INSTALAR NODE.JS]
echo No se pudo instalar Node.js automaticamente.
echo Descargalo de https://nodejs.org/ (version LTS) y vuelve a abrir este archivo.
echo.
pause
exit /b 1

:falta_java
if defined MYSQL_CNF if exist "%MYSQL_CNF%" del /f /q "%MYSQL_CNF%" >nul 2>nul
echo.
echo [FALTA INSTALAR JAVA JDK 25]
echo No se pudo instalar Java JDK 25 automaticamente.
echo Descargalo de https://www.oracle.com/java/technologies/downloads/#java25-windows
echo y vuelve a abrir este archivo.
echo.
pause
exit /b 1

:falta_mysql
if defined MYSQL_CNF if exist "%MYSQL_CNF%" del /f /q "%MYSQL_CNF%" >nul 2>nul
echo.
echo [FALTA INSTALAR XAMPP O MYSQL]
echo No se pudo instalar XAMPP automaticamente.
echo Descargalo de https://www.apachefriends.org/es/index.html y vuelve a abrir este archivo.
echo.
pause
exit /b 1

:error_sql
if defined MYSQL_CNF if exist "%MYSQL_CNF%" del /f /q "%MYSQL_CNF%" >nul 2>nul
echo.
echo [ERROR] Ocurrio un problema al configurar la base de datos CARMIR_DB en MySQL.
pause
exit /b 1

:error_npm_popd
popd
echo.
echo [ERROR] Ocurrio un problema al descargar las librerias con npm.
echo Verifica tu conexion a Internet e intenta de nuevo.
pause
exit /b 1

:error_maven_popd
popd
echo.
echo [ERROR] Ocurrio un problema al compilar el Panel Administrativo con Maven.
echo Verifica tu conexion a Internet y que tengas instalado JDK 25 o superior.
pause
exit /b 1
