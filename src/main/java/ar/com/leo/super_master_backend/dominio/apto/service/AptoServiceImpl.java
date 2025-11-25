package ar.com.leo.super_master_backend.dominio.apto.service;

import ar.com.leo.super_master_backend.dominio.apto.dto.AptoDTO;
import ar.com.leo.super_master_backend.dominio.apto.entity.Apto;
import ar.com.leo.super_master_backend.dominio.apto.mapper.AptoMapper;
import ar.com.leo.super_master_backend.dominio.apto.repository.AptoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AptoServiceImpl implements AptoService {

    private final AptoRepository repo;
    private final AptoMapper mapper;

    @Override
    public AptoDTO obtener(Integer id) {
        Apto entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Apto no encontrado"));
        return mapper.toDTO(entity);
    }

    @Override
    public List<AptoDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public AptoDTO crear(AptoDTO dto) {
        Apto entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public AptoDTO actualizar(Integer id, AptoDTO dto) {
        Apto entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Apto no encontrado"));

        entity.setApto(dto.apto());

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}