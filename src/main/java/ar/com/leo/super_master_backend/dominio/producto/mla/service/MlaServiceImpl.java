package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoResumenDTO;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaCreateDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaUpdateDTO;
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
    public MlaDTO crear(MlaCreateDTO dto) {
        Mla entity = mapper.toEntity(dto);
        repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public MlaDTO actualizar(Integer id, MlaUpdateDTO dto) {
        Mla entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("MLA no encontrado"));

        // Guardar valores anteriores para detectar cambios
        BigDecimal precioEnvioAnterior = entity.getPrecioEnvio();
        BigDecimal comisionPorcentajeAnterior = entity.getComisionPorcentaje();

        mapper.updateEntity(dto, entity);

        repo.save(entity);

        // Recalcular si cambió precioEnvio o comisionPorcentaje
        boolean cambioPrecioEnvio = !Objects.equals(precioEnvioAnterior, entity.getPrecioEnvio());
        boolean cambioComision = !Objects.equals(comisionPorcentajeAnterior, entity.getComisionPorcentaje());

        if (cambioPrecioEnvio || cambioComision) {
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
