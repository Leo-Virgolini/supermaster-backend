package ar.com.leo.super_master_backend.dominio.catalogo.service;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoCreateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CatalogoService {

    Page<CatalogoDTO> listar(String search, Pageable pageable);

    CatalogoDTO obtener(Integer id);

    CatalogoDTO crear(CatalogoCreateDTO dto);

    CatalogoDTO actualizar(Integer id, CatalogoUpdateDTO dto);

    void eliminar(Integer id);
}
