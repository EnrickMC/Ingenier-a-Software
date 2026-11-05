@echo off
setlocal EnableExtensions DisableDelayedExpansion
title CARMIR - Restablecer contrasena de root de MySQL
cd /d "%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%PS_EXE%" set "PS_EXE=powershell"

REM Uso:
REM   RESTABLECER_ROOT_MYSQL.bat            Cambia la contrasena de root de MySQL a "admin"
REM   RESTABLECER_ROOT_MYSQL.bat /verificar Solo revisa si root/admin entra (sale con 0 o 1)

set "MODO=restablecer"
if /i "%~1"=="/verificar" set "MODO=verificar"

REM Para restablecer se necesitan permisos de administrador (detener el servicio MySQL)
if "%MODO%"=="verificar" goto :preparar_script
net session >nul 2>nul
if not errorlevel 1 goto :preparar_script
echo [INFO] Windows te pedira permiso de administrador para reiniciar MySQL. Haz clic en "Si".
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath $env:ComSpec -ArgumentList '/c', ([char]34 + '%~f0' + [char]34) -Verb RunAs -Wait"
exit /b %ERRORLEVEL%

:preparar_script
set "CARMIR_BAT_FILE=%~f0"
set "CARMIR_PS_TMP=%TEMP%\carmir_root_%RANDOM%_%RANDOM%.ps1"
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -Command "$lines = [IO.File]::ReadAllLines($env:CARMIR_BAT_FILE, [Text.Encoding]::ASCII); $idx = [Array]::IndexOf($lines, 'REM === CARMIR_PS_START ==='); if ($idx -ge 0) { [IO.File]::WriteAllLines($env:CARMIR_PS_TMP, $lines[($idx + 1)..($lines.Length - 1)], [Text.Encoding]::ASCII) }"
if not exist "%CARMIR_PS_TMP%" exit /b 1
"%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%CARMIR_PS_TMP%" %MODO%
set "RESULTADO=%ERRORLEVEL%"
del /f /q "%CARMIR_PS_TMP%" >nul 2>nul
if "%MODO%"=="restablecer" pause
exit /b %RESULTADO%

REM === CARMIR_PS_START ===
param([string]$modo = 'restablecer')
$nuevaClave = 'admin'

# Buscar el servicio de MySQL / MariaDB y su carpeta bin
$svc = Get-CimInstance Win32_Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^(MySQL|MariaDB)' } | Sort-Object { $_.State -ne 'Running' } | Select-Object -First 1
if ($null -eq $svc) {
    Write-Host '[ERROR] No se encontro un servicio de MySQL o MariaDB en este equipo.'
    exit 1
}
if ($svc.PathName -notmatch '(?i)^\W?(.+?\\bin)\\([^\\]+?\.exe)') {
    Write-Host ('[ERROR] No se pudo leer la ruta del servicio: ' + $svc.PathName)
    exit 1
}
$bin = $Matches[1]
$mysqld = Join-Path $bin $Matches[2]
$mysql = Join-Path $bin 'mysql.exe'
$mysqladmin = Join-Path $bin 'mysqladmin.exe'

# Archivo my.ini que usa el servicio (si lo tiene)
$ini = $null
if ($svc.PathName -match '--defaults-file="([^"]+)"') {
    $ini = $Matches[1]
} elseif ($svc.PathName -match '--defaults-file=(\S+)') {
    $ini = $Matches[1]
}

function Probar-Root {
    $p = Start-Process -FilePath $mysql -ArgumentList '-u', 'root', ('-p' + $nuevaClave), '-h', '127.0.0.1', '-P', '3306', '-e', '"SELECT 1;"' -NoNewWindow -Wait -PassThru -RedirectStandardOutput "$env:TEMP\carmir_root_out.txt" -RedirectStandardError "$env:TEMP\carmir_root_err.txt"
    return ($p.ExitCode -eq 0)
}

if ($modo -eq 'verificar') {
    if (Probar-Root) { exit 0 }
    exit 1
}

Write-Host ('Servicio encontrado: ' + $svc.Name + '  (' + $mysqld + ')')
if (Probar-Root) {
    Write-Host '[OK] root ya entra con la contrasena "admin". No hace falta cambiar nada.'
    exit 0
}

# Archivo SQL que MySQL ejecuta al arrancar para cambiar la contrasena
$init = Join-Path $env:TEMP 'carmir_restablecer_root.sql'
$sql = @(
    "ALTER USER 'root'@'localhost' IDENTIFIED BY '$nuevaClave';",
    "ALTER USER IF EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$nuevaClave';",
    "ALTER USER IF EXISTS 'root'@'::1' IDENTIFIED BY '$nuevaClave';",
    "ALTER USER IF EXISTS 'root'@'%' IDENTIFIED BY '$nuevaClave';"
)
[IO.File]::WriteAllLines($init, $sql, [Text.Encoding]::ASCII)

Write-Host '[1/4] Deteniendo el servicio de MySQL...'
Stop-Service -Name $svc.Name -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

Write-Host '[2/4] Arrancando MySQL un momento para cambiar la contrasena de root...'
$argumentos = @()
if ($ini) { $argumentos += ('"--defaults-file=' + $ini + '"') }
$argumentos += ('"--init-file=' + $init + '"')
$argumentos += '--console'
$temporal = Start-Process -FilePath $mysqld -ArgumentList $argumentos -WindowStyle Hidden -PassThru
$listo = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    if (Probar-Root) { $listo = $true; break }
}

Write-Host '[3/4] Apagando el MySQL temporal...'
if ($listo) {
    & $mysqladmin -u root ('-p' + $nuevaClave) -h 127.0.0.1 -P 3306 shutdown 2>$null
    $temporal.WaitForExit(20000) | Out-Null
}
if (-not $temporal.HasExited) { Stop-Process -Id $temporal.Id -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 3 }
Remove-Item -LiteralPath $init -Force -ErrorAction SilentlyContinue

Write-Host '[4/4] Encendiendo de nuevo el servicio de MySQL...'
Start-Service -Name $svc.Name -ErrorAction SilentlyContinue
Start-Sleep -Seconds 4

if (Probar-Root) {
    Write-Host ''
    Write-Host '[OK] Listo. Ahora root entra con la contrasena "admin".'
    Write-Host '     Ya puedes abrir 2_INICIAR_SISTEMA.bat.'
    exit 0
}
Write-Host ''
Write-Host '[ERROR] No se pudo cambiar la contrasena. Esto respondio MySQL:'
Get-Content "$env:TEMP\carmir_root_err.txt" -ErrorAction SilentlyContinue
exit 1
