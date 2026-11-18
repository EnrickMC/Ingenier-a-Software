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

REM --- Elegir que se prepara en esta PC ---
REM 1 = sistema local (como siempre); 2 = solo el Panel conectado a AWS; 3 = ambos.
REM Si este archivo lo llama 2_INICIAR_SISTEMA.bat, siempre es la opcion 1.
set "MODO_INSTALACION=1"
if defined LLAMADO_DESDE_INICIAR goto :modo_elegido
echo Que quieres preparar en esta PC?
echo   [1] Sistema local completo: MySQL, API, pagina web y Panel en esta PC
echo   [2] Solo el Panel Administrativo conectado a AWS - no instala MySQL ni Node.js
echo   [3] Ambos: el sistema local y tambien la conexion del Panel con AWS
echo.
set /p "MODO_INSTALACION=Elige 1, 2 o 3 (presiona Enter para elegir 1): "
if "%MODO_INSTALACION%"=="2" goto :modo_elegido
if "%MODO_INSTALACION%"=="3" goto :modo_elegido
set "MODO_INSTALACION=1"
:modo_elegido
echo.
REM Con la opcion 2 no se necesita Node.js: se salta directo a Java
if "%MODO_INSTALACION%"=="2" goto :paso_java

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

:paso_java
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

REM Con la opcion 2 no se instala MySQL, la base local ni las librerias de la web
if "%MODO_INSTALACION%"=="2" goto :paso_aws

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

REM --- 5. Conectar como root (se pide la contrasena; Enter = sin contrasena) ---
set "MYSQL_CNF=%TEMP%\carmir_mysql_%RANDOM%_%RANDOM%.cnf"
set "MYSQL_PASS="
set "MYSQL_ERR=%TEMP%\carmir_mysql_err_%RANDOM%_%RANDOM%.txt"

REM Si 2_INICIAR_SISTEMA.bat ya pidio y verifico la contrasena, usarla sin volver a preguntar
if not defined CARMIR_ROOT_VERIFICADO goto :pedir_pass_mysql
set "MYSQL_PASS=%CARMIR_ROOT_PASS%"
call :probar_login_root
if not errorlevel 1 goto :root_conectado

REM Siempre se pregunta la contrasena de root (cada equipo puede tener una distinta o ninguna)
:pedir_pass_mysql
set "MYSQL_PASS="
echo Escribe la contrasena del usuario "root" de tu MySQL.
echo Si tu MySQL no tiene contrasena, solo presiona Enter.
set /p "MYSQL_PASS=Contrasena de root: "
call :probar_login_root
if not errorlevel 1 goto :root_conectado
call :explicar_error_mysql
echo.
goto :pedir_pass_mysql

:root_conectado
echo [OK] Conexion verificada con MySQL (usuario root).
echo.

REM --- 6. Configurar Base de Datos CARMIR_DB sin conflictos ---
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul

REM Verificar si CARMIR_DB ya existe y ya tiene productos registrados
set "TIENE_DATOS_PREVIOS=0"
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" -N -B -e "SELECT 1 FROM CARMIR_DB.Postre LIMIT 1;" >nul 2>nul
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
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" < "%~dp0Software_CARMIR\DBCarmir\ACTUALIZAR_SIN_BORRAR.sql" >nul
if errorlevel 1 goto :error_sql
goto :configurar_usuarios_api

:instalar_bd_limpia
echo [1/4] Creando estructura completa de la base de datos CARMIR_DB...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" < "%~dp0Software_CARMIR\DBCarmir\INSTALACION_COMPLETA.sql" >nul
if errorlevel 1 goto :error_sql

echo [2/4] Cargando catalogo, recetas, insumos, usuarios y pedidos de prueba...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" < "%~dp0Software_CARMIR\DBCarmir\POBLAR_BASE.sql" >nul
if errorlevel 1 goto :error_sql

