-- Migración para agregar soporte de conceptos de gasto condicionales por canal
-- Esto permite conceptos que se aplican solo a ciertos canales

ALTER TABLE `conceptos_gastos`
ADD COLUMN `id_canal` INT NULL AFTER `cuotas`;

-- Agregar índices para mejorar el rendimiento de las consultas
CREATE INDEX `idx_conceptos_gastos_canal` ON `conceptos_gastos` (`id_canal`);

-- Agregar foreign key
ALTER TABLE `conceptos_gastos`
ADD CONSTRAINT `fk_conceptos_gastos_canal`
    FOREIGN KEY (`id_canal`) REFERENCES `canales` (`id_canal`)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

-- Comentarios sobre cómo usar los campos:
-- 
-- IMPORTANTE: Hay DOS formas de asociar conceptos a canales:
-- 1. A través de la tabla canal_concepto (relación explícita)
-- 2. A través del campo id_canal en conceptos_gastos (conceptos condicionales)
--
-- Un concepto NO debe estar en ambas. Si está en canal_concepto, se ignora id_canal.
--
-- Casos de uso con id_canal:
-- - id_canal = NULL: Concepto global (aplica a todos los canales)
-- - id_canal = X: Concepto específico del canal X
--
-- Ejemplo: Para crear un concepto que aplica solo a canal NUBE:
-- INSERT INTO conceptos_gastos (concepto, porcentaje, aplica_sobre, id_canal)
-- VALUES ('Comisión NUBE', 1.00, 'PVP', 
--         (SELECT id_canal FROM canales WHERE canal = 'NUBE'));

