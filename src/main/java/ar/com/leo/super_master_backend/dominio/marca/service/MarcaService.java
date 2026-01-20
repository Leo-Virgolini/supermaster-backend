package ar.com.leo.super_master_backend.dominio.marca.service;

import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaCreateDTO;
import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaDTO;
import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MarcaService {

    Page<MarcaDTO> listar(String search, Pageable pageable);

    MarcaDTO obtener(Integer id);

    MarcaDTO crear(MarcaCreateDTO dto);

    MarcaDTO actualizar(Integer id, MarcaUpdateDTO dto);

    void eliminar(Integer id);
}