package ar.com.leo.super_master_backend.dominio.canal.repository;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanalConceptoReglaRepository extends JpaRepository<CanalConceptoRegla, Long> {
    
    List<CanalConceptoRegla> findByCanalId(Integer canalId);
    
    List<CanalConceptoRegla> findByConceptoId(Integer conceptoId);
    
    List<CanalConceptoRegla> findByCanalIdAndConceptoId(Integer canalId, Integer conceptoId);
}

