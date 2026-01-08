package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.common.mapper.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import org.mapstruct.*;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductoCanalMapper {

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "canal.id", target = "canalId")
    ProductoCanalDTO toDTO(ProductoCanal entity);

    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    @Mapping(target = "canal", expression = "java(new Canal(dto.canalId()))")
    ProductoCanal toEntity(ProductoCanalDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "canal", ignore = true)
    void updateEntityFromDTO(ProductoCanalDTO dto, @MappingTarget ProductoCanal entity);
}