package ar.com.leo.super_master_backend.dominio.clasif_gral.service;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
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
    public ClasifGralDTO obtener(Integer id) {
        ClasifGral entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clasificación general no encontrada"));
        return mapper.toDTO(entity);
    }

    @Override
    public List<ClasifGralDTO> listar() {
        return repo.findAll().stream()
                .map(entity -> mapper.toDTO(entity))
                .toList();
    }

    @Override
    public ClasifGralDTO crear(String nombre, Integer padreId) {
        ClasifGral entity = new ClasifGral();
        entity.setNombre(nombre);

        if (padreId != null)
            entity.setPadre(new ClasifGral(padreId));

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ClasifGralDTO actualizar(Integer id, String nombre, Integer padreId) {
        ClasifGral entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clasificación general no encontrada"));

        if (nombre != null)
            entity.setNombre(nombre);

        if (padreId != null)
            entity.setPadre(new ClasifGral(padreId));

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }
}