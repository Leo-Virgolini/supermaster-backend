package ar.com.leo.super_master_backend.dominio.origen.service;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;

import java.util.List;

public interface OrigenService {

    OrigenDTO obtener(Integer id);

    List<OrigenDTO> listar();

    OrigenDTO crear(OrigenDTO dto);

    OrigenDTO actualizar(Integer id, OrigenDTO dto);

    void eliminar(Integer id);
}