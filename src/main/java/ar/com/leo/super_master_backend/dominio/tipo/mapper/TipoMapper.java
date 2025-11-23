package ar.com.leo.super_master_backend.dominio.tipo.mapper;

import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoDTO;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TipoMapper {

    @Mapping(source = "idPadre.id", target = "padreId")
    TipoDTO toDTO(Tipo tipo);

    @Mapping(source = "padreId", target = "idPadre.id")
    Tipo toEntity(TipoDTO dto);
}