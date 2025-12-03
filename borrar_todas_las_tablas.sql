-- ============================================================
-- Script para BORRAR TODO el contenido de todas las tablas
-- ============================================================
-- ⚠️ ADVERTENCIA: Este script eliminará TODOS los datos de la base de datos
-- Ejecutar solo si estás seguro de querer borrar todo
-- ============================================================

USE supermaster;

-- Deshabilitar verificación de claves foráneas temporalmente
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. ELIMINAR TABLAS DE RELACIÓN (tablas hijas)
-- ============================================================
-- Estas tablas tienen claves foráneas hacia otras tablas

TRUNCATE TABLE producto_canal;
TRUNCATE TABLE producto_catalogo;
TRUNCATE TABLE producto_cliente;
TRUNCATE TABLE producto_apto;
TRUNCATE TABLE producto_canal_precios;
TRUNCATE TABLE mlas;
TRUNCATE TABLE canal_concepto;

-- ============================================================
-- 2. ELIMINAR TABLA PRINCIPAL DE PRODUCTOS
-- ============================================================
-- Esta tabla tiene muchas referencias a otras tablas

TRUNCATE TABLE productos;

-- ============================================================
-- 3. ELIMINAR TABLAS MAESTRAS (sin dependencias)
-- ============================================================

TRUNCATE TABLE marcas;
TRUNCATE TABLE tipos;
TRUNCATE TABLE origenes;
TRUNCATE TABLE clasif_gral;
TRUNCATE TABLE clasif_gastro;
TRUNCATE TABLE materiales;
TRUNCATE TABLE catalogos;
TRUNCATE TABLE canales;
TRUNCATE TABLE proveedores;
TRUNCATE TABLE clientes;
TRUNCATE TABLE aptos;
TRUNCATE TABLE impuestos;
TRUNCATE TABLE conceptos_gastos;
TRUNCATE TABLE reglas_descuentos;

-- Habilitar nuevamente la verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 4. REINICIAR AUTO_INCREMENT de todas las tablas
-- ============================================================

ALTER TABLE productos AUTO_INCREMENT = 1;
ALTER TABLE marcas AUTO_INCREMENT = 1;
ALTER TABLE tipos AUTO_INCREMENT = 1;
ALTER TABLE origenes AUTO_INCREMENT = 1;
ALTER TABLE clasif_gral AUTO_INCREMENT = 1;
ALTER TABLE clasif_gastro AUTO_INCREMENT = 1;
ALTER TABLE materiales AUTO_INCREMENT = 1;
ALTER TABLE catalogos AUTO_INCREMENT = 1;
ALTER TABLE canales AUTO_INCREMENT = 1;
ALTER TABLE proveedores AUTO_INCREMENT = 1;
ALTER TABLE clientes AUTO_INCREMENT = 1;
ALTER TABLE aptos AUTO_INCREMENT = 1;
ALTER TABLE impuestos AUTO_INCREMENT = 1;
ALTER TABLE conceptos_gastos AUTO_INCREMENT = 1;
ALTER TABLE reglas_descuentos AUTO_INCREMENT = 1;
ALTER TABLE mlas AUTO_INCREMENT = 1;
ALTER TABLE producto_canal AUTO_INCREMENT = 1;
ALTER TABLE producto_catalogo AUTO_INCREMENT = 1;
ALTER TABLE producto_canal_precios AUTO_INCREMENT = 1;
ALTER TABLE producto_cliente AUTO_INCREMENT = 1;
ALTER TABLE producto_apto AUTO_INCREMENT = 1;
ALTER TABLE canal_concepto AUTO_INCREMENT = 1;

-- ============================================================
-- ✅ COMPLETADO
-- ============================================================
-- Todas las tablas han sido vaciadas y los AUTO_INCREMENT reiniciados
-- Los próximos registros insertados empezarán con ID = 1

