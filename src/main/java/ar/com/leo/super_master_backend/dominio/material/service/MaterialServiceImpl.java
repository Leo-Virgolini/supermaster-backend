package ar.com.leo.super_master_backend.dominio.material.service;

import ar.com.leo.super_master_backend.dominio.material.dto.MaterialCreateDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialDTO;
import ar.com.leo.super_master_backend.dominio.material.dto.MaterialUpdateDTO;
import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import ar.com.leo.super_master_backend.dominio.material.mapper.MaterialMapper;
import ar.com.leo.super_master_backend.dominio.material.repository.MaterialRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository repo;
    private final MaterialMapper mapper;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByMaterialContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Material no encontrado"));
    }

    @Override
    @Transactional
    public MaterialDTO crear(MaterialCreateDTO dto) {
        Material entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public MaterialDTO actualizar(Integer id, MaterialUpdateDTO dto) {
        Material entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Material no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Material no encontrado");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer materialId) {
        if (!repo.existsById(materialId)) {
            throw new NotFoundException("Material no encontrado");
        }
        return productoRepository.findByMaterialId(materialId)
                .stream()
                .map(productoMapper::toResumenDTO)
                .toList();
    }

}