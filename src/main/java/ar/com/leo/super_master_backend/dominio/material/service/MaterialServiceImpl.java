package ar.com.leo.super_master_backend.dominio.material.service;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import ar.com.leo.super_master_backend.dominio.material.mapper.MaterialMapper;
import ar.com.leo.super_master_backend.dominio.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository repo;
    private final MaterialMapper mapper;

    @Override
    public MaterialDTO obtener(Integer id) {
        Material mat = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Material no encontrado"));
        return mapper.toDTO(mat);
    }

    @Override
    public List<MaterialDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public MaterialDTO crear(MaterialDTO dto) {
        Material m = mapper.toEntity(dto);
        repo.save(m);
        return mapper.toDTO(m);
    }

    @Override
    public MaterialDTO actualizar(Integer id, MaterialDTO dto) {
        Material entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Material no encontrado"));

        entity.setMaterial(dto.material());

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}