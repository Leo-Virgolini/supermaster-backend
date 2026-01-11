package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import org.mapstruct.*;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductoCanalMapper {

    @Mapping(source = "producto.id", target = "productoId")
    ProductoCanalDTO toDTO(ProductoCanal entity);

    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    ProductoCanal toEntity(ProductoCanalDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    void updateEntityFromDTO(ProductoCanalDTO dto, @MappingTarget ProductoCanal entity);
}
