package ar.com.leo.super_master_backend.dominio.origen.repository;

import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface OrigenRepository extends JpaRepository<Origen, Integer> {
    Optional<Origen> findByOrigenIgnoreCase(String origen);

    Page<Origen> findByOrigenContainingIgnoreCase(String texto, Pageable pageable);
}