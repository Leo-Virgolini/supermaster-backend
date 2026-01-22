package ar.com.leo.super_master_backend.dominio.catalogo.service;

import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoCreateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.dto.CatalogoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import ar.com.leo.super_master_backend.dominio.catalogo.mapper.CatalogoMapper;
import ar.com.leo.super_master_backend.dominio.catalogo.repository.CatalogoRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCatalogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final CatalogoRepository repo;
    private final CatalogoMapper mapper;
    private final ProductoCatalogoRepository productoCatalogoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByCatalogoContainingIgnoreCase(search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Catálogo no encontrado"));
    }

    @Override
    @Transactional
    public CatalogoDTO crear(CatalogoCreateDTO dto) {
        Catalogo entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public CatalogoDTO actualizar(Integer id, CatalogoUpdateDTO dto) {

        Catalogo entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Catálogo no encontrado"));

        mapper.updateEntityFromDTO(dto, entity);

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Catálogo no encontrado");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer catalogoId) {
        if (!repo.existsById(catalogoId)) {
            throw new NotFoundException("Catálogo no encontrado");
        }
        return productoCatalogoRepository.findByCatalogoId(catalogoId)
                .stream()
                .map(pc -> productoMapper.toResumenDTO(pc.getProducto()))
                .toList();
    }

}