package ar.com.leo.super_master_backend.dominio.material.mapper;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialCreateDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialUpdateDTO;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MaterialMapper {

    MaterialDTO toDTO(Material entity);

    Material toEntity(MaterialCreateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(MaterialUpdateDTO dto, @MappingTarget Material entity);
}