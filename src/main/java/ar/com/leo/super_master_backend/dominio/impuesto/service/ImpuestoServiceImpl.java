package ar.com.leo.super_master_backend.dominio.impuesto.service;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoCreateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.impuesto.entity.Impuesto;
import ar.com.leo.super_master_backend.dominio.impuesto.mapper.ImpuestoMapper;
import ar.com.leo.super_master_backend.dominio.impuesto.repository.ImpuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImpuestoServiceImpl implements ImpuestoService {

    private final ImpuestoRepository repo;
    private final ImpuestoMapper mapper;

    @Override
    public List<ImpuestoDTO> listar() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ImpuestoDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Impuesto no encontrado"));
    }

    @Override
    public ImpuestoDTO crear(ImpuestoCreateDTO dto) {
        Impuesto entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    public ImpuestoDTO actualizar(Integer id, ImpuestoUpdateDTO dto) {
        Impuesto entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Impuesto no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminar(Integer id) {
        repo.deleteById(id);
    }

}