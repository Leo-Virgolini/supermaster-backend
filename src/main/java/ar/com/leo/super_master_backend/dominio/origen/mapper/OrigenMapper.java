package ar.com.leo.super_master_backend.dominio.origen.mapper;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenCreateDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenUpdateDTO;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrigenMapper {

    OrigenDTO toDTO(Origen entity);

    Origen toEntity(OrigenCreateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(OrigenUpdateDTO dto, @MappingTarget Origen entity);
}