package ar.com.leo.super_master_backend.dominio.producto.mla.mapper;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MlaMapper {

    @Mapping(source = "producto.id", target = "productoId")
    MlaDTO toDTO(Mla entity);

    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    Mla toEntity(MlaDTO dto);
}