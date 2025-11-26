package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;

import java.util.List;

public interface CanalConceptoService {

    List<CanalConceptoDTO> listarPorCanal(Integer canalId);

    CanalConceptoDTO asignarConcepto(Integer canalId, Integer conceptoId);

    void eliminarConcepto(Integer canalId, Integer conceptoId);


}