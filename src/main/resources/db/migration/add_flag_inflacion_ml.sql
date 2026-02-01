-- ============================================================
-- Migración: Agregar FLAG_INFLACION_ML al enum aplica_sobre
-- ============================================================

ALTER TABLE conceptos_calculo
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
        'FLAG_COMISION_ML',
        'FLAG_INFLACION_ML',
        'CALCULO_SOBRE_CANAL_BASE',
        'RECARGO_CUPON',
        'DESCUENTO_PORCENTUAL',
        'INFLACION_DIVISOR',
        'FLAG_APLICAR_PRECIO_INFLADO'
    ) NOT NULL;
