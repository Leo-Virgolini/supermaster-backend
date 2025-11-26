package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.mapper.ClasifGastroMapper;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.repository.ClasifGastroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClasifGastroServiceImpl implements ClasifGastroService {

    private final ClasifGastroRepository repo;
    private final ClasifGastroMapper mapper;

    @Override
    public List<ClasifGastroDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ClasifGastroDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Clasificación Gastro no encontrada"));
    }

    @Override
    public ClasifGastroDTO crear(ClasifGastroCreateDTO dto) {
        ClasifGastro entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ClasifGastroDTO actualizar(Integer id, ClasifGastroUpdateDTO dto) {
        ClasifGastro entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clasificación Gastro no encontrada"));

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }
}