package ar.com.leo.super_master_backend.dominio.producto.service;

import java.util.Optional;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoMargenDTO;

public interface ProductoMargenService {

    Optional<ProductoMargenDTO> obtener(Integer productoId);

    ProductoMargenDTO guardar(ProductoMargenDTO dto);

    void eliminar(Integer productoId);
}
