#!/bin/bash
# =====================================================================
# Instala o actualiza ApiCARMIR en la EC2 (Amazon Linux 2023).
#
# Uso (en la EC2, como ec2-user):
#   1. Trae el código a /opt/carmir/repo (la primera vez con git clone, después con git pull).
#   2. bash /opt/carmir/repo/Software_CARMIR/PaginaWebCARMIR/ApiCARMIR/despliegue/instalar_en_ec2.sh
#
# Qué hace:
#   - Copia la carpeta ApiCARMIR a /opt/carmir/ApiCARMIR (sin node_modules ni .env).
#   - Descarga el certificado de RDS a /opt/carmir/global-bundle.pem (solo la primera vez).
#   - Instala las dependencias de producción (npm ci --omit=dev).
#   - Instala el servicio carmir-api y lo reinicia.
#   - Revisa la conexión con npm run diagnostico y /api/salud.
# Requisito: /opt/carmir/api.env ya existe (ver api.env.ejemplo).
# =====================================================================
set -e

CARPETA_API_REPO="$(cd "$(dirname "$0")/.." && pwd)"
DESTINO=/opt/carmir/ApiCARMIR
CERTIFICADO=/opt/carmir/global-bundle.pem
VARIABLES=/opt/carmir/api.env

echo "== 1. Revisando /opt/carmir/api.env =="
if [ ! -f "$VARIABLES" ]; then
  echo "FALTA $VARIABLES. Créalo con el contenido de despliegue/api.env.ejemplo y chmod 600."
  exit 1
fi
chmod 600 "$VARIABLES"

echo "== 2. Certificado de Amazon RDS =="
if [ ! -f "$CERTIFICADO" ]; then
  curl -sSf -o "$CERTIFICADO" https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
fi
echo "Certificado en $CERTIFICADO"

echo "== 3. Copiando el código a $DESTINO =="
mkdir -p "$DESTINO"
if ! command -v rsync > /dev/null; then
  sudo dnf install -y rsync
fi
# rsync deja la carpeta igual a la del repositorio, sin tocar node_modules
rsync -a --delete --exclude node_modules --exclude .env "$CARPETA_API_REPO/" "$DESTINO/"

echo "== 4. Dependencias de producción =="
cd "$DESTINO"
npm ci --omit=dev

echo "== 5. Servicio carmir-api =="
sudo cp "$DESTINO/despliegue/carmir-api.service" /etc/systemd/system/carmir-api.service
sudo systemctl daemon-reload
sudo systemctl enable carmir-api
sudo systemctl restart carmir-api

echo "== 6. Diagnóstico =="
set -a
source "$VARIABLES"
set +a
npm run diagnostico || true
sleep 3
echo "Respuesta de /api/salud:"
curl -s http://127.0.0.1:3000/api/salud
echo
echo "Listo. Mensajes de la API: sudo journalctl -u carmir-api -n 50"
