package ar.com.leo.super_master_backend.dominio.concepto_calculo.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.entity.ConceptoCalculo;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.mapper.ConceptoCalculoMapper;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.repository.ConceptoCalculoRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConceptoCalculoServiceImpl implements ConceptoCalculoService {

    private final ConceptoCalculoRepository conceptoRepository;
    private final RecalculoPrecioFacade recalculoFacade;
    private final ConceptoCalculoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ConceptoCalculoDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return conceptoRepository.findByConceptoContainingIgnoreCaseOrDescripcionContainingIgnoreCase(search, search, pageable)
                    .map(mapper::toDTO);
        }
        return conceptoRepository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ConceptoCalculoDTO obtener(Integer id) {
        return conceptoRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Concepto no encontrado"));
    }

    @Override
    @Transactional
    public ConceptoCalculoDTO crear(ConceptoCalculoCreateDTO dto) {
        ConceptoCalculo entity = mapper.toEntity(dto);
        conceptoRepository.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ConceptoCalculoDTO actualizar(Integer id, ConceptoCalculoUpdateDTO dto) {

        ConceptoCalculo entity = conceptoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Concepto no encontrado"));

        BigDecimal porcentajeAnterior = entity.getPorcentaje();
        var aplicaSobreAnterior = entity.getAplicaSobre();

        mapper.updateEntityFromDTO(dto, entity);
        conceptoRepository.save(entity);

        // Si cambió el porcentaje o aplicaSobre, recalcular todos los productos afectados
        boolean cambioPorcentaje = dto.porcentaje() != null && (porcentajeAnterior == null || dto.porcentaje().compareTo(porcentajeAnterior) != 0);
        boolean cambioAplicaSobre = dto.aplicaSobre() != null && !dto.aplicaSobre().equals(aplicaSobreAnterior);

        if (cambioPorcentaje || cambioAplicaSobre) {
            recalculoFacade.recalcularPorCambioConceptoCalculo(id);
        }

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!conceptoRepository.existsById(id)) {
            throw new NotFoundException("Concepto no encontrado");
        }
        conceptoRepository.deleteById(id);
    }
}
