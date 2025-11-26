package ar.com.leo.super_master_backend.dominio.material.service;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialCreateDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialUpdateDTO;

import java.util.List;

public interface MaterialService {

    List<MaterialDTO> listar();

    MaterialDTO obtener(Integer id);

    MaterialDTO crear(MaterialCreateDTO dto);

    MaterialDTO actualizar(Integer id, MaterialUpdateDTO dto);

    void eliminar(Integer id);
}