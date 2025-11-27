package ar.com.leo.super_master_backend.dominio.apto.service;

import ar.com.leo.super_master_backend.dominio.apto.dto.AptoCreateDTO;
import ar.com.leo.super_master_backend.dominio.apto.dto.AptoDTO;
import ar.com.leo.super_master_backend.dominio.apto.dto.AptoUpdateDTO;

import java.util.List;

public interface AptoService {
    List<AptoDTO> listar();

    AptoDTO obtener(Integer id);

    AptoDTO crear(AptoCreateDTO dto);

    AptoDTO actualizar(Integer id, AptoUpdateDTO dto);

    void eliminar(Integer id);
}