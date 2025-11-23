package ar.com.leo.super_master_backend.dominio.proveedor.service;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;

import java.util.List;

public interface ProveedorService {
    List<ProveedorDTO> listar();
}