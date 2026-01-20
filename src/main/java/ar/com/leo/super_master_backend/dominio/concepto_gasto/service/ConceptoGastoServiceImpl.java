package ar.com.leo.super_master_backend.dominio.concepto_gasto.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.mapper.ConceptoGastoMapper;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.repository.ConceptoGastoRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConceptoGastoServiceImpl implements ConceptoGastoService {

    private final ConceptoGastoRepository conceptoRepository;
    private final RecalculoPrecioFacade recalculoFacade;
    private final ConceptoGastoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ConceptoGastoDTO> listar(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return conceptoRepository.findByConceptoContainingIgnoreCaseOrDescripcionContainingIgnoreCase(search, search, pageable)
                    .map(mapper::toDTO);
        }
        return conceptoRepository.findAll(pageable)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ConceptoGastoDTO obtener(Integer id) {
        return conceptoRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Concepto no encontrado"));
    }

    @Override
    @Transactional
    public ConceptoGastoDTO crear(ConceptoGastoCreateDTO dto) {
        ConceptoGasto entity = mapper.toEntity(dto);
        conceptoRepository.save(entity);
        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ConceptoGastoDTO actualizar(Integer id, ConceptoGastoUpdateDTO dto) {

        ConceptoGasto entity = conceptoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Concepto no encontrado"));

        BigDecimal porcentajeAnterior = entity.getPorcentaje();

        mapper.updateEntityFromDTO(dto, entity);
        conceptoRepository.save(entity);

        // Si cambió el porcentaje, recalcular todos los productos afectados
        if (dto.porcentaje() != null && dto.porcentaje().compareTo(porcentajeAnterior) != 0) {
            recalculoFacade.recalcularPorCambioConceptoGasto(id);
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