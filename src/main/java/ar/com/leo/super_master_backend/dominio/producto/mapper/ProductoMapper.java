package ar.com.leo.super_master_backend.dominio.producto.mapper;

import ar.com.leo.super_master_backend.config.GlobalMapperConfig;
import ar.com.leo.super_master_backend.dominio.producto.dto.*;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(config = GlobalMapperConfig.class, imports = {
        ar.com.leo.super_master_backend.dominio.marca.entity.Marca.class,
        ar.com.leo.super_master_backend.dominio.origen.entity.Origen.class,
        ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral.class,
        ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro.class,
        ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo.class,
        ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor.class,
        ar.com.leo.super_master_backend.dominio.material.entity.Material.class
})
public interface ProductoMapper {

    // ================================================================
    // ENTITY → DTO
    // ================================================================
    default ProductoDTO toDTO(Producto entity) {
        if (entity == null) return null;
        return new ProductoDTO(
                entity.getId(),
                entity.getSku(),
                entity.getCodExt(),
                entity.getDescripcion(),
                entity.getTituloWeb(),
                entity.getEsCombo(),
                entity.getUxb(),
                entity.getMoq(),
                entity.getImagenUrl(),
                entity.getStock(),
                entity.getActivo(),
                entity.getTagReposicion(),
                entity.getMarca() != null ? entity.getMarca().getId() : null,
                entity.getOrigen() != null ? entity.getOrigen().getId() : null,
                entity.getClasifGral() != null ? entity.getClasifGral().getId() : null,
                entity.getClasifGastro() != null ? entity.getClasifGastro().getId() : null,
                entity.getTipo() != null ? entity.getTipo().getId() : null,
                entity.getProveedor() != null ? entity.getProveedor().getId() : null,
                entity.getMaterial() != null ? entity.getMaterial().getId() : null,
                entity.getMla() != null ? entity.getMla().getId() : null,
                entity.getCapacidad(),
                entity.getLargo(),
                entity.getAncho(),
                entity.getAlto(),
                entity.getDiamboca(),
                entity.getDiambase(),
                entity.getEspesor(),
                entity.getCosto(),
                entity.getFechaUltimoCosto(),
                entity.getIva(),
                entity.getFechaCreacion(),
                entity.getFechaModificacion(),
                entity.getProductosApto() != null
                        ? entity.getProductosApto().stream()
                            .map(pa -> pa.getApto().getApto())
                            .sorted()
                            .toList()
                        : List.of(),
                entity.getProductoCatalogos() != null
                        ? entity.getProductoCatalogos().stream()
                            .map(pc -> pc.getCatalogo().getCatalogo())
                            .sorted()
                            .toList()
                        : List.of(),
                entity.getProductoClientes() != null
                        ? entity.getProductoClientes().stream()
                            .map(pcl -> pcl.getCliente().getCliente())
                            .sorted()
                            .toList()
                        : List.of()
        );
    }

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

    /**
     * Versión que usa descripciones por defecto (para compatibilidad).
     */
    default ProductoConPreciosDTO toProductoConPreciosDTO(Producto producto, ProductoMargen productoMargen, List<ProductoCanalPrecio> precios) {
        return toProductoConPreciosDTO(producto, productoMargen, precios, null, null, null);
    }

    /**
     * Versión que usa descripciones de canal_concepto_cuota.
     * @param descripcionesCuotas Mapa de (canalId + "_" + cuotas) -> descripcion
     */
    default ProductoConPreciosDTO toProductoConPreciosDTO(Producto producto, ProductoMargen productoMargen, List<ProductoCanalPrecio> precios, Map<String, String> descripcionesCuotas) {
        return toProductoConPreciosDTO(producto, productoMargen, precios, descripcionesCuotas, null, null);
    }

    /**
     * Versión completa que incluye descuentos aplicables por canal.
     * @param descripcionesCuotas Mapa de (canalId + "_" + cuotas) -> descripcion
     * @param descuentosPorCanal Mapa de canalId -> lista de descuentos aplicables (puede ser null)
     */
    default ProductoConPreciosDTO toProductoConPreciosDTO(
            Producto producto,
            ProductoMargen productoMargen,
            List<ProductoCanalPrecio> precios,
            Map<String, String> descripcionesCuotas,
            Map<Integer, List<DescuentoAplicableDTO>> descuentosPorCanal) {
        return toProductoConPreciosDTO(producto, productoMargen, precios, descripcionesCuotas, descuentosPorCanal, null);
    }

