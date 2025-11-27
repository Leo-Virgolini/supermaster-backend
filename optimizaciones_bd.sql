-- ============================================
-- SCRIPT DE OPTIMIZACIÓN - BASE DE DATOS SUPERMASTER
-- MySQL 8.4.7
-- ============================================
-- Este script contiene las optimizaciones recomendadas
-- Ejecutar en orden y verificar resultados
-- ============================================

USE supermaster;

-- ============================================
-- 1. HACER VISIBLES LOS ÍNDICES INVISIBLES
-- ============================================
-- Los índices invisibles no se usan en consultas normales
-- Hacerlos visibles para mejorar el rendimiento

ALTER TABLE canal_concepto 
    ALTER INDEX fk_concepto_idx VISIBLE,
    ALTER INDEX fk_canal_idx VISIBLE;

ALTER TABLE producto_cliente 
    ALTER INDEX fk_id_cliente_idx VISIBLE;

-- Verificar que los índices ahora son visibles
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    IS_VISIBLE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'supermaster'
  AND TABLE_NAME IN ('canal_concepto', 'producto_cliente')
  AND INDEX_NAME IN ('fk_concepto_idx', 'fk_canal_idx', 'fk_id_cliente_idx')
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================
-- 2. ÍNDICE COMPUESTO PARA REGLAS_DESCUENTOS
-- ============================================
-- Optimiza consultas frecuentes: reglas activas por canal ordenadas por prioridad
-- Ejemplo: SELECT * FROM reglas_descuentos WHERE id_canal = ? AND activo = 1 ORDER BY prioridad DESC;

CREATE INDEX idx_rd_canal_activo_prioridad 
ON reglas_descuentos(id_canal, activo, prioridad DESC);

-- Verificar creación
SHOW INDEX FROM reglas_descuentos WHERE Key_name = 'idx_rd_canal_activo_prioridad';

-- ============================================
-- 3. ÍNDICE EN MLAS.MLA
-- ============================================
-- Si se busca productos por código MLA, este índice mejora el rendimiento

CREATE INDEX idx_mlas_mla ON mlas(mla);

-- Si el campo mla debe ser único, usar:
-- CREATE UNIQUE INDEX idx_mlas_mla_unique ON mlas(mla);

-- Verificar creación
SHOW INDEX FROM mlas WHERE Key_name = 'idx_mlas_mla';

-- ============================================
-- 4. ÍNDICE EN PRODUCTOS.COD_EXT
-- ============================================
-- Si se busca productos por código externo frecuentemente

CREATE INDEX idx_productos_cod_ext ON productos(cod_ext);

-- Verificar creación
SHOW INDEX FROM productos WHERE Key_name = 'idx_productos_cod_ext';

-- ============================================
-- 5. ÍNDICES FULLTEXT PARA BÚSQUEDAS DE TEXTO
-- ============================================
-- Mejora significativamente las búsquedas por descripción y título
-- Uso: SELECT * FROM productos WHERE MATCH(descripcion) AGAINST('texto' IN NATURAL LANGUAGE MODE);

-- Índice FULLTEXT en descripcion
ALTER TABLE productos 
    ADD FULLTEXT INDEX idx_ft_descripcion (descripcion);

-- Índice FULLTEXT en titulo_web
ALTER TABLE productos 
    ADD FULLTEXT INDEX idx_ft_titulo_web (titulo_web);

-- Índice FULLTEXT compuesto (opcional, si se buscan ambos campos juntos)
-- ALTER TABLE productos 
--     ADD FULLTEXT INDEX idx_ft_descripcion_titulo (descripcion, titulo_web);

-- Verificar creación
SHOW INDEX FROM productos WHERE Key_name LIKE 'idx_ft_%';

-- ============================================
-- 6. ÍNDICE EN FECHA_ULTIMO_CALCULO
-- ============================================
-- Si necesitas consultar precios calculados recientemente
-- Ejemplo: SELECT * FROM producto_canal_precios WHERE fecha_ultimo_calculo > DATE_SUB(NOW(), INTERVAL 1 DAY);

CREATE INDEX idx_pcp_fecha_calculo 
ON producto_canal_precios(fecha_ultimo_calculo DESC);

-- Verificar creación
SHOW INDEX FROM producto_canal_precios WHERE Key_name = 'idx_pcp_fecha_calculo';

-- ============================================
-- 7. VERIFICACIÓN FINAL
-- ============================================
-- Listar todos los índices creados
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    NON_UNIQUE,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'supermaster'
  AND INDEX_NAME IN (
    'idx_rd_canal_activo_prioridad',
    'idx_mlas_mla',
    'idx_productos_cod_ext',
    'idx_ft_descripcion',
    'idx_ft_titulo_web',
    'idx_pcp_fecha_calculo'
  )
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ============================================
-- 8. ESTADÍSTICAS DE TABLAS
-- ============================================
-- Actualizar estadísticas para que el optimizador use los nuevos índices
ANALYZE TABLE productos;
ANALYZE TABLE reglas_descuentos;
ANALYZE TABLE mlas;
ANALYZE TABLE producto_canal_precios;
ANALYZE TABLE canal_concepto;
ANALYZE TABLE producto_cliente;

-- ============================================
-- 9. VALIDACIÓN DE INTEGRIDAD
-- ============================================
-- Ejecutar el stored procedure de validación
CALL validar_integridad_supermaster();

-- ============================================
-- NOTAS IMPORTANTES
-- ============================================
-- 1. Los índices FULLTEXT requieren que la tabla use InnoDB (ya lo es)
-- 2. Los índices FULLTEXT solo funcionan con campos de tipo CHAR, VARCHAR o TEXT
-- 3. Después de crear índices, ejecutar ANALYZE TABLE para actualizar estadísticas
-- 4. Monitorear el rendimiento y ajustar según patrones de uso reales
-- 5. Si algún índice no se usa, considerarlo para eliminación (pero esperar un tiempo de observación)

