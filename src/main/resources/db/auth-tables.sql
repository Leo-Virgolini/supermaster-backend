-- =============================================
-- Sistema de Autenticación - Tablas y Seed Data
-- Schema: supermaster
-- =============================================

USE supermaster;

-- 1) Tabla de roles
CREATE TABLE roles (
    id_rol INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id_rol),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) Tabla de permisos
CREATE TABLE permisos (
    id_permiso INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id_permiso),
    CONSTRAINT uk_permisos_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Tabla intermedia rol_permiso
CREATE TABLE rol_permiso (
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (id_permiso) REFERENCES permisos(id_permiso) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) Tabla de usuarios
CREATE TABLE usuarios (
    id_usuario INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    id_rol INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- SEED DATA
-- =============================================

-- Permisos (11 módulos × 2 = 22 permisos)
INSERT INTO permisos (nombre, descripcion) VALUES
('PRODUCTOS_VER', 'Ver productos'),
('PRODUCTOS_EDITAR', 'Crear, editar y eliminar productos'),
('CANALES_VER', 'Ver canales y sus conceptos'),
('CANALES_EDITAR', 'Crear, editar y eliminar canales y sus conceptos'),
('PRECIOS_VER', 'Ver precios calculados, conceptos de cálculo y reglas de descuento'),
('PRECIOS_EDITAR', 'Gestionar precios, conceptos de cálculo y reglas de descuento'),
('MAESTROS_VER', 'Ver maestros (marcas, tipos, orígenes, etc.)'),
('MAESTROS_EDITAR', 'Crear, editar y eliminar maestros'),
('MLAS_VER', 'Ver datos de MercadoLibre'),
('MLAS_EDITAR', 'Gestionar datos de MercadoLibre'),
('EXCEL_VER', 'Exportar archivos Excel'),
('EXCEL_EDITAR', 'Importar archivos Excel'),
('INTEGRACIONES_VER', 'Ver integraciones (Dux, TiendaNube)'),
('INTEGRACIONES_EDITAR', 'Ejecutar integraciones (Dux, TiendaNube)'),
('ORDENES_COMPRA_VER', 'Ver órdenes de compra'),
('ORDENES_COMPRA_EDITAR', 'Gestionar órdenes de compra'),
('REPOSICION_VER', 'Ver reposición'),
('REPOSICION_EDITAR', 'Gestionar reposición'),
('CONFIGURACION_VER', 'Ver configuración'),
('CONFIGURACION_EDITAR', 'Editar configuración'),
('USUARIOS_VER', 'Ver usuarios'),
('USUARIOS_EDITAR', 'Gestionar usuarios');

-- Roles
INSERT INTO roles (nombre, descripcion) VALUES
('ADMIN', 'Administrador con acceso total'),
('OPERADOR', 'Operador con acceso a funciones operativas'),
('VISOR', 'Solo lectura');

-- ADMIN: todos los permisos (22)
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre = 'ADMIN';

-- OPERADOR: todos los VER + EDITAR operativos (sin USUARIOS_EDITAR ni CONFIGURACION_EDITAR)
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre = 'OPERADOR'
  AND (p.nombre LIKE '%_VER'
       OR p.nombre IN ('PRODUCTOS_EDITAR', 'CANALES_EDITAR', 'PRECIOS_EDITAR',
                        'MAESTROS_EDITAR', 'MLAS_EDITAR', 'EXCEL_EDITAR',
                        'INTEGRACIONES_EDITAR', 'ORDENES_COMPRA_EDITAR', 'REPOSICION_EDITAR'));

-- VISOR: solo permisos VER
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre = 'VISOR'
  AND p.nombre LIKE '%_VER';

-- Usuario admin inicial (password: admin123, BCrypt hash)
INSERT INTO usuarios (username, password_hash, nombre_completo, activo, id_rol)
SELECT 'admin', '$2a$10$SIeINKq2QxMYCTtcMdJdqek93cnzqK7fj61te5gy.a6CK/m08MAN.', 'Administrador', 1, r.id_rol
FROM roles r WHERE r.nombre = 'ADMIN';
