# Migración de CARMIR a AWS

Guía de lo que se ejecuta, en orden, para pasar CARMIR a AWS. La configuración de AWS
(buckets, CloudFront, EC2 `carmir-api`, RDS `BaseCarmir`, usuarios IAM y de MySQL) ya está hecha.

```
Clientes ──https──▶ CloudFront (d3vsfgax7m05ih.cloudfront.net)
                      ├─ /            ▶ S3 carmir-web-frontend-prod   (página Angular)
                      ├─ /imagenes/*  ▶ S3 carmir-media-catalogo-prod (fotos del catálogo)
                      └─ /api/*       ▶ EC2 carmir-api :3000 (ApiCARMIR) ──SSL──▶ RDS BaseCarmir (CARMIR_DB)
PC del negocio: Panel JavaFX ──túnel SSM (127.0.0.1:3307)──▶ EC2 ──▶ RDS
```

La forma de trabajar en tu computadora **no cambia**: `2_INICIAR_SISTEMA.bat` sigue usando MySQL local.
Todo lo de AWS se activa solo en la EC2 (variables de `/opt/carmir/api.env`) o al abrir el Panel con
`INICIAR_CARMIR_AWS.bat` (variable `CARMIR_MODO=aws`).

## 0. Antes de empezar

- [ ] Instantánea de RDS: RDS → BaseCarmir → Acciones → Tomar instantánea → `basecarmir-antes-de-migracion`.
- [ ] Ninguna contraseña real en Git (`db.local.properties`, `.env`, `~/.carmir/aws.properties` y `~/.aws/` están fuera).

## 1. Base de datos en RDS (desde la EC2, como `ec2-user`)

```bash
cd /opt/carmir/repo/Software_CARMIR/DBCarmir
mysql -h basecarmir.clsq6iea8ubs.us-east-2.rds.amazonaws.com -u admin -p --ssl-mode=REQUIRED < INSTALACION_COMPLETA.sql
mysql -h basecarmir.clsq6iea8ubs.us-east-2.rds.amazonaws.com -u admin -p --ssl-mode=REQUIRED < aws/PERMISOS_RDS.sql
```

- `INSTALACION_COMPLETA.sql` borra y vuelve a crear `CARMIR_DB` (trae las PARTES 1 a 10). Los permisos de
  `carmir_web`, `carmir_respaldo` y `carmir_panel` se conservan. El aviso «Sin usuario carmir_pedidos@localhost» es normal.
- `aws/PERMISOS_RDS.sql` da los permisos por tabla de `carmir_pedidos@'172.31.%'` y al final muestra la revisión:
  25+ tablas, 5 vistas y `NOW()` con la hora de León.
- Solo si se quiere una base de prueba: `semillas/POBLAR_BASE.sql` (no viene en esta rama).

## 2. ApiCARMIR en la EC2

```bash
# primera vez
git clone -b VersionOnline https://github.com/EnrickMC/Ingenier-a-Software.git /opt/carmir/repo
# después
cd /opt/carmir/repo && git pull

# agrega a /opt/carmir/api.env las variables nuevas (ver ApiCARMIR/despliegue/api.env.ejemplo):
#   NODE_ENV=production, DB_SSL_CA=/opt/carmir/global-bundle.pem, PROXIES_CONFIABLES=1
bash /opt/carmir/repo/Software_CARMIR/PaginaWebCARMIR/ApiCARMIR/despliegue/instalar_en_ec2.sh
```

El instalador descarga el certificado de RDS, instala dependencias, instala el servicio `carmir-api`
(systemd, arranca solo y se reinicia si se cae) y prueba la conexión.
Verificar: `https://d3vsfgax7m05ih.cloudfront.net/api/salud` responde `"estado":"ok"`.

Cambios de código: SSL a RDS (`DB_SSL_CA`), IP real del cliente detrás de CloudFront (`PROXIES_CONFIABLES`),
fotos `/imagenes/...` se mandan tal cual, y `npm run diagnostico` funciona sin `.env`.

## 3. Página web

Requisito: la API ya responde en `https://<dominio de CloudFront>/api/salud` (sección 2); sin ella la página
abre pero no muestra productos.

