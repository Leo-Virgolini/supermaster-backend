package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClasifGastroService {

    Page<ClasifGastroDTO> listar(String search, Pageable pageable);

    ClasifGastroDTO obtener(Integer id);

    ClasifGastroDTO crear(ClasifGastroCreateDTO dto);

    ClasifGastroDTO actualizar(Integer id, ClasifGastroUpdateDTO dto);

    void eliminar(Integer id);
}