package ar.com.leo.super_master_backend.dominio.clasif_gral.mapper;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClasifGralMapper {

    @Mapping(source = "idPadre.id", target = "padreId")
    ClasifGralDTO toDTO(ClasifGral entity);
}