package ar.com.leo.super_master_backend.dominio.regla_descuento.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoCreateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.mapper.ReglaDescuentoMapper;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReglaDescuentoServiceImpl implements ReglaDescuentoService {

    private final ReglaDescuentoRepository repo;
    private final ReglaDescuentoMapper mapper;
    private final RecalculoPrecioFacade recalculoFacade;

    @Override
    @Transactional(readOnly = true)
    public Page<ReglaDescuentoDTO> listar(Pageable pageable) {
        return repo.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReglaDescuentoDTO> listarPorCanal(Integer canalId) {
        return repo.findByCanalId(canalId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReglaDescuentoDTO obtener(Integer id) {
        return repo.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Regla de descuento no encontrada"));
    }

    @Override
    @Transactional
    public ReglaDescuentoDTO crear(ReglaDescuentoCreateDTO dto) {
        ReglaDescuento entity = mapper.toEntity(dto);
        repo.save(entity);

        // Recalcular precios del canal
        recalculoFacade.recalcularPorCambioReglaDescuentoEnCanal(entity.getCanal().getId());

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ReglaDescuentoDTO actualizar(Integer id, ReglaDescuentoUpdateDTO dto) {
        ReglaDescuento entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Regla de descuento no encontrada"));

        Integer canalId = entity.getCanal().getId();

        mapper.updateEntityFromDTO(dto, entity);
        repo.save(entity);

        // Recalcular precios del canal
        recalculoFacade.recalcularPorCambioReglaDescuentoEnCanal(canalId);

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        ReglaDescuento entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Regla de descuento no encontrada"));

        Integer canalId = entity.getCanal().getId();

        repo.deleteById(id);

        // Recalcular precios del canal
        recalculoFacade.recalcularPorCambioReglaDescuentoEnCanal(canalId);
    }

}