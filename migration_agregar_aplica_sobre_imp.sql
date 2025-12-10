-- ============================================================
-- MIGRACIÓN: Agregar tipo IMP a aplica_sobre para conceptos de impuestos
-- ============================================================
-- 
-- Este cambio permite almacenar conceptos como IIBB que se suman
-- directamente al factor de impuestos (IMP = 1 + IVA/100 + concepto/100)
--
-- Ejemplo de uso:
-- INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal, id_tipo)
-- VALUES ('IIBB', 5.00, 'IMP', NULL, NULL);
-- -- IIBB aplica a todos los canales y tipos, se suma al factor IMP

-- Paso 1: Modificar el ENUM para incluir 'IMP'
ALTER TABLE `conceptos_gastos`
MODIFY COLUMN `aplica_sobre` ENUM('COSTO','PVP','COSTO_IVA','COSTO_MARGEN','IMP') DEFAULT 'PVP';

-- Paso 2: Insertar IIBB como concepto global (opcional, si no existe)
-- INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal, id_tipo)
-- VALUES ('IIBB', 5.00, 'IMP', NULL, NULL)
-- ON DUPLICATE KEY UPDATE porcentaje = 5.00, aplica_sobre = 'IMP';

