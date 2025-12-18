-- =====================================================
-- MIGRACIÓN: Cambiar canal_concepto_cuota a ID PK
-- =====================================================
-- Cambia la clave primaria compuesta por un ID separado
-- y agrega un índice único para mantener la unicidad

USE supermaster;

SET SQL_SAFE_UPDATES = 0;

-- 1) Agregar columna id como AUTO_INCREMENT
ALTER TABLE canal_concepto_cuota
ADD COLUMN id BIGINT AUTO_INCREMENT FIRST;

-- 2) Eliminar la clave primaria compuesta actual
ALTER TABLE canal_concepto_cuota
DROP PRIMARY KEY;

-- 3) Establecer id como nueva clave primaria
ALTER TABLE canal_concepto_cuota
ADD PRIMARY KEY (id);

-- 4) Crear índice único para mantener la unicidad de (id_canal, cuotas, tipo)
ALTER TABLE canal_concepto_cuota
ADD UNIQUE KEY uk_canal_cuotas_tipo (id_canal, cuotas, tipo);

SET SQL_SAFE_UPDATES = 1;

-- Verificar la estructura final
-- DESCRIBE canal_concepto_cuota;
-- SHOW INDEX FROM canal_concepto_cuota;

