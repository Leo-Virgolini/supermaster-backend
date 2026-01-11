package ar.com.leo.super_master_backend.dominio.producto.mla.mapper;

import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface MlaMapper {

    MlaDTO toDTO(Mla entity);

    @Mapping(target = "productos", ignore = true)
    Mla toEntity(MlaDTO dto);
}