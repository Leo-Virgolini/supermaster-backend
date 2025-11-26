package ar.com.leo.super_master_backend.dominio.concepto_gasto.mapper;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE
)
public interface ConceptoGastoMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    ConceptoGastoDTO toDTO(ConceptoGasto entity);


    // =============================
    // CREATE DTO → ENTITY
    // =============================
    ConceptoGasto toEntity(ConceptoGastoCreateDTO dto);


    // =============================
    // UPDATE DTO → ENTITY (PATCH)
    // =============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(ConceptoGastoUpdateDTO dto, @MappingTarget ConceptoGasto entity);
}