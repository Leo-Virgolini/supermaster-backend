package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoCuotaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCuota;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalConceptoCuotaMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalConceptoCuotaServiceImpl implements CanalConceptoCuotaService {

    private final CanalConceptoCuotaRepository repository;
    private final CanalRepository canalRepository;
    private final CanalConceptoCuotaMapper mapper;

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

        // Verificar si ya existe una cuota con la misma combinación (canal, cuotas,
        // tipo)
        TipoCuota tipoCuota = parseTipoCuota(dto.tipo());
        if (repository.findByCanalIdAndCuotasAndTipo(dto.canalId(), dto.cuotas(), tipoCuota).isPresent()) {
            throw new NotFoundException("Ya existe una cuota con estos parámetros");
        }

        CanalConceptoCuota entity = mapper.toEntity(dto);
        repository.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public CanalConceptoCuotaDTO actualizar(Long id, CanalConceptoCuotaUpdateDTO dto) {
        CanalConceptoCuota entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cuota no encontrada"));

        // Si se actualiza el tipo o las cuotas, verificar que no exista otra con la
        // misma combinación
        if (dto.tipo() != null || dto.cuotas() != null) {
            Integer nuevasCuotas = dto.cuotas() != null ? dto.cuotas() : entity.getCuotas();
            TipoCuota nuevoTipo = dto.tipo() != null ? parseTipoCuota(dto.tipo()) : entity.getTipo();
            Integer canalId = entity.getCanal().getId();

            // Verificar si existe otra cuota con la misma combinación (excluyendo la
            // actual)
            repository.findByCanalIdAndCuotasAndTipo(canalId, nuevasCuotas, nuevoTipo)
                    .ifPresent(existente -> {
                        if (!existente.getId().equals(id)) {
                            throw new NotFoundException("Ya existe otra cuota con estos parámetros");
                        }
                    });
        }

        mapper.updateEntityFromDTO(dto, entity);
        repository.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Cuota no encontrada");
        }
        repository.deleteById(id);
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

    private TipoCuota parseTipoCuota(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return TipoCuota.NORMAL;
        }
        try {
            return TipoCuota.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoCuota.NORMAL;
        }
    }
}
