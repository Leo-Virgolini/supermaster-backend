package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
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
    public List<ProductoDTO> listar() {
        return productoRepository.findAll()
                .stream()
                .map(productoMapper::toDTO)
                .toList();
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