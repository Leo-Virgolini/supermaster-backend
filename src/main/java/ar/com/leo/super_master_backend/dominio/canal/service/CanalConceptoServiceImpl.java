package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalConceptoDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoId;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalConceptoMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalConceptoServiceImpl implements CanalConceptoService {

    private final CanalConceptoRepository repo;
    private final CanalConceptoMapper mapper;

    @Override
    public List<CanalConceptoDTO> listarPorCanal(Integer canalId) {
        return repo.findByIdCanalId(canalId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public CanalConceptoDTO asignarConcepto(Integer canalId, Integer conceptoId) {

        CanalConcepto entity = new CanalConcepto();
        entity.setId(new CanalConceptoId(canalId, conceptoId));
        entity.setCanal(new Canal(canalId));
        entity.setConcepto(new ConceptoGasto(conceptoId));

        repo.save(entity);

        return mapper.toDTO(entity);
    }

    @Override
    public void eliminarConcepto(Integer canalId, Integer conceptoId) {
        repo.deleteByIdCanalIdAndIdConceptoId(canalId, conceptoId);
    }

}