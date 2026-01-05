package ar.com.leo.super_master_backend.dominio.producto.service;


import java.util.List;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;

public interface ProductoCanalService {

    List<ProductoCanalDTO> listar(Integer productoId);

    ProductoCanalDTO agregar(Integer productoId, Integer canalId);

    ProductoCanalDTO actualizar(Integer productoId, Integer canalId, ProductoCanalDTO dto);

    void eliminar(Integer productoId, Integer canalId);
}