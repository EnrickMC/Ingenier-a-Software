# ApiCARMIR

API REST de la página web de CARMIR (Node.js 22 + Express 5 + mysql2). Ver `../ESTRUCTURA.md`.

1. `npm install`
2. Copia `.env.example` como `.env` y llena `DB_USUARIO` / `DB_CONTRASENA`.
   Recomendado: ejecuta `sql/crear_usuario_lectura.sql` como root y usa `carmir_web`.
3. `npm start` (o `npm run dev` para reiniciar al guardar cambios)
4. Para registrar pedidos (Módulo 2): ejecuta `sql/crear_usuario_pedidos.sql` como root y llena
   `DB_ESCRITURA_USUARIO` / `DB_ESCRITURA_CONTRASENA`. Sin ese usuario el catálogo funciona igual,
   pero «Enviar pedido» responde que el servicio no está disponible.
5. `npm test` (requiere la base con las semillas 3, 5 y 1)
6. `npm run test:pedidos` guarda pedidos de prueba: **solo** contra una base de prueba.

## Si la página dice «No fue posible cargar el catálogo»

- `npm run diagnostico`: revisa el `.env`, la conexión a MySQL, las tablas/vistas y cuántos
  productos cumplen cada condición para verse. Si algo falla, dice qué hacer.
- http://localhost:3000/api/salud: con la API encendida, responde `productosVisibles`.
- La página ya explica la causa: «La API no responde» (la API está apagada) o
  «no pudo conectarse a MySQL» (revisa el `.env`).

Un producto aparece en la página si: tiene nombre, «Mostrar en tienda» marcado,
su categoría está Activa y su precio (calculado de las recetas de sus opciones) es mayor a 0.
