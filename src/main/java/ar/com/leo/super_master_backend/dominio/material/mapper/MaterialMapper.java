package ar.com.leo.super_master_backend.dominio.material.mapper;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MaterialMapper {

    MaterialDTO toDTO(Material material);

    Material toEntity(MaterialDTO dto);
}