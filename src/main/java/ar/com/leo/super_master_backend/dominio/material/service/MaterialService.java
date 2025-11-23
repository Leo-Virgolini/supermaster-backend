package ar.com.leo.super_master_backend.dominio.material.service;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;

import java.util.List;

public interface MaterialService {

    MaterialDTO obtener(Integer id);

    List<MaterialDTO> listar();

    MaterialDTO crear(MaterialDTO dto);

    MaterialDTO actualizar(Integer id, MaterialDTO dto);

    void eliminar(Integer id);
}