-- ============================================================
-- Migración: Renombrar Promocion → PrecioInflado
-- ============================================================

-- 1. Renombrar tablas
RENAME TABLE promociones TO precios_inflados;
RENAME TABLE producto_canal_promocion TO producto_canal_precio_inflado;

-- 2. Renombrar FK en producto_canal_precio_inflado
ALTER TABLE producto_canal_precio_inflado
    DROP FOREIGN KEY producto_canal_promocion_ibfk_3;

ALTER TABLE producto_canal_precio_inflado
    CHANGE COLUMN id_promocion id_precio_inflado INT NOT NULL;

ALTER TABLE producto_canal_precio_inflado
    ADD CONSTRAINT producto_canal_precio_inflado_ibfk_3
        FOREIGN KEY (id_precio_inflado) REFERENCES precios_inflados(id);

-- 3. Actualizar enum aplica_sobre en conceptos_calculo
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
        'CALCULO_SOBRE_CANAL_BASE',
        'RECARGO_CUPON',
        'DESCUENTO_PORCENTUAL',
        'INFLACION_DIVISOR',
        'FLAG_APLICAR_PROMOCIONES',
        'FLAG_APLICAR_PRECIO_INFLADO'
    ) NOT NULL;

UPDATE conceptos_calculo
SET aplica_sobre = 'FLAG_APLICAR_PRECIO_INFLADO'
WHERE aplica_sobre = 'FLAG_APLICAR_PROMOCIONES';

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
        'CALCULO_SOBRE_CANAL_BASE',
        'RECARGO_CUPON',
        'DESCUENTO_PORCENTUAL',
        'INFLACION_DIVISOR',
        'FLAG_APLICAR_PRECIO_INFLADO'
    ) NOT NULL;

-- 4. Renombrar concepto y descripción en datos
UPDATE conceptos_calculo
SET concepto = 'PRECIO INFLADO',
    descripcion = 'SI EL CANAL LO TIENE, EL PRODUCTO APLICA EL VALOR DE PRECIO INFLADO'
WHERE concepto = 'PROMOCION';
