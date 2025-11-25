package ar.com.leo.super_master_backend.dominio.apto.service;

import ar.com.leo.super_master_backend.dominio.apto.dto.AptoDTO;

import java.util.List;

public interface AptoService {
    List<AptoDTO> listar();

    AptoDTO obtener(Integer id);

    AptoDTO crear(AptoDTO dto);

    AptoDTO actualizar(Integer id, AptoDTO dto);

    void eliminar(Integer id);
}