:configurar_usuarios_api
echo [+] Configurando permisos de seguridad para la API web (carmir_web y carmir_pedidos)...
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" -e "FLUSH PRIVILEGES;" >nul 2>nul
if errorlevel 1 call :reparar_tablas_sistema_mysql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" < "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_lectura.sql" >nul
if errorlevel 1 goto :error_sql

"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" < "%~dp0Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_pedidos.sql" >nul
if errorlevel 1 goto :error_sql

REM Sincronizar contrasena de usuarios de la API y escribir archivo .env local
"%MYSQL_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" -e "ALTER USER 'carmir_web'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_web'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_pedidos'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026'; ALTER USER 'carmir_pedidos'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026'; FLUSH PRIVILEGES;" >nul 2>nul

del /f /q "%MYSQL_CNF%" >nul 2>nul
if exist "%MYSQL_ERR%" del /f /q "%MYSQL_ERR%" >nul 2>nul

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

REM --- 8. Conexion con AWS (opciones 2 y 3): AWS CLI v2, llaves y datos de conexion ---
:paso_aws
if "%MODO_INSTALACION%"=="1" goto :compilar_panel
call :configurar_aws
if errorlevel 1 goto :error_aws

:compilar_panel
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
if not "%MODO_INSTALACION%"=="2" echo       2_INICIAR_SISTEMA.bat     - sistema local en esta PC
if not "%MODO_INSTALACION%"=="1" echo       INICIAR_CARMIR_AWS.bat    - Panel conectado a la base de AWS
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
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$cands = @(); Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(MySQL|MariaDB)' -and $_.State -eq 'Running' } | ForEach-Object { if ($_.PathName -match '(?i)^\W?(.+?\\bin)\\') { $cands += (Join-Path $Matches[1] 'mysql.exe') } }; foreach ($r in @('C:\Program Files\MySQL', 'C:\Program Files')) { if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Directory -Filter 'MySQL Server*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName 'bin\mysql.exe') }; Get-ChildItem -LiteralPath $r -Directory -Filter 'MariaDB*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName 'bin\mysql.exe') } } }; $cands += 'C:\xampp\mysql\bin\mysql.exe'; $cands += 'D:\xampp\mysql\bin\mysql.exe'; $cmd = Get-Command mysql -ErrorAction SilentlyContinue; if ($cmd) { $cands += $cmd.Source }; foreach ($c in $cands) { if ($c -and (Test-Path -LiteralPath $c)) { [IO.File]::WriteAllText($env:MYSQL_TMP, $c, (New-Object Text.UTF8Encoding($false))); break } }" >nul 2>nul
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

:reparar_tablas_sistema_mysql
for %%F in ("%MYSQL_EXE%") do set "MYSQLCHECK_EXE=%%~dpFmysqlcheck.exe"
if exist "%MYSQLCHECK_EXE%" "%MYSQLCHECK_EXE%" "--defaults-extra-file=%MYSQL_CNF%" -u root "--password=%MYSQL_PASS%" --auto-repair --databases mysql >nul 2>nul
exit /b 0

