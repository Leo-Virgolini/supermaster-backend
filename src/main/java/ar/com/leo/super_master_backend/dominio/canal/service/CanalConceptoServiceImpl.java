package ar.com.leo.super_master_backend.dominio.canal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoId;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalConceptoMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.repository.ConceptoGastoRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CanalConceptoServiceImpl implements CanalConceptoService {

    private final CanalConceptoRepository canalConceptoRepository;
    private final CanalRepository canalRepository;
    private final ConceptoGastoRepository conceptoRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final CalculoPrecioService calculoPrecioService;
    private final CanalConceptoMapper canalConceptoMapper;

    // ==========================================
    // LISTAR
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<CanalConceptoDTO> listarPorCanal(Integer canalId) {

        return canalConceptoRepository.findByCanalId(canalId)
                .stream()
                .map(canalConceptoMapper::toDTO)
                .toList();
    }

    // ==========================================
    // ASIGNAR CONCEPTO A CANAL
    // ==========================================
    @Override
    @Transactional
    public CanalConceptoDTO asignarConcepto(Integer canalId, Integer conceptoId) {

        // 1) validar canal
        Canal canal = canalRepository.findById(canalId)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));

        // 2) validar concepto
        ConceptoGasto concepto = conceptoRepository.findById(conceptoId)
                .orElseThrow(() -> new NotFoundException("Concepto no encontrado"));

        // 3) crear relación
        CanalConcepto cc = new CanalConcepto();
        cc.setId(new CanalConceptoId(canalId, conceptoId));
        cc.setCanal(canal);
        cc.setConcepto(concepto);

        canalConceptoRepository.save(cc);

        // 4) 🔥 recalcular precios de todos los productos del canal
        recalcularProductosDelCanal(canalId);

        return canalConceptoMapper.toDTO(cc);
    }

    // ==========================================
    // ELIMINAR CONCEPTO DE CANAL
    // ==========================================
    @Override
    @Transactional
    public void eliminarConcepto(Integer canalId, Integer conceptoId) {
        CanalConceptoId id = new CanalConceptoId(canalId, conceptoId);
        if (canalConceptoRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Relación Canal-Concepto no existe");
        }

        canalConceptoRepository.deleteByCanalIdAndConceptoId(canalId, conceptoId);

        // 🔥 recalcular
        recalcularProductosDelCanal(canalId);
    }

    // ==========================================
    // MÉTODO CENTRAL DE RECÁLCULO
    // ==========================================
    private void recalcularProductosDelCanal(Integer canalId) {

        List<ProductoCanalPrecio> preciosCanal =
                productoCanalPrecioRepository.findByCanalId(canalId);

        preciosCanal.forEach(precio ->
                calculoPrecioService.recalcularYGuardarPrecioCanal(
                        precio.getProducto().getId(),
                        canalId
                )
        );
    }

}