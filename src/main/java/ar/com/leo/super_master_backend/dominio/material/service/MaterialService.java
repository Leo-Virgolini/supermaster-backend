package ar.com.leo.super_master_backend.dominio.material.service;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialCreateDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaterialService {

    Page<MaterialDTO> listar(String search, Pageable pageable);

    
    MaterialDTO obtener(Integer id);

    MaterialDTO crear(MaterialCreateDTO dto);

    MaterialDTO actualizar(Integer id, MaterialUpdateDTO dto);

    void eliminar(Integer id);
}