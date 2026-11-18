# PaginaWebCARMIR

Página web para clientes de CARMIR (Angular 22) y su API (`ApiCARMIR`, Node.js + Express).
El navegador **nunca** se conecta a MySQL: la página llama a la API y la API consulta la base.

```
Navegador (Angular, puerto 4200)  →  ApiCARMIR (Express, puerto 3000)  →  MySQL (CARMIR_DB)
```

## Requisitos

- Node.js 22 o superior (`node -v`)
- MySQL 8 con la base `CARMIR_DB` creada con `DBCarmir/CARMIR_BaseDeDatos.sql` + `DBCarmir/migraciones/MIGRACION_COMPLETA.sql`
- Para ver productos: semillas 3 → 5 → 1 (`DBCarmir/semillas/`)
- Para pedidos (Módulo 2): ejecutar una vez `ApiCARMIR/sql/crear_usuario_pedidos.sql` como root (no cambia tablas)

## Cómo ejecutarlo (dos terminales)

**Terminal 1 · API**

```bash
cd PaginaWebCARMIR/ApiCARMIR
npm install
copy .env.example .env        # en Windows (en Mac/Linux: cp .env.example .env)
# abre .env y escribe DB_USUARIO y DB_CONTRASENA (lectura)
# y DB_ESCRITURA_USUARIO / DB_ESCRITURA_CONTRASENA (carmir_pedidos) para poder registrar pedidos
npm start
```

Debe decir `Conexión a MySQL correcta` y `Usuario de pedidos correcto (carmir_pedidos)`. Prueba: http://localhost:3000/api/salud

**Terminal 2 · Página web**

```bash
cd PaginaWebCARMIR
npm install
npm start
```

Abre http://localhost:4200

## Pruebas

```bash
npm test                      # Angular (Vitest)
cd ApiCARMIR && npm test      # API (necesita la base con las semillas 3, 5 y 1)
npm run test:pedidos          # (en ApiCARMIR) GUARDA pedidos de prueba: úsalo solo con una base de prueba
```

## Publicar en AWS

La página se publica en un bucket de S3 y la sirve CloudFront. En el mismo dominio CloudFront manda
`/api/*` a la ApiCARMIR de la EC2 y `/imagenes/*` al bucket de fotos, por eso `environment.ts` usa
`apiUrl: '/api'` y no hay ningún dato de AWS dentro del código.

Doble clic en `despliegue/PUBLICAR_WEB.bat`. La primera vez pregunta el perfil de AWS CLI, la región,
el bucket, el ID y el dominio de la distribución de CloudFront (los guarda en
`C:\Users\<usuario>\.carmir\web.aws.properties`) y, si el perfil no tiene llaves, las pide. Después
compila, pide confirmar, sube a S3 (`index.html` sin caché) y limpia la caché de CloudFront.
El usuario IAM que publica necesita los permisos de la sección 3 de `MIGRACION_AWS.md`.

Las fotos que el Panel sube a S3 se guardan como `/imagenes/...`. En producción se piden al mismo
dominio. Con `ng serve` no se ven, salvo que escribas tu dominio de CloudFront en `dominioImagenes`
de `environment.development.ts` (no lo subas a Git).

## Estructura

Ver [ESTRUCTURA.md](ESTRUCTURA.md).
