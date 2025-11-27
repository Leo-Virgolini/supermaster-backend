package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    // ================================================================
    // ENTITY → DTO
    // ================================================================
    @Mapping(source = "marca.id", target = "marcaId")
    @Mapping(source = "origen.id", target = "origenId")
    @Mapping(source = "clasifGral.id", target = "clasifGralId")
    @Mapping(source = "clasifGastro.id", target = "clasifGastroId")
    @Mapping(source = "tipo.id", target = "tipoId")
    @Mapping(source = "proveedor.id", target = "proveedorId")
    @Mapping(source = "material.id", target = "materialId")
    ProductoDTO toDTO(Producto entity);

    // ================================================================
    // DTO CREATE → ENTITY
    // ================================================================
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "marca", expression = "java(dto.marcaId() != null ? new Marca(dto.marcaId()) : null)")
    @Mapping(target = "origen", expression = "java(new Origen(dto.origenId()))")
    @Mapping(target = "clasifGral", expression = "java(new ClasifGral(dto.clasifGralId()))")
    @Mapping(target = "clasifGastro", expression = "java(dto.clasifGastroId() != null ? new ClasifGastro(dto.clasifGastroId()) : null)")
    @Mapping(target = "tipo", expression = "java(new Tipo(dto.tipoId()))")
    @Mapping(target = "proveedor", expression = "java(dto.proveedorId() != null ? new Proveedor(dto.proveedorId()) : null)")
    @Mapping(target = "material", expression = "java(dto.materialId() != null ? new Material(dto.materialId()) : null)")
    @Mapping(target = "fechaCreacion", ignore = true)     // se setea en @PrePersist
    @Mapping(target = "fechaModificacion", ignore = true)
    // se setea en @PreUpdate
    Producto toEntity(ProductoCreateDTO dto);

    // ================================================================
    // DTO UPDATE → ENTITY (solo patch, ignora nulls)
    // ================================================================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "marca", expression = "java(dto.marcaId() != null ? new Marca(dto.marcaId()) : entity.getMarca())")
    @Mapping(target = "origen", expression = "java(dto.origenId() != null ? new Origen(dto.origenId()) : entity.getOrigen())")
    @Mapping(target = "clasifGral", expression = "java(dto.clasifGralId() != null ? new ClasifGral(dto.clasifGralId()) : entity.getClasifGral())")
    @Mapping(target = "clasifGastro", expression = "java(dto.clasifGastroId() != null ? new ClasifGastro(dto.clasifGastroId()) : entity.getClasifGastro())")
    @Mapping(target = "tipo", expression = "java(dto.tipoId() != null ? new Tipo(dto.tipoId()) : entity.getTipo())")
    @Mapping(target = "proveedor", expression = "java(dto.proveedorId() != null ? new Proveedor(dto.proveedorId()) : entity.getProveedor())")
    @Mapping(target = "material", expression = "java(dto.materialId() != null ? new Material(dto.materialId()) : entity.getMaterial())")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    void updateEntityFromDTO(ProductoUpdateDTO dto, @MappingTarget Producto entity);

    // ================================================================
    // RESUMEN PARA LISTADOS (si lo necesitás)
    // ================================================================
    @Named("toResumen")
    ProductoResumenDTO toResumenDTO(Producto entity);
}