package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.mapper.ClasifGastroMapper;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.repository.ClasifGastroRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClasifGastroServiceImpl implements ClasifGastroService {

    private final ClasifGastroRepository repo;
    private final ClasifGastroMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ClasifGastroDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClasifGastroDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Clasificación Gastro no encontrada"));
    }

    @Override
    @Transactional
    public ClasifGastroDTO crear(ClasifGastroCreateDTO dto) {
        ClasifGastro entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ClasifGastroDTO actualizar(Integer id, ClasifGastroUpdateDTO dto) {
        ClasifGastro entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Clasificación Gastro no encontrada"));

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Clasificación Gastro no encontrada");
        }
        repo.deleteById(id);
    }
}