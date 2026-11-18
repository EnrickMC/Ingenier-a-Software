@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title CARMIR - Panel Administrativo (AWS)
cd /d "%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

REM =====================================================================
REM  CARMIR - Abre el Panel Administrativo conectado a la base de AWS (Amazon RDS)
REM
REM  La pagina web y la API ya viven en AWS; en esta PC solo se abre el Panel.
REM  La base de datos es privada: se llega por un tunel de AWS Systems Manager (SSM)
REM  que pasa por la instancia EC2 de la API y queda en 127.0.0.1 de esta PC.
REM
REM  Este archivo NO tiene ningun dato fijo de AWS. Todo se lee de
REM  %USERPROFILE%\.carmir\aws.properties, que escribe 1_INSTALAR_PRIMERA_VEZ.bat
REM  (opcion 2 o 3) junto con el perfil de AWS CLI que guarda las llaves de acceso.
REM
REM  Pasos: abre el tunel, espera a que el puerto local responda, abre el Panel y,
REM  cuando el Panel se cierra, cierra el tunel.
REM =====================================================================

set "TITULO_TUNEL=CARMIR Tunel RDS"
set "ARCHIVO_AWS=%USERPROFILE%\.carmir\aws.properties"

echo ======================================================================
echo         CARMIR - PANEL ADMINISTRATIVO CONECTADO A AWS
echo ======================================================================
echo.

REM --- 1. Leer la configuracion de AWS de esta PC ---
if not exist "%ARCHIVO_AWS%" goto :falta_configuracion
call :leer_aws_properties
if not defined CARMIR_AWS_PERFIL goto :configuracion_incompleta
if not defined CARMIR_AWS_REGION goto :configuracion_incompleta
if not defined CARMIR_AWS_INSTANCIA goto :configuracion_incompleta
if not defined CARMIR_AWS_SERVIDOR_RDS goto :configuracion_incompleta
if not defined CARMIR_AWS_PUERTO_RDS goto :configuracion_incompleta
if not defined CARMIR_AWS_PUERTO_LOCAL goto :configuracion_incompleta

REM --- 2. AWS CLI y Session Manager Plugin en esta sesion ---
REM (Sin parentesis alrededor de %PATH%: puede traer "Program Files (x86)" y romper el bloque)
if defined CARMIR_AWS_CLI_CARPETA if exist "%CARMIR_AWS_CLI_CARPETA%\aws.exe" set "PATH=%CARMIR_AWS_CLI_CARPETA%;%PATH%"
if defined CARMIR_AWS_PLUGIN_CARPETA if exist "%CARMIR_AWS_PLUGIN_CARPETA%\session-manager-plugin.exe" set "PATH=%CARMIR_AWS_PLUGIN_CARPETA%;%PATH%"
where aws >nul 2>nul
if errorlevel 1 goto :falta_aws
where session-manager-plugin >nul 2>nul
if errorlevel 1 goto :falta_plugin
aws sts get-caller-identity --profile "%CARMIR_AWS_PERFIL%" --region "%CARMIR_AWS_REGION%" >nul 2>nul
if errorlevel 1 goto :perfil_falla

REM --- 3. JDK 25 (misma busqueda que 2_INICIAR_SISTEMA.bat) ---
set "JDK_ENCONTRADO="
set "JDK_TMP=%TEMP%\carmir_jdk_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @(); if ($env:JAVA_HOME) { $dirs += $env:JAVA_HOME }; $roots = @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft', 'C:\Program Files\BellSoft', 'C:\Program Files\Amazon Corretto', 'C:\Program Files\Zulu', (Join-Path $env:USERPROFILE '.jdks')); foreach ($r in $roots) { if (Test-Path -LiteralPath $r) { Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $dirs += $_.FullName } } }; $cmd = Get-Command javac -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path (Split-Path $cmd.Source -Parent) -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp -and (Test-Path -LiteralPath (Join-Path $rp.Trim() 'javac.exe'))) { $dirs += (Split-Path $rp.Trim() -Parent) } }; foreach ($d in $dirs) { $jc = Join-Path $d 'bin\javac.exe'; if (Test-Path -LiteralPath $jc) { $ver = (& $jc -version 2>&1 | Out-String); if ($ver -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 25) { [IO.File]::WriteAllText($env:JDK_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } }" >nul 2>nul
if exist "%JDK_TMP%" set /p JDK_ENCONTRADO=<"%JDK_TMP%"
if exist "%JDK_TMP%" del /f /q "%JDK_TMP%" >nul 2>nul
if not defined JDK_ENCONTRADO goto :falta_jdk
set "JAVA_HOME=%JDK_ENCONTRADO%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM --- 4. Abrir el tunel en otra ventana minimizada ---
echo [1/3] Abriendo el tunel seguro hacia la base de datos ...
start "%TITULO_TUNEL%" /min cmd /c aws ssm start-session --profile "%CARMIR_AWS_PERFIL%" --region "%CARMIR_AWS_REGION%" --target "%CARMIR_AWS_INSTANCIA%" --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters "host=%CARMIR_AWS_SERVIDOR_RDS%,portNumber=%CARMIR_AWS_PUERTO_RDS%,localPortNumber=%CARMIR_AWS_PUERTO_LOCAL%"

REM Esperar hasta 40 segundos a que el puerto local responda
"%PS_EXE%" -NoProfile -Command "$ok = $false; for ($i = 0; $i -lt 40; $i++) { try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', [int]$env:CARMIR_AWS_PUERTO_LOCAL); $c.Close(); $ok = $true; break } catch { Start-Sleep -Seconds 1 } }; if ($ok) { exit 0 } else { exit 1 }"
if errorlevel 1 goto :tunel_falla
echo       Tunel listo en 127.0.0.1:%CARMIR_AWS_PUERTO_LOCAL%

