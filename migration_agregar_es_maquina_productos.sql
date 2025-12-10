-- ============================================================
-- MIGRACIÓN: Agregar campo es_maquina a la tabla productos
-- ============================================================
-- 
-- Este campo permite identificar explícitamente si un producto
-- es una máquina, lo cual afecta qué conceptos de gasto se aplican
-- en los canales NUBE (KT HOGAR y KT GASTRO).
--
-- Para máquinas en NUBE: se excluyen los conceptos EMBALAJE y NUBE
-- Para no máquinas en NUBE: se aplican todos los conceptos normalmente

-- Paso 1: Agregar el campo es_maquina
ALTER TABLE `productos`
ADD COLUMN `es_maquina` BOOLEAN DEFAULT FALSE AFTER `es_combo`;

-- Paso 2: Agregar índice para mejorar consultas
CREATE INDEX IF NOT EXISTS `idx_productos_es_maquina` ON `productos` (`es_maquina`);

-- Paso 3: Actualizar productos existentes que sean máquinas (ajustar según tus datos)
-- Ejemplo: si tienes productos con un tipo específico que son máquinas
-- UPDATE `productos` p
-- INNER JOIN `tipos` t ON p.id_tipo = t.id_tipo
-- SET p.es_maquina = TRUE
-- WHERE t.nombre LIKE '%MAQUINA%';

