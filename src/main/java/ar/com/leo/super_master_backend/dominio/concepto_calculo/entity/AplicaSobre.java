package ar.com.leo.super_master_backend.dominio.concepto_calculo.entity;

/**
 * Enum que representa los valores posibles para el campo aplica_sobre
 * en la tabla conceptos_calculo.
 *
 * Los valores están agrupados por etapa del cálculo de precio.
 * El nombre de cada valor indica su función y en qué etapa se aplica.
 */
public enum AplicaSobre {

    // ===== ETAPA: COSTO =====
    /**
     * Gasto que multiplica el costo base: COSTO × (1 + %/100)
     * Ejemplo: Embalaje +2%
     */
    GASTO_SOBRE_COSTO,

    /**
     * Flag: usa proveedor.porcentaje para financiación.
     * Se aplica como multiplicador sobre el COSTO: COSTO × (1 + %FIN/100)
     * El concepto actúa como marcador, el valor real viene de proveedor.porcentaje.
     */
    FLAG_FINANCIACION_PROVEEDOR,

    // ===== ETAPA: MARGEN =====
    /**
     * Ajusta el margen sumando/restando puntos porcentuales: GAN.MIN.ML + porcentaje
     * El signo del porcentaje determina si aumenta (+) o reduce (-).
     * Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = +25%, entonces ganancia = 85%
     */
    AJUSTE_MARGEN_PUNTOS,

    /**
     * Ajusta el margen proporcionalmente: GAN.MIN.ML × (1 + porcentaje/100)
     * El signo del porcentaje determina si aumenta (+) o reduce (-).
     * Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = +10%, entonces ganancia = 66%
     */
    AJUSTE_MARGEN_PROPORCIONAL,

    /**
     * Flag: usa productoMargen.margenMinorista para el canal.
     * Solo actúa como marcador, el porcentaje del concepto se ignora.
     */
    FLAG_USAR_MARGEN_MINORISTA,

    /**
     * Flag: usa productoMargen.margenMayorista para el canal.
     * Solo actúa como marcador, el porcentaje del concepto se ignora.
     * Si un canal no tiene ninguno de estos conceptos, se usa margenMinorista por defecto.
     */
    FLAG_USAR_MARGEN_MAYORISTA,

    /**
     * Gasto después de calcular ganancia, pero antes de impuestos.
     * Multiplica: COSTO_CON_GANANCIA × (1 + concepto/100)
     * Ejemplo: LGELOG, LGEMKT se aplican con este tipo.
     */
    GASTO_POST_GANANCIA,

    // ===== ETAPA: IMPUESTOS =====
    /**
     * Flag: habilita la aplicación del IVA del producto para el canal.
     * Si existe este concepto, se aplica el IVA del producto.
     * Si NO existe, NO se aplica IVA (0%).
     * El porcentaje del concepto se ignora, solo actúa como habilitador.
     */
    FLAG_APLICAR_IVA,

    /**
     * Impuesto que se suma al factor IMP: (IMP = 1 + IVA/100 + concepto/100)
     * Ejemplo: IIBB se suma directamente al factor IMP.
     */
    IMPUESTO_ADICIONAL,

    /**
     * Gasto después de aplicar impuestos: costoConImp × (1 + %/100)
     */
    GASTO_POST_IMPUESTOS,

    // ===== ETAPA: PRECIO =====
    /**
     * Flag: incluye mla.precioEnvio para el producto.
     * El concepto actúa como marcador, el valor real viene de mlas.precio_envio.
     */
    FLAG_INCLUIR_ENVIO,

    /**
     * Comisión que se aplica como divisor sobre el PVP: PVP / (1 - %/100)
     * Ejemplo: Comisión ML -13%
     * NOTA: Para conceptos de cuotas (con campo cuotas != NULL), se procesan de manera especial.
     */
    COMISION_SOBRE_PVP,

    /**
     * Flag: usa mla.comisionPorcentaje como comisión sobre PVP.
     * El concepto actúa como marcador, el valor real viene de mlas.comision_porcentaje.
     * Se suma a COMISION_SOBRE_PVP y se aplica como divisor: PVP / (1 - %/100)
     */
    FLAG_COMISION_ML,

    /**
     * Calcula el PVP basándose en el PVP del canal base (canalBase).
     * Si existe este concepto, se omite el cálculo normal y se usa:
     * PVP = PVP_CANAL_BASE × (1 + porcentaje/100)
     * Requiere que el canal tenga un canalBase configurado.
     */
    CALCULO_SOBRE_CANAL_BASE,

    // ===== ETAPA: POST_PRECIO =====
    /**
     * Recargo cupón: se aplica como divisor adicional después de GT3C.
     * PVP / (1 - RECARGO_CUPON/100)
     */
    RECARGO_CUPON,

    /**
     * Descuento porcentual que reduce el PVP al final del cálculo.
     * PVP × (1 - DESCUENTO/100)
     * Ejemplo: DESCUENTO_MAQUINA
     */
    DESCUENTO_PORCENTUAL,

    /**
     * Inflación aplicada como divisor sobre el PVP.
     * PVP / (1 - INFLACION/100)
     */
    INFLACION_DIVISOR,

    /**
     * Flag: habilita la aplicación de promociones para el canal.
     * Si existe este concepto, se aplican las promociones de producto_canal_promocion.
     * Si NO existe, NO se aplican promociones.
     * Solo actúa como habilitador, el porcentaje del concepto se ignora.
     */
    FLAG_APLICAR_PROMOCIONES
}
