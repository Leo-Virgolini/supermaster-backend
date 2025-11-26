package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoService {

    List<ProductoDTO> listar();

    ProductoDTO obtener(Integer id);

    ProductoDTO crear(ProductoCreateDTO dto);

    ProductoDTO actualizar(Integer id, ProductoUpdateDTO dto);

    void eliminar(Integer id);

    ProductoDTO obtenerPorSku(String sku);

    // 🔥 regla de negocio que sí modifica entidades
    void actualizarCosto(Integer idProducto, BigDecimal nuevoCosto);
}
