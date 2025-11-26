package ar.com.leo.super_master_backend.dominio.producto.service;


import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;

import java.util.List;

public interface ProductoCanalService {

    List<ProductoCanalDTO> listar(Integer productoId);

    ProductoCanalDTO agregar(Integer productoId, Integer canalId);

    ProductoCanalDTO actualizar(Integer productoId, Integer canalId, ProductoCanalDTO dto);

    void eliminar(Integer productoId, Integer canalId);
}