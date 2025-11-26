package ar.com.leo.super_master_backend.dominio.clasif_gral.service;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.clasif_gral.mapper.ClasifGralMapper;
import ar.com.leo.super_master_backend.dominio.clasif_gral.repository.ClasifGralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClasifGralServiceImpl implements ClasifGralService {

    private final ClasifGralRepository repo;
    private final ClasifGralMapper mapper;

    @Override
    public List<ClasifGralDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ClasifGralDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Clasificación General no encontrada"));
    }

    @Override
    public ClasifGralDTO crear(ClasifGralCreateDTO dto) {
        ClasifGral entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ClasifGralDTO actualizar(Integer id, ClasifGralUpdateDTO dto) {
        ClasifGral entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clasificación General no encontrada"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}