package ar.com.leo.super_master_backend.dominio.catalogo.mapper;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogoMapper {
    CatalogoDTO toDTO(Catalogo c);

    Catalogo toEntity(CatalogoDTO dto);
}