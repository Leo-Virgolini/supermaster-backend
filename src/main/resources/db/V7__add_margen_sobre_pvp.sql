-- =====================================================
-- Migración: Agregar margen_sobre_pvp y renombrar margen_porcentaje
-- Fecha: 2026-01-22
-- Descripción:
--   - Renombra margen_porcentaje a margen_sobre_ingreso_neto
--   - Agrega nuevo campo margen_sobre_pvp
-- =====================================================

-- Renombrar columna existente
ALTER TABLE producto_canal_precios
CHANGE COLUMN margen_porcentaje margen_sobre_ingreso_neto DECIMAL(6,2) NULL;

-- Agregar nueva columna
ALTER TABLE producto_canal_precios
ADD COLUMN margen_sobre_pvp DECIMAL(6,2) NULL AFTER margen_sobre_ingreso_neto;

-- =====================================================
-- Descripción de campos:
-- =====================================================
-- margen_sobre_ingreso_neto: (ganancia / ingresoNetoVendedor) × 100
--   Muestra qué % del ingreso neto es ganancia (rentabilidad real)
--
-- margen_sobre_pvp: (ganancia / pvp) × 100
--   Muestra qué % del PVP es ganancia (comparable con mercado)
