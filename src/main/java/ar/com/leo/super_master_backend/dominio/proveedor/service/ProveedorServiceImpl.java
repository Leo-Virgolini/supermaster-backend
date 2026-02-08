package ar.com.leo.super_master_backend.dominio.proveedor.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository repo;
    private final ProveedorMapper mapper;
    private final RecalculoPrecioFacade recalculoFacade;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repo.findByProveedorContainingIgnoreCaseOrApodoContainingIgnoreCase(search, search, pageable)
                    .map(mapper::toDTO);
        }
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

        BigDecimal porcentajeAnterior = entity.getFinanciacionPorcentaje();

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);

        // Recalcular precios si cambió el porcentaje de financiación
        if (cambioPorcentaje(porcentajeAnterior, entity.getFinanciacionPorcentaje())) {
            recalculoFacade.recalcularPorCambioProveedor(id);
        }

        return mapper.toDTO(entity);
    }

    private boolean cambioPorcentaje(BigDecimal anterior, BigDecimal nuevo) {
        if (anterior == null && nuevo == null) return false;
        if (anterior == null || nuevo == null) return true;
        return anterior.compareTo(nuevo) != 0;
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Proveedor no encontrado");
        }
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResumenDTO> listarProductos(Integer proveedorId) {
        if (!repo.existsById(proveedorId)) {
            throw new NotFoundException("Proveedor no encontrado");
        }
        return productoRepository.findByProveedorId(proveedorId)
                .stream()
                .map(productoMapper::toResumenDTO)
                .toList();
    }

}