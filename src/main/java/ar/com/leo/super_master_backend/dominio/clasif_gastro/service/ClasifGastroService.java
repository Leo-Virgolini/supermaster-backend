package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;

import java.util.List;

public interface ClasifGastroService {

    List<ClasifGastroDTO> listar();

    ClasifGastroDTO obtener(Integer id);

    ClasifGastroDTO crear(ClasifGastroCreateDTO dto);

    ClasifGastroDTO actualizar(Integer id, ClasifGastroUpdateDTO dto);

    void eliminar(Integer id);
}