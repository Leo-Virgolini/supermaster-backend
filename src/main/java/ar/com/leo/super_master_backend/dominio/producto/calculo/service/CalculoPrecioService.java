package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.FormulaCalculoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.RecalculoMasivoResultDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.CanalPreciosDTO;

import java.util.List;

public interface CalculoPrecioService {

    /**
     * Calcula el precio de un producto para un canal (sin persistir).
     * 
     * @param idProducto ID del producto
     * @param idCanal ID del canal
     * @param numeroCuotas Número de cuotas (opcional). Si es null o 1, no se aplican gastos de cuotas.
     *                     Si se especifica (ej: 3, 6, 12), se aplica el concepto de gasto correspondiente.
     */
    PrecioCalculadoDTO calcularPrecioCanal(Integer idProducto, Integer idCanal, Integer numeroCuotas);

    /**
     * Calcula el precio de un producto para un canal (sin persistir, sin cuotas).
     * Equivalente a calcularPrecioCanal(idProducto, idCanal, null).
     */
    default PrecioCalculadoDTO calcularPrecioCanal(Integer idProducto, Integer idCanal) {
        return calcularPrecioCanal(idProducto, idCanal, null);
    }

    /**
     * Calcula y además guarda/actualiza el registro en producto_canal_precios.
     * 
     * @param idProducto ID del producto
     * @param idCanal ID del canal
     * @param numeroCuotas Número de cuotas (opcional). Si es null o 1, no se aplican gastos de cuotas.
     */
    PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer idProducto, Integer idCanal, Integer numeroCuotas);

    /**
     * Calcula y además guarda/actualiza el registro en producto_canal_precios (sin cuotas).
     * Equivalente a recalcularYGuardarPrecioCanal(idProducto, idCanal, null).
     */
    default PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer idProducto, Integer idCanal) {
        return recalcularYGuardarPrecioCanal(idProducto, idCanal, null);
    }

    /**
     * Obtiene la fórmula del cálculo de precio para un producto, canal y número de cuotas.
     * Muestra paso a paso cómo se calcula el precio con los valores reales.
     *
     * @param idProducto ID del producto
     * @param idCanal ID del canal
     * @param numeroCuotas Número de cuotas (opcional)
     * @return DTO con la fórmula desglosada paso a paso
     */
    FormulaCalculoDTO obtenerFormulaCalculo(Integer idProducto, Integer idCanal, Integer numeroCuotas);

    /**
     * Calcula el precio de un producto para todas las cuotas configuradas en el canal (sin persistir).
     * Incluye contado (null) y todas las cuotas de canal_concepto_cuota.
     *
     * @param idProducto ID del producto
     * @param idCanal ID del canal
     * @return Lista de precios calculados (contado + cada opción de cuotas)
     */
    List<PrecioCalculadoDTO> calcularPrecioCanalTodasCuotas(Integer idProducto, Integer idCanal);

    /**
     * Calcula y guarda los precios de un producto para todas las cuotas configuradas en el canal.
     * Incluye contado (null) y todas las cuotas de canal_concepto_cuota.
     *
     * @param idProducto ID del producto
     * @param idCanal ID del canal
     * @return DTO con el canal y sus precios calculados y guardados
     */
    CanalPreciosDTO recalcularYGuardarPrecioCanalTodasCuotas(Integer idProducto, Integer idCanal);

    /**
     * Calcula y guarda precios de un producto para un canal.
     * Si cuotas es null, calcula para todas las cuotas configuradas en el canal.
     * Si cuotas tiene valor, calcula solo para esa cantidad de cuotas.
     *
     * @param idProducto ID del producto
     * @param idCanal ID del canal
     * @param cuotas Número de cuotas (opcional). Si es null, calcula todas.
     * @return DTO con el canal y sus precios calculados y guardados
     */
    CanalPreciosDTO recalcularYGuardar(Integer idProducto, Integer idCanal, Integer cuotas);

    /**
     * Calcula y guarda precios de un producto en todos sus canales configurados.
     *
     * @param idProducto ID del producto
     * @return Lista de DTOs con los precios de cada canal
     */
    List<CanalPreciosDTO> recalcularProductoTodosCanales(Integer idProducto);

    /**
     * Calcula y guarda precios de un producto en todos sus canales configurados,
     * pero solo para las cuotas especificadas.
     *
     * @param idProducto ID del producto
     * @param cuotas Número de cuotas a calcular
     * @return Lista de DTOs con los precios de cada canal (solo la cuota indicada)
     */
    List<CanalPreciosDTO> recalcularProductoTodosCanales(Integer idProducto, Integer cuotas);

    /**
     * Calcula y guarda precios de TODOS los productos en TODOS los canales.
     * Operación masiva que puede tomar tiempo considerable.
     *
     * @return Resultado con total de precios calculados y productos ignorados
     */
    RecalculoMasivoResultDTO recalcularTodos();
}