REM --- 5. Abrir el Panel en modo AWS (el Panel lee aws.properties por su cuenta) ---
echo [2/3] Abriendo el Panel Administrativo ...
echo       (esta ventana se queda abierta mientras uses el Panel)
set "CARMIR_MODO=aws"
pushd "%~dp0Software_CARMIR\PanelAdministrativoCARMIR"
call mvnw.cmd -q javafx:run
popd

:cerrar_tunel
echo [3/3] Cerrando el tunel ...
taskkill /FI "WINDOWTITLE eq %TITULO_TUNEL%*" /T /F >nul 2>nul
echo Listo.
endlocal
exit /b 0

REM ===================== SUBRUTINAS Y MENSAJES =====================

:leer_aws_properties
REM Pasa los valores de aws.properties a variables CARMIR_AWS_* (la contrasena de la base NO se lee)
set "AWS_LEER_TMP=%TEMP%\carmir_aws_%RANDOM%_%RANDOM%.cmd"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$q = [string][char]34; $pc = [string][char]37; $mapa = [ordered]@{ 'aws.perfil' = 'CARMIR_AWS_PERFIL'; 'aws.region' = 'CARMIR_AWS_REGION'; 'aws.ec2.instancia' = 'CARMIR_AWS_INSTANCIA'; 'aws.rds.servidor' = 'CARMIR_AWS_SERVIDOR_RDS'; 'aws.rds.puerto' = 'CARMIR_AWS_PUERTO_RDS'; 'aws.tunel.puertoLocal' = 'CARMIR_AWS_PUERTO_LOCAL'; 'aws.bd.nombre' = 'CARMIR_AWS_BD_NOMBRE'; 'aws.bd.usuario' = 'CARMIR_AWS_BD_USUARIO'; 'aws.s3.bucketImagenes' = 'CARMIR_AWS_BUCKET'; 'aws.imagenes.dominio' = 'CARMIR_AWS_DOMINIO'; 'aws.cli.carpeta' = 'CARMIR_AWS_CLI_CARPETA'; 'aws.ssm.pluginCarpeta' = 'CARMIR_AWS_PLUGIN_CARPETA' }; $salida = @(); foreach ($v in $mapa.Values) { $salida += ('set ' + $q + $v + '=' + $q) }; $src = Join-Path $env:USERPROFILE '.carmir\aws.properties'; if (Test-Path -LiteralPath $src) { foreach ($l in [IO.File]::ReadAllLines($src)) { if ($l -match '^\s*([A-Za-z0-9_.]+)\s*[=:]\s*(.*)$') { $k = $Matches[1]; if ($mapa.Contains($k)) { $v = $Matches[2].Trim().Replace('\\', '\').Replace($q, '').Replace($pc, $pc + $pc); $salida += ('set ' + $q + $mapa[$k] + '=' + $v + $q) } } } }; [IO.File]::WriteAllLines($env:AWS_LEER_TMP, $salida, (New-Object Text.UTF8Encoding($false)))" >nul 2>nul
if exist "%AWS_LEER_TMP%" call "%AWS_LEER_TMP%"
if exist "%AWS_LEER_TMP%" del /f /q "%AWS_LEER_TMP%" >nul 2>nul
exit /b 0

:tunel_falla
echo [FALLA] El tunel no respondio. Revisa la ventana "%TITULO_TUNEL%" para ver el error.
echo         Causas comunes: la EC2 esta apagada o sin internet, el ID de la instancia o el
echo         endpoint de RDS estan mal escritos, o el usuario IAM no tiene permiso de SSM.
echo         Para corregir los datos vuelve a ejecutar 1_INSTALAR_PRIMERA_VEZ.bat (opcion 2).
pause
goto :cerrar_tunel

:falta_configuracion
echo [FALLA] Esta PC todavia no tiene la configuracion de AWS:
echo         "%ARCHIVO_AWS%"
echo         Ejecuta 1_INSTALAR_PRIMERA_VEZ.bat y elige la opcion 2 (o 3).
pause
exit /b 1

:configuracion_incompleta
echo [FALLA] Faltan datos en "%ARCHIVO_AWS%".
echo         Vuelve a ejecutar 1_INSTALAR_PRIMERA_VEZ.bat con la opcion 2 (o 3).
pause
exit /b 1

:falta_aws
echo [FALLA] No se encontro AWS CLI v2. Ejecuta 1_INSTALAR_PRIMERA_VEZ.bat con la opcion 2 (o 3).
pause
exit /b 1

:falta_plugin
echo [FALLA] No se encontro el Session Manager Plugin de AWS.
echo         Ejecuta 1_INSTALAR_PRIMERA_VEZ.bat con la opcion 2 (o 3).
pause
exit /b 1

:perfil_falla
echo [FALLA] AWS no acepto las llaves del perfil "%CARMIR_AWS_PERFIL%" en la region "%CARMIR_AWS_REGION%".
echo         Revisa tu conexion a internet o vuelve a escribir las llaves con
echo         1_INSTALAR_PRIMERA_VEZ.bat (opcion 2).
pause
exit /b 1

:falta_jdk
echo [FALLA] No se encontro el JDK 25. Ejecuta primero 1_INSTALAR_PRIMERA_VEZ.bat
pause
exit /b 1
