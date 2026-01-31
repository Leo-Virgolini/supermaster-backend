package ar.com.leo.super_master_backend.dominio.precio_inflado.service;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.precio_inflado.dto.PrecioInfladoCreateDTO;
import ar.com.leo.super_master_backend.dominio.precio_inflado.dto.PrecioInfladoDTO;
import ar.com.leo.super_master_backend.dominio.precio_inflado.dto.PrecioInfladoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.precio_inflado.entity.PrecioInflado;
import ar.com.leo.super_master_backend.dominio.precio_inflado.mapper.PrecioInfladoMapper;
import ar.com.leo.super_master_backend.dominio.precio_inflado.repository.PrecioInfladoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrecioInfladoServiceImpl implements PrecioInfladoService {

    private final PrecioInfladoRepository repository;
    private final PrecioInfladoMapper mapper;

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
            throw new IllegalArgumentException("Ya existe un precio inflado con el código: " + dto.codigo());
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
                throw new IllegalArgumentException("Ya existe un precio inflado con el código: " + dto.codigo());
            }
        }

        mapper.updateEntityFromDTO(dto, precioInflado);
        precioInflado = repository.save(precioInflado);
        return mapper.toDTO(precioInflado);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        PrecioInflado precioInflado = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Precio inflado no encontrado con ID: " + id));
        repository.delete(precioInflado);
    }
}
