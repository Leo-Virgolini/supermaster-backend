package ar.com.leo.super_master_backend.dominio.origen.service;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenCreateDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenUpdateDTO;

import java.util.List;

public interface OrigenService {

    List<OrigenDTO> listar();

    OrigenDTO obtener(Integer id);

    OrigenDTO crear(OrigenCreateDTO dto);

    OrigenDTO actualizar(Integer id, OrigenUpdateDTO dto);

    void eliminar(Integer id);
}