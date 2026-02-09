package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.FormulaCalculoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.RecalculoMasivoResultDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.CanalPreciosDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CalculoPrecioService {

    /**
     * Calcula el precio de un producto para un canal (sin persistir).
     * 
     * @param productoId ID del producto
     * @param canalId ID del canal
     * @param numeroCuotas Número de cuotas (opcional). Si es null o 1, no se aplican gastos de cuotas.
     *                     Si se especifica (ej: 3, 6, 12), se aplica el concepto de gasto correspondiente.
     */
    PrecioCalculadoDTO calcularPrecioCanal(Integer productoId, Integer canalId, Integer numeroCuotas);

    /**
     * Calcula el precio de un producto para un canal con un costo de envío personalizado (sin persistir).
     * Útil para simular el PVP con un costo de envío diferente al almacenado en MLA.
     *
     * @param productoId ID del producto
     * @param canalId ID del canal
     * @param numeroCuotas Número de cuotas (opcional)
     * @param precioEnvioOverride Costo de envío a usar en lugar del almacenado en mla.precio_envio
     * @return PrecioCalculadoDTO con el precio simulado
     */
    PrecioCalculadoDTO calcularPrecioCanalConEnvio(Integer productoId, Integer canalId, Integer numeroCuotas, BigDecimal precioEnvioOverride);

    /**
     * Calcula y además guarda/actualiza el registro en producto_canal_precios.
     *
     * @param productoId ID del producto
     * @param canalId ID del canal
     * @param numeroCuotas Número de cuotas (opcional). Si es null o 1, no se aplican gastos de cuotas.
     */
    PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer productoId, Integer canalId, Integer numeroCuotas);

    /**
     * Obtiene la fórmula del cálculo de precio para un producto, canal y número de cuotas.
     * Muestra paso a paso cómo se calcula el precio con los valores reales.
     *
     * @param productoId ID del producto
     * @param canalId ID del canal
     * @param numeroCuotas Número de cuotas (opcional)
     * @return DTO con la fórmula desglosada paso a paso
     */
    FormulaCalculoDTO obtenerFormulaCalculo(Integer productoId, Integer canalId, Integer numeroCuotas);

    /**
     * Calcula y guarda los precios de un producto para todas las cuotas configuradas en el canal.
     * Incluye contado (null) y todas las cuotas de canal_concepto_cuota.
     *
     * @param productoId ID del producto
     * @param canalId ID del canal
     * @return DTO con el canal y sus precios calculados y guardados
     */
    CanalPreciosDTO recalcularYGuardarPrecioCanalTodasCuotas(Integer productoId, Integer canalId);

    /**
     * Calcula y guarda precios de un producto para un canal.
     * Si cuotas es null, calcula para todas las cuotas configuradas en el canal.
     * Si cuotas tiene valor, calcula solo para esa cantidad de cuotas.
     *
     * @param productoId ID del producto
     * @param canalId ID del canal
     * @param cuotas Número de cuotas (opcional). Si es null, calcula todas.
     * @return DTO con el canal y sus precios calculados y guardados
     */
    CanalPreciosDTO recalcularYGuardar(Integer productoId, Integer canalId, Integer cuotas);

    /**
     * Calcula y guarda precios de un producto en todos sus canales configurados.
     *
     * @param productoId ID del producto
     * @return Lista de DTOs con los precios de cada canal
     */
    List<CanalPreciosDTO> recalcularProductoTodosCanales(Integer productoId);

    /**
     * Calcula y guarda precios de un producto en todos sus canales configurados,
     * pero solo para las cuotas especificadas.
     *
     * @param productoId ID del producto
     * @param cuotas Número de cuotas a calcular
     * @return Lista de DTOs con los precios de cada canal (solo la cuota indicada)
     */
    List<CanalPreciosDTO> recalcularProductoTodosCanales(Integer productoId, Integer cuotas);

    /**
     * Calcula y guarda precios de TODOS los productos en TODOS los canales.
     * Operación masiva que puede tomar tiempo considerable.
     *
     * @return Resultado con total de precios calculados y productos ignorados
     */
    RecalculoMasivoResultDTO recalcularTodos();
}