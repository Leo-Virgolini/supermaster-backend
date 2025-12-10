-- ============================================================
-- MIGRACIÓN: Crear canal NUBE y asignar KT HOGAR y KT GASTRO como hijos
-- ============================================================
-- 
-- Esta migración crea la jerarquía de canales:
-- - NUBE (canal padre)
--   - KT HOGAR (canal hijo)
--   - KT GASTRO (canal hijo)
--
-- Los conceptos asignados a NUBE se aplicarán automáticamente
-- a KT HOGAR y KT GASTRO.

-- Paso 1: Crear el canal NUBE (si no existe)
INSERT INTO `canales` (`canal`, `id_canal_base`)
SELECT 'NUBE', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `canales` WHERE `canal` = 'NUBE'
);

-- Paso 2: Obtener el ID del canal NUBE
SET @id_nube = (SELECT `id_canal` FROM `canales` WHERE `canal` = 'NUBE' LIMIT 1);

-- Paso 3: Asignar KT HOGAR como hijo de NUBE
UPDATE `canales`
SET `id_canal_base` = @id_nube
WHERE `canal` = 'KT HOGAR'
  AND (`id_canal_base` IS NULL OR `id_canal_base` != @id_nube);

-- Paso 4: Asignar KT GASTRO como hijo de NUBE
UPDATE `canales`
SET `id_canal_base` = @id_nube
WHERE `canal` = 'KT GASTRO'
  AND (`id_canal_base` IS NULL OR `id_canal_base` != @id_nube);

-- ============================================================
-- NOTAS DE USO:
-- ============================================================
-- 
-- Ahora puedes asignar conceptos a NUBE y se aplicarán automáticamente
-- a KT HOGAR y KT GASTRO:
--
-- Ejemplo: Asignar concepto "MP" a NUBE (aplica a ambos hijos)
-- INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal)
-- VALUES ('MP', 5.00, 'PVP', @id_nube);
--
-- Ejemplo: Asignar concepto específico solo a KT HOGAR
-- INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal)
-- VALUES ('Concepto Específico', 2.00, 'PVP', 
--         (SELECT id_canal FROM canales WHERE canal = 'KT HOGAR'));

