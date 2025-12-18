-- MIGRACIÓN: Agregar campo porcentaje_retencion a canales
-- ============================================================
-- 
-- Este campo permite configurar el porcentaje de gasto fijo para el cálculo de PVP
-- Por defecto es 0 (no afecta el cálculo)
-- Para ML se puede configurar como 10 (10% de gasto fijo, equivalente a dividir por 0.9)
--
-- La fórmula de conversión es: divisor = 1 - (porcentaje / 100)
-- Ejemplo: 10% de gasto → divisor = 1 - 0.10 = 0.9
--
-- Ejemplo:
--   UPDATE canales SET porcentaje_retencion = 10 WHERE canal LIKE '%ML%';
--   UPDATE canales SET porcentaje_retencion = 0 WHERE canal NOT LIKE '%ML%';

-- Paso 1: Agregar el campo porcentaje_retencion
ALTER TABLE `canales`
ADD COLUMN `porcentaje_retencion` DECIMAL(5,2) NULL DEFAULT 0 
    COMMENT 'Porcentaje de gasto fijo para el cálculo de PVP. Por defecto 0. Para ML típicamente 10 (10% de gasto fijo)';

-- Paso 2: Actualizar canales ML con el valor 10 por defecto
UPDATE `canales`
SET `porcentaje_retencion` = 10
WHERE `canal` LIKE '%ML%' AND `porcentaje_retencion` IS NULL;

-- Paso 3: Asegurar que todos los demás canales tengan 0
UPDATE `canales`
SET `porcentaje_retencion` = 0
WHERE `porcentaje_retencion` IS NULL;

