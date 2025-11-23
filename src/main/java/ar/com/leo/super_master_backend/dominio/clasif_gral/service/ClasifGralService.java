package ar.com.leo.super_master_backend.dominio.clasif_gral.service;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;

import java.util.List;

public interface ClasifGralService {

    ClasifGralDTO obtener(Integer id);

    List<ClasifGralDTO> listar();

    ClasifGralDTO crear(String nombre, Integer padreId);

    ClasifGralDTO actualizar(Integer id, String nombre, Integer padreId);

    void eliminar(Integer id);
}