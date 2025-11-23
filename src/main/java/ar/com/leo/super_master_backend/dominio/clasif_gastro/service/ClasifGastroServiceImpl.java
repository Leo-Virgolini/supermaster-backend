package ar.com.leo.super_master_backend.dominio.clasif_gastro.service;

import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroDTO;
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
    public ClasifGastroDTO obtener(Integer id) {
        ClasifGastro entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clasificación gastro no encontrada"));
        return mapper.toDTO(entity);
    }

    @Override
    public List<ClasifGastroDTO> listar() {
        return repo.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ClasifGastroDTO crear(String nombre, Integer padreId) {
        ClasifGastro entity = new ClasifGastro();
        entity.setNombre(nombre);

        if (padreId != null)
            entity.setPadre(new ClasifGastro(padreId));

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ClasifGastroDTO actualizar(Integer id, String nombre, Integer padreId) {
        ClasifGastro entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clasificación gastro no encontrada"));

        if (nombre != null)
            entity.setNombre(nombre);

        if (padreId != null)
            entity.setPadre(new ClasifGastro(padreId));

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }
}