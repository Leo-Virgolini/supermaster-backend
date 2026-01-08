package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.common.mapper.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPromocion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface ProductoCanalPromocionMapper {

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "canal.id", target = "canalId")
    @Mapping(source = "promocion", target = "promocion")
    ProductoCanalPromocionDTO toDTO(ProductoCanalPromocion entity);

    @Mapping(target = "producto", expression = "java(new Producto(dto.productoId()))")
    @Mapping(target = "canal", expression = "java(new Canal(dto.canalId()))")
    @Mapping(target = "promocion", expression = "java(new Promocion(dto.promocionId()))")
    @Mapping(target = "id", ignore = true)
    ProductoCanalPromocion toEntity(ProductoCanalPromocionCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "canal", ignore = true)
    @Mapping(target = "promocion", expression = "java(dto.promocionId() != null ? new Promocion(dto.promocionId()) : entity.getPromocion())")
    void updateEntityFromDTO(ProductoCanalPromocionUpdateDTO dto, @MappingTarget ProductoCanalPromocion entity);
}
