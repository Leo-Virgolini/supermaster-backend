package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioInfladoCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioInfladoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioInfladoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecioInflado;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCanalPrecioInfladoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioInfladoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.precio_inflado.entity.PrecioInflado;
import ar.com.leo.super_master_backend.dominio.precio_inflado.repository.PrecioInfladoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoCanalPrecioInfladoServiceImpl implements ProductoCanalPrecioInfladoService {

    private final ProductoCanalPrecioInfladoRepository repository;
    private final ProductoCanalPrecioInfladoMapper mapper;
    private final ProductoRepository productoRepository;
    private final CanalRepository canalRepository;
    private final PrecioInfladoRepository precioInfladoRepository;
    private final RecalculoPrecioFacade recalculoFacade;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalPrecioInfladoDTO> listarPorProducto(Integer productoId) {
        return repository.findByProductoId(productoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalPrecioInfladoDTO> listarPorCanal(Integer canalId) {
        return repository.findByCanalId(canalId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCanalPrecioInfladoDTO> listarActivas() {
        return repository.findByActivaTrue()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoCanalPrecioInfladoDTO obtenerPorId(Integer id) {
        ProductoCanalPrecioInflado precioInflado = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + id));
        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoCanalPrecioInfladoDTO obtenerPorProductoYCanal(Integer productoId, Integer canalId) {
        ProductoCanalPrecioInflado precioInflado = repository.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException(
                        "Precio inflado no encontrado para producto ID: " + productoId + " y canal ID: " + canalId));
        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public ProductoCanalPrecioInfladoDTO crear(ProductoCanalPrecioInfladoCreateDTO dto) {
        productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + dto.productoId()));
        canalRepository.findById(dto.canalId())
                .orElseThrow(() -> new NotFoundException("Canal no encontrado con ID: " + dto.canalId()));
        PrecioInflado precioInfladoMaestro =
                precioInfladoRepository.findById(dto.precioInfladoId())
                        .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + dto.precioInfladoId()));

        if (repository.findByProductoIdAndCanalId(dto.productoId(), dto.canalId()).isPresent()) {
            throw new ConflictException(
                    "Ya existe un precio inflado para el producto ID: " + dto.productoId() + " y canal ID: "
                            + dto.canalId());
        }

        ProductoCanalPrecioInflado precioInflado = mapper.toEntity(dto);
        // Reemplazar la referencia del mapper (solo tiene ID) con la entidad completa
        // para evitar NPE cuando el recálculo accede a tipo/valor en la misma transacción
        precioInflado.setPrecioInflado(precioInfladoMaestro);

        if (precioInflado.getActiva() == null) {
            precioInflado.setActiva(true);
        }

        precioInflado = repository.save(precioInflado);

        recalculoFacade.recalcularPorCambioPrecioInflado(dto.productoId(), dto.canalId());

        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public ProductoCanalPrecioInfladoDTO actualizar(Integer productoId, Integer canalId,
            ProductoCanalPrecioInfladoUpdateDTO dto) {
        ProductoCanalPrecioInflado precioInflado = repository.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException(
                        "Precio inflado no encontrado para producto ID: " + productoId + " y canal ID: " + canalId));

        if (dto.precioInfladoId() != null) {
            PrecioInflado precioInfladoMaestro =
                    precioInfladoRepository.findById(dto.precioInfladoId())
                            .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + dto.precioInfladoId()));
            mapper.updateEntityFromDTO(dto, precioInflado);
            // Reemplazar la referencia del mapper (solo tiene ID) con la entidad completa
            precioInflado.setPrecioInflado(precioInfladoMaestro);
        } else {
            mapper.updateEntityFromDTO(dto, precioInflado);
        }

        precioInflado = repository.save(precioInflado);

        recalculoFacade.recalcularPorCambioPrecioInflado(productoId, canalId);

        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId, Integer canalId) {
        ProductoCanalPrecioInflado precioInflado = repository.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException(
                        "Precio inflado no encontrado para producto ID: " + productoId + " y canal ID: " + canalId));
        repository.delete(precioInflado);

        recalculoFacade.recalcularPorCambioPrecioInflado(productoId, canalId);
    }
}
