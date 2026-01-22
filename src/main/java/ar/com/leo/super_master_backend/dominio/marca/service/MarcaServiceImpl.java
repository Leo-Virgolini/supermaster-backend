package ar.com.leo.super_master_backend.dominio.marca.service;

import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaCreateDTO;
import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaDTO;
import ar.com.leo.super_master_backend.dominio.marca.dto.MarcaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import ar.com.leo.super_master_backend.dominio.marca.mapper.MarcaMapper;
import ar.com.leo.super_master_backend.dominio.marca.repository.MarcaRepository;
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
public class MarcaServiceImpl implements MarcaService {

    private final MarcaRepository repo;
    private final MarcaMapper mapper;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MarcaDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByNombreContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public MarcaDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Marca no encontrada"));
    }

    @Override
    @Transactional
    public MarcaDTO crear(MarcaCreateDTO dto) {
        Marca entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public MarcaDTO actualizar(Integer id, MarcaUpdateDTO dto) {
        Marca entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Marca no encontrada"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Marca no encontrada");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer marcaId) {
        if (!repo.existsById(marcaId)) {
            throw new NotFoundException("Marca no encontrada");
        }
        return productoRepository.findByMarcaId(marcaId)
                .stream()
                .map(productoMapper::toResumenDTO)
                .toList();
    }

}