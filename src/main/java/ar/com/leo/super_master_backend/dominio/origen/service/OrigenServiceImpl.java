package ar.com.leo.super_master_backend.dominio.origen.service;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenCreateDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenUpdateDTO;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.origen.mapper.OrigenMapper;
import ar.com.leo.super_master_backend.dominio.origen.repository.OrigenRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrigenServiceImpl implements OrigenService {

    private final OrigenRepository repo;
    private final OrigenMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<OrigenDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrigenDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Origen no encontrado"));
    }

    @Override
    @Transactional
    public OrigenDTO crear(OrigenCreateDTO dto) {
        Origen entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public OrigenDTO actualizar(Integer id, OrigenUpdateDTO dto) {
        Origen entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Origen no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Origen no encontrado");
        }
        repo.deleteById(id);
    }

}