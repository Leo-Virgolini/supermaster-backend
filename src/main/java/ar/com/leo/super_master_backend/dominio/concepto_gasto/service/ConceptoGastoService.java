package ar.com.leo.super_master_backend.dominio.concepto_gasto.service;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;

import java.util.List;

public interface ConceptoGastoService {

    List<ConceptoGastoDTO> listar();

    ConceptoGastoDTO obtener(Integer id);

    ConceptoGastoDTO crear(ConceptoGastoCreateDTO dto);

    ConceptoGastoDTO actualizar(Integer id, ConceptoGastoUpdateDTO dto);

    void eliminar(Integer id);
}