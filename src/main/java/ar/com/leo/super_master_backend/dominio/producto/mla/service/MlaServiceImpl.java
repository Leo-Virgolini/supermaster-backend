package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.producto.mla.mapper.MlaMapper;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MlaServiceImpl implements MlaService {

    private final MlaRepository repo;
    private final MlaMapper mapper;
    private final RecalculoPrecioFacade recalculoFacade;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MlaDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByMlaContainingIgnoreCaseOrMlauContainingIgnoreCase(search, search, pageable)
                    .map(mapper::toDTO);
        }
        return repo.findAll(pageable).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public MlaDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("MLA no encontrado"));
    }

    @Override
    @Transactional
    public MlaDTO crear(MlaDTO dto) {
        // Validar que no exista otro MLA con el mismo código
        if (repo.findByMla(dto.mla()).isPresent()) {
            throw new ConflictException("Ya existe un MLA con el código: " + dto.mla());
        }

        Mla entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public MlaDTO actualizar(Integer id, MlaDTO dto) {
        Mla entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("MLA no encontrado"));

        // Validar unicidad si cambió el código
        if (!entity.getMla().equals(dto.mla()) && repo.findByMla(dto.mla()).isPresent()) {
            throw new ConflictException("Ya existe un MLA con el código: " + dto.mla());
        }

        // Guardar valor anterior para detectar cambio
        BigDecimal precioEnvioAnterior = entity.getPrecioEnvio();

        entity.setMla(dto.mla());
        entity.setMlau(dto.mlau());
        entity.setPrecioEnvio(dto.precioEnvio());

        repo.save(entity);

        // Recalcular si cambió el precioEnvio
        if (!Objects.equals(precioEnvioAnterior, dto.precioEnvio())) {
            recalculoFacade.recalcularPorCambioMla(id);
        }

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("MLA no encontrado");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer mlaId) {
        if (!repo.existsById(mlaId)) {
            throw new NotFoundException("MLA no encontrado");
        }
        return productoRepository.findByMlaId(mlaId)
                .stream()
                .map(productoMapper::toResumenDTO)
                .toList();
    }
}
