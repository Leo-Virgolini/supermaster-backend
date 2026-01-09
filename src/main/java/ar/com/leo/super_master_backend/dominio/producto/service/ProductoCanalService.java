package ar.com.leo.super_master_backend.dominio.producto.service;

import java.util.Optional;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;

public interface ProductoCanalService {

    Optional<ProductoCanalDTO> obtener(Integer productoId);

    ProductoCanalDTO guardar(ProductoCanalDTO dto);

    void eliminar(Integer productoId);
}
