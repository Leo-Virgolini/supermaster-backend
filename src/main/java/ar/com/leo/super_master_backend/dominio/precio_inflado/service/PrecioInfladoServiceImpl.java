package ar.com.leo.super_master_backend.dominio.precio_inflado.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.precio_inflado.dto.PrecioInfladoCreateDTO;
import ar.com.leo.super_master_backend.dominio.precio_inflado.dto.PrecioInfladoDTO;
import ar.com.leo.super_master_backend.dominio.precio_inflado.dto.PrecioInfladoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.precio_inflado.entity.PrecioInflado;
import ar.com.leo.super_master_backend.dominio.precio_inflado.mapper.PrecioInfladoMapper;
import ar.com.leo.super_master_backend.dominio.precio_inflado.repository.PrecioInfladoRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecioInflado;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioInfladoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrecioInfladoServiceImpl implements PrecioInfladoService {

    private final PrecioInfladoRepository repository;
    private final PrecioInfladoMapper mapper;
    private final ProductoCanalPrecioInfladoRepository asignacionRepository;
    private final RecalculoPrecioFacade recalculoPrecioFacade;

    @Override
    @Transactional(readOnly = true)
    public Page<PrecioInfladoDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repository.findByCodigoContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PrecioInfladoDTO obtenerPorId(Integer id) {
        PrecioInflado precioInflado = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + id));
        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional(readOnly = true)
    public PrecioInfladoDTO obtenerPorCodigo(String codigo) {
        PrecioInflado precioInflado = repository.findByCodigo(codigo)
                .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con código: " + codigo));
        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public PrecioInfladoDTO crear(PrecioInfladoCreateDTO dto) {
        if (repository.existsByCodigo(dto.codigo())) {
            throw new ConflictException("Ya existe un precio inflado con el código: " + dto.codigo());
        }

        PrecioInflado precioInflado = mapper.toEntity(dto);
        precioInflado = repository.save(precioInflado);
        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public PrecioInfladoDTO actualizar(Integer id, PrecioInfladoUpdateDTO dto) {
        PrecioInflado precioInflado = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + id));

        if (dto.codigo() != null && !dto.codigo().equals(precioInflado.getCodigo())) {
            if (repository.existsByCodigo(dto.codigo())) {
                throw new ConflictException("Ya existe un precio inflado con el código: " + dto.codigo());
            }
        }

        boolean cambioValor = (dto.tipo() != null && dto.tipo() != precioInflado.getTipo())
                || (dto.valor() != null && dto.valor().compareTo(precioInflado.getValor()) != 0);

        mapper.updateEntityFromDTO(dto, precioInflado);
        precioInflado = repository.save(precioInflado);

        if (cambioValor) {
            recalcularAsignaciones(id);
        }

        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        PrecioInflado precioInflado = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + id));

        List<ProductoCanalPrecioInflado> asignaciones = asignacionRepository.findByPrecioInfladoId(id);
        repository.delete(precioInflado);

        for (ProductoCanalPrecioInflado asignacion : asignaciones) {
            recalculoPrecioFacade.recalcularPorCambioPrecioInflado(
                    asignacion.getProducto().getId(), asignacion.getCanal().getId());
        }
    }

    private void recalcularAsignaciones(Integer precioInfladoId) {
        asignacionRepository.findByPrecioInfladoId(precioInfladoId)
                .forEach(asignacion -> recalculoPrecioFacade.recalcularPorCambioPrecioInflado(
                        asignacion.getProducto().getId(), asignacion.getCanal().getId()));
    }
}
