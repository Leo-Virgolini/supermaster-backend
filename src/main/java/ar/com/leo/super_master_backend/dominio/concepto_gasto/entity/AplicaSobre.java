package ar.com.leo.super_master_backend.dominio.concepto_gasto.entity;

/**
 * Enum que representa los valores posibles para el campo aplica_sobre
 * en la tabla conceptos_gastos.
 * 
 * Valores permitidos según la base de datos:
 * - COSTO: Se aplica sobre el costo base (se suma al costo antes de ganancia)
 * - PVP: Se aplica sobre el precio de venta (se resta del PVP)
 *          NOTA: Para conceptos de cuotas (con campo cuotas != NULL), usar PVP.
 *                Estos conceptos se procesan de manera especial como divisor sobre el PVP base.
 * - COSTO_IVA: Se aplica sobre el costo después de aplicar IVA (se multiplica después de IMP)
 * - COSTO_MARGEN: Se aplica sobre el costo después de aplicar margen (se multiplica después de ganancia)
 *                  NOTA: Deprecado. Usar AUMENTA_MARGEN o REDUCE_MARGEN según corresponda.
 * - AUMENTA_MARGEN: Suma puntos porcentuales directamente al margen: GAN.MIN.ML + AUMENTA_MARGEN
 *                   Ejemplo: Si GAN.MIN.ML = 60% y AUMENTA_MARGEN = 25%, entonces ganancia = 60% + 25% = 85%
 * - REDUCE_MARGEN: Resta puntos porcentuales directamente del margen: GAN.MIN.ML - REDUCE_MARGEN
 *                  Ejemplo: Si GAN.MIN.ML = 60% y REDUCE_MARGEN = 20%, entonces ganancia = 60% - 20% = 40%
 * - IMP: Se suma al factor de impuestos (IMP = 1 + IVA/100 + concepto/100)
 *        Ejemplo: IIBB se suma directamente al factor IMP
 * - CUPON: Se aplica como divisor adicional sobre el PVP después de GT3C
 *          Ejemplo: CUPON se divide por (1 - CUPON/100) al final del cálculo
 * - DESCUENTO: Se aplica como descuento al final del cálculo sobre el PVP
 *              Ejemplo: DESCUENTO_MAQUINA se multiplica por (1 - DESCUENTO/100) al final
 */
public enum AplicaSobre {
    COSTO,
    PVP,
    COSTO_IVA,
    COSTO_MARGEN,
    AUMENTA_MARGEN,
    REDUCE_MARGEN,
    IMP,
    CUPON,
    DESCUENTO
}

