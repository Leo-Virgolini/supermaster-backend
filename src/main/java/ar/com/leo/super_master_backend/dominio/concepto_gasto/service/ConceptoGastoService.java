package ar.com.leo.super_master_backend.dominio.concepto_gasto.service;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConceptoGastoService {

    Page<ConceptoGastoDTO> listar(String search, Pageable pageable);

    
    ConceptoGastoDTO obtener(Integer id);

    ConceptoGastoDTO crear(ConceptoGastoCreateDTO dto);

    ConceptoGastoDTO actualizar(Integer id, ConceptoGastoUpdateDTO dto);

    void eliminar(Integer id);
}