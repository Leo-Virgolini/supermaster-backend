package ar.com.leo.super_master_backend.dominio.impuesto.service;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoCreateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ImpuestoService {

    Page<ImpuestoDTO> listar(Pageable pageable);

    ImpuestoDTO obtener(Integer id);

    ImpuestoDTO crear(ImpuestoCreateDTO dto);

    ImpuestoDTO actualizar(Integer id, ImpuestoUpdateDTO dto);

    void eliminar(Integer id);
}