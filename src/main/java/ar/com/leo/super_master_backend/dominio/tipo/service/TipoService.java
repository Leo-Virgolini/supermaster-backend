package ar.com.leo.super_master_backend.dominio.tipo.service;

import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoCreateDTO;
import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoDTO;
import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TipoService {

    Page<TipoDTO> listar(Pageable pageable);

    TipoDTO obtener(Integer id);

    TipoDTO crear(TipoCreateDTO dto);

    TipoDTO actualizar(Integer id, TipoUpdateDTO dto);

    void eliminar(Integer id);
}