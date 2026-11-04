@echo off
setlocal EnableExtensions
chcp 65001 >nul
title CARMIR - Iniciar Sistema Completo
cd /d "%~dp0"

echo ======================================================================
echo              INICIANDO SISTEMA CARMIR (3 COMPONENTES)
echo ======================================================================
echo.

REM --- 1. Asegurar Node.js y JAVA_HOME (JDK 25) ---
if exist "C:\Program Files\nodejs\node.exe" set "PATH=C:\Program Files\nodejs;%PATH%"

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
if defined JDK25_ENCONTRADO set "JAVA_HOME=%JDK25_ENCONTRADO%"

REM --- 2. Encender MySQL de XAMPP automaticamente si esta apagado ---
call :puerto_activo 3306
if errorlevel 1 (
    if exist "C:\xampp\mysql\bin\mysqld.exe" (
        echo [1/4] Encendiendo MySQL de XAMPP...
        start "MySQL CARMIR (XAMPP)" /min "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
        ping 127.0.0.1 -n 6 >nul
        "C:\xampp\mysql\bin\mysql.exe" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
    ) else if exist "D:\xampp\mysql\bin\mysqld.exe" (
        echo [1/4] Encendiendo MySQL de XAMPP...
        start "MySQL CARMIR (XAMPP)" /min "D:\xampp\mysql\bin\mysqld.exe" --defaults-file="D:\xampp\mysql\bin\my.ini" --standalone
        ping 127.0.0.1 -n 6 >nul
        "D:\xampp\mysql\bin\mysql.exe" -u root -h 127.0.0.1 -P 3306 -e "SET GLOBAL max_allowed_packet = 16777216;" >nul 2>nul
    )
)

REM --- 3. Si aun no se ha instalado la primera vez, ejecutar el Paso 1 automaticamente ---
if not exist "PaginaWebCARMIR\ApiCARMIR\node_modules" goto :ejecutar_instalacion_previa
if not exist "PaginaWebCARMIR\node_modules" goto :ejecutar_instalacion_previa
if not exist "PaginaWebCARMIR\ApiCARMIR\.env" goto :ejecutar_instalacion_previa

pushd "PaginaWebCARMIR\ApiCARMIR"
call node src\diagnostico.js >nul 2>nul
if errorlevel 1 (
    popd
    goto :ejecutar_instalacion_previa
)
popd
goto :arrancar_servicios

:ejecutar_instalacion_previa
echo [AVISO] Detectamos que falta preparar dependencias o la base de datos.
echo         Ejecutando automaticamente 1_INSTALAR_PRIMERA_VEZ.bat ...
echo.
set "LLAMADO_DESDE_INICIAR=1"
call "%~dp01_INSTALAR_PRIMERA_VEZ.bat"
if errorlevel 1 exit /b 1

:arrancar_servicios
echo [1/3] Iniciando API REST (http://127.0.0.1:3000)...
call :puerto_activo 3000
if errorlevel 1 (
    start "1. API CARMIR - Puerto 3000 (No cerrar)" /d "%~dp0PaginaWebCARMIR\ApiCARMIR" cmd /k "npm.cmd start"
) else (
    echo       La API ya estaba encendida en el puerto 3000.
)

echo [2/3] Iniciando Pagina Web en Angular (http://localhost:4200)...
call :puerto_activo 4200
if errorlevel 1 (
    start "2. Pagina Web CARMIR - Puerto 4200 (No cerrar)" /d "%~dp0PaginaWebCARMIR" cmd /k "set NG_CLI_ANALYTICS=false&& npm.cmd start"
) else (
    echo       La Pagina Web ya estaba encendida en el puerto 4200.
)

echo [3/3] Abriendo Panel Administrativo CARMIR (Escritorio JavaFX)...
start "3. Panel Administrativo CARMIR (No cerrar)" /d "%~dp0PanelAdministrativoCARMIR" cmd /k ".\mvnw.cmd javafx:run"

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
echo   - Panel Administrativo:        Se abrira en una ventana aparte
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

:puerto_activo
powershell -NoProfile -Command "try { $t = New-Object Net.Sockets.TcpClient('127.0.0.1', %~1); $t.Close(); exit 0 } catch { try { $t2 = New-Object Net.Sockets.TcpClient('localhost', %~1); $t2.Close(); exit 0 } catch { exit 1 } }" >nul 2>nul
exit /b %ERRORLEVEL%
