package ar.com.leo.super_master_backend.dominio.canal.repository;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CanalConceptoCuotaRepository extends JpaRepository<CanalConceptoCuota, Long> {
    
    List<CanalConceptoCuota> findByCanalId(Integer canalId);
    
    List<CanalConceptoCuota> findByCanalIdAndCuotas(Integer canalId, Integer cuotas);
    
    Optional<CanalConceptoCuota> findByCanalIdAndCuotasAndTipo(Integer canalId, Integer cuotas, TipoCuota tipo);
}

