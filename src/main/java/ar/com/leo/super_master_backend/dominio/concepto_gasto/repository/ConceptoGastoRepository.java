package ar.com.leo.super_master_backend.dominio.concepto_gasto.repository;

import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptoGastoRepository extends JpaRepository<ConceptoGasto, Integer> {

    Page<ConceptoGasto> findByConceptoContainingIgnoreCaseOrDescripcionContainingIgnoreCase(String concepto, String descripcion, Pageable pageable);
}