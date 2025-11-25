package ar.com.leo.super_master_backend.dominio.canal.repository;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanalConceptoRepository extends JpaRepository<CanalConcepto, CanalConceptoId> {

    List<CanalConcepto> findByIdCanalId(Integer idCanal);

    List<CanalConcepto> findByIdConceptoId(Integer idConcepto);

    void deleteByIdCanalIdAndIdConceptoId(Integer idCanal, Integer idConcepto);
}