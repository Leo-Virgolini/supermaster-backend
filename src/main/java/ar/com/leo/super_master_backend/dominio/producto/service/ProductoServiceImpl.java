package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.*;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecioInflado;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioInfladoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoMargenRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.PrecioSpecifications;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final ProductoMargenRepository productoMargenRepository;
    private final ProductoCanalPrecioInfladoRepository productoCanalPrecioInfladoRepository;
    private final RecalculoPrecioFacade recalculoFacade;
    private final CanalRepository canalRepository;
    private final CanalConceptoCuotaRepository canalConceptoCuotaRepository;
    private final ReglaDescuentoRepository reglaDescuentoRepository;

    private static final int PRECISION_RESULTADO = 2;

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
        Integer proveedorIdAnterior = entity.getProveedor() != null ? entity.getProveedor().getId() : null;

        productoMapper.updateEntityFromDTO(dto, entity);
        productoRepository.save(entity);

        // Recalcular precios si cambió costo, IVA, clasifGastro o proveedor
        boolean cambioCosto = dto.costo() != null && (costoAnterior == null || costoAnterior.compareTo(dto.costo()) != 0);
        boolean cambioIva = dto.iva() != null && (ivaAnterior == null || ivaAnterior.compareTo(dto.iva()) != 0);
        boolean cambioClasifGastro = dto.clasifGastroId() != null && !Objects.equals(clasifGastroIdAnterior, dto.clasifGastroId());
        boolean cambioProveedor = dto.proveedorId() != null && !Objects.equals(proveedorIdAnterior, dto.proveedorId());

        if (cambioCosto || cambioIva || cambioClasifGastro || cambioProveedor) {
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
                // 1.1) FILTROS DE TEXTO DEDICADOS
                // =======================
                ProductoSpecifications.sku(filter.sku()),
                ProductoSpecifications.codExt(filter.codExt()),
                ProductoSpecifications.descripcion(filter.descripcion()),
                ProductoSpecifications.tituloWeb(filter.tituloWeb()),

                // =======================
                // 2) BOOLEANOS / NÚMEROS
                // =======================
                ProductoSpecifications.esCombo(filter.esCombo()),
                ProductoSpecifications.uxb(filter.uxb()),
                ProductoSpecifications.esMaquina(filter.esMaquina()),
                ProductoSpecifications.tieneMla(filter.tieneMla()),
                ProductoSpecifications.activo(filter.activo()),
                ProductoSpecifications.tagReposicion(filter.tagReposicion()),

                // =======================
                // 2.1) FILTROS MLA
                // =======================
                ProductoSpecifications.mla(filter.mla()),
                ProductoSpecifications.mlau(filter.mlau()),
                ProductoSpecifications.precioEnvioMin(filter.precioEnvioMin()),
                ProductoSpecifications.precioEnvioMax(filter.precioEnvioMax()),
                ProductoSpecifications.comisionPorcentajeMin(filter.comisionPorcentajeMin()),
                ProductoSpecifications.comisionPorcentajeMax(filter.comisionPorcentajeMax()),
                ProductoSpecifications.tieneComision(filter.tieneComision()),
                ProductoSpecifications.tienePrecioEnvio(filter.tienePrecioEnvio()),

                // =======================
                // 3) MANY-TO-ONE (multi-valor)
                // =======================
                ProductoSpecifications.marcaIds(filter.marcaIds()),
                ProductoSpecifications.origenIds(filter.origenIds()),
                ProductoSpecifications.tipoIds(filter.tipoIds()),
                ProductoSpecifications.clasifGralIds(filter.clasifGralIds()),
                ProductoSpecifications.clasifGastroIds(filter.clasifGastroIds()),
                ProductoSpecifications.proveedorIds(filter.proveedorIds()),
                ProductoSpecifications.materialIds(filter.materialIds()),

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
                ProductoSpecifications.desdeFechaUltimoCosto(filter.desdeFechaUltimoCosto()),
                ProductoSpecifications.hastaFechaUltimoCosto(filter.hastaFechaUltimoCosto()),
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
    // Pagina sobre filas de precio (producto+canal+cuota),
    // no sobre productos. Cada fila = 1 producto con 1 precio.
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoConPreciosDTO> listarConPrecios(ProductoFilter filter, Pageable pageable) {

        // Validar que el canal tenga las cuotas especificadas
        validarCanalConCuotas(filter.canalId(), filter.cuotas());

        // Traducir sort de campos especiales a campos de ProductoCanalPrecio
        Pageable sortedPageable = traducirSort(pageable);

        // 1) Construir Specification sobre ProductoCanalPrecio
        Specification<ProductoCanalPrecio> spec = Specification.allOf(
                // Filtros de precio (canal/cuotas)
                PrecioSpecifications.canalId(filter.canalId()),
                PrecioSpecifications.cuotas(filter.cuotas()),
                // ID
                PrecioSpecifications.productoId(filter.productoId()),
                // Texto
                PrecioSpecifications.textoLike(filter.search()),
                // Filtros de texto dedicados
                PrecioSpecifications.sku(filter.sku()),
                PrecioSpecifications.codExt(filter.codExt()),
                PrecioSpecifications.descripcion(filter.descripcion()),
                PrecioSpecifications.tituloWeb(filter.tituloWeb()),
                // Booleanos/Numéricos
                PrecioSpecifications.esCombo(filter.esCombo()),
                PrecioSpecifications.uxb(filter.uxb()),
                PrecioSpecifications.esMaquina(filter.esMaquina()),
                PrecioSpecifications.tieneMla(filter.tieneMla()),
                PrecioSpecifications.activo(filter.activo()),
                PrecioSpecifications.tagReposicion(filter.tagReposicion()),
                // Filtros MLA
                PrecioSpecifications.mla(filter.mla()),
                PrecioSpecifications.mlau(filter.mlau()),
                PrecioSpecifications.precioEnvioMin(filter.precioEnvioMin()),
                PrecioSpecifications.precioEnvioMax(filter.precioEnvioMax()),
                PrecioSpecifications.comisionPorcentajeMin(filter.comisionPorcentajeMin()),
                PrecioSpecifications.comisionPorcentajeMax(filter.comisionPorcentajeMax()),
                PrecioSpecifications.tieneComision(filter.tieneComision()),
                PrecioSpecifications.tienePrecioEnvio(filter.tienePrecioEnvio()),
                // Many-to-One (multi-valor)
                PrecioSpecifications.marcaIds(filter.marcaIds()),
                PrecioSpecifications.origenIds(filter.origenIds()),
                PrecioSpecifications.tipoIds(filter.tipoIds()),
                PrecioSpecifications.clasifGralIds(filter.clasifGralIds()),
                PrecioSpecifications.clasifGastroIds(filter.clasifGastroIds()),
                PrecioSpecifications.proveedorIds(filter.proveedorIds()),
                PrecioSpecifications.materialIds(filter.materialIds()),
                // Rangos
                PrecioSpecifications.costoMin(filter.costoMin()),
                PrecioSpecifications.costoMax(filter.costoMax()),
                PrecioSpecifications.ivaMin(filter.ivaMin()),
                PrecioSpecifications.ivaMax(filter.ivaMax()),
                PrecioSpecifications.stockMin(filter.stockMin()),
                PrecioSpecifications.stockMax(filter.stockMax()),
                // Rango PVP (directo sobre precio)
                PrecioSpecifications.pvpMin(filter.pvpMin()),
                PrecioSpecifications.pvpMax(filter.pvpMax()),
                // Fechas
                PrecioSpecifications.desdeFechaUltimoCosto(filter.desdeFechaUltimoCosto()),
                PrecioSpecifications.hastaFechaUltimoCosto(filter.hastaFechaUltimoCosto()),
                PrecioSpecifications.desdeFechaCreacion(filter.desdeFechaCreacion()),
                PrecioSpecifications.hastaFechaCreacion(filter.hastaFechaCreacion()),
                PrecioSpecifications.desdeFechaModificacion(filter.desdeFechaModificacion()),
                PrecioSpecifications.hastaFechaModificacion(filter.hastaFechaModificacion()),
                // Many-to-Many
                PrecioSpecifications.aptoIds(filter.aptoIds()),
                PrecioSpecifications.canalIds(filter.canalIds()),
                PrecioSpecifications.catalogoIds(filter.catalogoIds()),
                PrecioSpecifications.clienteIds(filter.clienteIds()),
                PrecioSpecifications.mlaIds(filter.mlaIds())
        );

        // 2) Paginar sobre filas de precio (producto+canal+cuota)
        Page<ProductoCanalPrecio> preciosPage = productoCanalPrecioRepository.findAll(spec, sortedPageable);

        if (preciosPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 3) Extraer productos únicos de esta página de precios
        List<ProductoCanalPrecio> preciosEnPagina = preciosPage.getContent();
        List<Integer> productoIds = preciosEnPagina.stream()
                .map(pcp -> pcp.getProducto().getId())
                .distinct()
                .toList();

        // 4) Cargar productos con todas las relaciones ManyToOne (1 query en vez de N+1)
        Map<Integer, Producto> productosMap = productoRepository.findAllByIdWithRelaciones(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        // 4.1) Cargar márgenes
        Map<Integer, ProductoMargen> margenesPorProducto = productoMargenRepository.findByProductoIdIn(productoIds).stream()
                .collect(Collectors.toMap(pm -> pm.getProducto().getId(), pm -> pm));

        // 4.2) Cargar nombres de canales
        Set<Integer> canalIdsEnPagina = preciosEnPagina.stream()
                .map(p -> p.getCanal().getId())
                .collect(Collectors.toSet());
        Map<Integer, String> nombresPorCanal = canalRepository.findAllById(canalIdsEnPagina).stream()
                .collect(Collectors.toMap(Canal::getId, Canal::getCanal));

        // 4.3) Cargar descripciones de cuotas
        Map<String, String> descripcionesCuotas = canalConceptoCuotaRepository.findByCanalIdIn(canalIdsEnPagina).stream()
                .filter(c -> c.getDescripcion() != null)
                .collect(Collectors.toMap(
                        c -> c.getCanal().getId() + "_" + c.getCuotas(),
                        CanalConceptoCuota::getDescripcion,
                        (a, b) -> a
                ));

        // 4.4) Cargar reglas de descuento
        Map<Integer, List<ReglaDescuento>> reglasPorCanal = new HashMap<>();
        for (Integer canalId : canalIdsEnPagina) {
            List<ReglaDescuento> reglas = reglaDescuentoRepository
                    .findByCanalIdAndActivoTrueOrderByPrioridadAsc(canalId);
            if (!reglas.isEmpty()) {
                reglasPorCanal.put(canalId, reglas);
            }
        }

        // 4.5) Cargar reglas de precio inflado activas para los productos de esta página
        // Clave: "productoId_canalId" -> ProductoCanalPrecioInflado
        Map<String, ProductoCanalPrecioInflado> infladosPorProductoCanal = new HashMap<>();
        if (!productoIds.isEmpty()) {
            List<ProductoCanalPrecioInflado> inflados = productoCanalPrecioInfladoRepository
                    .findByProductoIdInAndActivaTrueWithFetch(productoIds);
            for (ProductoCanalPrecioInflado pcpi : inflados) {
                String key = pcpi.getProducto().getId() + "_" + pcpi.getCanal().getId();
                infladosPorProductoCanal.put(key, pcpi);
            }
        }

        // 5) Mapear cada fila de precio a un ProductoConPreciosDTO (1 producto, 1 canal, 1 cuota)
        List<ProductoConPreciosDTO> dtos = preciosEnPagina.stream()
                .map(pcp -> {
                    Producto producto = productosMap.get(pcp.getProducto().getId());
                    ProductoMargen productoMargen = margenesPorProducto.get(producto.getId());

                    // Calcular descuentos para este canal si hay reglas
                    Map<Integer, List<DescuentoAplicableDTO>> descuentosPorCanal = null;
                    if (!reglasPorCanal.isEmpty()) {
                        descuentosPorCanal = calcularDescuentosPorCanal(List.of(pcp), reglasPorCanal);
                    }

                    return productoMapper.toProductoConPreciosDTO(
                            producto, productoMargen, List.of(pcp),
                            descripcionesCuotas, descuentosPorCanal, nombresPorCanal, infladosPorProductoCanal);
                })
                .toList();

        // 6) Retornar Page con metadata de filas de precio
        return new PageImpl<>(dtos, pageable, preciosPage.getTotalElements());
    }

    /**
     * Traduce nombres de sort del frontend a campos de ProductoCanalPrecio.
     * Campos de producto se prefijan con "producto.", campos de precio se usan directo.
     */
    private Pageable traducirSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> translated = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String campo = order.getProperty().toLowerCase();
            String mappedField = switch (campo) {
                case "pvp" -> "pvp";
                case "pvpinflado" -> "pvpInflado";
                case "costoproducto" -> "costoProducto";
                case "costosventa" -> "costosVenta";
                case "ingresonetovendedor" -> "ingresoNetoVendedor";
                case "ganancia" -> "ganancia";
                case "margensobreingreso" -> "margenSobreIngresoNeto";
                case "margensobrepvp" -> "margenSobrePvp";
                case "markup" -> "markupPorcentaje";
                case "cuotas" -> "cuotas";
                case "canal" -> "canal.canal";
                case "canalid" -> "canal.id";
                // Campos de producto via join
                case "mla" -> "producto.mla.mla";
                case "mlau" -> "producto.mla.mlau";
                case "precioenvio" -> "producto.mla.precioEnvio";
                case "comisionporcentaje" -> "producto.mla.comisionPorcentaje";
                case "margenminorista" -> "producto.productoMargenes.margenMinorista";
                case "margenmayorista" -> "producto.productoMargenes.margenMayorista";
                case "margenfijominorista" -> "producto.productoMargenes.margenFijoMinorista";
                case "margenfijomayorista" -> "producto.productoMargenes.margenFijoMayorista";
                // Precio inflado (regla asignada)
                case "precioinflado", "precioinfladocodigo" -> "precioInfladoAsignacion.precioInflado.codigo";
                case "precioinfladotipo" -> "precioInfladoAsignacion.precioInflado.tipo";
                case "precioinfladovalor" -> "precioInfladoAsignacion.precioInflado.valor";
                default -> "producto." + order.getProperty();
            };
            translated.add(new Sort.Order(order.getDirection(), mappedField));
        }

        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(translated));
    }

    /**
     * Lista de campos que requieren ordenamiento especial (en memoria).
     */
    private static final Set<String> CAMPOS_SORT_ESPECIAL = Set.of(
            // Campos de precios calculados
            "pvp", "pvpinflado", "costoproducto", "costosventa", "ingresonetovendedor",
            "ganancia", "margensobreingreso", "margensobrepvp", "markup",
            // Campos de MLA
            "mla", "mlau", "comisionporcentaje", "precioenvio",
            // Campos de relaciones
            "esmaquina",
            // Campos de fechas
            "fechaultimocosto", "fechaultimocalculo"
    );

    /**
     * Aplica ordenamiento especial si el sort del Pageable contiene un campo especial.
     */
    private void aplicarOrdenamientoEspecial(
            List<ProductoConPreciosDTO> dtos,
            Sort sort,
            Integer canalId,
            Integer cuotas,
            Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto) {

        if (sort == null || sort.isUnsorted()) {
            return;
        }

        // Buscar el primer campo de ordenamiento especial
        for (Sort.Order order : sort) {
            String campo = order.getProperty().toLowerCase();
            if (CAMPOS_SORT_ESPECIAL.contains(campo)) {
                Comparator<ProductoConPreciosDTO> comparator = getComparator(campo, canalId, cuotas, preciosPorProducto);
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
     * Para campos de precio: usa los filtros canalId/cuotas, o MAX de todos si no se especifican.
     */
    private Comparator<ProductoConPreciosDTO> getComparator(
            String sortBy,
            Integer canalId,
            Integer cuotas,
            Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto) {

        return switch (sortBy.toLowerCase()) {
            // Campos de MLA
            case "mla" -> Comparator.comparing(
                    ProductoConPreciosDTO::mla,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "mlau" -> Comparator.comparing(
                    ProductoConPreciosDTO::mlau,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "comisionporcentaje" -> Comparator.comparing(
                    ProductoConPreciosDTO::comisionPorcentaje,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "precioenvio" -> Comparator.comparing(
                    ProductoConPreciosDTO::precioEnvio,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            // Campos de relaciones
            case "esmaquina" -> Comparator.comparing(
                    ProductoConPreciosDTO::esMaquina,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            // Campos de precios calculados
            case "pvp" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getPvp);
            case "pvpinflado" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getPvpInflado);
            case "costoproducto" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getCostoProducto);
            case "costosventa" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getCostosVenta);
            case "ingresonetovendedor" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getIngresoNetoVendedor);
            case "ganancia" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getGanancia);
            case "margensobreingreso" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getMargenSobreIngresoNeto);
            case "margensobrepvp" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getMargenSobrePvp);
            case "markup" -> crearComparadorPrecio(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getMarkupPorcentaje);
            case "fechaultimocosto" -> Comparator.comparing(
                    ProductoConPreciosDTO::fechaUltimoCosto,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "fechaultimocalculo" -> crearComparadorPrecioTemporal(canalId, cuotas, preciosPorProducto, ProductoCanalPrecio::getFechaUltimoCalculo);
            default -> null;
        };
    }

    /**
     * Crea un comparador para un campo de precio.
     * Si canalId/cuotas se especifican, filtra por ellos.
     * Si no, usa el valor MAX de todos los canales/cuotas.
     */
    private Comparator<ProductoConPreciosDTO> crearComparadorPrecio(
            Integer canalId,
            Integer cuotas,
            Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto,
            java.util.function.Function<ProductoCanalPrecio, BigDecimal> extractor) {

        return Comparator.comparing(
                (ProductoConPreciosDTO dto) -> {
                    List<ProductoCanalPrecio> precios = preciosPorProducto.get(dto.id());
                    if (precios == null || precios.isEmpty()) return null;

                    // Filtrar por canal si se especifica
                    if (canalId != null) {
                        precios = precios.stream()
                                .filter(p -> p.getCanal().getId().equals(canalId))
                                .toList();
                    }

                    // Filtrar por cuotas si se especifica
                    if (cuotas != null) {
                        precios = precios.stream()
                                .filter(p -> cuotas.equals(p.getCuotas()))
                                .toList();
                    }

                    // Obtener el MAX del campo especificado
                    return precios.stream()
                            .map(extractor)
                            .filter(v -> v != null)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                },
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }

    private Comparator<ProductoConPreciosDTO> crearComparadorPrecioTemporal(
            Integer canalId,
            Integer cuotas,
            Map<Integer, List<ProductoCanalPrecio>> preciosPorProducto,
            java.util.function.Function<ProductoCanalPrecio, LocalDateTime> extractor) {

        return Comparator.comparing(
                (ProductoConPreciosDTO dto) -> {
                    List<ProductoCanalPrecio> precios = preciosPorProducto.get(dto.id());
                    if (precios == null || precios.isEmpty()) return null;

                    if (canalId != null) {
                        precios = precios.stream()
                                .filter(p -> p.getCanal().getId().equals(canalId))
                                .toList();
                    }

                    if (cuotas != null) {
                        precios = precios.stream()
                                .filter(p -> cuotas.equals(p.getCuotas()))
                                .toList();
                    }

                    return precios.stream()
                            .map(extractor)
                            .filter(v -> v != null)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                },
                Comparator.nullsLast(Comparator.naturalOrder())
        );
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
                // Filtros de texto dedicados
                ProductoSpecifications.sku(filter.sku()),
                ProductoSpecifications.codExt(filter.codExt()),
                ProductoSpecifications.descripcion(filter.descripcion()),
                ProductoSpecifications.tituloWeb(filter.tituloWeb()),
                // Booleanos/Numéricos
                ProductoSpecifications.esCombo(filter.esCombo()),
                ProductoSpecifications.uxb(filter.uxb()),
                ProductoSpecifications.esMaquina(filter.esMaquina()),
                ProductoSpecifications.tieneMla(filter.tieneMla()),
                ProductoSpecifications.activo(filter.activo()),
                ProductoSpecifications.tagReposicion(filter.tagReposicion()),
                // Filtros MLA
                ProductoSpecifications.mla(filter.mla()),
                ProductoSpecifications.mlau(filter.mlau()),
                ProductoSpecifications.precioEnvioMin(filter.precioEnvioMin()),
                ProductoSpecifications.precioEnvioMax(filter.precioEnvioMax()),
                ProductoSpecifications.comisionPorcentajeMin(filter.comisionPorcentajeMin()),
                ProductoSpecifications.comisionPorcentajeMax(filter.comisionPorcentajeMax()),
                ProductoSpecifications.tieneComision(filter.tieneComision()),
                ProductoSpecifications.tienePrecioEnvio(filter.tienePrecioEnvio()),
                // Many-to-One (multi-valor)
                ProductoSpecifications.marcaIds(filter.marcaIds()),
                ProductoSpecifications.origenIds(filter.origenIds()),
                ProductoSpecifications.tipoIds(filter.tipoIds()),
                ProductoSpecifications.clasifGralIds(filter.clasifGralIds()),
                ProductoSpecifications.clasifGastroIds(filter.clasifGastroIds()),
                ProductoSpecifications.proveedorIds(filter.proveedorIds()),
                ProductoSpecifications.materialIds(filter.materialIds()),
                ProductoSpecifications.costoMin(filter.costoMin()),
                ProductoSpecifications.costoMax(filter.costoMax()),
                ProductoSpecifications.ivaMin(filter.ivaMin()),
                ProductoSpecifications.ivaMax(filter.ivaMax()),
                ProductoSpecifications.stockMin(filter.stockMin()),
                ProductoSpecifications.stockMax(filter.stockMax()),
                ProductoSpecifications.pvpEnRango(filter.pvpMin(), filter.pvpMax(), filter.pvpCanalId()),
                ProductoSpecifications.desdeFechaUltimoCosto(filter.desdeFechaUltimoCosto()),
                ProductoSpecifications.hastaFechaUltimoCosto(filter.hastaFechaUltimoCosto()),
                ProductoSpecifications.desdeFechaCreacion(filter.desdeFechaCreacion()),
                ProductoSpecifications.hastaFechaCreacion(filter.hastaFechaCreacion()),
                ProductoSpecifications.desdeFechaModificacion(filter.desdeFechaModificacion()),
                ProductoSpecifications.hastaFechaModificacion(filter.hastaFechaModificacion()),
                ProductoSpecifications.aptoIds(filter.aptoIds()),
                ProductoSpecifications.canalIds(filter.canalIds()),
                ProductoSpecifications.catalogoIds(filter.catalogoIds()),
                ProductoSpecifications.clienteIds(filter.clienteIds()),
                ProductoSpecifications.mlaIds(filter.mlaIds()),
                // Filtro SQL: solo productos que tengan precios para este canal/cuotas
                ProductoSpecifications.tienePreciosEnCanalCuotas(filter.canalId(), filter.cuotas())
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

        // Obtener IDs de canales y sus nombres
        Set<Integer> canalIds = todosPrecios.stream()
                .map(p -> p.getCanal().getId())
                .collect(Collectors.toSet());

        // Cargar nombres de canales explícitamente (evita problemas de lazy loading)
        Map<Integer, String> nombresPorCanal = canalRepository.findAllById(canalIds).stream()
                .collect(Collectors.toMap(Canal::getId, Canal::getCanal));

        // Obtener descripciones de cuotas por canal (para PrecioDTO.descripcion)
        Map<String, String> descripcionesCuotas = canalConceptoCuotaRepository.findByCanalIdIn(canalIds).stream()
                .filter(c -> c.getDescripcion() != null)
                .collect(Collectors.toMap(
                        c -> c.getCanal().getId() + "_" + c.getCuotas(),
                        CanalConceptoCuota::getDescripcion,
                        (a, b) -> a
                ));

        // Obtener reglas de descuento por canal (siempre se cargan si existen)
        Map<Integer, List<ReglaDescuento>> reglasPorCanal = new HashMap<>();
        for (Integer canalId : canalIds) {
            List<ReglaDescuento> reglas = reglaDescuentoRepository
                    .findByCanalIdAndActivoTrueOrderByPrioridadAsc(canalId);
            if (!reglas.isEmpty()) {
                reglasPorCanal.put(canalId, reglas);
            }
        }

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

                    // Calcular descuentos aplicables por canal (siempre si hay reglas)
                    Map<Integer, List<DescuentoAplicableDTO>> descuentosPorCanal = null;
                    if (!precios.isEmpty() && !reglasPorCanal.isEmpty()) {
                        descuentosPorCanal = calcularDescuentosPorCanal(precios, reglasPorCanal);
                    }

                    return productoMapper.toProductoConPreciosDTO(producto, productoMargen, precios, descripcionesCuotas, descuentosPorCanal, nombresPorCanal);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Aplicar ordenamiento especial si se solicita
        aplicarOrdenamientoEspecial(dtos, sort, filter.canalId(), filter.cuotas(), preciosPorProducto);

        return dtos;
    }

    // ============================
    // ACTUALIZAR COSTO + RECALCULAR PRECIOS
    // ============================
    @Override
    @Transactional
    public void actualizarCosto(Integer productoId, BigDecimal nuevoCosto) {

        // 1) Actualizar costo del producto
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        producto.setCosto(nuevoCosto);
        productoRepository.save(producto);

        // 2) Recalcular precios en todos los canales
        recalculoFacade.recalcularPorCambioProducto(productoId);
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

    // ============================
    // CÁLCULO DE DESCUENTOS APLICABLES
    // ============================

    /**
     * Calcula los descuentos aplicables por canal basado en las reglas de descuento.
     * Para cada canal con reglas, calcula cómo quedaría el PVP y la ganancia con cada nivel de descuento.
     *
     * @param precios Lista de precios del producto
     * @param reglasPorCanal Mapa de canalId -> reglas de descuento activas ordenadas por prioridad
     * @return Mapa de canalId -> lista de descuentos aplicables
     */
    private Map<Integer, List<DescuentoAplicableDTO>> calcularDescuentosPorCanal(
            List<ProductoCanalPrecio> precios,
            Map<Integer, List<ReglaDescuento>> reglasPorCanal) {

        if (reglasPorCanal.isEmpty()) {
            return null;
        }

        Map<Integer, List<DescuentoAplicableDTO>> resultado = new HashMap<>();

        // Agrupar precios por canal
        Map<Integer, List<ProductoCanalPrecio>> preciosPorCanal = precios.stream()
                .collect(Collectors.groupingBy(p -> p.getCanal().getId()));

        for (Map.Entry<Integer, List<ReglaDescuento>> entry : reglasPorCanal.entrySet()) {
            Integer canalId = entry.getKey();
            List<ReglaDescuento> reglas = entry.getValue();

            // Obtener el primer precio del canal para calcular descuentos
            // (usamos el precio base, típicamente contado cuotas=0)
            List<ProductoCanalPrecio> preciosCanal = preciosPorCanal.get(canalId);
            if (preciosCanal == null || preciosCanal.isEmpty()) {
                continue;
            }

            // Buscar precio de contado (cuotas=0) o el primero disponible
            ProductoCanalPrecio precioBase = preciosCanal.stream()
                    .filter(p -> p.getCuotas() != null && p.getCuotas() == 0)
                    .findFirst()
                    .orElse(preciosCanal.get(0));

            BigDecimal pvp = precioBase.getPvp();
            BigDecimal costoProducto = precioBase.getCostoProducto();

            if (pvp == null || pvp.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            List<DescuentoAplicableDTO> descuentos = new ArrayList<>();

            for (ReglaDescuento regla : reglas) {
                BigDecimal descuentoPct = regla.getDescuentoPorcentaje();
                BigDecimal montoMinimo = regla.getMontoMinimo();

                // Calcular PVP con descuento (descuento real = resta)
                // pvpConDescuento = pvp * (1 - descuento/100)
                BigDecimal factorDescuento = BigDecimal.ONE.subtract(
                        descuentoPct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                BigDecimal pvpConDescuento = pvp.multiply(factorDescuento)
                        .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

                // Calcular métricas con descuento
                BigDecimal gananciaOriginal = precioBase.getGanancia();
                BigDecimal costosVentaConDescuento = BigDecimal.ZERO;
                BigDecimal ingresoNetoConDescuento = BigDecimal.ZERO;
                BigDecimal gananciaConDescuento = BigDecimal.ZERO;
                BigDecimal margenSobreIngresoNetoConDescuento = BigDecimal.ZERO;
                BigDecimal margenSobrePvpConDescuento = BigDecimal.ZERO;
                BigDecimal markupConDescuento = BigDecimal.ZERO;

                if (gananciaOriginal != null && costoProducto != null) {
                    BigDecimal ingresoNetoOriginal = precioBase.getIngresoNetoVendedor();
                    if (ingresoNetoOriginal != null && ingresoNetoOriginal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal proporcionIngreso = ingresoNetoOriginal.divide(pvp, 6, RoundingMode.HALF_UP);
                        ingresoNetoConDescuento = pvpConDescuento.multiply(proporcionIngreso)
                                .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);
                        costosVentaConDescuento = pvpConDescuento.subtract(ingresoNetoConDescuento)
                                .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);
                        gananciaConDescuento = ingresoNetoConDescuento.subtract(costoProducto)
                                .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

                        if (ingresoNetoConDescuento.compareTo(BigDecimal.ZERO) > 0) {
                            margenSobreIngresoNetoConDescuento = gananciaConDescuento.multiply(BigDecimal.valueOf(100))
                                    .divide(ingresoNetoConDescuento, PRECISION_RESULTADO, RoundingMode.HALF_UP);
                        }
                        if (pvpConDescuento.compareTo(BigDecimal.ZERO) > 0) {
                            margenSobrePvpConDescuento = gananciaConDescuento.multiply(BigDecimal.valueOf(100))
                                    .divide(pvpConDescuento, PRECISION_RESULTADO, RoundingMode.HALF_UP);
                        }
                        if (costoProducto.compareTo(BigDecimal.ZERO) > 0) {
                            markupConDescuento = gananciaConDescuento.multiply(BigDecimal.valueOf(100))
                                    .divide(costoProducto, PRECISION_RESULTADO, RoundingMode.HALF_UP);
                        }
                    }
                }

                descuentos.add(new DescuentoAplicableDTO(
                        montoMinimo,
                        descuentoPct,
                        pvpConDescuento,
                        costosVentaConDescuento,
                        ingresoNetoConDescuento,
                        gananciaConDescuento,
                        margenSobreIngresoNetoConDescuento,
                        margenSobrePvpConDescuento,
                        markupConDescuento
                ));
            }

            if (!descuentos.isEmpty()) {
                resultado.put(canalId, descuentos);
            }
        }

        return resultado.isEmpty() ? null : resultado;
    }

}