:escribir_db_local_properties
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$pw = $env:MYSQL_PASS; if ($null -eq $pw) { $pw = '' }; $esc = $pw.Replace('\', '\\'); $dest = Join-Path $env:CARMIR_ROOT 'Software_CARMIR\PanelAdministrativoCARMIR\db.local.properties'; $lines = @('# Configuracion local autogenerada para este equipo (no se sube a Git)', 'db.url=jdbc:mysql://localhost:3306/CARMIR_DB?useSSL=false&serverTimezone=America/Mexico_City&allowPublicKeyRetrieval=true', 'db.usuario=root', ('db.contrasena=' + $esc)); [IO.File]::WriteAllLines($dest, $lines, [Text.Encoding]::GetEncoding('ISO-8859-1'))" >nul 2>nul
exit /b 0

:escribir_env_api
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dest = Join-Path $env:CARMIR_ROOT 'Software_CARMIR\PaginaWebCARMIR\ApiCARMIR\.env'; $lines = @('API_HOST=127.0.0.1', 'API_PUERTO=3000', 'CORS_ORIGENES=http://localhost:4200', 'LIMITE_PETICIONES_MINUTO=300', 'LIMITE_PEDIDOS_HORA=10', 'DB_HOST=127.0.0.1', 'DB_PUERTO=3306', 'DB_NOMBRE=CARMIR_DB', 'DB_USUARIO=carmir_web', 'DB_CONTRASENA=CambiaEstaContrasena2026', 'DB_CONEXIONES_MAXIMAS=10', 'DB_TIEMPO_ESPERA_MS=10000', 'DB_ESCRITURA_USUARIO=carmir_pedidos', 'DB_ESCRITURA_CONTRASENA=CambiaEstaContrasena2026'); [IO.File]::WriteAllLines($dest, $lines, [Text.Encoding]::ASCII)" >nul 2>nul
exit /b 0

REM ===================== CONEXION CON AWS (opciones 2 y 3) =====================
REM Nada de AWS queda fijo en el programa: todo se pregunta aqui y se guarda solo en esta PC.
REM  - Llaves de acceso (Access Key ID y Secret): perfil de AWS CLI, carpeta .aws del usuario.
REM  - Datos de conexion: %USERPROFILE%\.carmir\aws.properties (lo leen el Panel e INICIAR_CARMIR_AWS.bat).
REM Si se vuelve a ejecutar, Enter conserva cada valor que ya estaba guardado.

:configurar_aws
echo.
echo ======================================================================
echo   CONEXION DEL PANEL CON AWS
echo ======================================================================
echo   Se instalara AWS CLI v2 y se pediran los datos de la cuenta de AWS.
echo   Todo se guarda solo en esta PC, fuera de la carpeta del proyecto:
echo     - Llaves de acceso:   perfil de AWS CLI, carpeta .aws de tu usuario
echo     - Datos de conexion:  %USERPROFILE%\.carmir\aws.properties
echo   Si ya habias configurado esta PC, presiona Enter para conservar cada valor.
echo ======================================================================
echo.

REM Leer lo que ya estaba guardado para ofrecerlo como valor actual
call :leer_aws_properties
if not defined CARMIR_AWS_PERFIL set "CARMIR_AWS_PERFIL=carmir-panel"
if not defined CARMIR_AWS_PUERTO_RDS set "CARMIR_AWS_PUERTO_RDS=3306"
if not defined CARMIR_AWS_PUERTO_LOCAL set "CARMIR_AWS_PUERTO_LOCAL=3307"
if not defined CARMIR_AWS_BD_NOMBRE set "CARMIR_AWS_BD_NOMBRE=CARMIR_DB"
if not defined CARMIR_AWS_BD_USUARIO set "CARMIR_AWS_BD_USUARIO=carmir_panel"

REM --- A. AWS CLI v2 ---
call :buscar_aws_cli
if defined AWS_CLI_CARPETA goto :aws_cli_listo
echo [AUTO-INSTALACION] Descargando e instalando AWS CLI v2 desde Internet...
echo (Si Windows abre una ventana pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :aws_cli_msi
winget install --id Amazon.AWSCLI --exact --accept-package-agreements --accept-source-agreements
call :buscar_aws_cli
if defined AWS_CLI_CARPETA goto :aws_cli_listo
:aws_cli_msi
echo Usando el instalador oficial de Amazon (AWSCLIV2.msi)...
start "" /wait msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi /passive /norestart
call :buscar_aws_cli
if not defined AWS_CLI_CARPETA goto :falta_aws_cli
:aws_cli_listo
REM Agregar AWS CLI a ESTA ventana (la ventana abierta no ve el PATH nuevo de Windows hasta reiniciarla)
set "PATH=%AWS_CLI_CARPETA%;%PATH%"
set "AWS_EXE=%AWS_CLI_CARPETA%\aws.exe"
echo [OK] AWS CLI listo en esta sesion:
"%AWS_EXE%" --version
echo.

REM --- B. Session Manager Plugin (abre el tunel seguro hacia la base de datos) ---
call :buscar_plugin_ssm
if defined AWS_PLUGIN_CARPETA goto :plugin_ssm_listo
echo [AUTO-INSTALACION] Descargando e instalando Session Manager Plugin de AWS...
echo (Si Windows abre una ventana pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :plugin_ssm_descarga
winget install --id Amazon.SessionManagerPlugin --exact --accept-package-agreements --accept-source-agreements
call :buscar_plugin_ssm
if defined AWS_PLUGIN_CARPETA goto :plugin_ssm_listo
:plugin_ssm_descarga
echo Usando el instalador oficial de Amazon (SessionManagerPluginSetup.exe)...
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$f = Join-Path $env:TEMP 'SessionManagerPluginSetup.exe'; try { Invoke-WebRequest -UseBasicParsing -Uri 'https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe' -OutFile $f; Start-Process -FilePath $f -Wait } catch { Write-Host $_.Exception.Message; exit 1 }"
call :buscar_plugin_ssm
if not defined AWS_PLUGIN_CARPETA goto :falta_plugin_ssm
:plugin_ssm_listo
set "PATH=%AWS_PLUGIN_CARPETA%;%PATH%"
echo [OK] Session Manager Plugin listo en esta sesion.
echo.

REM --- C. Perfil, region y llaves de acceso del usuario IAM del Panel ---
echo --- Cuenta de AWS ---
call :preguntar_aws CARMIR_AWS_PERFIL "Nombre del perfil de AWS CLI para el Panel"
call :preguntar_aws CARMIR_AWS_REGION "Region de AWS donde esta CARMIR, por ejemplo us-east-2"

:reintentar_llaves_aws
call :pedir_llaves_aws
if errorlevel 1 exit /b 1
"%AWS_EXE%" configure set region "%CARMIR_AWS_REGION%" --profile "%CARMIR_AWS_PERFIL%"
"%AWS_EXE%" configure set output json --profile "%CARMIR_AWS_PERFIL%"

echo Verificando las llaves con AWS...
"%AWS_EXE%" sts get-caller-identity --profile "%CARMIR_AWS_PERFIL%" --region "%CARMIR_AWS_REGION%" --query Arn --output text
if not errorlevel 1 goto :llaves_aws_correctas
echo.
echo [ATENCION] AWS no acepto las llaves o la region (arriba esta el mensaje de AWS).
set "REINTENTAR_AWS=1"
set /p "REINTENTAR_AWS=[1] Volver a escribirlas   [2] Continuar de todos modos  (Enter = 1): "
if not "%REINTENTAR_AWS%"=="2" goto :reintentar_llaves_aws
goto :datos_conexion_aws
:llaves_aws_correctas
echo [OK] Llaves de AWS correctas (arriba se ve el usuario IAM que las usa).
echo.

REM --- D. Datos de la base de datos (Amazon RDS) y de las imagenes (S3 y CloudFront) ---
:datos_conexion_aws
echo --- Base de datos en AWS (Amazon RDS, por tunel de Systems Manager) ---
call :preguntar_aws CARMIR_AWS_INSTANCIA "ID de la instancia EC2 que hace de puente, i-..."
call :preguntar_aws CARMIR_AWS_SERVIDOR_RDS "Punto de enlace - endpoint - de la base RDS"
call :preguntar_aws CARMIR_AWS_PUERTO_RDS "Puerto de la base RDS"
call :preguntar_aws CARMIR_AWS_PUERTO_LOCAL "Puerto del tunel en esta PC"
call :preguntar_aws CARMIR_AWS_BD_NOMBRE "Nombre de la base de datos"
call :preguntar_aws CARMIR_AWS_BD_USUARIO "Usuario MySQL del Panel"
echo.
echo --- Fotos del catalogo (Amazon S3 y CloudFront) ---
call :preguntar_aws CARMIR_AWS_BUCKET "Bucket de S3 donde se suben las fotos"
call :preguntar_aws CARMIR_AWS_DOMINIO "Dominio de CloudFront, por ejemplo https://dxxxx.cloudfront.net"
echo.

REM --- E. Guardar aws.properties (aqui se pide la contrasena de la base, sin mostrarla) ---
call :escribir_aws_properties
if errorlevel 1 goto :falta_escribir_aws
echo [OK] Datos guardados en "%USERPROFILE%\.carmir\aws.properties"
echo.

REM --- F. Llave que cifra las contrasenas de WhatsApp y correo (debe ser la misma en todas las PC) ---
if exist "%USERPROFILE%\.carmir\clave-secretos.key" goto :clave_secretos_lista
echo --- Llave de cifrado de notificaciones ---
echo   El archivo clave-secretos.key cifra las contrasenas de WhatsApp y correo que el Panel
echo   guarda en la base. Todas las PC que usan la base de AWS deben tener LA MISMA llave.
echo   Si otra PC ya la tiene, esta en C:\Users\SU_USUARIO\.carmir\clave-secretos.key de esa PC.
echo   Si esta es la primera PC, solo presiona Enter: el Panel creara una nueva.
set "RUTA_CLAVE_SECRETOS="
set /p "RUTA_CLAVE_SECRETOS=Ruta del archivo clave-secretos.key a copiar (Enter = omitir): "
if not defined RUTA_CLAVE_SECRETOS goto :clave_secretos_lista
set "RUTA_CLAVE_SECRETOS=%RUTA_CLAVE_SECRETOS:"=%"
if not exist "%RUTA_CLAVE_SECRETOS%" echo [ATENCION] No se encontro ese archivo; se omite este paso.
if not exist "%RUTA_CLAVE_SECRETOS%" goto :clave_secretos_lista
copy /y "%RUTA_CLAVE_SECRETOS%" "%USERPROFILE%\.carmir\clave-secretos.key" >nul
if not errorlevel 1 echo [OK] Llave copiada a "%USERPROFILE%\.carmir\clave-secretos.key"
:clave_secretos_lista
echo.
exit /b 0

:pedir_llaves_aws
REM Access Key ID (se ve) y Secret Access Key (no se ve). Se guardan con "aws configure set".
set "AWS_LLAVE_ACTUAL="
set "AWS_LLAVE_NUEVA="
set "AWS_SECRETA_OBLIGATORIA=0"
set "AWS_LLAVE_TMP=%TEMP%\carmir_llave_%RANDOM%_%RANDOM%.txt"
"%AWS_EXE%" configure get aws_access_key_id --profile "%CARMIR_AWS_PERFIL%" > "%AWS_LLAVE_TMP%" 2>nul
if exist "%AWS_LLAVE_TMP%" set /p AWS_LLAVE_ACTUAL=<"%AWS_LLAVE_TMP%"
if exist "%AWS_LLAVE_TMP%" del /f /q "%AWS_LLAVE_TMP%" >nul 2>nul
REM Mostrar la llave actual solo con los primeros y ultimos 4 caracteres
if defined AWS_LLAVE_ACTUAL "%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$k = $env:AWS_LLAVE_ACTUAL.Trim(); if ($k.Length -gt 8) { $k = $k.Substring(0, 4) + '********' + $k.Substring($k.Length - 4) } else { $k = '********' }; Write-Host ('  Llave guardada en el perfil: ' + $k)"
:pedir_llave_id
if defined AWS_LLAVE_ACTUAL set /p "AWS_LLAVE_NUEVA=Access Key ID del usuario IAM del Panel (Enter = conservar la guardada): "
if not defined AWS_LLAVE_ACTUAL set /p "AWS_LLAVE_NUEVA=Access Key ID del usuario IAM del Panel: "
if defined AWS_LLAVE_NUEVA goto :guardar_llave_id
if defined AWS_LLAVE_ACTUAL goto :pedir_llave_secreta
echo   Este dato es obligatorio.
goto :pedir_llave_id
:guardar_llave_id
REM Una llave nueva siempre necesita su Secret Access Key nueva
set "AWS_SECRETA_OBLIGATORIA=1"
"%AWS_EXE%" configure set aws_access_key_id "%AWS_LLAVE_NUEVA%" --profile "%CARMIR_AWS_PERFIL%"
if errorlevel 1 exit /b 1
:pedir_llave_secreta
REM PowerShell la pide oculta y la guarda directo en el perfil: no pasa por variables de esta ventana
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$aws = $env:AWS_EXE; $perfil = $env:CARMIR_AWS_PERFIL; $obligatoria = ($env:AWS_SECRETA_OBLIGATORIA -eq '1'); if (-not $obligatoria) { $actual = & $aws configure get aws_secret_access_key --profile $perfil 2>$null; if (-not $actual) { $obligatoria = $true } }; while ($true) { if ($obligatoria) { $txt = 'Secret Access Key (no se muestra al escribir)' } else { $txt = 'Secret Access Key (no se muestra; Enter = conservar la guardada)' }; $s = Read-Host -Prompt $txt -AsSecureString; $p = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)); $p = $p.Trim(); if ($p.Length -gt 0) { & $aws configure set aws_secret_access_key $p --profile $perfil; exit $LASTEXITCODE }; if (-not $obligatoria) { exit 0 }; Write-Host '  Este dato es obligatorio.' }"
if errorlevel 1 exit /b 1
exit /b 0

:preguntar_aws
REM %1 = variable donde se guarda la respuesta, %2 = pregunta. Enter conserva el valor actual.
set "VALOR_NUEVO="
call set "VALOR_ACTUAL=%%%~1%%"
if defined VALOR_ACTUAL set /p "VALOR_NUEVO=%~2 [%VALOR_ACTUAL%]: "
if not defined VALOR_ACTUAL set /p "VALOR_NUEVO=%~2: "
if defined VALOR_NUEVO set "%~1=%VALOR_NUEVO%"
call set "VALOR_ACTUAL=%%%~1%%"
if defined VALOR_ACTUAL exit /b 0
echo   Este dato es obligatorio.
goto :preguntar_aws

:leer_aws_properties
REM Pasa los valores de aws.properties a variables CARMIR_AWS_* (la contrasena de la base NO se lee)
set "AWS_LEER_TMP=%TEMP%\carmir_aws_%RANDOM%_%RANDOM%.cmd"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$q = [string][char]34; $pc = [string][char]37; $mapa = [ordered]@{ 'aws.perfil' = 'CARMIR_AWS_PERFIL'; 'aws.region' = 'CARMIR_AWS_REGION'; 'aws.ec2.instancia' = 'CARMIR_AWS_INSTANCIA'; 'aws.rds.servidor' = 'CARMIR_AWS_SERVIDOR_RDS'; 'aws.rds.puerto' = 'CARMIR_AWS_PUERTO_RDS'; 'aws.tunel.puertoLocal' = 'CARMIR_AWS_PUERTO_LOCAL'; 'aws.bd.nombre' = 'CARMIR_AWS_BD_NOMBRE'; 'aws.bd.usuario' = 'CARMIR_AWS_BD_USUARIO'; 'aws.s3.bucketImagenes' = 'CARMIR_AWS_BUCKET'; 'aws.imagenes.dominio' = 'CARMIR_AWS_DOMINIO'; 'aws.cli.carpeta' = 'CARMIR_AWS_CLI_CARPETA'; 'aws.ssm.pluginCarpeta' = 'CARMIR_AWS_PLUGIN_CARPETA' }; $salida = @(); foreach ($v in $mapa.Values) { $salida += ('set ' + $q + $v + '=' + $q) }; $src = Join-Path $env:USERPROFILE '.carmir\aws.properties'; if (Test-Path -LiteralPath $src) { foreach ($l in [IO.File]::ReadAllLines($src)) { if ($l -match '^\s*([A-Za-z0-9_.]+)\s*[=:]\s*(.*)$') { $k = $Matches[1]; if ($mapa.Contains($k)) { $v = $Matches[2].Trim().Replace('\\', '\').Replace($q, '').Replace($pc, $pc + $pc); $salida += ('set ' + $q + $mapa[$k] + '=' + $v + $q) } } } }; [IO.File]::WriteAllLines($env:AWS_LEER_TMP, $salida, (New-Object Text.UTF8Encoding($false)))" >nul 2>nul
if exist "%AWS_LEER_TMP%" call "%AWS_LEER_TMP%"
if exist "%AWS_LEER_TMP%" del /f /q "%AWS_LEER_TMP%" >nul 2>nul
exit /b 0

:escribir_aws_properties
REM Escribe aws.properties en UTF-8. Las diagonales \ se escriben dobles porque asi lo pide Java.
REM La contrasena de la base se pide aqui mismo, oculta; Enter conserva la que ya estaba guardada.
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dir = Join-Path $env:USERPROFILE '.carmir'; if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }; $dest = Join-Path $dir 'aws.properties'; $anterior = ''; if (Test-Path -LiteralPath $dest) { foreach ($l in [IO.File]::ReadAllLines($dest)) { if ($l -match '^aws\.bd\.contrasena=(.*)$') { $anterior = $Matches[1] } } }; $pw = ''; while ($pw.Length -eq 0) { if ($anterior.Length -gt 0) { $txt = 'Contrasena del usuario MySQL del Panel (no se muestra; Enter = conservar la guardada)' } else { $txt = 'Contrasena del usuario MySQL del Panel (no se muestra al escribir)' }; $s = Read-Host -Prompt $txt -AsSecureString; $p = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)); if ($p.Length -gt 0) { $pw = $p.Replace('\', '\\') } elseif ($anterior.Length -gt 0) { $pw = $anterior } else { Write-Host '  Este dato es obligatorio.' } }; function V($nombre) { $v = [Environment]::GetEnvironmentVariable($nombre); if ($null -eq $v) { $v = '' }; return $v.Trim().Replace('\', '\\') }; $lineas = @('# Configuracion de AWS de esta PC para el Panel Administrativo CARMIR.', '# La escribe 1_INSTALAR_PRIMERA_VEZ.bat (opcion 2 o 3); para cambiarla vuelve a ejecutarlo.', '# NO se sube a Git. Las llaves de acceso de AWS no estan aqui: estan en el perfil de AWS CLI.', ('aws.perfil=' + (V 'CARMIR_AWS_PERFIL')), ('aws.region=' + (V 'CARMIR_AWS_REGION')), ('aws.ec2.instancia=' + (V 'CARMIR_AWS_INSTANCIA')), ('aws.rds.servidor=' + (V 'CARMIR_AWS_SERVIDOR_RDS')), ('aws.rds.puerto=' + (V 'CARMIR_AWS_PUERTO_RDS')), ('aws.tunel.puertoLocal=' + (V 'CARMIR_AWS_PUERTO_LOCAL')), ('aws.bd.nombre=' + (V 'CARMIR_AWS_BD_NOMBRE')), ('aws.bd.usuario=' + (V 'CARMIR_AWS_BD_USUARIO')), ('aws.bd.contrasena=' + $pw), ('aws.s3.bucketImagenes=' + (V 'CARMIR_AWS_BUCKET')), ('aws.imagenes.dominio=' + (V 'CARMIR_AWS_DOMINIO')), ('aws.cli.carpeta=' + (V 'AWS_CLI_CARPETA')), ('aws.ssm.pluginCarpeta=' + (V 'AWS_PLUGIN_CARPETA'))); try { [IO.File]::WriteAllLines($dest, $lineas, (New-Object Text.UTF8Encoding($false))); exit 0 } catch { Write-Host $_.Exception.Message; exit 1 }"
exit /b %ERRORLEVEL%

:buscar_aws_cli
REM Busca aws.exe version 2: carpeta guardada, Program Files, PATH de esta ventana y PATH de Windows
set "AWS_CLI_CARPETA="
set "AWSCLI_TMP=%TEMP%\carmir_awscli_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @(); if ($env:CARMIR_AWS_CLI_CARPETA) { $dirs += $env:CARMIR_AWS_CLI_CARPETA }; $dirs += (Join-Path $env:ProgramFiles 'Amazon\AWSCLIV2'); $cmd = Get-Command aws.exe -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path $cmd.Source -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp) { $dirs += $rp.Trim() } }; foreach ($d in $dirs) { if ($d) { $ae = Join-Path $d 'aws.exe'; if (Test-Path -LiteralPath $ae) { $v = (& $ae --version 2>&1 | Out-String); if ($v -match 'aws-cli/2\.') { [IO.File]::WriteAllText($env:AWSCLI_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } } }" >nul 2>nul
if exist "%AWSCLI_TMP%" set /p AWS_CLI_CARPETA=<"%AWSCLI_TMP%"
if exist "%AWSCLI_TMP%" del /f /q "%AWSCLI_TMP%" >nul 2>nul
exit /b 0

:buscar_plugin_ssm
REM Busca session-manager-plugin.exe en la carpeta guardada, Program Files y el PATH
set "AWS_PLUGIN_CARPETA="
set "PLUGIN_TMP=%TEMP%\carmir_plugin_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @(); if ($env:CARMIR_AWS_PLUGIN_CARPETA) { $dirs += $env:CARMIR_AWS_PLUGIN_CARPETA }; $dirs += (Join-Path $env:ProgramFiles 'Amazon\SessionManagerPlugin\bin'); if (${env:ProgramFiles(x86)}) { $dirs += (Join-Path ${env:ProgramFiles(x86)} 'Amazon\SessionManagerPlugin\bin') }; $cmd = Get-Command session-manager-plugin.exe -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path $cmd.Source -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp) { $dirs += $rp.Trim() } }; foreach ($d in $dirs) { if ($d -and (Test-Path -LiteralPath (Join-Path $d 'session-manager-plugin.exe'))) { [IO.File]::WriteAllText($env:PLUGIN_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } }" >nul 2>nul
if exist "%PLUGIN_TMP%" set /p AWS_PLUGIN_CARPETA=<"%PLUGIN_TMP%"
if exist "%PLUGIN_TMP%" del /f /q "%PLUGIN_TMP%" >nul 2>nul
exit /b 0

:falta_aws_cli
echo.
echo [FALTA INSTALAR AWS CLI v2]
echo No se pudo instalar AWS CLI v2 automaticamente.
echo Descargalo de https://awscli.amazonaws.com/AWSCLIV2.msi, instalalo y vuelve a abrir este archivo.
exit /b 1

:falta_plugin_ssm
echo.
echo [FALTA INSTALAR SESSION MANAGER PLUGIN]
echo No se pudo instalar el Session Manager Plugin de AWS automaticamente.
echo Descargalo de https://s3.amazonaws.com/session-manager-downloads/plugin/latest/windows/SessionManagerPluginSetup.exe
echo instalalo y vuelve a abrir este archivo.
exit /b 1

:falta_escribir_aws
echo.
echo [ERROR] No se pudo guardar "%USERPROFILE%\.carmir\aws.properties".
exit /b 1

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

:error_aws
echo.
echo [ERROR] No se termino de configurar la conexion con AWS (el motivo esta arriba).
echo Corrige el problema y vuelve a abrir este archivo con la opcion 2 o 3.
echo Los datos que ya escribiste se conservan.
pause
exit /b 1
