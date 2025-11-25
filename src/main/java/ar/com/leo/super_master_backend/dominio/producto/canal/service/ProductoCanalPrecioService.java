package ar.com.leo.super_master_backend.dominio.producto.canal.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;

public interface ProductoCanalPrecioService {
    PrecioCalculadoDTO recalcular(Integer idProducto, Integer idCanal);
}