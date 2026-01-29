package ar.com.leo.super_master_backend.dominio.config_automatizacion.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.config_automatizacion.dto.ConfigAutomatizacionCreateDTO;
import ar.com.leo.super_master_backend.dominio.config_automatizacion.dto.ConfigAutomatizacionDTO;
import ar.com.leo.super_master_backend.dominio.config_automatizacion.dto.ConfigAutomatizacionUpdateDTO;
import ar.com.leo.super_master_backend.dominio.config_automatizacion.entity.ConfigAutomatizacion;
import ar.com.leo.super_master_backend.dominio.config_automatizacion.mapper.ConfigAutomatizacionMapper;
import ar.com.leo.super_master_backend.dominio.config_automatizacion.repository.ConfigAutomatizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfigAutomatizacionServiceImpl implements ConfigAutomatizacionService {

    private final ConfigAutomatizacionRepository repo;
    private final ConfigAutomatizacionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ConfigAutomatizacionDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByClaveContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
                    search, search, pageable).map(mapper::toDTO);
        }
        return repo.findAll(pageable).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigAutomatizacionDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Configuración no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigAutomatizacionDTO> obtenerPorClave(String clave) {
        return repo.findByClaveIgnoreCase(clave).map(mapper::toDTO);
    }

    @Override
    @Transactional
    public ConfigAutomatizacionDTO crear(ConfigAutomatizacionCreateDTO dto) {
        if (repo.existsByClaveIgnoreCase(dto.clave())) {
            throw new ConflictException("Ya existe una configuración con la clave: " + dto.clave());
        }
        ConfigAutomatizacion entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ConfigAutomatizacionDTO actualizar(Integer id, ConfigAutomatizacionUpdateDTO dto) {
        ConfigAutomatizacion entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Configuración no encontrada"));

        // Verificar clave duplicada si se está cambiando
        if (dto.clave() != null && !dto.clave().equalsIgnoreCase(entity.getClave())) {
            if (repo.existsByClaveIgnoreCase(dto.clave())) {
                throw new ConflictException("Ya existe una configuración con la clave: " + dto.clave());
            }
        }

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Configuración no encontrada");
        }
        repo.deleteById(id);
    }
}
