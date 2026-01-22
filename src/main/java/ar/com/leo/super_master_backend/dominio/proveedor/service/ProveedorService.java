package ar.com.leo.super_master_backend.dominio.proveedor.service;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorCreateDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProveedorService {

    Page<ProveedorDTO> listar(String search, Pageable pageable);


    ProveedorDTO obtener(Integer id);

    ProveedorDTO crear(ProveedorCreateDTO dto);

    ProveedorDTO actualizar(Integer id, ProveedorUpdateDTO dto);

    void eliminar(Integer id);

    List<ProductoResumenDTO> listarProductos(Integer proveedorId);
}