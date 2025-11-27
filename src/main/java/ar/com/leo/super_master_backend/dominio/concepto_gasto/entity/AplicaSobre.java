package ar.com.leo.super_master_backend.dominio.concepto_gasto.entity;

/**
 * Enum que representa los valores posibles para el campo aplica_sobre
 * en la tabla conceptos_gastos.
 * 
 * Valores permitidos según la base de datos:
 * - COSTO
 * - PVP
 * - COSTO_IVA
 * - COSTO_MARGEN
 */
public enum AplicaSobre {
    COSTO,
    PVP,
    COSTO_IVA,
    COSTO_MARGEN
}

