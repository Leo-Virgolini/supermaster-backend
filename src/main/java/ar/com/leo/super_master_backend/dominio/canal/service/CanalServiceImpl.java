package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalServiceImpl implements CanalService {

    private final CanalRepository repo;
    private final CanalMapper mapper;

    @Override
    public CanalDTO obtener(Integer id) {
        return mapper.toDTO(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Canal no encontrado")));
    }

    @Override
    public List<CanalDTO> listar() {
        return repo.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    public CanalDTO crear(CanalCreateDTO dto) {
        return mapper.toDTO(repo.save(mapper.toEntity(dto)));
    }

    @Override
    public CanalDTO actualizar(Integer id, CanalUpdateDTO dto) {
        Canal c = repo.findById(id).orElseThrow();
        mapper.update(c, dto);
        return mapper.toDTO(repo.save(c));
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}