1. Una vez, en la consola de AWS → **IAM** → Usuarios → **Crear usuario** `carmir-despliegue` (sin acceso a la
   consola). Permisos → **Crear política insertada** → JSON (cambia el nombre del bucket si es otro):

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       { "Effect": "Allow", "Action": "s3:ListBucket",
         "Resource": "arn:aws:s3:::carmir-web-frontend-prod" },
       { "Effect": "Allow", "Action": ["s3:PutObject", "s3:DeleteObject"],
         "Resource": "arn:aws:s3:::carmir-web-frontend-prod/*" },
       { "Effect": "Allow", "Action": "cloudfront:CreateInvalidation", "Resource": "*" }
     ]
   }
   ```

   Nombre de la política: `carmir-publicar-web`. Luego Credenciales de seguridad → **Crear clave de acceso**
   → «Interfaz de línea de comandos (CLI)» y copia el Access Key ID y la Secret Access Key.
2. Doble clic en `Software_CARMIR/PaginaWebCARMIR/despliegue/PUBLICAR_WEB.bat`. La primera vez pregunta:
   perfil (`carmir-despliegue`), región, bucket de la página, ID de la distribución (CloudFront →
   Distribuciones → columna ID, empieza con `E`), dominio de CloudFront y las dos llaves (la secreta no se ve).
   Guarda los datos en `C:\Users\<usuario>\.carmir\web.aws.properties` y las llaves en el perfil de AWS CLI.
3. Compila, pide escribir **S** para confirmar, sube a S3 (`index.html` sin caché) y limpia la caché de
   CloudFront (`/*`). Las siguientes veces solo se abre, Enter y **S**.

Verificar: portada, `/catalogo` escrito directo, pastel 3D, carrito, pedido con comprobante y Mis pedidos.

## 4. Panel en cada PC del negocio

Nada de AWS queda fijo en el código ni en los `.bat`: todo se configura por PC con `1_INSTALAR_PRIMERA_VEZ.bat`.

1. Doble clic en **`1_INSTALAR_PRIMERA_VEZ.bat`** y elegir la opción **2** (solo el Panel con AWS) o **3**
   (sistema local y AWS). El archivo:
   - instala JDK 25, **AWS CLI v2** y **Session Manager Plugin**, y los agrega al PATH de esa misma ventana;
   - pide el perfil (por ejemplo `carmir-panel`), la región, el **Access Key ID** y la **Secret Access Key**
     del usuario IAM del Panel (la secreta no se ve al escribirla) y las guarda con `aws configure set`
     en `C:\Users\<usuario>\.aws\`;
   - comprueba las llaves con `aws sts get-caller-identity`;
   - pide el ID de la EC2 puente, el endpoint de RDS, puertos, base, usuario y contraseña de MySQL del Panel,
     el bucket de fotos y el dominio de CloudFront, y los guarda en `C:\Users\<usuario>\.carmir\aws.properties`
     (formato en `Software_CARMIR/PanelAdministrativoCARMIR/aws.properties.ejemplo`);
   - si la PC no tiene `clave-secretos.key`, ofrece copiarla desde otra PC.
   Para cambiar algún dato, se vuelve a ejecutar: Enter conserva cada valor guardado.
2. Copiar **la misma** `C:\Users\<usuario>\.carmir\clave-secretos.key` a todas las PC (los tokens de
   WhatsApp y la contraseña del correo se cifran con esa llave; con otra no se pueden leer).
3. Abrir con **`INICIAR_CARMIR_AWS.bat`** (raíz del repositorio): lee `aws.properties`, abre el túnel, abre el
   Panel y cierra el túnel al salir.
4. En el Panel: **Configuración → Respaldos y sistema → Conexión con AWS** muestra la configuración vigente
   (perfil, región, Access Key ID con la mitad oculta, si hay Secret guardada, EC2, RDS, túnel, base, usuario,
   bucket y dominio). Las contraseñas nunca se muestran. «Probar credenciales» llama a AWS con el perfil.

Qué cambia en modo AWS:
- Las fotos elegidas en Productos y Opciones se suben a S3 al guardar (`imagenes/postres/` y
  `imagenes/opciones/`) y se guarda la ruta relativa `/imagenes/...`. Se usa AWS CLI con el perfil y el bucket de `aws.properties`.
- Respaldos: `mysqldump` 8.4 con `--ssl-mode=REQUIRED --set-gtid-purged=OFF --no-tablespaces`; al restaurar
  se quitan los `DEFINER` del archivo. En `app.properties` → `respaldos.carpetaMysql` va la carpeta `bin`
  del ZIP de MySQL 8.4 (el cliente de XAMPP no sirve con RDS).
- Si el túnel corta una conexión inactiva, el Panel abre otra sola.

Verificar: iniciar sesión, ver un pedido hecho en la web, validar el pago y ver el comprobante, subir una
foto y verla en la web, enviar una notificación de prueba y hacer un respaldo.

## 5. Datos existentes

- Fotos que estaban como `C:\...`: abrir el producto u opción en el Panel (modo AWS), elegir la foto y guardar;
  se sube sola.
- Volver a capturar Parámetros, Cobertura y las cuentas de WhatsApp/correo en el Panel ya conectado a RDS.

## 6. Cierre

- Prueba completa (secciones 3 y 4) y revisar al día siguiente que el respaldo nocturno aparezca en
  `s3://carmir-privado-operaciones-prod/respaldos/`.
- Plan de regreso: restaurar `basecarmir-antes-de-migracion` o abrir el Panel con `2_INICIAR_SISTEMA.bat` (base local).
- Actualizar diagramas de despliegue, documento de pruebas y Diccionario de Datos si hubo PARTE nueva.
- Revisar el gasto del primer mes en Presupuestos.
