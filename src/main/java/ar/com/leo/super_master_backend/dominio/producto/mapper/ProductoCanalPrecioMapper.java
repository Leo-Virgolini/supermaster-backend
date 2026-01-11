package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductoCanalPrecioMapper {

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "canal.id", target = "canalId")
    ProductoCanalPrecioDTO toDTO(ProductoCanalPrecio entity);

    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    @Mapping(target = "canal", expression = "java(new Canal(dto.canalId()))")
    ProductoCanalPrecio toEntity(ProductoCanalPrecioDTO dto);
}