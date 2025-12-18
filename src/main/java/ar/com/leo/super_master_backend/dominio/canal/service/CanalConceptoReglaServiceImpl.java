package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoReglaCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoReglaDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoReglaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalConceptoReglaMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoReglaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.repository.ClasifGastroRepository;
import ar.com.leo.super_master_backend.dominio.clasif_gral.repository.ClasifGralRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.repository.ConceptoGastoRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.marca.repository.MarcaRepository;
import ar.com.leo.super_master_backend.dominio.tipo.repository.TipoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalConceptoReglaServiceImpl implements CanalConceptoReglaService {

    private final CanalConceptoReglaRepository repository;
    private final CanalRepository canalRepository;
    private final ConceptoGastoRepository conceptoGastoRepository;
    private final TipoRepository tipoRepository;
    private final ClasifGastroRepository clasifGastroRepository;
    private final ClasifGralRepository clasifGralRepository;
    private final MarcaRepository marcaRepository;
    private final CanalConceptoReglaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CanalConceptoReglaDTO> listar(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CanalConceptoReglaDTO obtener(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Regla no encontrada"));
    }

    @Override
    @Transactional
    public CanalConceptoReglaDTO crear(CanalConceptoReglaCreateDTO dto) {
        // Validar que el canal existe
        if (!canalRepository.existsById(dto.canalId())) {
            throw new NotFoundException("Canal no encontrado");
        }

        // Validar que el concepto existe
        if (!conceptoGastoRepository.existsById(dto.conceptoId())) {
            throw new NotFoundException("Concepto no encontrado");
        }

        // Validar relaciones opcionales si están presentes
        if (dto.tipoId() != null && !tipoRepository.existsById(dto.tipoId())) {
            throw new NotFoundException("Tipo no encontrado");
        }
        if (dto.clasifGastroId() != null && !clasifGastroRepository.existsById(dto.clasifGastroId())) {
            throw new NotFoundException("Clasificación gastro no encontrada");
        }
        if (dto.clasifGralId() != null && !clasifGralRepository.existsById(dto.clasifGralId())) {
            throw new NotFoundException("Clasificación general no encontrada");
        }
        if (dto.marcaId() != null && !marcaRepository.existsById(dto.marcaId())) {
            throw new NotFoundException("Marca no encontrada");
        }

        CanalConceptoRegla entity = mapper.toEntity(dto);
        repository.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public CanalConceptoReglaDTO actualizar(Long id, CanalConceptoReglaUpdateDTO dto) {
        CanalConceptoRegla entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Regla no encontrada"));

        // Validar relaciones si están presentes en el DTO
        if (dto.canalId() != null && !canalRepository.existsById(dto.canalId())) {
            throw new NotFoundException("Canal no encontrado");
        }
        if (dto.conceptoId() != null && !conceptoGastoRepository.existsById(dto.conceptoId())) {
            throw new NotFoundException("Concepto no encontrado");
        }
        if (dto.tipoId() != null && !tipoRepository.existsById(dto.tipoId())) {
            throw new NotFoundException("Tipo no encontrado");
        }
        if (dto.clasifGastroId() != null && !clasifGastroRepository.existsById(dto.clasifGastroId())) {
            throw new NotFoundException("Clasificación gastro no encontrada");
        }
        if (dto.clasifGralId() != null && !clasifGralRepository.existsById(dto.clasifGralId())) {
            throw new NotFoundException("Clasificación general no encontrada");
        }
        if (dto.marcaId() != null && !marcaRepository.existsById(dto.marcaId())) {
            throw new NotFoundException("Marca no encontrada");
        }

        mapper.updateEntityFromDTO(dto, entity);
        repository.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Regla no encontrada");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalConceptoReglaDTO> listarPorCanal(Integer canalId) {
        return repository.findByCanalId(canalId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CanalConceptoReglaDTO> listarPorConcepto(Integer conceptoId) {
        return repository.findByConceptoId(conceptoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
}
