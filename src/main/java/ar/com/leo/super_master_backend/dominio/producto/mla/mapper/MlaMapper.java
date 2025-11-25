package ar.com.leo.super_master_backend.dominio.producto.mla.mapper;

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

    @Mapping(source = "idProducto.id", target = "productoId")
    MlaDTO toDTO(Mla entity);

    @Mapping(source = "productoId", target = "idProducto.id")
    Mla toEntity(MlaDTO dto);
}