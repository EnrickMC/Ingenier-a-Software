@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title CARMIR - Publicar la pagina web en AWS
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

REM =====================================================================
REM  CARMIR - Publica la pagina web (Angular) en AWS: bucket de S3 + CloudFront
REM
REM  Este archivo NO tiene ningun dato fijo de AWS. La primera vez pregunta:
REM    - perfil de AWS CLI y region,
REM    - bucket de la pagina, ID y dominio de la distribucion de CloudFront,
REM    - llaves del usuario IAM que publica (si el perfil todavia no funciona).
REM  Los datos se guardan en %USERPROFILE%\.carmir\web.aws.properties (fuera de Git)
REM  y las llaves en el perfil de AWS CLI (carpeta .aws del usuario).
REM  Para cambiarlos, al abrir este archivo elige la opcion C.
REM
REM  Que hace:
REM    1. ng build (version de produccion: la API se llama en /api del mismo dominio).
REM    2. Sube el CONTENIDO de dist\PaginaWebCARMIR\browser a la raiz del bucket y borra lo viejo.
REM    3. index.html se sube sin cache para que los clientes vean la version nueva al momento.
REM    4. Limpia la cache de CloudFront (invalidacion /*).
REM =====================================================================

REM Carpeta PaginaWebCARMIR (este archivo esta en PaginaWebCARMIR\despliegue)
cd /d "%~dp0.."
set "ARCHIVO_WEB=%USERPROFILE%\.carmir\web.aws.properties"

echo ======================================================================
echo         CARMIR - PUBLICAR LA PAGINA WEB EN AWS
echo ======================================================================
echo.

REM --- 1. Datos de AWS de esta PC ---
call :leer_configuracion
set "CAMBIAR_DATOS=0"
if not defined WEB_PERFIL set "CAMBIAR_DATOS=1"
if not defined WEB_REGION set "CAMBIAR_DATOS=1"
if not defined WEB_BUCKET set "CAMBIAR_DATOS=1"
if not defined WEB_DISTRIBUCION set "CAMBIAR_DATOS=1"
if not defined WEB_DOMINIO set "CAMBIAR_DATOS=1"
if "%CAMBIAR_DATOS%"=="1" goto :pedir_datos

echo Configuracion guardada en "%ARCHIVO_WEB%":
echo   Perfil: %WEB_PERFIL%   Region: %WEB_REGION%
echo   Bucket: %WEB_BUCKET%
echo   CloudFront: %WEB_DISTRIBUCION%  -  %WEB_DOMINIO%
echo.
set "OPCION_DATOS=P"
set /p "OPCION_DATOS=[P] Publicar con estos datos   [C] Cambiar los datos  (Enter = P): "
if /i "%OPCION_DATOS%"=="C" goto :pedir_datos
goto :datos_listos

:pedir_datos
if not defined WEB_PERFIL set "WEB_PERFIL=carmir-despliegue"
echo Escribe los datos (Enter conserva el valor entre corchetes):
call :preguntar WEB_PERFIL "Perfil de AWS CLI que publica la pagina"
call :preguntar WEB_REGION "Region de AWS, por ejemplo us-east-2"
call :preguntar WEB_BUCKET "Bucket de S3 de la pagina web - nombre"
call :preguntar WEB_DISTRIBUCION "ID de la distribucion de CloudFront - empieza con E"
call :preguntar WEB_DOMINIO "Dominio de CloudFront, por ejemplo https://dxxxx.cloudfront.net"
call :guardar_configuracion
if errorlevel 1 goto :falla_guardar
echo [OK] Datos guardados en "%ARCHIVO_WEB%"
echo.

:datos_listos

REM --- 2. AWS CLI v2 en esta ventana ---
call :buscar_aws_cli
if defined AWS_CLI_CARPETA goto :aws_cli_listo
echo [AUTO-INSTALACION] Instalando AWS CLI v2...
echo (Si Windows abre una ventana pidiendo permiso, haz clic en "Si")
where winget >nul 2>nul
if errorlevel 1 goto :aws_cli_msi
winget install --id Amazon.AWSCLI --exact --accept-package-agreements --accept-source-agreements
call :buscar_aws_cli
if defined AWS_CLI_CARPETA goto :aws_cli_listo
:aws_cli_msi
start "" /wait msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi /passive /norestart
call :buscar_aws_cli
if not defined AWS_CLI_CARPETA goto :falta_aws_cli
:aws_cli_listo
set "PATH=%AWS_CLI_CARPETA%;%PATH%"
set "AWS_EXE=%AWS_CLI_CARPETA%\aws.exe"

REM --- 3. Revisar las llaves del perfil (si no funcionan, se piden) ---
:revisar_perfil
echo == 1. Revisando la cuenta de AWS (perfil %WEB_PERFIL%) ==
"%AWS_EXE%" sts get-caller-identity --profile "%WEB_PERFIL%" --region "%WEB_REGION%" --query Arn --output text
if not errorlevel 1 goto :perfil_listo
echo.
echo El perfil "%WEB_PERFIL%" todavia no tiene llaves validas.
echo Escribe las llaves del usuario IAM que publica la pagina (Access Key ID y Secret Access Key).
call :pedir_llaves
if errorlevel 1 goto :falla_llaves
goto :revisar_perfil
:perfil_listo
echo.

REM --- 4. Compilar ---
where npm.cmd >nul 2>nul
if errorlevel 1 goto :falta_node
echo == 2. Compilando la pagina (ng build) ==
set "NG_CLI_ANALYTICS=false"
if not exist node_modules call npm.cmd ci --no-audit --no-fund
call npm.cmd run build -- --configuration production
if errorlevel 1 goto :falla_build
set "SALIDA=dist\PaginaWebCARMIR\browser"
if not exist "%SALIDA%\index.html" goto :falla_build
echo.

REM --- 5. Confirmar antes de reemplazar la pagina publicada ---
echo Se va a REEMPLAZAR la pagina publicada en s3://%WEB_BUCKET% con esta version.
set "CONFIRMAR=N"
set /p "CONFIRMAR=Escribe S para publicar (cualquier otra cosa cancela): "
if /i not "%CONFIRMAR%"=="S" goto :cancelado

REM --- 6. Subir al bucket ---
echo == 3. Subiendo archivos a s3://%WEB_BUCKET% ==
REM Todo menos index.html, con cache de un dia (los .js y .css cambian de nombre en cada version)
"%AWS_EXE%" s3 sync "%SALIDA%" "s3://%WEB_BUCKET%" --delete --exclude index.html --cache-control "public,max-age=86400" --profile "%WEB_PERFIL%" --region "%WEB_REGION%"
if errorlevel 1 goto :falla_s3
REM index.html sin cache
"%AWS_EXE%" s3 cp "%SALIDA%\index.html" "s3://%WEB_BUCKET%/index.html" --cache-control "no-cache" --content-type "text/html; charset=utf-8" --profile "%WEB_PERFIL%" --region "%WEB_REGION%"
if errorlevel 1 goto :falla_s3

REM --- 7. Limpiar la cache de CloudFront ---
echo == 4. Limpiando la cache de CloudFront ==
"%AWS_EXE%" cloudfront create-invalidation --distribution-id "%WEB_DISTRIBUCION%" --paths "/*" --profile "%WEB_PERFIL%" >nul
if errorlevel 1 echo [AVISO] No se pudo limpiar la cache. La pagina nueva puede tardar hasta un dia en verse.

echo.
echo ======================================================================
echo   Listo. Abre %WEB_DOMINIO%
echo   La limpieza de cache de CloudFront tarda 1 o 2 minutos.
echo ======================================================================
pause
exit /b 0

REM ===================== SUBRUTINAS =====================

:preguntar
REM %1 = variable, %2 = pregunta. Enter conserva el valor actual.
set "VALOR_NUEVO="
call set "VALOR_ACTUAL=%%%~1%%"
if defined VALOR_ACTUAL set /p "VALOR_NUEVO=%~2 [%VALOR_ACTUAL%]: "
if not defined VALOR_ACTUAL set /p "VALOR_NUEVO=%~2: "
if defined VALOR_NUEVO set "%~1=%VALOR_NUEVO%"
call set "VALOR_ACTUAL=%%%~1%%"
if defined VALOR_ACTUAL exit /b 0
echo   Este dato es obligatorio.
goto :preguntar

:leer_configuracion
REM Pasa web.aws.properties a variables WEB_* (y la carpeta de AWS CLI que guardo el instalador)
set "WEB_LEER_TMP=%TEMP%\carmir_web_%RANDOM%_%RANDOM%.cmd"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$q = [string][char]34; $pc = [string][char]37; $mapa = [ordered]@{ 'web.perfil' = 'WEB_PERFIL'; 'web.region' = 'WEB_REGION'; 'web.s3.bucket' = 'WEB_BUCKET'; 'web.cloudfront.distribucion' = 'WEB_DISTRIBUCION'; 'web.cloudfront.dominio' = 'WEB_DOMINIO'; 'aws.cli.carpeta' = 'WEB_CLI_CARPETA' }; $salida = @(); foreach ($v in $mapa.Values) { $salida += ('set ' + $q + $v + '=' + $q) }; $archivos = @((Join-Path $env:USERPROFILE '.carmir\aws.properties'), $env:ARCHIVO_WEB); foreach ($src in $archivos) { if (Test-Path -LiteralPath $src) { foreach ($l in [IO.File]::ReadAllLines($src)) { if ($l -match '^\s*([A-Za-z0-9_.]+)\s*[=:]\s*(.*)$') { $k = $Matches[1]; if ($mapa.Contains($k)) { $v = $Matches[2].Trim().Replace('\\', '\').Replace($q, '').Replace($pc, $pc + $pc); $salida += ('set ' + $q + $mapa[$k] + '=' + $v + $q) } } } } }; [IO.File]::WriteAllLines($env:WEB_LEER_TMP, $salida, (New-Object Text.UTF8Encoding($false)))" >nul 2>nul
if exist "%WEB_LEER_TMP%" call "%WEB_LEER_TMP%"
if exist "%WEB_LEER_TMP%" del /f /q "%WEB_LEER_TMP%" >nul 2>nul
exit /b 0

:guardar_configuracion
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dir = Join-Path $env:USERPROFILE '.carmir'; if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }; function V($nombre) { $v = [Environment]::GetEnvironmentVariable($nombre); if ($null -eq $v) { $v = '' }; return $v.Trim().Replace('\', '\\') }; $lineas = @('# Datos para publicar la pagina web de CARMIR en AWS. Los escribe PUBLICAR_WEB.bat.', '# NO se sube a Git. Las llaves de acceso estan en el perfil de AWS CLI, no aqui.', ('web.perfil=' + (V 'WEB_PERFIL')), ('web.region=' + (V 'WEB_REGION')), ('web.s3.bucket=' + (V 'WEB_BUCKET')), ('web.cloudfront.distribucion=' + (V 'WEB_DISTRIBUCION')), ('web.cloudfront.dominio=' + (V 'WEB_DOMINIO'))); try { [IO.File]::WriteAllLines($env:ARCHIVO_WEB, $lineas, (New-Object Text.UTF8Encoding($false))); exit 0 } catch { Write-Host $_.Exception.Message; exit 1 }"
exit /b %ERRORLEVEL%

:pedir_llaves
set "LLAVE_ID="
:pedir_llave_id
set /p "LLAVE_ID=Access Key ID: "
if not defined LLAVE_ID goto :pedir_llave_id
"%AWS_EXE%" configure set aws_access_key_id "%LLAVE_ID%" --profile "%WEB_PERFIL%"
if errorlevel 1 exit /b 1
"%AWS_EXE%" configure set region "%WEB_REGION%" --profile "%WEB_PERFIL%"
REM La Secret se pide oculta y PowerShell la guarda directo en el perfil
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "while ($true) { $s = Read-Host -Prompt 'Secret Access Key (no se muestra al escribir)' -AsSecureString; $p = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)).Trim(); if ($p.Length -gt 0) { & $env:AWS_EXE configure set aws_secret_access_key $p --profile $env:WEB_PERFIL; exit $LASTEXITCODE }; Write-Host '  Este dato es obligatorio.' }"
exit /b %ERRORLEVEL%

:buscar_aws_cli
REM Busca aws.exe version 2: carpeta guardada por el instalador, Program Files y PATH
set "AWS_CLI_CARPETA="
set "AWSCLI_TMP=%TEMP%\carmir_awscli_%RANDOM%.txt"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$dirs = @(); if ($env:WEB_CLI_CARPETA) { $dirs += $env:WEB_CLI_CARPETA }; $dirs += (Join-Path $env:ProgramFiles 'Amazon\AWSCLIV2'); $cmd = Get-Command aws.exe -ErrorAction SilentlyContinue; if ($cmd) { $dirs += (Split-Path $cmd.Source -Parent) }; $regP = ([Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')) -split ';'; foreach ($rp in $regP) { if ($rp) { $dirs += $rp.Trim() } }; foreach ($d in $dirs) { if ($d) { $ae = Join-Path $d 'aws.exe'; if (Test-Path -LiteralPath $ae) { $v = (& $ae --version 2>&1 | Out-String); if ($v -match 'aws-cli/2\.') { [IO.File]::WriteAllText($env:AWSCLI_TMP, $d, (New-Object Text.UTF8Encoding($false))); break } } } }" >nul 2>nul
if exist "%AWSCLI_TMP%" set /p AWS_CLI_CARPETA=<"%AWSCLI_TMP%"
if exist "%AWSCLI_TMP%" del /f /q "%AWSCLI_TMP%" >nul 2>nul
exit /b 0

:cancelado
echo Cancelado. No se subio nada; la pagina publicada sigue igual.
pause
exit /b 0

:falla_guardar
echo [FALLA] No se pudo guardar "%ARCHIVO_WEB%".
pause
exit /b 1

:falta_aws_cli
echo [FALLA] No se pudo instalar AWS CLI v2. Descargalo de https://awscli.amazonaws.com/AWSCLIV2.msi
pause
exit /b 1

:falla_llaves
echo [FALLA] No se pudieron guardar las llaves en el perfil "%WEB_PERFIL%".
pause
exit /b 1

:falta_node
echo [FALLA] No se encontro Node.js (npm). Ejecuta primero 1_INSTALAR_PRIMERA_VEZ.bat con la opcion 1 o 3.
pause
exit /b 1

:falla_build
echo [FALLA] La compilacion fallo. Revisa los errores de arriba.
pause
exit /b 1

:falla_s3
echo [FALLA] No se pudieron subir los archivos al bucket %WEB_BUCKET%.
echo         Revisa que el usuario IAM del perfil "%WEB_PERFIL%" tenga permiso sobre ese bucket.
pause
exit /b 1
