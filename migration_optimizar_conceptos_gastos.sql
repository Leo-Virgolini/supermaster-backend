-- ============================================================
-- OPTIMIZACIÓN: Sistema unificado para conceptos de gasto
-- ============================================================
-- 
-- SISTEMA UNIFICADO: Eliminación de canal_concepto, uso exclusivo de id_canal
-- 
-- Estrategia:
-- - Todos los conceptos se gestionan directamente en conceptos_gastos usando id_canal
-- - Conceptos globales: id_canal = NULL (aplican a todos los canales)
-- - Conceptos específicos de canal: id_canal = X (solo aplican a ese canal)
-- - Conceptos con cuotas: campo cuotas = "3", "6", "9", "12", etc.
--
-- Si un concepto necesita estar en varios canales específicos:
--   Se crean múltiples registros en conceptos_gastos (uno por canal)
--   Ejemplo: "3 CUOTAS" para NUBE y "3 CUOTAS" para ML son registros separados
--
-- VENTAJAS:
-- - Una sola fuente de verdad (conceptos_gastos)
-- - Consultas más simples y eficientes
-- - Sin redundancia ni ambigüedad
-- - Fácil de entender y mantener

-- Paso 1: Agregar campo id_canal si no existe
ALTER TABLE `conceptos_gastos`
ADD COLUMN IF NOT EXISTS `id_canal` INT NULL AFTER `cuotas`;

-- Paso 2: Agregar índice
CREATE INDEX IF NOT EXISTS `idx_conceptos_gastos_canal` ON `conceptos_gastos` (`id_canal`);

-- Paso 3: Agregar foreign key
ALTER TABLE `conceptos_gastos`
ADD CONSTRAINT IF NOT EXISTS `fk_conceptos_gastos_canal`
    FOREIGN KEY (`id_canal`) REFERENCES `canales` (`id_canal`)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- ============================================================
-- NOTAS DE USO:
-- ============================================================
-- 
-- CASO 1: Concepto global (todos los canales):
--   INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal)
--   VALUES ('MARKETING', 5.00, 'PVP', NULL);
--   -- Aplica a todos los canales
--
-- CASO 2: Concepto para un canal específico:
--   INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal)
--   VALUES ('COMISION ML', 14.00, 'PVP', 
--           (SELECT id_canal FROM canales WHERE canal = 'ML'));
--   -- Aplica solo al canal ML
--
-- CASO 3: Concepto con cuotas:
--   INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal, cuotas)
--   VALUES ('3 CUOTAS', 12.10, 'PVP', 
--           (SELECT id_canal FROM canales WHERE canal = 'NUBE'), '3');
--   -- Aplica a canal NUBE, solo cuando hay 3 cuotas
--
-- CASO 4: Concepto que aplica a varios canales específicos:
--   -- Crear un registro por cada canal (NO usar canal_concepto)
--   INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal) VALUES
--   ('Comisión Especial', 2.00, 'PVP', (SELECT id_canal FROM canales WHERE canal = 'ML')),
--   ('Comisión Especial', 2.00, 'PVP', (SELECT id_canal FROM canales WHERE canal = 'KT GASTRO'));
--   -- Dos registros separados, uno para cada canal

