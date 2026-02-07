package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalConceptoCuotaMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.ConflictException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalConceptoCuotaServiceImpl implements CanalConceptoCuotaService {

    private final CanalConceptoCuotaRepository repository;
    private final CanalRepository canalRepository;
    private final CanalConceptoCuotaMapper mapper;
    private final RecalculoPrecioFacade recalculoFacade;

    @Override
    @Transactional(readOnly = true)
    public Page<CanalConceptoCuotaDTO> listar(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CanalConceptoCuotaDTO obtener(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Cuota no encontrada"));
    }

    @Override
    @Transactional
    public CanalConceptoCuotaDTO crear(CanalConceptoCuotaCreateDTO dto) {
        // Validar que el canal existe
        if (!canalRepository.existsById(dto.canalId())) {
            throw new NotFoundException("Canal no encontrado");
        }

        // Verificar si ya existe una cuota con la misma combinación (canal, cuotas)
        if (!repository.findByCanalIdAndCuotas(dto.canalId(), dto.cuotas()).isEmpty()) {
            throw new ConflictException("Ya existe una cuota con estos parámetros");
        }

        CanalConceptoCuota entity = mapper.toEntity(dto);
        repository.save(entity);

        // Recalcular precios del canal
        recalculoFacade.recalcularPorCambioCuotaCanal(dto.canalId());

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public CanalConceptoCuotaDTO actualizar(Long id, CanalConceptoCuotaUpdateDTO dto) {
        CanalConceptoCuota entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cuota no encontrada"));

        // Si se actualizan las cuotas, verificar que no exista otra con la misma combinación
        if (dto.cuotas() != null) {
            Integer nuevasCuotas = dto.cuotas();
            Integer canalId = entity.getCanal().getId();

            // Verificar si existe otra cuota con la misma combinación (excluyendo la actual)
            List<CanalConceptoCuota> existentes = repository.findByCanalIdAndCuotas(canalId, nuevasCuotas);
            for (CanalConceptoCuota existente : existentes) {
                if (!existente.getId().equals(id)) {
                    throw new ConflictException("Ya existe otra cuota con estos parámetros");
                }
            }
        }

        BigDecimal porcentajeAnterior = entity.getPorcentaje();
        Integer canalId = entity.getCanal().getId();

        mapper.updateEntityFromDTO(dto, entity);
        repository.save(entity);

        // Recalcular si cambió el porcentaje
        if (dto.porcentaje() != null && (porcentajeAnterior == null || porcentajeAnterior.compareTo(dto.porcentaje()) != 0)) {
            recalculoFacade.recalcularPorCambioCuotaCanal(canalId);
        }

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        CanalConceptoCuota entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cuota no encontrada"));

        Integer canalId = entity.getCanal().getId();

        repository.deleteById(id);

        // Recalcular precios del canal
        recalculoFacade.recalcularPorCambioCuotaCanal(canalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalConceptoCuotaDTO> listarPorCanal(Integer canalId) {
        return repository.findByCanalId(canalId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalConceptoCuotaDTO> listarPorCanalYCuotas(Integer canalId, Integer cuotas) {
        return repository.findByCanalIdAndCuotas(canalId, cuotas)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
}
