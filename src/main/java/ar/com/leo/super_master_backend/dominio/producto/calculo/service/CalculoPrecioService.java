package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;

public interface CalculoPrecioService {

    /**
     * Calcula el precio de un producto en un canal,
     * sin tocar la base de datos (solo en memoria).
     */
    PrecioCalculadoDTO calcular(Integer idProducto, Integer idCanal);

    /**
     * Calcula y además persiste el resultado en producto_canal_precios.
     * Devuelve el DTO con los datos finales.
     */
    PrecioCalculadoDTO recalcularYGuardar(Integer idProducto, Integer idCanal);
}