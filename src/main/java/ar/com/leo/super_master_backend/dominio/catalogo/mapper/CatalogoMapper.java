package ar.com.leo.super_master_backend.dominio.catalogo.mapper;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoCreateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface CatalogoMapper {
    CatalogoDTO toDTO(Catalogo c);

    Catalogo toEntity(CatalogoCreateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(CatalogoUpdateDTO dto, @MappingTarget Catalogo entity);
}