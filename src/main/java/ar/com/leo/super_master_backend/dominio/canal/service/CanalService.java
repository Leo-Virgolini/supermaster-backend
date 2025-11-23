package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;

import java.util.List;

public interface CanalService {
    CanalDTO obtener(Integer id);

    List<CanalDTO> listar();

    CanalDTO crear(CanalCreateDTO dto);

    CanalDTO actualizar(Integer id, CanalUpdateDTO dto);

    void eliminar(Integer id);
}