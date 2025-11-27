package ar.com.leo.super_master_backend.dominio.proveedor.service;

import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorCreateDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorUpdateDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.proveedor.mapper.ProveedorMapper;
import ar.com.leo.super_master_backend.dominio.proveedor.repository.ProveedorRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository repo;
    private final ProveedorMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorDTO> listar(Pageable pageable) {
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Proveedor no encontrado"));
    }

    @Override
    @Transactional
    public ProveedorDTO crear(ProveedorCreateDTO dto) {
        Proveedor entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ProveedorDTO actualizar(Integer id, ProveedorUpdateDTO dto) {
        Proveedor entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Proveedor no encontrado");
        }
        repo.deleteById(id);
    }

}