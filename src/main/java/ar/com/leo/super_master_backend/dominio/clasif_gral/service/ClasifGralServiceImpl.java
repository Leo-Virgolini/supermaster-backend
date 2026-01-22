package ar.com.leo.super_master_backend.dominio.clasif_gral.service;

import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralCreateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.dto.ClasifGralUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.clasif_gral.mapper.ClasifGralMapper;
import ar.com.leo.super_master_backend.dominio.clasif_gral.repository.ClasifGralRepository;
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
public class ClasifGralServiceImpl implements ClasifGralService {

    private final ClasifGralRepository repo;
    private final ClasifGralMapper mapper;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ClasifGralDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByNombreContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ClasifGralDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Clasificación General no encontrada"));
    }

    @Override
    @Transactional
    public ClasifGralDTO crear(ClasifGralCreateDTO dto) {
        ClasifGral entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ClasifGralDTO actualizar(Integer id, ClasifGralUpdateDTO dto) {
        ClasifGral entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Clasificación General no encontrada"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Clasificación General no encontrada");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer clasifGralId) {
        if (!repo.existsById(clasifGralId)) {
            throw new NotFoundException("Clasificación General no encontrada");
        }
        return productoRepository.findByClasifGralId(clasifGralId)
                .stream()
                .map(productoMapper::toResumenDTO)
                .toList();
    }

}