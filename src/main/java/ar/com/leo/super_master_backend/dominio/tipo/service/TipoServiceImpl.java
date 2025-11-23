package ar.com.leo.super_master_backend.dominio.tipo.service;


import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoDTO;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import ar.com.leo.super_master_backend.dominio.tipo.mapper.TipoMapper;
import ar.com.leo.super_master_backend.dominio.tipo.repository.TipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoServiceImpl implements TipoService {

    private final TipoRepository repo;
    private final TipoMapper mapper;

    @Override
    public TipoDTO obtener(Integer id) {
        Tipo t = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo no encontrado"));
        return mapper.toDTO(t);
    }

    @Override
    public List<TipoDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public TipoDTO crear(TipoDTO dto) {
        Tipo entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public TipoDTO actualizar(Integer id, TipoDTO dto) {
        Tipo entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo no encontrado"));

        // Actualizar campos
        entity.setNombre(dto.nombre());

        if (dto.padreId() != null) {
            entity.setPadre(new Tipo(dto.padreId()));
        }

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }
}