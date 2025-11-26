package ar.com.leo.super_master_backend.dominio.canal.repository;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanalConceptoRepository extends JpaRepository<CanalConcepto, CanalConceptoId> {

    List<CanalConcepto> findByCanalId(Integer canalId);

    List<CanalConcepto> findByConceptoId(Integer conceptoId);

    void deleteByCanalIdAndConceptoId(Integer canalId, Integer conceptoId);

}