package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPromocion;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCanalPromocionMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPromocionRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.promocion.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoCanalPromocionServiceImpl implements ProductoCanalPromocionService {

    private final ProductoCanalPromocionRepository repository;
    private final ProductoCanalPromocionMapper mapper;
    private final ProductoRepository productoRepository;
    private final CanalRepository canalRepository;
    private final PromocionRepository promocionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalPromocionDTO> listarPorProducto(Integer productoId) {
        return repository.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalPromocionDTO> listarPorCanal(Integer canalId) {
        return repository.findByCanalId(canalId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalPromocionDTO> listarActivas() {
        return repository.findByActivaTrue()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoCanalPromocionDTO obtenerPorId(Integer id) {
        ProductoCanalPromocion promocion = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada con ID: " + id));
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoCanalPromocionDTO obtenerPorProductoYCanal(Integer productoId, Integer canalId) {
        ProductoCanalPromocion promocion = repository.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException(
                        "Promoción no encontrada para producto ID: " + productoId + " y canal ID: " + canalId));
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional
    public ProductoCanalPromocionDTO crear(ProductoCanalPromocionCreateDTO dto) {
        // Validar que existan producto, canal y promoción
        productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + dto.productoId()));
        canalRepository.findById(dto.canalId())
                .orElseThrow(() -> new NotFoundException("Canal no encontrado con ID: " + dto.canalId()));
        promocionRepository.findById(dto.promocionId())
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada con ID: " + dto.promocionId()));

        // Verificar que no exista ya una promoción para este producto-canal
        if (repository.findByProductoIdAndCanalId(dto.productoId(), dto.canalId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe una promoción para el producto ID: " + dto.productoId() + " y canal ID: "
                            + dto.canalId());
        }

        ProductoCanalPromocion promocion = mapper.toEntity(dto);

        // Valores por defecto si no se especifican
        if (promocion.getActiva() == null) {
            promocion.setActiva(true);
        }

        promocion = repository.save(promocion);
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional
    public ProductoCanalPromocionDTO actualizar(Integer productoId, Integer canalId,
            ProductoCanalPromocionUpdateDTO dto) {
        ProductoCanalPromocion promocion = repository.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException(
                        "Promoción no encontrada para producto ID: " + productoId + " y canal ID: " + canalId));

        // Validar que la promoción exista si se está actualizando
        if (dto.promocionId() != null) {
            promocionRepository.findById(dto.promocionId())
                    .orElseThrow(() -> new NotFoundException("Promoción no encontrada con ID: " + dto.promocionId()));
        }

        mapper.updateEntityFromDTO(dto, promocion);
        promocion = repository.save(promocion);
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId, Integer canalId) {
        ProductoCanalPromocion promocion = repository.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException(
                        "Promoción no encontrada para producto ID: " + productoId + " y canal ID: " + canalId));
        repository.delete(promocion);
    }
}
