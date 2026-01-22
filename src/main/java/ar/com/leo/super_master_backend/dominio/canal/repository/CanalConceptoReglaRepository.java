package ar.com.leo.super_master_backend.dominio.canal.repository;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanalConceptoReglaRepository extends JpaRepository<CanalConceptoRegla, Long> {

    List<CanalConceptoRegla> findByCanalId(Integer canalId);

    List<CanalConceptoRegla> findByConceptoId(Integer conceptoId);

    List<CanalConceptoRegla> findByCanalIdAndConceptoId(Integer canalId, Integer conceptoId);

    /**
     * Obtiene reglas del canal con FETCH JOIN de relaciones para evitar N+1.
     * Usado en obtención de conceptos aplicables durante cálculos de precios.
     */
    @Query("SELECT ccr FROM CanalConceptoRegla ccr " +
           "LEFT JOIN FETCH ccr.concepto " +
           "LEFT JOIN FETCH ccr.tipo " +
           "LEFT JOIN FETCH ccr.marca " +
           "LEFT JOIN FETCH ccr.clasifGral " +
           "LEFT JOIN FETCH ccr.clasifGastro " +
           "WHERE ccr.canal.id = :canalId")
    List<CanalConceptoRegla> findByCanalIdWithRelationsFetch(@Param("canalId") Integer canalId);
}

