package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MlaService {

    Page<MlaDTO> listar(Pageable pageable);

    MlaDTO obtener(Integer id);

    MlaDTO crear(MlaDTO dto);

    MlaDTO actualizar(Integer id, MlaDTO dto);

    void eliminar(Integer id);
}
