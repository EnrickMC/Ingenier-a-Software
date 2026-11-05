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

## Estructura

Ver [ESTRUCTURA.md](ESTRUCTURA.md).
