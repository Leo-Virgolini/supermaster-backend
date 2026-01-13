package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoConPreciosDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductoService {

    Page<ProductoDTO> listar(Pageable pageable);

    ProductoDTO obtener(Integer id);

    ProductoDTO crear(ProductoCreateDTO dto);

    ProductoDTO actualizar(Integer id, ProductoUpdateDTO dto);

    void eliminar(Integer id);

    ProductoDTO obtenerPorSku(String sku);

    Page<ProductoDTO> filtrar(ProductoFilter filter, Pageable pageable);

    Page<ProductoConPreciosDTO> listarConPrecios(ProductoFilter filter, Pageable pageable);

    void actualizarCosto(Integer idProducto, BigDecimal nuevoCosto);
}
