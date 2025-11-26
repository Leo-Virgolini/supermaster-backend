package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoFilter;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final ProductoCanalRepository productoCanalRepository;
    private final CalculoPrecioService calculoPrecioService;

    // ============================
    // LISTAR
    // ============================
    @Override
    public Page<ProductoDTO> listar(Pageable pageable) {
        return productoRepository.findAll(pageable)
                .map(productoMapper::toDTO);
    }

    // ============================
    // OBTENER
    // ============================
    @Override
    public ProductoDTO obtener(Integer id) {
        return productoRepository.findById(id)
                .map(productoMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    // ============================
    // CREAR
    // ============================
    @Override
    @Transactional
    public ProductoDTO crear(ProductoCreateDTO dto) {
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
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        productoMapper.updateEntityFromDTO(dto, entity);
        productoRepository.save(entity);

        return productoMapper.toDTO(entity);
    }

    // ============================
    // ELIMINAR
    // ============================
    @Override
    @Transactional
    public void eliminar(Integer id) {
        productoRepository.deleteById(id);
    }

    // ============================
    // OBTENER POR SKU
    // ============================
    @Override
    public ProductoDTO obtenerPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .map(productoMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    // ======================================================
    // BUSQUEDA / FILTRADO PROFESIONAL
    // ======================================================
    @Override
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
                // 4) RANGOS (costo / iva)
                // =======================
                ProductoSpecifications.costoMin(filter.costoMin()),
                ProductoSpecifications.costoMax(filter.costoMax()),
                ProductoSpecifications.ivaMin(filter.ivaMin()),
                ProductoSpecifications.ivaMax(filter.ivaMax()),

                // =======================
                // 5) FECHAS
                // =======================
                ProductoSpecifications.desdeFechaUltCosto(filter.desdeFechaUltCosto()),
                ProductoSpecifications.hastaFechaUltCosto(filter.hastaFechaUltCosto()),

                // ⭐ NUEVO: fechaCreacion
                ProductoSpecifications.desdeFechaCreacion(filter.desdeFechaCreacion()),
                ProductoSpecifications.hastaFechaCreacion(filter.hastaFechaCreacion()),

                // ⭐ NUEVO: fechaModificacion
                ProductoSpecifications.desdeFechaModificacion(filter.desdeFechaModificacion()),
                ProductoSpecifications.hastaFechaModificacion(filter.hastaFechaModificacion()),

                // =======================
                // 6) MANY-TO-MANY
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

    // ============================
    // 🔥 ACTUALIZAR COSTO + RECALCULAR PRECIOS
    // ============================
    @Override
    @Transactional
    public void actualizarCosto(Integer idProducto, BigDecimal nuevoCosto) {

        // 1) Actualizar costo del producto
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setCosto(nuevoCosto);
        productoRepository.save(producto);

        // 2) Obtener todos los canales donde participa el producto
        List<ProductoCanal> canales = productoCanalRepository.findByProductoId(idProducto);

        // 3) Recalcular automáticamente el precio para cada canal
        canales.forEach(pc -> {
            Integer idCanal = pc.getCanal().getId();
            calculoPrecioService.recalcularYGuardarPrecioCanal(idProducto, idCanal);
        });
    }

}