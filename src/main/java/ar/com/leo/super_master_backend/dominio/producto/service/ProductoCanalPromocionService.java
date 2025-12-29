package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionUpdateDTO;

import java.util.List;

public interface ProductoCanalPromocionService {

    List<ProductoCanalPromocionDTO> listarPorProducto(Integer productoId);

    List<ProductoCanalPromocionDTO> listarPorCanal(Integer canalId);

    List<ProductoCanalPromocionDTO> listarActivas();

    ProductoCanalPromocionDTO obtenerPorId(Integer id);

    ProductoCanalPromocionDTO obtenerPorProductoYCanal(Integer productoId, Integer canalId);

    ProductoCanalPromocionDTO crear(ProductoCanalPromocionCreateDTO dto);

    ProductoCanalPromocionDTO actualizar(Integer productoId, Integer canalId, ProductoCanalPromocionUpdateDTO dto);

    void eliminar(Integer productoId, Integer canalId);
}
