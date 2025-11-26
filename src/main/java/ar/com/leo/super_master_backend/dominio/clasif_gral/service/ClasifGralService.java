package ar.com.leo.super_master_backend.dominio.clasif_gral.service;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralUpdateDTO;

import java.util.List;

public interface ClasifGralService {

    List<ClasifGralDTO> listar();

    ClasifGralDTO obtener(Integer id);

    ClasifGralDTO crear(ClasifGralCreateDTO dto);

    ClasifGralDTO actualizar(Integer id, ClasifGralUpdateDTO dto);

    void eliminar(Integer id);
}