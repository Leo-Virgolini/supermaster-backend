package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    // =============================
    // ENTITY → DTO
    // =============================
    @Mapping(source = "marca.id", target = "marcaId")
    @Mapping(source = "origen.id", target = "origenId")
    @Mapping(source = "clasifGral.id", target = "clasifGralId")
    @Mapping(source = "clasifGastro.id", target = "clasifGastroId")
    @Mapping(source = "tipo.id", target = "tipoId")
    @Mapping(source = "proveedor.id", target = "proveedorId")
    @Mapping(source = "material.id", target = "materialId")
    ProductoDTO toDTO(Producto producto);


    // =============================
    // DTO CREATE → ENTITY
    // =============================
    @Mapping(source = "marcaId", target = "marca.id")
    @Mapping(source = "origenId", target = "origen.id")
    @Mapping(source = "clasifGralId", target = "clasifGral.id")
    @Mapping(source = "clasifGastroId", target = "clasifGastro.id")
    @Mapping(source = "tipoId", target = "tipo.id")
    @Mapping(source = "proveedorId", target = "proveedor.id")
    @Mapping(source = "materialId", target = "material.id")
    Producto toEntity(ProductoCreateDTO dto);


    // =============================
    // DTO UPDATE → ENTITY existente
    // =============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "marcaId", target = "marca.id")
    @Mapping(source = "origenId", target = "origen.id")
    @Mapping(source = "clasifGralId", target = "clasifGral.id")
    @Mapping(source = "clasifGastroId", target = "clasifGastro.id")
    @Mapping(source = "tipoId", target = "tipo.id")
    @Mapping(source = "proveedorId", target = "proveedor.id")
    @Mapping(source = "materialId", target = "material.id")
    void updateEntityFromDTO(ProductoUpdateDTO dto, @MappingTarget Producto producto);

}