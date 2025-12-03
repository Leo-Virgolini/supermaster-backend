package ar.com.leo.super_master_backend.dominio.marca.repository;

import ar.com.leo.super_master_backend.dominio.marca.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Integer> {
    Optional<Marca> findByNombreIgnoreCase(String nombre);
}