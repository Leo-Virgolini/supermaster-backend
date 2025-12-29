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
 * - IMP: Se suma al factor de impuestos (IMP = 1 + IVA/100 + concepto/100)
 *        Ejemplo: IIBB se suma directamente al factor IMP
 * - CUPON: Se aplica como divisor adicional sobre el PVP después de GT3C
 *          Ejemplo: CUPON se divide por (1 - CUPON/100) al final del cálculo
 */
public enum AplicaSobre {
    COSTO,
    PVP,
    COSTO_IVA,
    COSTO_MARGEN,
    IMP,
    CUPON
}

