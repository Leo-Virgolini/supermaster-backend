package ar.com.leo.super_master_backend.dominio.promocion.service;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import ar.com.leo.super_master_backend.dominio.promocion.mapper.PromocionMapper;
import ar.com.leo.super_master_backend.dominio.promocion.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromocionServiceImpl implements PromocionService {

    private final PromocionRepository repository;
    private final PromocionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PromocionDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repository.findByCodigoContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PromocionDTO obtenerPorId(Integer id) {
        Promocion promocion = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada con ID: " + id));
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromocionDTO obtenerPorCodigo(String codigo) {
        Promocion promocion = repository.findByCodigo(codigo)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada con código: " + codigo));
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional
    public PromocionDTO crear(PromocionCreateDTO dto) {
        // Verificar que no exista ya una promoción con el mismo código
        if (repository.existsByCodigo(dto.codigo())) {
            throw new IllegalArgumentException("Ya existe una promoción con el código: " + dto.codigo());
        }

        Promocion promocion = mapper.toEntity(dto);
        promocion = repository.save(promocion);
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional
    public PromocionDTO actualizar(Integer id, PromocionUpdateDTO dto) {
        Promocion promocion = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada con ID: " + id));

        // Si se está cambiando el código, verificar que no exista otro con el mismo código
        if (dto.codigo() != null && !dto.codigo().equals(promocion.getCodigo())) {
            if (repository.existsByCodigo(dto.codigo())) {
                throw new IllegalArgumentException("Ya existe una promoción con el código: " + dto.codigo());
            }
        }

        mapper.updateEntityFromDTO(dto, promocion);
        promocion = repository.save(promocion);
        return mapper.toDTO(promocion);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        Promocion promocion = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promoción no encontrada con ID: " + id));
        repository.delete(promocion);
    }
}
