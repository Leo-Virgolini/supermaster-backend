-- =====================================================
-- Migración: Renombrar valores de aplica_sobre
-- Fecha: 2026-01-22
-- Descripción: Actualiza los nombres del enum AplicaSobre
--              para hacerlos más autodescriptivos
-- =====================================================

-- Primero modificar el tipo ENUM en la columna para aceptar los nuevos valores
ALTER TABLE conceptos_gastos
MODIFY COLUMN aplica_sobre ENUM(
    -- Valores antiguos (para compatibilidad durante migración)
    'COSTO','PVP','COSTO_IVA','MARGEN_PTS','MARGEN_PROP','IMP','RECARGO_CUPON','DESCUENTO','ENVIO','INFLACION','PROVEEDOR_FIN','COSTO_GANANCIA','IVA','SOBRE_PVP_BASE','MARGEN_MINORISTA','MARGEN_MAYORISTA','PROMOCION',
    -- Valores nuevos
    'GASTO_SOBRE_COSTO','FLAG_FINANCIACION_PROVEEDOR','AJUSTE_MARGEN_PUNTOS','AJUSTE_MARGEN_PROPORCIONAL','FLAG_USAR_MARGEN_MINORISTA','FLAG_USAR_MARGEN_MAYORISTA','GASTO_POST_GANANCIA','FLAG_APLICAR_IVA','IMPUESTO_ADICIONAL','GASTO_POST_IMPUESTOS','FLAG_INCLUIR_ENVIO','COMISION_SOBRE_PVP','CALCULO_SOBRE_CANAL_BASE','DESCUENTO_PORCENTUAL','INFLACION_DIVISOR','FLAG_APLICAR_PROMOCIONES'
) DEFAULT 'COMISION_SOBRE_PVP';

-- Actualizar los valores existentes
UPDATE conceptos_gastos SET aplica_sobre = 'GASTO_SOBRE_COSTO' WHERE aplica_sobre = 'COSTO';
UPDATE conceptos_gastos SET aplica_sobre = 'FLAG_FINANCIACION_PROVEEDOR' WHERE aplica_sobre = 'PROVEEDOR_FIN';
UPDATE conceptos_gastos SET aplica_sobre = 'AJUSTE_MARGEN_PUNTOS' WHERE aplica_sobre = 'MARGEN_PTS';
UPDATE conceptos_gastos SET aplica_sobre = 'AJUSTE_MARGEN_PROPORCIONAL' WHERE aplica_sobre = 'MARGEN_PROP';
UPDATE conceptos_gastos SET aplica_sobre = 'FLAG_USAR_MARGEN_MINORISTA' WHERE aplica_sobre = 'MARGEN_MINORISTA';
UPDATE conceptos_gastos SET aplica_sobre = 'FLAG_USAR_MARGEN_MAYORISTA' WHERE aplica_sobre = 'MARGEN_MAYORISTA';
UPDATE conceptos_gastos SET aplica_sobre = 'GASTO_POST_GANANCIA' WHERE aplica_sobre = 'COSTO_GANANCIA';
UPDATE conceptos_gastos SET aplica_sobre = 'FLAG_APLICAR_IVA' WHERE aplica_sobre = 'IVA';
UPDATE conceptos_gastos SET aplica_sobre = 'IMPUESTO_ADICIONAL' WHERE aplica_sobre = 'IMP';
UPDATE conceptos_gastos SET aplica_sobre = 'GASTO_POST_IMPUESTOS' WHERE aplica_sobre = 'COSTO_IVA';
UPDATE conceptos_gastos SET aplica_sobre = 'FLAG_INCLUIR_ENVIO' WHERE aplica_sobre = 'ENVIO';
UPDATE conceptos_gastos SET aplica_sobre = 'COMISION_SOBRE_PVP' WHERE aplica_sobre = 'PVP';
UPDATE conceptos_gastos SET aplica_sobre = 'CALCULO_SOBRE_CANAL_BASE' WHERE aplica_sobre = 'SOBRE_PVP_BASE';
-- RECARGO_CUPON no cambia (ya es claro)
UPDATE conceptos_gastos SET aplica_sobre = 'DESCUENTO_PORCENTUAL' WHERE aplica_sobre = 'DESCUENTO';
UPDATE conceptos_gastos SET aplica_sobre = 'INFLACION_DIVISOR' WHERE aplica_sobre = 'INFLACION';
UPDATE conceptos_gastos SET aplica_sobre = 'FLAG_APLICAR_PROMOCIONES' WHERE aplica_sobre = 'PROMOCION';

-- Finalmente, remover los valores antiguos del ENUM (solo dejar los nuevos)
ALTER TABLE conceptos_gastos
MODIFY COLUMN aplica_sobre ENUM(
    'GASTO_SOBRE_COSTO',
    'FLAG_FINANCIACION_PROVEEDOR',
    'AJUSTE_MARGEN_PUNTOS',
    'AJUSTE_MARGEN_PROPORCIONAL',
    'FLAG_USAR_MARGEN_MINORISTA',
    'FLAG_USAR_MARGEN_MAYORISTA',
    'GASTO_POST_GANANCIA',
    'FLAG_APLICAR_IVA',
    'IMPUESTO_ADICIONAL',
    'GASTO_POST_IMPUESTOS',
    'FLAG_INCLUIR_ENVIO',
    'COMISION_SOBRE_PVP',
    'CALCULO_SOBRE_CANAL_BASE',
    'RECARGO_CUPON',
    'DESCUENTO_PORCENTUAL',
    'INFLACION_DIVISOR',
    'FLAG_APLICAR_PROMOCIONES'
) DEFAULT 'COMISION_SOBRE_PVP';

-- =====================================================
-- Mapeo de nombres (referencia):
-- =====================================================
-- COSTO              → GASTO_SOBRE_COSTO
-- PROVEEDOR_FIN      → FLAG_FINANCIACION_PROVEEDOR
-- MARGEN_PTS         → AJUSTE_MARGEN_PUNTOS
-- MARGEN_PROP        → AJUSTE_MARGEN_PROPORCIONAL
-- MARGEN_MINORISTA   → FLAG_USAR_MARGEN_MINORISTA
-- MARGEN_MAYORISTA   → FLAG_USAR_MARGEN_MAYORISTA
-- COSTO_GANANCIA     → GASTO_POST_GANANCIA
-- IVA                → FLAG_APLICAR_IVA
-- IMP                → IMPUESTO_ADICIONAL
-- COSTO_IVA          → GASTO_POST_IMPUESTOS
-- ENVIO              → FLAG_INCLUIR_ENVIO
-- PVP                → COMISION_SOBRE_PVP
-- SOBRE_PVP_BASE     → CALCULO_SOBRE_CANAL_BASE
-- RECARGO_CUPON      → RECARGO_CUPON (sin cambio)
-- DESCUENTO          → DESCUENTO_PORCENTUAL
-- INFLACION          → INFLACION_DIVISOR
-- PROMOCION          → FLAG_APLICAR_PROMOCIONES
