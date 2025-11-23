package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;

import java.util.List;

public interface ClasifGastroService {

    ClasifGastroDTO obtener(Integer id);

    List<ClasifGastroDTO> listar();

    ClasifGastroDTO crear(String nombre, Integer padreId);

    ClasifGastroDTO actualizar(Integer id, String nombre, Integer padreId);

    void eliminar(Integer id);
}