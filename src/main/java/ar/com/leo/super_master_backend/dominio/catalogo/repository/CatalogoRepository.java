package ar.com.leo.super_master_backend.dominio.catalogo.repository;

import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
    Optional<Catalogo> findByCatalogoIgnoreCase(String catalogo);

    Page<Catalogo> findByCatalogoContainingIgnoreCase(String texto, Pageable pageable);
}