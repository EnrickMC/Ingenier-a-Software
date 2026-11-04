@echo off
setlocal EnableExtensions
chcp 65001 >nul
title CARMIR - Paso 1: Instalacion Inicial
cd /d "%~dp0"

echo ======================================================================
echo        SISTEMA CARMIR - INSTALACION AUTOMATICA (PRIMERA VEZ)
echo ======================================================================
echo.

REM --- 1. Verificar Node.js (version 22 o superior) ---
if exist "C:\Program Files\nodejs\node.exe" set "PATH=C:\Program Files\nodejs;%PATH%"
where node >nul 2>nul
if errorlevel 1 goto :instalar_node

node -e "process.exit(parseInt(process.versions.node.split('.')[0], 10) >= 22 ? 0 : 1)" >nul 2>nul
if errorlevel 1 (
    echo [AVISO] Se detecto una version antigua de Node.js. Se actualizara a la version LTS...
    goto :instalar_node
)
goto :node_listo

:instalar_node
echo [AUTO-INSTALACION] Descargando e instalando Node.js LTS desde Internet...
echo (Si aparece una ventana de Windows pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :falta_node
winget install --id OpenJS.NodeJS.LTS --exact --accept-package-agreements --accept-source-agreements
if exist "C:\Program Files\nodejs\node.exe" set "PATH=C:\Program Files\nodejs;%PATH%"
where node >nul 2>nul
if errorlevel 1 goto :falta_node

:node_listo
echo [OK] Node.js listo:
call node -v
echo.

REM --- 2. Detectar o instalar Java JDK 25 (JAVA_HOME) ---
call :buscar_jdk25
if defined JDK25_ENCONTRADO goto :java_listo

echo [AUTO-INSTALACION] Descargando e instalando Java JDK 25 desde Internet...
echo (No borra ni afecta otras versiones de Java que ya tengas instaladas)
echo (Si aparece una ventana de Windows pidiendo permiso, haz clic en "Si")
echo.
where winget >nul 2>nul
if errorlevel 1 goto :falta_java
winget install --id Oracle.JDK.25 --exact --accept-package-agreements --accept-source-agreements
call :buscar_jdk25
if not defined JDK25_ENCONTRADO goto :falta_java

:java_listo
set "JAVA_HOME=%JDK25_ENCONTRADO%"
setx JAVA_HOME "%JAVA_HOME%" >nul 2>nul
echo [OK] Java JDK 25 configurado en: "%JAVA_HOME%"
echo.

REM --- 3. Localizar o instalar MySQL / MariaDB (XAMPP) ---
call :buscar_mysql
if defined MYSQL_EXE goto :mysql_encontrado

echo [AUTO-INSTALACION] No se encontro MySQL ni XAMPP.
echo Descargando e instalando XAMPP desde Internet...
echo (Si aparece una ventana de Windows pidiendo permiso, haz clic en "Si")
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

if exist "C:\xampp\mysql\bin\mysqld.exe" (
    echo [INFO] Iniciando MySQL de XAMPP automaticamente...
    start "MySQL CARMIR (XAMPP)" /min "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
    ping 127.0.0.1 -n 6 >nul
    call :puerto_activo 3306
    if not errorlevel 1 goto :mysql_encendido
)
if exist "D:\xampp\mysql\bin\mysqld.exe" (
    echo [INFO] Iniciando MySQL de XAMPP automaticamente...
    start "MySQL CARMIR (XAMPP)" /min "D:\xampp\mysql\bin\mysqld.exe" --defaults-file="D:\xampp\mysql\bin\my.ini" --standalone
    ping 127.0.0.1 -n 6 >nul
    call :puerto_activo 3306
    if not errorlevel 1 goto :mysql_encendido
)

echo.
echo [ATENCION] MySQL todavia no esta encendido en el puerto 3306.
echo Por favor abre XAMPP Control Panel y presiona "Start" en MySQL
echo (o inicia tu servicio de MySQL), y luego presiona cualquier tecla aqui...
pause >nul
goto :revisar_puerto_mysql

:mysql_encendido
echo [OK] Servidor MySQL activo en el puerto 3306.
echo.

REM --- 5. Probar conexion root (sin contrasena o con contrasena) ---
set "MYSQL_PASS="
"%MYSQL_EXE%" -u root -h 127.0.0.1 -P 3306 -e "SELECT 1;" >nul 2>nul
if not errorlevel 1 goto :ejecutar_sql_sin_pass

:pedir_pass_mysql
echo Tu instalacion de MySQL tiene contrasena para el usuario "root".
set /p "MYSQL_PASS=Escribe la contrasena de root de tu MySQL y presiona Enter: "
"%MYSQL_EXE%" -u root "-p%MYSQL_PASS%" -h 127.0.0.1 -P 3306 -e "SELECT 1;" >nul 2>nul
if not errorlevel 1 goto :ejecutar_sql_con_pass
echo [ERROR] Contrasena incorrecta. Intenta de nuevo.
echo.
goto :pedir_pass_mysql

:ejecutar_sql_sin_pass
echo [1/4] Creando estructura de la base de datos CARMIR_DB...
"%MYSQL_EXE%" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root -h 127.0.0.1 -P 3306 < "DBCarmir\INSTALACION_COMPLETA.sql"
if errorlevel 1 goto :error_sql

echo [2/4] Cargando datos de prueba (productos, recetas, usuarios, pedidos)...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root -h 127.0.0.1 -P 3306 < "DBCarmir\POBLAR_BASE.sql"
if errorlevel 1 goto :error_sql

echo [3/4] Configurando usuario de lectura (carmir_web)...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root -h 127.0.0.1 -P 3306 < "PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_lectura.sql" >nul
if errorlevel 1 goto :error_sql

echo [4/4] Configurando usuario de pedidos (carmir_pedidos)...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root -h 127.0.0.1 -P 3306 < "PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_pedidos.sql" >nul
if errorlevel 1 goto :error_sql

call :escribir_db_properties ""
goto :configurar_env

:ejecutar_sql_con_pass
echo [1/4] Creando estructura de la base de datos CARMIR_DB...
"%MYSQL_EXE%" -u root "-p%MYSQL_PASS%" -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root "-p%MYSQL_PASS%" -h 127.0.0.1 -P 3306 < "DBCarmir\INSTALACION_COMPLETA.sql"
if errorlevel 1 goto :error_sql

echo [2/4] Cargando datos de prueba (productos, recetas, usuarios, pedidos)...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root "-p%MYSQL_PASS%" -h 127.0.0.1 -P 3306 < "DBCarmir\POBLAR_BASE.sql"
if errorlevel 1 goto :error_sql

echo [3/4] Configurando usuario de lectura (carmir_web)...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root "-p%MYSQL_PASS%" -h 127.0.0.1 -P 3306 < "PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_lectura.sql" >nul
if errorlevel 1 goto :error_sql

echo [4/4] Configurando usuario de pedidos (carmir_pedidos)...
"%MYSQL_EXE%" --default-character-set=utf8mb4 -u root "-p%MYSQL_PASS%" -h 127.0.0.1 -P 3306 < "PaginaWebCARMIR\ApiCARMIR\sql\crear_usuario_pedidos.sql" >nul
if errorlevel 1 goto :error_sql

call :escribir_db_properties "%MYSQL_PASS%"
goto :configurar_env

REM --- 6. Crear archivo .env de la API ---
:configurar_env
echo.
echo [OK] Base de datos CARMIR_DB lista y poblada.
if not exist "PaginaWebCARMIR\ApiCARMIR\.env" (
    (
        echo API_HOST=127.0.0.1
        echo API_PUERTO=3000
        echo CORS_ORIGENES=http://localhost:4200
        echo LIMITE_PETICIONES_MINUTO=300
        echo LIMITE_PEDIDOS_HORA=10
        echo DB_HOST=127.0.0.1
        echo DB_PUERTO=3306
        echo DB_NOMBRE=CARMIR_DB
        echo DB_USUARIO=carmir_web
        echo DB_CONTRASENA=CambiaEstaContrasena2026
        echo DB_CONEXIONES_MAXIMAS=10
        echo DB_TIEMPO_ESPERA_MS=10000
        echo DB_ESCRITURA_USUARIO=carmir_pedidos
        echo DB_ESCRITURA_CONTRASENA=CambiaEstaContrasena2026
    ) > "PaginaWebCARMIR\ApiCARMIR\.env"
)

REM --- 7. Instalar paquetes de la API y la Pagina Web ---
echo.
echo ======================================================================
echo   Instalando librerias de ApiCARMIR (Node.js)...
echo ======================================================================
pushd "PaginaWebCARMIR\ApiCARMIR"
call npm.cmd install
if errorlevel 1 (
    popd
    goto :error_npm
)
call npm.cmd run diagnostico
popd

echo.
echo ======================================================================
echo   Instalando librerias de PaginaWebCARMIR (Angular)...
echo   (Esto puede tardar de 1 a 3 minutos la primera vez)
echo ======================================================================
pushd "PaginaWebCARMIR"
set "NG_CLI_ANALYTICS=false"
call npm.cmd install
if errorlevel 1 (
    popd
    goto :error_npm
)
popd

echo.
echo ======================================================================
echo   Compilando PanelAdministrativoCARMIR (JavaFX + Maven)...
echo   (Descargando dependencias la primera vez, por favor espera...)
echo ======================================================================
pushd "PanelAdministrativoCARMIR"
call .\mvnw.cmd compile
if errorlevel 1 (
    popd
    goto :error_maven
)
popd

echo.
echo ======================================================================
echo   INSTALACION COMPLETADA CON EXITO
echo ======================================================================
echo   Todo quedo listo. Ahora solo haz doble clic en:
echo       2_INICIAR_SISTEMA.bat
echo ======================================================================
echo.
if not defined LLAMADO_DESDE_INICIAR pause
exit /b 0

REM ===================== SUBRUTINAS DE APOYO =====================

:buscar_jdk25
set "JDK25_ENCONTRADO="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    "%JAVA_HOME%\bin\javac.exe" -version 2>&1 | findstr " 25" >nul && set "JDK25_ENCONTRADO=%JAVA_HOME%"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("C:\Program Files\Java\jdk-25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("C:\Program Files\Microsoft\jdk-25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("C:\Program Files\BellSoft\LibericaJDK-25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("C:\Program Files\Amazon Corretto\jdk25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("C:\Program Files\Zulu\zulu-25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /d %%D in ("%USERPROFILE%\.jdks\*25*") do if exist "%%~fD\bin\javac.exe" set "JDK25_ENCONTRADO=%%~fD"
)
if not defined JDK25_ENCONTRADO (
    for /f "delims=" %%J in ('where javac 2^>nul') do (
        "%%~fJ" -version 2>&1 | findstr " 25" >nul && for %%P in ("%%~dpJ..") do set "JDK25_ENCONTRADO=%%~fP"
    )
)
exit /b 0

:buscar_mysql
set "MYSQL_EXE="
if exist "C:\xampp\mysql\bin\mysql.exe" set "MYSQL_EXE=C:\xampp\mysql\bin\mysql.exe"
if not defined MYSQL_EXE if exist "D:\xampp\mysql\bin\mysql.exe" set "MYSQL_EXE=D:\xampp\mysql\bin\mysql.exe"
if not defined MYSQL_EXE (
    for /d %%M in ("C:\Program Files\MySQL\MySQL Server *") do if exist "%%~fM\bin\mysql.exe" set "MYSQL_EXE=%%~fM\bin\mysql.exe"
)
if not defined MYSQL_EXE (
    for /d %%M in ("C:\Program Files\MariaDB *") do if exist "%%~fM\bin\mysql.exe" set "MYSQL_EXE=%%~fM\bin\mysql.exe"
)
if not defined MYSQL_EXE (
    for /f "delims=" %%M in ('where mysql 2^>nul') do if not defined MYSQL_EXE set "MYSQL_EXE=%%~fM"
)
exit /b 0

:puerto_activo
powershell -NoProfile -Command "try { $t = New-Object Net.Sockets.TcpClient('127.0.0.1', %~1); $t.Close(); exit 0 } catch { exit 1 }" >nul 2>nul
exit /b %ERRORLEVEL%

:escribir_db_properties
(
    echo # Conexion a la base de datos MySQL
    echo db.url=jdbc:mysql://localhost:3306/CARMIR_DB?useSSL=false^&serverTimezone=America/Mexico_City^&allowPublicKeyRetrieval=true
    echo db.usuario=root
    echo db.contrasena=%~1
) > "PanelAdministrativoCARMIR\src\main\resources\com\pac\config\db.properties"
exit /b 0

:falta_node
echo.
echo [FALTA INSTALAR NODE.JS]
echo No se pudo instalar Node.js automaticamente.
echo Descargalo de https://nodejs.org/ (version LTS) y vuelve a abrir este archivo.
echo.
pause
exit /b 1

:falta_java
echo.
echo [FALTA INSTALAR JAVA JDK 25]
echo No se pudo instalar Java JDK 25 automaticamente.
echo Descargalo de https://www.oracle.com/java/technologies/downloads/#java25-windows
echo y vuelve a abrir este archivo.
echo.
pause
exit /b 1

:falta_mysql
echo.
echo [FALTA INSTALAR XAMPP O MYSQL]
echo No se pudo instalar XAMPP automaticamente.
echo Descargalo de https://www.apachefriends.org/es/index.html y vuelve a abrir este archivo.
echo.
pause
exit /b 1

:error_sql
echo.
echo [ERROR] Ocurrio un problema al ejecutar los scripts SQL de CARMIR_DB.
pause
exit /b 1

:error_npm
echo.
echo [ERROR] Ocurrio un problema al descargar las librerias con npm.
echo Verifica tu conexion a Internet e intenta de nuevo.
pause
exit /b 1

:error_maven
echo.
echo [ERROR] Ocurrio un problema al compilar el Panel Administrativo con Maven.
echo Verifica tu conexion a Internet y que tengas instalado JDK 25.
pause
exit /b 1
