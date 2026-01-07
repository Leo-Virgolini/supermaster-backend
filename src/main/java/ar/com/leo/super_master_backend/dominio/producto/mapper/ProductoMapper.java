package ar.com.leo.super_master_backend.dominio.producto.mapper;

import java.util.List;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoConPreciosDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
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

    // ================================================================
    // PRODUCTO CON PRECIOS POR CANAL
    // ================================================================
    default ProductoConPreciosDTO toProductoConPreciosDTO(Producto producto, List<ProductoCanalPrecio> precios) {
        List<ProductoConPreciosDTO.CanalPrecioDTO> preciosCanales = precios.stream()
                .map(pcp -> new ProductoConPreciosDTO.CanalPrecioDTO(
                        pcp.getCanal().getId(),
                        pcp.getCanal().getCanal(),
                        pcp.getCuotas(),
                        pcp.getPvp(),
                        pcp.getPvpInflado(),
                        pcp.getCostoTotal(),
                        pcp.getGananciaAbs(),
                        pcp.getGananciaPorcentaje(),
                        pcp.getGastosTotalPorcentaje(),
                        pcp.getFechaUltimoCalculo()
                ))
                .toList();

        return new ProductoConPreciosDTO(
                // Identificación
                producto.getId(),
                producto.getSku(),
                producto.getCodExt(),
                producto.getDescripcion(),
                producto.getTituloWeb(),
                producto.getEsCombo(),

                // Relaciones (nombres)
                producto.getMarca() != null ? producto.getMarca().getNombre() : null,
                producto.getOrigen() != null ? producto.getOrigen().getOrigen() : null,
                producto.getClasifGral() != null ? producto.getClasifGral().getNombre() : null,
                producto.getClasifGastro() != null ? producto.getClasifGastro().getNombre() : null,
                producto.getTipo() != null ? producto.getTipo().getNombre() : null,
                producto.getProveedor() != null ? producto.getProveedor().getProveedor() : null,
                producto.getMaterial() != null ? producto.getMaterial().getMaterial() : null,

                // Dimensiones y atributos
                producto.getUxb(),
                producto.getCapacidad(),
                producto.getLargo(),
                producto.getAncho(),
                producto.getAlto(),
                producto.getDiamboca(),
                producto.getDiambase(),
                producto.getEspesor(),

                // Precios y costos
                producto.getCosto(),
                producto.getFechaUltCosto(),
                producto.getIva(),

                // Fechas
                producto.getFechaCreacion(),
                producto.getFechaModificacion(),

                // Precios por canal
                preciosCanales
        );
    }
}