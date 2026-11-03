-- =====================================================================
-- CARMIR · Usuario de MySQL para la API de la página web (SOLO LECTURA)
--
-- Por qué: la página web es pública. Si la API usara root y alguien encontrara
-- una falla, podría borrar o cambiar datos. Con este usuario la API solo puede
-- LEER la base CARMIR_DB, aunque algo saliera mal.
--
-- Cómo usarlo (una sola vez, como root):
--   1. Cambia 'CambiaEstaContrasena2026' por una contraseña tuya.
--   2. Ejecuta el archivo en MySQL Workbench (rayo) o:  mysql -u root -p < crear_usuario_lectura.sql
--   3. Pon el mismo usuario y contraseña en ApiCARMIR/.env (DB_USUARIO y DB_CONTRASENA).
--
-- Cuando se programe el Módulo 2 (pedidos) se dará permiso de escritura solo
-- en las tablas que lo necesiten, en un script aparte.
-- =====================================================================
CREATE USER IF NOT EXISTS 'carmir_web'@'localhost' IDENTIFIED BY 'CambiaEstaContrasena2026';
CREATE USER IF NOT EXISTS 'carmir_web'@'127.0.0.1' IDENTIFIED BY 'CambiaEstaContrasena2026';

-- Solo SELECT sobre la base del sistema (incluye las vistas)
GRANT SELECT ON CARMIR_DB.* TO 'carmir_web'@'localhost';
GRANT SELECT ON CARMIR_DB.* TO 'carmir_web'@'127.0.0.1';

-- Revisión: debe mostrar solo "GRANT SELECT"
SHOW GRANTS FOR 'carmir_web'@'localhost';
