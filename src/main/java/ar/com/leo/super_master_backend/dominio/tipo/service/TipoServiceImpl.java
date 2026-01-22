package ar.com.leo.super_master_backend.dominio.tipo.service;

import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoCreateDTO;
import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoDTO;
import ar.com.leo.super_master_backend.dominio.tipo.dto.TipoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import ar.com.leo.super_master_backend.dominio.tipo.mapper.TipoMapper;
import ar.com.leo.super_master_backend.dominio.tipo.repository.TipoRepository;
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
public class TipoServiceImpl implements TipoService {

    private final TipoRepository repo;
    private final TipoMapper mapper;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TipoDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByNombreContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Tipo no encontrado"));
    }

    @Override
    @Transactional
    public TipoDTO crear(TipoCreateDTO dto) {
        Tipo entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public TipoDTO actualizar(Integer id, TipoUpdateDTO dto) {
        Tipo entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Tipo no encontrado");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer tipoId) {
        if (!repo.existsById(tipoId)) {
            throw new NotFoundException("Tipo no encontrado");
        }
        return productoRepository.findByTipoId(tipoId)
                .stream()
                .map(productoMapper::toResumenDTO)
                .toList();
    }

}