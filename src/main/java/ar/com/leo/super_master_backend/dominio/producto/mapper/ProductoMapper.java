package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.dominio.common.mapper.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.producto.dto.*;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
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
        // Obtener MLA (si existe)
        var mlaEntity = producto.getMla();
        String mla = mlaEntity != null ? mlaEntity.getMla() : null;
        String mlau = mlaEntity != null ? mlaEntity.getMlau() : null;
        BigDecimal precioEnvio = mlaEntity != null ? mlaEntity.getPrecioEnvio() : null;

        // Mapear precios por canal
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
                        pcp.getGananciaRealPorcentaje(),
                        pcp.getGastosTotalPorcentaje(),
                        pcp.getFechaUltimoCalculo()
                ))
                .toList();

        // Calcular PVP mínimo y máximo (solo contado, cuotas=null)
        BigDecimal pvpMin = precios.stream()
                .filter(p -> p.getCuotas() == null && p.getPvp() != null)
                .map(ProductoCanalPrecio::getPvp)
                .min(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal pvpMax = precios.stream()
                .filter(p -> p.getCuotas() == null && p.getPvp() != null)
                .map(ProductoCanalPrecio::getPvp)
                .max(BigDecimal::compareTo)
                .orElse(null);

        return new ProductoConPreciosDTO(
                // Identificación
                producto.getId(),
                producto.getSku(),

                // MLA
                mla,
                mlau,
                precioEnvio,

                producto.getCodExt(),
                producto.getDescripcion(),
                producto.getTituloWeb(),
                producto.getEsCombo(),
                producto.getClasifGastro() != null ? producto.getClasifGastro().getEsMaquina() : null,
                producto.getImagenUrl(),
                producto.getStock(),
                producto.getActivo(),

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

                // Resumen de precios
                pvpMin,
                pvpMax,

                // Precios por canal
                preciosCanales
        );
    }
}