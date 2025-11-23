package ar.com.leo.super_master_backend.dominio.marca.service;

import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaDTO;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import ar.com.leo.super_master_backend.dominio.marca.mapper.MarcaMapper;
import ar.com.leo.super_master_backend.dominio.marca.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarcaServiceImpl implements MarcaService {

    private final MarcaRepository repo;
    private final MarcaMapper mapper;

    @Override
    public MarcaDTO obtener(Integer id) {
        Marca m = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
        return mapper.toDTO(m);
    }

    @Override
    public List<MarcaDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public MarcaDTO crear(MarcaDTO dto) {
        Marca m = mapper.toEntity(dto);
        repo.save(m);
        return mapper.toDTO(m);
    }

    @Override
    public MarcaDTO actualizar(Integer id, MarcaDTO dto) {
        Marca entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));

        // Actualizar campos
        entity.setNombre(dto.nombre());

        if (dto.padreId() != null) {
            entity.setPadre(new Marca(dto.padreId())); // relación con padre
        }

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}