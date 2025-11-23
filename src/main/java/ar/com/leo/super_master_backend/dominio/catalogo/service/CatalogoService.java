package ar.com.leo.super_master_backend.dominio.catalogo.service;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;

import java.util.List;

public interface CatalogoService {
    List<CatalogoDTO> listar();

    CatalogoDTO crear(String nombre);
}