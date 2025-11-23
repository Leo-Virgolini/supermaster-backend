package ar.com.leo.super_master_backend.dominio.tipo.service;

import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoDTO;

import java.util.List;

public interface TipoService {

    TipoDTO obtener(Integer id);

    List<TipoDTO> listar();

    TipoDTO crear(TipoDTO dto);

    TipoDTO actualizar(Integer id, TipoDTO dto);

    void eliminar(Integer id);
}