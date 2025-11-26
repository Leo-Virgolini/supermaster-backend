package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductoCanalMapper {

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "canal.id", target = "canalId")
    ProductoCanalDTO toDTO(ProductoCanal entity);

    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    @Mapping(target = "canal", expression = "java(new Canal(dto.canalId()))")
    ProductoCanal toEntity(ProductoCanalDTO dto);
}