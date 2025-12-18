package ar.com.leo.super_master_backend.dominio.concepto_gasto.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoCreateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.dto.ConceptoGastoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.mapper.ConceptoGastoMapper;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.repository.ConceptoGastoRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptoGastoServiceImpl implements ConceptoGastoService {

    private final ConceptoGastoRepository conceptoRepository;
    private final CanalConceptoRepository canalConceptoRepository;
    private final ProductoCanalRepository productoCanalRepository;
    private final CalculoPrecioService calculoPrecioService;
    private final ConceptoGastoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ConceptoGastoDTO> listar(Pageable pageable) {
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

        // 🔥 Si no cambió el porcentaje → NO recalcular (mejora opcional)
        if (dto.porcentaje() != null && dto.porcentaje().compareTo(porcentajeAnterior) == 0) {
            return mapper.toDTO(entity);
        }

        // 🔍 buscar todos los canales que usan este concepto
        List<CanalConcepto> canales = canalConceptoRepository.findByConceptoId(id);

        // 🔥 recalcular todos los productos de cada canal
        canales.stream()
                .map(cc -> cc.getCanal().getId())
                .distinct()
                .forEach(idCanal -> productoCanalRepository.findByCanalId(idCanal)
                        .forEach(pc -> calculoPrecioService.recalcularYGuardarPrecioCanal(
                                pc.getProducto().getId(),
                                idCanal)));

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