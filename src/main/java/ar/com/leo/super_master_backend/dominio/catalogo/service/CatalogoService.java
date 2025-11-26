package ar.com.leo.super_master_backend.dominio.catalogo.service;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoCreateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoUpdateDTO;

import java.util.List;

public interface CatalogoService {

    List<CatalogoDTO> listar();

    CatalogoDTO obtener(Integer id);

    CatalogoDTO crear(CatalogoCreateDTO dto);

    CatalogoDTO actualizar(Integer id, CatalogoUpdateDTO dto);

    void eliminar(Integer id);
}
