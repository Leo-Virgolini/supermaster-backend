package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CanalService {
    List<CanalDTO> listar();

    CanalDTO obtener(Integer id);

    CanalDTO crear(CanalCreateDTO dto);

    CanalDTO actualizar(Integer id, CanalUpdateDTO dto);

    void eliminar(Integer id);

    // 🔥 nueva regla de negocio
    void actualizarMargen(Integer idCanal, BigDecimal nuevoMargen);
}