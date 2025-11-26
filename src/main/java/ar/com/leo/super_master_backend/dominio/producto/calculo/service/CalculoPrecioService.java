package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;

public interface CalculoPrecioService {

    /**
     * Calcula el precio de un producto para un canal (sin persistir).
     */
    PrecioCalculadoDTO calcularPrecioCanal(Integer idProducto, Integer idCanal);

    /**
     * Calcula y además guarda/actualiza el registro en producto_canal_precios.
     */
    PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer idProducto, Integer idCanal);
}