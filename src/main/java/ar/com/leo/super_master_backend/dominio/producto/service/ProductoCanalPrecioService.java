package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioDTO;

public interface ProductoCanalPrecioService {
    ProductoCanalPrecioDTO obtener(Integer productoId, Integer canalId);

    ProductoCanalPrecioDTO recalcular(Integer productoId, Integer canalId);
}