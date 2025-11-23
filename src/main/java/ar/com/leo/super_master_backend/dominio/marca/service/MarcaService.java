package ar.com.leo.super_master_backend.dominio.marca.service;

import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaDTO;

import java.util.List;

public interface MarcaService {

    MarcaDTO obtener(Integer id);

    List<MarcaDTO> listar();

    MarcaDTO crear(MarcaDTO dto);

    MarcaDTO actualizar(Integer id, MarcaDTO dto);

    void eliminar(Integer id);
}