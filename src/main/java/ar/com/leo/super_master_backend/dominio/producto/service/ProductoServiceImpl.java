package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.*;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final RecalculoPrecioFacade recalculoFacade;

    // ============================
    // LISTAR
    // ============================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDTO> listar(Pageable pageable) {
        return productoRepository.findAll(pageable)
                .map(productoMapper::toDTO);
    }

    // ============================
    // OBTENER
    // ============================
    @Override
    @Transactional(readOnly = true)
    public ProductoDTO obtener(Integer id) {
        return productoRepository.findById(id)
                .map(productoMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    // ============================
    // OBTENER CON PRECIOS POR CANAL
    // ============================
    @Override
    @Transactional(readOnly = true)
    public ProductoConPreciosDTO obtenerConPrecios(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Ordenado por canal y cuotas (null primero = contado, luego 3, 6, 12...)
        List<ProductoCanalPrecio> precios = productoCanalPrecioRepository.findByProductoIdOrderByCanalIdAscCuotasAsc(id);

        return productoMapper.toProductoConPreciosDTO(producto, precios);
    }

    // ============================
    // CREAR
    // ============================
    @Override
    @Transactional
    public ProductoDTO crear(ProductoCreateDTO dto) {
        // Validar SKU único
        if (productoRepository.findBySku(dto.sku()).isPresent()) {
            throw new ConflictException("Ya existe un producto con el SKU: " + dto.sku());
        }

        Producto entity = productoMapper.toEntity(dto);
        productoRepository.save(entity);
        return productoMapper.toDTO(entity);
    }

    // ============================
    // ACTUALIZAR
    // ============================
    @Override
    @Transactional
    public ProductoDTO actualizar(Integer id, ProductoUpdateDTO dto) {
        Producto entity = productoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Guardar valores anteriores para detectar cambios
        BigDecimal costoAnterior = entity.getCosto();
        BigDecimal ivaAnterior = entity.getIva();

        productoMapper.updateEntityFromDTO(dto, entity);
        productoRepository.save(entity);

        // Recalcular precios si cambió costo o IVA
        boolean cambioCosto = dto.costo() != null && !Objects.equals(costoAnterior, dto.costo());
        boolean cambioIva = dto.iva() != null && !Objects.equals(ivaAnterior, dto.iva());

        if (cambioCosto || cambioIva) {
            recalculoFacade.recalcularPorCambioProducto(id);
        }

        return productoMapper.toDTO(entity);
    }

    // ============================
    // ELIMINAR
    // ============================
    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!productoRepository.existsById(id)) {
            throw new NotFoundException("Producto no encontrado");
        }
        productoRepository.deleteById(id);
    }

    // ============================
    // OBTENER POR SKU
    // ============================
    @Override
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .map(productoMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    // ======================================================
    // BUSQUEDA / FILTRADO PROFESIONAL
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoDTO> filtrar(ProductoFilter filter, Pageable pageable) {

        Specification<Producto> spec = Specification.allOf(

                // =======================
                // 1) TEXTO
                // =======================
                ProductoSpecifications.textoLike(filter.texto()),

                // =======================
                // 2) BOOLEANOS / NÚMEROS
                // =======================
                ProductoSpecifications.esCombo(filter.esCombo()),
                ProductoSpecifications.uxb(filter.uxb()),
                ProductoSpecifications.esMaquina(filter.esMaquina()),
                ProductoSpecifications.tieneMla(filter.tieneMla()),
                ProductoSpecifications.activo(filter.activo()),

                // =======================
                // 3) MANY-TO-ONE
                // =======================
                ProductoSpecifications.marcaId(filter.marcaId()),
                ProductoSpecifications.origenId(filter.origenId()),
                ProductoSpecifications.tipoId(filter.tipoId()),
                ProductoSpecifications.clasifGralId(filter.clasifGralId()),
                ProductoSpecifications.clasifGastroId(filter.clasifGastroId()),
                ProductoSpecifications.proveedorId(filter.proveedorId()),
                ProductoSpecifications.materialId(filter.materialId()),

                // =======================
                // 4) RANGOS (costo / iva / stock)
                // =======================
                ProductoSpecifications.costoMin(filter.costoMin()),
                ProductoSpecifications.costoMax(filter.costoMax()),
                ProductoSpecifications.ivaMin(filter.ivaMin()),
                ProductoSpecifications.ivaMax(filter.ivaMax()),
                ProductoSpecifications.stockMin(filter.stockMin()),
                ProductoSpecifications.stockMax(filter.stockMax()),

                // =======================
                // 5) RANGO PVP
                // =======================
                ProductoSpecifications.pvpEnRango(filter.pvpMin(), filter.pvpMax(), filter.pvpCanalId()),

                // =======================
                // 6) FECHAS
                // =======================
                ProductoSpecifications.desdeFechaUltCosto(filter.desdeFechaUltCosto()),
                ProductoSpecifications.hastaFechaUltCosto(filter.hastaFechaUltCosto()),
                ProductoSpecifications.desdeFechaCreacion(filter.desdeFechaCreacion()),
                ProductoSpecifications.hastaFechaCreacion(filter.hastaFechaCreacion()),
                ProductoSpecifications.desdeFechaModificacion(filter.desdeFechaModificacion()),
                ProductoSpecifications.hastaFechaModificacion(filter.hastaFechaModificacion()),

                // =======================
                // 7) MANY-TO-MANY
                // =======================
                ProductoSpecifications.aptoIds(filter.aptoIds()),
                ProductoSpecifications.canalIds(filter.canalIds()),
                ProductoSpecifications.catalogoIds(filter.catalogoIds()),
                ProductoSpecifications.clienteIds(filter.clienteIds()),
                ProductoSpecifications.mlaIds(filter.mlaIds())
        );

        return productoRepository.findAll(spec, pageable)
                .map(productoMapper::toDTO);
    }

    // ======================================================
    // LISTAR CON PRECIOS (PAGINADO)
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoConPreciosDTO> listarConPrecios(ProductoFilter filter, Pageable pageable) {

        Specification<Producto> spec = Specification.allOf(
                // Texto
                ProductoSpecifications.textoLike(filter.texto()),
                // Booleanos/Numéricos
                ProductoSpecifications.esCombo(filter.esCombo()),
                ProductoSpecifications.uxb(filter.uxb()),
                ProductoSpecifications.esMaquina(filter.esMaquina()),
                ProductoSpecifications.tieneMla(filter.tieneMla()),
                ProductoSpecifications.activo(filter.activo()),
                // Many-to-One
                ProductoSpecifications.marcaId(filter.marcaId()),
                ProductoSpecifications.origenId(filter.origenId()),
                ProductoSpecifications.tipoId(filter.tipoId()),
                ProductoSpecifications.clasifGralId(filter.clasifGralId()),
                ProductoSpecifications.clasifGastroId(filter.clasifGastroId()),
                ProductoSpecifications.proveedorId(filter.proveedorId()),
                ProductoSpecifications.materialId(filter.materialId()),
                // Rangos
                ProductoSpecifications.costoMin(filter.costoMin()),
                ProductoSpecifications.costoMax(filter.costoMax()),
                ProductoSpecifications.ivaMin(filter.ivaMin()),
                ProductoSpecifications.ivaMax(filter.ivaMax()),
                ProductoSpecifications.stockMin(filter.stockMin()),
                ProductoSpecifications.stockMax(filter.stockMax()),
                // Rango PVP
                ProductoSpecifications.pvpEnRango(filter.pvpMin(), filter.pvpMax(), filter.pvpCanalId()),
                // Fechas
                ProductoSpecifications.desdeFechaUltCosto(filter.desdeFechaUltCosto()),
                ProductoSpecifications.hastaFechaUltCosto(filter.hastaFechaUltCosto()),
                ProductoSpecifications.desdeFechaCreacion(filter.desdeFechaCreacion()),
                ProductoSpecifications.hastaFechaCreacion(filter.hastaFechaCreacion()),
                ProductoSpecifications.desdeFechaModificacion(filter.desdeFechaModificacion()),
                ProductoSpecifications.hastaFechaModificacion(filter.hastaFechaModificacion()),
                // Many-to-Many
                ProductoSpecifications.aptoIds(filter.aptoIds()),
                ProductoSpecifications.canalIds(filter.canalIds()),
                ProductoSpecifications.catalogoIds(filter.catalogoIds()),
                ProductoSpecifications.clienteIds(filter.clienteIds()),
                ProductoSpecifications.mlaIds(filter.mlaIds())
        );

        // 1) Obtener página de productos (entidades)
        Page<Producto> productosPage = productoRepository.findAll(spec, pageable);

        if (productosPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 2) Extraer IDs de productos
        List<Integer> productoIds = productosPage.getContent().stream()
                .map(Producto::getId)
                .toList();

        // 3) Obtener todos los precios en UNA query (evita N+1)
        List<ProductoCanalPrecio> todosPrecios = productoCanalPrecioRepository
                .findByProductoIdInOrderByProductoIdAscCanalIdAscCuotasAsc(productoIds);

        // 4) Agrupar precios por producto_id
        Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto = todosPrecios.stream()
                .collect(Collectors.groupingBy(pcp -> pcp.getProducto().getId()));

        // 5) Mapear cada producto + sus precios a DTO
        List<ProductoConPreciosDTO> dtos = productosPage.getContent().stream()
                .map(producto -> {
                    List<ProductoCanalPrecio> precios = preciosPorProducto
                            .getOrDefault(producto.getId(), Collections.emptyList());
                    return productoMapper.toProductoConPreciosDTO(producto, precios);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // 6) Aplicar ordenamiento especial si se solicita
        if (filter.sortBy() != null && !filter.sortBy().isBlank()) {
            boolean asc = !"desc".equalsIgnoreCase(filter.sortDir());
            Comparator<ProductoConPreciosDTO> comparator = getComparator(filter.sortBy(), filter.sortCanalId(), preciosPorProducto);
            if (comparator != null) {
                dtos.sort(asc ? comparator : comparator.reversed());
            }
        }

        // 7) Retornar nueva Page con el mismo metadata
        return new PageImpl<>(dtos, pageable, productosPage.getTotalElements());
    }

    /**
     * Obtiene un comparador basado en el campo de ordenamiento.
     */
    private Comparator<ProductoConPreciosDTO> getComparator(
            String sortBy,
            Integer sortCanalId,
            Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto) {

        return switch (sortBy.toLowerCase()) {
            case "costo" -> Comparator.comparing(
                    ProductoConPreciosDTO::costo,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "mla" -> Comparator.comparing(
                    ProductoConPreciosDTO::mla,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "esmaquina" -> Comparator.comparing(
                    ProductoConPreciosDTO::esMaquina,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "pvp" -> {
                if (sortCanalId == null) {
                    // Si no se especifica canal, ordenar por pvpMin
                    yield Comparator.comparing(
                            ProductoConPreciosDTO::pvpMin,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    );
                }
                // Ordenar por PVP del canal específico (contado)
                yield Comparator.comparing(
                        (ProductoConPreciosDTO dto) -> {
                            List<ProductoCanalPrecio> precios = preciosPorProducto.get(dto.id());
                            if (precios == null) return null;
                            return precios.stream()
                                    .filter(p -> p.getCanal().getId().equals(sortCanalId) && p.getCuotas() == null)
                                    .map(ProductoCanalPrecio::getPvp)
                                    .findFirst()
                                    .orElse(null);
                        },
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
            }
            default -> null;
        };
    }

    // ============================
    // ACTUALIZAR COSTO + RECALCULAR PRECIOS
    // ============================
    @Override
    @Transactional
    public void actualizarCosto(Integer idProducto, BigDecimal nuevoCosto) {

        // 1) Actualizar costo del producto
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        producto.setCosto(nuevoCosto);
        productoRepository.save(producto);

        // 2) Recalcular precios en todos los canales
        recalculoFacade.recalcularPorCambioProducto(idProducto);
    }

}