package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductoMapper {

    // ============================
    // ENTITY → DTO (SAFE)
    // ============================
    @Mapping(source = "marca.id", target = "marcaId")
    @Mapping(source = "origen.id", target = "origenId")
    @Mapping(source = "clasifGral.id", target = "clasifGralId")
    @Mapping(source = "clasifGastro.id", target = "clasifGastroId")
    @Mapping(source = "tipo.id", target = "tipoId")
    @Mapping(source = "proveedor.id", target = "proveedorId")
    @Mapping(source = "material.id", target = "materialId")
    ProductoDTO toDTO(Producto producto);

    // =============================
    // ENTITY → RESUMEN DTO
    // =============================
    @Mapping(source = "id", target = "id")
    @Mapping(source = "sku", target = "sku")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "tituloWeb", target = "tituloWeb")
    ProductoResumenDTO toResumen(Producto producto);

    // ============================
    // DTO CREATE → ENTITY (SAFE)
    // ============================
    @Mapping(target = "marca", expression = "java(dto.marcaId() != null ? new Marca(dto.marcaId()) : null)")
    @Mapping(target = "origen", expression = "java(dto.origenId() != null ? new Origen(dto.origenId()) : null)")
    @Mapping(target = "clasifGral", expression = "java(dto.clasifGralId() != null ? new ClasifGral(dto.clasifGralId()) : null)")
    @Mapping(target = "clasifGastro", expression = "java(dto.clasifGastroId() != null ? new ClasifGastro(dto.clasifGastroId()) : null)")
    @Mapping(target = "tipo", expression = "java(dto.tipoId() != null ? new Tipo(dto.tipoId()) : null)")
    @Mapping(target = "proveedor", expression = "java(dto.proveedorId() != null ? new Proveedor(dto.proveedorId()) : null)")
    @Mapping(target = "material", expression = "java(dto.materialId() != null ? new Material(dto.materialId()) : null)")
    Producto toEntity(ProductoCreateDTO dto);

    // ============================
    // DTO UPDATE → ENTITY EXISTENTE
    // ============================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "marca", expression = "java(dto.marcaId() != null ? new Marca(dto.marcaId()) : producto.getMarca())")
    @Mapping(target = "origen", expression = "java(dto.origenId() != null ? new Origen(dto.origenId()) : producto.getOrigen())")
    @Mapping(target = "clasifGral", expression = "java(dto.clasifGralId() != null ? new ClasifGral(dto.clasifGralId()) : producto.getClasifGral())")
    @Mapping(target = "clasifGastro", expression = "java(dto.clasifGastroId() != null ? new ClasifGastro(dto.clasifGastroId()) : producto.getClasifGastro())")
    @Mapping(target = "tipo", expression = "java(dto.tipoId() != null ? new Tipo(dto.tipoId()) : producto.getTipo())")
    @Mapping(target = "proveedor", expression = "java(dto.proveedorId() != null ? new Proveedor(dto.proveedorId()) : producto.getProveedor())")
    @Mapping(target = "material", expression = "java(dto.materialId() != null ? new Material(dto.materialId()) : producto.getMaterial())")
    void updateEntityFromDTO(ProductoUpdateDTO dto, @MappingTarget Producto producto);

}