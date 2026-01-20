package ar.com.leo.super_master_backend.dominio.producto.mla.repository;

import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface MlaRepository extends JpaRepository<Mla, Integer> {

    Optional<Mla> findByMla(String mla);

    Page<Mla> findByMlaContainingIgnoreCaseOrMlauContainingIgnoreCase(String mla, String mlau, Pageable pageable);
}
