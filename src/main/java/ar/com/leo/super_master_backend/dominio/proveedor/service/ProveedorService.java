package ar.com.leo.super_master_backend.dominio.proveedor.service;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorCreateDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorUpdateDTO;

import java.util.List;

public interface ProveedorService {

    List<ProveedorDTO> listar();

    ProveedorDTO obtener(Integer id);

    ProveedorDTO crear(ProveedorCreateDTO dto);

    ProveedorDTO actualizar(Integer id, ProveedorUpdateDTO dto);

    void eliminar(Integer id);
}