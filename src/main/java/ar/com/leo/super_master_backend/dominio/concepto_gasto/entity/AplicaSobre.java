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
 * - AUMENTA_MARGEN_PTS: Suma puntos porcentuales directamente al margen: GAN.MIN.ML + porcentaje
 *                       Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 25%, entonces ganancia = 60% + 25% = 85%
 * - REDUCE_MARGEN_PTS: Resta puntos porcentuales directamente del margen: GAN.MIN.ML - porcentaje
 *                      Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 20%, entonces ganancia = 60% - 20% = 40%
 * - AUMENTA_MARGEN_PROP: Aumenta el margen proporcionalmente: GAN.MIN.ML * (1 + porcentaje/100)
 *                        Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 10%, entonces ganancia = 60% * 1.10 = 66%
 * - REDUCE_MARGEN_PROP: Reduce el margen proporcionalmente: GAN.MIN.ML * (1 - porcentaje/100)
 *                       Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 10%, entonces ganancia = 60% * 0.90 = 54%
 * - IMP: Se suma al factor de impuestos (IMP = 1 + IVA/100 + concepto/100)
 *        Ejemplo: IIBB se suma directamente al factor IMP
 * - RECARGO_CUPON: Se aplica como divisor adicional sobre el PVP después de GT3C, aumentando el precio
 *                  Ejemplo: RECARGO_CUPON se divide por (1 - RECARGO_CUPON/100) al final del cálculo
 * - DESCUENTO: Se aplica como descuento al final del cálculo sobre el PVP, reduciendo el precio
 *              Ejemplo: DESCUENTO_MAQUINA se multiplica por (1 - DESCUENTO/100) al final
 * - ENVIO: Indicador que indica que se debe buscar el precio de envío de mlas.precio_envio para el producto
 *          El concepto actúa como marcador, el valor real viene de mlas.precio_envio
 * - INFLACION: Se aplica como divisor sobre el PVP: PVP / (1 - INFLACION/100)
 *              Ejemplo: INFLACION = 10% → PVP / (1 - 10/100) = PVP / 0.9
 * - PROVEEDOR_FIN: Obtiene el porcentaje de financiación del proveedor (proveedores.porcentaje)
 *                  Se aplica como multiplicador sobre el COSTO: COSTO * (1 + %FIN/100)
 *                  El concepto actúa como marcador, el valor real viene de proveedor.porcentaje
 *                  Ejemplo: Si proveedor.porcentaje = 5%, entonces COSTO * (1 + 5/100) = COSTO * 1.05
 * - COSTO_GANANCIA: Se aplica después de calcular el costo con ganancia, pero antes de aplicar impuestos
 *                   Multiplica: COSTO_CON_GANANCIA * (1 + concepto/100)
 *                   Diferente de COSTO_IVA que se aplica después de impuestos
 *                   Ejemplo: LGELOG, LGEMKT se aplican con este tipo
 * - IVA: Habilita la aplicación del IVA del producto para el canal.
 *        Si existe un concepto con este tipo para el canal, se aplica el IVA del producto.
 *        Si NO existe el concepto IVA para el canal, NO se aplica IVA (0%).
 *        El porcentaje del concepto se ignora, solo actúa como habilitador.
 *        Ejemplo: Canal con IVA → crear concepto con aplicaSobre=IVA
 *                 Canal sin IVA → no asociar concepto IVA al canal
 * - SOBRE_PVP_BASE: Calcula el PVP basándose en el PVP del canal base (canalBase).
 *        Si existe un concepto con este tipo, se omite el cálculo normal y se usa:
 *        PVP = PVP_CANAL_BASE * (1 + porcentaje/100)
 *        Ejemplo: porcentaje = 8 → PVP = PVP_BASE * 1.08 (+8% sobre el canal base)
 *        Requiere que el canal tenga un canalBase configurado y que ese canal tenga PVP calculado.
 */
public enum AplicaSobre {
    COSTO,
    PVP,
    COSTO_IVA,
    AUMENTA_MARGEN_PTS,
    REDUCE_MARGEN_PTS,
    AUMENTA_MARGEN_PROP,
    REDUCE_MARGEN_PROP,
    IMP,
    RECARGO_CUPON,
    DESCUENTO,
    ENVIO,
    INFLACION,
    PROVEEDOR_FIN,
    COSTO_GANANCIA,
    IVA,
    SOBRE_PVP_BASE
}

