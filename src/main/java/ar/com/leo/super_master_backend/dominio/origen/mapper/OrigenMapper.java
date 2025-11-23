package ar.com.leo.super_master_backend.dominio.origen.mapper;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrigenMapper {

    OrigenDTO toDTO(Origen origen);

    Origen toEntity(OrigenDTO dto);
}