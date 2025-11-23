package ar.com.leo.super_master_backend.dominio.concepto_gasto.service;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;

import java.util.List;

public interface ConceptoGastoService {
    List<ConceptoGastoDTO> listar();

    ConceptoGastoDTO obtener(Integer id);
}