package ar.com.leo.super_master_backend.dominio.origen.service;

import ar.com.leo.super_master_backend.dominio.origen.dto.OrigenDTO;
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
    public OrigenDTO obtener(Integer id) {
        Origen entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Origen no encontrado"));
        return mapper.toDTO(entity);
    }

    @Override
    public List<OrigenDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public OrigenDTO crear(OrigenDTO dto) {
        Origen entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public OrigenDTO actualizar(Integer id, OrigenDTO dto) {
        Origen entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Origen no encontrado"));

        entity.setOrigen(dto.origen());

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}