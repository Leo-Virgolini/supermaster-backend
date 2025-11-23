package ar.com.leo.super_master_backend.dominio.concepto_gasto.mapper;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConceptoGastoMapper {
    ConceptoGastoDTO toDTO(ConceptoGasto g);
}