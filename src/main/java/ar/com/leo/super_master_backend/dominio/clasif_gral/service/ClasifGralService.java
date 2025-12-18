package ar.com.leo.super_master_backend.dominio.clasif_gral.service;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClasifGralService {

    Page<ClasifGralDTO> listar(Pageable pageable);

    ClasifGralDTO obtener(Integer id);

    ClasifGralDTO crear(ClasifGralCreateDTO dto);

    ClasifGralDTO actualizar(Integer id, ClasifGralUpdateDTO dto);

    void eliminar(Integer id);
}