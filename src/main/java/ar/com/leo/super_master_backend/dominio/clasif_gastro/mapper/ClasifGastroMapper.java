package ar.com.leo.super_master_backend.dominio.clasif_gastro.mapper;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClasifGastroMapper {

    @Mapping(source = "idPadre.id", target = "padreId")
    ClasifGastroDTO toDTO(ClasifGastro entity);
}