    /**
     * Versión completa que incluye nombres de canales explícitos.
     * @param descripcionesCuotas Mapa de (canalId + "_" + cuotas) -> descripcion
     * @param descuentosPorCanal Mapa de canalId -> lista de descuentos aplicables (puede ser null)
     * @param nombresPorCanal Mapa de canalId -> nombre del canal (puede ser null, usa lazy loading)
     */
    default ProductoConPreciosDTO toProductoConPreciosDTO(
            Producto producto,
            ProductoMargen productoMargen,
            List<ProductoCanalPrecio> precios,
            Map<String, String> descripcionesCuotas,
            Map<Integer, List<DescuentoAplicableDTO>> descuentosPorCanal,
            Map<Integer, String> nombresPorCanal) {
        // Obtener MLA (si existe)
        ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla mlaEntity = producto.getMla();
        String mla = mlaEntity != null ? mlaEntity.getMla() : null;
        String mlau = mlaEntity != null ? mlaEntity.getMlau() : null;
        BigDecimal precioEnvio = mlaEntity != null ? mlaEntity.getPrecioEnvio() : null;
        java.time.LocalDateTime fechaCalculoEnvio = mlaEntity != null ? mlaEntity.getFechaCalculoEnvio() : null;
        BigDecimal comisionPorcentaje = mlaEntity != null ? mlaEntity.getComisionPorcentaje() : null;

        // Obtener márgenes (si existen)
        BigDecimal margenMinorista = productoMargen != null ? productoMargen.getMargenMinorista() : null;
        BigDecimal margenMayorista = productoMargen != null ? productoMargen.getMargenMayorista() : null;

        // Agrupar precios por canal
        Map<Integer, List<ProductoCanalPrecio>> preciosPorCanal = precios.stream()
                .collect(Collectors.groupingBy(pcp -> pcp.getCanal().getId()));

        List<CanalPreciosDTO> preciosCanales = preciosPorCanal.entrySet().stream()
                .map(entry -> {
                    Integer canalId = entry.getKey();
                    List<ProductoCanalPrecio> preciosDelCanal = entry.getValue();
                    ProductoCanalPrecio primerPrecio = preciosDelCanal.get(0);

                    // Obtener descuentos para este canal (si existen)
                    List<DescuentoAplicableDTO> descuentosCanal = descuentosPorCanal != null
                            ? descuentosPorCanal.get(canalId)
                            : null;

                    List<PrecioDTO> preciosList = preciosDelCanal.stream()
                            .map(pcp -> new PrecioDTO(
                                    pcp.getCuotas(),
                                    obtenerDescripcionCuota(pcp.getCanal().getId(), pcp.getCuotas(), descripcionesCuotas),
                                    pcp.getPvp(),
                                    pcp.getPvpInflado(),
                                    pcp.getCostoProducto(),
                                    pcp.getCostosVenta(),
                                    pcp.getIngresoNetoVendedor(),
                                    pcp.getGanancia(),
                                    pcp.getMargenSobreIngresoNeto(),
                                    pcp.getMargenSobrePvp(),
                                    pcp.getMarkupPorcentaje(),
                                    pcp.getFechaUltimoCalculo(),
                                    descuentosCanal
                            ))
                            .toList();

                    // Obtener nombre del canal: primero del mapa explícito, luego de la entidad
                    String canalNombre = nombresPorCanal != null
                            ? nombresPorCanal.get(canalId)
                            : primerPrecio.getCanal().getCanal();

                    return new CanalPreciosDTO(
                            canalId,
                            canalNombre,
                            preciosList
                    );
                })
                .sorted((a, b) -> a.canalId().compareTo(b.canalId()))
                .toList();

        return new ProductoConPreciosDTO(
                // Identificación
                producto.getId(),
                producto.getSku(),

                // MLA
                mla,
                mlau,
                precioEnvio,
                fechaCalculoEnvio,
                comisionPorcentaje,

                producto.getCodExt(),
                producto.getDescripcion(),
                producto.getTituloWeb(),
                producto.getEsCombo(),
                producto.getClasifGastro() != null ? producto.getClasifGastro().getEsMaquina() : null,
                producto.getImagenUrl(),
                producto.getStock(),
                producto.getActivo(),
                producto.getTagReposicion(),

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
                producto.getMoq(),
                producto.getCapacidad(),
                producto.getLargo(),
                producto.getAncho(),
                producto.getAlto(),
                producto.getDiamboca(),
                producto.getDiambase(),
                producto.getEspesor(),

                // Precios y costos
                producto.getCosto(),
                producto.getFechaUltimoCosto(),
                producto.getIva(),

                // Márgenes
                margenMinorista,
                margenMayorista,

                // Fechas
                producto.getFechaCreacion(),
                producto.getFechaModificacion(),

                // Many-to-many (nombres)
                producto.getProductosApto() != null
                        ? producto.getProductosApto().stream()
                            .map(pa -> pa.getApto().getApto())
                            .sorted()
                            .toList()
                        : List.of(),
                producto.getProductoCatalogos() != null
                        ? producto.getProductoCatalogos().stream()
                            .map(pc -> pc.getCatalogo().getCatalogo())
                            .sorted()
                            .toList()
                        : List.of(),
                producto.getProductoClientes() != null
                        ? producto.getProductoClientes().stream()
                            .map(pcl -> pcl.getCliente().getCliente())
                            .sorted()
                            .toList()
                        : List.of(),

                // Precios por canal
                preciosCanales
        );
    }

    /**
     * Obtiene la descripción de la cuota del mapa o genera una por defecto.
     */
    default String obtenerDescripcionCuota(Integer canalId, Integer cuotas, Map<String, String> descripcionesCuotas) {
        if (descripcionesCuotas != null && canalId != null && cuotas != null) {
            String key = canalId + "_" + cuotas;
            String descripcion = descripcionesCuotas.get(key);
            if (descripcion != null && !descripcion.isBlank()) {
                return descripcion;
            }
        }
        return null;
    }
}