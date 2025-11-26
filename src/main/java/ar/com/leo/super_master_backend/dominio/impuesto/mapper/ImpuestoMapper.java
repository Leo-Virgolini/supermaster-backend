package ar.com.leo.super_master_backend.dominio.impuesto.mapper;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoCreateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.entity.Impuesto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ImpuestoMapper {

    // ENTITY → DTO
    ImpuestoDTO toDTO(Impuesto entity);

    // CREATE DTO → ENTITY
    Impuesto toEntity(ImpuestoCreateDTO dto);

    // UPDATE DTO → ENTITY existente
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(ImpuestoUpdateDTO dto, @MappingTarget Impuesto entity);
}