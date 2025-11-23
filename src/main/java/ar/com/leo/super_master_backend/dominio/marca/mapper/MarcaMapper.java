package ar.com.leo.super_master_backend.dominio.marca.mapper;

import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaDTO;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MarcaMapper {

    @Mapping(source = "idPadre.id", target = "padreId")
    MarcaDTO toDTO(Marca marca);

    @Mapping(source = "padreId", target = "idPadre.id")
    Marca toEntity(MarcaDTO dto);
}