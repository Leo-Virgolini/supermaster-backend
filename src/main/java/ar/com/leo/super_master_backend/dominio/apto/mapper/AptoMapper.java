package ar.com.leo.super_master_backend.dominio.apto.mapper;

import ar.com.leo.super_master_backend.dominio.apto.dto.AptoDTO;
import ar.com.leo.super_master_backend.dominio.apto.entity.Apto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AptoMapper {
    AptoDTO toDTO(Apto entity);

    Apto toEntity(AptoDTO dto);
}