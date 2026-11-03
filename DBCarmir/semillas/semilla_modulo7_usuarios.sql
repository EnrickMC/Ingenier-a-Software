-- =====================================================================
-- CARMIR · Datos semilla del Módulo 7 (Gestión de Usuarios)
-- OPCIONAL: el panel da de alta los permisos solo al abrir "Usuarios y Permisos".
-- La Administradora NO está en la base de datos: su usuario y contraseña están
-- fijos en el código (AdministradoraSistema.java). Por defecto: admin / admin.
-- Ejecutar DESPUÉS de CARMIR_BaseDeDatos.sql. No cambia ninguna tabla,
-- solo agrega datos. Se puede ejecutar varias veces sin duplicar nada.
--
-- Crea:
--   1. Los permisos del sistema (los mismos de la clase Permisos.java).
--   2. Un usuario de prueba del Personal (puedes darlo de baja después).
--        usuario: maria        contraseña temporal: Maria2026
--
-- Las contraseñas van cifradas con PBKDF2 (formato pbkdf2$iteraciones$sal$hash),
-- igual que las genera el panel en HashContrasena.java.
-- =====================================================================
USE CARMIR_DB;

-- 1. Permisos -----------------------------------------------------------
INSERT IGNORE INTO Permiso (NombrePermiso) VALUES
  ('Gestionar usuarios'),
  ('Gestionar pedidos'),
  ('Ver agenda de producción'),
  ('Validar pagos'),
  ('Gestionar catálogo'),
  ('Gestionar productos'),
  ('Gestionar insumos'),
  ('Gestionar recetas'),
  ('Ver datos financieros'),
  ('Configurar sistema');

-- 2. Usuario de prueba del Personal --------------------------------------
INSERT IGNORE INTO Usuario (NombreCompleto, NombreUsuario, `Contraseña`, EstadoUsuario, IntentosFallidos)
VALUES ('María López (prueba)', 'maria', 'pbkdf2$120000$w4y+r20Zk7UKzJAUPsyCWQ==$R4IrJnYJ9RRMdbRyrxQ2KwWMBuNcnDbq1UxIiIiTDGw=', 'Activo', 0);

-- Permisos de ejemplo: pedidos, agenda, insumos y recetas
INSERT IGNORE INTO UsuarioPermiso (IdUsuario, IdPermiso)
SELECT u.IdUsuario, p.IdPermiso
FROM Usuario u JOIN Permiso p
  ON p.NombrePermiso IN ('Gestionar pedidos', 'Ver agenda de producción', 'Gestionar insumos', 'Gestionar recetas')
WHERE u.NombreUsuario = 'maria';

-- Revisión rápida
SELECT u.NombreUsuario, u.EstadoUsuario, COUNT(up.IdPermiso) AS Permisos
FROM Usuario u LEFT JOIN UsuarioPermiso up ON up.IdUsuario = u.IdUsuario
GROUP BY u.IdUsuario, u.NombreUsuario, u.EstadoUsuario;
