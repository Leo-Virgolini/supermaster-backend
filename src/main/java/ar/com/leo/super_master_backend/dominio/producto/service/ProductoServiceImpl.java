package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.*;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoMargenRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ProductoMargenRepository productoMargenRepository;
    private final RecalculoPrecioFacade recalculoFacade;
    private final CanalRepository canalRepository;
    private final CanalConceptoCuotaRepository canalConceptoCuotaRepository;

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
        Integer clasifGastroIdAnterior = entity.getClasifGastro() != null ? entity.getClasifGastro().getId() : null;

        productoMapper.updateEntityFromDTO(dto, entity);
        productoRepository.save(entity);

        // Recalcular precios si cambió costo o IVA
        boolean cambioCosto = dto.costo() != null && !Objects.equals(costoAnterior, dto.costo());
        boolean cambioIva = dto.iva() != null && !Objects.equals(ivaAnterior, dto.iva());
        boolean cambioClasifGastro = dto.clasifGastroId() != null && !Objects.equals(clasifGastroIdAnterior, dto.clasifGastroId());

        if (cambioCosto || cambioIva || cambioClasifGastro) {
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
                ProductoSpecifications.textoLike(filter.search()),

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

        // Validar que el canal tenga las cuotas especificadas
        validarCanalConCuotas(filter.canalId(), filter.cuotas());

        Specification<Producto> spec = Specification.allOf(
                // ID
                ProductoSpecifications.productoId(filter.productoId()),
                // Texto
                ProductoSpecifications.textoLike(filter.search()),
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

        // 4.1) Obtener todos los márgenes en UNA query (evita N+1)
        List<ProductoMargen> todosMargenes = productoMargenRepository.findByProductoIdIn(productoIds);
        Map<Integer, ProductoMargen> margenesPorProducto = todosMargenes.stream()
                .collect(Collectors.toMap(pm -> pm.getProducto().getId(), pm -> pm));

        // 4.2) Obtener descripciones de cuotas por canal (para PrecioDTO.descripcion)
        Set<Integer> canalIds = todosPrecios.stream()
                .map(p -> p.getCanal().getId())
                .collect(Collectors.toSet());
        Map<String, String> descripcionesCuotas = canalConceptoCuotaRepository.findByCanalIdIn(canalIds).stream()
                .filter(c -> c.getDescripcion() != null)
                .collect(Collectors.toMap(
                        c -> c.getCanal().getId() + "_" + c.getCuotas(),
                        CanalConceptoCuota::getDescripcion,
                        (a, b) -> a
                ));

        // 5) Mapear cada producto + sus márgenes + sus precios a DTO
        List<ProductoConPreciosDTO> dtos = productosPage.getContent().stream()
                .map(producto -> {
                    ProductoMargen productoMargen = margenesPorProducto.get(producto.getId());
                    List<ProductoCanalPrecio> precios = preciosPorProducto
                            .getOrDefault(producto.getId(), Collections.emptyList());

                    // Filtrar por canalId (singular) si se especifica
                    if (filter.canalId() != null) {
                        precios = precios.stream()
                                .filter(p -> p.getCanal().getId().equals(filter.canalId()))
                                .toList();
                    }
                    // Filtrar por canalIds (lista) si se especifica
                    else if (filter.canalIds() != null && !filter.canalIds().isEmpty()) {
                        precios = precios.stream()
                                .filter(p -> filter.canalIds().contains(p.getCanal().getId()))
                                .toList();
                    }

                    // Filtrar por cuotas si se especifica
                    if (filter.cuotas() != null) {
                        precios = precios.stream()
                                .filter(p -> filter.cuotas().equals(p.getCuotas()))
                                .toList();
                    }

                    return productoMapper.toProductoConPreciosDTO(producto, productoMargen, precios, descripcionesCuotas);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // 5.1) Si se filtra por canal o cuotas, excluir productos sin precios
        if (filter.canalId() != null || filter.cuotas() != null) {
            dtos.removeIf(dto -> dto.canales() == null || dto.canales().isEmpty() ||
                    dto.canales().stream().allMatch(c -> c.precios() == null || c.precios().isEmpty()));
        }

        // 6) Aplicar ordenamiento especial si se solicita (detectar desde Pageable.sort)
        aplicarOrdenamientoEspecial(dtos, pageable.getSort(), filter.sortCanalId(), preciosPorProducto);

        // 7) Retornar nueva Page con el mismo metadata
        return new PageImpl<>(dtos, pageable, productosPage.getTotalElements());
    }

    /**
     * Lista de campos que requieren ordenamiento especial (en memoria).
     */
    private static final Set<String> CAMPOS_SORT_ESPECIAL = Set.of("pvp", "mla", "esmaquina", "costo");

    /**
     * Aplica ordenamiento especial si el sort del Pageable contiene un campo especial.
     */
    private void aplicarOrdenamientoEspecial(
            List<ProductoConPreciosDTO> dtos,
            Sort sort,
            Integer sortCanalId,
            Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto) {

        if (sort == null || sort.isUnsorted()) {
            return;
        }

        // Buscar el primer campo de ordenamiento especial
        for (Sort.Order order : sort) {
            String campo = order.getProperty().toLowerCase();
            if (CAMPOS_SORT_ESPECIAL.contains(campo)) {
                Comparator<ProductoConPreciosDTO> comparator = getComparator(campo, sortCanalId, preciosPorProducto);
                if (comparator != null) {
                    boolean asc = order.isAscending();
                    dtos.sort(asc ? comparator : comparator.reversed());
                }
                break; // Solo aplicar el primer campo especial encontrado
            }
        }
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
                    // Sin canal especificado no se puede ordenar por PVP
                    yield null;
                }
                // Ordenar por PVP del canal específico (cuotas=0 o primera cuota disponible)
                yield Comparator.comparing(
                        (ProductoConPreciosDTO dto) -> {
                            List<ProductoCanalPrecio> precios = preciosPorProducto.get(dto.id());
                            if (precios == null) return null;
                            // Primero buscar cuotas=0, si no existe usar la primera cuota disponible
                            return precios.stream()
                                    .filter(p -> p.getCanal().getId().equals(sortCanalId))
                                    .sorted(Comparator.comparing(ProductoCanalPrecio::getCuotas,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
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
    // LISTAR SIN PAGINACIÓN (PARA EXPORTACIÓN)
    // ============================
    @Override
    @Transactional(readOnly = true)
    public List<ProductoConPreciosDTO> listarConPreciosSinPaginar(ProductoFilter filter, Sort sort) {

        // Validar que el canal tenga las cuotas especificadas
        validarCanalConCuotas(filter.canalId(), filter.cuotas());

        Specification<Producto> spec = Specification.allOf(
                ProductoSpecifications.productoId(filter.productoId()),
                ProductoSpecifications.textoLike(filter.search()),
                ProductoSpecifications.esCombo(filter.esCombo()),
                ProductoSpecifications.uxb(filter.uxb()),
                ProductoSpecifications.esMaquina(filter.esMaquina()),
                ProductoSpecifications.tieneMla(filter.tieneMla()),
                ProductoSpecifications.activo(filter.activo()),
                ProductoSpecifications.marcaId(filter.marcaId()),
                ProductoSpecifications.origenId(filter.origenId()),
                ProductoSpecifications.tipoId(filter.tipoId()),
                ProductoSpecifications.clasifGralId(filter.clasifGralId()),
                ProductoSpecifications.clasifGastroId(filter.clasifGastroId()),
                ProductoSpecifications.proveedorId(filter.proveedorId()),
                ProductoSpecifications.materialId(filter.materialId()),
                ProductoSpecifications.costoMin(filter.costoMin()),
                ProductoSpecifications.costoMax(filter.costoMax()),
                ProductoSpecifications.ivaMin(filter.ivaMin()),
                ProductoSpecifications.ivaMax(filter.ivaMax()),
                ProductoSpecifications.stockMin(filter.stockMin()),
                ProductoSpecifications.stockMax(filter.stockMax()),
                ProductoSpecifications.pvpEnRango(filter.pvpMin(), filter.pvpMax(), filter.pvpCanalId()),
                ProductoSpecifications.desdeFechaUltCosto(filter.desdeFechaUltCosto()),
                ProductoSpecifications.hastaFechaUltCosto(filter.hastaFechaUltCosto()),
                ProductoSpecifications.desdeFechaCreacion(filter.desdeFechaCreacion()),
                ProductoSpecifications.hastaFechaCreacion(filter.hastaFechaCreacion()),
                ProductoSpecifications.desdeFechaModificacion(filter.desdeFechaModificacion()),
                ProductoSpecifications.hastaFechaModificacion(filter.hastaFechaModificacion()),
                ProductoSpecifications.aptoIds(filter.aptoIds()),
                ProductoSpecifications.canalIds(filter.canalIds()),
                ProductoSpecifications.catalogoIds(filter.catalogoIds()),
                ProductoSpecifications.clienteIds(filter.clienteIds()),
                ProductoSpecifications.mlaIds(filter.mlaIds())
        );

        // Obtener todos los productos (sin paginación)
        List<Producto> productos = productoRepository.findAll(spec);

        if (productos.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> productoIds = productos.stream()
                .map(Producto::getId)
                .toList();

        // Obtener todos los precios
        List<ProductoCanalPrecio> todosPrecios = productoCanalPrecioRepository
                .findByProductoIdInOrderByProductoIdAscCanalIdAscCuotasAsc(productoIds);

        Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto = todosPrecios.stream()
                .collect(Collectors.groupingBy(pcp -> pcp.getProducto().getId()));

        // Obtener todos los márgenes
        List<ProductoMargen> todosMargenes = productoMargenRepository.findByProductoIdIn(productoIds);
        Map<Integer, ProductoMargen> margenesPorProducto = todosMargenes.stream()
                .collect(Collectors.toMap(pm -> pm.getProducto().getId(), pm -> pm));

        // Obtener descripciones de cuotas por canal (para PrecioDTO.descripcion)
        Set<Integer> canalIds = todosPrecios.stream()
                .map(p -> p.getCanal().getId())
                .collect(Collectors.toSet());
        Map<String, String> descripcionesCuotas = canalConceptoCuotaRepository.findByCanalIdIn(canalIds).stream()
                .filter(c -> c.getDescripcion() != null)
                .collect(Collectors.toMap(
                        c -> c.getCanal().getId() + "_" + c.getCuotas(),
                        CanalConceptoCuota::getDescripcion,
                        (a, b) -> a
                ));

        // Mapear a DTOs
        List<ProductoConPreciosDTO> dtos = productos.stream()
                .map(producto -> {
                    ProductoMargen productoMargen = margenesPorProducto.get(producto.getId());
                    List<ProductoCanalPrecio> precios = preciosPorProducto
                            .getOrDefault(producto.getId(), Collections.emptyList());

                    if (filter.canalId() != null) {
                        precios = precios.stream()
                                .filter(p -> p.getCanal().getId().equals(filter.canalId()))
                                .toList();
                    } else if (filter.canalIds() != null && !filter.canalIds().isEmpty()) {
                        precios = precios.stream()
                                .filter(p -> filter.canalIds().contains(p.getCanal().getId()))
                                .toList();
                    }

                    // Filtrar por cuotas si se especifica
                    if (filter.cuotas() != null) {
                        precios = precios.stream()
                                .filter(p -> filter.cuotas().equals(p.getCuotas()))
                                .toList();
                    }

                    return productoMapper.toProductoConPreciosDTO(producto, productoMargen, precios, descripcionesCuotas);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Si se filtra por canal o cuotas, excluir productos sin precios
        if (filter.canalId() != null || filter.cuotas() != null) {
            dtos.removeIf(dto -> dto.canales() == null || dto.canales().isEmpty() ||
                    dto.canales().stream().allMatch(c -> c.precios() == null || c.precios().isEmpty()));
        }

        // Aplicar ordenamiento especial si se solicita
        aplicarOrdenamientoEspecial(dtos, sort, filter.sortCanalId(), preciosPorProducto);

        return dtos;
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

    // ============================
    // VALIDACIÓN DE CANAL CON CUOTAS
    // ============================
    private void validarCanalConCuotas(Integer canalId, Integer cuotas) {
        if (canalId != null && cuotas != null) {
            boolean existenPrecios = productoCanalPrecioRepository.existsByCanalIdAndCuotas(canalId, cuotas);
            if (!existenPrecios) {
                String nombreCanal = canalRepository.findById(canalId)
                        .map(c -> c.getCanal())
                        .orElse("ID " + canalId);
                throw new IllegalArgumentException(
                        String.format("El canal '%s' no tiene precios configurados para %d cuotas", nombreCanal, cuotas));
            }
        }
    }

}