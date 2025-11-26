package ar.com.leo.super_master_backend.dominio.origen.service;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenCreateDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenUpdateDTO;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.origen.mapper.OrigenMapper;
import ar.com.leo.super_master_backend.dominio.origen.repository.OrigenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrigenServiceImpl implements OrigenService {

    private final OrigenRepository repo;
    private final OrigenMapper mapper;

    @Override
    public List<OrigenDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public OrigenDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Origen no encontrado"));
    }

    @Override
    public OrigenDTO crear(OrigenCreateDTO dto) {
        Origen entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public OrigenDTO actualizar(Integer id, OrigenUpdateDTO dto) {
        Origen entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Origen no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}