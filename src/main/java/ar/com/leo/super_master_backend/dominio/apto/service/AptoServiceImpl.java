package ar.com.leo.super_master_backend.dominio.apto.service;

import ar.com.leo.super_master_backend.dominio.apto.dto.AptoCreateDTO;
import ar.com.leo.super_master_backend.dominio.apto.dto.AptoDTO;
import ar.com.leo.super_master_backend.dominio.apto.dto.AptoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.apto.entity.Apto;
import ar.com.leo.super_master_backend.dominio.apto.mapper.AptoMapper;
import ar.com.leo.super_master_backend.dominio.apto.repository.AptoRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AptoServiceImpl implements AptoService {

    private final AptoRepository repo;
    private final AptoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public AptoDTO obtener(Integer id) {
        Apto entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Apto no encontrado con id: " + id));
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AptoDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public AptoDTO crear(AptoCreateDTO dto) {
        Apto entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public AptoDTO actualizar(Integer id, AptoUpdateDTO dto) {
        Apto entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Apto no encontrado con id: " + id));

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Apto no encontrado con id: " + id);
        }
        repo.deleteById(id);
    }

}